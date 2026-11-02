package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.StatusOffline
import com.example.ui.theme.TextPrimary

@Composable
fun StatusIndicator(
    isRunning: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val badgeBg = if (isRunning) EmeraldActive.copy(alpha = 0.12f) else Color(0xFF1E283D)
    val borderColor = if (isRunning) EmeraldActive.copy(alpha = 0.5f) else Color(0xFF32405D)
    val dotColor = if (isRunning) EmeraldActive else StatusOffline

    Row(
        modifier = modifier
            .background(badgeBg, RoundedCornerShape(999.dp))
            .border(1.dp, borderColor, RoundedCornerShape(999.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("server_status_badge"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isRunning) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .scale(pulseScale)
                        .background(EmeraldActive.copy(alpha = pulseAlpha), CircleShape)
                )
            }
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(dotColor, CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = if (isRunning) "SERVIDOR ACTIVO" else "SERVIDOR DETENIDO",
            color = if (isRunning) EmeraldActive else Color(0xFF94A3B8),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.sp
        )
    }
}
