package com.efx.twitterwrapper.core.webview

import android.content.Context
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.webkit.WebView

/**
 * WebView with a built-in gesture for opening the wrapper menu:
 * two fingers, double tap (or three fingers, single tap as a fail-safe).
 */
class TwitterWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : WebView(context, attrs) {

    var onMenuGesture: (() -> Unit)? = null

    private val tapDetector = MultiFingerTapDetector(
        touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat(),
        onTwoFingerDoubleTap = ::notifyMenuGesture,
        onThreeFingerTap = ::notifyMenuGesture,
    )

    override fun onTouchEvent(event: MotionEvent): Boolean {
        tapDetector.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    private fun notifyMenuGesture() {
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        onMenuGesture?.invoke()
    }
}
