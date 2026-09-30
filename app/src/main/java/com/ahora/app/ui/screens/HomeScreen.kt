package com.ahora.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahora.app.AhoraApplication
import com.ahora.app.data.Task
import com.ahora.app.speech.SpeechInputManager
import com.ahora.app.ui.MainViewModel
import com.ahora.app.ui.adaptive.AdaptiveLayout
import com.ahora.app.ui.components.AdaptiveListDetail
import com.ahora.app.ui.components.CollectUiEvents
import com.ahora.app.ui.components.AdvancedCreationDialog
import com.ahora.app.ui.components.CreationValues
import com.ahora.app.ui.components.EmptyState
import com.ahora.app.ui.components.TaskDetailPanel
import com.ahora.app.ui.components.TasksColumn
import com.ahora.app.ui.components.rememberCreationDraft
import com.ahora.app.ui.theme.Haptics
import com.ahora.app.ui.theme.Motion
import com.ahora.app.ui.theme.Sizes
import com.ahora.app.ui.theme.Spacing
import kotlinx.coroutines.launch

/** Pantalla principal: capturar en segundos y ver lo de hoy. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val tasks by viewModel.todayTasks.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    /**
     * Borrador de la creación (bloque C): un solo holder de estado con un
     * único `rememberSaveable`, en vez de los 12 estados sueltos de antes.
     * El texto escrito o dictado alimenta los campos vía lenguaje natural;
     * lo que el usuario toca a mano en "Más opciones" siempre gana.
     */
    val draft = rememberCreationDraft()
    var showAdvanced by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val haptics = LocalHapticFeedback.current

    val app = context.applicationContext as AhoraApplication
    val speech = remember { app.container.createSpeechInputManager(context) }
    DisposableEffect(Unit) { onDispose { speech.release() } }
    val speechState by speech.state.collectAsStateWithLifecycle()
    val listening = speechState is SpeechInputManager.State.Listening

    fun showMessage(text: String) {
        scope.launch { snackbarHostState.showSnackbar(text) }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (speech.isAvailable()) speech.startListening()
            else showMessage("El reconocimiento de voz no está disponible aquí. Puedes escribir la tarea.")
        } else {
            showMessage("Sin permiso de micrófono no puedo escucharte.")
        }
    }

    /**
     * Creación pendiente del permiso de notificaciones: el launcher vive
     * aquí (siempre compuesto) porque el resultado llega después de que
     * el borrador ya se consumió (auditoría 1.26.0).
     */
    var pendingCreation by remember { mutableStateOf<CreationValues?>(null) }

    fun saveTask(values: CreationValues) {
        // Tick de confirmación: la tarea quedó guardada (bloque H).
        Haptics.tick(haptics)
        viewModel.addTask(
            values.title,
            values.priority,
            values.dueAt,
            values.recurrence,
            values.reminderAt
        )
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val values = pendingCreation
        pendingCreation = null
        if (values == null) return@rememberLauncherForActivityResult
        if (granted) {
            saveTask(values)
        } else {
            // Denegado: sin el permiso el recordatorio nunca avisaría, así
            // que la tarea se guarda sin él y se avisa (misma regla que en
            // ReminderFlowHost: no se descarta la elección en silencio).
            saveTask(values.copy(reminderAt = null))
            showMessage("Sin permiso de notificaciones, el recordatorio no te avisará")
        }
    }

    fun submit() {
        // El borrador devuelve el título limpio (sin lo detectado) y los
        // valores, y se vacía: la lógica de guardado no cambió.
        val values = draft.consumeForSave()
        if (values.title.isBlank()) return
        // El recordatorio puede venir del lenguaje natural o del diálogo
        // de opciones: en Android 13+ se pide el permiso justo al guardar,
        // igual que en el flujo normal (auditoría 1.26.0).
        val needsPermission =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                values.reminderAt != null &&
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            pendingCreation = values
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            saveTask(values)
        }
    }

    fun onMicClick() {
        if (listening) {
            speech.stopListening()
            return
        }
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            if (speech.isAvailable()) speech.startListening()
            else showMessage("El reconocimiento de voz no está disponible aquí. Puedes escribir la tarea.")
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // El texto reconocido va al campo para que el usuario lo confirme antes de guardar.
    LaunchedEffect(speechState) {
        when (val s = speechState) {
            is SpeechInputManager.State.Result -> {
                // El texto dictado pasa por el mismo parser que el
                // escrito: el lenguaje natural también funciona por voz.
                draft.applyText(s.text)
                speech.consumeResult()
            }
            is SpeechInputManager.State.Error -> {
                snackbarHostState.showSnackbar(s.message)
                speech.consumeResult()
            }
            else -> Unit
        }
    }

    // Eventos de una sola vez (snackbars): la misma recolección
    // compartida que usa Todas (CollectUiEvents), sin duplicar lógica.
    CollectUiEvents(viewModel, snackbarHostState)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // Los insets del sistema (status bar, navegación) ya los aplica el
        // Scaffold de NavGraph: este Scaffold interior solo aloja el
        // snackbar, así que no los reaplica (bloque J: evita doble padding
        // superior e inferior).
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        // BoxWithConstraints (bloque J): mide el espacio real disponible
        // para decidir el layout, sin dependencias nuevas.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Dos paneles en pantallas anchas (lista + detalle); una sola
            // columna + diálogos en teléfonos, como hasta ahora.
            val wide = AdaptiveLayout.isTwoPane(maxWidth)
            // Encabezado compacto en pantallas bajas (landscape en
            // teléfono): la creación y la lista no pelean por el alto.
            val compactHeader = AdaptiveLayout.isCompactHeader(maxHeight)
            // Tarea seleccionada para el panel de detalle (solo en dos
            // paneles). Sobrevive a la rotación; se limpia si la tarea
            // sale de la lista (completada o eliminada).
            var selectedTaskId by rememberSaveable { mutableStateOf<Long?>(null) }
            LaunchedEffect(tasks, selectedTaskId) {
                if (selectedTaskId != null &&
                    tasks.none { it.id == selectedTaskId }
                ) {
                    selectedTaskId = null
                }
            }
            // Instancias estables (bloque K, rendimiento): sin esto, cada
            // recomposición de la pantalla (p. ej. cada tecla en la barra
            // de creación) creaba lambdas nuevas que forzaban la
            // recomposición de la lista, sus filas y el panel de detalle.
            val onEditRequest: ((Task) -> Unit)? = remember(wide) {
                if (wide) { { task -> selectedTaskId = task.id } } else null
            }
            val onDetailDelete: (Task) -> Unit = remember {
                { task ->
                    if (task.id == selectedTaskId) selectedTaskId = null
                    viewModel.deleteTask(task)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.screenHorizontal)
            ) {
                Spacer(modifier = Modifier.height(if (compactHeader) Spacing.s else Spacing.xxl))
                Text(text = "AHORA", style = MaterialTheme.typography.displayLarge)
                if (!compactHeader) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = "Sácalo de tu cabeza.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(if (compactHeader) Spacing.m else Spacing.xxl))

                // Aviso puntual si el sistema revocó el permiso de alarmas exactas:
                // sin él, los recordatorios pueden llegar tarde. Superficie neutra,
                // una sola vía clara, descartable, que no insiste.
                val showNudge by viewModel.exactAlarmNudge.collectAsStateWithLifecycle()
                if (showNudge) {
                    Surface(
                        shape = RoundedCornerShape(Spacing.l),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(
                                start = Spacing.l,
                                top = Spacing.s,
                                bottom = Spacing.s,
                                end = Spacing.xs
                            )
                        ) {
                            Text(
                                text = "Tus recordatorios podrían llegar tarde: " +
                                    "el permiso de alarmas exactas está desactivado.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = {
                                viewModel.exactAlarmSettingsIntent()
                                    ?.let { context.startActivity(it) }
                                viewModel.dismissExactAlarmNudge()
                            }) { Text("Ajustes") }
                            IconButton(onClick = { viewModel.dismissExactAlarmNudge() }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Descartar aviso",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(Spacing.l))
                }

                // Barra de captura: escribir, dictar o enviar desde un solo lugar.
                // Sin bordes: la superficie la distingue del fondo con calma.
                TextField(
                    value = draft.text,
                    onValueChange = { draft.applyText(it) },
                    placeholder = { Text("¿Qué tienes en mente?") },
                    singleLine = true,
                    shape = RoundedCornerShape(Spacing.l),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val interactionSource = remember { MutableInteractionSource() }
                            val pressed by interactionSource.collectIsPressedAsState()
                            val micScale by animateFloatAsState(
                                targetValue = if (pressed || listening) 1.15f else 1f,
                                label = "micScale"
                            )
                            Box(contentAlignment = Alignment.Center) {
                                if (listening) {
                                    PulseRing(delayMillis = 0)
                                    PulseRing(delayMillis = Motion.PULSE_DURATION_MILLIS / 2)
                                }
                                IconButton(
                                    onClick = { onMicClick() },
                                    interactionSource = interactionSource,
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = micScale
                                        scaleY = micScale
                                    }
                                ) {
                                    Icon(
                                        Icons.Filled.Mic,
                                        // El botón alterna dictar/detener: la
                                        // etiqueta sigue al estado (bloque I).
                                        // El estado también se ve en el texto
                                        // "Escuchando…" y en el tinte: nunca
                                        // depende solo del color.
                                        contentDescription = micButtonDescription(listening),
                                        tint = if (listening) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            AnimatedVisibility(
                                visible = draft.text.isNotBlank(),
                                enter = scaleIn(animationSpec = Motion.checkSpring()) + fadeIn(),
                                exit = scaleOut() + fadeOut()
                            ) {
                                IconButton(
                                    onClick = { showAdvanced = true }
                                ) {
                                    Icon(
                                        Icons.Filled.Tune,
                                        contentDescription = "Más opciones",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            AnimatedVisibility(
                                visible = draft.text.isNotBlank(),
                                enter = scaleIn(animationSpec = Motion.checkSpring()) + fadeIn(),
                                exit = scaleOut() + fadeOut()
                            ) {
                                FilledIconButton(
                                    onClick = { submit() },
                                    // Sin tamaño explícito a propósito (bloque I):
                                    // el IconButton de M3 mide 48dp de área
                                    // táctil por defecto; fijarlo a 40dp lo
                                    // dejaba por debajo del mínimo de
                                    // accesibilidad.
                                    modifier = Modifier.padding(end = Spacing.xs)
                                ) {
                                    Icon(
                                        Icons.Filled.ArrowUpward,
                                        contentDescription = "Añadir tarea"
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
                if (listening) {
                    Spacer(modifier = Modifier.height(Spacing.s))
                    Text(
                        text = "Escuchando…",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                // Chips de feedback del lenguaje natural (bloque C): muestran lo
                // que la app entendió o lo que el usuario configuró. Entran con
                // fundido + expansión suave: la barra ya no salta entre dos
                // modos. Tocar un chip abre la configuración avanzada.
                val feedbackChips = draft.feedbackChips()
                AnimatedVisibility(
                    visible = feedbackChips.isNotEmpty(),
                    enter = Motion.softExpand(),
                    exit = Motion.softExit()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(Spacing.s))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                        ) {
                            feedbackChips.forEach { label ->
                                AssistChip(
                                    onClick = { showAdvanced = true },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }

                // Configuración avanzada (bloque C): prioridad, fecha límite,
                // recordatorio y recurrencia en un diálogo dedicado, separado
                // de la creación rápida. Edita el borrador; al enviar se aplica.
                if (showAdvanced) {
                    // El diálogo entra con fundido + escala sutil (bloque H);
                    // al cerrar desaparece al instante.
                    AnimatedVisibility(
                        visible = true,
                        enter = Motion.dialogEnter()
                    ) {
                        AdvancedCreationDialog(
                            draft = draft,
                            onDismiss = { showAdvanced = false },
                            onPastReminder = { showMessage("Esa hora ya pasó, elige una futura") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (compactHeader) Spacing.m else Spacing.xxl))

                // Lista + detalle en pantallas anchas; solo la lista en
                // teléfonos (el detalle se abre como diálogo, como antes).
                AdaptiveListDetail(
                    wide = wide,
                    modifier = Modifier.weight(1f),
                    list = { listModifier ->
                        Column(modifier = listModifier) {
                            // Etiqueta de sección con contador discreto: con muchas tareas
                            // se lee de un vistazo cuánto queda, sin añadir ruido visual.
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "HOY",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    // Encabezado para la navegación de TalkBack (bloque I).
                                    modifier = Modifier.semantics { heading() }
                                )
                                if (tasks.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(Spacing.xs))
                                    Text(
                                        // Hechas/total ("2/6"): el progreso del día se lee
                                        // de un vistazo, sin añadir ruido visual.
                                        text = "${tasks.count { it.isDone }}/${tasks.size}",
                                        style = MaterialTheme.typography.titleMedium,
                                        // Contador discreto de la etiqueta de sección (bloque B): informa sin
    // ruido. Bloque G (1.20.0): 0.65f -> 0.90f. En oscuro, onSurfaceVariant al
    // 65% daba 3.97:1 sobre el fondo (bajo AA); al 90% da 6.65:1. En claro,
    // con el nuevo onSurfaceVariant (#625F58), al 90% da 4.62:1. Sigue siendo
    // discreto frente a la etiqueta a plena opacidad, pero legible en ambas
    // paletas (fijado por ColorContrastTest).
    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.90f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(Spacing.s))

                            if (tasks.isEmpty()) {
                                EmptyState(
                                    onAdd = { focusRequester.requestFocus() },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                TasksColumn(
                                    tasks = tasks,
                                    onToggleDone = viewModel::toggleDone,
                                    onDelete = viewModel::deleteTask,
                                    onUpdateDetails = viewModel::updateDetails,
                                    onSetReminder = viewModel::setReminder,
                                    onClearReminder = viewModel::clearReminder,
                                    onPastReminder = viewModel::pastReminderSelected,
                                    onNotifPermissionDenied = viewModel::notifPermissionDenied,
                                    alarmScheduler = viewModel.scheduler,
                                    scrollToTopEvents = viewModel.scrollToTopEvents,
                                    modifier = Modifier.weight(1f),
                                    // En dos paneles, editar selecciona la tarea
                                    // para el panel de detalle; en teléfonos se
                                    // conserva el diálogo (onEditRequest = null).
                                    onEditRequest = onEditRequest,
                                    // La fila de la tarea en el detalle se
                                    // resalta (evolución visual 2026-09-30).
                                    selectedTaskId = selectedTaskId
                                )
                            }
                        }
                    },
                    detail = { detailModifier ->
                        TaskDetailPanel(
                            task = tasks.firstOrNull { it.id == selectedTaskId },
                            onToggleDone = viewModel::toggleDone,
                            onDelete = onDetailDelete,
                            onUpdateDetails = viewModel::updateDetails,
                            onSetReminder = viewModel::setReminder,
                            onClearReminder = viewModel::clearReminder,
                            onPastReminder = viewModel::pastReminderSelected,
                            onNotifPermissionDenied = viewModel::notifPermissionDenied,
                            alarmScheduler = viewModel.scheduler,
                            modifier = detailModifier
                        )
                    }
                )
            }
        }
    }
}

/**
 * Etiqueta de accesibilidad del botón del micrófono: el botón alterna
 * entre dictar y detener, así que la etiqueta sigue al estado (bloque I).
 * Función pura para poder probarla.
 */
internal fun micButtonDescription(listening: Boolean): String =
    if (listening) "Detener dictado" else "Dictar tarea"

/**
 * Anillo que se expande y se desvanece en bucle detrás del micrófono
 * mientras la app escucha.
 */
@Composable
private fun PulseRing(delayMillis: Int) {
    val infinite = rememberInfiniteTransition(label = "micPulse")
    val ringScale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable<Float>(
            animation = Motion.pulseSpec(delayMillis),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val ringAlpha by infinite.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable<Float>(
            animation = Motion.pulseSpec(delayMillis),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )
    Box(
        modifier = Modifier
            .size(Sizes.pulseRing)
            .graphicsLayer {
                scaleX = ringScale
                scaleY = ringScale
                alpha = ringAlpha
            }
            .clip(CircleShape)
            .border(Sizes.pulseRingStroke, MaterialTheme.colorScheme.primary, CircleShape)
    )
}

