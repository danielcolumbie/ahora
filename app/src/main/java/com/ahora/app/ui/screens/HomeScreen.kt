package com.ahora.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahora.app.AhoraApplication
import com.ahora.app.speech.SpeechInputManager
import com.ahora.app.ui.MainViewModel
import com.ahora.app.ui.UiEvent
import com.ahora.app.ui.components.AdvancedCreationDialog
import com.ahora.app.ui.components.EmptyState
import com.ahora.app.ui.components.TasksColumn
import com.ahora.app.ui.components.rememberCreationDraft
import com.ahora.app.ui.theme.Motion
import com.ahora.app.ui.theme.Spacing
import kotlinx.coroutines.launch

/** Pantalla principal: capturar en segundos y ver lo de hoy. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val tasks by viewModel.pendingTasks.collectAsStateWithLifecycle()
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

    fun submit() {
        // El borrador devuelve el título limpio (sin lo detectado) y los
        // valores, y se vacía: la lógica de guardado no cambió.
        val values = draft.consumeForSave()
        if (values.title.isBlank()) return
        viewModel.addTask(
            values.title,
            values.priority,
            values.dueAt,
            values.recurrence,
            values.reminderAt
        )
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

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.TaskDeleted -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "Tarea eliminada",
                        actionLabel = "Deshacer",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete(event.task)
                }
                is UiEvent.Message -> snackbarHostState.showSnackbar(event.text)
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.screenHorizontal)
        ) {
            Spacer(modifier = Modifier.height(Spacing.xxl))
            Text(text = "AHORA", style = MaterialTheme.typography.displayLarge)
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = "Sácalo de tu cabeza.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.xxl))

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
                                    contentDescription = "Dictar tarea",
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
                                modifier = Modifier
                                    .padding(end = Spacing.xs)
                                    .size(40.dp)
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
                enter = fadeIn(animationSpec = tween(240, easing = FastOutSlowInEasing)) +
                    expandVertically(animationSpec = tween(240, easing = FastOutSlowInEasing)),
                exit = fadeOut(animationSpec = tween(200)) +
                    shrinkVertically(animationSpec = tween(200))
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
                AdvancedCreationDialog(
                    draft = draft,
                    onDismiss = { showAdvanced = false },
                    onPastReminder = { showMessage("Esa hora ya pasó, elige una futura") }
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xxl))
            // Etiqueta de sección con contador discreto: con muchas tareas
            // se lee de un vistazo cuánto queda, sin añadir ruido visual.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HOY",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (tasks.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(
                        text = tasks.size.toString(),
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
                    alarmScheduler = viewModel.scheduler,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

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
            .size(40.dp)
            .graphicsLayer {
                scaleX = ringScale
                scaleY = ringScale
                alpha = ringAlpha
            }
            .clip(CircleShape)
            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
    )
}

