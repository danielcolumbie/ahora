package com.ahora.app.domain

import com.ahora.app.data.TaskPriority
import com.ahora.app.data.TaskRecurrence
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/**
 * Lenguaje natural al crear tareas (ETAPA 13 del plan maestro).
 *
 * Analiza el texto que escribe (o dicta) el usuario y extrae, sin red y
 * sin dependencias nuevas (solo regex y java.time):
 * - fecha límite: "hoy", "mañana", "pasado mañana", días de la semana
 *   ("el viernes", "este lunes", "el próximo martes"), fechas explícitas
 *   ("3 de octubre", "15/10") y plazos ("en 3 días", "en una semana");
 * - prioridad: "urgente", "importante", "alta prioridad", "baja prioridad"…;
 * - recurrencia: "todos los días", "entre semana", "cada lunes",
 *   "cada semana", "cada mes";
 * - hora: "a las 3", "a las 3pm", "a las 15:30" → recordatorio.
 *
 * Lo detectado se REMUEVE del título ([NaturalLanguageResult.title] es el
 * texto limpio) para que la tarea no quede con "mañana" o "urgente"
 * pegados al nombre. La UI muestra lo entendido en los chips de la
 * captura: esos son el indicador de qué detectó la app.
 *
 * Todo es puro (zona y "ahora" inyectables) para poder probarlo en JVM.
 * Funciona 100% offline: el texto de la entrada por voz pasa por aquí
 * igual que el escrito a mano.
 *
 * Decisiones documentadas (no magia):
 * - "el próximo X" = la próxima ocurrencia de X, salvo que hoy sea X,
 *   en cuyo caso es dentro de 7 días ("el viernes" un viernes = hoy).
 * - "el 3 de octubre" pasado este año = el 3 de octubre del año que viene.
 * - "a las 3" sin am/pm = la próxima vez que sean las 3 (hoy a las 15:00
 *   si aún no pasó; si no, mañana a las 3:00). Con "am"/"pm" o
 *   "de la mañana/tarde/noche" se respeta lo dicho.
 * - Un recordatorio nunca nace en el pasado: si la hora ya pasó hoy,
 *   se corre al día siguiente.
 * - Los números en palabras ("tres", "doce") se entienden en las horas
 *   y en "en N días/semanas" porque el dictado por voz los devuelve así.
 */
data class NaturalLanguageResult(
    /** Título limpio, sin los fragmentos detectados. */
    val title: String,
    /** Fecha límite detectada (solo día), o null si no se dijo fecha. */
    val dueDate: LocalDate?,
    /**
     * Instante del recordatorio en milisegundos (fecha detectada + hora
     * dicha), o null si no se dijo hora. Siempre futuro respecto a [now].
     */
    val reminderAt: Long?,
    val priority: TaskPriority,
    val recurrence: TaskRecurrence
)

/**
 * Analiza [text] y extrae fecha límite, hora, prioridad y recurrencia.
 * [now] y [zone] son inyectables para tests deterministas.
 */
fun parseNaturalLanguage(
    text: String,
    now: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault()
): NaturalLanguageResult {
    val parser = NaturalLanguageParser(text, now, zone)
    return parser.parse()
}

