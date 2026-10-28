/* First worked operational APK build compiled on 28.10.26 by Argon */
package com.slick.browser

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.PlatformImeOptions
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import com.slick.browser.model.CardExpandAnimState
import com.slick.browser.model.SlickBrowserState
import com.slick.browser.model.UiPanelState
import com.slick.browser.ui.components.BookmarksDrawerPanel
import com.slick.browser.ui.components.CardExpandOverlay
import com.slick.browser.ui.components.DestructiveConfirmDialog
import com.slick.browser.ui.components.FloatingContextPillOverlay
import com.slick.browser.ui.components.GlobalSettingsModal
import com.slick.browser.ui.components.HistoryModal
import com.slick.browser.ui.components.SiteMenuPopupModal
import com.slick.browser.ui.components.StrictNetworkErrorView
import com.slick.browser.ui.components.TabsManagerDrawer
import com.slick.browser.ui.components.VectorToastOverlay
import com.slick.browser.ui.effects.FogBurstOverlay
import com.slick.browser.ui.effects.SlickLiquidLogo
import com.slick.browser.ui.effects.diffusedFogAura
import com.slick.browser.ui.effects.rememberFogPhase
import com.slick.browser.ui.theme.JetBrainsMonoFamily
import com.slick.browser.ui.theme.LocalSlickColors
import com.slick.browser.ui.theme.SlickBrowserTheme
import com.slick.browser.web.WebViewPoolManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private lateinit var browserState: SlickBrowserState
    private lateinit var webViewPool: WebViewPoolManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.i("SlickCore", "/* First worked operational APK build compiled on 28.10.26 by Argon */")

        WindowCompat.setDecorFitsSystemWindows(window, false)

        browserState = SlickBrowserState(applicationContext)
        webViewPool = WebViewPoolManager(applicationContext, browserState)

        // Авто-детект системного режима энергосбережения
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        browserState.isSystemPowerSave = powerManager?.isPowerSaveMode == true

        // Обработка входящего Intent (если браузер открыли по внешней ссылке)
        intent?.dataString?.takeIf { it.startsWith("http") }?.let { externalUrl ->
            browserState.openInputOrUrl(externalUrl, createNewTab = true)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    browserState.confirmDialogState != null -> {
                        browserState.confirmDialogState = null
                    }
                    browserState.contextPillState != null -> {
                        browserState.contextPillState = null
                    }
                    browserState.isSettingsOpen || browserState.isSiteMenuOpen || browserState.isHistoryOpen -> {
                        browserState.closeAllPopups()
                    }
                    browserState.isEditingBottomOmni -> {
                        browserState.isEditingBottomOmni = false
                    }
                    browserState.panelState == UiPanelState.TABS_GRID -> {
                        browserState.setPanel(UiPanelState.TABS_CAROUSEL)
                    }
                    browserState.panelState != UiPanelState.IDLE -> {
                        browserState.setPanel(UiPanelState.IDLE)
                    }
                    browserState.overlayZeroOnWeb -> {
                        browserState.overlayZeroOnWeb = false
                    }
                    browserState.inWebView && webViewPool.canGoBackOnActiveTab() -> {
                        webViewPool.goBackOnActiveTab()
                    }
                    browserState.inWebView -> {
                        browserState.overlayZeroOnWeb = true
                    }
                    else -> {
                        finish()
                    }
                }
            }
        })

        setContent {
            // Синхронизация FLAG_SECURE при переключении Инкогнито
            LaunchedEffect(browserState.isIncognito) {
                if (browserState.isIncognito) {
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE
                    )
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            // Слушатель системного режима энергосбережения в реальном времени
            DisposableEffect(Unit) {
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(ctx: Context?, intent: Intent?) {
                        val pm = ctx?.getSystemService(Context.POWER_SERVICE) as? PowerManager
                        browserState.isSystemPowerSave = pm?.isPowerSaveMode == true
                    }
                }
                registerReceiver(receiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED))
                onDispose {
                    unregisterReceiver(receiver)
                }
            }

            SlickBrowserTheme(isLightTheme = browserState.isLightTheme) {
                SlickBrowserRootScreen(
                    state = browserState,
                    webViewPool = webViewPool
                )
            }
        }
    }
}

