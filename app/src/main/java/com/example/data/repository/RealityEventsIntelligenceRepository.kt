package com.example.data.repository

import com.example.data.model.EventVerificationTier
import com.example.data.model.ExternalOpenSourceEventNote
import com.example.data.model.LiveIntelligenceResult
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
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class WikiSearchEnvelopeDto(
    val query: WikiSearchQueryDto? = null
)

@JsonClass(generateAdapter = true)
data class WikiSearchQueryDto(
    val search: List<WikiSearchHitDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class WikiSearchHitDto(
    val pageid: Long = 0L,
    val title: String = "",
    val snippet: String = ""
)

interface WikipediaEventsApi {
    @GET("w/api.php")
    suspend fun searchPublicCulturalTopics(
        @Query("action") action: String = "query",
        @Query("list") list: String = "search",
        @Query("format") format: String = "json",
        @Query("srlimit") limit: Int = 6,
        @Query("srsearch") searchQuery: String,
        @Header("User-Agent") userAgent: String = "NILORA-Reality-Pulse/1.0"
    ): WikiSearchEnvelopeDto
}

/**
 * Fetches free, zero-key public cultural & sporting festival reference context from
 * Wikimedia/Wikipedia Open-Source Search API (CC BY-SA 4.0).
 * Explicitly tagged EXTERNAL_OPEN_SOURCE and never fabricates ticket prices, attendance, or offline events.
 */
class RealityEventsIntelligenceRepository {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val wikiEventsApi: WikipediaEventsApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://en.wikipedia.org/")
            .client(httpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(WikipediaEventsApi::class.java)
    }

    suspend fun fetchOpenSourceEventContext(
        cityName: String,
        countryName: String,
        categoryKeyword: String = "festival cultural sports exhibition"
    ): LiveIntelligenceResult<List<ExternalOpenSourceEventNote>> {
        val cleanCity = cityName.trim().ifBlank { "Cairo" }
        val cleanCountry = countryName.trim().ifBlank { "Egypt" }
        val queryText = "$cleanCity $cleanCountry $categoryKeyword".trim()
        return try {
            val response = wikiEventsApi.searchPublicCulturalTopics(searchQuery = queryText)
            val hits = response.query?.search.orEmpty().map { hit ->
                val plainSnippet = hit.snippet
                    .replace(Regex("<[^>]*>"), "")
                    .replace("&quot;", "\"")
                    .replace("&#039;", "'")
                    .replace("&amp;", "&")
                    .trim()
                val slug = hit.title.replace(" ", "_")
                ExternalOpenSourceEventNote(
                    id = "wiki_${hit.pageid}",
                    title = hit.title,
                    summary = plainSnippet.ifBlank { "Public encyclopedic entry for $cleanCity." },
                    cityName = cleanCity,
                    countryName = cleanCountry,
                    sourceUrl = "https://en.wikipedia.org/wiki/$slug",
                    attribution = "Wikipedia Open-Source API (CC BY-SA 4.0) — Confirm current dates officially",
                    verificationTier = EventVerificationTier.EXTERNAL_OPEN_SOURCE.key
                )
            }
            if (hits.isEmpty()) {
                LiveIntelligenceResult.Unavailable("No open-source cultural/event references found for $cleanCity.")
            } else {
                LiveIntelligenceResult.Available(hits)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LiveIntelligenceResult.Unavailable(
                "Open-source public event feed unavailable (${e.localizedMessage ?: "network offline"})."
            )
        }
    }
}
