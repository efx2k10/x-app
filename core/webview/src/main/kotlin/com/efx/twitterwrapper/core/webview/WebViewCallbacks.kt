package com.efx.twitterwrapper.core.webview

import android.net.Uri

/** Host application hooks. Every method has a default no-op implementation. */
interface WebViewCallbacks {

    fun onPageStarted(url: String?) {}

    fun onPageFinished(url: String?) {}

    /** Called for every main frame commit, including in-app navigations. */
    fun onPageCommit(url: String?) {}

    fun onProgressChanged(progress: Int) {}

    fun onCanGoBackChanged(canGoBack: Boolean) {}

    fun onNetworkError(description: String?) {}

    fun onHttpError(statusCode: Int) {}

    fun onSslError() {}

    /** The renderer process died; the WebView cannot be reused. */
    fun onRendererGone() {}
}

internal fun openExternally(context: android.content.Context, uri: Uri) {
    runCatching {
        context.startActivity(
            android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
