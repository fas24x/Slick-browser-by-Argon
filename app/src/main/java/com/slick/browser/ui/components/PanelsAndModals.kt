package com.slick.browser.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.slick.browser.model.BookmarkItem
import com.slick.browser.model.CardExpandAnimState
import com.slick.browser.model.ConfirmDialogState
import com.slick.browser.model.ContextPillAction
import com.slick.browser.model.ContextPillIcon
import com.slick.browser.model.ContextPillState
import com.slick.browser.model.SearchEngine
import com.slick.browser.model.SlickBrowserState
import com.slick.browser.model.TabItem
import com.slick.browser.model.UiPanelState
import com.slick.browser.ui.effects.diffusedFogAura
import com.slick.browser.ui.effects.edgeFogGlow
import com.slick.browser.ui.theme.JetBrainsMonoFamily
import com.slick.browser.ui.theme.LocalSlickColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

// Вспомогательные функции копирования и шаринга
fun copyUrlToClipboard(context: Context, state: SlickBrowserState, url: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("Slick URL", url))
    state.showToast("ССЫЛКА СКОПИРОВАНА")
}

fun shareUrlIntent(context: Context, state: SlickBrowserState, title: String, url: String) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, url)
        }
        context.startActivity(Intent.createChooser(intent, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        state.showToast("ПОДЕЛИТЬСЯ: $title")
    }
}

/**
 * Векторные иконки в строгом соответствии с эталоном concept2.html
 */
@Composable
fun SlickVectorIcon(
    icon: ContextPillIcon,
    tint: Color,
    modifier: Modifier = Modifier.size(15.dp)
) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val w = size.width
        val h = size.height

        when (icon) {
            ContextPillIcon.BOOKMARK_ADD -> {
                val path = Path().apply {
                    moveTo(w * 0.2f, h * 0.12f)
                    lineTo(w * 0.8f, h * 0.12f)
                    lineTo(w * 0.8f, h * 0.88f)
                    lineTo(w * 0.5f, h * 0.66f)
                    lineTo(w * 0.2f, h * 0.88f)
                    close()
                }
                drawPath(path, color = tint, style = stroke)
            }
            ContextPillIcon.BOOKMARK_DELETE -> {
                val path = Path().apply {
                    moveTo(w * 0.2f, h * 0.12f)
                    lineTo(w * 0.8f, h * 0.12f)
                    lineTo(w * 0.8f, h * 0.88f)
                    lineTo(w * 0.5f, h * 0.66f)
                    lineTo(w * 0.2f, h * 0.88f)
                    close()
                }
                drawPath(path, color = tint, style = stroke)
                drawLine(
                    color = tint,
                    start = Offset(w * 0.12f, h * 0.12f),
                    end = Offset(w * 0.88f, h * 0.88f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            ContextPillIcon.COPY_URL -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.34f, h * 0.34f),
                    size = Size(w * 0.54f, h * 0.54f),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                    style = stroke
                )
                val backPath = Path().apply {
                    moveTo(w * 0.22f, h * 0.64f)
                    lineTo(w * 0.14f, h * 0.64f)
                    lineTo(w * 0.14f, h * 0.14f)
                    lineTo(w * 0.64f, h * 0.14f)
                    lineTo(w * 0.64f, h * 0.22f)
                }
                drawPath(backPath, color = tint, style = stroke)
            }
            ContextPillIcon.SHARE_URL -> {
                val r = w * 0.12f
                val c1 = Offset(w * 0.75f, h * 0.22f)
                val c2 = Offset(w * 0.25f, h * 0.5f)
                val c3 = Offset(w * 0.75f, h * 0.78f)
                drawCircle(color = tint, radius = r, center = c1, style = stroke)
                drawCircle(color = tint, radius = r, center = c2, style = stroke)
                drawCircle(color = tint, radius = r, center = c3, style = stroke)
                drawLine(tint, Offset(c2.x + r, c2.y - r * 0.4f), Offset(c1.x - r, c1.y + r * 0.4f), strokeWidth = 1.8.dp.toPx())
                drawLine(tint, Offset(c2.x + r, c2.y + r * 0.4f), Offset(c3.x - r, c3.y - r * 0.4f), strokeWidth = 1.8.dp.toPx())
            }
        }
    }
}

/**
 * Кнопка-переключатель в стиле concept2.html (.toggle-btn) с защитой от выхода текста за рамки
 */
