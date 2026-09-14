package com.dailybook.app.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/**
 * 自定义背景图。
 *
 * 不引图片加载库：直接用系统的 `BitmapFactory` 解码，并做**两遍采样**——
 * 先用 `inJustDecodeBounds` 读出尺寸，再按屏幕宽度算 `inSampleSize`。
 * 这样用户从相册挑一张 4000×3000 的照片当背景也不会因为整图解码而 OOM。
 *
 * 解码失败（URI 失权、文件被删、不是图片）一律当作「没有背景」处理，绝不让界面崩。
 */
@Composable
fun rememberBackgroundBitmap(uriString: String): ImageBitmap? {
    val context = LocalContext.current
    val targetWidth = LocalConfiguration.current.screenWidthDp * 2
    return remember(uriString, targetWidth) {
        if (uriString.isBlank()) return@remember null
        runCatching {
            val uri = Uri.parse(uriString)

            // 第一遍：只读尺寸
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

            // 第二遍：按需缩放
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= targetWidth && sample < 16) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)?.asImageBitmap()
            }
        }.getOrNull()
    }
}

/**
 * 把背景图铺在内容底下，并盖一层蒙版。
 *
 * `scrimPercent` 是蒙版浓度（0~80）：调高 → 图更淡、文字更清楚。
 * 蒙版用的是主题底色，所以浅色主题是「白纱」、深色主题是「黑纱」，两种模式下文字都稳。
 */
@Composable
fun AppBackground(
    uriString: String,
    scrimPercent: Int,
    content: @Composable () -> Unit
) {
    val bitmap = rememberBackgroundBitmap(uriString)
    if (bitmap == null) {
        content()
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    MaterialTheme.colorScheme.background.copy(
                        alpha = (scrimPercent.coerceIn(0, 80) / 100f)
                    )
                )
        )
        content()
    }
}
