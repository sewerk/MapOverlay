package pl.srw.fattap

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.res.Resources
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private val lifecycleOwner = OverlayLifecycleOwner()
    private var fabView: View? = null
    private val edgeViews = mutableListOf<View>()
    private val expanded = mutableStateOf(false)

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        lifecycleOwner.handleCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        if (!Settings.canDrawOverlays(this)) {
            Log.e(TAG, "Overlay permission not granted")
            stopSelf()
            return
        }

        try {
            addFab()
            lifecycleOwner.handleResume()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create overlay", e)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        START_NOT_STICKY

    private fun toggle() {
        if (expanded.value) {
            removeEdgeButtons()
        } else {
            addEdgeButtons()
        }
        expanded.value = !expanded.value
    }

    private fun addFab() {
        val size = BUTTON_SIZE_DP.dp.toPx()
        val params = overlayParams(size, size, Gravity.TOP or Gravity.START).apply {
            x = 8.dp.toPx()
            y = 8.dp.toPx()
        }
        val view = createComposeView {
            FabToggle(expanded = expanded.value) { toggle() }
        }
        windowManager.addView(view, params)
        fabView = view
    }

    private fun addEdgeButtons() {
        val btnSize = BUTTON_SIZE_DP.dp.toPx()
        val screenH = Resources.getSystem().displayMetrics.heightPixels
        val yOneThird = screenH / 3 - btnSize / 2
        val yTwoThirds = screenH * 2 / 3 - btnSize / 2

        data class Spec(
            val label: String, val gravity: Int,
            val yOff: Int = 0, val action: () -> Unit
        )

        val buttons = listOf(
            Spec("▲", Gravity.TOP or Gravity.CENTER_HORIZONTAL) {
                MapGestureAccessibilityService.instance?.performSwipe(SwipeDirection.UP)
            },
            Spec("▼", Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL) {
                MapGestureAccessibilityService.instance?.performSwipe(SwipeDirection.DOWN)
            },
            Spec("◀", Gravity.TOP or Gravity.START, yOff = yOneThird) {
                MapGestureAccessibilityService.instance?.performSwipe(SwipeDirection.LEFT)
            },
            Spec("▶", Gravity.TOP or Gravity.END, yOff = yOneThird) {
                MapGestureAccessibilityService.instance?.performSwipe(SwipeDirection.RIGHT)
            },
            Spec("＋", Gravity.TOP or Gravity.START, yOff = yTwoThirds) {
                MapGestureAccessibilityService.instance?.performZoom(true)
            },
            Spec("－", Gravity.TOP or Gravity.END, yOff = yTwoThirds) {
                MapGestureAccessibilityService.instance?.performZoom(false)
            }
        )

        for (spec in buttons) {
            val params = overlayParams(btnSize, btnSize, spec.gravity).apply {
                y = spec.yOff
            }
            val sizeDp = BUTTON_SIZE_DP.dp
            val view = createComposeView {
                EdgeButton(
                    label = spec.label,
                    width = sizeDp,
                    height = sizeDp,
                    onPress = spec.action
                )
            }
            windowManager.addView(view, params)
            edgeViews.add(view)
        }
    }

    private fun removeEdgeButtons() {
        for (view in edgeViews) removeSafe(view)
        edgeViews.clear()
    }

    private fun overlayParams(width: Int, height: Int, gravity: Int) =
        WindowManager.LayoutParams(
            width, height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply { this.gravity = gravity }

    private fun createComposeView(content: @Composable () -> Unit): ComposeView =
        ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setContent { MaterialTheme { content() } }
        }

    private fun removeSafe(view: View?) {
        if (view == null) return
        try { windowManager.removeView(view) }
        catch (e: Exception) { Log.w(TAG, "Failed to remove view", e) }
    }

    override fun onDestroy() {
        isRunning = false
        removeEdgeButtons()
        removeSafe(fabView)
        fabView = null
        lifecycleOwner.handleDestroy()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val intent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(intent)
            .build()
    }

    companion object {
        private const val TAG = "OverlayService"
        private const val CHANNEL_ID = "map_overlay_channel"
        private const val NOTIFICATION_ID = 1
        private const val BUTTON_SIZE_DP = 84 // 1.5x standard FAB (56dp)
        @Volatile var isRunning = false
            private set
    }
}

private fun androidx.compose.ui.unit.Dp.toPx(): Int {
    val density = Resources.getSystem().displayMetrics.density
    return (value * density + 0.5f).toInt()
}
