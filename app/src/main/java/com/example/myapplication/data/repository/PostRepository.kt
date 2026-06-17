package com.example.myapplication.data.repository

import android.content.Context
import com.example.myapplication.data.model.Post
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class PostRepository(private val context: Context) {

    private val gson = Gson()
    private val storageFile: File
        get() = File(context.filesDir, "posts.json")

    fun savePosts(posts: List<Post>) {
        val json = gson.toJson(posts)
        storageFile.writeText(json)
    }

    fun loadPosts(): List<Post> {
        if (!storageFile.exists()) return emptyList()
        val json = storageFile.readText()
        val type = object : TypeToken<List<Post>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun hasCachedData(): Boolean = storageFile.exists() && storageFile.length() > 0
}
