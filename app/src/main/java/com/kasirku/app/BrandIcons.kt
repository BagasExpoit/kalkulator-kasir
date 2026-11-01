package com.kasirku.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FacebookIcon(iconSize: Dp = 44.dp) {
    Box(
        Modifier.size(iconSize).clip(CircleShape).background(Color(0xFF1877F2)),
        contentAlignment = Alignment.Center
    ) {
        Text("f", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = (iconSize.value * 0.62f).sp)
    }
}

@Composable
fun InstagramIcon(iconSize: Dp = 44.dp) {
    val brush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFEDA75), Color(0xFFFA7E1E), Color(0xFFD62976), Color(0xFF962FBF), Color(0xFF4F5BD5)
        ),
        start = Offset(0f, Float.POSITIVE_INFINITY),
        end = Offset(Float.POSITIVE_INFINITY, 0f)
    )
    Canvas(Modifier.size(iconSize)) {
        val s = this.size.minDimension
        val sw = s * 0.075f
        drawRoundRect(brush, cornerRadius = CornerRadius(s * 0.28f))
        drawRoundRect(
            Color.White,
            topLeft = Offset(s * 0.2f, s * 0.2f),
            size = Size(s * 0.6f, s * 0.6f),
            cornerRadius = CornerRadius(s * 0.18f),
            style = Stroke(sw)
        )
        drawCircle(Color.White, radius = s * 0.14f, style = Stroke(sw))
        drawCircle(Color.White, radius = sw * 0.6f, center = Offset(s * 0.7f, s * 0.3f))
    }
}
