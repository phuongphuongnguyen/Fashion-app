package com.example.fashionapp.ui.app.shopDashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fashionapp.data.shop.DailyRevenuePoint
import com.example.fashionapp.data.shop.ShopDashboardRepository
import com.example.fashionapp.data.shop.ShopProductStat
import com.example.fashionapp.data.shop.ShopStats
import com.example.fashionapp.data.user.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ShopDashboardUiState(
    val isLoading: Boolean = true,
    val shopId: String = "",
    val shopName: String = "",
    val stats: ShopStats = ShopStats(),
    val dailyRevenue: List<DailyRevenuePoint> = emptyList(),
    val products: List<ShopProductStat> = emptyList(),
    val error: String? = null
)

class ShopDashboardViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(ShopDashboardUiState())
    val uiState: StateFlow<ShopDashboardUiState> = _uiState.asStateFlow()

    init { load() }

    fun refresh() = load()

    private fun load() {
        val uid = auth.currentUser?.uid.orEmpty()
        if (uid.isBlank()) {
            _uiState.value = ShopDashboardUiState(isLoading = false, error = "Bạn cần đăng nhập")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val user = userRepository.getUserProfile(uid)
            // shopId: ưu tiên users/{uid}.shopId, fallback về chính uid (lúc đăng ký shop, shopId = uid)
            val shopId = user?.shopId?.takeIf { it.isNotBlank() } ?: uid

            val statsDeferred = async { ShopDashboardRepository.getShopStats(shopId) }
            val dailyDeferred = async { ShopDashboardRepository.getShopDailyRevenue(shopId, limit = 7) }
            val productsDeferred = async { ShopDashboardRepository.getProductsForShop(shopId) }

            _uiState.value = ShopDashboardUiState(
                isLoading = false,
                shopId = shopId,
                shopName = user?.name.orEmpty(),
                stats = statsDeferred.await() ?: ShopStats(),
                dailyRevenue = dailyDeferred.await(),
                products = productsDeferred.await(),
                error = null
            )
        }
    }
}