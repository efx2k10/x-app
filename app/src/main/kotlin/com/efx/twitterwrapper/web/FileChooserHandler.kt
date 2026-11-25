package com.efx.twitterwrapper.web

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Handles `<input type="file">` through SAF.
 *
 * The launcher is registered from the constructor, which must be called in
 * `Activity.onCreate` (before the lifecycle reaches STARTED).
 */
class FileChooserHandler(private val activity: ComponentActivity) {

    private var pendingCallback: ValueCallback<Array<Uri>>? = null

    private val launcher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val callback = pendingCallback ?: return@registerForActivityResult
        pendingCallback = null
        callback.onReceiveValue(extractUris(result.resultCode, result.data))
    }

    fun onShowFileChooser(
        callback: ValueCallback<Array<Uri>>?,
        params: WebChromeClient.FileChooserParams?,
    ): Boolean {
        pendingCallback?.onReceiveValue(null)
        pendingCallback = callback

        val intent = params?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        return try {
            launcher.launch(Intent.createChooser(intent, null))
            true
        } catch (t: ActivityNotFoundException) {
            pendingCallback = null
            callback?.onReceiveValue(null)
            false
        }
    }

    private fun extractUris(resultCode: Int, data: Intent?): Array<Uri>? {
        if (resultCode != Activity.RESULT_OK || data == null) return null

        data.clipData?.let { clip ->
            return Array(clip.itemCount) { index -> clip.getItemAt(index).uri }
        }
        return data.data?.let { uri -> arrayOf(uri) }
    }
}
