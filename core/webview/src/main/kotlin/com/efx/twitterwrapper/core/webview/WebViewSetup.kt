package com.efx.twitterwrapper.core.webview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView

/**
 * One place for every WebView setting. Kept free of Compose and of the host
 * application's settings model: the caller passes plain values.
 */
object WebViewSetup {

    const val HOME_URL = "https://x.com/"

    private val CHROME_VERSION = Regex("""Chrome/([0-9][0-9.]*)""")

    @SuppressLint("SetJavaScriptEnabled")
    fun configure(webView: WebView, desktopMode: Boolean) {
        webView.setBackgroundColor(Color.BLACK)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            setGeolocationEnabled(true)
            allowFileAccess = false
            // Content access stays on: the SAF picker returns content:// URIs
            // that the WebView must read for <input type="file"> uploads.
            allowContentAccess = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            setSupportMultipleWindows(false)
            useWideViewPort = desktopMode
            loadWithOverviewMode = desktopMode
            builtInZoomControls = false
            displayZoomControls = false
        }

        applyUserAgent(webView, desktopMode)

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }
    }

    /** Applies the user agent for the selected site mode. Needs a reload to take effect. */
    fun applySiteMode(webView: WebView, desktopMode: Boolean) {
        applyUserAgent(webView, desktopMode)
        webView.settings.useWideViewPort = desktopMode
        webView.settings.loadWithOverviewMode = desktopMode
    }

    private fun applyUserAgent(webView: WebView, desktopMode: Boolean) {
        webView.settings.userAgentString = if (desktopMode) {
            desktopUserAgent(webView.context)
        } else {
            mobileUserAgent(webView.context)
        }
    }

    /**
     * Blink marks the WebView with "wv"; dropping the token avoids a class of
     * site-side WebView checks without changing the engine.
     */
    private fun mobileUserAgent(context: Context): String =
        WebSettings.getDefaultUserAgent(context).replace("; wv", "")

    /**
     * Desktop UA that keeps the real Chrome version of the installed WebView,
     * instead of a hard-coded version that goes stale and gets blocked.
     */
    private fun desktopUserAgent(context: Context): String {
        val chromeVersion = CHROME_VERSION
            .find(WebSettings.getDefaultUserAgent(context))
            ?.groupValues?.getOrNull(1)
            ?: "124.0.0.0"

        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/$chromeVersion Safari/537.36"
    }

    /** Hosts that must stay inside the WebView instead of opening a browser. */
    fun isInternalHost(host: String?): Boolean {
        val normalized = host?.lowercase() ?: return false
        return INTERNAL_DOMAINS.any { domain ->
            normalized == domain || normalized.endsWith(".$domain")
        }
    }

    private val INTERNAL_DOMAINS = listOf(
        "x.com",
        "twitter.com",
        "t.co",
        "twimg.com",
        "x.ai",
    )
}
