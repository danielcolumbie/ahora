package com.ahora.app.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahora.app.BuildConfig
import com.ahora.app.data.ThemeMode

/** Ajustes mínimos: apariencia, notificaciones, acerca de y licencias. */
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val sound by viewModel.notificationSound.collectAsStateWithLifecycle()
    val vibration by viewModel.vibration.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAbout by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* el permiso se pide solo al activar; sin él, no hay avisos */ }

    fun toggleNotifications(enabled: Boolean) {
        viewModel.setNotificationsEnabled(enabled)
        // El permiso se solicita únicamente cuando el usuario activa las notificaciones.
        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Ajustes", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Apariencia", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = themeMode == ThemeMode.SYSTEM,
                onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                label = { Text("Automática") }
            )
            FilterChip(
                selected = themeMode == ThemeMode.LIGHT,
                onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                label = { Text("Claro") }
            )
            FilterChip(
                selected = themeMode == ThemeMode.DARK,
                onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                label = { Text("Oscuro") }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        SettingSwitchRow(
            title = "Notificaciones",
            subtitle = "Avisos de tus recordatorios",
            checked = notificationsEnabled,
            onCheckedChange = { toggleNotifications(it) }
        )
        SettingSwitchRow(
            title = "Sonido",
            subtitle = "Sonido en las notificaciones",
            checked = sound,
            enabled = notificationsEnabled,
            onCheckedChange = { viewModel.setNotificationSound(it) }
        )
        SettingSwitchRow(
            title = "Vibración",
            subtitle = "Vibrar al notificar",
            checked = vibration,
            enabled = notificationsEnabled,
            onCheckedChange = { viewModel.setVibration(it) }
        )

        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = { showAbout = true }) { Text("Acerca de Ahora") }
        TextButton(onClick = { showLicenses = true }) { Text("Licencias de código abierto") }
        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("Ahora ${BuildConfig.VERSION_NAME}") },
            text = {
                Text("Sácalo de tu cabeza.\n\nTus tareas viven solo en tu teléfono: sin cuentas, sin publicidad, sin analítica y sin servidores.")
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) { Text("Cerrar") }
            }
        )
    }

    if (showLicenses) {
        AlertDialog(
            onDismissRequest = { showLicenses = false },
            title = { Text("Licencias") },
            text = {
                Text(
                    "Esta app usa componentes de código abierto:\n\n" +
                        "· Kotlin — Apache 2.0\n" +
                        "· Jetpack Compose (UI, Material 3) — Apache 2.0\n" +
                        "· AndroidX Activity, Core, Lifecycle — Apache 2.0\n" +
                        "· Navigation Compose — Apache 2.0\n" +
                        "· Room — Apache 2.0\n" +
                        "· DataStore — Apache 2.0\n\n" +
                        "Ahora se distribuye bajo licencia MIT."
                )
            },
            confirmButton = {
                TextButton(onClick = { showLicenses = false }) { Text("Cerrar") }
            }
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}
