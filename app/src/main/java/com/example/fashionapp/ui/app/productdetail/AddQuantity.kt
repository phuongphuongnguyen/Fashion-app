package com.example.fashionapp.ui.app.productdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.fashionapp.R
import com.example.fashionapp.model.Product
import com.example.fashionapp.model.ProductVariant
import com.example.fashionapp.ui.app.settings.LocalAppSettings
import com.example.fashionapp.ui.theme.AppTheme

// ── Palette (accent màu tồn kho — không phụ thuộc theme) ──────────────────────
private val OrangeStock  = Color(0xFFFF9500)
private val RedStock     = Color(0xFFFF3B30)

// ─────────────────────────────────────────────────────────────────────────────
//  PUBLIC COMPOSABLE
//
//  isBuyNow = false → nút đen "Add to Cart"
//  isBuyNow = true  → nút đen "Buy Now"
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddQuantity(
    product: Product,
    isBuyNow: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (variant: ProductVariant, quantity: Int) -> Unit,
) {
    val settings = LocalAppSettings.current
    // ── Nhóm variants theo màu ────────────────────────────────────────────
    val colorGroups = remember(product.variants) {
        product.variants.groupBy { it.color }
    }
    val colorList = remember(colorGroups) { colorGroups.keys.toList() }

    var selectedColor   by remember { mutableStateOf(colorList.firstOrNull() ?: "") }
    var selectedVariant by remember {
        mutableStateOf(colorGroups[colorList.firstOrNull()]?.firstOrNull())
    }
    var quantity        by remember { mutableIntStateOf(1) }

    // Reset khi đổi màu
    LaunchedEffect(selectedColor) {
        selectedVariant = (colorGroups[selectedColor] ?: emptyList()).firstOrNull()
        quantity = 1
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = MaterialTheme.colorScheme.surface,
        dragHandle       = { BottomSheetDefaults.DragHandle() },
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Thumbnail + giá + tag hiện tại
            SummaryRow(product = product, selectedVariant = selectedVariant)

            Spacer(Modifier.height(24.dp))

            // Color Options — chỉ hiện khi có > 1 màu
            if (colorList.size > 1) {
                SectionLabel(settings.t("Color Options", "Tùy chọn màu sắc"))
                Spacer(Modifier.height(12.dp))
                ColorOptionsRow(
                    product       = product,
                    colorList     = colorList,
                    selectedColor = selectedColor,
                    onSelect      = { selectedColor = it }
                )
                Spacer(Modifier.height(24.dp))
            }

            // Size
            SectionLabel(settings.t("Size", "Kích cỡ"))
            Spacer(Modifier.height(12.dp))
            SizeRow(
                sizes           = colorGroups[selectedColor] ?: emptyList(),
                selectedVariant = selectedVariant,
                onSelect        = {
                    selectedVariant = it
                    quantity = 1
                }
            )

            Spacer(Modifier.height(24.dp))

            // Quantity
            QuantityRow(
                quantity   = quantity,
                maxStock   = selectedVariant?.stock ?: 0,
                onDecrease = { if (quantity > 1) quantity-- },
                onIncrease = { if (quantity < (selectedVariant?.stock ?: 0)) quantity++ }
            )

            Spacer(Modifier.height(28.dp))

            // CTA
            Button(
                onClick  = {
                    selectedVariant?.let { onConfirm(it, quantity) }
                    onDismiss()
                },
                enabled  = selectedVariant != null && (selectedVariant?.stock ?: 0) > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape  = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor         = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text       = if (isBuyNow) settings.t("Buy Now", "Mua ngay") else settings.t("Add to cart", "Thêm vào giỏ hàng"),
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 16.sp,
                    color      = if (selectedVariant != null && (selectedVariant?.stock ?: 0) > 0)
                        MaterialTheme.colorScheme.onPrimary else AppTheme.colors.textSecondary
                )
            }

            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SUMMARY ROW: ảnh vuông + giá + chips màu/size + badge tồn kho
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SummaryRow(product: Product, selectedVariant: ProductVariant?) {
    val settings = LocalAppSettings.current
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFEEEEEE))
        ) {
            AsyncImage(
                model              = product.imageUrl.ifBlank { null },
                contentDescription = null,
                modifier           = Modifier.fillMaxSize(),
                contentScale       = ContentScale.Crop,
                error              = painterResource(R.drawable.ic_shopping)
            )
        }

        Spacer(Modifier.width(16.dp))

        Column {
            // Giá
            Text(
                "₫${formatPrice(product.price)}",
                fontWeight = FontWeight.ExtraBold,
                fontSize   = 26.sp,
                color      = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(6.dp))

            // Chips màu + size
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                selectedVariant?.color?.takeIf { it.isNotBlank() }?.let { MiniChip(it) }
                selectedVariant?.size?.takeIf { it.isNotBlank() }?.let  { MiniChip(it) }
            }

            // Tồn kho
            Spacer(Modifier.height(5.dp))
            val stock = selectedVariant?.stock ?: 0
            Text(
                text  = when {
                    selectedVariant == null -> settings.t("Select variation", "Chọn phân loại")
                    stock <= 0  -> settings.t("Out of stock", "Hết hàng")
                    stock <= 5  -> settings.t("Only $stock items left", "Còn $stock sản phẩm (sắp hết)")
                    stock <= 20 -> settings.t("$stock items left", "Còn $stock sản phẩm")
                    else        -> settings.t("In stock ($stock)", "Còn hàng ($stock)")
                },
                fontSize = 11.sp,
                color    = when {
                    selectedVariant == null -> AppTheme.colors.textSecondary
                    stock <= 0  -> RedStock
                    stock <= 5  -> OrangeStock
                    else        -> AppTheme.colors.success
                }
            )
        }
    }
}

