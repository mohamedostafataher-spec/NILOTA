package com.example.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.BuildConfig
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class GpsCoordinates(val latitude: Double, val longitude: Double)

data class LivePlaceResult(
    val placeId: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val rating: Double?,
    val openNow: Boolean?
)

sealed interface PlacesSearchOutcome {
    data class Success(val places: List<LivePlaceResult>, val source: String) : PlacesSearchOutcome
    data object MissingCredentials : PlacesSearchOutcome
    data class Error(val message: String) : PlacesSearchOutcome
}

@JsonClass(generateAdapter = true)
data class GooglePlacesResponse(
    val status: String = "",
    @Json(name = "error_message") val errorMessage: String? = null,
    val results: List<GooglePlaceDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GooglePlaceDto(
    @Json(name = "place_id") val placeId: String = "",
    val name: String = "",
    @Json(name = "formatted_address") val formattedAddress: String = "",
    val geometry: GooglePlaceGeometry? = null,
    val rating: Double? = null,
    @Json(name = "opening_hours") val openingHours: GooglePlaceHours? = null
)

@JsonClass(generateAdapter = true)
data class GooglePlaceGeometry(val location: GoogleLatLng? = null)

@JsonClass(generateAdapter = true)
data class GoogleLatLng(val lat: Double = 0.0, val lng: Double = 0.0)

@JsonClass(generateAdapter = true)
data class GooglePlaceHours(@Json(name = "open_now") val openNow: Boolean? = null)

interface GooglePlacesApi {
    @GET("maps/api/place/textsearch/json")
    suspend fun textSearch(
        @Query("query") query: String,
        @Query("key") apiKey: String
    ): GooglePlacesResponse
}

class LocationAndPlacesRepository(private val context: Context) {
    private val appContext = context.applicationContext
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(appContext)

    private val placesApi: GooglePlacesApi by lazy {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val client = OkHttpClient.Builder().build()
        Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GooglePlacesApi::class.java)
    }

    fun isNetworkOnline(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val active = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(active) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun isGoogleMapsKeyConfigured(): Boolean =
        BuildConfig.GOOGLE_MAPS_API_KEY.isNotBlank() &&
            BuildConfig.GOOGLE_MAPS_API_KEY != "MY_GOOGLE_MAPS_API_KEY"

    @SuppressLint("MissingPermission")
    suspend fun getCurrentGpsLocation(): Result<GpsCoordinates> = try {
        val tokenSource = CancellationTokenSource()
        val loc = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, tokenSource.token).await()
            ?: fusedLocationClient.lastLocation.await()
        if (loc != null) {
            Result.success(GpsCoordinates(loc.latitude, loc.longitude))
        } else {
            Result.failure(IllegalStateException("Device GPS location unavailable right now. Ensure location services are enabled."))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun searchVerifiedGooglePlaces(query: String): PlacesSearchOutcome {
        val key = BuildConfig.GOOGLE_MAPS_API_KEY
        if (!isGoogleMapsKeyConfigured()) {
            return PlacesSearchOutcome.MissingCredentials
        }
        return try {
            val resp = placesApi.textSearch(query = query, apiKey = key)
            if (resp.status == "OK" || resp.status == "ZERO_RESULTS") {
                val mapped = resp.results.mapNotNull { dto ->
                    val latLng = dto.geometry?.location ?: return@mapNotNull null
                    LivePlaceResult(
                        placeId = dto.placeId,
                        name = dto.name,
                        address = dto.formattedAddress,
                        latitude = latLng.lat,
                        longitude = latLng.lng,
                        rating = dto.rating,
                        openNow = dto.openingHours?.openNow
                    )
                }
                PlacesSearchOutcome.Success(mapped, "Google Places API")
            } else {
                PlacesSearchOutcome.Error(resp.errorMessage ?: "Google Places API status: ${resp.status}")
            }
        } catch (e: Exception) {
            PlacesSearchOutcome.Error(e.localizedMessage ?: "Failed to reach Google Places API")
        }
    }
}
