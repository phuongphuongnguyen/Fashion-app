package com.example.fashionapp.data.search

import android.content.Context

class SearchHistoryRepository(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Lấy danh sách lịch sử các từ khóa tìm kiếm đã lưu từ bộ nhớ cục bộ SharedPreferences
    fun getHistory(): List<String> {
        val raw = prefs.getString(KEY_HISTORY, null).orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.split(SEPARATOR).filter { it.isNotBlank() }
    }

    // Thêm một từ khóa tìm kiếm mới vào lịch sử, giới hạn số lượng lưu tối đa và sắp xếp lên đầu danh sách
    fun addQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val updated = buildList {
            add(trimmed)
            addAll(getHistory().filterNot { it.equals(trimmed, ignoreCase = true) })
        }.take(MAX_ITEMS)
        save(updated)
    }

    // Xóa một từ khóa cụ thể khỏi danh sách lịch sử tìm kiếm cục bộ của người dùng
    fun removeQuery(query: String) {
        val updated = getHistory().filterNot { it.equals(query, ignoreCase = true) }
        save(updated)
    }

    // Xóa sạch toàn bộ lịch sử tìm kiếm của người dùng trong bộ nhớ cục bộ
    fun clear() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    private fun save(items: List<String>) {
        prefs.edit().putString(KEY_HISTORY, items.joinToString(SEPARATOR)).apply()
    }

    companion object {
        private const val PREFS_NAME = "fashion_app_search"
        private const val KEY_HISTORY = "search_history"
        private const val SEPARATOR = ""
        private const val MAX_ITEMS = 10

        val POPULAR_KEYWORDS = listOf(
            "Váy mùa hè", "Áo sơ mi", "Quần jean", "Giày sneaker",
            "Túi xách", "Đồng hồ", "Áo khoác", "Hoodie", "Thể thao"
        )
    }
}
