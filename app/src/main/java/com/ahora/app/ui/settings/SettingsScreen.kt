package com.ahora.app.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahora.app.BuildConfig
import com.ahora.app.data.ThemeMode
import com.ahora.app.notifications.NotificationHelper
import kotlinx.coroutines.launch

/** Ajustes mínimos: apariencia, notificaciones, respaldo, acerca de y licencias. */
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAbout by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }
    var backupMessage by remember { mutableStateOf<String?>(null) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* el permiso se pide solo al activar; sin él, no hay avisos */ }

    // Respaldo local: la app no usa la nube de Android; el usuario guarda
    // y restaura su propio archivo JSON donde quiera (Storage Access
    // Framework: no necesita permisos de almacenamiento).
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) scope.launch {
            val result = runCatching {
                val json = viewModel.exportBackup()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                } ?: error("No se pudo abrir el archivo")
            }
            backupMessage = result.fold(
                onSuccess = { "Respaldo guardado." },
                onFailure = { e -> "No se pudo guardar: ${e.message}" }
            )
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scope.launch {
            val result = runCatching {
                val json = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.readBytes().toString(Charsets.UTF_8)
                } ?: error("No se pudo leer el archivo")
                viewModel.importBackup(json)
            }
            backupMessage = result.fold(
                onSuccess = { r ->
                    val skipped = if (r.skipped > 0) " (${r.skipped} omitidas)" else ""
                    "Se importaron ${r.imported} tareas$skipped."
                },
                onFailure = { e -> "No se pudo importar: ${e.message}" }
            )
        }
    }

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

    fun openSystemChannelSettings() {
        // Android no permite cambiar el sonido ni la vibración de un canal ya
        // creado desde la app: se abre el ajuste del sistema, que sí puede.
        val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, NotificationHelper.CHANNEL_ID)
        }
        runCatching { context.startActivity(intent) }.onFailure {
            val fallback = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            runCatching { context.startActivity(fallback) }
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
        SettingLinkRow(
            title = "Sonido y vibración",
            subtitle = "Se configura en los ajustes del sistema",
            onClick = { openSystemChannelSettings() }
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Respaldo", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        SettingLinkRow(
            title = "Exportar tareas",
            subtitle = "Guarda un respaldo en un archivo",
            onClick = { exportLauncher.launch("ahora-respaldo.json") }
        )
        SettingLinkRow(
            title = "Importar tareas",
            subtitle = "Restaura desde un archivo de respaldo",
            onClick = { importLauncher.launch(arrayOf("application/json")) }
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
                Text("Sácalo de tu cabeza.\n\nTus tareas viven solo en tu teléfono: sin cuentas, sin publicidad, sin analítica, sin servidores y sin respaldo en la nube. Si quieres conservarlas, guarda tu propio respaldo en Ajustes → Respaldo.")
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

    if (backupMessage != null) {
        AlertDialog(
            onDismissRequest = { backupMessage = null },
            text = { Text(backupMessage!!) },
            confirmButton = {
                TextButton(onClick = { backupMessage = null }) { Text("Cerrar") }
            }
        )
    }
}

@Composable
private fun SettingLinkRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
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
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
