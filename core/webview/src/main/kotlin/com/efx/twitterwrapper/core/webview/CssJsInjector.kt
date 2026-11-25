package com.efx.twitterwrapper.core.webview

import android.webkit.WebView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.json.JSONObject

/**
 * Injects user CSS/JS into the page.
 *
 * A tiny, data-free bootstrap script is registered once with
 * `DOCUMENT_START_SCRIPT`, so it also runs on full navigations. The actual CSS
 * is pushed from native code and re-applied by the bootstrap when X's React
 * tree replaces DOM nodes. User JS runs once per loaded URL.
 */
class CssJsInjector(private val webView: WebView) {

    private var css: String = ""
    private var js: String = ""
    private var bootstrapRequested = false
    private var lastJsUrl: String? = null

    fun setInjection(css: String, js: String) {
        this.css = css
        this.js = js
        this.lastJsUrl = null
        installBootstrap()
    }

    /** Called on every main frame commit. */
    fun applyOnPage(url: String?) {
        installBootstrap()
        applyCss()
        if (js.isNotBlank() && url != lastJsUrl) {
            lastJsUrl = url
            evaluate(js)
        }
    }

    private fun installBootstrap() {
        if (bootstrapRequested) return
        bootstrapRequested = true
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) return

        runCatching {
            WebViewCompat.addDocumentStartJavaScript(webView, BOOTSTRAP_SCRIPT, ALLOWED_ORIGINS)
        }
    }

    private fun applyCss() {
        val quotedCss = JSONObject.quote(css)
        evaluate(
            "(function(){window.__efxCss=$quotedCss;" +
                "if(window.__efxApplyCss){window.__efxApplyCss();}})();",
        )
    }

    private fun evaluate(script: String) {
        runCatching { webView.evaluateJavascript(script, null) }
    }

    private companion object {
        val ALLOWED_ORIGINS = setOf(
            "https://x.com",
            "https://*.x.com",
            "https://twitter.com",
            "https://*.twitter.com",
        )

        val BOOTSTRAP_SCRIPT = """
            (function () {
              if (window.__efxWrapperInstalled) return;
              window.__efxWrapperInstalled = true;
              window.__efxCss = window.__efxCss || '';

              function applyCss() {
                var head = document.head || document.documentElement;
                if (!head) return;
                var style = document.getElementById('__efx_user_style');
                if (!style) {
                  style = document.createElement('style');
                  style.id = '__efx_user_style';
                  head.appendChild(style);
                }
                if (style.textContent !== window.__efxCss) {
                  style.textContent = window.__efxCss;
                }
              }

              window.__efxApplyCss = applyCss;

              function watch(head) {
                try {
                  new MutationObserver(function () {
                    if (!document.getElementById('__efx_user_style')) applyCss();
                  }).observe(head, { childList: true, subtree: true });
                } catch (ignored) {}
              }

              if (document.head) watch(document.head);
              document.addEventListener('DOMContentLoaded', function () {
                applyCss();
                if (document.head) watch(document.head);
              });
              applyCss();
            })();
        """.trimIndent()
    }
}
