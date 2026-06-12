package com.example.fashionapp.ui.app.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fashionapp.data.shop.ShopRepository
import com.example.fashionapp.model.Category
import com.example.fashionapp.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ShoppingUiState(
    val isLoading: Boolean = true,
    val categories: List<Category> = emptyList(),
    val newItems: List<Product> = emptyList(),
    val mostPopular: List<Product> = emptyList(),
    val forYou: List<Product> = emptyList(),
)

class ShoppingViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ShoppingUiState())
    val uiState: StateFlow<ShoppingUiState> = _uiState.asStateFlow()

    init { loadAll() }

    private fun loadAll() {
        // Progressive load: mỗi section update vào state độc lập ngay khi xong,
        // không đợi cả 4 query hoàn tất. UI thấy section đầu tiên xuất hiện sớm.
        viewModelScope.launch {
            launch {
                val cats = ShopRepository.getCategories()
                _uiState.update { it.copy(categories = cats, isLoading = false) }
            }
            launch {
                val items = ShopRepository.getNewItems()
                _uiState.update { it.copy(newItems = items, isLoading = false) }
            }
            launch {
                val popular = ShopRepository.getMostPopular()
                _uiState.update { it.copy(mostPopular = popular, isLoading = false) }
            }
            launch {
                val forYou = ShopRepository.getForYou()
                _uiState.update { it.copy(forYou = forYou, isLoading = false) }
            }
        }
    }
}
