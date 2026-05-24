package com.example.fashionapp.ui.app.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fashionapp.data.shopping.ShoppingRepository
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

    private val _uiState = MutableStateFlow(ShoppingUiState())
    val uiState: StateFlow<ShoppingUiState> = _uiState.asStateFlow()

    init { loadAll() }

    private fun loadAll() {
        viewModelScope.launch {
            val catDeferred     = async { ShoppingRepository.getCategories() }
            val newDeferred     = async { ShoppingRepository.getNewItems() }
            val popularDeferred = async { ShoppingRepository.getMostPopular() }
            val forYouDeferred  = async { ShoppingRepository.getForYou() }

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
