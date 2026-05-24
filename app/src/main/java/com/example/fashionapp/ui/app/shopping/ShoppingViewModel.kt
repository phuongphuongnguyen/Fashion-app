package com.example.fashionapp.ui.app.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fashionapp.data.CartItem
import com.example.fashionapp.data.ReviewOrder
import com.example.fashionapp.data.shopping.ShoppingRepository
import com.example.fashionapp.model.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ShoppingUiState(
    val products: List<Product> = emptyList(),
    val cartItems: List<CartItem> = emptyList(),
    val orders: List<ReviewOrder> = emptyList(),
    val isLoadingProducts: Boolean = true,
    val isLoadingCart: Boolean = true,
    val isLoadingOrders: Boolean = true
)

class ShoppingViewModel : ViewModel() {
    private val repository = ShoppingRepository()
    private val auth = FirebaseAuth.getInstance()
    
    private val _uiState = MutableStateFlow(ShoppingUiState())
    val uiState: StateFlow<ShoppingUiState> = _uiState.asStateFlow()

    init {
        loadProducts()
        loadCartItems()
        loadOrders()
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
            id = "${product.id}_${System.currentTimeMillis()}", // Generate a simple ID
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

    fun placeOrderFromCart() {
        val userId = auth.currentUser?.uid ?: return
        val items = _uiState.value.cartItems
        if (items.isEmpty()) return
        
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
        val currentDate = dateFormat.format(Date())

        val orders = items.map {
            ReviewOrder(
                id = "order_${System.currentTimeMillis()}_${it.id}",
                product = it.product,
                status = "Paid",
                orderDate = currentDate
            )
        }
        
        viewModelScope.launch {
            repository.placeOrder(userId, orders)
            repository.clearCart(userId, items)
        }
    }
}
