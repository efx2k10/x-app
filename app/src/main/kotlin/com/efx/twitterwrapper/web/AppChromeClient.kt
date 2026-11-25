package com.efx.twitterwrapper.web

import android.view.View
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.efx.twitterwrapper.MainActivity

class AppChromeClient(
    private val activity: MainActivity,
    private val onProgressChanged: (Int) -> Unit,
) : WebChromeClient() {

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: android.webkit.ValueCallback<Array<android.net.Uri>>?,
        fileChooserParams: FileChooserParams?,
    ): Boolean = activity.fileChooserHandler.onShowFileChooser(filePathCallback, fileChooserParams)

    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: android.webkit.GeolocationPermissions.Callback?,
    ) {
        if (origin == null || callback == null) return
        activity.geoPermissionHandler.request(origin, callback)
    }

    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        activity.showFullscreen(view, callback)
    }

    override fun onHideCustomView() {
        activity.hideFullscreen()
    }

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressChanged(newProgress)
    }

    override fun onPermissionRequest(request: PermissionRequest?) {
        // Camera/microphone are not part of the wrapper scope.
        request?.deny()
    }
}
