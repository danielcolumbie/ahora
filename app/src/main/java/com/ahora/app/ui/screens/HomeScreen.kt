package com.ahora.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.ahora.app.ui.components.EmptyState
import com.ahora.app.ui.components.TasksColumn
import kotlinx.coroutines.launch

/** Pantalla principal: capturar en segundos y ver lo de hoy. */
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val tasks by viewModel.pendingTasks.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var input by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val app = context.applicationContext as AhoraApplication
    val speech = remember { app.container.createSpeechInputManager(context) }
    DisposableEffect(Unit) { onDispose { speech.release() } }
    val speechState by speech.state.collectAsStateWithLifecycle()

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
        viewModel.addTask(input)
        input = ""
    }

    fun onMicClick() {
        if (speechState is SpeechInputManager.State.Listening) {
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
                input = s.text
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
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "AHORA", style = MaterialTheme.typography.displayLarge)
            Text(
                text = "Sácalo de tu cabeza.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Aviso puntual si el sistema revocó el permiso de alarmas exactas:
            // sin él, los recordatorios pueden llegar tarde. Una sola vía clara,
            // descartable, que no insiste.
            val showNudge by viewModel.exactAlarmNudge.collectAsStateWithLifecycle()
            if (showNudge) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp)
                    ) {
                        Text(
                            text = "Tus recordatorios podrían llegar tarde: " +
                                "el permiso de alarmas exactas está desactivado.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
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
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("¿Qué tienes en mente?") },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                trailingIcon = {
                    if (input.isNotBlank()) {
                        IconButton(onClick = { submit() }) {
                            Icon(Icons.Filled.Add, contentDescription = "Añadir tarea")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Botón circular de micrófono: escala sutil al tocarlo y
            // anillos pulsantes mientras escucha.
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val interactionSource = remember { MutableInteractionSource() }
                val pressed by interactionSource.collectIsPressedAsState()
                val listening = speechState is SpeechInputManager.State.Listening
                val scale by animateFloatAsState(
                    targetValue = if (pressed || listening) 1.12f else 1f,
                    label = "micScale"
                )
                if (listening) {
                    PulseRing(delayMillis = 0)
                    PulseRing(delayMillis = 800)
                }
                FilledIconButton(
                    onClick = { onMicClick() },
                    modifier = Modifier
                        .size(72.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        },
                    shape = CircleShape,
                    interactionSource = interactionSource
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Dictar tarea",
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            if (speechState is SpeechInputManager.State.Listening) {
                Text(
                    text = "Escuchando…",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "HOY",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

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
                    onUpdateTitle = viewModel::updateTitle,
                    onSetReminder = viewModel::setReminder,
                    onClearReminder = viewModel::clearReminder,
                    onPastReminder = viewModel::pastReminderSelected,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Anillo que se expande y se desvanece en bucle, detrás del botón de
 * micrófono, mientras la app está escuchando.
 */
@Composable
private fun PulseRing(delayMillis: Int) {
    val infinite = rememberInfiniteTransition(label = "micPulse")
    val ringScale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.55f,
        animationSpec = infiniteRepeatable<Float>(
            animation = tween(1600, delayMillis = delayMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val ringAlpha by infinite.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable<Float>(
            animation = tween(1600, delayMillis = delayMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )
    Box(
        modifier = Modifier
            .size(72.dp)
            .graphicsLayer {
                scaleX = ringScale
                scaleY = ringScale
                alpha = ringAlpha
            }
            .clip(CircleShape)
            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
    )
}
