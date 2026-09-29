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

    /** Entrada suave del estado vacío. */
    fun softEnter(): EnterTransition {
        val duration = 350
        return fadeIn(animationSpec = tween(durationMillis = duration)) +
            slideInVertically(animationSpec = tween(durationMillis = duration)) { it / 8 }
    }

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
