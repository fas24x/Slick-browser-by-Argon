package com.slick.browser.model

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class SlickBrowserState(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("slick_browser_prefs_v2", Context.MODE_PRIVATE)

    // Глобальные настройки
    var searchEngine by mutableStateOf(
        SearchEngine.valueOf(prefs.getString("search_engine", SearchEngine.GOOGLE.name) ?: SearchEngine.GOOGLE.name)
    )
        private set

    var monochromeWeb by mutableStateOf(prefs.getBoolean("monochrome_web", true))
        private set

    var isLightTheme by mutableStateOf(prefs.getBoolean("light_theme", false))
        private set

    var isMonoLogo by mutableStateOf(prefs.getBoolean("mono_logo", false))
        private set

    var isPowerSaveManual by mutableStateOf(prefs.getBoolean("power_save_manual", false))
    var isSystemPowerSave by mutableStateOf(false)

    val effectivePowerSave: Boolean
        get() = isPowerSaveManual || isSystemPowerSave

    var isDesktopUa by mutableStateOf(prefs.getBoolean("desktop_ua", false))
        private set

    var textZoomPercent by mutableIntStateOf(prefs.getInt("text_zoom", 100))
        private set

    var storagePath by mutableStateOf(
        prefs.getString("storage_path", "/Download/Slick") ?: "/Download/Slick"
    )
        private set

    var isNetworkSimulatedOffline by mutableStateOf(false)
        private set

    // Инкогнито состояние
    var isIncognito by mutableStateOf(false)
        private set

    // Режимы отображения
    var inWebView by mutableStateOf(false)
    var overlayZeroOnWeb by mutableStateOf(false)
    var panelState by mutableStateOf(UiPanelState.IDLE)

    // Модальные окна
    var isSettingsOpen by mutableStateOf(false)
    var isSiteMenuOpen by mutableStateOf(false)
    var isHistoryOpen by mutableStateOf(false)
    var isEditingBottomOmni by mutableStateOf(false)

    // Контекстная таблетка из 3 иконок и диалог подтверждения Да/Нет
    var contextPillState by mutableStateOf<ContextPillState?>(null)
    var confirmDialogState by mutableStateOf<ConfirmDialogState?>(null)

    // Анимация раскрытия карточки на весь экран
    var expandingCardState by mutableStateOf<CardExpandAnimState?>(null)

    // Всплеск тумана (координаты и счётчик триггеров)
    var fogBurstTrigger by mutableIntStateOf(0)
        private set
    var fogBurstOriginYRatio by mutableFloatStateOf(0.5f)
        private set

    // Прогресс скачивания файла (в кнопке «Загрузки»)
    var downloadProgress by mutableFloatStateOf(0f)
    var downloadStatusText by mutableStateOf("0 МБ / 0 МБ")

    // Тост-уведомление
    var toastMessage by mutableStateOf<String?>(null)
    var toastTrigger by mutableIntStateOf(0)

    // Пулы вкладок: Обычный и Изолированный Инкогнито (в ОЗУ)
    val normalTabs = mutableStateListOf<TabItem>()
    val incognitoTabs = mutableStateListOf<TabItem>()

    var normalActiveIndex by mutableIntStateOf(0)
    var incognitoActiveIndex by mutableIntStateOf(0)

    val bookmarks = mutableStateListOf<BookmarkItem>()
    val historyLog = mutableStateListOf<HistoryItem>()

    val activeTabsPool: MutableList<TabItem>
        get() = if (isIncognito) incognitoTabs else normalTabs

    val activeTabIndex: Int
        get() = if (isIncognito) {
            incognitoActiveIndex.coerceIn(0, (incognitoTabs.size - 1).coerceAtLeast(0))
        } else {
            normalActiveIndex.coerceIn(0, (normalTabs.size - 1).coerceAtLeast(0))
        }

    val currentTab: TabItem?
        get() = activeTabsPool.getOrNull(activeTabIndex)

    init {
        loadSavedBookmarks()
        loadSavedHistory()
        initDefaultTabs()
    }

    private fun initDefaultTabs() {
        normalTabs.clear()
        normalTabs.addAll(
            listOf(
                TabItem(
                    id = "tab_1",
                    title = "GOOGLE // SEARCH",
                    url = "https://www.google.com",
                    snippetText = "Монохромное ядро второй ревизии. Все переходы подчиняются закону обратимости двухфазных жестов."
                ),
                TabItem(
                    id = "tab_2",
                    title = "WIKIPEDIA // RU",
                    url = "https://ru.m.wikipedia.org",
                    snippetText = "Свободная энциклопедия с аппаратной заморозкой Bitmap-снапшотов во время скольжения между вкладками."
                ),
                TabItem(
                    id = "tab_3",
                    title = "HACKER NEWS",
                    url = "https://news.ycombinator.com",
                    snippetText = "Чистая монохромная выдача без рекламного шума, вкладок-фильтров и цветных видео-превью."
                )
            )
        )
    }

    fun showToast(msg: String) {
        toastMessage = msg
        toastTrigger++
    }

    fun triggerFogBurst(originYRatio: Float = 0.5f) {
        if (!effectivePowerSave && !isMonoLogo) {
            fogBurstOriginYRatio = originYRatio
            fogBurstTrigger++
        }
    }

    fun closeAllPopups() {
        isSettingsOpen = false
        isSiteMenuOpen = false
        isHistoryOpen = false
        isEditingBottomOmni = false
        contextPillState = null
    }

    fun setPanel(newPanel: UiPanelState, burstYRatio: Float? = null) {
        val prev = panelState
        panelState = newPanel
        contextPillState = null
        if (prev != newPanel && (newPanel == UiPanelState.BOOKMARKS_BAR || newPanel == UiPanelState.TABS_CAROUSEL)) {
            val yRatio = burstYRatio ?: if (newPanel == UiPanelState.BOOKMARKS_BAR) 0.08f else 0.92f
            triggerFogBurst(yRatio)
        }
    }

    fun updateActiveTabIndex(newIndex: Int) {
        val pool = activeTabsPool
        if (pool.isEmpty()) return
        val clamped = ((newIndex % pool.size) + pool.size) % pool.size
        if (isIncognito) {
            incognitoActiveIndex = clamped
        } else {
            normalActiveIndex = clamped
        }
    }

    fun toggleIncognito(onDestroyIncognitoWebViews: () -> Unit) {
        isIncognito = !isIncognito
        if (isIncognito) {
            if (incognitoTabs.isEmpty()) {
                incognitoTabs.add(
                    TabItem(
                        id = "inc_${System.currentTimeMillis()}",
                        title = "INCOGNITO // SHIELD",
                        url = "https://duckduckgo.com",
                        isIncognito = true,
                        snippetText = "FLAG_SECURE и IME_FLAG_NO_PERSONALIZED_LEARNING включены. История и куки изолированы в ОЗУ."
                    )
                )
            }
            incognitoActiveIndex = 0
            showToast("ИНКОГНИТО // FLAG_SECURE ВКЛ")
        } else {
            incognitoTabs.clear()
            incognitoActiveIndex = 0
            onDestroyIncognitoWebViews()
            showToast("ИНКОГНИТО СЕССИЯ СТЁРТА")
        }
        triggerFogBurst(0.5f)
    }

    fun openInputOrUrl(rawQuery: String, createNewTab: Boolean = true) {
        val trimmed = rawQuery.trim()
        if (trimmed.isEmpty()) return
        val finalUrl = searchEngine.buildQueryUrl(trimmed)
        val displayTitle = trimmed.uppercase(Locale.getDefault()).take(36)

        val pool = activeTabsPool
        if (createNewTab || pool.isEmpty()) {
            val newTab = TabItem(
                id = "${if (isIncognito) "inc_" else "tab_"}${UUID.randomUUID()}",
                title = displayTitle,
                url = finalUrl,
                isIncognito = isIncognito,
                isLoading = true,
                loadProgress = 0.08f,
                snippetText = "Загрузка узла $finalUrl..."
            )
            pool.add(0, newTab)
            updateActiveTabIndex(0)
        } else {
            val idx = activeTabIndex
            pool[idx] = pool[idx].copy(
                title = displayTitle,
                url = finalUrl,
                isLoading = true,
                loadProgress = 0.08f,
                errorCode = null
            )
        }

        if (!isIncognito) {
            recordHistory(displayTitle, finalUrl)
        }

        inWebView = true
        overlayZeroOnWeb = false
        panelState = UiPanelState.IDLE
        closeAllPopups()
    }

    fun updateTabState(
        tabId: String,
        title: String? = null,
        url: String? = null,
        isLoading: Boolean? = null,
        progress: Float? = null,
        errorCode: String? = null,
        clearError: Boolean = false,
        snapshot: Bitmap? = null
    ) {
        val list = if (tabId.startsWith("inc_")) incognitoTabs else normalTabs
        val idx = list.indexOfFirst { it.id == tabId }
        if (idx == -1) return
        val current = list[idx]
        list[idx] = current.copy(
            title = if (!title.isNullOrBlank()) title.uppercase(Locale.getDefault()) else current.title,
            url = url ?: current.url,
            isLoading = isLoading ?: current.isLoading,
            loadProgress = progress ?: current.loadProgress,
            errorCode = if (clearError) null else (errorCode ?: current.errorCode),
            snapshot = snapshot ?: current.snapshot
        )
    }

    fun closeTabAt(index: Int, onRemoveWebView: (String) -> Unit) {
        val pool = activeTabsPool
        if (pool.size <= 1) {
            showToast("ПОСЛЕДНЯЯ ВКЛАДКА")
            return
        }
        val removed = pool.removeAt(index)
        onRemoveWebView(removed.id)
        if (activeTabIndex >= pool.size) {
            updateActiveTabIndex(pool.size - 1)
        }
        showToast("ВКЛАДКА ЗАКРЫТА")
    }

    fun switchToTab(index: Int) {
        updateActiveTabIndex(index)
        inWebView = true
        overlayZeroOnWeb = false
        panelState = UiPanelState.IDLE
        closeAllPopups()
    }

    // Закладки + Drag & Drop
    fun addBookmark(title: String, url: String) {
        if (bookmarks.any { it.url.equals(url, ignoreCase = true) }) {
            showToast("УЖЕ В СОХРАНЁННЫХ")
            return
        }
        val cleanLetters = title.replace(Regex("[^a-zA-Zа-яА-Я0-9]"), "").take(2).uppercase(Locale.getDefault())
        val icon = cleanLetters.ifEmpty { "WB" }
        bookmarks.add(
            0,
            BookmarkItem(
                id = "bm_${System.currentTimeMillis()}",
                iconCode = icon,
                title = title,
                url = url
            )
        )
        saveBookmarks()
        showToast("ДОБАВЛЕНО В СОХРАНЁННЫЕ")
    }

    fun requestDeleteBookmark(bookmark: BookmarkItem) {
        confirmDialogState = ConfirmDialogState(
            title = "УДАЛЕНИЕ ЗАКЛАДКИ",
            message = "Удалить «${bookmark.title}» из сохранённых закладок?",
            onConfirm = {
                bookmarks.removeAll { it.id == bookmark.id }
                saveBookmarks()
                showToast("УДАЛЕНО ИЗ СОХРАНЁННЫХ")
            }
        )
    }

    fun moveBookmark(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in bookmarks.indices || toIndex !in bookmarks.indices || fromIndex == toIndex) return
        val item = bookmarks.removeAt(fromIndex)
        bookmarks.add(toIndex, item)
        saveBookmarks()
    }

    // История и подтверждение очистки
    fun recordHistory(title: String, url: String) {
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        historyLog.removeAll { it.url == url }
        historyLog.add(
            0,
            HistoryItem(
                id = "h_${System.currentTimeMillis()}",
                title = title,
                url = url,
                timeFormatted = timeStr
            )
        )
        if (historyLog.size > 100) {
            historyLog.removeLast()
        }
        saveHistory()
    }

    fun requestClearHistory() {
        confirmDialogState = ConfirmDialogState(
            title = "ОЧИСТКА ИСТОРИИ",
            message = "Очистить всю историю переходов браузера без возможности возврата?",
            onConfirm = {
                historyLog.clear()
                saveHistory()
                showToast("ИСТОРИЯ ОЧИЩЕНА")
            }
        )
    }

    fun requestClearCacheAndCookies(onClearAction: () -> Unit) {
        confirmDialogState = ConfirmDialogState(
            title = "СБРОС КЭША И КУКИ",
            message = "Стереть кэш, локальное хранилище и все куки сайтов?",
            onConfirm = {
                onClearAction()
                showToast("КЭШ И КУКИ ОЧИЩЕНЫ")
            }
        )
    }

    // Переключатели настроек
    fun selectSearchEngine(engine: SearchEngine) {
        searchEngine = engine
        prefs.edit().putString("search_engine", engine.name).apply()
        showToast("ПОИСКОВИК: ${engine.label}")
    }

    fun toggleMonochromeWeb() {
        monochromeWeb = !monochromeWeb
        prefs.edit().putBoolean("monochrome_web", monochromeWeb).apply()
    }

    fun toggleNetworkSimulation() {
        isNetworkSimulatedOffline = !isNetworkSimulatedOffline
        showToast(if (isNetworkSimulatedOffline) "СИМУЛЯЦИЯ: ОФФЛАЙН" else "СИМУЛЯЦИЯ: ОНЛАЙН")
    }

    fun toggleTheme() {
        isLightTheme = !isLightTheme
        prefs.edit().putBoolean("light_theme", isLightTheme).apply()
    }

    fun toggleLogoMode() {
        isMonoLogo = !isMonoLogo
        prefs.edit().putBoolean("mono_logo", isMonoLogo).apply()
    }

    fun toggleStoragePath() {
        storagePath = if (storagePath.contains("SDCard")) "/Download/Slick" else "/SDCard/Slick"
        prefs.edit().putString("storage_path", storagePath).apply()
        showToast("ПУТЬ: $storagePath")
    }

    fun togglePowerSaveManual() {
        isPowerSaveManual = !isPowerSaveManual
        prefs.edit().putBoolean("power_save_manual", isPowerSaveManual).apply()
        showToast(if (effectivePowerSave) "ЭНЕРГОСБЕРЕЖЕНИЕ: ВКЛ" else "ПОЛНАЯ ЧАСТОТА 120 FPS")
    }

    fun toggleDesktopUa() {
        isDesktopUa = !isDesktopUa
        prefs.edit().putBoolean("desktop_ua", isDesktopUa).apply()
        showToast(if (isDesktopUa) "ВЕРСИЯ ДЛЯ ПК: ВКЛ" else "МОБИЛЬНЫЙ ВИД")
    }

    fun updateTextZoom(newZoom: Int) {
        textZoomPercent = newZoom.coerceIn(30, 200)
        prefs.edit().putInt("text_zoom", textZoomPercent).apply()
    }

    private fun loadSavedBookmarks() {
        val raw = prefs.getString("saved_bookmarks_json", null)
        bookmarks.clear()
        if (raw.isNullOrBlank()) {
            bookmarks.addAll(
                listOf(
                    BookmarkItem("b1", "GH", "GitHub", "https://github.com"),
                    BookmarkItem("b2", "HN", "HackerNews", "https://news.ycombinator.com"),
                    BookmarkItem("b3", "AR", "ArXiv", "https://arxiv.org"),
                    BookmarkItem("b4", "WK", "Wikipedia", "https://ru.wikipedia.org"),
                    BookmarkItem("b5", "MD", "MDN Docs", "https://developer.mozilla.org"),
                    BookmarkItem("b6", "DD", "DuckDuckGo", "https://duckduckgo.com")
                )
            )
            saveBookmarks()
            return
        }
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                bookmarks.add(
                    BookmarkItem(
                        id = obj.optString("id", "bm_$i"),
                        iconCode = obj.optString("icon", "WB"),
                        title = obj.optString("title", "Site"),
                        url = obj.optString("url", "https://google.com")
                    )
                )
            }
        } catch (_: Exception) {
        }
    }

    private fun saveBookmarks() {
        val arr = JSONArray()
        bookmarks.forEach { bm ->
            val obj = JSONObject()
            obj.put("id", bm.id)
            obj.put("icon", bm.iconCode)
            obj.put("title", bm.title)
            obj.put("url", bm.url)
            arr.put(obj)
        }
        prefs.edit().putString("saved_bookmarks_json", arr.toString()).apply()
    }

    private fun loadSavedHistory() {
        val raw = prefs.getString("saved_history_json", null) ?: return
        historyLog.clear()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                historyLog.add(
                    HistoryItem(
                        id = obj.optString("id", "h_$i"),
                        title = obj.optString("title", "NODE"),
                        url = obj.optString("url", "https://google.com"),
                        timeFormatted = obj.optString("time", "12:00")
                    )
                )
            }
        } catch (_: Exception) {
        }
    }

    private fun saveHistory() {
        val arr = JSONArray()
        historyLog.forEach { h ->
            val obj = JSONObject()
            obj.put("id", h.id)
            obj.put("title", h.title)
            obj.put("url", h.url)
            obj.put("time", h.timeFormatted)
            arr.put(obj)
        }
        prefs.edit().putString("saved_history_json", arr.toString()).apply()
    }
}
