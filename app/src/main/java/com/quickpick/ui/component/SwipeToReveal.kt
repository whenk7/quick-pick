package com.quickpick.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 左滑露出动作（v1.1.7 §4.3）：下层右对齐动作块随卡片左移露出；
 * 松手时拖过 [thresholdFraction] 触发 [onTrigger] 并回弹，未过阈值直接回弹。
 * 只接水平手势（detectHorizontalDragGestures）：垂直滚动/斜滑由外层 LazyColumn 处理；
 * 与长按的竞争由 Compose 手势仲裁——按下不动=长按，按下即动=拖动。
 */
@Composable
fun SwipeToReveal(
    actionLabel: String,
    actionColor: Color,
    modifier: Modifier = Modifier,
    thresholdFraction: Float = 0.35f,
    onTrigger: () -> Unit,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    var widthPx by remember { mutableFloatStateOf(0f) }
    // 露出上限：卡宽 60%（露出动作块足够宽即可，不追求全拖走）
    val maxRevealPx = widthPx * 0.6f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .onSizeChanged { widthPx = it.width.toFloat() }
    ) {
        // 下层：右对齐动作块（露出多少看多少，靠上层平移揭开）
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(actionColor)
                .padding(end = 20.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = actionLabel,
                style = MiuixTheme.textStyles.body1,
                color = Color.White
            )
        }

        // 上层：内容卡，随手势左移
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = offsetX.value }
                .pointerInput(widthPx) {
                    if (widthPx == 0f) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val passed = -offsetX.value > widthPx * thresholdFraction
                            scope.launch {
                                if (passed) onTrigger()
                                offsetX.animateTo(0f, spring(dampingRatio = 0.8f))
                            }
                        },
                        onDragCancel = {
                            scope.launch { offsetX.animateTo(0f, spring(dampingRatio = 0.8f)) }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                offsetX.snapTo(
                                    (offsetX.value + dragAmount).coerceIn(-maxRevealPx, 0f)
                                )
                            }
                        }
                    )
                }
        ) {
            content()
        }
    }
}