@Composable
fun SlickBrowserRootScreen(
    state: SlickBrowserState,
    webViewPool: WebViewPoolManager
) {
    val colors = LocalSlickColors.current
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val fogPhase = rememberFogPhase(enabled = !state.effectivePowerSave && !state.isMonoLogo)

    // Индикатор натяжения двухфазного жеста (#tension-indicator) с рассеянным туманным свечением
    var tensionHeightDp by remember { mutableStateOf(0.dp) }
    var tensionWidthFraction by remember { mutableFloatStateOf(0.36f) }
    var tensionIsTop by remember { mutableStateOf(true) }

    // Живое следование панелей за пальцем (закон обратимости без задержки 200 мс)
    var bookmarksLiveOffsetPx by remember { mutableFloatStateOf(0f) }
    var tabsLiveOffsetPx by remember { mutableFloatStateOf(0f) }

    // Демо-симуляция скачивания пакета 128 МБ с туманным заполнением
    var downloadJob by remember { mutableStateOf<Job?>(null) }
    fun triggerDownloadDemo() {
        downloadJob?.cancel()
        downloadJob = scope.launch {
            val totalMb = 128
            var currentMb = 0
            while (currentMb < totalMb) {
                delay(60L)
                currentMb = (currentMb + 6).coerceAtMost(totalMb)
                state.downloadProgress = currentMb.toFloat() / totalMb.toFloat()
                state.downloadStatusText = "$currentMb МБ / $totalMb МБ"
            }
            state.showToast("ФАЙЛ ЗАГРУЖЕН (128 МБ)")
            delay(850L)
            state.downloadProgress = 0f
            state.downloadStatusText = "128 МБ ГОТОВО"
        }
    }

    val isZeroVisible = !state.inWebView || state.overlayZeroOnWeb

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            // Глобальный обработчик обратимых двухфазных жестов (на главной странице и в открытых панелях)
            .pointerInput(state.panelState, isZeroVisible) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startX = down.position.x
                    val startY = down.position.y

                    // Если мы в WebView и панели закрыты, основные вертикальные свайпы обрабатываются нижней строкой
                    if (!isZeroVisible && state.panelState == UiPanelState.IDLE) {
                        return@awaitEachGesture
                    }

                    var phase1Opened: UiPanelState? = null
                    var hold200Job: Job? = null
                    var lastDy = 0f
                    var lastDx = 0f

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) {
                            break
                        }

                        val dx = change.position.x - startX
                        val dy = change.position.y - startY
                        lastDx = dx
                        lastDy = dy

                        // 1. Закон обратимости: если панель открылась во время удержания 200 мс, но палец потянул назад
                        if (phase1Opened == UiPanelState.BOOKMARKS_BAR) {
                            if (dy < 36.dp.toPx()) {
                                bookmarksLiveOffsetPx = (dy - 64.dp.toPx()) * 1.4f
                                if (dy <= 6.dp.toPx()) {
                                    bookmarksLiveOffsetPx = 0f
                                    state.setPanel(UiPanelState.IDLE)
                                    phase1Opened = null
                                }
                            }
                            continue
                        }

                        if (phase1Opened == UiPanelState.TABS_CAROUSEL) {
                            if (dy > -36.dp.toPx()) {
                                tabsLiveOffsetPx = (dy + 64.dp.toPx()) * 1.4f
                                if (dy >= -6.dp.toPx()) {
                                    tabsLiveOffsetPx = 0f
                                    state.setPanel(UiPanelState.IDLE)
                                    phase1Opened = null
                                }
                            }
                            continue
                        }

                        // 2. Если панель уже была открыта — обратный свайп мгновенно двигает её за пальцем без 200 мс
                        if (state.panelState == UiPanelState.BOOKMARKS_BAR && dy < 0f) {
                            bookmarksLiveOffsetPx = dy
                            continue
                        }

                        if (state.panelState == UiPanelState.TABS_CAROUSEL && dy > 0f) {
                            tabsLiveOffsetPx = dy
                            continue
                        }

                        // 3. Из состояния IDLE: натяжение и таймер 200 мс
                        if (state.panelState == UiPanelState.IDLE) {
                            if (abs(dy) > 42.dp.toPx() && abs(dy) > abs(dx) * 1.2f) {
                                val pullingDown = dy > 0f
                                tensionIsTop = pullingDown
                                tensionHeightDp = (abs(dy) * 0.05f).dp.coerceAtMost(12.dp)
                                tensionWidthFraction = (0.36f + (abs(dy) / size.height) * 0.45f).coerceAtMost(0.76f)

                                if (hold200Job == null) {
                                    hold200Job = scope.launch {
                                        delay(200L)
                                        tensionHeightDp = 0.dp
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        if (pullingDown) {
                                            phase1Opened = UiPanelState.BOOKMARKS_BAR
                                            state.setPanel(UiPanelState.BOOKMARKS_BAR, 0.06f)
                                        } else {
                                            phase1Opened = UiPanelState.TABS_CAROUSEL
                                            state.setPanel(UiPanelState.TABS_CAROUSEL, 0.94f)
                                        }
                                    }
                                }
                            } else {
                                hold200Job?.cancel()
                                hold200Job = null
                                tensionHeightDp = 0.dp
                            }
                        }
                    }

                    hold200Job?.cancel()
                    tensionHeightDp = 0.dp
                    bookmarksLiveOffsetPx = 0f
                    tabsLiveOffsetPx = 0f

                    if (phase1Opened == null) {
                        when (state.panelState) {
                            UiPanelState.BOOKMARKS_BAR -> {
                                if (lastDy < -28.dp.toPx() ||
                                    (abs(lastDx) < 10.dp.toPx() && abs(lastDy) < 10.dp.toPx() && startY > 170.dp.toPx())
                                ) {
                                    state.setPanel(UiPanelState.IDLE)
                                }
                            }
                            UiPanelState.TABS_CAROUSEL -> {
                                val topBoundaryPx = size.height * 0.56f
                                if (lastDy < -44.dp.toPx() && abs(lastDy) > abs(lastDx)) {
                                    state.setPanel(UiPanelState.TABS_GRID)
                                } else if (lastDy > 28.dp.toPx() ||
                                    (abs(lastDx) < 10.dp.toPx() && abs(lastDy) < 10.dp.toPx() && startY < topBoundaryPx)
                                ) {
                                    state.setPanel(UiPanelState.IDLE)
                                }
                            }
                            UiPanelState.TABS_GRID -> {
                                if (lastDy > 48.dp.toPx() && abs(lastDy) > abs(lastDx)) {
                                    state.setPanel(UiPanelState.TABS_CAROUSEL)
                                }
                            }
                            UiPanelState.IDLE -> {}
                        }
                    }
                }
            }
    ) {
        // 1. Слой активного сайта (#web-viewport) с масштабированием при открытии панелей и 3-слайдовым Bitmap-треком
        if (state.inWebView && !state.overlayZeroOnWeb) {
            ActiveWebViewportSection(
                state = state,
                webViewPool = webViewPool,
                fogPhase = fogPhase,
                onTriggerTension = { isTop, hDp, wFrac ->
                    tensionIsTop = isTop
                    tensionHeightDp = hDp
                    tensionWidthFraction = wFrac
                },
                onLivePanelDrag = { bmOffset, tabsOffset ->
                    bookmarksLiveOffsetPx = bmOffset
                    tabsLiveOffsetPx = tabsOffset
                }
            )
        }

        // 2. Главная страница (Zero Point — #app) с честной геометрией над клавиатурой WindowInsets.ime
        AnimatedVisibility(
            visible = isZeroVisible,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(180))
        ) {
            ZeroPointHomeScreen(
                state = state,
                fogPhase = fogPhase,
                onDestroyIncognitoWebViews = { webViewPool.destroyIncognitoSession() },
                onSubmitSearch = { query ->
                    state.currentTab?.let { webViewPool.captureTabSnapshot(it.id) }
                    state.openInputOrUrl(query, createNewTab = true)
                    state.currentTab?.let { newTab ->
                        webViewPool.loadUrlInTab(newTab.id, newTab.url, newTab.isIncognito)
                    }
                }
            )
        }

        // 3. Верхняя панель Сохранённых закладок
        BookmarksDrawerPanel(
            state = state,
            fogPhase = fogPhase,
            extraDragOffsetPx = bookmarksLiveOffsetPx,
            onBookmarkClick = { bm ->
                state.currentTab?.let { webViewPool.captureTabSnapshot(it.id) }
                state.openInputOrUrl(bm.url, createNewTab = true)
                state.currentTab?.let { newTab ->
                    webViewPool.loadUrlInTab(newTab.id, newTab.url, newTab.isIncognito)
                }
            }
        )

        // 4. Нижний менеджер вкладок (Карусель + Плиточная сетка)
        TabsManagerDrawer(
            state = state,
            fogPhase = fogPhase,
            extraDragOffsetPx = tabsLiveOffsetPx,
            onRequestExpandTab = { tab, index, bounds ->
                state.expandingCardState = CardExpandAnimState(
                    tabId = tab.id,
                    startBounds = bounds,
                    snapshot = tab.snapshot,
                    title = tab.title
                )
                state.switchToTab(index)
            },
            onRequestCloseTab = { index ->
                state.closeTabAt(index) { removedId ->
                    webViewPool.removeWebView(removedId)
                }
            }
        )

        // 5. Анимация раскрытия карточки вкладки на весь экран
        CardExpandOverlay(
            animState = state.expandingCardState,
            fogPhase = fogPhase,
            isIncognito = state.isIncognito,
            isMonoLogo = state.isMonoLogo,
            powerSave = state.effectivePowerSave,
            onAnimationFinished = {
                state.expandingCardState = null
            }
        )

        // 6. Индикатор натяжения жеста (#tension-indicator) с рассеянным туманным свечением
        if (tensionHeightDp > 0.5.dp) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp),
                contentAlignment = if (tensionIsTop) Alignment.TopCenter else Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(tensionWidthFraction)
                        .height(tensionHeightDp)
                        .diffusedFogAura(
                            phase = fogPhase,
                            isIncognito = state.isIncognito,
                            isMonoLogo = state.isMonoLogo,
                            enabled = !state.effectivePowerSave,
                            blurRadius = 16.dp,
                            spread = 6.dp,
                            alpha = 0.95f
                        )
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (state.isMonoLogo) colors.fg else Color.Transparent
                        )
                )
            }
        }

        // 7. Всплеск самого тумана (вместо колец shockwave)
        FogBurstOverlay(
            triggerCount = state.fogBurstTrigger,
            originYRatio = state.fogBurstOriginYRatio,
            phase = fogPhase,
            isIncognito = state.isIncognito,
            isMonoLogo = state.isMonoLogo,
            powerSave = state.effectivePowerSave
        )

        // 8. Меню сайта «Три точки»
        SiteMenuPopupModal(
            state = state,
            fogPhase = fogPhase,
            onTriggerDownloadDemo = ::triggerDownloadDemo,
            onApplyWebSettings = { webViewPool.applyDynamicSettingsToAll() }
        )

        // 9. Глобальные настройки браузера
        GlobalSettingsModal(
            state = state,
            fogPhase = fogPhase,
            onTriggerDownloadDemo = ::triggerDownloadDemo,
            onClearCacheAndCookies = { webViewPool.clearAllCacheAndCookies() },
            onApplyWebSettings = { webViewPool.applyDynamicSettingsToAll() }
        )

        // 10. Модальное окно Истории
        HistoryModal(
            state = state,
            fogPhase = fogPhase,
            onHistoryItemClick = { url ->
                state.currentTab?.let { webViewPool.captureTabSnapshot(it.id) }
                state.openInputOrUrl(url, createNewTab = true)
                state.currentTab?.let { newTab ->
                    webViewPool.loadUrlInTab(newTab.id, newTab.url, newTab.isIncognito)
                }
            }
        )

        // 11. Всплывающая капсула из 3 круглых иконок
        FloatingContextPillOverlay(
            pillState = state.contextPillState,
            fogPhase = fogPhase,
            isIncognito = state.isIncognito,
            isMonoLogo = state.isMonoLogo,
            powerSave = state.effectivePowerSave,
            onDismiss = { state.contextPillState = null }
        )

        // 12. Диалог подтверждения (ДА / ОТМЕНА) для деструктивных операций
        DestructiveConfirmDialog(
            dialogState = state.confirmDialogState,
            fogPhase = fogPhase,
            isIncognito = state.isIncognito,
            isMonoLogo = state.isMonoLogo,
            powerSave = state.effectivePowerSave,
            onDismiss = { state.confirmDialogState = null }
        )

        // 13. Компактный тост
        VectorToastOverlay(
            message = state.toastMessage,
            trigger = state.toastTrigger,
            onClear = { state.toastMessage = null }
        )
    }
}

