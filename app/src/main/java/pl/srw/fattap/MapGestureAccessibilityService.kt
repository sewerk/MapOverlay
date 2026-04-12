package pl.srw.fattap

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.SharedPreferences
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.edit
import java.util.concurrent.ConcurrentLinkedQueue

object GestureConfig {
    var swipeDistanceRatio = DEFAULT_SWIPE_DISTANCE_RATIO
    const val SWIPE_DURATION_MS = 300L
    var zoomInPercent = DEFAULT_ZOOM_IN_PERCENT
    var zoomOutPercent = DEFAULT_ZOOM_OUT_PERCENT
    const val ZOOM_IN_DURATION_MS = 400L
    const val ZOOM_OUT_DURATION_MS = 400L

    const val DEFAULT_SWIPE_DISTANCE_RATIO = 0.25f
    const val DEFAULT_ZOOM_IN_PERCENT = 0.14f
    const val DEFAULT_ZOOM_OUT_PERCENT = 0.07f

    private const val KEY_SWIPE_DISTANCE = "swipe_distance_ratio"
    private const val KEY_ZOOM_IN = "zoom_in_percent"
    private const val KEY_ZOOM_OUT = "zoom_out_percent"

    fun loadFrom(prefs: SharedPreferences) {
        swipeDistanceRatio = prefs.getFloat(KEY_SWIPE_DISTANCE, DEFAULT_SWIPE_DISTANCE_RATIO)
        zoomInPercent = prefs.getFloat(KEY_ZOOM_IN, DEFAULT_ZOOM_IN_PERCENT)
        zoomOutPercent = prefs.getFloat(KEY_ZOOM_OUT, DEFAULT_ZOOM_OUT_PERCENT)
    }

    fun saveTo(prefs: SharedPreferences) {
        prefs.edit {
            putFloat(KEY_SWIPE_DISTANCE, swipeDistanceRatio)
            putFloat(KEY_ZOOM_IN, zoomInPercent)
            putFloat(KEY_ZOOM_OUT, zoomOutPercent)
        }
    }
}

enum class SwipeDirection { LEFT, RIGHT, UP, DOWN }

class MapGestureAccessibilityService : AccessibilityService() {

    private val gestureQueue = ConcurrentLinkedQueue<GestureDescription>()
    @Volatile private var dispatching = false

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onServiceConnected() {
        instance = this
    }

    override fun onDestroy() {
        instance = null
        gestureQueue.clear()
        super.onDestroy()
    }

    fun performSwipe(direction: SwipeDirection) {
        val metrics = resources.displayMetrics
        val cx = metrics.widthPixels / 2f
        val cy = metrics.heightPixels / 2f
        val distance = metrics.widthPixels * GestureConfig.swipeDistanceRatio

        val path = Path()
        // Swipe direction = direction the content moves.
        // LEFT arrow → finger drags left-to-right so map pans left.
        when (direction) {
            SwipeDirection.LEFT -> {
                path.moveTo(cx - distance, cy)
                path.lineTo(cx + distance, cy)
            }
            SwipeDirection.RIGHT -> {
                path.moveTo(cx + distance, cy)
                path.lineTo(cx - distance, cy)
            }
            SwipeDirection.UP -> {
                path.moveTo(cx, cy - distance)
                path.lineTo(cx, cy + distance)
            }
            SwipeDirection.DOWN -> {
                path.moveTo(cx, cy + distance)
                path.lineTo(cx, cy - distance)
            }
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, GestureConfig.SWIPE_DURATION_MS))
            .build()
        enqueue(gesture)
    }

    fun performZoom(zoomIn: Boolean) {
        val metrics = resources.displayMetrics
        val cx = metrics.widthPixels / 2f
        val cy = metrics.heightPixels / 2f
        val percent = if (zoomIn) GestureConfig.zoomInPercent else GestureConfig.zoomOutPercent
        val offset = metrics.widthPixels * 0.03f  // min finger gap from center
        val travel = metrics.widthPixels * percent / 2f
        Log.d(TAG, "zoom ${if (zoomIn) "IN" else "OUT"}: offset=${offset.toInt()} travel=${travel.toInt()} screen=${metrics.widthPixels}")

        val path1 = Path()
        val path2 = Path()

        if (zoomIn) {
            path1.moveTo(cx - offset, cy); path1.lineTo(cx - offset - travel, cy)
            path2.moveTo(cx + offset, cy); path2.lineTo(cx + offset + travel, cy)
        } else {
            path1.moveTo(cx - offset - travel, cy); path1.lineTo(cx - offset, cy)
            path2.moveTo(cx + offset + travel, cy); path2.lineTo(cx + offset, cy)
        }

        val duration = if (zoomIn) GestureConfig.ZOOM_IN_DURATION_MS else GestureConfig.ZOOM_OUT_DURATION_MS
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path1, 0L, duration))
            .addStroke(GestureDescription.StrokeDescription(path2, 0L, duration))
            .build()
        enqueue(gesture)
    }

    private fun enqueue(gesture: GestureDescription) {
        if (gestureQueue.size >= 1) return  // max 1 queued, drop excess
        gestureQueue.add(gesture)
        dispatchNext()
    }

    @Synchronized
    private fun dispatchNext() {
        if (dispatching) return
        val gesture = gestureQueue.poll() ?: return
        dispatching = true
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                dispatching = false
                dispatchNext()
            }
            override fun onCancelled(gestureDescription: GestureDescription) {
                Log.w(TAG, "Gesture cancelled")
                dispatching = false
                dispatchNext()
            }
        }, null)
    }

    companion object {
        private const val TAG = "MapGesture"
        var instance: MapGestureAccessibilityService? = null
            private set
    }
}
