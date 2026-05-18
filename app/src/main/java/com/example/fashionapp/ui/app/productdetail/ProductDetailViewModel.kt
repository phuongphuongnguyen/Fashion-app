package com.example.fashionapp.ui.app.productdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fashionapp.data.product.ProductRepository
import com.example.fashionapp.model.Product
import com.example.fashionapp.model.ProductVariant
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProductDetailUiState(
    val isLoading: Boolean = true,
    val product: Product? = null,
    val relatedProducts: List<Product> = emptyList(),
    val selectedVariant: ProductVariant? = null,
    val selectedImageIndex: Int = 0,
    val isWishlisted: Boolean = false,
    val error: String? = null,
)

class ProductDetailViewModel(private val productId: String) : ViewModel() {
    private val repository = ProductRepository()

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    init { loadAll() }

    private fun loadAll() {
        viewModelScope.launch {
            val productDeferred = async { repository.getProductById(productId) }
            val relatedDeferred = async { repository.getMostPopular(excludeId = productId) }

            val product = productDeferred.await()
            val related = relatedDeferred.await()

            _uiState.value = ProductDetailUiState(
                isLoading        = false,
                product          = product,
                relatedProducts  = related,
                selectedVariant  = product?.variants?.firstOrNull(),
                error            = if (product == null) "Không tìm thấy sản phẩm" else null,
            )
        }
    }

    fun selectVariant(variant: ProductVariant) {
        _uiState.value = _uiState.value.copy(selectedVariant = variant)
    }

    fun selectImage(index: Int) {
        _uiState.value = _uiState.value.copy(selectedImageIndex = index)
    }

    fun toggleWishlist() {
        _uiState.value = _uiState.value.copy(
            isWishlisted = !_uiState.value.isWishlisted
        )
    }
}

// Factory để truyền productId vào ViewModel
class ProductDetailViewModelFactory(private val productId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ProductDetailViewModel(productId) as T
    }
}
