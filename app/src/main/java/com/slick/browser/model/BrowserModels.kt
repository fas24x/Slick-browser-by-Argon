package com.slick.browser.model

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Rect
import java.net.URLEncoder

enum class SearchEngine(val code: String, val label: String, val searchUrlPrefix: String) {
    GOOGLE("GGL", "GOOGLE", "https://www.google.com/search?q="),
    YANDEX("YND", "YANDEX", "https://ya.ru/search/?text="),
    DUCKGO("DDG", "DUCKGO", "https://duckduckgo.com/?q=");

    fun buildQueryUrl(rawInput: String): String {
        val trimmed = rawInput.trim()
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("slick://", ignoreCase = true)
        ) {
            return trimmed
        }
        val looksLikeDomain = trimmed.contains('.') &&
            !trimmed.contains(' ') &&
            trimmed.substringAfterLast('.').length in 2..12
        return if (looksLikeDomain) {
            "https://$trimmed"
        } else {
            val encoded = try {
                URLEncoder.encode(trimmed, "UTF-8")
            } catch (e: Exception) {
                trimmed.replace(" ", "+")
            }
            "$searchUrlPrefix$encoded"
        }
    }
}

enum class UiPanelState {
    IDLE,
    BOOKMARKS_BAR,
    TABS_CAROUSEL,
    TABS_GRID
}

data class TabItem(
    val id: String,
    val title: String,
    val url: String,
    val isIncognito: Boolean = false,
    val isLoading: Boolean = false,
    val loadProgress: Float = 0f,
    val errorCode: String? = null,
    val snapshot: Bitmap? = null,
    val snippetText: String = "SLICK CORE // MONOCHROME NODE"
) {
    val displayDomain: String
        get() = url
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .substringBefore('/')
            .ifBlank { url }
}

data class BookmarkItem(
    val id: String,
    val iconCode: String,
    val title: String,
    val url: String
)

data class HistoryItem(
    val id: String,
    val title: String,
    val url: String,
    val timeFormatted: String
)

enum class ContextPillIcon {
    BOOKMARK_ADD,
    BOOKMARK_DELETE,
    COPY_URL,
    SHARE_URL
}

data class ContextPillAction(
    val icon: ContextPillIcon,
    val onClick: () -> Unit
)

data class ContextPillState(
    val centerX: Float,
    val topY: Float,
    val actions: List<ContextPillAction>
)

data class ConfirmDialogState(
    val title: String,
    val message: String,
    val confirmText: String = "ДА",
    val cancelText: String = "ОТМЕНА",
    val onConfirm: () -> Unit
)

data class CardExpandAnimState(
    val tabId: String,
    val startBounds: Rect,
    val snapshot: Bitmap?,
    val title: String
)

object MonochromeWebInjector {
    val JS_INJECTOR: String = """
        (function() {
            var styleId = 'slick-monochrome-core-style';
            var existing = document.getElementById(styleId);
            if (!existing) {
                var style = document.createElement('style');
                style.id = styleId;
                style.innerHTML = `
                    html {
                        filter: grayscale(100%) contrast(115%) !important;
                        -webkit-filter: grayscale(100%) contrast(115%) !important;
                    }
                    /* Скрытие лишних табов-фильтров поисковиков (Карты, Товары, Картинки, Новости, Видео) */
                    div[role="navigation"] a[href*="tbm="],
                    div[role="navigation"] a[href*="udm="],
                    div[role="list"] a[href*="tbm="],
                    .HeaderNav, .navigation__region, .ServiceTabs,
                    .zItAnd, .O3S9Rb, .IUOThf, .crJ18e {
                        display: none !important;
                    }
                    video, iframe[src*="youtube"], iframe[src*="vimeo"] {
                        border: 2px solid currentColor !important;
                        border-radius: 16px !important;
                    }
                `;
                if (document.head) {
                    document.head.appendChild(style);
                } else if (document.documentElement) {
                    document.documentElement.appendChild(style);
                }
            }
            /* Дополнительная фильтрация текстовых табов в поисковиках */
            var forbiddenTabs = ['картинки', 'видео', 'новости', 'карты', 'товары', 'покупки', 'images', 'videos', 'news', 'maps', 'shopping'];
            var links = document.querySelectorAll('div[role="navigation"] a, nav a, header a');
            for (var i = 0; i < links.length; i++) {
                var txt = (links[i].innerText || '').trim().toLowerCase();
                if (forbiddenTabs.indexOf(txt) !== -1) {
                    links[i].style.display = 'none';
                }
            }
        })();
    """.trimIndent()

    val JS_REMOVE_INJECTOR: String = """
        (function() {
            var existing = document.getElementById('slick-monochrome-core-style');
            if (existing) existing.remove();
        })();
    """.trimIndent()
}