@Composable
fun SlickToggleButton(
    text: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSlickColors.current
    val bg = if (active) colors.fg else colors.bg
    val fg = if (active) colors.bg else colors.fg

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(1.5.dp, colors.fg, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            color = fg,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Круглая мини-кнопка иконки (.mini-icon-btn)
 */
@Composable
fun MiniCircleIconButton(
    icon: ContextPillIcon,
    onClick: () -> Unit
) {
    val colors = LocalSlickColors.current
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(colors.bg)
            .border(1.5.dp, colors.fg, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        SlickVectorIcon(icon = icon, tint = colors.fg)
    }
}

/**
 * Всплывающая контекстная капсула из 3 круглых иконок без текста, окружённая мягким рассеянным туманом
 */
@Composable
fun FloatingContextPillOverlay(
    pillState: ContextPillState?,
    fogPhase: Float,
    isIncognito: Boolean,
    isMonoLogo: Boolean,
    powerSave: Boolean,
    onDismiss: () -> Unit
) {
    val colors = LocalSlickColors.current
    val density = LocalDensity.current
    val config = LocalConfiguration.current

    AnimatedVisibility(
        visible = pillState != null,
        enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.85f, animationSpec = spring(dampingRatio = 0.7f)),
        exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.85f, animationSpec = tween(120))
    ) {
        if (pillState != null) {
            val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
            val pillWidthPx = with(density) { 126.dp.toPx() }
            val pillHeightPx = with(density) { 46.dp.toPx() }

            val clampedX = (pillState.centerX - pillWidthPx / 2f)
                .coerceIn(16f, (screenWidthPx - pillWidthPx - 16f).coerceAtLeast(16f))
            val clampedY = (pillState.topY - pillHeightPx - 12f).coerceAtLeast(24f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown()
                            onDismiss()
                        }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .offset { IntOffset(clampedX.roundToInt(), clampedY.roundToInt()) }
                        .diffusedFogAura(
                            phase = fogPhase,
                            isIncognito = isIncognito,
                            isMonoLogo = isMonoLogo,
                            enabled = !powerSave && !isMonoLogo,
                            blurRadius = 18.dp,
                            spread = 6.dp,
                            alpha = 0.9f
                        )
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.bg)
                        .border(1.5.dp, colors.fg, RoundedCornerShape(999.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    pillState.actions.forEach { action ->
                        MiniCircleIconButton(
                            icon = action.icon,
                            onClick = {
                                action.onClick()
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Верхний эшелон: Панель Сохранённых закладок (#bookmarks-drawer)
 * Поддерживает горизонтальный Drag-and-Drop для смены порядка и Long Press для вызова капсулы из 3 иконок.
 */
@Composable
fun BookmarksDrawerPanel(
    state: SlickBrowserState,
    fogPhase: Float,
    extraDragOffsetPx: Float,
    onBookmarkClick: (BookmarkItem) -> Unit
) {
    val colors = LocalSlickColors.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val isVisible = state.panelState == UiPanelState.BOOKMARKS_BAR
    val drawerHeight = 166.dp
    val drawerHeightPx = with(density) { drawerHeight.toPx() }

    val baseOffsetY by animateFloatAsState(
        targetValue = if (isVisible) 0f else -drawerHeightPx * 1.15f,
        animationSpec = if (state.effectivePowerSave) tween(0) else spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "bookmarks_drawer_offset"
    )

    val finalTranslationY = (baseOffsetY + extraDragOffsetPx).coerceAtMost(0f)

    if (finalTranslationY > -drawerHeightPx * 1.1f) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(drawerHeight)
                .graphicsLayer { translationY = finalTranslationY }
                .edgeFogGlow(
                    phase = fogPhase,
                    isIncognito = state.isIncognito,
                    isMonoLogo = state.isMonoLogo,
                    isTopEdge = false,
                    enabled = !state.effectivePowerSave && !state.isMonoLogo
                )
                .clip(RoundedCornerShape(bottomStart = 34.dp, bottomEnd = 34.dp))
                .background(colors.bg)
                .border(
                    width = 2.dp,
                    color = colors.fg,
                    shape = RoundedCornerShape(bottomStart = 34.dp, bottomEnd = 34.dp)
                )
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(
                    items = state.bookmarks,
                    key = { _, item -> item.id }
                ) { index, bm ->
                    var itemBounds by remember { mutableStateOf<Rect?>(null) }
                    var dragOffsetX by remember { mutableFloatStateOf(0f) }
                    var isDragging by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .width(82.dp)
                            .onGloballyPositioned { coords ->
                                itemBounds = coords.boundsInWindow()
                            }
                            .graphicsLayer {
                                translationX = dragOffsetX
                                val scale = if (isDragging) 1.08f else 1f
                                scaleX = scale
                                scaleY = scale
                                alpha = if (isDragging) 0.55f else 1f
                            }
                            .pointerInput(bm.id, index) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val startX = down.position.x
                                    val startY = down.position.y
                                    var longPressed = false
                                    var draggingNow = false
                                    var accumulatedDx = 0f
                                    var accumulatedDy = 0f

                                    val holdJob: Job = scope.launch {
                                        delay(420L)
                                        longPressed = true
                                        val rect = itemBounds
                                        if (rect != null) {
                                            state.contextPillState = ContextPillState(
                                                centerX = rect.center.x,
                                                topY = rect.bottom + with(density) { 58.dp.toPx() },
                                                actions = listOf(
                                                    ContextPillAction(ContextPillIcon.BOOKMARK_DELETE) {
                                                        state.requestDeleteBookmark(bm)
                                                    },
                                                    ContextPillAction(ContextPillIcon.COPY_URL) {
                                                        copyUrlToClipboard(context, state, bm.url)
                                                    },
                                                    ContextPillAction(ContextPillIcon.SHARE_URL) {
                                                        shareUrlIntent(context, state, bm.title, bm.url)
                                                    }
                                                )
                                            )
                                        }
                                    }

                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull() ?: break
                                        if (!change.pressed) {
                                            change.consume()
                                            break
                                        }
                                        val dx = change.position.x - startX
                                        val dy = change.position.y - startY
                                        accumulatedDx = dx
                                        accumulatedDy = dy

                                        if (longPressed && abs(dx) > 14.dp.toPx() && !draggingNow) {
                                            draggingNow = true
                                            isDragging = true
                                            state.contextPillState = null
                                        } else if (!longPressed && (abs(dx) > 12.dp.toPx() || abs(dy) > 12.dp.toPx())) {
                                            holdJob.cancel()
                                        }

                                        if (draggingNow) {
                                            change.consume()
                                            dragOffsetX = dx
                                        }
                                    }

                                    holdJob.cancel()

                                    if (draggingNow) {
                                        val slotWidthPx = 94.dp.toPx()
                                        val shift = (accumulatedDx / slotWidthPx).roundToInt()
                                        val targetIdx = (index + shift).coerceIn(0, (state.bookmarks.size - 1).coerceAtLeast(0))
                                        dragOffsetX = 0f
                                        isDragging = false
                                        if (targetIdx != index) {
                                            state.moveBookmark(index, targetIdx)
                                            state.showToast("ПОРЯДОК ОБНОВЛЁН")
                                        }
                                    } else if (!longPressed && abs(accumulatedDx) < 10.dp.toPx() && abs(accumulatedDy) < 10.dp.toPx()) {
                                        onBookmarkClick(bm)
                                    }
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(colors.bg)
                                .border(2.dp, colors.fg, RoundedCornerShape(22.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = bm.iconCode,
                                color = colors.fg,
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                maxLines = 1
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = bm.title,
                            color = colors.fg,
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

/**
 * Нижний эшелон: Менеджер вкладок (Ступень 1 — Карусель, Ступень 2 — Плиточная сетка 2/3 колонки)
 */
@Composable
fun TabsManagerDrawer(
    state: SlickBrowserState,
    fogPhase: Float,
    extraDragOffsetPx: Float,
    onRequestExpandTab: (TabItem, Int, Rect) -> Unit,
    onRequestCloseTab: (Int) -> Unit
) {
    val colors = LocalSlickColors.current
    val config = LocalConfiguration.current
    val density = LocalDensity.current

    val screenHeightDp = config.screenHeightDp.dp
    val screenHeightPx = with(density) { screenHeightDp.toPx() }
    val isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE

    val isCarousel = state.panelState == UiPanelState.TABS_CAROUSEL
    val isGrid = state.panelState == UiPanelState.TABS_GRID
    val isOpen = isCarousel || isGrid

    val targetHeightDp by animateDpAsState(
        targetValue = when (state.panelState) {
            UiPanelState.TABS_GRID -> screenHeightDp
            UiPanelState.TABS_CAROUSEL -> screenHeightDp * 0.42f
            else -> screenHeightDp * 0.42f
        },
        animationSpec = if (state.effectivePowerSave) tween(0) else spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "tabs_drawer_height"
    )

    val baseOffsetY by animateFloatAsState(
        targetValue = if (isOpen) 0f else screenHeightPx * 0.5f,
        animationSpec = if (state.effectivePowerSave) tween(0) else spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "tabs_drawer_offset"
    )

    val finalTranslationY = (baseOffsetY + extraDragOffsetPx).coerceAtLeast(0f)
    val topCornerRadius = if (isGrid) 0.dp else 30.dp

    if (isOpen || finalTranslationY < screenHeightPx * 0.48f) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(targetHeightDp)
                    .graphicsLayer { translationY = finalTranslationY }
                    .edgeFogGlow(
                        phase = fogPhase,
                        isIncognito = state.isIncognito,
                        isMonoLogo = state.isMonoLogo,
                        isTopEdge = true,
                        enabled = !state.effectivePowerSave && !state.isMonoLogo
                    )
                    .clip(RoundedCornerShape(topStart = topCornerRadius, topEnd = topCornerRadius))
                    .background(colors.bg)
                    .border(
                        width = 2.dp,
                        color = colors.fg,
                        shape = RoundedCornerShape(topStart = topCornerRadius, topEnd = topCornerRadius)
                    )
            ) {
                val tabs = state.activeTabsPool
                val activeIdx = state.activeTabIndex

                if (isCarousel) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 22.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        itemsIndexed(
                            items = tabs,
                            key = { _, item -> item.id }
                        ) { index, tab ->
                            TabPreviewCard(
                                tab = tab,
                                isActive = index == activeIdx,
                                isGridMode = false,
                                fogPhase = fogPhase,
                                isIncognito = state.isIncognito,
                                isMonoLogo = state.isMonoLogo,
                                powerSave = state.effectivePowerSave,
                                state = state,
                                onExpand = { rect -> onRequestExpandTab(tab, index, rect) },
                                onClose = { onRequestCloseTab(index) },
                                modifier = Modifier
                                    .width(176.dp)
                                    .fillMaxHeight()
                                    .heightIn(max = 215.dp)
                            )
                        }
                    }
                } else {
                    val columns = if (isLandscape) 3 else 2
                    val cardHeight = if (isLandscape) 152.dp else 188.dp

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 18.dp,
                            end = 18.dp,
                            top = 28.dp,
                            bottom = 92.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(
                            items = tabs,
                            key = { _, item -> item.id }
                        ) { index, tab ->
                            TabPreviewCard(
                                tab = tab,
                                isActive = index == activeIdx,
                                isGridMode = true,
                                fogPhase = fogPhase,
                                isIncognito = state.isIncognito,
                                isMonoLogo = state.isMonoLogo,
                                powerSave = state.effectivePowerSave,
                                state = state,
                                onExpand = { rect -> onRequestExpandTab(tab, index, rect) },
                                onClose = { onRequestCloseTab(index) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(cardHeight)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Карточка вкладки без номеров и без пинов.
 * Активная вкладка подсвечивается инверсией шапки и мягким рассеянным туманным свечением вокруг карточки.
 * Поддерживает:
 * - Тап ИЛИ свайп вниз -> плавная анимация раскрытия карточки на весь экран.
 * - Свайп вверх (в карусели) или влево/вправо (в сетке) -> закрытие вкладки.
 * - Long Press -> всплывающая капсула из 3 иконок над карточкой.
 */
@Composable
fun TabPreviewCard(
    tab: TabItem,
    isActive: Boolean,
    isGridMode: Boolean,
    fogPhase: Float,
    isIncognito: Boolean,
    isMonoLogo: Boolean,
    powerSave: Boolean,
    state: SlickBrowserState,
    onExpand: (Rect) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSlickColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var cardBounds by remember { mutableStateOf(Rect.Zero) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    val grayscaleMatrix = remember {
        ColorMatrix().apply {
            setToSaturation(0f)
        }
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                cardBounds = coords.boundsInWindow()
            }
            .graphicsLayer {
                translationX = dragOffsetX
                translationY = dragOffsetY
            }
            .diffusedFogAura(
                phase = fogPhase,
                isIncognito = isIncognito,
                isMonoLogo = isMonoLogo,
                enabled = isActive && !powerSave && !isMonoLogo,
                blurRadius = 20.dp,
                spread = 7.dp,
                alpha = 0.88f
            )
            .clip(RoundedCornerShape(16.dp))
            .background(colors.bg)
            .border(
                width = if (isActive) 2.2.dp else 1.5.dp,
                color = colors.fg,
                shape = RoundedCornerShape(16.dp)
            )
            .pointerInput(tab.id, isGridMode) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startX = down.position.x
                    val startY = down.position.y
                    var longFired = false
                    var moved = false
                    var totalDx = 0f
                    var totalDy = 0f

                    val holdJob: Job = scope.launch {
                        delay(450L)
                        if (!moved) {
                            longFired = true
                            state.contextPillState = ContextPillState(
                                centerX = cardBounds.center.x,
                                topY = cardBounds.top + 32f,
                                actions = listOf(
                                    ContextPillAction(ContextPillIcon.BOOKMARK_ADD) {
                                        state.addBookmark(tab.title, tab.url)
                                    },
                                    ContextPillAction(ContextPillIcon.COPY_URL) {
                                        copyUrlToClipboard(context, state, tab.url)
                                    },
                                    ContextPillAction(ContextPillIcon.SHARE_URL) {
                                        shareUrlIntent(context, state, tab.title, tab.url)
                                    }
                                )
                            )
                        }
                    }

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) {
                            change.consume()
                            break
                        }
                        val dx = change.position.x - startX
                        val dy = change.position.y - startY
                        totalDx = dx
                        totalDy = dy

                        if (abs(dx) > 10.dp.toPx() || abs(dy) > 10.dp.toPx()) {
                            moved = true
                            holdJob.cancel()
                        }

                        if (!longFired) {
                            if (!isGridMode && abs(dy) > abs(dx) && abs(dy) > 10.dp.toPx()) {
                                change.consume()
                                dragOffsetY = dy * 0.65f
                            } else if (isGridMode) {
                                if (abs(dx) > abs(dy) && abs(dx) > 10.dp.toPx()) {
                                    change.consume()
                                    dragOffsetX = dx * 0.75f
                                } else if (dy > 12.dp.toPx() && abs(dy) > abs(dx)) {
                                    change.consume()
                                    dragOffsetY = dy * 0.55f
                                }
                            }
                        }
                    }

                    holdJob.cancel()
                    dragOffsetX = 0f
                    dragOffsetY = 0f

                    if (!longFired) {
                        val closeCarouselThreshold = -48.dp.toPx()
                        val expandDownThreshold = 42.dp.toPx()
                        val closeGridXThreshold = 62.dp.toPx()
                        val tapSlop = 12.dp.toPx()

                        if (!isGridMode) {
                            if (totalDy < closeCarouselThreshold && abs(totalDy) > abs(totalDx)) {
                                onClose()
                            } else if ((totalDy > expandDownThreshold && abs(totalDy) > abs(totalDx)) ||
                                (abs(totalDx) < tapSlop && abs(totalDy) < tapSlop)
                            ) {
                                onExpand(cardBounds)
                            }
                        } else {
                            if (abs(totalDx) > closeGridXThreshold && abs(totalDx) > abs(totalDy)) {
                                onClose()
                            } else if ((totalDy > expandDownThreshold && abs(totalDy) > abs(totalDx)) ||
                                (abs(totalDx) < tapSlop && abs(totalDy) < tapSlop)
                            ) {
                                onExpand(cardBounds)
                            }
                        }
                    }
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Шапка карточки (инвертируется у активной вкладки, защищена от переполнения)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isActive) colors.fg else colors.bg)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = tab.title,
                    color = if (isActive) colors.bg else colors.fg,
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Разделитель под шапкой
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(colors.fg)
            )

            // Тело карточки: Ч/Б снапшот или минималистичный скелетон
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                val snap = tab.snapshot
                if (snap != null && !snap.isRecycled) {
                    Image(
                        bitmap = snap.asImageBitmap(),
                        contentDescription = tab.title,
                        contentScale = ContentScale.Crop,
                        colorFilter = ColorFilter.colorMatrix(grayscaleMatrix),
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, colors.fg.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    )
                } else {
                    // Минималистичный скелетон (прямоугольник + 3 полоски)
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.5.dp, colors.fg.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                                .padding(6.dp)
                        ) {
                            Text(
                                text = tab.displayDomain.uppercase(),
                                color = colors.fg.copy(alpha = 0.7f),
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(colors.fg.copy(alpha = 0.65f))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.74f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(colors.fg.copy(alpha = 0.55f))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.56f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(colors.fg.copy(alpha = 0.45f))
                        )
                    }
                }
            }
        }
    }
}

/**
 * Оверлей анимации раскрытия карточки вкладки из её текущих координат на весь экран
 * с сохранением рассеянного туманного свечения.
 */
@Composable
fun CardExpandOverlay(
    animState: CardExpandAnimState?,
    fogPhase: Float,
    isIncognito: Boolean,
    isMonoLogo: Boolean,
    powerSave: Boolean,
    onAnimationFinished: () -> Unit
) {
    if (animState == null) return

    val colors = LocalSlickColors.current
    val density = LocalDensity.current
    val config = LocalConfiguration.current

    val progress = remember(animState.tabId) { Animatable(0f) }

    LaunchedEffect(animState.tabId) {
        if (powerSave) {
            onAnimationFinished()
        } else {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 390, easing = FastOutSlowInEasing)
            )
            onAnimationFinished()
        }
    }

    val p = progress.value
    val screenW = config.screenWidthDp.dp
    val screenH = config.screenHeightDp.dp

    val startLeftDp = with(density) { animState.startBounds.left.toDp() }
    val startTopDp = with(density) { animState.startBounds.top.toDp() }
    val startWidthDp = with(density) { animState.startBounds.width.toDp().coerceAtLeast(120.dp) }
    val startHeightDp = with(density) { animState.startBounds.height.toDp().coerceAtLeast(120.dp) }

    val curLeft = lerp(startLeftDp, 0.dp, p)
    val curTop = lerp(startTopDp, 0.dp, p)
    val curWidth = lerp(startWidthDp, screenW, p)
    val curHeight = lerp(startHeightDp, screenH, p)
    val curCorner = lerp(16.dp, 0.dp, p)

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .offset(x = curLeft, y = curTop)
                .size(width = curWidth, height = curHeight)
                .diffusedFogAura(
                    phase = fogPhase,
                    isIncognito = isIncognito,
                    isMonoLogo = isMonoLogo,
                    enabled = !powerSave && !isMonoLogo,
                    blurRadius = 26.dp,
                    spread = 10.dp,
                    alpha = (1f - p * 0.35f).coerceIn(0.2f, 1f)
                )
                .clip(RoundedCornerShape(curCorner))
                .background(colors.bg)
                .border(2.dp, colors.fg, RoundedCornerShape(curCorner))
        )
    }
}

/**
 * Кнопка «Загрузки» (.liquid-dl-btn) с рассеянным туманным прогрессом внутри
 */
@Composable
fun LiquidDownloadButton(
    label: String,
    statusText: String,
    progress: Float,
    fogPhase: Float,
    isIncognito: Boolean,
    isMonoLogo: Boolean,
    powerSave: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalSlickColors.current
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(140),
        label = "dl_progress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(colors.bg)
            .border(2.dp, colors.fg, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .diffusedFogAura(
                phase = fogPhase,
                isIncognito = isIncognito,
                isMonoLogo = isMonoLogo,
                enabled = animatedProgress > 0.01f && !powerSave,
                blurRadius = 18.dp,
                spread = 2.dp,
                alpha = 0.85f,
                progressFraction = animatedProgress
            )
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = colors.fg,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = statusText,
                color = colors.fg,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Меню «Три точки» (Настройки сайта — #site-menu-popup)
 */
@Composable
fun SiteMenuPopupModal(
    state: SlickBrowserState,
    fogPhase: Float,
    onTriggerDownloadDemo: () -> Unit,
    onApplyWebSettings: () -> Unit
) {
    val colors = LocalSlickColors.current
    val context = LocalContext.current
    val currentTab = state.currentTab

    AnimatedVisibility(
        visible = state.isSiteMenuOpen,
        enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.92f),
        exit = fadeOut(tween(140)) + scaleOut(targetScale = 0.92f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) { state.isSiteMenuOpen = false }
                .padding(bottom = 78.dp, start = 16.dp, end = 16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 355.dp)
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) {}
                    .clip(RoundedCornerShape(28.dp))
                    .background(colors.bg)
                    .border(2.dp, colors.fg, RoundedCornerShape(28.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Шапка меню с рассеянным туманным свечением вокруг
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .diffusedFogAura(
                            phase = fogPhase,
                            isIncognito = state.isIncognito,
                            isMonoLogo = state.isMonoLogo,
                            enabled = !state.effectivePowerSave && !state.isMonoLogo,
                            blurRadius = 18.dp,
                            spread = 4.dp,
                            alpha = 0.85f
                        )
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.bg)
                        .border(1.5.dp, colors.fg, RoundedCornerShape(18.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentTab?.title ?: "SLICK CORE",
                        color = colors.fg,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 10.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MiniCircleIconButton(ContextPillIcon.BOOKMARK_ADD) {
                            if (currentTab != null) {
                                state.addBookmark(currentTab.title, currentTab.url)
                            }
                        }
                        MiniCircleIconButton(ContextPillIcon.COPY_URL) {
                            if (currentTab != null) {
                                copyUrlToClipboard(context, state, currentTab.url)
                            }
                        }
                        MiniCircleIconButton(ContextPillIcon.SHARE_URL) {
                            if (currentTab != null) {
                                shareUrlIntent(context, state, currentTab.title, currentTab.url)
                            }
                        }
                    }
                }

                // 1. Загрузки
                LiquidDownloadButton(
                    label = "ЗАГРУЗКИ",
                    statusText = state.downloadStatusText,
                    progress = state.downloadProgress,
                    fogPhase = fogPhase,
                    isIncognito = state.isIncognito,
                    isMonoLogo = state.isMonoLogo,
                    powerSave = state.effectivePowerSave,
                    onClick = onTriggerDownloadDemo
                )

                // 2. Версия для ПК
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.bg)
                        .border(1.5.dp, colors.fg, RoundedCornerShape(18.dp))
                        .clickable {
                            state.toggleDesktopUa()
                            onApplyWebSettings()
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ВЕРСИЯ ДЛЯ ПК",
                        color = colors.fg,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (state.isDesktopUa) "ВКЛ" else "ВЫКЛ",
                        color = colors.fg,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }

                // 3. Размер текста (30% - 200%)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.bg)
                        .border(1.5.dp, colors.fg, RoundedCornerShape(18.dp))
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ТЕКСТ: ${state.textZoomPercent}%",
                        color = colors.fg,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Slider(
                        value = state.textZoomPercent.toFloat(),
                        onValueChange = {
                            state.updateTextZoom(it.roundToInt())
                            onApplyWebSettings()
                        },
                        valueRange = 30f..200f,
                        colors = SliderDefaults.colors(
                            thumbColor = colors.fg,
                            activeTrackColor = colors.fg,
                            inactiveTrackColor = colors.fgMuted
                        ),
                        modifier = Modifier.width(120.dp)
                    )
                }

                // 4. История
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.bg)
                        .border(1.5.dp, colors.fg, RoundedCornerShape(18.dp))
                        .clickable {
                            state.isSiteMenuOpen = false
                            state.isHistoryOpen = true
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ИСТОРИЯ",
                        color = colors.fg,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "ОТКРЫТЬ",
                        color = colors.fg,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }

                // 5. Настройки браузера
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.bg)
                        .border(1.5.dp, colors.fg, RoundedCornerShape(18.dp))
                        .clickable {
                            state.isSiteMenuOpen = false
                            state.isSettingsOpen = true
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "НАСТРОЙКИ БРАУЗЕРА",
                        color = colors.fg,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "→",
                        color = colors.fg,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

/**
 * Глобальные настройки браузера (#settings-modal) с рассеянным туманным свечением вокруг окна
 * и пунктом «Очистить кэш и куки» (с диалогом подтверждения).
 */
@Composable
fun GlobalSettingsModal(
    state: SlickBrowserState,
    fogPhase: Float,
    onTriggerDownloadDemo: () -> Unit,
    onClearCacheAndCookies: () -> Unit,
    onApplyWebSettings: () -> Unit
) {
    val colors = LocalSlickColors.current
    val scrollState = rememberScrollState()

    AnimatedVisibility(
        visible = state.isSettingsOpen,
        enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.88f),
        exit = fadeOut(tween(140)) + scaleOut(targetScale = 0.88f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) { state.isSettingsOpen = false }
                .padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 395.dp)
                    .fillMaxWidth()
                    .heightIn(max = 620.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) {}
                    .diffusedFogAura(
                        phase = fogPhase,
                        isIncognito = state.isIncognito,
                        isMonoLogo = state.isMonoLogo,
                        enabled = !state.effectivePowerSave && !state.isMonoLogo,
                        blurRadius = 26.dp,
                        spread = 8.dp,
                        alpha = 0.85f
                    )
                    .clip(RoundedCornerShape(32.dp))
                    .background(colors.bg)
                    .border(2.dp, colors.fg, RoundedCornerShape(32.dp))
                    .padding(20.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Группа 1: Поиск и Сеть
                SettingsSectionHeader("1. ПОИСК И СЕТЬ")

                SettingItemRow(label = "ПОИСКОВИК") {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        SearchEngine.entries.forEach { engine ->
                            SlickToggleButton(
                                text = engine.code,
                                active = state.searchEngine == engine,
                                onClick = { state.selectSearchEngine(engine) }
                            )
                        }
                    }
                }

                SettingItemRow(label = "МОНОХРОМ САЙТОВ") {
                    SlickToggleButton(
                        text = if (state.monochromeWeb) "ВКЛ" else "ВЫКЛ",
                        active = state.monochromeWeb,
                        onClick = {
                            state.toggleMonochromeWeb()
                            onApplyWebSettings()
                        }
                    )
                }

                SettingItemRow(label = "СИМУЛЯЦИЯ СЕТИ") {
                    SlickToggleButton(
                        text = if (!state.isNetworkSimulatedOffline) "ОНЛАЙН" else "ОФФЛАЙН",
                        active = !state.isNetworkSimulatedOffline,
                        onClick = { state.toggleNetworkSimulation() }
                    )
                }

                // Группа 2: Интерфейс
                SettingsSectionHeader("2. ИНТЕРФЕЙС")

                SettingItemRow(label = "ТЕМА") {
                    SlickToggleButton(
                        text = if (state.isLightTheme) "#FFF" else "#000",
                        active = state.isLightTheme,
                        onClick = { state.toggleTheme() }
                    )
                }

                SettingItemRow(label = "ЛОГО И АУРА") {
                    SlickToggleButton(
                        text = if (state.isMonoLogo) "MONO" else "LIQUID",
                        active = !state.isMonoLogo,
                        onClick = { state.toggleLogoMode() }
                    )
                }

                // Группа 3: Данные и Хранилище
                SettingsSectionHeader("3. ДАННЫЕ И ХРАНИЛИЩЕ")

                SettingItemRow(label = "ИСТОРИЯ ПЕРЕХОДОВ") {
                    SlickToggleButton(
                        text = "ЖУРНАЛ",
                        active = false,
                        onClick = {
                            state.isSettingsOpen = false
                            state.isHistoryOpen = true
                        }
                    )
                }

                SettingItemRow(label = "КЭШ И КУКИ САЙТОВ") {
                    SlickToggleButton(
                        text = "ОЧИСТИТЬ",
                        active = false,
                        onClick = {
                            state.requestClearCacheAndCookies(onClearCacheAndCookies)
                        }
                    )
                }

                SettingItemRow(label = "ПУТЬ СОХРАНЕНИЯ") {
                    SlickToggleButton(
                        text = state.storagePath,
                        active = false,
                        onClick = { state.toggleStoragePath() }
                    )
                }

                // Группа 4: Система и Энергосбережение
                SettingsSectionHeader("4. СИСТЕМА И ЭНЕРГОСБЕРЕЖЕНИЕ")

                SettingItemRow(label = "ЭНЕРГОСБЕРЕЖЕНИЕ") {
                    SlickToggleButton(
                        text = if (state.effectivePowerSave) "ВКЛ" else "ВЫКЛ",
                        active = state.effectivePowerSave,
                        onClick = { state.togglePowerSaveManual() }
                    )
                }

                // Нижняя кнопка Загрузки
                LiquidDownloadButton(
                    label = "ЗАГРУЗКИ (ТЕСТ ПАКЕТА)",
                    statusText = if (state.downloadStatusText == "0 МБ / 0 МБ") "СТАРТ" else state.downloadStatusText,
                    progress = state.downloadProgress,
                    fogPhase = fogPhase,
                    isIncognito = state.isIncognito,
                    isMonoLogo = state.isMonoLogo,
                    powerSave = state.effectivePowerSave,
                    onClick = onTriggerDownloadDemo
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    val colors = LocalSlickColors.current
    Text(
        text = title,
        color = colors.fgMuted,
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 10.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = 6.dp)
    )
}

@Composable
private fun SettingItemRow(
    label: String,
    trailingContent: @Composable () -> Unit
) {
    val colors = LocalSlickColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.bg)
            .border(2.dp, colors.fg, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = colors.fg,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )
        trailingContent()
    }
}

