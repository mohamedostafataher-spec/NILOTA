package com.example.data.repository

import com.example.data.model.LiveIntelligenceResult
import com.example.data.model.LiveWeatherSnapshot
import com.example.data.model.WikipediaSummaryResult
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class OpenMeteoForecastResponse(
    val current: OpenMeteoCurrentDto? = null
)

@JsonClass(generateAdapter = true)
data class OpenMeteoCurrentDto(
    @Json(name = "temperature_2m") val temperature2m: Double? = null,
    @Json(name = "wind_speed_10m") val windSpeed10m: Double? = null,
    @Json(name = "weather_code") val weatherCode: Int? = null
)

@JsonClass(generateAdapter = true)
data class OpenMeteoMarineResponse(
    val current: OpenMeteoMarineCurrentDto? = null
)

@JsonClass(generateAdapter = true)
data class OpenMeteoMarineCurrentDto(
    @Json(name = "wave_height") val waveHeight: Double? = null,
    @Json(name = "sea_surface_temperature") val seaSurfaceTemperature: Double? = null
)

@JsonClass(generateAdapter = true)
data class WikipediaSummaryDto(
    val title: String = "",
    val extract: String = "",
    @Json(name = "content_urls") val contentUrls: WikiContentUrlsDto? = null
)

@JsonClass(generateAdapter = true)
data class WikiContentUrlsDto(
    val mobile: WikiUrlDto? = null,
    val desktop: WikiUrlDto? = null
)

@JsonClass(generateAdapter = true)
data class WikiUrlDto(
    val page: String = ""
)

interface OpenMeteoForecastApi {
    @GET("v1/forecast")
    suspend fun getCurrentForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,wind_speed_10m,weather_code"
    ): OpenMeteoForecastResponse
}

interface OpenMeteoMarineApi {
    @GET("v1/marine")
    suspend fun getCurrentMarine(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "wave_height,sea_surface_temperature"
    ): OpenMeteoMarineResponse
}

interface WikipediaSummaryApi {
    @GET("api/rest_v1/page/summary/{title}")
    suspend fun getPageSummary(
        @Path("title") title: String,
        @Header("User-Agent") userAgent: String = "NILORA-Travel-Platform/1.0"
    ): WikipediaSummaryDto
}

class DestinationIntelligenceRepository {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val forecastApi: OpenMeteoForecastApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(httpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoForecastApi::class.java)
    }

    private val marineApi: OpenMeteoMarineApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://marine-api.open-meteo.com/")
            .client(httpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoMarineApi::class.java)
    }

    private val wikiApi: WikipediaSummaryApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://en.wikipedia.org/")
            .client(httpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(WikipediaSummaryApi::class.java)
    }

    suspend fun fetchLiveConditions(
        latitude: Double,
        longitude: Double,
        includeMarine: Boolean
    ): LiveIntelligenceResult<LiveWeatherSnapshot> {
        return try {
            val forecast = forecastApi.getCurrentForecast(latitude, longitude).current
            if (forecast?.temperature2m == null || forecast.windSpeed10m == null) {
                return LiveIntelligenceResult.Unavailable("Live Open-Meteo telemetry returned empty readings.")
            }
            val marineCurrent = if (includeMarine) {
                try {
                    marineApi.getCurrentMarine(latitude, longitude).current
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    null
                }
            } else {
                null
            }
            val code = forecast.weatherCode ?: 0
            LiveIntelligenceResult.Available(
                LiveWeatherSnapshot(
                    temperatureC = forecast.temperature2m,
                    windSpeedKmh = forecast.windSpeed10m,
                    weatherCode = code,
                    weatherLabel = describeWmoWeatherCode(code),
                    seaSurfaceTempC = marineCurrent?.seaSurfaceTemperature,
                    waveHeightMeters = marineCurrent?.waveHeight
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LiveIntelligenceResult.Unavailable(
                "Open-Meteo live conditions unavailable right now (${e.localizedMessage ?: "no network"})."
            )
        }
    }

    suspend fun fetchWikipediaSummary(slug: String): LiveIntelligenceResult<WikipediaSummaryResult> {
        if (slug.isBlank()) {
            return LiveIntelligenceResult.Unavailable("No Wikipedia entry linked.")
        }
        return try {
            val dto = wikiApi.getPageSummary(slug)
            if (dto.extract.isBlank()) {
                LiveIntelligenceResult.Unavailable("Wikipedia summary returned no text.")
            } else {
                LiveIntelligenceResult.Available(
                    WikipediaSummaryResult(
                        title = dto.title,
                        extract = dto.extract,
                        pageUrl = dto.contentUrls?.mobile?.page
                            ?: dto.contentUrls?.desktop?.page
                            ?: "https://en.wikipedia.org/wiki/$slug"
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LiveIntelligenceResult.Unavailable(
                "Wikipedia geographic context unavailable offline (${e.localizedMessage ?: "network error"})."
            )
        }
    }

    companion object {
        fun describeWmoWeatherCode(code: Int): String = when (code) {
            0 -> "Clear Sky"
            1, 2 -> "Mostly Clear"
            3 -> "Overcast"
            45, 48 -> "Coastal Mist"
            51, 53, 55 -> "Light Drizzle"
            61, 63, 65 -> "Rain"
            80, 81, 82 -> "Showers"
            95 -> "Thunderstorm Advisory"
            else -> "Ambient Conditions ($code)"
        }
    }
}
