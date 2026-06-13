package com.example.fashionapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FeedPostSkeleton() {
    Column {
        // Header: avatar + tên
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShimmerBox(
                modifier = Modifier.size(38.dp),
                shape    = CircleShape
            )
            Spacer(Modifier.width(10.dp))
            ShimmerBox(
                modifier = Modifier
                    .width(120.dp)
                    .height(12.dp),
                shape    = RoundedCornerShape(4.dp)
            )
        }

        // Ảnh chính (vuông)
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            shape    = RoundedCornerShape(0.dp)
        )

        // Hàng icon like / comment / save
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ShimmerBox(Modifier.size(22.dp), CircleShape)
            ShimmerBox(Modifier.size(22.dp), CircleShape)
            Spacer(Modifier.weight(1f))
            ShimmerBox(Modifier.size(22.dp), CircleShape)
        }

        // Số like
        ShimmerBox(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .width(70.dp)
                .height(12.dp),
            shape    = RoundedCornerShape(4.dp)
        )
        Spacer(Modifier.height(6.dp))

        // Caption 2 dòng
        ShimmerBox(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .fillMaxWidth(0.9f)
                .height(12.dp),
            shape    = RoundedCornerShape(4.dp)
        )
        Spacer(Modifier.height(4.dp))
        ShimmerBox(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .fillMaxWidth(0.6f)
                .height(12.dp),
            shape    = RoundedCornerShape(4.dp)
        )
        Spacer(Modifier.height(12.dp))
    }
}
