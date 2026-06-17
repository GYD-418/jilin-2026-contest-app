package com.example.myapplication.data.network

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class HuaweiIamService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    suspend fun getToken(
        ak: String,
        sk: String,
        projectId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = "https://iam.myhuaweicloud.com/v3/auth/tokens"
            val bodyJson = """
                {
                    "auth": {
                        "identity": {
                            "methods": ["hw_ak_sk_request_body"],
                            "hw_ak_sk_request_body": {
                                "access_key": "$ak",
                                "secret_access_key": "$sk"
                            }
                        },
                        "scope": {
                            "project": {
                                "id": "$projectId"
                            }
                        }
                    }
                }
            """.trimIndent()

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = bodyJson.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .build()

            val response = client.newCall(request).execute()

            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    IOException("HTTP ${response.code}: $responseBody")
                )
            }

            val token = response.header("X-Subject-Token")
                ?: return@withContext Result.failure(
                    IOException("响应中无 Token，Body: $responseBody")
                )

            Result.success(token)
        } catch (e: IOException) {
            Result.failure(e)
        }
    }
}
