package com.example.niloralive

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Zero-permission image compression helper paired with Android Photo Picker
 * (`ActivityResultContracts.PickVisualMedia`). Scales and JPEG-compresses
 * picked images so they fit safely inside Firestore document bounds without
 * requesting broad storage permissions.
 */
object ImageEncodingHelper {
    private const val MAX_DIMENSION_PX = 768
    private const val TARGET_MAX_BYTES = 110_000
    const val MAX_BASE64_LENGTH = 165_000

    suspend fun encodePickerUriToBase64(context: Context, uri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, boundsOpts)
                } ?: return@withContext Result.failure(IllegalStateException("Unable to open selected image."))

                val sampleSize = calculateInSampleSize(
                    rawWidth = boundsOpts.outWidth,
                    rawHeight = boundsOpts.outHeight,
                    reqMax = MAX_DIMENSION_PX
                )
                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val decoded = resolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOpts)
                } ?: return@withContext Result.failure(IllegalStateException("Selected photo could not be decoded."))

                val scaled = scaleDownIfNeeded(decoded, MAX_DIMENSION_PX)
                if (scaled !== decoded) decoded.recycle()

                var quality = 82
                var bytes: ByteArray
                do {
                    val out = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                    bytes = out.toByteArray()
                    quality -= 12
                } while (bytes.size > TARGET_MAX_BYTES && quality >= 34)

                scaled.recycle()
                val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
                if (encoded.length > MAX_BASE64_LENGTH) {
                    Result.failure(IllegalStateException("Selected photo exceeds size limits even after compression."))
                } else {
                    Result.success(encoded)
                }
            } catch (e: Exception) {
                Result.failure(IllegalStateException("Could not process photo: ${e.localizedMessage ?: "Unsupported format"}"))
            }
        }

    fun decodeBase64ToBitmap(base64: String): Bitmap? {
        if (base64.isBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(rawWidth: Int, rawHeight: Int, reqMax: Int): Int {
        var inSampleSize = 1
        if (rawWidth <= 0 || rawHeight <= 0) return 1
        val maxDim = maxOf(rawWidth, rawHeight)
        while ((maxDim / inSampleSize) > reqMax * 2) {
            inSampleSize *= 2
        }
        return inSampleSize
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap
        val ratio = maxDim.toFloat() / maxOf(w, h).toFloat()
        val newW = (w * ratio).toInt().coerceAtLeast(1)
        val newH = (h * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }
}

@Composable
fun rememberDecodedBase64Image(base64: String): ImageBitmap? {
    val state by produceState<ImageBitmap?>(initialValue = null, key1 = base64) {
        value = if (base64.isBlank()) {
            null
        } else {
            withContext(Dispatchers.Default) {
                ImageEncodingHelper.decodeBase64ToBitmap(base64)?.asImageBitmap()
            }
        }
    }
    return state
}
