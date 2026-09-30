package com.ahora.app.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Voz a texto con la API gratuita de Android (sin servicios externos de pago).
 * El texto reconocido se devuelve para que el usuario lo confirme antes de guardar:
 * nunca se inventa texto si el reconocimiento falla.
 *
 * Privacidad (auditoría 1.26.0): se pide preferencia por el reconocimiento
 * offline ([RecognizerIntent.EXTRA_PREFER_OFFLINE]) para que el audio no
 * salga del teléfono cuando el dispositivo lo soporta. Si el dispositivo no
 * tiene el paquete de voz offline, se reintenta una vez en modo normal: la
 * funcionalidad no se rompe, solo que ese audio puede procesarse en los
 * servidores de Google (ver README → Privacidad).
 */
class SpeechInputManager(private val context: Context) {

    sealed interface State {
        data object Idle : State
        data object Listening : State
        data class Result(val text: String) : State
        data class Error(val message: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private var recognizer: SpeechRecognizer? = null

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    /**
     * Errores en los que reintentar sin preferencia offline no tiene
     * sentido: el usuario no habló, falta el permiso o el reconocedor
     * está ocupado. Cualquier otro error tras pedir offline se interpreta
     * como "este dispositivo no lo soporta" y se prueba en modo normal.
     */
    private val noOfflineRetryErrors = setOf(
        SpeechRecognizer.ERROR_NO_MATCH,
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS,
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY
    )

    fun startListening() {
        startListening(preferOffline = true, retried = false)
    }

    private fun startListening(preferOffline: Boolean, retried: Boolean) {
        if (!isAvailable()) {
            _state.value = State.Error("El reconocimiento de voz no está disponible en este dispositivo. Puedes escribir la tarea.")
            return
        }
        stopInternal()
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _state.value = State.Listening
            }
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onError(error: Int) {
                // Un solo reintento en modo normal si el offline no está
                // disponible en este dispositivo.
                if (preferOffline && !retried && error !in noOfflineRetryErrors) {
                    startListening(preferOffline = false, retried = true)
                    return
                }
                _state.value = State.Error(
                    when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No te escuché bien. Inténtalo de nuevo o escribe la tarea."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Falta el permiso de micrófono."
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                            "Se necesita conexión para el reconocimiento de voz en este dispositivo. Puedes escribir la tarea."
                        else -> "No pude entenderte. Inténtalo de nuevo o escribe la tarea."
                    }
                )
                // Estado terminal: liberar el servicio del sistema de inmediato,
                // sin esperar al próximo uso ni a salir de la pantalla.
                stopInternal()
            }
            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                _state.value = if (text.isNullOrEmpty()) {
                    State.Error("No capté nada. Inténtalo de nuevo o escribe la tarea.")
                } else {
                    State.Result(text)
                }
                // Estado terminal: liberar el servicio del sistema de inmediato.
                stopInternal()
            }
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        }
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(listener)
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                if (preferOffline) {
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                }
            }
            startListening(intent)
        }
    }

    fun stopListening() {
        recognizer?.stopListening()
        if (_state.value is State.Listening) _state.value = State.Idle
    }

    fun consumeResult() {
        if (_state.value is State.Result || _state.value is State.Error) {
            _state.value = State.Idle
        }
    }

    private fun stopInternal() {
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
    }

    fun release() {
        stopInternal()
    }
}
