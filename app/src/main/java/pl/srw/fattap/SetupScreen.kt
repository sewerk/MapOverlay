package pl.srw.fattap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun SetupScreen(
    hasOverlayPermission: Boolean,
    hasAccessibilityService: Boolean,
    overlayRunning: Boolean,
    swipeDistance: Float,
    zoomIn: Float,
    zoomOut: Float,
    onRequestOverlay: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onStartOverlay: () -> Unit,
    onStopOverlay: () -> Unit,
    onSwipeDistanceChange: (Float) -> Unit,
    onZoomInChange: (Float) -> Unit,
    onZoomOutChange: (Float) -> Unit
) {
    val canStart = hasOverlayPermission && hasAccessibilityService && !overlayRunning

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(40.dp))

        StepButton(
            step = "1",
            label = stringResource(R.string.step_overlay_permission),
            done = hasOverlayPermission,
            onClick = onRequestOverlay
        )
        Spacer(Modifier.height(12.dp))

        StepButton(
            step = "2",
            label = stringResource(R.string.step_accessibility_service),
            done = hasAccessibilityService,
            onClick = onOpenAccessibility
        )
        Spacer(Modifier.height(40.dp))

        Button(
            onClick = onStartOverlay,
            modifier = Modifier.fillMaxWidth(),
            enabled = canStart
        ) {
            Text(stringResource(R.string.action_start))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onStopOverlay,
            modifier = Modifier.fillMaxWidth(),
            enabled = overlayRunning
        ) {
            Text(stringResource(R.string.action_stop))
        }

        Spacer(Modifier.height(40.dp))
        Text(
            stringResource(R.string.settings_header),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(16.dp))

        SettingSlider(
            label = stringResource(R.string.setting_swipe_distance),
            value = swipeDistance,
            valueRange = 0.05f..0.50f,
            onValueChange = onSwipeDistanceChange
        )
        Spacer(Modifier.height(12.dp))
        SettingSlider(
            label = stringResource(R.string.setting_zoom_in),
            value = zoomIn,
            valueRange = 0.07f..0.30f,
            onValueChange = onZoomInChange
        )
        Spacer(Modifier.height(12.dp))
        SettingSlider(
            label = stringResource(R.string.setting_zoom_out),
            value = zoomOut,
            valueRange = 0.07f..0.20f,
            onValueChange = onZoomOutChange
        )
    }
}

@Composable
private fun SettingSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${(value * 100).roundToInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StepButton(step: String, label: String, done: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (done)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.primary
        )
    ) {
        Text(
            text = if (done) "✓  $label" else "$step.  $label",
            textAlign = TextAlign.Center,
            color = if (done)
                MaterialTheme.colorScheme.onSecondaryContainer
            else
                MaterialTheme.colorScheme.onPrimary
        )
    }
}