private class NaturalLanguageParser(
    private val original: String,
    now: Long,
    private val zone: ZoneId
) {
    /** Versión normalizada (minúsculas, sin tildes) con la MISMA longitud
     * que el original: así los índices de los matches sirven para recortar
     * el texto original. */
    private val normalized: String = original.map(::normalizeChar).joinToString("")

    private val today: LocalDate =
        java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    private val nowMillis: Long = now

    /** Fragmentos ya reclamados por una categoría (no se reutilizan). */
    private val claimed = mutableListOf<IntRange>()

    private var dueDate: LocalDate? = null
    private var time: DetectedTime? = null
    private var priority = TaskPriority.NONE
    private var recurrence = TaskRecurrence.NONE

    fun parse(): NaturalLanguageResult {
        parseRecurrence()
        parseDate()
        parseTime()
        parsePriority()
        val title = cleanTitle()
        val reminderAt = time?.let { toReminderInstant(it) }
        // Si se dijo hora pero no fecha, la fecha límite es el día del
        // recordatorio: "llamar a las 3" cae en Hoy.
        val effectiveDue = dueDate ?: reminderAt?.let {
            java.time.Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
        }
        return NaturalLanguageResult(
            title = title,
            dueDate = effectiveDue,
            reminderAt = reminderAt,
            priority = priority,
            recurrence = recurrence
        )
    }

    // ---------------------------------------------------------- utilidades

    /** Reclama el span del match si no pisa otro ya reclamado. */
    private fun overlaps(span: IntRange): Boolean =
        claimed.any { it.first <= span.last && span.first <= it.last }

    /**
     * Primer match reclamable entre todos los patrones: gana el que
     * aparezca antes en el texto; a igual posición, el orden de los
     * patrones (de más específico a más general).
     */
    private fun firstMatch(vararg patterns: Regex): MatchResult? {
        var best: MatchResult? = null
        var bestOrder = Int.MAX_VALUE
        patterns.forEachIndexed { order, pattern ->
            var m = pattern.find(normalized)
            while (m != null && overlaps(m.range)) m = m.next()
            if (m != null &&
                (best == null || m.range.first < best.range.first ||
                    (m.range.first == best.range.first && order < bestOrder))
            ) {
                best = m
                bestOrder = order
            }
        }
        if (best != null) claimed.add(best.range)
        return best
    }

    // -------------------------------------------------------- recurrencia

    private fun parseRecurrence() {
        val match = firstMatch(
            Regex("\\b(todos los dias|cada dia|diariamente|a diario)\\b"),
            Regex("\\b(entre semana|de lunes a viernes|dias laborables)\\b"),
            Regex("\\bcada (lunes|martes|miercoles|jueves|viernes|sabado|domingo)\\b"),
            Regex("\\b(cada semana|todas las semanas|semanalmente)\\b"),
            Regex("\\b(cada mes|todos los meses|mensualmente)\\b")
        ) ?: return
        val word = match.value
        recurrence = when {
            word.startsWith("todos los dias") || word == "cada dia" ||
                word == "diariamente" || word == "a diario" -> TaskRecurrence.DAILY
            word.startsWith("entre semana") || word.startsWith("de lunes") ||
                word == "dias laborables" -> TaskRecurrence.WEEKDAYS
            word.startsWith("cada mes") || word.startsWith("todos los meses") ||
                word == "mensualmente" -> TaskRecurrence.MONTHLY
            else -> TaskRecurrence.WEEKLY
        }
        // "cada lunes" además fija la fecha en el próximo lunes.
        val weekdayWord = Regex(
            "lunes|martes|miercoles|jueves|viernes|sabado|domingo"
        ).find(word)?.value
        if (weekdayWord != null && dueDate == null) {
            dueDate = nextWeekday(today, WEEKDAYS_BY_WORD[weekdayWord]!!, strictNextWeek = false)
        }
        // Diaria / entre semana sin fecha explícita: vence hoy para que
        // la tarea aparezca en Hoy desde que se crea.
        if (dueDate == null &&
            (recurrence == TaskRecurrence.DAILY || recurrence == TaskRecurrence.WEEKDAYS)
        ) {
            dueDate = today
        }
    }

    // -------------------------------------------------------------- fecha

    private fun parseDate() {
        // De lo más específico a lo más general: "pasado mañana" contiene
        // "mañana", así que debe ir primero.
        var match = firstMatch(
            Regex("\\bpara pasado manana\\b"),
            Regex("\\bpasado manana\\b")
        )
        if (match != null) {
            dueDate = today.plusDays(2)
            return
        }

        match = firstMatch(
            Regex("\\bpara manana\\b"),
            Regex("\\bmanana\\b")
        )
        if (match != null) {
            dueDate = today.plusDays(1)
            return
        }

        match = firstMatch(
            Regex("\\bpara hoy\\b"),
            Regex("\\bhoy\\b")
        )
        if (match != null) {
            dueDate = today
            return
        }

        // "el próximo martes" (estricto: si hoy es martes, dentro de 7 días).
        match = firstMatch(
            Regex("\\b(el\\s+)?proximo\\s+(lunes|martes|miercoles|jueves|viernes|sabado|domingo)\\b")
        )
        if (match != null) {
            val wd = WEEKDAYS_BY_WORD[match.groupValues[2]]!!
            dueDate = nextWeekday(today, wd, strictNextWeek = true)
            return
        }

        // "el viernes", "este lunes", "viernes" (si hoy es viernes = hoy).
        match = firstMatch(
            Regex("\\b(el\\s+|este\\s+|esta\\s+|del\\s+|para\\s+el\\s+)?(lunes|martes|miercoles|jueves|viernes|sabado|domingo)\\b")
        )
        if (match != null) {
            val wd = WEEKDAYS_BY_WORD[match.groupValues[2]]!!
            dueDate = nextWeekday(today, wd, strictNextWeek = false)
            return
        }

        // "el 3 de octubre" (pasado este año → el año que viene).
        match = firstMatch(
            Regex("\\b(el\\s+|para\\s+el\\s+)?(\\d{1,2})\\s+de\\s+(enero|febrero|marzo|abril|mayo|junio|julio|agosto|septiembre|setiembre|octubre|noviembre|diciembre)\\b")
        )
        if (match != null) {
            val day = match.groupValues[2].toInt()
            val month = MONTHS[match.groupValues[3]]!!
            val monthLength = java.time.YearMonth.of(today.year, month).lengthOfMonth()
            if (day in 1..monthLength) {
                var date = LocalDate.of(today.year, month, day)
                if (date.isBefore(today)) date = date.plusYears(1)
                dueDate = date
                return
            }
            // Día imposible ("45 de octubre"): no se reclama como fecha;
            // queda en el título tal cual. Hay que liberar el span.
            // NOTA (migración SDK 36): no usar removeLast(); el toolchain
            // nuevo lo resuelve a List.removeLast() de JDK 21, que no existe
            // en el runtime de tests (JDK 17) ni en Android API < 36.
            claimed.removeAt(claimed.lastIndex)
        }

        // "15/10" o "15/10/2026".
        match = firstMatch(
            Regex("\\b(el\\s+|para\\s+el\\s+)?(\\d{1,2})/(\\d{1,2})(?:/(\\d{2,4}))?\\b")
        )
        if (match != null) {
            val day = match.groupValues[2].toInt()
            val month = match.groupValues[3].toInt()
            val yearText = match.groupValues[4]
            if (month in 1..12) {
                val year = when {
                    yearText.isEmpty() -> today.year
                    yearText.length == 2 -> 2000 + yearText.toInt()
                    else -> yearText.toInt()
                }
                val monthLength = runCatching {
                    java.time.YearMonth.of(year, month).lengthOfMonth()
                }.getOrNull()
                if (monthLength != null && day in 1..monthLength) {
                    var date = LocalDate.of(year, month, day)
                    if (yearText.isEmpty() && date.isBefore(today)) {
                        date = date.plusYears(1)
                    }
                    dueDate = date
                    return
                }
            }
            // Igual que arriba: removeAt(lastIndex) en vez de removeLast()
            // (ver nota de la migración a SDK 36).
            claimed.removeAt(claimed.lastIndex)
        }

        // "en 3 días", "en una semana", "en 2 meses".
        match = firstMatch(
            Regex("\\ben\\s+(\\d{1,2}|una|un|uno|dos|tres|cuatro|cinco|seis|siete|ocho|nueve|diez|once|doce)\\s+semanas?\\b"),
            Regex("\\ben\\s+(\\d{1,2}|una|un|uno|dos|tres|cuatro|cinco|seis|siete|ocho|nueve|diez|once|doce)\\s+mes(es)?\\b"),
            Regex("\\ben\\s+(\\d{1,3}|una|un|uno|dos|tres|cuatro|cinco|seis|siete|ocho|nueve|diez|once|doce)\\s+dias\\b")
        )
        if (match != null) {
            val n = match.groupValues[1].toIntOrNull() ?: WORD_NUMBERS[match.groupValues[1]]!!
            dueDate = when {
                match.value.contains("semana") -> today.plusWeeks(n.toLong())
                match.value.contains("mes") -> today.plusMonths(n.toLong())
                else -> today.plusDays(n.toLong())
            }
        }
    }

    // --------------------------------------------------------------- hora

    private fun parseTime() {
        val number = "(\\d{1,2}|una|un|uno|dos|tres|cuatro|cinco|seis|siete|ocho|nueve|diez|once|doce)"
        val match = firstMatch(
            Regex("\\ba\\s+las\\s+$number(?::(\\d{2}))?\\s+de\\s+la\\s+(manana|tarde|noche)\\b"),
            Regex("\\ba\\s+las\\s+$number(?::(\\d{2}))?\\s*(am|pm|a\\.m\\.|p\\.m\\.)\\b"),
            Regex("\\ba\\s+las\\s+$number(?::(\\d{2}))?(?!\\d)\\b")
        ) ?: return
        val hourText = match.groupValues[1]
        val hour = hourText.toIntOrNull() ?: WORD_NUMBERS[hourText] ?: return
        val minute = match.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
        if (hour !in 0..23 || minute !in 0..59) {
            // Igual que arriba: removeAt(lastIndex) en vez de removeLast()
            // (ver nota de la migración a SDK 36).
            claimed.removeAt(claimed.lastIndex)
            return
        }
        val marker = match.groupValues.getOrNull(3).orEmpty().replace(".", "")
        time = when {
            marker.startsWith("am") || marker == "manana" -> DetectedTime(hour % 12, minute)
            marker.startsWith("pm") || marker == "tarde" || marker == "noche" ->
                DetectedTime(if (hour < 12) hour + 12 else hour, minute)
            // Sin marcador: la hora se resuelve a la próxima ocurrencia.
            else -> DetectedTime(hour, minute, explicit = false)
        }
    }

    private data class DetectedTime(val hour: Int, val minute: Int, val explicit: Boolean = true)

    /**
     * Convierte la hora detectada al instante del recordatorio, sobre la
     * fecha detectada (o hoy). Nunca en el pasado: si la hora ya pasó,
     * se corre al día siguiente. Sin marcador am/pm se elige la próxima
     * ocurrencia de esa hora ("a las 3" a las 10:00 = hoy 15:00).
     */
    private fun toReminderInstant(detected: DetectedTime): Long {
        val base = dueDate ?: today
        fun instantOn(date: LocalDate, h: Int): Long =
            date.atTime(h, detected.minute).atZone(zone).toInstant().toEpochMilli()

        if (detected.explicit) {
            val instant = instantOn(base, detected.hour)
            return if (instant > nowMillis) instant
            else instantOn(base.plusDays(1), detected.hour)
        }
        // Sin marcador: candidatos en orden (la hora tal cual, y +12h).
        val candidates = buildList {
            add(detected.hour)
            if (detected.hour in 0..11) add(detected.hour + 12)
        }
        for (h in candidates) {
            val instant = instantOn(base, h)
            if (instant > nowMillis) return instant
        }
        return instantOn(base.plusDays(1), candidates.first())
    }

    // ----------------------------------------------------------- prioridad

    private fun parsePriority() {
        val match = firstMatch(
            Regex("\\b(baja prioridad|prioridad baja)\\b"),
            Regex("\\b(media prioridad|prioridad media)\\b"),
            Regex("\\b(urgente(mente)?|importante|muy importante|alta prioridad|prioridad alta|maxima prioridad|prioritari[oa]s?)\\b")
        ) ?: return
        val word = match.value
        priority = when {
            word.contains("baja") -> TaskPriority.LOW
            word.contains("media") -> TaskPriority.MEDIUM
            else -> TaskPriority.HIGH
        }
    }

    // ------------------------------------------------------- título limpio

    /**
     * Recorta los fragmentos detectados del texto ORIGINAL y limpia
     * espacios y puntuación sobrantes. Si no queda nada (el usuario solo
     * escribió "mañana"), devuelve "" y quien llama decide el fallback.
     */
    private fun cleanTitle(): String {
        if (claimed.isEmpty()) return original.trim()
        val spans = claimed.sortedBy { it.first }
        val out = StringBuilder()
        var cursor = 0
        for (span in spans) {
            if (span.first > cursor) out.append(original.substring(cursor, span.first))
            cursor = maxOf(cursor, span.last + 1)
        }
        if (cursor < original.length) out.append(original.substring(cursor))
        return out.toString()
            .replace(Regex("\\s+"), " ")
            .trim()
            .trim(',', ';', ':', '.', '¡', '!', '¿', '?', '-', '—', '(', ')', '"')
            .trim()
    }
}

