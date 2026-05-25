package com.example.fashionapp.ui.app.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fashionapp.data.search.SearchRepository
import com.example.fashionapp.data.search.SubCategory
import com.example.fashionapp.model.Product
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SearchUiState(
    val isLoadingInitial: Boolean = true,  // lần đầu fetch Firestore
    val isSearching: Boolean = false,       // đang filter (debounce)
    val query: String = "",
    val subCategories: List<SubCategory> = emptyList(),
    val selectedCategoryId: String? = null,
    val products: List<Product> = emptyList(),
    val sortOption: SortOption = SortOption.DEFAULT,
)

enum class SortOption(val label: String) {
    DEFAULT("Mặc định"),
    PRICE_ASC("Giá tăng dần"),
    PRICE_DESC("Giá giảm dần"),
    POPULAR("Bán chạy nhất"),
    RATING("Đánh giá cao nhất"),
}

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val initialQuery: String = "",
    private val initialCategoryId: String? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SearchUiState(
            query              = initialQuery,
            selectedCategoryId = initialCategoryId,
        )
    )
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    // Flow nội bộ để debounce input — tách khỏi uiState
    private val _searchTrigger = MutableStateFlow(
        Pair(initialQuery, initialCategoryId)
    )

    init {
        loadSubCategories()
        setupSearchPipeline()
        // Fetch Firestore lần đầu ngay khi init
        triggerSearch(initialQuery, initialCategoryId)
    }

    // ── Pipeline: debounce → search ───────────────────────────────────────
    private fun setupSearchPipeline() {
        viewModelScope.launch {
            _searchTrigger
                .debounce(350L)           // chờ user ngừng gõ 350ms
                .distinctUntilChanged()   // bỏ qua nếu giống lần trước
                .collect { (query, categoryId) ->
                    performSearch(query, categoryId)
                }
        }
    }

    private fun triggerSearch(query: String, categoryId: String?) {
        _searchTrigger.value = Pair(query, categoryId)
    }

    private suspend fun performSearch(query: String, categoryId: String?) {
        _uiState.update { it.copy(isSearching = true) }

        // search() dùng cache từ Singleton SearchRepository
        val results = SearchRepository.search(query, categoryId)
        val sorted  = applySorting(results, _uiState.value.sortOption)

        _uiState.update {
            it.copy(
                isLoadingInitial = false,
                isSearching      = false,
                products         = sorted,
            )
        }
    }

    // ── Public actions ────────────────────────────────────────────────────

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        triggerSearch(newQuery, _uiState.value.selectedCategoryId)
    }

    fun onCategorySelected(categoryId: String?) {
        // Click lại category đang chọn → bỏ chọn
        val newId = if (_uiState.value.selectedCategoryId == categoryId) null else categoryId
        _uiState.update { it.copy(selectedCategoryId = newId) }
        triggerSearch(_uiState.value.query, newId)
    }

    fun onSortSelected(option: SortOption) {
        val sorted = applySorting(_uiState.value.products, option)
        _uiState.update { it.copy(sortOption = option, products = sorted) }
    }

    fun clearQuery() {
        _uiState.update { it.copy(query = "") }
        triggerSearch("", _uiState.value.selectedCategoryId)
    }

    fun refresh() {
        SearchRepository.clearCache()
        triggerSearch(_uiState.value.query, _uiState.value.selectedCategoryId)
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun loadSubCategories() {
        viewModelScope.launch {
            val subs = SearchRepository.getSubCategories()
            _uiState.update { it.copy(subCategories = subs) }
        }
    }

    private fun applySorting(products: List<Product>, option: SortOption): List<Product> {
        return when (option) {
            SortOption.DEFAULT    -> products
            SortOption.PRICE_ASC  -> products.sortedBy { it.price }
            SortOption.PRICE_DESC -> products.sortedByDescending { it.price }
            SortOption.POPULAR    -> products.sortedByDescending { it.soldCount }
            SortOption.RATING     -> products.sortedByDescending { it.rating }
        }
    }
}

// ── Factory ───────────────────────────────────────────────────────────────────
class SearchViewModelFactory(
    private val initialQuery: String,
    private val initialCategoryId: String?,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SearchViewModel(initialQuery, initialCategoryId) as T
    }
}
