package com.example.myapplication.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapplication.data.network.WebSyncService
import com.example.myapplication.data.repository.PostRepository

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val SYNC_URL_KEY = "sync_url"
        const val DEFAULT_SYNC_URL = "https://jsonplaceholder.typicode.com/posts"
    }

    override suspend fun doWork(): Result {
        val url = inputData.getString(SYNC_URL_KEY) ?: DEFAULT_SYNC_URL
        val service = WebSyncService()
        val repository = PostRepository(applicationContext)

        return service.fetchPosts(url).fold(
            onSuccess = { posts ->
                repository.savePosts(posts)
                Result.success()
            },
            onFailure = { _ ->
                if (repository.hasCachedData()) {
                    Result.success()
                } else {
                    Result.retry()
                }
            }
        )
    }
}
