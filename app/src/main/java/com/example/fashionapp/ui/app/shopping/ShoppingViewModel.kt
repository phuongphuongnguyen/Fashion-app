package com.example.fashionapp.ui.app.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fashionapp.data.shop.ShopRepository
import com.example.fashionapp.model.Category
import com.example.fashionapp.model.Product
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ShoppingUiState(
    val isLoading: Boolean = true,
    val categories: List<Category> = emptyList(),
    val newItems: List<Product> = emptyList(),
    val mostPopular: List<Product> = emptyList(),
    val forYou: List<Product> = emptyList(),
)

class ShoppingViewModel : ViewModel() {
    private val repository = ShopRepository()

    private val _uiState = MutableStateFlow(ShoppingUiState())
    val uiState: StateFlow<ShoppingUiState> = _uiState.asStateFlow()

    init { loadAll() }

    private fun loadAll() {
        viewModelScope.launch {
            // Load song song
            val catDeferred     = async { repository.getCategories() }
            val newDeferred     = async { repository.getNewItems() }
            val popularDeferred = async { repository.getMostPopular() }
            val forYouDeferred  = async { repository.getForYou() }

            _uiState.value = ShoppingUiState(
                isLoading   = false,
                categories  = catDeferred.await(),
                newItems    = newDeferred.await(),
                mostPopular = popularDeferred.await(),
                forYou      = forYouDeferred.await(),
            )
        }
    }
}