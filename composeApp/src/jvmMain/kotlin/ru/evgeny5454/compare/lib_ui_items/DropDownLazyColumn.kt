package ru.evgeny5454.compare.lib_ui_items

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuBoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Suppress("FrequentlyChangingValue")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun <T> DropDownLazyColumn(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onDismissRequest: () -> Unit,
    searchItem: @Composable ExposedDropdownMenuBoxScope.(onUserTyping: () -> Unit) -> Unit,
    properties: PopupProperties = PopupProperties(),
    typingDelay: Long = 150,
    parentHeightPx: Int = 0,
    list: List<T>,
    menu: @Composable (List<T>) -> Unit
) {
    var searchItemCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var searchItemSize by remember { mutableStateOf(IntSize(0, 0)) }
    val density = LocalDensity.current

    val imeInsets = WindowInsets.ime
    val imeBottomDp = with(density) { imeInsets.getBottom(density).toDp() }


    val scope = rememberCoroutineScope()
    var typingJob: Job? by remember { mutableStateOf(null) }
    var isTyping by remember { mutableStateOf(false) }

    val onUserTyping: () -> Unit = {
        typingJob?.cancel()
        typingJob = scope.launch {
            isTyping = true
            delay(typingDelay)
            isTyping = false
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .onSizeChanged { searchItemSize = it }
                .onGloballyPositioned { coordinates -> searchItemCoordinates = coordinates }
        ) {
            this@ExposedDropdownMenuBox.searchItem(onUserTyping)
        }

        var popupAvailableHeight by remember { mutableStateOf(0.dp) }

        val popupPositionProvider = derivedStateOf {
            if (searchItemCoordinates == null) {
                return@derivedStateOf object : PopupPositionProvider {
                    override fun calculatePosition(
                        anchorBounds: IntRect,
                        windowSize: IntSize,
                        layoutDirection: LayoutDirection,
                        popupContentSize: IntSize
                    ) = IntOffset.Zero
                }
            }

            val coordinates = searchItemCoordinates!!
            val anchorBounds = coordinates.boundsInWindow()
            val anchorX = anchorBounds.left.toInt()
            val anchorYBottom = anchorBounds.bottom.toInt()



            object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize
                ): IntOffset {
                    val actualWindowHeight =
                        if (parentHeightPx > 0) parentHeightPx else windowSize.height

                    val availableHeightBelow = actualWindowHeight - anchorYBottom - imeHeightPx(
                        density,
                        imeBottomDp
                    ).toInt()
                    val availableHeightAbove =
                        anchorBounds.top - 16 /*- 200// небольшой отступ сверху*/

                    // Выбираем максимальную высоту для popup
                    popupAvailableHeight = with(density) {
                        maxOf(availableHeightBelow, availableHeightAbove).toDp()
                    }

                    // Решаем, открывать вниз или вверх
                    val showBelow =
                        availableHeightBelow >= popupContentSize.height || availableHeightBelow >= availableHeightAbove
                    val popupY =
                        if (showBelow) anchorBounds.bottom.toInt() else (anchorBounds.top - popupContentSize.height).toInt()

                    return IntOffset(anchorX, popupY)
                }
            }
        }.value


        var dismissJob: Job? = null

        if (expanded) {
            Popup(
                onDismissRequest = {
                    dismissJob?.cancel()
                    dismissJob = scope.launch {
                        delay(typingDelay)
                        if (!isTyping) {
                            onDismissRequest()
                        }
                    }
                },
                popupPositionProvider = popupPositionProvider,
                properties = properties
            ) {
                ElevatedCard(
                    modifier = Modifier
                        .width(with(density) { searchItemSize.width.toDp() })
                        .heightIn(max = popupAvailableHeight),
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.elevatedCardColors().copy(
                        containerColor = MaterialTheme.colors.background
                    ),
                    elevation = CardDefaults.elevatedCardElevation(
                        defaultElevation = 16.dp,
                        pressedElevation = 16.dp,
                        focusedElevation = 16.dp,
                        hoveredElevation = 16.dp,
                        draggedElevation = 16.dp,
                        disabledElevation = 16.dp
                    )
                ) {
                    menu(list)
                }
            }
        }
    }
}

private fun imeHeightPx(density: Density, imeBottomDp: Dp): Float = 0f