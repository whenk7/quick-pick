// 悬浮玻璃底栏。移植自 miuix 官方 example 的 IosLiquidGlassNavigationBar（Apache-2.0），简化点：
// - Tab 数据源换成本项目的 BottomTab（emoji + 文案），去掉 Badge/键盘导航/iOS 平台分支
// - 距底固定 12dp + 手势条 inset（spec §8.4），不再按平台区分
// 玻璃强度四旋钮由设置页调节（GlassTuning），基线：blur 4dp、lens 24dp、容器 40% 不透明。
package com.quickpick.ui.component.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.quickpick.ui.component.BottomTab
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * 悬浮底栏动态占位高度（栏 64 + 距底 12 + 导航条 inset 动态读），
 * 列表底部 contentPadding 用它留白。三键导航 inset ≈48dp，静态估算会确定性遮挡末项。
 */
@Composable
fun glassNavBarClearance(): Dp =
    76.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

/** 栏内容内边距（spec §8.4：内边距 4dp）；双排/指示器的 8dp 扣减 = BarInnerPadding * 2 */
private val BarInnerPadding = 4.dp

private val LocalTabPressScale = staticCompositionLocalOf { { 1f } }

/** 玻璃底栏强度旋钮（全部 0..1，设置页滑杆直写；映射见函数内注释） */
data class GlassTuning(
    val blur: Float = 0.6f,       // → 模糊半径 2..12dp
    val lens: Float = 0.55f,      // → 折射 12..36dp（选中 pill 的按压折射按比例跟随）
    val highlight: Float = 0.85f, // → 高光 alpha 0.4..1.0
    val clarity: Float = 0.8f,    // → 容器不透明度 0.6..0.25（越高越透）
)