private fun normalizeChar(c: Char): Char = when (c.lowercaseChar()) {
    'á', 'à', 'ä', 'â' -> 'a'
    'é', 'è', 'ë', 'ê' -> 'e'
    'í', 'ì', 'ï', 'î' -> 'i'
    'ó', 'ò', 'ö', 'ô' -> 'o'
    'ú', 'ù', 'ü', 'û' -> 'u'
    // La ñ se trata como n para el matching ("mañana" = "manana"):
    // casi nadie escribe la tilde al dictar o teclear rápido.
    'ñ' -> 'n'
    else -> c.lowercaseChar()
}

private val WEEKDAYS_BY_WORD: Map<String, DayOfWeek> = mapOf(
    "lunes" to DayOfWeek.MONDAY,
    "martes" to DayOfWeek.TUESDAY,
    "miercoles" to DayOfWeek.WEDNESDAY,
    "jueves" to DayOfWeek.THURSDAY,
    "viernes" to DayOfWeek.FRIDAY,
    "sabado" to DayOfWeek.SATURDAY,
    "domingo" to DayOfWeek.SUNDAY
)

private val MONTHS: Map<String, Int> = mapOf(
    "enero" to 1, "febrero" to 2, "marzo" to 3, "abril" to 4,
    "mayo" to 5, "junio" to 6, "julio" to 7, "agosto" to 8,
    "septiembre" to 9, "setiembre" to 9, "octubre" to 10,
    "noviembre" to 11, "diciembre" to 12
)

private val WORD_NUMBERS: Map<String, Int> = mapOf(
    "un" to 1, "uno" to 1, "una" to 1, "dos" to 2, "tres" to 3,
    "cuatro" to 4, "cinco" to 5, "seis" to 6, "siete" to 7,
    "ocho" to 8, "nueve" to 9, "diez" to 10, "once" to 11, "doce" to 12
)

/** Próxima ocurrencia del día [wd] desde [from]. */
private fun nextWeekday(from: LocalDate, wd: DayOfWeek, strictNextWeek: Boolean): LocalDate {
    if (!strictNextWeek && from.dayOfWeek == wd) return from
    var date = from
    do {
        date = date.plusDays(1)
    } while (date.dayOfWeek != wd)
    return date
}
