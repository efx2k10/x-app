package com.efx.twitterwrapper.core.webview

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebSettings
import android.webkit.WebView

/**
 * Routes page downloads through the system DownloadManager, forwarding the
 * WebView user agent and cookies so authenticated media keeps working.
 */
object DownloadHandler {

    interface Callbacks {
        fun onDownloadStarted(fileName: String) {}
        fun onUnsupported(url: String) {}
        fun onError(message: String?) {}
    }

    fun attach(webView: WebView, callbacks: Callbacks) {
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            when {
                url.startsWith("blob:", ignoreCase = true) ||
                    url.startsWith("data:", ignoreCase = true) -> callbacks.onUnsupported(url)

                else -> runCatching {
                    enqueue(webView.context, url, userAgent, contentDisposition, mimeType)
                }.onSuccess { fileName ->
                    callbacks.onDownloadStarted(fileName)
                }.onFailure { error ->
                    callbacks.onError(error.message)
                }
            }
        }
    }

    private fun enqueue(
        context: Context,
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
    ): String {
        val fileName = sanitizeFileName(URLUtil.guessFileName(url, contentDisposition, mimeType))

        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setMimeType(mimeType ?: "application/octet-stream")
            addRequestHeader("User-Agent", userAgent ?: WebSettings.getDefaultUserAgent(context))
            CookieManager.getInstance().getCookie(url)?.let { cookie ->
                addRequestHeader("Cookie", cookie)
            }
            setTitle(fileName)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(false)
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
        return fileName
    }

    private fun sanitizeFileName(raw: String): String {
        val cleaned = raw.replace(INVALID_FILE_NAME_CHARS, "_").trim().take(MAX_FILE_NAME_LENGTH)
        return cleaned.ifBlank { "download" }
    }

    private val INVALID_FILE_NAME_CHARS = Regex("""[/\\:*?"<>|\u0000-\u001F]""")
    private const val MAX_FILE_NAME_LENGTH = 120
}