@Composable
fun LiquidGlassNavigationBar(
    currentTab: BottomTab,
    onTabClick: (BottomTab) -> Unit,
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    tuning: GlassTuning = GlassTuning(),
    // 长按 tab（v1.1.7 §11 沉浸模式入口）：按下不动触发，按下即动归 DampedDrag 正常切换
    onTabLongClick: ((BottomTab) -> Unit)? = null,
) {
    val items = BottomTab.entries
    val selectedIndex = currentTab.ordinal
    // minSdk 33 以上 runtime shader 恒可用；isRuntimeShaderSupported 兜底降级为实体底
    val isBlurActive = isRuntimeShaderSupported()

    val isDark = isAppInDarkTheme()
    val pillShape = remember { CircleShape }
    val accentColor = MiuixTheme.colorScheme.primary
    val tabContentColor = MiuixTheme.colorScheme.onSurface
    val surfaceContainer = MiuixTheme.colorScheme.surfaceContainer
    val containerAlpha = lerp(0.6f, 0.25f, tuning.clarity)
    val containerColor = if (isBlurActive) surfaceContainer.copy(alpha = containerAlpha) else surfaceContainer
    val blurRadiusDp = lerp(2f, 12f, tuning.blur)
    val lensDp = lerp(12f, 36f, tuning.lens)
    val highlightAlpha = lerp(0.4f, 1f, tuning.highlight)

    val tabsBackdrop = rememberLayerBackdrop()
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()
    val tabsCount = items.size

    var tabWidthPx by remember { mutableFloatStateOf(0f) }
    var totalWidthPx by remember { mutableFloatStateOf(0f) }

    val offsetAnimation = remember { Animatable(0f) }
    val rubberBandPx = with(density) { 4.dp.toPx() }
    val panelOffset by remember(rubberBandPx) {
        derivedStateOf {
            if (totalWidthPx == 0f) {
                0f
            } else {
                val fraction = (offsetAnimation.value / totalWidthPx).coerceIn(-1f, 1f)
                rubberBandPx * fraction.sign * EaseOut.transform(abs(fraction))
            }
        }
    }

    var currentIndex by remember { mutableIntStateOf(selectedIndex) }
    val onItemClickUpdated by rememberUpdatedState(onTabClick)

    fun indexAt(positionX: Float): Int {
        if (tabWidthPx == 0f) return currentIndex
        val horizontalPaddingPx = with(density) { BarInnerPadding.toPx() }
        val logicalX = if (isLtr) positionX else totalWidthPx - positionX
        return ((logicalX - horizontalPaddingPx) / tabWidthPx)
            .toInt()
            .coerceIn(0, tabsCount - 1)
    }

    // 长按检测（v1.1.7 §11）：挂在 DampedDrag 的回调钩子上，零事件侵入——
    // 自研 pointerInput 方案在 Initial pass 旁观也会掐死 DampedDrag 点击（压测实证）。
    // 按下起 longPressTimeout 计时；移动（onDrag）或抬起（onDragStopped/Cancelled）取消
    val longPressTimeout = androidx.compose.ui.platform.LocalViewConfiguration.current.longPressTimeoutMillis
    var longPressJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    // remembered DampedDragAnimation 的 key 不含本回调——rememberUpdatedState 防捕获旧 lambda
    val onTabLongClickUpdated by rememberUpdatedState(onTabLongClick)

    val dampedDrag = remember(animationScope, tabsCount, density, isLtr) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = selectedIndex.toFloat(),
            valueRange = 0f..(tabsCount - 1).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 78f / 56f,
            canDrag = { position ->
                position.x in 0f..totalWidthPx
            },
            onDragStarted = { position ->
                updateValue(indexAt(position.x).toFloat())
                longPressJob?.cancel()
                longPressJob = animationScope.launch {
                    kotlinx.coroutines.delay(longPressTimeout)
                    onTabLongClickUpdated?.invoke(items[indexAt(position.x)])
                }
            },
            onDragStopped = {
                longPressJob?.cancel()
                val targetIndex = targetValue.roundToInt().coerceIn(0, tabsCount - 1)
                if (currentIndex != targetIndex) {
                    currentIndex = targetIndex
                    onItemClickUpdated(items[targetIndex])
                }
                updateValue(targetIndex.toFloat())
                animationScope.launch {
                    offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                }
            },
            onDragCancelled = {
                longPressJob?.cancel()
                updateValue(currentIndex.toFloat())
                animationScope.launch {
                    offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                }
            },
            onDrag = { _, dragAmount ->
                // 注意：inspectDragGestures 按下后立即回调一次 onDrag(Offset.Zero)，
                // 无条件取消会把刚启动的长按计时器掐死——只在真实拖动时取消
                if (dragAmount != androidx.compose.ui.geometry.Offset.Zero) longPressJob?.cancel()
                if (tabWidthPx > 0f && dragAmount.x != 0f) {
                    updateValue(
                        (targetValue + dragAmount.x / tabWidthPx * if (isLtr) 1f else -1f)
                            .coerceIn(0f, (tabsCount - 1).toFloat()),
                    )
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                }
            },
        )
    }

    LaunchedEffect(selectedIndex) {
        if (currentIndex != selectedIndex) {
            currentIndex = selectedIndex
            dampedDrag.animateToValue(selectedIndex.toFloat())
        }
    }

    fun activateTab(index: Int) {
        if (currentIndex != index) {
            currentIndex = index
            onItemClickUpdated(items[index])
        }
        dampedDrag.animateToValue(index.toFloat())
    }

    // keyed on dampedDrag：position lambda 捕获它，过期捕获会冻住按压点
    val interactiveHighlight = remember(animationScope, isLtr, dampedDrag) {
        InteractiveHighlight(
            animationScope = animationScope,
            position = { layerSize, _ ->
                Offset(
                    x = if (isLtr) {
                        (dampedDrag.value + 0.5f) * tabWidthPx + panelOffset
                    } else {
                        layerSize.width - (dampedDrag.value + 0.5f) * tabWidthPx + panelOffset
                    },
                    y = layerSize.height / 2f,
                )
            },
        )
    }

    // .value 只在 highlight lambda（绘制阶段）里读，不在组合期读
    val baseHighlight = rememberGravityRotatedHighlight(GlassSpecularHighlight, extraDegrees = -45f)
    val pillHighlight = rememberGravityRotatedHighlight(GlassSpecularHighlight, extraDegrees = 90f)

    val combinedBackdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop)

    // spec §8.4：距底 12dp + 手势条 inset
    val navBarBottomPadding = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding()
    val bottomPaddingValue = 12.dp + navBarBottomPadding

    val tabsContent: @Composable RowScope.() -> Unit = {
        val tabScale = LocalTabPressScale.current
        items.forEachIndexed { index, tab ->
            Column(
                modifier = Modifier
                    .semantics(mergeDescendants = true) {
                        selected = index == currentIndex
                        role = Role.Tab
                        onClick {
                            activateTab(index)
                            true
                        }
                    }
                    .weight(1f)
                    .fillMaxHeight()
                    .graphicsLayer {
                        val s = tabScale()
                        scaleX = s
                        scaleY = s
                    },
                verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
                horizontalAlignment = CenterHorizontally,
            ) {
                Text(text = tab.icon, fontSize = 20.sp)
                Text(
                    text = tab.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .padding(bottom = bottomPaddingValue, start = 24.dp, end = 24.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.CenterStart,
        ) {
            CompositionLocalProvider(LocalContentColor provides tabContentColor) {
                Row(
                    modifier = Modifier
                        .selectableGroup()
                        .onSizeChanged { coords ->
                            totalWidthPx = coords.width.toFloat()
                            val contentWidthPx = totalWidthPx - with(density) { (BarInnerPadding * 2).toPx() }
                            tabWidthPx = (contentWidthPx / tabsCount).coerceAtLeast(0f)
                        }
                        .graphicsLayer { translationX = panelOffset }
                        .dropShadow(
                            shape = pillShape,
                            shadow = Shadow(
                                radius = 10.dp,
                                color = Color.Black,
                                // 浅色主题用更淡的阴影，避免灰边
                                alpha = if (isDark) 0.2f else 0.1f,
                            ),
                        )
                        .then(
                            if (isBlurActive) {
                                Modifier.drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { pillShape },
                                    effects = {
                                        // 折射 + 16dp 按压放大余量，需在 blur() 读取前抬高
                                        padding = maxOf(padding, 40.dp.toPx())
                                        vibrancy()
                                        blur(
                                            blurRadiusDp.dp.toPx(),
                                            blurRadiusDp.dp.toPx(),
                                        )
                                        lens(
                                            refractionHeight = lensDp.dp.toPx(),
                                            refractionAmount = lensDp.dp.toPx(),
                                        )
                                    },
                                    highlight = { baseHighlight.value.copy(alpha = highlightAlpha) },
                                    layerBlock = {
                                        val width = size.width.coerceAtLeast(1f)
                                        val s = lerp(1f, 1f + 16.dp.toPx() / width, dampedDrag.pressProgress)
                                        scaleX = s
                                        scaleY = s
                                    },
                                    onDrawSurface = { drawRect(containerColor) },
                                )
                            } else {
                                Modifier
                                    .background(containerColor, pillShape)
                            },
                        )
                        .then(
                            if (isBlurActive) {
                                interactiveHighlight.modifier.then(interactiveHighlight.gestureModifier)
                            } else {
                                Modifier
                            },
                        )
                        .then(dampedDrag.modifier)
                        .height(64.dp)
                        .padding(BarInnerPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    content = tabsContent,
                )
            }

            if (isBlurActive) {
                CompositionLocalProvider(
                    LocalTabPressScale provides { lerp(1f, 1.2f, dampedDrag.pressProgress) },
                    LocalContentColor provides accentColor,
                ) {
                    Row(
                        modifier = Modifier
                            .clearAndSetSemantics {}
                            .alpha(0f)
                            .layerBackdrop(tabsBackdrop)
                            .graphicsLayer { translationX = panelOffset }
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { pillShape },
                                effects = {
                                    vibrancy()
                                    blur(blurRadiusDp.dp.toPx(), blurRadiusDp.dp.toPx())
                                    lens(
                                        refractionHeight = lensDp.dp.toPx(),
                                        refractionAmount = lensDp.dp.toPx(),
                                    )
                                },
                                onDrawSurface = { drawRect(containerColor) },
                            )
                            .then(interactiveHighlight.modifier)
                            .height(56.dp)
                            .padding(horizontal = BarInnerPadding),
                        verticalAlignment = Alignment.CenterVertically,
                        content = tabsContent,
                    )
                }
            }

            if (tabWidthPx > 0f) {
                val tabWidthDp = with(density) { tabWidthPx.toDp() }
                if (isBlurActive) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = BarInnerPadding)
                            .graphicsLayer {
                                val singleTabWidth = tabWidthPx
                                val progressOffset = dampedDrag.value * singleTabWidth
                                translationX = if (isLtr) progressOffset + panelOffset else -progressOffset + panelOffset
                            }
                            .drawBackdrop(
                                backdrop = combinedBackdrop,
                                shape = { pillShape },
                                effects = {
                                    val progress = dampedDrag.pressProgress
                                    // 按压折射按主折射强度等比缩放（基线 24dp → 10/14dp）
                                    val k = lensDp / 24f
                                    lens(
                                        refractionHeight = 10.dp.toPx() * k * progress,
                                        refractionAmount = 14.dp.toPx() * k * progress,
                                        depthEffect = true,
                                        chromaticAberration = 0.5f,
                                    )
                                },
                                highlight = { pillHighlight.value.copy(alpha = dampedDrag.pressProgress * highlightAlpha) },
                                layerBlock = {
                                    scaleX = dampedDrag.scaleX
                                    scaleY = dampedDrag.scaleY
                                    val v = dampedDrag.velocity / 10f
                                    scaleX /= 1f - (v * 0.75f).coerceIn(-0.2f, 0.2f)
                                    scaleY *= 1f - (v * 0.25f).coerceIn(-0.2f, 0.2f)
                                },
                                onDrawSurface = {
                                    val progress = dampedDrag.pressProgress
                                    drawRect(
                                        color = if (!isDark) Color.Black.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.1f),
                                        alpha = 1f - progress,
                                    )
                                    drawRect(Color.Black.copy(alpha = 0.03f * progress))
                                },
                            )
                            .innerShadow(shape = pillShape) {
                                InnerShadow(
                                    radius = 8.dp * dampedDrag.pressProgress,
                                    color = Color.Black.copy(alpha = 0.15f),
                                    alpha = dampedDrag.pressProgress,
                                )
                            }
                            .height(56.dp)
                            .width(tabWidthDp),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = BarInnerPadding)
                            .graphicsLayer {
                                val progressOffset = dampedDrag.value * tabWidthPx
                                translationX = if (isLtr) progressOffset + panelOffset else -progressOffset + panelOffset
                            }
                            .clip(pillShape)
                            // 选中态 pill：主题色 12%（example 原值 15%，按设计收；降级路径用，blur 可用时不可达）
                            .background(accentColor.copy(alpha = 0.12f), pillShape)
                            .height(56.dp)
                            .width(tabWidthDp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        CompositionLocalProvider(LocalContentColor provides accentColor) {
                            Row(
                                modifier = Modifier
                                    .clearAndSetSemantics {}
                                    .wrapContentWidth(align = Alignment.Start, unbounded = true)
                                    .requiredWidth(with(density) { (totalWidthPx - (BarInnerPadding * 2).toPx()).toDp() })
                                    .height(56.dp)
                                    .graphicsLayer {
                                        val progressOffset = dampedDrag.value * tabWidthPx
                                        translationX = if (isLtr) -progressOffset else progressOffset
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                content = tabsContent,
                            )
                        }
                    }
                }
            }
        }
    }
}
