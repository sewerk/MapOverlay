package pl.srw.fattap

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.core.content.edit

private const val PREFS_NAME = "map_overlay_prefs"
private const val KEY_DISCLOSURE_ACCEPTED = "disclosure_accepted"

class MainActivity : ComponentActivity() {

    private val prefs by lazy { getSharedPreferences(PREFS_NAME, MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GestureConfig.loadFrom(prefs)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var hasOverlayPermission by remember {
                        mutableStateOf(Settings.canDrawOverlays(this))
                    }
                    var hasAccessibilityService by remember {
                        mutableStateOf(MapGestureAccessibilityService.instance != null)
                    }
                    var overlayRunning by remember {
                        mutableStateOf(OverlayService.isRunning)
                    }
                    var showDisclosure by remember { mutableStateOf(false) }
                    var swipeDistance by remember { mutableFloatStateOf(GestureConfig.swipeDistanceRatio) }
                    var zoomIn by remember { mutableFloatStateOf(GestureConfig.zoomInPercent) }
                    var zoomOut by remember { mutableFloatStateOf(GestureConfig.zoomOutPercent) }

                    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                        hasOverlayPermission = Settings.canDrawOverlays(this@MainActivity)
                        hasAccessibilityService = MapGestureAccessibilityService.instance != null
                        overlayRunning = OverlayService.isRunning
                    }

                    SetupScreen(
                        hasOverlayPermission = hasOverlayPermission,
                        hasAccessibilityService = hasAccessibilityService,
                        overlayRunning = overlayRunning,
                        swipeDistance = swipeDistance,
                        zoomIn = zoomIn,
                        zoomOut = zoomOut,
                        onRequestOverlay = {
                            startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    "package:$packageName".toUri()
                                )
                            )
                        },
                        onOpenAccessibility = {
                            if (isDisclosureAccepted()) {
                                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            } else {
                                showDisclosure = true
                            }
                        },
                        onStartOverlay = {
                            startForegroundService(Intent(this, OverlayService::class.java))
                            overlayRunning = true
                        },
                        onStopOverlay = {
                            stopService(Intent(this, OverlayService::class.java))
                            overlayRunning = false
                        },
                        onSwipeDistanceChange = {
                            swipeDistance = it
                            GestureConfig.swipeDistanceRatio = it
                            GestureConfig.saveTo(prefs)
                        },
                        onZoomInChange = {
                            zoomIn = it
                            GestureConfig.zoomInPercent = it
                            GestureConfig.saveTo(prefs)
                        },
                        onZoomOutChange = {
                            zoomOut = it
                            GestureConfig.zoomOutPercent = it
                            GestureConfig.saveTo(prefs)
                        }
                    )

                    if (showDisclosure) {
                        AccessibilityDisclosureDialog(
                            onAccept = {
                                acceptDisclosure()
                                showDisclosure = false
                                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            },
                            onDecline = { showDisclosure = false }
                        )
                    }
                }
            }
        }
    }

    private fun isDisclosureAccepted(): Boolean =
        prefs.getBoolean(KEY_DISCLOSURE_ACCEPTED, false)

    private fun acceptDisclosure() {
        prefs.edit { putBoolean(KEY_DISCLOSURE_ACCEPTED, true) }
    }
}