/**
 * Главная страница (Zero Point):
 * - Логотип `slick` без пульсации масштаба.
 * - Поисковая капсула с рассеянным туманным свечением вокруг.
 * - Если текста нет — курсора нет вообще; когда текст есть — тонкий белый (или чёрный) курсор.
 * - Честное перемещение над системной клавиатурой (WindowInsets.ime) с запасом 26.dp, чтобы туман не обрезался.
 */
@Composable
fun ZeroPointHomeScreen(
    state: SlickBrowserState,
    fogPhase: Float,
    onDestroyIncognitoWebViews: () -> Unit,
    onSubmitSearch: (String) -> Unit
) {
    val colors = LocalSlickColors.current
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    var searchFieldValue by remember { mutableStateOf(TextFieldValue("")) }

    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val isImeVisible = imeBottomPx > 0
    val imeBottomDp = with(density) { imeBottomPx.toDp() }
    val isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Смещение центральной группы при открытии верхней панели закладок или нижней карусели вкладок
    val panelShiftY by animateDpAsState(
        targetValue = when (state.panelState) {
            UiPanelState.BOOKMARKS_BAR -> 72.dp
            UiPanelState.TABS_CAROUSEL -> (-110).dp
            else -> 0.dp
        },
        animationSpec = if (state.effectivePowerSave) tween(0) else spring(stiffness = Spring.StiffnessMediumLow),
        label = "zero_panel_shift"
    )

    // В альбомной ориентации с открытой клавиатурой логотип плавно скрывается (alpha = 0)
    val logoAlpha by animateFloatAsState(
        targetValue = if (isLandscape && isImeVisible) 0f else 1f,
        animationSpec = tween(220),
        label = "logo_landscape_alpha"
    )

    // Честный расчёт позиции поисковой капсулы:
    // В покое — по центру экрана. При открытой клавиатуре — опускается к клавиатуре с запасом 28.dp, чтобы туман не резался.
    val screenHeightDp = config.screenHeightDp.dp
    val targetBottomPadding = if (isImeVisible) {
        (imeBottomDp + 28.dp).coerceAtMost(screenHeightDp - 80.dp)
    } else {
        (screenHeightDp * 0.40f).coerceAtLeast(140.dp)
    }

    val animatedBottomPadding by animateDpAsState(
        targetValue = targetBottomPadding,
        animationSpec = if (state.effectivePowerSave) tween(0) else spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "search_bar_ime_offset"
    )

    val noPersonalizedIme = remember {
        PlatformImeOptions(privateImeOptions = "nm,com.google.android.inputmethod.latin.noMicrophoneKey")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset(y = panelShiftY)
            .padding(bottom = animatedBottomPadding, start = 22.dp, end = 22.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (logoAlpha > 0.02f) {
                SlickLiquidLogo(
                    phase = fogPhase,
                    isIncognito = state.isIncognito,
                    isMonoLogo = state.isMonoLogo,
                    powerSave = state.effectivePowerSave,
                    onOpenSettings = { state.isSettingsOpen = true },
                    onToggleIncognito = {
                        state.toggleIncognito(onDestroyIncognitoWebViews)
                    },
                    modifier = Modifier
                        .graphicsLayer { alpha = logoAlpha }
                        .padding(bottom = if (isImeVisible) 16.dp else 26.dp)
                )
            }

            // Поисковая капсула (#search-capsule) с мягким рассеянным туманным свечением (#search-aura)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .height(60.dp)
                    .diffusedFogAura(
                        phase = fogPhase,
                        isIncognito = state.isIncognito,
                        isMonoLogo = state.isMonoLogo,
                        enabled = !state.effectivePowerSave && !state.isMonoLogo,
                        blurRadius = 24.dp,
                        spread = 10.dp,
                        alpha = 0.88f
                    )
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.bg)
                    .border(2.dp, colors.fg, RoundedCornerShape(999.dp))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        state.setPanel(UiPanelState.IDLE)
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    }
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                val hasText = searchFieldValue.text.isNotEmpty()

                BasicTextField(
                    value = searchFieldValue,
                    onValueChange = { searchFieldValue = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = colors.fg
                    ),
                    // Если текста нет — курсора нет вообще; как только появляется текст — тонкий вертикальный курсор
                    cursorBrush = SolidColor(if (hasText) colors.fg else Color.Transparent),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrect = false,
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Search,
                        platformImeOptions = if (state.isIncognito) noPersonalizedIme else null
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            val q = searchFieldValue.text.trim()
                            if (q.isNotEmpty()) {
                                keyboardController?.hide()
                                onSubmitSearch(q)
                                searchFieldValue = TextFieldValue("")
                            }
                        },
                        onGo = {
                            val q = searchFieldValue.text.trim()
                            if (q.isNotEmpty()) {
                                keyboardController?.hide()
                                onSubmitSearch(q)
                                searchFieldValue = TextFieldValue("")
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }
        }
    }
}

