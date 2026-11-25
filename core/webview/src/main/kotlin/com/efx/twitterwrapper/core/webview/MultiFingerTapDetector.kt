package com.efx.twitterwrapper.core.webview

import android.view.MotionEvent
import kotlin.math.hypot

/**
 * Detects multi-finger taps on a live [android.webkit.WebView] touch stream.
 *
 * Unlike a Compose overlay on top of an interop view, observing `MotionEvent`s
 * from `WebView.onTouchEvent` is reliable: events are never consumed, so page
 * scrolling, pinch zoom and link taps keep working.
 *
 * Recognised gestures:
 *  * two fingers, double tap - primary menu gesture;
 *  * three fingers, single tap - fail-safe in case the primary gesture is
 *    intercepted by the page or by an accessibility service.
 */
internal class MultiFingerTapDetector(
    private val touchSlop: Float,
    private val doubleTapTimeoutMs: Long = 400L,
    private val tapTimeoutMs: Long = 400L,
    private val onTwoFingerDoubleTap: () -> Unit,
    private val onThreeFingerTap: () -> Unit,
) {

    private val downPositions = HashMap<Int, FloatArray>()

    private var tracking = false
    private var gestureStartTime = 0L
    private var maxPointers = 0
    private var movedTooFar = false

    private var hasFirstTap = false
    private var firstTapEndTime = 0L
    private var firstTapX = 0f
    private var firstTapY = 0f

    fun onTouchEvent(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> begin(event)
            MotionEvent.ACTION_POINTER_DOWN -> addPointer(event)
            MotionEvent.ACTION_MOVE -> trackMovement(event)
            MotionEvent.ACTION_UP -> finish(event)
            MotionEvent.ACTION_CANCEL -> cancel()
        }
    }

    private fun begin(event: MotionEvent) {
        downPositions.clear()
        downPositions[event.getPointerId(0)] = floatArrayOf(event.x, event.y)
        tracking = true
        gestureStartTime = event.eventTime
        maxPointers = 1
        movedTooFar = false
    }

    private fun addPointer(event: MotionEvent) {
        if (!tracking) return
        val index = event.actionIndex
        downPositions[event.getPointerId(index)] = floatArrayOf(event.getX(index), event.getY(index))
        if (event.pointerCount > maxPointers) maxPointers = event.pointerCount
    }

    private fun trackMovement(event: MotionEvent) {
        if (!tracking) return
        for (index in 0 until event.pointerCount) {
            val start = downPositions[event.getPointerId(index)] ?: continue
            val dx = event.getX(index) - start[0]
            val dy = event.getY(index) - start[1]
            if (hypot(dx, dy) > touchSlop) {
                movedTooFar = true
                break
            }
        }
    }

    private fun finish(event: MotionEvent) {
        if (!tracking) return
        tracking = false

        val duration = event.eventTime - gestureStartTime
        val midpoint = midpoint()

        when {
            movedTooFar -> hasFirstTap = false

            maxPointers == 2 && duration <= tapTimeoutMs -> {
                val endTime = event.eventTime
                val withinTime = hasFirstTap && endTime - firstTapEndTime in 0..doubleTapTimeoutMs
                val withinDistance = hasFirstTap &&
                    hypot(midpoint[0] - firstTapX, midpoint[1] - firstTapY) <= touchSlop * 3f

                if (withinTime && withinDistance) {
                    hasFirstTap = false
                    onTwoFingerDoubleTap()
                } else {
                    hasFirstTap = true
                    firstTapEndTime = endTime
                    firstTapX = midpoint[0]
                    firstTapY = midpoint[1]
                }
            }

            maxPointers >= 3 && duration <= tapTimeoutMs -> {
                hasFirstTap = false
                onThreeFingerTap()
            }

            else -> hasFirstTap = false
        }

        downPositions.clear()
    }

    private fun cancel() {
        tracking = false
        movedTooFar = false
        hasFirstTap = false
        downPositions.clear()
    }

    /** Average of the positions where all fingers of the gesture landed. */
    private fun midpoint(): FloatArray {
        if (downPositions.isEmpty()) return floatArrayOf(0f, 0f)
        var sumX = 0f
        var sumY = 0f
        for (position in downPositions.values) {
            sumX += position[0]
            sumY += position[1]
        }
        val count = downPositions.size
        return floatArrayOf(sumX / count, sumY / count)
    }
}
