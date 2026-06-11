package com.example.fashionapp.ui.app.productdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.fashionapp.data.ProductReview
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
    val productReviews: List<ProductReview> = emptyList(),
    val isAddingToCart: Boolean = false,
    val cartError: String? = null,
    // Khi != null → đã thêm món "Buy Now" vào giỏ, sẵn sàng sang checkout
    val buyNowCartItemId: String? = null,
    val error: String? = null,
)

class ProductDetailViewModel(private val productId: String) : ViewModel() {
    private companion object {
        const val TAG = "ProductDetailViewModel"
    }

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
            product?.id?.takeIf { it.isNotBlank() }?.let { loadedProductId ->
                launch {
                    ShopRepository.getReviewsForProductFlow(loadedProductId).collect { reviews ->
                        _uiState.value = _uiState.value.copy(productReviews = reviews)
                    }
                }
            }
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

    fun addToCart(variant: ProductVariant? = _uiState.value.selectedVariant, quantity: Int = 1) {
        val product = _uiState.value.product
        if (product == null) {
            _uiState.value = _uiState.value.copy(cartError = "Product is not available")
            return
        }

        val userId = auth.currentUser?.uid
        if (userId.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(cartError = "Please sign in before adding to cart")
            return
        }

        val cartItem = createCartItem(product, variant, quantity)

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAddingToCart = true, cartError = null)
            try {
                ShopRepository.addToCart(userId, cartItem)
                Log.d(TAG, "Added cart item ${cartItem.id} for user $userId")
                _uiState.value = _uiState.value.copy(
                    isAddedToCart = true,
                    isAddingToCart = false,
                    cartError = null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add cart item ${cartItem.id} for user $userId", e)
                _uiState.value = _uiState.value.copy(
                    isAddedToCart = false,
                    isAddingToCart = false,
                    cartError = e.message ?: "Failed to add to cart"
                )
            }
        }
    }

    fun buyNow(variant: ProductVariant? = _uiState.value.selectedVariant, quantity: Int = 1) {
        val product = _uiState.value.product
        if (product == null) {
            _uiState.value = _uiState.value.copy(cartError = "Product is not available")
            return
        }

        val userId = auth.currentUser?.uid
        if (userId.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(cartError = "Please sign in before checkout")
            return
        }

        val cartItem = createCartItem(product, variant, quantity)

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAddingToCart = true, cartError = null)
            try {
                val cartItemId = ShopRepository.addToCart(userId, cartItem)
                Log.d(TAG, "Buy now: added cart item $cartItemId for user $userId")
                _uiState.value = _uiState.value.copy(
                    buyNowCartItemId = cartItemId,
                    isAddingToCart = false,
                    cartError = null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Buy now failed for ${cartItem.id} (user $userId)", e)
                _uiState.value = _uiState.value.copy(
                    isAddingToCart = false,
                    cartError = e.message ?: "Failed to checkout"
                )
            }
        }
    }

    private fun createCartItem(product: Product, variant: ProductVariant?, quantity: Int) =
        CartItem(
            id = "${product.id}_${System.currentTimeMillis()}",
            product = product,
            color = variant?.color ?: "Default",
            size = variant?.size ?: "Default",
            quantity = quantity
        )

    fun consumeCartResult() {
        _uiState.value = _uiState.value.copy(isAddedToCart = false, cartError = null)
    }

    fun consumeBuyNow() {
        _uiState.value = _uiState.value.copy(buyNowCartItemId = null)
    }
}

// Factory để truyền productId vào ViewModel
class ProductDetailViewModelFactory(private val productId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ProductDetailViewModel(productId) as T
    }
}