/**
 * Модальное окно Истории переходов (#history-modal)
 */
@Composable
fun HistoryModal(
    state: SlickBrowserState,
    fogPhase: Float,
    onHistoryItemClick: (String) -> Unit
) {
    val colors = LocalSlickColors.current

    AnimatedVisibility(
        visible = state.isHistoryOpen,
        enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.9f),
        exit = fadeOut(tween(140)) + scaleOut(targetScale = 0.9f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) { state.isHistoryOpen = false }
                .padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 385.dp)
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) {}
                    .diffusedFogAura(
                        phase = fogPhase,
                        isIncognito = state.isIncognito,
                        isMonoLogo = state.isMonoLogo,
                        enabled = !state.effectivePowerSave && !state.isMonoLogo,
                        blurRadius = 24.dp,
                        spread = 6.dp,
                        alpha = 0.8f
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(colors.bg)
                    .border(2.dp, colors.fg, RoundedCornerShape(28.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ИСТОРИЯ БРАУЗЕРА",
                        color = colors.fg,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (state.historyLog.isNotEmpty()) {
                        SlickToggleButton(
                            text = "ОЧИСТИТЬ",
                            active = false,
                            onClick = { state.requestClearHistory() }
                        )
                    }
                }

                if (state.historyLog.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ИСТОРИЯ ПУСТА",
                            color = colors.fgMuted,
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(state.historyLog, key = { it.id }) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(1.5.dp, colors.fg, RoundedCornerShape(14.dp))
                                    .clickable {
                                        state.isHistoryOpen = false
                                        onHistoryItemClick(item.url)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.title,
                                    color = colors.fg,
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                )
                                Text(
                                    text = item.timeFormatted,
                                    color = colors.fgMuted,
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Разумное модальное окно подтверждения (ДА / ОТМЕНА) с мягким рассеянным туманом.
 * Вызывается только для необратимых деструктивных действий с данными.
 */
@Composable
fun DestructiveConfirmDialog(
    dialogState: ConfirmDialogState?,
    fogPhase: Float,
    isIncognito: Boolean,
    isMonoLogo: Boolean,
    powerSave: Boolean,
    onDismiss: () -> Unit
) {
    val colors = LocalSlickColors.current

    AnimatedVisibility(
        visible = dialogState != null,
        enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.9f),
        exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.9f)
    ) {
        if (dialogState != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) { onDismiss() }
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 340.dp)
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        ) {}
                        .diffusedFogAura(
                            phase = fogPhase,
                            isIncognito = isIncognito,
                            isMonoLogo = isMonoLogo,
                            enabled = !powerSave && !isMonoLogo,
                            blurRadius = 24.dp,
                            spread = 8.dp,
                            alpha = 0.9f
                        )
                        .clip(RoundedCornerShape(26.dp))
                        .background(colors.bg)
                        .border(2.dp, colors.fg, RoundedCornerShape(26.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = dialogState.title,
                        color = colors.fg,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = dialogState.message,
                        color = colors.fgMuted,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
                    ) {
                        SlickToggleButton(
                            text = dialogState.cancelText,
                            active = false,
                            onClick = onDismiss
                        )
                        SlickToggleButton(
                            text = dialogState.confirmText,
                            active = true,
                            onClick = {
                                dialogState.onConfirm()
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Строгий кастомный экран ошибки сети (.net-error-box) с кнопкой ПОПРОБОВАТЬ СНОВА
 */
@Composable
fun StrictNetworkErrorView(
    errorCode: String,
    onRetry: () -> Unit
) {
    val colors = LocalSlickColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(colors.bg)
                .border(3.dp, colors.fg, RoundedCornerShape(28.dp))
                .padding(horizontal = 24.dp, vertical = 38.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Text(
                text = errorCode,
                color = colors.fg,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 19.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            SlickToggleButton(
                text = "ПОПРОБОВАТЬ СНОВА",
                active = true,
                onClick = onRetry
            )
        }
    }
}

/**
 * Компактный системный тост (#vector-toast) с защитой от выхода текста за края
 */
@Composable
fun VectorToastOverlay(
    message: String?,
    trigger: Int,
    onClear: () -> Unit
) {
    val colors = LocalSlickColors.current

    LaunchedEffect(trigger) {
        if (message != null) {
            delay(1600L)
            onClear()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 82.dp, start = 24.dp, end = 24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = message != null,
            enter = fadeIn(tween(160)) + scaleIn(initialScale = 0.9f),
            exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.9f)
        ) {
            if (message != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.fg)
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = message.uppercase(),
                        color = colors.bg,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        letterSpacing = 0.6.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
