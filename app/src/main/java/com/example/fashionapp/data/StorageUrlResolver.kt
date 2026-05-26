package com.example.fashionapp.data

import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.tasks.await

object StorageUrlResolver {
    private const val TAG = "StorageUrlResolver"
    private val storage = FirebaseStorage.getInstance()
    private val cache   = ConcurrentHashMap<String, String>()

    suspend fun resolve(path: String): String {
        if (path.isBlank()) return ""
        if (path.startsWith("http")) return path
        if (path.startsWith("gs://")) {
            return resolveStorageUrl(path)
        }
        
        val sanitizedPath = path.removePrefix("/")
        
        if (cache.containsKey(sanitizedPath)) {
            return cache[sanitizedPath] ?: ""
        }

        return try {
            val url = storage.reference.child(sanitizedPath).downloadUrl.await().toString()
            cache[sanitizedPath] = url
            url
        } catch (_: Exception) {
            // Cache the failure to avoid repeated network calls for non-existent objects
            cache[sanitizedPath] = ""
            Log.w(TAG, "Missing or inaccessible storage object: $sanitizedPath")
            ""
        }
    }

    private suspend fun resolveStorageUrl(url: String): String {
        if (cache.containsKey(url)) {
            return cache[url] ?: ""
        }

        return try {
            val downloadUrl = storage.getReferenceFromUrl(url).downloadUrl.await().toString()
            cache[url] = downloadUrl
            downloadUrl
        } catch (_: Exception) {
            cache[url] = ""
            Log.w(TAG, "Missing or inaccessible storage URL: $url")
            ""
        }
    }

    suspend fun resolveAll(paths: List<String>): List<String> {
        return paths.mapNotNull { path ->
            resolve(path).takeIf { it.isNotBlank() }
        }
    }

    fun clearCache() { cache.clear() }
}
