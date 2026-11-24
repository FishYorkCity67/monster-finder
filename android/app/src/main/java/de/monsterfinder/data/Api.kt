package de.monsterfinder.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiException(message: String) : Exception(message)

class MonsterApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun search(baseUrl: String, plz: String, radiusKm: Int): SearchResponse =
        withContext(Dispatchers.IO) {
            val url = base(baseUrl).newBuilder()
                .addPathSegments("api/search")
                .addQueryParameter("plz", plz)
                .addQueryParameter("radius", radiusKm.toString())
                .build()
            json.decodeFromString(SearchResponse.serializer(), get(url.toString()))
        }

    suspend fun ping(baseUrl: String): Boolean = withContext(Dispatchers.IO) {
        runCatching { get(base(baseUrl).newBuilder().addPathSegment("health").build().toString()) }.isSuccess
    }

    private fun base(raw: String) =
        raw.trim().trimEnd('/').let { if (it.startsWith("http")) it else "http://$it" }
            .toHttpUrlOrNull() ?: throw ApiException("Ungültige Server-Adresse")

    private fun get(url: String): String {
        try {
            client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    val detail = runCatching {
                        json.parseToJsonElement(body).jsonObject["detail"]?.jsonPrimitive?.content
                    }.getOrNull()
                    throw ApiException(detail ?: "Serverfehler (${resp.code})")
                }
                return body
            }
        } catch (e: IOException) {
            throw ApiException("Raspberry Pi nicht erreichbar – läuft der Server und bist du im selben WLAN?")
        }
    }
}