/**
 * Активный сайт (#web-viewport) + Нижняя поисковая строка (#bottom-omni-strip):
 * - Масштабирование окна сайта со скруглёнными углами (32.dp) при открытии панелей закладок/вкладок.
 * - Горизонтальный свайп вкладок на нижней строке с Bitmap-заморозкой и фиксацией за 50 мс БЕЗ спам-тостов.
 * - Двухфазные обратимые вертикальные жесты (вверх -> Вкладки, вниз -> Закладки) прямо с нижней строки.
 * - Рассеянный градиентный туман прогресса загрузки вокруг нижней капсулы (линейный, без квадратичного ускорения).
 */
@Composable
fun ActiveWebViewportSection(
    state: SlickBrowserState,
    webViewPool: WebViewPoolManager,
    fogPhase: Float,
    onTriggerTension: (Boolean, androidx.compose.ui.unit.Dp, Float) -> Unit,
    onLivePanelDrag: (Float, Float) -> Unit
) {
    val colors = LocalSlickColors.current
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val inlineFocusRequester = remember { FocusRequester() }

    val screenHeightDp = config.screenHeightDp.dp
    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }

    val pool = state.activeTabsPool
    val activeIdx = state.activeTabIndex
    val currentTab = state.currentTab ?: return
    val prevTab = if (pool.isNotEmpty()) pool[(activeIdx - 1 + pool.size) % pool.size] else currentTab
    val nextTab = if (pool.isNotEmpty()) pool[(activeIdx + 1) % pool.size] else currentTab

    // Анимированные отступы и радиус скругления окна сайта при открытии панелей
    val topInset by animateDpAsState(
        targetValue = when (state.panelState) {
            UiPanelState.BOOKMARKS_BAR -> 176.dp
            UiPanelState.TABS_CAROUSEL -> 16.dp
            else -> 0.dp
        },
        animationSpec = if (state.effectivePowerSave) tween(0) else spring(stiffness = Spring.StiffnessMediumLow),
        label = "viewport_top_inset"
    )

    val bottomInset by animateDpAsState(
        targetValue = when (state.panelState) {
            UiPanelState.BOOKMARKS_BAR -> 76.dp
            UiPanelState.TABS_CAROUSEL -> screenHeightDp * 0.42f + 14.dp
            else -> 0.dp
        },
        animationSpec = if (state.effectivePowerSave) tween(0) else spring(stiffness = Spring.StiffnessMediumLow),
        label = "viewport_bottom_inset"
    )

    val sideInset by animateDpAsState(
        targetValue = if (state.panelState == UiPanelState.IDLE) 0.dp else 14.dp,
        animationSpec = if (state.effectivePowerSave) tween(0) else spring(stiffness = Spring.StiffnessMediumLow),
        label = "viewport_side_inset"
    )

    val viewportCorner by animateDpAsState(
        targetValue = if (state.panelState == UiPanelState.IDLE) 0.dp else 32.dp,
        animationSpec = if (state.effectivePowerSave) tween(0) else spring(stiffness = Spring.StiffnessMediumLow),
        label = "viewport_corner"
    )

    // Горизонтальное смещение слайдера вкладок (в пикселях)
    val sliderOffsetPx = remember { Animatable(0f) }
    var isHorizontalSliding by remember { mutableStateOf(false) }

    val grayscaleMatrix = remember {
        ColorMatrix().apply { setToSaturation(0f) }
    }

    // Линейная (мягкая lerp) интерполяция прогресса загрузки без квадратичного ускорения
    val smoothLoadProgress by animateFloatAsState(
        targetValue = if (currentTab.isLoading) currentTab.loadProgress.coerceIn(0.08f, 1f) else 1f,
        animationSpec = tween(durationMillis = 220),
        label = "linear_fog_progress"
    )

    val loadFogAlpha by animateFloatAsState(
        targetValue = if (currentTab.isLoading) 0.92f else 0f,
        animationSpec = tween(durationMillis = 340),
        label = "load_fog_alpha"
    )

    // Состояние редактирования URL по Long Press на нижнюю строку
    var inlineUrlField by remember(currentTab.url, state.isEditingBottomOmni) {
        mutableStateOf(
            TextFieldValue(
                text = currentTab.url,
                selection = TextRange(0, currentTab.url.length)
            )
        )
    }

    LaunchedEffect(state.isEditingBottomOmni) {
        if (state.isEditingBottomOmni) {
            delay(60L)
            inlineFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val imeBottomDp = with(density) { WindowInsets.ime.getBottom(density).toDp() }
    val omniBottomOffset = if (state.isEditingBottomOmni && imeBottomDp > 0.dp) {
        imeBottomDp + 18.dp
    } else {
        14.dp
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Окно сайта (#web-viewport)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = topInset,
                    bottom = bottomInset,
                    start = sideInset,
                    end = sideInset
                )
                .clip(RoundedCornerShape(viewportCorner))
                .background(colors.bg)
                .then(
                    if (state.panelState != UiPanelState.IDLE) {
                        Modifier.border(3.dp, colors.fg, RoundedCornerShape(viewportCorner))
                    } else {
                        Modifier
                    }
                )
        ) {
            // Предыдущая вкладка (слева, замороженный Ч/Б Bitmap)
            if (isHorizontalSliding) {
                FrozenTabSlideBox(
                    tab = prevTab,
                    grayscaleMatrix = grayscaleMatrix,
                    modifier = Modifier
                        .fillMaxSize()
                        .offset { IntOffset((sliderOffsetPx.value - screenWidthPx).roundToInt(), 0) }
                )
                // Следующая вкладка (справа, замороженный Ч/Б Bitmap)
                FrozenTabSlideBox(
                    tab = nextTab,
                    grayscaleMatrix = grayscaleMatrix,
                    modifier = Modifier
                        .fillMaxSize()
                        .offset { IntOffset((sliderOffsetPx.value + screenWidthPx).roundToInt(), 0) }
                )
            }

            // Активная центральная вкладка
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(sliderOffsetPx.value.roundToInt(), 0) }
                    .padding(bottom = if (state.panelState == UiPanelState.IDLE) 68.dp else 0.dp)
            ) {
                if (currentTab.errorCode != null) {
                    StrictNetworkErrorView(
                        errorCode = currentTab.errorCode,
                        onRetry = { webViewPool.reloadCurrentTab() }
                    )
                } else if (isHorizontalSliding && currentTab.snapshot != null && !currentTab.snapshot.isRecycled) {
                    // Во время свайпа замораживаем и активную вкладку в Ч/Б Bitmap
                    FrozenTabSlideBox(
                        tab = currentTab,
                        grayscaleMatrix = grayscaleMatrix,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    key(currentTab.id) {
                        AndroidView(
                            factory = {
                                val wv = webViewPool.getOrCreateWebView(
                                    tabId = currentTab.id,
                                    initialUrl = currentTab.url,
                                    isIncognito = currentTab.isIncognito
                                )
                                (wv.parent as? ViewGroup)?.removeView(wv)
                                wv
                            },
                            update = { wv ->
                                wv.isEnabled = state.panelState == UiPanelState.IDLE
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // Верхняя невидимая зона для свайпа сверху вниз (открытие Сохранённых закладок прямо поверх сайта)
        if (state.panelState == UiPanelState.IDLE) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .align(Alignment.TopCenter)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val startY = down.position.y
                            var holdJob: Job? = null
                            var opened = false

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (!change.pressed) break
                                val dy = change.position.y - startY

                                if (opened) {
                                    if (dy < 36.dp.toPx()) {
                                        onLivePanelDrag((dy - 64.dp.toPx()) * 1.4f, 0f)
                                        if (dy <= 6.dp.toPx()) {
                                            onLivePanelDrag(0f, 0f)
                                            state.setPanel(UiPanelState.IDLE)
                                            opened = false
                                        }
                                    }
                                    continue
                                }

                                if (dy > 38.dp.toPx()) {
                                    onTriggerTension(true, (dy * 0.05f).dp.coerceAtMost(12.dp), 0.55f)
                                    if (holdJob == null) {
                                        holdJob = scope.launch {
                                            delay(200L)
                                            onTriggerTension(true, 0.dp, 0.36f)
                                            opened = true
                                            webViewPool.captureTabSnapshot(currentTab.id)
                                            state.setPanel(UiPanelState.BOOKMARKS_BAR, 0.06f)
                                        }
                                    }
                                } else {
                                    holdJob?.cancel()
                                    holdJob = null
                                    onTriggerTension(true, 0.dp, 0.36f)
                                }
                            }
                            holdJob?.cancel()
                            onTriggerTension(true, 0.dp, 0.36f)
                            onLivePanelDrag(0f, 0f)
                        }
                    }
            )
        }

        // Нижняя поисковая строка на сайте (#bottom-omni-strip)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = omniBottomOffset, start = 16.dp, end = 16.dp)
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .height(52.dp)
                // Рассеянный туман прогресса загрузки вокруг нижней строки (без жёсткой контурной линии и без t^2)
                .diffusedFogAura(
                    phase = fogPhase,
                    isIncognito = state.isIncognito,
                    isMonoLogo = state.isMonoLogo,
                    enabled = loadFogAlpha > 0.02f && !state.effectivePowerSave,
                    blurRadius = 22.dp,
                    spread = 10.dp,
                    alpha = loadFogAlpha,
                    progressFraction = smoothLoadProgress
                )
                .clip(RoundedCornerShape(999.dp))
                .background(colors.bg)
                .border(2.dp, colors.fg, RoundedCornerShape(999.dp))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Слева: Кнопка обновления страницы с вращением на 360°
                ReloadSpinButton(
                    onReload = { webViewPool.reloadCurrentTab() }
                )

                // По центру: Универсальная зона жестов и домена
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 8.dp)
                        .pointerInput(state.isEditingBottomOmni, activeIdx, pool.size) {
                            if (state.isEditingBottomOmni) return@pointerInput

                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val startX = down.position.x
                                val startY = down.position.y
                                var lastMoveX = startX

                                var axisLocked = 0 // 0 = none, 1 = horizontal (tabs), 2 = vertical (panels)
                                var longPressTriggered = false
                                var tabHold50Ready = false
                                var phase1PanelOpened: UiPanelState? = null

                                var longPressJob: Job? = scope.launch {
                                    delay(480L)
                                    longPressTriggered = true
                                    state.closeAllPopups()
                                    state.isEditingBottomOmni = true
                                }
                                var tabHold50Job: Job? = null
                                var panelHold200Job: Job? = null

                                var totalDx = 0f
                                var totalDy = 0f

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
                                        longPressJob?.cancel()
                                        longPressJob = null
                                    }

                                    if (longPressTriggered) continue

                                    if (axisLocked == 0) {
                                        if (abs(dx) > 14.dp.toPx() && abs(dx) > abs(dy) * 1.15f) {
                                            axisLocked = 1
                                            webViewPool.captureTabSnapshot(currentTab.id)
                                            isHorizontalSliding = true
                                        } else if (abs(dy) > 14.dp.toPx() && abs(dy) > abs(dx) * 1.15f) {
                                            axisLocked = 2
                                        }
                                    }

                                    if (axisLocked == 1) {
                                        // Горизонтальное скольжение вкладок + фиксация за 50 мс БЕЗ спам-тостов
                                        change.consume()
                                        scope.launch {
                                            sliderOffsetPx.snapTo(dx)
                                        }

                                        if (abs(change.position.x - lastMoveX) > 3.5.dp.toPx()) {
                                            lastMoveX = change.position.x
                                            tabHold50Ready = false
                                            tabHold50Job?.cancel()
                                            if (abs(dx) > 48.dp.toPx()) {
                                                tabHold50Job = scope.launch {
                                                    delay(50L)
                                                    tabHold50Ready = true
                                                }
                                            }
                                        }
                                    } else if (axisLocked == 2) {
                                        // Двухфазные обратимые вертикальные жесты прямо с нижней строки сайта
                                        change.consume()

                                        if (phase1PanelOpened == UiPanelState.TABS_CAROUSEL) {
                                            if (dy > -34.dp.toPx()) {
                                                onLivePanelDrag(0f, (dy + 64.dp.toPx()) * 1.4f)
                                                if (dy >= -6.dp.toPx()) {
                                                    onLivePanelDrag(0f, 0f)
                                                    state.setPanel(UiPanelState.IDLE)
                                                    phase1PanelOpened = null
                                                }
                                            }
                                            continue
                                        }

                                        if (phase1PanelOpened == UiPanelState.BOOKMARKS_BAR) {
                                            if (dy < 34.dp.toPx()) {
                                                onLivePanelDrag((dy - 64.dp.toPx()) * 1.4f, 0f)
                                                if (dy <= 6.dp.toPx()) {
                                                    onLivePanelDrag(0f, 0f)
                                                    state.setPanel(UiPanelState.IDLE)
                                                    phase1PanelOpened = null
                                                }
                                            }
                                            continue
                                        }

                                        if (abs(dy) > 38.dp.toPx()) {
                                            val pullingDown = dy > 0f
                                            onTriggerTension(
                                                pullingDown,
                                                (abs(dy) * 0.05f).dp.coerceAtMost(12.dp),
                                                (0.36f + (abs(dy) / screenWidthPx) * 0.35f).coerceAtMost(0.76f)
                                            )
                                            if (panelHold200Job == null) {
                                                panelHold200Job = scope.launch {
                                                    delay(200L)
                                                    onTriggerTension(pullingDown, 0.dp, 0.36f)
                                                    webViewPool.captureTabSnapshot(currentTab.id)
                                                    if (pullingDown) {
                                                        phase1PanelOpened = UiPanelState.BOOKMARKS_BAR
                                                        state.setPanel(UiPanelState.BOOKMARKS_BAR, 0.06f)
                                                    } else {
                                                        phase1PanelOpened = UiPanelState.TABS_CAROUSEL
                                                        state.setPanel(UiPanelState.TABS_CAROUSEL, 0.94f)
                                                    }
                                                }
                                            }
                                        } else {
                                            panelHold200Job?.cancel()
                                            panelHold200Job = null
                                            onTriggerTension(false, 0.dp, 0.36f)
                                        }
                                    }
                                }

                                longPressJob?.cancel()
                                tabHold50Job?.cancel()
                                panelHold200Job?.cancel()
                                onTriggerTension(false, 0.dp, 0.36f)
                                onLivePanelDrag(0f, 0f)

                                if (longPressTriggered) return@awaitEachGesture

                                if (axisLocked == 1) {
                                    if (abs(totalDx) > 48.dp.toPx() && tabHold50Ready && pool.size > 1) {
                                        val targetOffset = if (totalDx < 0f) -screenWidthPx else screenWidthPx
                                        scope.launch {
                                            sliderOffsetPx.animateTo(
                                                targetValue = targetOffset,
                                                animationSpec = tween(220, easing = FastOutSlowInEasing)
                                            )
                                            val nextIdx = if (totalDx < 0f) activeIdx + 1 else activeIdx - 1
                                            state.updateActiveTabIndex(nextIdx)
                                            sliderOffsetPx.snapTo(0f)
                                            isHorizontalSliding = false
                                        }
                                    } else {
                                        scope.launch {
                                            sliderOffsetPx.animateTo(
                                                targetValue = 0f,
                                                animationSpec = spring(stiffness = Spring.StiffnessMedium)
                                            )
                                            isHorizontalSliding = false
                                        }
                                    }
                                } else if (axisLocked == 0 && abs(totalDx) < 12.dp.toPx() && abs(totalDy) < 12.dp.toPx()) {
                                    // Одиночный тап по центру нижней строки -> открытие Zero Point поверх сайта
                                    webViewPool.captureTabSnapshot(currentTab.id)
                                    state.closeAllPopups()
                                    state.setPanel(UiPanelState.IDLE)
                                    state.overlayZeroOnWeb = true
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isEditingBottomOmni) {
                        val hasInlineText = inlineUrlField.text.isNotEmpty()
                        BasicTextField(
                            value = inlineUrlField,
                            onValueChange = { inlineUrlField = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = colors.fg,
                                textAlign = TextAlign.Center
                            ),
                            cursorBrush = SolidColor(if (hasInlineText) colors.fg else Color.Transparent),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                autoCorrect = false,
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Go
                            ),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    val edited = inlineUrlField.text.trim()
                                    state.isEditingBottomOmni = false
                                    keyboardController?.hide()
                                    if (edited.isNotEmpty()) {
                                        state.openInputOrUrl(edited, createNewTab = false)
                                        state.currentTab?.let { updated ->
                                            webViewPool.loadUrlInTab(updated.id, updated.url, updated.isIncognito)
                                        }
                                    }
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(inlineFocusRequester)
                        )
                    } else {
                        Text(
                            text = currentTab.displayDomain,
                            color = colors.fg,
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Справа: Кнопка «Три точки»
                ThreeDotsButton(
                    onClick = {
                        state.isEditingBottomOmni = false
                        state.isSiteMenuOpen = !state.isSiteMenuOpen
                    }
                )
            }
        }
    }
}

