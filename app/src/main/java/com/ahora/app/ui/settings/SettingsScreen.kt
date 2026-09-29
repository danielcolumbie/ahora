package com.ahora.app.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahora.app.BuildConfig
import com.ahora.app.data.ThemeMode
import com.ahora.app.notifications.NotificationHelper
import com.ahora.app.ui.components.FormSection
import com.ahora.app.ui.components.SettingLinkRow
import com.ahora.app.ui.components.SettingSwitchRow
import com.ahora.app.ui.theme.Spacing
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

/** Nombre por defecto del archivo de respaldo exportado. */
internal const val DEFAULT_BACKUP_NAME = "ahora-respaldo.json"

/** Mensaje de éxito al exportar el respaldo. */
internal fun formatBackupSaved(): String = "Respaldo guardado."

/** Mensaje de fallo de respaldo: acción ("guardar"/"importar") + causa. */
internal fun formatBackupError(action: String, cause: String?): String =
    "No se pudo $action: ${cause ?: "error desconocido"}"

/** Mensaje de éxito al importar: tareas importadas y, si las hay, omitidas. */
internal fun formatImportSuccess(imported: Int, skipped: Int): String {
    val skippedPart = if (skipped > 0) " ($skipped omitidas)" else ""
    return "Se importaron $imported tareas$skippedPart."
}

/**
 * Ajustes (BLOQUE F del rediseño premium): secciones con la etiqueta del
 * sistema ([FormSection]) y filas reutilizables ([SettingLinkRow] /
 * [SettingSwitchRow]); los resultados del respaldo salen en un Snackbar
 * del sistema en vez de un diálogo suelto.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    /** Tocar la pestaña «Ajustes» ya activa sube el scroll al inicio (bloque H). */
    scrollToTopEvents: SharedFlow<Unit>
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAbout by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val settingsScroll = rememberScrollState()

    // Tocar la pestaña activa sube al inicio con desplazamiento suave
    // (bloque H), igual que en las listas de Hoy y Todas.
    LaunchedEffect(scrollToTopEvents) {
        scrollToTopEvents.collect { settingsScroll.animateScrollTo(0) }
    }

    // El "Reintentar" del Snackbar necesita el launcher ya creado, pero el
    // callback no puede referenciar su propio val: los holders se declaran
    // antes y se arman con SideEffect después de crear cada launcher.
    val exportRetry = remember { mutableStateOf<(() -> Unit)?>(null) }
    val importRetry = remember { mutableStateOf<(() -> Unit)?>(null) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* el permiso se pide solo al activar; sin él, no hay avisos */ }

    /** Resultado del respaldo en Snackbar; los fallos ofrecen reintentar. */
    fun showBackupResult(message: String, retry: (() -> Unit)? = null) {
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = if (retry != null) "Reintentar" else null,
                withDismissAction = true
            )
            if (result == SnackbarResult.ActionPerformed) retry?.invoke()
        }
    }

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
            result.fold(
                onSuccess = { showBackupResult(formatBackupSaved()) },
                onFailure = { e ->
                    showBackupResult(formatBackupError("guardar", e.message), exportRetry.value)
                }
            )
        }
    }
    SideEffect { exportRetry.value = { exportLauncher.launch(DEFAULT_BACKUP_NAME) } }
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
            result.fold(
                onSuccess = { r ->
                    showBackupResult(formatImportSuccess(r.imported, r.skipped))
                },
                onFailure = { e ->
                    showBackupResult(formatBackupError("importar", e.message), importRetry.value)
                }
            )
        }
    }
    SideEffect { importRetry.value = { importLauncher.launch(arrayOf("application/json")) } }

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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(settingsScroll)
                .padding(horizontal = Spacing.screenHorizontal)
        ) {
            Spacer(modifier = Modifier.height(Spacing.xxl))
            Text(
                text = "Ajustes",
                style = MaterialTheme.typography.titleLarge,
                // Encabezado para la navegación de TalkBack (bloque I).
                modifier = Modifier.semantics { heading() }
            )

            Spacer(modifier = Modifier.height(Spacing.xxl))
            FormSection(title = "Apariencia") {
                ThemeSelector(
                    selected = themeMode,
                    onSelect = { viewModel.setThemeMode(it) }
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xxl))
            FormSection(title = "Notificaciones") {
                SettingSwitchRow(
                    title = "Notificaciones",
                    subtitle = "Avisos de tus recordatorios",
                    icon = Icons.Outlined.Notifications,
                    checked = notificationsEnabled,
                    onCheckedChange = { toggleNotifications(it) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingLinkRow(
                    title = "Sonido y vibración",
                    subtitle = "Se configura en los ajustes del sistema",
                    icon = Icons.Outlined.VolumeUp,
                    onClick = { openSystemChannelSettings() }
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xxl))
            FormSection(title = "Respaldo") {
                SettingLinkRow(
                    title = "Exportar tareas",
                    subtitle = "Guarda un respaldo en un archivo",
                    icon = Icons.Outlined.Upload,
                    onClick = { exportLauncher.launch(DEFAULT_BACKUP_NAME) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingLinkRow(
                    title = "Importar tareas",
                    subtitle = "Restaura desde un archivo de respaldo",
                    icon = Icons.Outlined.Download,
                    onClick = { importLauncher.launch(arrayOf("application/json")) }
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xxl))
            FormSection(title = "Acerca de") {
                SettingLinkRow(
                    title = "Acerca de Ahora",
                    subtitle = "Qué hace esta app con tus datos",
                    icon = Icons.Outlined.Info,
                    onClick = { showAbout = true }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingLinkRow(
                    title = "Licencias de código abierto",
                    subtitle = "Componentes que usa la app",
                    icon = Icons.Outlined.Description,
                    onClick = { showLicenses = true }
                )
            }
            Spacer(modifier = Modifier.height(Spacing.xxl))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.m)
        )
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
}

/**
 * Selector de tema: los tres `FilterChip` usan el `primaryContainer`
 * propio de Ahora cuando están seleccionados (antes, el contenedor por
 * defecto de Material 3, ajeno a la paleta de un solo acento).
 *
 * `FlowRow` en vez de `Row` (bloque I): con la escala de fuente del
 * sistema grande, los tres chips no caben en una línea y se recortarían;
 * así bajan a la siguiente sin romperse.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeSelector(selected: Int, onSelect: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = scheme.primaryContainer,
        selectedLabelColor = scheme.onPrimaryContainer,
        selectedLeadingIconColor = scheme.onPrimaryContainer
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        ThemeChip("Automática", ThemeMode.SYSTEM, selected, chipColors, onSelect)
        ThemeChip("Claro", ThemeMode.LIGHT, selected, chipColors, onSelect)
        ThemeChip("Oscuro", ThemeMode.DARK, selected, chipColors, onSelect)
    }
}

@Composable
private fun ThemeChip(
    label: String,
    mode: Int,
    selected: Int,
    colors: androidx.compose.material3.SelectableChipColors,
    onSelect: (Int) -> Unit
) {
    FilterChip(
        selected = selected == mode,
        onClick = { onSelect(mode) },
        label = { Text(label) },
        colors = colors
    )
}
