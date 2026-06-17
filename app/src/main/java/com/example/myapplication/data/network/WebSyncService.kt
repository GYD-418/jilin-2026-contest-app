package com.example.myapplication.data.network

import com.example.myapplication.data.model.Post
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class WebSyncService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun fetchPosts(url: String): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    IOException("HTTP ${response.code}: ${response.message}")
                )
            }

            val body = response.body?.string() ?: return@withContext Result.failure(
                IOException("Empty response body")
            )

            val type = object : TypeToken<List<Post>>() {}.type
            val posts: List<Post> = gson.fromJson(body, type)

            Result.success(posts)
        } catch (e: IOException) {
            Result.failure(e)
        }
    }
}
