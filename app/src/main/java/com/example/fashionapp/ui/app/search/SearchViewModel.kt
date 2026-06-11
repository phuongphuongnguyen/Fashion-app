package com.example.fashionapp.ui.app.search

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fashionapp.data.search.SearchHistoryRepository
import com.example.fashionapp.data.search.SearchRepository
import com.example.fashionapp.data.search.SubCategory
import com.example.fashionapp.model.Product
import com.example.fashionapp.model.User
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val isLoadingInitial: Boolean = false,
    val isSearching: Boolean = false,
    val query: String = "",
    val submittedQuery: String = "",
    val subCategories: List<SubCategory> = emptyList(),
    val selectedCategoryId: String? = null,
    val sortMode: String? = null,
    val products: List<Product> = emptyList(),
    val users: List<User> = emptyList(),
    val history: List<String> = emptyList(),
    val popular: List<String> = emptyList(),
) {
    val showSuggestions: Boolean
        get() = submittedQuery.isBlank() && selectedCategoryId.isNullOrBlank() && sortMode.isNullOrBlank()
}

class SearchViewModel(
    initialQuery: String = "",
    initialCategoryId: String? = null,
    initialSort: String? = null,
    private val historyRepo: SearchHistoryRepository,
) : ViewModel() {

    private val hasInitialCriteria = initialQuery.isNotBlank() || initialCategoryId != null || !initialSort.isNullOrBlank()

    private val _uiState = MutableStateFlow(
        SearchUiState(
            query              = initialQuery,
            submittedQuery     = if (hasInitialCriteria) initialQuery else "",
            selectedCategoryId = initialCategoryId,
            sortMode           = initialSort,
            popular            = SearchHistoryRepository.POPULAR_KEYWORDS,
            history            = historyRepo.getHistory(),
            isLoadingInitial   = hasInitialCriteria,
        )
    )
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        loadSubCategories()
        // Chỉ search nếu được mở với query/category sẵn (vd. từ Shopping → click category)
        if (hasInitialCriteria) {
            runSearch(initialQuery, initialCategoryId, initialSort)
        }
    }

    private fun runSearch(query: String, categoryId: String?, sortMode: String? = _uiState.value.sortMode) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            // Tìm sản phẩm + user song song
            val productsDeferred = async { SearchRepository.search(query, categoryId) }
            val usersDeferred = async { SearchRepository.searchUsers(query) }
            val products = sortProducts(productsDeferred.await(), sortMode)
            val users = usersDeferred.await()
            _uiState.update {
                it.copy(
                    isLoadingInitial = false,
                    isSearching      = false,
                    products         = products,
                    users            = users,
                    submittedQuery   = query,
                    sortMode         = sortMode,
                )
            }
        }
    }

    // ── Public actions ────────────────────────────────────────────────────

    // Chỉ cập nhật text — KHÔNG search ngay
    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
    }

    // Bấm Enter trên bàn phím → search
    fun onSearchSubmit() {
        val q = _uiState.value.query.trim()
        if (q.isNotBlank()) {
            historyRepo.addQuery(q)
            _uiState.update { it.copy(history = historyRepo.getHistory()) }
        }
        _uiState.update { it.copy(sortMode = null) }
        runSearch(q, _uiState.value.selectedCategoryId, null)
    }

    // Click category → search ngay
    fun onCategorySelected(categoryId: String?) {
        val newId = if (_uiState.value.selectedCategoryId == categoryId) null else categoryId
        _uiState.update { it.copy(selectedCategoryId = newId) }
        val q = _uiState.value.query.trim()
        if (newId != null || q.isNotBlank()) {
            runSearch(q, newId)
        } else {
            // Bỏ chọn category + không có query → về màn gợi ý
            resetToSuggestions()
        }
    }

    fun clearQuery() {
        _uiState.update { it.copy(query = "") }
        val cat = _uiState.value.selectedCategoryId
        if (cat != null) {
            runSearch("", cat)
        } else {
            resetToSuggestions()
        }
    }

    // Click vào gợi ý/lịch sử → search ngay
    fun onPickSuggestion(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        historyRepo.addQuery(trimmed)
        _uiState.update {
            it.copy(
                query   = trimmed,
                history = historyRepo.getHistory(),
            )
        }
        _uiState.update { it.copy(sortMode = null) }
        runSearch(trimmed, _uiState.value.selectedCategoryId, null)
    }

    fun removeHistoryItem(item: String) {
        historyRepo.removeQuery(item)
        _uiState.update { it.copy(history = historyRepo.getHistory()) }
    }

    fun clearHistory() {
        historyRepo.clear()
        _uiState.update { it.copy(history = emptyList()) }
    }

    fun refresh() {
        SearchRepository.clearCache()
        val s = _uiState.value
        if (s.submittedQuery.isNotBlank() || s.selectedCategoryId != null || !s.sortMode.isNullOrBlank()) {
            runSearch(s.submittedQuery, s.selectedCategoryId, s.sortMode)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun resetToSuggestions() {
        _uiState.update {
            it.copy(
                submittedQuery = "",
                products       = emptyList(),
                users          = emptyList(),
                isSearching    = false,
                sortMode       = null,
            )
        }
    }

    private fun loadSubCategories() {
        viewModelScope.launch {
            val subs = SearchRepository.getSubCategories()
            _uiState.update { it.copy(subCategories = subs) }
        }
    }

    private fun sortProducts(products: List<Product>, sortMode: String?): List<Product> {
        return when (sortMode) {
            "popular" -> products.sortedWith(
                compareByDescending<Product> { it.soldCount }
                    .thenByDescending { it.rating }
                    .thenBy { it.name }
            )
            "new" -> products.sortedWith(
                compareByDescending<Product> { it.createdAtMillis }
                    .thenByDescending { it.id }
            )
            else -> products
        }
    }
}

// ── Factory ───────────────────────────────────────────────────────────────────
class SearchViewModelFactory(
    private val initialQuery: String,
    private val initialCategoryId: String?,
    private val initialSort: String?,
    private val context: Context,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SearchViewModel(
            initialQuery      = initialQuery,
            initialCategoryId = initialCategoryId,
            initialSort       = initialSort,
            historyRepo       = SearchHistoryRepository(context),
        ) as T
    }
}
