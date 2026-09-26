package com.example.domain.ai

import com.example.BuildConfig
import com.example.domain.german.GermanLanguageEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiGermanService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun isKeyConfigured(): Boolean {
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            key.isNotBlank() && key != "MY_GEMINI_API_KEY"
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Queries Gemini for in-depth German synonyms with nuances, register, and sample sentences.
     * Falls back to offline GermanLanguageEngine if key is missing or network fails.
     */
    suspend fun fetchDynamicSynonyms(word: String): List<String> = withContext(Dispatchers.IO) {
        val offlineSyns = GermanLanguageEngine.getSynonyms(word)
        if (!isKeyConfigured()) {
            return@withContext offlineSyns
        }

        val apiKey = BuildConfig.GEMINI_API_KEY
        val prompt = "List 4-6 high quality German synonyms or closely related words for the German word '$word'. Return ONLY a comma-separated list of the German words, nothing else."

        try {
            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val root = JSONObject(body)
                    val candidates = root.optJSONArray("candidates")
                    val text = candidates?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text")

                    if (!text.isNullOrBlank()) {
                        val parsed = text.split(",", ";", "\n")
                            .map { it.trim().removePrefix("-").trim() }
                            .filter { it.isNotBlank() && !it.equals(word, ignoreCase = true) }
                        if (parsed.isNotEmpty()) {
                            return@withContext (parsed + offlineSyns).distinct()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Graceful fallback to offline synonym engine
        }

        offlineSyns
    }
}
