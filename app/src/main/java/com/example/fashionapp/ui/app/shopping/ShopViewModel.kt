package com.example.fashionapp.ui.app.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fashionapp.data.CartItem
import com.example.fashionapp.data.ReviewOrder
import com.example.fashionapp.data.feed.FeedRepository
import com.example.fashionapp.data.shop.ShopRepository
import com.example.fashionapp.data.user.UserSession
import com.example.fashionapp.data.user.UserRepository
import com.example.fashionapp.model.Post
import com.example.fashionapp.model.Product
import com.example.fashionapp.model.User
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ShopUiState(
    val posts: List<Post> = emptyList(),
    val products: List<Product> = emptyList(),
    val cartItems: List<CartItem> = emptyList(),
    val orders: List<ReviewOrder> = emptyList(),
    val shopUser: User? = null,
    val shopLogoUrl: String = "",
    val isLoadingProducts: Boolean = true,
    val isLoadingCart: Boolean = true,
    val isLoadingOrders: Boolean = true
)

class ShopViewModel : ViewModel() {
    private val repository = ShopRepository
    private val feedRepository = FeedRepository()
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(ShopUiState())
    val uiState: StateFlow<ShopUiState> = _uiState.asStateFlow()

    init {
        loadProducts()
        loadCartItems()
        loadOrders()
    }

    fun loadShopUser(shopId: String) {
        if (shopId.isBlank()) return
        loadShopPosts(shopId)
        viewModelScope.launch {
            userRepository.getUserProfileFlow(shopId).collect { user ->
                _uiState.value = _uiState.value.copy(shopUser = user)
            }
        }
        viewModelScope.launch {
            repository.getShopLogoUrlFlow(shopId).collect { logoUrl ->
                _uiState.value = _uiState.value.copy(shopLogoUrl = logoUrl)
            }
        }
    }

    private fun loadShopPosts(shopId: String) {
        viewModelScope.launch {
            feedRepository.getPostsByAuthorFlow(shopId).collect { posts ->
                _uiState.value = _uiState.value.copy(posts = posts)
            }
        }
    }

    private fun loadProducts() {
        viewModelScope.launch {
            repository.getProductsFlow().collect { products ->
                _uiState.value = _uiState.value.copy(
                    products = products,
                    isLoadingProducts = false
                )
            }
        }
    }

    private fun loadCartItems() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            repository.getCartItemsFlow(userId).collect { items ->
                _uiState.value = _uiState.value.copy(
                    cartItems = items,
                    isLoadingCart = false
                )
            }
        }
    }

    private fun loadOrders() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            repository.getOrdersFlow(userId).collect { orders ->
                _uiState.value = _uiState.value.copy(
                    orders = orders,
                    isLoadingOrders = false
                )
            }
        }
    }

    fun addToCart(product: Product, color: String, size: String, quantity: Int = 1) {
        val userId = auth.currentUser?.uid ?: return
        val cartItem = CartItem(
            id = "${product.id}_${System.currentTimeMillis()}",
            product = product,
            color = color,
            size = size,
            quantity = quantity
        )
        viewModelScope.launch {
            repository.addToCart(userId, cartItem)
        }
    }

    fun updateCartQuantity(cartItemId: String, newQuantity: Int) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            repository.updateCartItemQuantity(userId, cartItemId, newQuantity)
        }
    }

    fun placeOrderFromCart(
        cartItems: List<CartItem> = _uiState.value.cartItems,
        paymentMethod: String = "visa",
        shippingMethod: String = "standard",
        shippingFee: Double = 0.0,
        shippingAddress: String = ""
    ) {
        val userId = auth.currentUser?.uid ?: return
        val items = cartItems
        if (items.isEmpty()) return

        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
        val currentDate = dateFormat.format(Date())
        val placedAtMillis = System.currentTimeMillis()

        val orders = items.map {
            ReviewOrder(
                id = "order_${System.currentTimeMillis()}_${it.id}",
                product = it.product,
                status = "Ongoing",
                orderDate = currentDate,
                placedAtMillis = placedAtMillis
            )
        }

        viewModelScope.launch {
            repository.placeOrder(
                userId = userId,
                orders = orders,
                paymentMethod = paymentMethod,
                shippingMethod = shippingMethod,
                shippingFee = shippingFee,
                shippingAddress = shippingAddress
            )
            repository.clearCart(userId, items)
        }
    }

    fun submitReview(
        orderId: String,
        rating: Int,
        comment: String,
        onComplete: (Boolean) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: run {
            onComplete(false)
            return
        }
        val order = _uiState.value.orders.firstOrNull { it.id == orderId } ?: run {
            onComplete(false)
            return
        }
        val currentUser = UserSession.currentUser.value

        viewModelScope.launch {
            val result = runCatching {
                repository.submitProductReview(
                    userId = userId,
                    order = order,
                    rating = rating,
                    comment = comment,
                    userName = currentUser?.name.orEmpty().ifBlank {
                        auth.currentUser?.displayName.orEmpty().ifBlank {
                            auth.currentUser?.email.orEmpty()
                        }
                    },
                    userAvatarUrl = currentUser?.avatarUrl.orEmpty()
                )
            }
            onComplete(result.isSuccess)
        }
    }
}
