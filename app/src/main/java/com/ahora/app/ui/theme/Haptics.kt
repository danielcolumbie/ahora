package com.ahora.app.ui.theme

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * Patrones de feedback háptico de Ahora (FASE 9, bloque H).
 *
 * Revisión holística (2026-09-29): el bloque D usaba
 * [HapticFeedbackType.TextHandleMove] para el tick al marcar/desmarcar
 * porque `ClockTick` no existe en este BOM de Compose (2024.06). Esa
 * elección es la correcta y se mantiene: en este BOM solo existen
 * `LongPress` (largo y fuerte, estilo tecla virtual — no sirve para una
 * confirmación rápida) y `TextHandleMove` (un tick corto y ligero, el
 * mismo que Android usa al mover el cursor de texto). Cuando se actualice
 * el BOM se puede reevaluar `ClockTick`, pero el valor táctil sería el
 * mismo: un tick sutil.
 *
 * Respeto al sistema: `performHapticFeedback` no vibra si el usuario
 * tiene desactivada la respuesta háptica a nivel de sistema — no hace
 * falta comprobar nada a mano.
 *
 * Regla de uso: SOLO como confirmación de una acción completada, nunca
 * como decoración. Usos aprobados:
 * - marcar/desmarcar una tarea,
 * - alternar la sección «Completadas» (colapsar/expandir),
 * - enviar desde la barra de creación rápida,
 * - guardar en el diálogo de edición,
 * - guardar un recordatorio.
 *
 * Deliberadamente SIN háptico (documentado en el bloque H):
 * - eliminar (la confirmación es el snackbar con Deshacer; vibrar en una
 *   acción destructiva se siente como un castigo),
 * - deshacer (el snackbar + la reaparición de la fila ya confirman),
 * - quitar recordatorio (destructiva leve; basta lo visual),
 * - chips de prioridad/fecha/recurrencia mientras se elige (vibrar en
 *   cada toque al decidir sería molesto),
 * - cambio de tema (el cambio visual de toda la pantalla ya es la
 *   confirmación más fuerte posible),
 * - navegación entre pestañas (Material 3 no vibra por defecto; seguir
 *   la plataforma es más consistente que inventar),
 * - pulsar el micrófono (ya tiene el pulso animado + «Escuchando…»).
 */
object Haptics {

    /**
     * Tick corto y ligero como confirmación (ver la revisión arriba para
     * por qué [HapticFeedbackType.TextHandleMove] es el tipo correcto en
     * este BOM). Si el sistema tiene la vibración desactivada, no pasa nada.
     */
    fun tick(feedback: HapticFeedback) {
        feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
}
