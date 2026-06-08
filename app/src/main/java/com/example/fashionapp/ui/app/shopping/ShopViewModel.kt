package com.example.fashionapp.ui.app.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fashionapp.data.CartItem
import com.example.fashionapp.data.ProductReview
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
    val reviewsByOrderId: Map<String, ProductReview> = emptyMap(),
    val shopUser: User? = null,
    val shopLogoUrl: String = "",
    val shopRating: Float = 0f,
    val productShopId: String = "",   // id collection 'shops' để lọc products
    val isOwnProfile: Boolean = false,
    val isFollowing: Boolean = false,
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

    // navId có thể là userId (vào từ feed/search) HOẶC shopId (vào từ product detail).
    // Resolve về đúng chủ tài khoản để mọi lối vào đều ra cùng một trang.
    fun loadShopUser(navId: String) {
        if (navId.isBlank()) return
        viewModelScope.launch {
            // 1) navId là userId? thử đọc trực tiếp. 2) nếu không, navId là shopId → tìm chủ shop.
            val owner = userRepository.getUserProfile(navId)
                ?: userRepository.findUserByShopId(navId)
            val ownerId = owner?.id ?: navId
            // shopId để lọc products: ưu tiên field shopId của user, nếu không có thì chính navId
            val productShopId = owner?.shopId?.takeIf { it.isNotBlank() } ?: navId

            val currentUid = auth.currentUser?.uid.orEmpty()
            val isOwn = currentUid.isNotBlank() && currentUid == ownerId
            _uiState.value = _uiState.value.copy(
                isOwnProfile = isOwn,
                productShopId = productShopId
            )

            // Posts theo ownerId
            launch {
                feedRepository.getPostsByAuthorFlow(ownerId).collect { posts ->
                    _uiState.value = _uiState.value.copy(posts = posts)
                }
            }
            // Profile realtime theo ownerId
            launch {
                userRepository.getUserProfileFlow(ownerId).collect { user ->
                    _uiState.value = _uiState.value.copy(shopUser = user)
                }
            }
            // Logo shop theo productShopId (doc shops/{shopId})
            launch {
                repository.getShopLogoUrlFlow(productShopId).collect { logoUrl ->
                    _uiState.value = _uiState.value.copy(shopLogoUrl = logoUrl)
                }
            }
            // Rating shop
            launch {
                repository.getShopRatingFlow(productShopId).collect { rating ->
                    _uiState.value = _uiState.value.copy(shopRating = rating)
                }
            }
            // Trạng thái follow
            if (!isOwn) {
                launch {
                    userRepository.isFollowingFlow(currentUid, ownerId).collect { following ->
                        _uiState.value = _uiState.value.copy(isFollowing = following)
                    }
                }
            }
        }
    }

    fun toggleFollow(targetUid: String) {
        val currentUid = auth.currentUser?.uid ?: return
        if (currentUid == targetUid) return
        val shouldFollow = !_uiState.value.isFollowing
        viewModelScope.launch {
            runCatching { userRepository.setFollowing(currentUid, targetUid, shouldFollow) }
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
                refreshReviews(orders)
            }
        }
    }

    private fun refreshReviews(orders: List<ReviewOrder> = _uiState.value.orders) {
        viewModelScope.launch {
            val reviews = repository.getReviewsForOrders(orders)
            _uiState.value = _uiState.value.copy(reviewsByOrderId = reviews)
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
            if (result.isSuccess) {
                refreshReviews()
            }
            onComplete(result.isSuccess)
        }
    }
}