@Composable
private fun MiniChip(text: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Text(
            text       = text,
            fontSize   = 13.sp,
            fontWeight = FontWeight.Medium,
            color      = MaterialTheme.colorScheme.onSurface,
            modifier   = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  COLOR OPTIONS: ảnh sản phẩm cho từng màu + tick khi chọn
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ColorOptionsRow(
    product: Product,
    colorList: List<String>,
    selectedColor: String,
    onSelect: (String) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(colorList) { color ->
            val isSelected = color == selectedColor
            val idx        = colorList.indexOf(color)
            // Mỗi màu 1 ảnh theo index — fallback về ảnh đầu tiên
            val imgUrl     = product.imageUrls.getOrElse(idx) { product.imageUrl }

            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = if (isSelected) 2.5.dp else 0.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onSelect(color) }
            ) {
                AsyncImage(
                    model              = imgUrl.ifBlank { null },
                    contentDescription = color,
                    modifier           = Modifier.fillMaxSize(),
                    contentScale       = ContentScale.Crop,
                    error              = painterResource(R.drawable.ic_shopping)
                )
                // Tick xanh góc dưới trái khi được chọn
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .padding(5.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary)
                            .align(Alignment.BottomStart),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint     = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SIZE ROW: chip size, out-of-stock mờ xám
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SizeRow(
    sizes: List<ProductVariant>,
    selectedVariant: ProductVariant?,
    onSelect: (ProductVariant) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(sizes) { v ->
            val isSelected = v.id == selectedVariant?.id
            val outOfStock = v.stock <= 0

            Box(
                modifier = Modifier
                    .height(42.dp)
                    .widthIn(min = 54.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        when {
                            isSelected   -> MaterialTheme.colorScheme.surface
                            outOfStock   -> MaterialTheme.colorScheme.surfaceVariant
                            else         -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                    .border(
                        width = if (isSelected) 1.5.dp else 0.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable(enabled = !outOfStock) { onSelect(v) }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = v.size,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize   = 14.sp,
                    color      = when {
                        isSelected  -> MaterialTheme.colorScheme.secondary
                        outOfStock  -> AppTheme.colors.textSecondary
                        else        -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  QUANTITY ROW: − [n] +
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuantityRow(
    quantity: Int,
    maxStock: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    val settings = LocalAppSettings.current
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            settings.t("Quantity", "Số lượng"),
            fontWeight = FontWeight.Bold,
            fontSize   = 16.sp,
            modifier   = Modifier.weight(1f),
            color      = MaterialTheme.colorScheme.onSurface
        )

        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Nút −
            CircleBtn(enabled = quantity > 1, onClick = onDecrease) {
                Icon(
                    Icons.Default.Remove, null,
                    tint     = if (quantity > 1) MaterialTheme.colorScheme.secondary else AppTheme.colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Số lượng
            Box(
                modifier = Modifier
                    .size(56.dp, 44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$quantity",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 18.sp,
                    color      = MaterialTheme.colorScheme.onSurface
                )
            }

            // Nút +
            CircleBtn(enabled = quantity < maxStock && maxStock > 0, onClick = onIncrease) {
                Icon(
                    Icons.Default.Add, null,
                    tint     = if (quantity < maxStock && maxStock > 0) MaterialTheme.colorScheme.secondary else AppTheme.colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun CircleBtn(
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .border(
                width = 1.5.dp,
                color = if (enabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape
            )
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
        content          = content
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  HELPERS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
}

private fun formatPrice(price: Double): String =
    "%.0f".format(price).reversed().chunked(3).joinToString(".").reversed()