@Composable
private fun FrozenTabSlideBox(
    tab: com.slick.browser.model.TabItem,
    grayscaleMatrix: ColorMatrix,
    modifier: Modifier = Modifier
) {
    val colors = LocalSlickColors.current
    Box(
        modifier = modifier
            .background(colors.bg)
            .padding(bottom = 68.dp),
        contentAlignment = Alignment.Center
    ) {
        val bmp = tab.snapshot
        if (bmp != null && !bmp.isRecycled) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = tab.title,
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.colorMatrix(grayscaleMatrix),
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = tab.title,
                    color = colors.fg,
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = tab.displayDomain,
                    color = colors.fgMuted,
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ReloadSpinButton(onReload: () -> Unit) {
    val colors = LocalSlickColors.current
    val scope = rememberCoroutineScope()
    val rotation = remember { Animatable(0f) }

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable {
                onReload()
                scope.launch {
                    rotation.snapTo(0f)
                    rotation.animateTo(
                        targetValue = 360f,
                        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing)
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(19.dp)
                .graphicsLayer { rotationZ = rotation.value }
        ) {
            val strokeWidth = 2.2.dp.toPx()
            drawArc(
                color = colors.fg,
                startAngle = 35f,
                sweepAngle = 285f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            val arrowPath = Path().apply {
                moveTo(size.width * 0.92f, size.height * 0.14f)
                lineTo(size.width * 0.92f, size.height * 0.44f)
                lineTo(size.width * 0.62f, size.height * 0.44f)
            }
            drawPath(
                path = arrowPath,
                color = colors.fg,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

@Composable
private fun ThreeDotsButton(onClick: () -> Unit) {
    val colors = LocalSlickColors.current
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(19.dp)) {
            val r = 1.7.dp.toPx()
            val cx = size.width / 2f
            drawCircle(color = colors.fg, radius = r, center = Offset(cx, size.height * 0.22f))
            drawCircle(color = colors.fg, radius = r, center = Offset(cx, size.height * 0.50f))
            drawCircle(color = colors.fg, radius = r, center = Offset(cx, size.height * 0.78f))
        }
    }
}
