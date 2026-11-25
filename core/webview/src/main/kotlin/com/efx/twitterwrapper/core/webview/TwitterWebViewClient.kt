package com.efx.twitterwrapper.core.webview

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.http.SslError
import android.util.Log
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * URL and error policy for the wrapper.
 *
 * Internal hosts stay in the WebView, everything else goes to the system
 * browser, and unknown schemes are handed to the OS with a graceful fallback.
 */
class TwitterWebViewClient(
    private val context: Context,
    private val callbacks: WebViewCallbacks,
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(webView: WebView, request: WebResourceRequest): Boolean {
        if (!request.isForMainFrame) return false
        return handleUri(webView.context, request.url)
    }

    @Deprecated("Required for WebViews that predate WebResourceRequest")
    override fun shouldOverrideUrlLoading(webView: WebView, url: String): Boolean =
        handleUri(webView.context, Uri.parse(url))

    private fun handleUri(context: Context, uri: Uri): Boolean {
        return when (val scheme = uri.scheme?.lowercase()) {
            "http", "https" -> {
                if (WebViewSetup.isInternalHost(uri.host)) {
                    false
                } else {
                    openExternally(context, uri)
                    true
                }
            }

            "intent" -> {
                openIntentUrl(context, uri)
                true
            }

            // Schemes the WebView must keep handling itself.
            "about", "data", "javascript", "blob", "file", "content" -> false

            // System handlers (mail, phone, maps, app deep links...).
            null -> false
            else -> {
                if (scheme.length <= 32) openExternally(context, uri)
                true
            }
        }
    }

    private fun openIntentUrl(context: Context, uri: Uri) {
        val intent = runCatching { Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME) }
            .getOrNull() ?: return

        val resolved = resolveActivity(context, intent)
        if (resolved != null) {
            runCatching { context.startActivity(resolved) }
                .onFailure { openExternalFallback(context, intent) }
        } else {
            openExternalFallback(context, intent)
        }
    }

    private fun resolveActivity(context: Context, intent: Intent): Intent? {
        val canResolve = runCatching {
            context.packageManager.resolveActivity(intent, 0)
        }.getOrNull() != null
        return if (canResolve) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) else null
    }

    private fun openExternalFallback(context: Context, intent: Intent) {
        val fallback = intent.getStringExtra("browser_fallback_url")
        if (!fallback.isNullOrBlank()) openExternally(context, Uri.parse(fallback))
    }

    override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
        super.onPageStarted(view, url, favicon)
        callbacks.onPageStarted(url)
        callbacks.onCanGoBackChanged(view.canGoBack())
    }

    override fun onPageCommitVisible(view: WebView, url: String?) {
        super.onPageCommitVisible(view, url)
        callbacks.onPageCommit(url)
    }

    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)
        callbacks.onPageFinished(url)
        callbacks.onPageCommit(url)
        callbacks.onCanGoBackChanged(view.canGoBack())
    }

    override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
        super.doUpdateVisitedHistory(view, url, isReload)
        callbacks.onCanGoBackChanged(view.canGoBack())
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError,
    ) {
        super.onReceivedError(view, request, error)
        if (!request.isForMainFrame) return
        callbacks.onNetworkError(error.description?.toString())
    }

    override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest,
        errorResponse: WebResourceResponse,
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        if (!request.isForMainFrame) return
        if (errorResponse.statusCode >= 400) callbacks.onHttpError(errorResponse.statusCode)
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        handler.cancel()
        callbacks.onSslError()
    }

    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        Log.w(TAG, "WebView renderer gone, didCrash=${detail.didCrash()}")
        // The view is unusable; the UI recreates it. Never touch it after this call.
        callbacks.onRendererGone()
        return true
    }

    private companion object {
        const val TAG = "TwitterWebViewClient"
    }
}
