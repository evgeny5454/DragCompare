package ru.evgeny5454.compare.lib_ui_items

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.collections.set
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun VerticalScrollIndicator(
    modifier: Modifier = Modifier,
    listState: LazyListState,
    minIndicatorHeight: Dp = 56.dp,
    trackWidth: Dp = 16.dp,
    lazyColumn: @Composable () -> Unit,

) {
    val density = LocalDensity.current
    val minIndicatorHeightPx = with(density) { minIndicatorHeight.toPx() }
    val itemHeights = remember { mutableStateMapOf<Int, Int>() }
    val coroutineScope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }

    var listHeightPx by remember { mutableStateOf(0) }

    BoxWithConstraints(
        modifier = modifier.wrapContentSize()
//            .border(1.dp, Color.Red)
    ) {
        Box(
            modifier = Modifier
                .onSizeChanged { listHeightPx = it.height }
        ) {
            lazyColumn()
        }

        Box(
            modifier = Modifier
                .width(trackWidth)
                .height(with(density) { listHeightPx.toDp() })
                .align(Alignment.CenterEnd)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .width(trackWidth)
                    .fillMaxHeight()
//                    .border(1.dp, Color.Green)
            ) {
                val layoutInfo = listState.layoutInfo
                val visibleItems = layoutInfo.visibleItemsInfo
                val totalItems = layoutInfo.totalItemsCount

                if (totalItems == 0 || visibleItems.isEmpty()) return@BoxWithConstraints

                // обновляем известные высоты элементов
                LaunchedEffect(visibleItems) {
                    visibleItems.forEach { item ->
                        val prev = itemHeights[item.index]
                        if (prev == null || prev != item.size) {
                            itemHeights[item.index] = item.size
                        }
                    }
                }

                LaunchedEffect(totalItems) {
                    val avg = itemHeights.values.average().toFloat().takeIf { it > 0 } ?: 100f
                    for (i in 0 until totalItems) if (!itemHeights.containsKey(i)) itemHeights[i] =
                        avg.toInt()
                    val obsolete = itemHeights.keys.filter { it >= totalItems }
                    obsolete.forEach { itemHeights.remove(it) }
                }

                val averageHeight = remember(itemHeights) {
                    if (itemHeights.isNotEmpty()) itemHeights.values.average().toFloat() else 100f
                }

                val totalHeightPx = itemHeights.values.take(totalItems).sum().toFloat()
                val viewportHeightPx = layoutInfo.viewportSize.height.toFloat()

                val scrollPx = visibleItems.firstOrNull()?.let { first ->
                    val before =
                        (0 until first.index).sumOf { (itemHeights[it] ?: averageHeight.toInt()) }
                            .toFloat()
                    val inside = -first.offset.toFloat()
                    (before + inside).coerceAtLeast(0f)
                } ?: 0f

                val lastItemHeight =
                    itemHeights[totalItems - 1]?.toFloat() ?: averageHeight
                val maxScrollPx = (totalHeightPx - viewportHeightPx + lastItemHeight)

                val scrollFraction = when {
                    maxScrollPx <= 0f -> 0f
                    else -> (scrollPx / maxScrollPx)
                        .takeIf { it.isFinite() }
                        ?.coerceIn(0f, 1f)
                        ?: 0f
                }

                val trackHeightPx = with(density) { maxHeight.toPx() }

                val hasScroll = viewportHeightPx < totalHeightPx
                val indicatorHeightPx = ((viewportHeightPx / totalHeightPx * trackHeightPx)
                    .takeIf { it.isFinite() } ?: trackHeightPx)
                    .coerceAtLeast(minIndicatorHeightPx)

                val maxScrollbarTopPx = (trackHeightPx - indicatorHeightPx).coerceAtLeast(0f)
                val offsetY =
                    (scrollFraction * maxScrollbarTopPx).coerceIn(0f, maxScrollbarTopPx).roundToInt()


                val isPressed by interactionSource.collectIsPressedAsState()
                val isDragged by interactionSource.collectIsDraggedAsState()
                val isHover by interactionSource.collectIsHoveredAsState()

                val indicatorWidth by animateDpAsState(
                    targetValue = if (isPressed || isDragged || isHover) 8.dp else 6.dp,
                    animationSpec = tween(durationMillis = 300),
                    label = "scrollIndicatorWidth"
                )

                val color by animateColorAsState(
                    targetValue = if (isPressed || isDragged || isHover)
                        MaterialTheme.colors.primary.copy(alpha = 1f)
                    else
                        MaterialTheme.colors.primary.copy(alpha = 0.3f),
                    animationSpec = tween(300),
                    label = "indicatorColor"
                )

                val indicatorHeight by animateDpAsState(
                    targetValue = with(density) { indicatorHeightPx.toDp() },
                    animationSpec = tween(300)
                )

                val dragModifier = Modifier
                    .draggable(
                        orientation = Orientation.Vertical,
                        interactionSource = interactionSource,
                        state = rememberDraggableState { delta ->
                            if (!hasScroll) return@rememberDraggableState
                            val newY = (offsetY + delta).coerceIn(0f, maxScrollbarTopPx)
                            val newFraction = (newY / maxScrollbarTopPx).coerceIn(0f, 1f)

                            val targetScrollPx = newFraction * maxScrollPx
                            val deltaPx = targetScrollPx - scrollPx

                            val dragThresholdPx = 500f
                            coroutineScope.launch {
                                if (abs(deltaPx) > dragThresholdPx) {
                                    val itemIndex = ((newFraction * (totalItems - 1)).roundToInt())
                                        .coerceIn(0, totalItems - 1)
                                    listState.scrollToItem(itemIndex)
                                } else {
                                    listState.scrollBy(deltaPx)
                                }
                            }
                        }
                    ).clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = hasScroll,
                        onClick = {}
                    )

                val visibleAlpha by animateFloatAsState(
                    targetValue = if (hasScroll) 1f else 0f,
                    animationSpec = tween(400),
                    label = "scrollbarAlpha"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(0, offsetY) }
                        .height(indicatorHeight)
                        .graphicsLayer { alpha = visibleAlpha }
                        .then(dragModifier),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .width(indicatorWidth)
                            .fillMaxHeight()
                            .background(color, CircleShape)
                    )
                }
            }

        }
    }
}