package com.ahora.app.ui.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally

/**
 * La "física" de la app en un solo lugar: duraciones, retardos y springs
 * compartidos por todas las animaciones.
 *
 * Sin cambios visibles: centraliza los valores que antes estaban repetidos
 * en cada pantalla, para que un ajuste futuro sea en un punto y no en cinco.
 */
object Motion {

    /** Duración base de la entrada de filas y estados. */
    const val ENTER_DURATION_MILLIS = 280

    /** Paso del retardo escalonado en la primera muestra de la lista. */
    const val STAGGER_STEP_MILLIS = 45

    /** Tope del retardo escalonado: la lista nunca tarda más en arrancar. */
    const val STAGGER_MAX_MILLIS = 360

    /**
     * Retardo escalonado para la entrada inicial de la lista.
     * Función pura para poder probarla sin Compose.
     */
    fun staggerDelayMillis(index: Int, isFirstShow: Boolean): Int =
        if (isFirstShow) (index * STAGGER_STEP_MILLIS).coerceAtMost(STAGGER_MAX_MILLIS) else 0

    /** Entrada de una fila de tarea: fundido + desliz + expansión. */
    fun taskEnter(delayMillis: Int): EnterTransition =
        fadeIn(
            animationSpec = tween(
                durationMillis = ENTER_DURATION_MILLIS,
                delayMillis = delayMillis
            )
        ) + slideInVertically(
            animationSpec = tween(
                durationMillis = ENTER_DURATION_MILLIS,
                delayMillis = delayMillis
            )
        ) { fullHeight -> fullHeight / 3 } + expandVertically(
            animationSpec = tween(
                durationMillis = ENTER_DURATION_MILLIS,
                delayMillis = delayMillis
            )
        )

    /** Duración de la entrada suave del estado vacío. */
    const val SOFT_ENTER_MILLIS = 350

    /** Entrada suave del estado vacío. */
    fun softEnter(): EnterTransition =
        fadeIn(animationSpec = tween(durationMillis = SOFT_ENTER_MILLIS)) +
            slideInVertically(animationSpec = tween(durationMillis = SOFT_ENTER_MILLIS)) { it / 8 }

    /**
     * Duración de las expansiones suaves (chips de feedback del lenguaje
     * natural en la barra de creación). Bloque H: antes un `tween(240)`
     * suelto en HomeScreen; ahora centralizado.
     */
    const val SOFT_EXPAND_MILLIS = 240

    /**
     * Duración de las salidas cortas: la fila que se elimina se desvanece
     * y se colapsa, y los chips de feedback se recogen al vaciar el
     * campo. Rápida y discreta: nadie espera a que una fila termine de
     * desaparecer.
     */
    const val QUICK_EXIT_MILLIS = 200

    /**
     * Expansión suave: fundido + crecimiento vertical, sin rebote.
     * Se usa para los chips de feedback bajo la barra de creación.
     */
    fun softExpand(): EnterTransition =
        fadeIn(
            animationSpec = tween(SOFT_EXPAND_MILLIS, easing = FastOutSlowInEasing)
        ) + expandVertically(
            animationSpec = tween(SOFT_EXPAND_MILLIS, easing = FastOutSlowInEasing)
        )

    /**
     * Salida corta: fundido + colapso vertical. Se usa cuando una fila
     * sale de la lista (eliminar, o completar en «Hoy») y cuando los
     * chips de feedback desaparecen.
     */
    fun softExit(): ExitTransition =
        fadeOut(
            animationSpec = tween(QUICK_EXIT_MILLIS, easing = FastOutSlowInEasing)
        ) + shrinkVertically(
            animationSpec = tween(QUICK_EXIT_MILLIS, easing = FastOutSlowInEasing)
        )

    /** Duración de la entrada de los diálogos (crear/editar/recordatorio). */
    const val DIALOG_ENTER_MILLIS = 200

    /**
     * Entrada de los diálogos: fundido + escala sutil (0.97 → 1), sin
     * rebote. El diálogo aparece con presencia pero sin llamar la
     * atención; al cerrar desaparece al instante (salir es la acción,
     * y debe sentirse inmediata).
     */
    fun dialogEnter(): EnterTransition =
        fadeIn(
            animationSpec = tween(DIALOG_ENTER_MILLIS, easing = FastOutSlowInEasing)
        ) + scaleIn(
            animationSpec = tween(DIALOG_ENTER_MILLIS, easing = FastOutSlowInEasing),
            initialScale = 0.97f
        )

    /**
     * Spring del círculo de completado al marcar/desmarcar: suave, sin
     * rebote exagerado (dampingRatio 0.8). El rediseño premium eliminó el
     * spring con rebote que había antes.
     */
    fun <T> checkSpring(): SpringSpec<T> = spring(
        dampingRatio = 0.8f,
        stiffness = Spring.StiffnessMediumLow
    )

    /** Duración del pulso del micrófono mientras escucha. */
    const val PULSE_DURATION_MILLIS = 1600

    /** Pulso del anillo del micrófono: se expande y se desvanece en bucle. */
    fun <T> pulseSpec(delayMillis: Int): TweenSpec<T> =
        tween(
            durationMillis = PULSE_DURATION_MILLIS,
            delayMillis = delayMillis,
            easing = LinearEasing
        )

    /** Duración de la entrada de pantalla (navegación entre Hoy/Todas/Ajustes). */
    const val SCREEN_ENTER_MILLIS = 280

    /** Duración de la salida de pantalla. */
    const val SCREEN_EXIT_MILLIS = 240

    /**
     * Transición de entrada entre pantallas: fundido + deslizamiento corto.
     * Comunica dirección: avanzar entra desde la derecha, volver desde la izquierda.
     */
    fun screenEnter(reverse: Boolean = false): EnterTransition =
        fadeIn(
            animationSpec = tween(SCREEN_ENTER_MILLIS, easing = FastOutSlowInEasing)
        ) + slideInHorizontally(
            animationSpec = tween(SCREEN_ENTER_MILLIS, easing = FastOutSlowInEasing)
        ) { fullWidth -> if (reverse) -fullWidth / 6 else fullWidth / 6 }

    /** Transición de salida entre pantallas. */
    fun screenExit(reverse: Boolean = false): ExitTransition =
        fadeOut(
            animationSpec = tween(SCREEN_EXIT_MILLIS, easing = FastOutSlowInEasing)
        ) + slideOutHorizontally(
            animationSpec = tween(SCREEN_EXIT_MILLIS, easing = FastOutSlowInEasing)
        ) { fullWidth -> if (reverse) fullWidth / 6 else -fullWidth / 6 }
}
