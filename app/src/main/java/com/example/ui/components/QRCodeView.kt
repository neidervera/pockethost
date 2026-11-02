package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CyanNeon
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun QRCodeView(
    content: String,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 200.dp
) {
    var qrBitmap by remember(content) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(content) {
        if (content.isNotEmpty()) {
            withContext(Dispatchers.Default) {
                try {
                    val hints = mapOf(
                        EncodeHintType.MARGIN to 1,
                        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
                    )
                    val writer = QRCodeWriter()
                    val matrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512, hints)
                    val width = matrix.width
                    val height = matrix.height
                    val pixels = IntArray(width * height)
                    for (y in 0 until height) {
                        for (x in 0 until width) {
                            pixels[y * width + x] = if (matrix.get(x, y)) {
                                android.graphics.Color.BLACK
                            } else {
                                android.graphics.Color.WHITE
                            }
                        }
                    }
                    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bmp.setPixels(pixels, 0, width, 0, 0, width, height)
                    qrBitmap = bmp
                } catch (e: Exception) {
                    qrBitmap = null
                }
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(2.dp, CyanNeon.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(12.dp)
            .testTag("qr_code_container"),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = qrBitmap
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Código QR para conectar al servidor: $content",
                modifier = Modifier
                    .size(sizeDp)
                    .testTag("qr_code_image")
            )
        } else {
            Box(
                modifier = Modifier.size(sizeDp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = CyanNeon,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}
