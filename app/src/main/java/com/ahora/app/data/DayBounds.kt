package com.ahora.app.data

import java.time.Instant
import java.time.ZoneId

/**
 * Inicio del día siguiente en la zona local, en milisegundos.
 * Límite exclusivo para la pantalla «Hoy»: una tarea con `dueAt` menor
 * que este valor vence hoy o ya venció (`dueAt` se guarda como inicio
 * del día local). Función pura para poder probarla (auditoría 1.26.0).
 */
fun startOfTomorrowMillis(
    now: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault()
): Long =
    Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        .plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
