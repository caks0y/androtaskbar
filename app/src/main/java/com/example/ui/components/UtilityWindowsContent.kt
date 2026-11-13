package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.SystemTelemetry
import com.example.ui.AppStrings
import com.example.ui.theme.LocalWinPadChrome
import java.util.Locale

@Composable
fun SystemInfoWindowContent(
    telemetry: SystemTelemetry,
    isOverlayRunning: Boolean,
    strings: AppStrings
) {
    val chrome = LocalWinPadChrome.current
    val config = LocalConfiguration.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = chrome.cardSurface,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, chrome.glassBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = strings.deviceSpecs,
                    style = MaterialTheme.typography.titleSmall,
                    color = chrome.accent,
                    fontWeight = FontWeight.SemiBold
                )
                InfoRow("Model", telemetry.deviceModel)
                InfoRow("OS / ROM", telemetry.osDisplay)
                InfoRow("Platform", telemetry.androidVersion)
                InfoRow("Viewport", "${config.screenWidthDp} x ${config.screenHeightDp} dp (${config.densityDpi} dpi)")
                InfoRow(
                    "Storage",
                    String.format(
                        Locale.getDefault(),
                        "%.1f GB free / %.1f GB total",
                        telemetry.freeStorageGb,
                        telemetry.totalStorageGb
                    )
                )
                InfoRow("Battery", "${telemetry.batteryPercent}% ${if (telemetry.isCharging) "(Charging)" else ""}")
                InfoRow("Network", telemetry.networkLabel)
            }
        }

        Surface(
            color = chrome.cardSurface,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, chrome.glassBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = strings.readinessChecklist,
                    style = MaterialTheme.typography.titleSmall,
                    color = chrome.accent,
                    fontWeight = FontWeight.SemiBold
                )
                StatusRow("Always-On-Top Permission", telemetry.hasOverlayPermission)
                StatusRow("Always-On-Top Service Active", isOverlayRunning)
                StatusRow("Default Home Launcher", telemetry.isDefaultLauncher)
                StatusRow("System Freeform Flag", telemetry.isFreeformSupported)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StatusRow(label: String, ok: Boolean) {
    val chrome = LocalWinPadChrome.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (ok) Icons.Outlined.CheckCircle else Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = if (ok) chrome.statusSuccess else chrome.statusWarning,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (ok) "Ready" else "Optional",
                style = MaterialTheme.typography.labelSmall,
                color = if (ok) chrome.statusSuccess else chrome.statusWarning
            )
        }
    }
}

@Composable
fun ScratchpadWindowContent(
    text: String,
    strings: AppStrings,
    onTextChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = { onTextChange("") }) {
                Text(strings.clearText)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text(strings.scratchpadPlaceholder) },
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(12.dp)
        )
    }
}
