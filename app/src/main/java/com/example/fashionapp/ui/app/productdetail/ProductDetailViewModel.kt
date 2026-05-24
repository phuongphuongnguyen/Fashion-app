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

import com.example.fashionapp.data.shop.ShopRepository
import com.example.fashionapp.data.CartItem
import com.google.firebase.auth.FirebaseAuth

data class ProductDetailUiState(
    val isLoading: Boolean = true,
    val product: Product? = null,
    val relatedProducts: List<Product> = emptyList(),
    val selectedVariant: ProductVariant? = null,
    val selectedImageIndex: Int = 0,
    val isWishlisted: Boolean = false,
    val isAddedToCart: Boolean = false,
    val error: String? = null,
)

class ProductDetailViewModel(private val productId: String) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    init { loadAll() }

    private fun loadAll() {
        viewModelScope.launch {
            // Lấy từ Singleton ProductRepository (có cache bên trong)
            val productDeferred = async { ProductRepository.getProductById(productId) }
            val relatedDeferred = async { ProductRepository.getMostPopular(excludeId = productId) }

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

    fun addToCart(quantity: Int = 1) {
        val product = _uiState.value.product ?: return
        val variant = _uiState.value.selectedVariant
        val userId = auth.currentUser?.uid ?: return

        val cartItem = CartItem(
            id = "${product.id}_${System.currentTimeMillis()}",
            product = product,
            color = variant?.color ?: "Default",
            size = variant?.size ?: "Default",
            quantity = quantity
        )

        viewModelScope.launch {
            ShopRepository.addToCart(userId, cartItem)
            _uiState.value = _uiState.value.copy(isAddedToCart = true)
        }
    }
}

// Factory để truyền productId vào ViewModel
class ProductDetailViewModelFactory(private val productId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ProductDetailViewModel(productId) as T
    }
}
