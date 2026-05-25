package com.example.fashionapp.data

import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

object StorageUrlResolver {
    private val storage = FirebaseStorage.getInstance()
    private val cache   = mutableMapOf<String, String>()

    suspend fun resolve(path: String): String {
        if (path.isBlank()) return ""
        if (path.startsWith("http")) return path
        cache[path]?.let { return it }
        return try {
            val url = storage.reference.child(path).downloadUrl.await().toString()
            cache[path] = url
            url
        } catch (_: Exception) { "" }
    }

    fun clearCache() { cache.clear() }
}
