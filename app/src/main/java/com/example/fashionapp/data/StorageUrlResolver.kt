package com.example.fashionapp.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object StorageUrlResolver {
    private const val TAG = "StorageUrlResolver"
    private const val PREFS_NAME = "storage_url_cache"

    private val storage = FirebaseStorage.getInstance()
    private val cache   = ConcurrentHashMap<String, String>()
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var prefs: SharedPreferences? = null

    // Gọi 1 lần từ MainActivity.onCreate (hoặc Application.onCreate)
    fun init(context: Context) {
        if (prefs != null) return
        synchronized(this) {
            if (prefs != null) return
            val sp = context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            // Nạp toàn bộ entry vào in-memory map ngay lập tức
            sp.all.forEach { (key, value) ->
                if (value is String && value.isNotBlank()) {
                    cache[key] = value
                }
            }
            prefs = sp
            Log.d(TAG, "Loaded ${cache.size} URLs from disk cache")
        }
    }

    suspend fun resolve(path: String): String {
        val normalizedPath = path.trim()
        if (normalizedPath.isBlank()) return ""
        if (normalizedPath.startsWith("http")) return normalizedPath
        if (normalizedPath.startsWith("gs://")) {
            return resolveStorageUrl(normalizedPath)
        }

        val sanitizedPath = normalizedPath.removePrefix("/").trim()

        cache[sanitizedPath]?.let { return it }

        return try {
            val url = storage.reference.child(sanitizedPath).downloadUrl.await().toString()
            cache[sanitizedPath] = url
            persist(sanitizedPath, url)
            url
        } catch (_: Exception) {
            // Cache thất bại trong memory để khỏi retry liên tục, nhưng KHÔNG persist
            // (lỡ là transient error → reset sẽ tự retry lần app sau)
            cache[sanitizedPath] = ""
            Log.w(TAG, "Missing or inaccessible storage object: $sanitizedPath")
            ""
        }
    }

    private suspend fun resolveStorageUrl(url: String): String {
        cache[url]?.let { return it }

        return try {
            val downloadUrl = storage.getReferenceFromUrl(url).downloadUrl.await().toString()
            cache[url] = downloadUrl
            persist(url, downloadUrl)
            downloadUrl
        } catch (_: Exception) {
            cache[url] = ""
            Log.w(TAG, "Missing or inaccessible storage URL: $url")
            ""
        }
    }

    suspend fun resolveAll(paths: List<String>): List<String> = coroutineScope {
        paths.map { path -> async { resolve(path) } }
            .awaitAll()
            .filter { it.isNotBlank() }
    }

    fun clearCache() {
        cache.clear()
        ioScope.launch { prefs?.edit()?.clear()?.apply() }
    }

    private fun persist(key: String, url: String) {
        val sp = prefs ?: return
        ioScope.launch {
            sp.edit().putString(key, url).apply()
        }
    }
}
