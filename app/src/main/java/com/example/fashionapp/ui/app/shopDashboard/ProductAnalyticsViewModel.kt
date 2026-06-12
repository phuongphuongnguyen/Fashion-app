package com.example.fashionapp.ui.app.shopDashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fashionapp.data.product.ProductRepository
import com.example.fashionapp.data.shop.DailyRevenuePoint
import com.example.fashionapp.data.shop.ShopDashboardRepository
import com.example.fashionapp.model.Product
import com.example.fashionapp.model.ProductVariant
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProductAnalyticsUiState(
    val isLoading: Boolean = true,
    val product: Product? = null,
    val revenue: Double = 0.0,
    val dailyRevenue: List<DailyRevenuePoint> = emptyList(),
    val error: String? = null
)

class ProductAnalyticsViewModel(private val productId: String) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductAnalyticsUiState())
    val uiState: StateFlow<ProductAnalyticsUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val productDeferred = async { ProductRepository.getProductById(productId) }
            val revenueDeferred = async { ShopDashboardRepository.getProductRevenue(productId) }
            val dailyDeferred = async { ShopDashboardRepository.getProductDailyRevenue(productId, limit = 7) }

            val product = productDeferred.await()

            _uiState.value = ProductAnalyticsUiState(
                isLoading = false,
                product = product,
                revenue = revenueDeferred.await(),
                dailyRevenue = dailyDeferred.await(),
                error = if (product == null) "Không tìm thấy sản phẩm" else null
            )
        }
    }

    fun updatePrice(newPrice: Double) {
        viewModelScope.launch {
            try {
                ProductRepository.updateProductPrice(productId, newPrice)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun updateStock(variants: List<ProductVariant>) {
        viewModelScope.launch {
            try {
                ProductRepository.updateProductStock(productId, variants)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}

class ProductAnalyticsViewModelFactory(
    private val productId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ProductAnalyticsViewModel(productId) as T
    }
}
