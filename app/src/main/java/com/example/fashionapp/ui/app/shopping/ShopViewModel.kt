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
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
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
    val currentUserProfile: User? = null,
    val shopUser: User? = null,
    val shopLogoUrl: String = "",
    val shopRating: Float = 0f,
    val productShopId: String = "",   // id collection 'shops' để lọc products
    val isOwnProfile: Boolean = false,
    val isFollowing: Boolean = false,
    val isLoadingProducts: Boolean = true,
    val isLoadingCart: Boolean = true,
    val isLoadingOrders: Boolean = true,
    val cartError: String? = null,
    val orderError: String? = null,
    val reviewError: String? = null,
    val isPlacingOrder: Boolean = false,
    val isSubmittingReview: Boolean = false
)

class ShopViewModel : ViewModel() {
    private val repository = ShopRepository
    private val feedRepository = FeedRepository()
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()
    private var observedUserId: String? = "__not_loaded__"
    private var cartJob: Job? = null
    private var ordersJob: Job? = null
    private var userJob: Job? = null
    private val authStateListener = AuthStateListener { firebaseAuth ->
        loadUserShoppingData(firebaseAuth.currentUser?.uid)
    }

    private val _uiState = MutableStateFlow(ShopUiState())
    val uiState: StateFlow<ShopUiState> = _uiState.asStateFlow()

    init {
        loadProducts()
        auth.addAuthStateListener(authStateListener)
        refreshShoppingData()
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authStateListener)
        super.onCleared()
    }

    fun refreshShoppingData() {
        loadUserShoppingData(auth.currentUser?.uid, force = true)
    }

    private fun loadUserShoppingData(userId: String?, force: Boolean = false) {
        if (!force && observedUserId == userId) return
        observedUserId = userId
        cartJob?.cancel()
        ordersJob?.cancel()
        userJob?.cancel()

        if (userId.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(
                cartItems = emptyList(),
                orders = emptyList(),
                reviewsByOrderId = emptyMap(),
                currentUserProfile = null,
                isLoadingCart = false,
                isLoadingOrders = false,
                cartError = null
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            cartItems = emptyList(),
            orders = emptyList(),
            reviewsByOrderId = emptyMap(),
            currentUserProfile = null,
            isLoadingCart = true,
            isLoadingOrders = true,
            cartError = null
        )
        loadCurrentUserProfile(userId)
        loadCartItems(userId)
        loadOrders(userId)
    }

    private fun loadCurrentUserProfile(userId: String) {
        userJob = viewModelScope.launch {
            userRepository.getUserProfileFlow(userId).collect { user ->
                _uiState.value = _uiState.value.copy(currentUserProfile = user)
            }
        }
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

    private fun loadCartItems(userId: String) {
        cartJob = viewModelScope.launch {
            repository.getCartItemsFlow(userId).collect { snapshot ->
                _uiState.value = _uiState.value.copy(
                    cartItems = snapshot.items,
                    isLoadingCart = false,
                    cartError = snapshot.errorMessage
                )
            }
        }
    }

    private fun loadOrders(userId: String) {
        ordersJob = viewModelScope.launch {
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
            val result = runCatching { repository.addToCart(userId, cartItem) }
            result.exceptionOrNull()?.let { error ->
                _uiState.value = _uiState.value.copy(
                    cartError = error.message ?: "Unable to add item to cart"
                )
            }
        }
    }

    fun updateCartQuantity(cartItemId: String, newQuantity: Int) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val result = runCatching {
                repository.updateCartItemQuantity(userId, cartItemId, newQuantity)
            }
            result.exceptionOrNull()?.let { error ->
                _uiState.value = _uiState.value.copy(
                    cartError = error.message ?: "Unable to update cart"
                )
            }
        }
    }

    fun restoreCartItem(cartItem: CartItem) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val result = runCatching {
                repository.restoreCartItem(userId, cartItem)
            }
            result.exceptionOrNull()?.let { error ->
                _uiState.value = _uiState.value.copy(
                    cartError = error.message ?: "Unable to restore cart item"
                )
            }
        }
    }

    fun placeOrderFromCart(
        cartItems: List<CartItem> = _uiState.value.cartItems,
        paymentMethod: String = "visa",
        paymentStatus: String = "PAID",
        shippingMethod: String = "standard",
        shippingFee: Double = 0.0,
        shippingAddress: String = "",
        momoOrderId: String = "",
        onComplete: (String?) -> Unit = {}
    ) {
        val userId = auth.currentUser?.uid ?: run {
            _uiState.value = _uiState.value.copy(orderError = "Please sign in before checkout")
            onComplete(null)
            return
        }
        val items = cartItems
        if (items.isEmpty()) {
            _uiState.value = _uiState.value.copy(orderError = "Your checkout items are no longer available")
            onComplete(null)
            return
        }

        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
        val currentDate = dateFormat.format(Date())
        val placedAtMillis = System.currentTimeMillis()

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPlacingOrder = true, orderError = null)
            val result = runCatching {
                repository.placeOrder(
                    userId = userId,
                    cartItems = items,
                    orderDate = currentDate,
                    placedAtMillis = placedAtMillis,
                    paymentMethod = paymentMethod,
                    paymentStatus = paymentStatus,
                    shippingMethod = shippingMethod,
                    shippingFee = shippingFee,
                    shippingAddress = shippingAddress,
                    momoOrderId = momoOrderId
                )
            }
            _uiState.value = _uiState.value.copy(
                isPlacingOrder = false,
                orderError = result.exceptionOrNull()?.message
                    ?: if (result.getOrNull().isNullOrBlank()) "Unable to place order" else null
            )
            onComplete(result.getOrNull()?.takeIf { it.isNotBlank() })
        }
    }

    fun cancelOrder(orderId: String) {
        val userId = auth.currentUser?.uid ?: run {
            _uiState.value = _uiState.value.copy(orderError = "Please sign in before cancelling")
            return
        }
        val order = _uiState.value.orders.firstOrNull { it.id == orderId } ?: run {
            _uiState.value = _uiState.value.copy(orderError = "Order is no longer available")
            return
        }

        viewModelScope.launch {
            val result = runCatching {
                repository.cancelOrder(userId, order)
            }
            result.exceptionOrNull()?.let { error ->
                _uiState.value = _uiState.value.copy(
                    orderError = error.message ?: "Unable to cancel order"
                )
            }
        }
    }

    fun submitReview(
        orderId: String,
        rating: Int,
        comment: String,
        onComplete: (Boolean) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: run {
            _uiState.value = _uiState.value.copy(reviewError = "Please sign in before reviewing")
            onComplete(false)
            return
        }
        val order = _uiState.value.orders.firstOrNull { it.id == orderId } ?: run {
            _uiState.value = _uiState.value.copy(reviewError = "Order is no longer available")
            onComplete(false)
            return
        }
        val currentUser = UserSession.currentUser.value

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmittingReview = true, reviewError = null)
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
            _uiState.value = _uiState.value.copy(
                isSubmittingReview = false,
                reviewError = result.exceptionOrNull()?.message
                    ?: if (result.isFailure) "Unable to submit review" else null
            )
            onComplete(result.isSuccess)
        }
    }

    fun consumeOrderError() {
        _uiState.value = _uiState.value.copy(orderError = null)
    }

    fun consumeReviewError() {
        _uiState.value = _uiState.value.copy(reviewError = null)
    }
}
