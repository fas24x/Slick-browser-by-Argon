package com.slick.browser.web

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import com.slick.browser.model.MonochromeWebInjector
import com.slick.browser.model.SlickBrowserState

class WebViewPoolManager(
    private val context: Context,
    private val browserState: SlickBrowserState
) {
    private val normalPool = mutableMapOf<String, WebView>()
    private val incognitoPool = mutableMapOf<String, WebView>()

    private val defaultMobileUserAgent: String by lazy {
        try {
            WebSettings.getDefaultUserAgent(context)
        } catch (e: Exception) {
            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
        }
    }

    private val desktopUserAgent =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

    fun getOrCreateWebView(tabId: String, initialUrl: String, isIncognito: Boolean): WebView {
        val targetPool = if (isIncognito) incognitoPool else normalPool
        val existing = targetPool[tabId]
        if (existing != null) {
            applyDynamicSettings(existing, isIncognito)
            return existing
        }

        val created = createConfiguredWebView(tabId, isIncognito)
        targetPool[tabId] = created
        if (browserState.isNetworkSimulatedOffline) {
            browserState.updateTabState(
                tabId = tabId,
                isLoading = false,
                progress = 0f,
                errorCode = "ERR_INTERNET_DISCONNECTED"
            )
        } else {
            created.loadUrl(initialUrl)
        }
        return created
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createConfiguredWebView(tabId: String, isIncognito: Boolean): WebView {
        return WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isFocusable = true
            isFocusableInTouchMode = true

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = !isIncognito
                databaseEnabled = !isIncognito
                saveFormData = false
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                useWideViewPort = true
                loadWithOverviewMode = true
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                cacheMode = if (isIncognito) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    safeBrowsingEnabled = true
                }
            }

            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, !isIncognito)

            applyDynamicSettings(this, isIncognito)

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    if (browserState.isNetworkSimulatedOffline) {
                        view?.stopLoading()
                        browserState.updateTabState(
                            tabId = tabId,
                            url = url,
                            isLoading = false,
                            progress = 0f,
                            errorCode = "ERR_INTERNET_DISCONNECTED"
                        )
                        return
                    }
                    browserState.updateTabState(
                        tabId = tabId,
                        url = url,
                        isLoading = true,
                        progress = 0.1f,
                        clearError = true
                    )
                    if (view != null) {
                        injectMonochromeStyleIfNeeded(view)
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (view != null) {
                        injectMonochromeStyleIfNeeded(view)
                        val pageTitle = view.title?.takeIf { it.isNotBlank() } ?: url ?: "SLICK NODE"
                        browserState.updateTabState(
                            tabId = tabId,
                            title = pageTitle,
                            url = url,
                            isLoading = false,
                            progress = 1f
                        )
                        if (!isIncognito && !url.isNullOrBlank() && !url.startsWith("about:")) {
                            browserState.recordHistory(pageTitle, url)
                        }
                        view.postDelayed({
                            captureTabSnapshot(tabId)
                        }, 180L)
                    }
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    if (request?.isForMainFrame == true) {
                        val mappedCode = mapWebErrorCode(error?.errorCode ?: ERROR_UNKNOWN)
                        browserState.updateTabState(
                            tabId = tabId,
                            isLoading = false,
                            progress = 0f,
                            errorCode = mappedCode
                        )
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    val normProgress = (newProgress / 100f).coerceIn(0.05f, 1f)
                    val stillLoading = newProgress < 100
                    browserState.updateTabState(
                        tabId = tabId,
                        isLoading = stillLoading,
                        progress = normProgress
                    )
                    if (view != null && newProgress >= 40) {
                        injectMonochromeStyleIfNeeded(view)
                    }
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    super.onReceivedTitle(view, title)
                    if (!title.isNullOrBlank()) {
                        browserState.updateTabState(
                            tabId = tabId,
                            title = title
                        )
                    }
                }
            }
        }
    }

    private fun mapWebErrorCode(code: Int): String {
        return when (code) {
            WebViewClient.ERROR_HOST_LOOKUP -> "ERR_NAME_NOT_RESOLVED"
            WebViewClient.ERROR_CONNECT -> "ERR_INTERNET_DISCONNECTED"
            WebViewClient.ERROR_TIMEOUT -> "ERR_CONNECTION_TIMED_OUT"
            WebViewClient.ERROR_SSL_HANDSHAKE -> "ERR_SSL_PROTOCOL_ERROR"
            WebViewClient.ERROR_UNSUPPORTED_SCHEME -> "ERR_UNKNOWN_URL_SCHEME"
            else -> "ERR_INTERNET_DISCONNECTED"
        }
    }

    fun applyDynamicSettingsToAll() {
        (normalPool.values + incognitoPool.values).forEach { wv ->
            val isInc = incognitoPool.containsValue(wv)
            applyDynamicSettings(wv, isInc)
            injectMonochromeStyleIfNeeded(wv)
        }
    }

    private fun applyDynamicSettings(webView: WebView, isIncognito: Boolean) {
        webView.settings.apply {
            textZoom = browserState.textZoomPercent
            userAgentString = if (browserState.isDesktopUa) desktopUserAgent else defaultMobileUserAgent
            cacheMode = if (isIncognito) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
        }
    }

    private fun injectMonochromeStyleIfNeeded(webView: WebView) {
        val script = if (browserState.monochromeWeb) {
            MonochromeWebInjector.JS_INJECTOR
        } else {
            MonochromeWebInjector.JS_REMOVE_INJECTOR
        }
        webView.evaluateJavascript(script, null)
    }

    fun loadUrlInTab(tabId: String, url: String, isIncognito: Boolean) {
        val wv = getOrCreateWebView(tabId, url, isIncognito)
        if (browserState.isNetworkSimulatedOffline) {
            wv.stopLoading()
            browserState.updateTabState(
                tabId = tabId,
                url = url,
                isLoading = false,
                progress = 0f,
                errorCode = "ERR_INTERNET_DISCONNECTED"
            )
        } else {
            browserState.updateTabState(
                tabId = tabId,
                url = url,
                isLoading = true,
                progress = 0.08f,
                clearError = true
            )
            wv.loadUrl(url)
        }
    }

    fun reloadCurrentTab() {
        val tab = browserState.currentTab ?: return
        captureTabSnapshot(tab.id)
        val pool = if (tab.isIncognito) incognitoPool else normalPool
        val wv = pool[tab.id]
        if (browserState.isNetworkSimulatedOffline) {
            wv?.stopLoading()
            browserState.updateTabState(
                tabId = tab.id,
                isLoading = false,
                progress = 0f,
                errorCode = "ERR_INTERNET_DISCONNECTED"
            )
            return
        }
        browserState.updateTabState(
            tabId = tab.id,
            isLoading = true,
            progress = 0.1f,
            clearError = true
        )
        if (wv != null) {
            wv.reload()
        } else {
            getOrCreateWebView(tab.id, tab.url, tab.isIncognito)
        }
    }

    fun captureTabSnapshot(tabId: String): Bitmap? {
        val wv = normalPool[tabId] ?: incognitoPool[tabId] ?: return null
        if (wv.width <= 0 || wv.height <= 0) return null
        return try {
            val scale = 0.45f
            val bmpW = (wv.width * scale).toInt().coerceAtLeast(1)
            val bmpH = (wv.height * scale).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.scale(scale, scale)
            wv.draw(canvas)
            browserState.updateTabState(tabId = tabId, snapshot = bitmap)
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    fun removeWebView(tabId: String) {
        val wv = normalPool.remove(tabId) ?: incognitoPool.remove(tabId)
        wv?.let {
            (it.parent as? ViewGroup)?.removeView(it)
            it.stopLoading()
            it.clearHistory()
            it.removeAllViews()
            it.destroy()
        }
    }

    fun destroyIncognitoSession() {
        incognitoPool.values.forEach { wv ->
            (wv.parent as? ViewGroup)?.removeView(wv)
            wv.stopLoading()
            wv.clearCache(true)
            wv.clearFormData()
            wv.clearHistory()
            wv.removeAllViews()
            wv.destroy()
        }
        incognitoPool.clear()
        CookieManager.getInstance().removeSessionCookies(null)
        CookieManager.getInstance().flush()
    }

    fun clearAllCacheAndCookies() {
        (normalPool.values + incognitoPool.values).forEach { wv ->
            wv.clearCache(true)
            wv.clearFormData()
            wv.clearHistory()
        }
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        WebStorage.getInstance().deleteAllData()
    }

    fun canGoBackOnActiveTab(): Boolean {
        val tab = browserState.currentTab ?: return false
        val wv = (if (tab.isIncognito) incognitoPool else normalPool)[tab.id] ?: return false
        return wv.canGoBack()
    }

    fun goBackOnActiveTab() {
        val tab = browserState.currentTab ?: return
        val wv = (if (tab.isIncognito) incognitoPool else normalPool)[tab.id] ?: return
        if (wv.canGoBack()) {
            wv.goBack()
        }
    }
}
