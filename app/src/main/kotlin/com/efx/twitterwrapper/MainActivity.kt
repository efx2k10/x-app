package com.efx.twitterwrapper

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.efx.twitterwrapper.ui.EfxTheme
import com.efx.twitterwrapper.ui.MainScreen
import com.efx.twitterwrapper.web.FileChooserHandler
import com.efx.twitterwrapper.web.GeoPermissionHandler

class MainActivity : ComponentActivity() {

    lateinit var fileChooserHandler: FileChooserHandler
        private set

    lateinit var geoPermissionHandler: GeoPermissionHandler
        private set

    /** Read from Compose to keep back navigation and insets in sync with fullscreen video. */
    val isFullscreen = mutableStateOf(false)

    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Registered before setContent: ActivityResultRegistry requires it
        // while the Activity is still in CREATED state.
        fileChooserHandler = FileChooserHandler(this)
        geoPermissionHandler = GeoPermissionHandler(this)

        // The app is always dark, so system bar icons must always be light.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (customView != null) {
                        hideFullscreen()
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            },
        )

        setContent {
            EfxTheme {
                MainScreen()
            }
        }
    }

    fun showFullscreen(view: View, callback: WebChromeClient.CustomViewCallback) {
        if (customView != null) {
            callback.onCustomViewHidden()
            return
        }

        val container = findViewById<ViewGroup>(android.R.id.content) ?: return
        container.addView(
            view,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )

        customView = view
        customViewCallback = callback
        isFullscreen.value = true
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    fun hideFullscreen() {
        val view = customView ?: return
        (view.parent as? ViewGroup)?.removeView(view)

        customView = null
        isFullscreen.value = false
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        WindowCompat.getInsetsController(window, window.decorView)
            .show(WindowInsetsCompat.Type.systemBars())

        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
    }

    override fun onStop() {
        // Persist the login session to disk before the process can be killed.
        CookieManager.getInstance().flush()
        super.onStop()
    }

    override fun onDestroy() {
        hideFullscreen()
        super.onDestroy()
    }
}
