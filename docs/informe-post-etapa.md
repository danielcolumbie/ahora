# INFORME POST-ETAPA — App «Ahora»

Fecha: 2026-09-29. Verificación realizada sobre la rama `master` con las 14 etapas
terminadas (última versión: **1.13.0**, versionCode 15, commit `379454d`).
Inspección hecha sobre el código real, no sobre documentación.

---

## FASE 0.1 — Qué quedó realmente implementado (ANTES → DESPUÉS)

### ETAPA 10 (1.9.0) — Prioridades y fechas
- **Pretendido:** prioridad (sin/baja/media/alta) y fecha límite por tarea.
- **Real:** verificado en código. `TaskPriority` enum, columna `priority` y
  `dueAt` en la entidad, `MIGRATION_1_2` (schema v1→v2 exportado), componente
  compartido `TaskFormFields` (chips de prioridad, atajos Hoy/Mañana,
  DatePicker M3), pills en la fila, orden SQL pendiente→prioridad→fecha,
  respaldo JSON compatible hacia atrás. 25 tests JVM verdes.
- **Parcialmente implementado:** la fecha límite no programa alarma (es solo
  visual; documentado). Sin filtros por prioridad/fecha. Sin insignias en el icono.
- **Nuevo problema:** ninguno.

### ETAPA 11 (1.10.0) — Recurrencia
- **Pretendido:** tareas que se repiten (diaria, entre semana, semanal, mensual).
- **Real:** verificado. `TaskRecurrence` (códigos DAILY/WEEKDAYS/WEEKLY/MONTHLY),
  columna `recurrence` (existía desde el schema v1, sin migración),
  `nextAfter` puro con java.time (sobrevive a cambios de horario), al completar
  se genera la siguiente ocurrencia en una sola transacción
  (`insertNextOccurrence`) conservando y desplazando el recordatorio, y nunca
  se genera una ocurrencia ya vencida. 17 tests JVM verdes.
- **Diferente a lo planeado:** desmarcar la ocurrencia anterior no elimina la
  siguiente ya generada (documentado). Sin intervalos personalizados ni límite
  de repeticiones (documentado).

### ETAPA 12 (1.11.0) — Widget
- **Pretendido:** widget de pantalla de inicio.
- **Real:** verificado. `AhoraWidgetProvider` + `WidgetRefresher` +
  `WidgetTaskService` (RemoteViews, sin Glance ni dependencias nuevas),
  lista de hasta 7 tareas, tocar una fila abre la app, el círculo marca como
  hecha sin abrir la app (pasa por `toggleDone`, respeta recurrencia y alarmas),
  `updatePeriodMillis = 0` y refresco reactivo al observar `observePending()`.
  8 tests JVM verdes.
- **Parcialmente implementado:** el "+" solo abre la app (no hay creación
  rápida en el widget, documentado). Sin insignia de conteo en el icono
  (documentado).

### ETAPA 13 (1.12.0) — Lenguaje natural
- **Pretendido:** crear tareas escribiendo/dictando en lenguaje natural.
- **Real:** verificado. `NaturalLanguageParser` (434 líneas, puro, offline,
  sin dependencias): detecta fecha límite ("hoy", "mañana", días de semana,
  "3 de octubre", "15/10", "en 3 días"), hora ("a las 3pm" → recordatorio,
  nunca en el pasado), prioridad ("urgente"→alta) y recurrencia
  ("todos los días", "cada lunes"...). Lo detectado se remueve del título;
  los chips bajo la barra muestran lo entendido; lo manual siempre gana sobre
  lo detectado. `TaskRepository.add` acepta `reminderAt` y nunca programa en
  el pasado. Tests: `NaturalLanguageParserTest` + ampliación de
  `TaskRepositoryTest`.
- **Parcialmente implementado:** entradas ambiguas quedan en el título
  (documentado, degrada con gracia).

### ETAPA 14 (1.13.0) — Optimización y estabilización final
- **Pretendido:** optimizar y estabilizar.
- **Real:** verificado. Es una etapa pequeña: añade `backup_rules.xml`
  (`fullBackupContent` para API 26–30, donde `dataExtractionRules` no aplica;
  reglas vacías = nada se incluye) y `StabilityAuditTest` (casos borde del
  parser y del respaldo que fijan degradación elegante). La app ya venía
  optimizada de las etapas 6–7.
- **Riesgo pendiente:** ninguno nuevo.

### Etapas 1–9 (resumen de verificación)
Auditoría (1), estabilidad y bug de fecha en Cuba (2 / 1.2.0), 29 tests JVM
(3), recordatorios a prueba de balas (4 / 1.3.0: BootReceiver escucha
BOOT_COMPLETED, TIME_SET, TIMEZONE_CHANGED, MY_PACKAGE_REPLACED; verificación
en frío al abrir; aviso de permiso de alarmas exactas revocado), privacidad +
respaldo JSON con `allowBackup=false` (5 / 1.4.0), fluidez Compose (6 / 1.5.0),
optimización Room/memoria/batería (7 / 1.6.0), rediseño visual minimalista
(8 / 1.7.0), búsqueda (9 / 1.8.0). Todo verificado en código; coherente con
el changelog de `MEJORAS.md`. La 1.2.1 corrigió los interruptores de
sonido/vibración (no cambiaban el canal ya creado; ahora enlazan a ajustes
del sistema).

---

## FASE 0.2 — VALIDACIÓN TÉCNICA (ejecutada 2026-09-29)

| Comprobación | Resultado |
|---|---|
| `assembleDebug` | ✅ OK |
| `assembleRelease` (R8, minify) | ✅ OK (6m 48s) |
| Tests JVM (18 clases, 183 tests) | ✅ 183/183 verdes |
| `lintDebug` | ✅ 0 errores, 48 avisos |
| Tests instrumentados | ⚠️ No ejecutables aquí (sin emulador/dispositivo) |

Notas honestas:
- El worker de tests de Gradle (`testDebugUnitTest`) está roto en este
  entorno (NPE en `SuiteTestClassProcessor`, preexistente y documentado en
  `MEJORAS.md`). Los 183 tests se ejecutaron con JUnitCore directo sobre las
  clases compiladas (classpath = `debugUnitTestRuntimeClasspath` con los
  `classes.jar` extraídos de los AAR y `android.jar` al final).
- Los 48 avisos de lint son todos "hay una versión más nueva disponible"
  (AGP 8.5.2, Room 2.6.1, lifecycle 2.8.3, etc.). Ningún warning de código.
  Actualizar dependencias es deuda técnica opcional, no regresión.
- Se limpiaron ~500 MB de restos de extracciones de AAR de sesiones
  anteriores en `/tmp` (estaba al 100%); no eran archivos de la app.

Revisión de riesgos técnicos (código leído):
- Coroutines: `viewModelScope`, `Dispatchers.IO` en receivers con `goAsync()`,
  `SupervisorJob` en la Application. Sin `GlobalScope`. Sin fugas evidentes.
- Lifecycle: `collectAsStateWithLifecycle`, `WhileSubscribed(5_000)`. Correcto.
- Persistencia: Room v2 con migración 1→2 y schemas exportados (`1.json`,
  `2.json`); DataStore solo para ajustes. Backup JSON idempotente.
- Notificaciones: canal `_v2` de alta importancia con sonido/vibración
  explícitos; `canPostNotifications` antes de `notify()`; `runCatching` en el
  `notify`. Sin `SecurityException` posible.
- Permisos: solo 4 en el manifest (RECORD_AUDIO, POST_NOTIFICATIONS,
  SCHEDULE_EXACT_ALARM, RECEIVE_BOOT_COMPLETED); se piden en el momento de uso.
- APIs obsoletas relevantes: ninguna. `targetSdk 34`, `compileSdk 34`.
- Referencias rotas: ninguna encontrada.

**Regresiones críticas: ninguna.** No hizo falta corregir código antes de
continuar.

---

## FASE 0.3 — VALIDACIÓN FUNCIONAL (sin dispositivo: recorrido estático del código)

| Función | Estado | Dónde se verifica |
|---|---|---|
| Crear tarea (texto, voz, lenguaje natural) | ✅ | HomeScreen → `addTask` → `repository.add` |
| Editar (título, prioridad, fecha, recurrencia) | ✅ | `EditTaskDialog` → `updateDetails` |
| Completar / desmarcar (incl. recurrentes) | ✅ | `toggleDone` (transacción + siguiente ocurrencia) |
| Deshacer eliminación | ✅ | Snackbar en Hoy y en Todas (`CollectUiEvents`) |
| Eliminar | ✅ | `delete` (cancela su alarma) |
| Persistencia | ✅ | Room, schema v2 exportado |
| Navegación (Hoy/Todas/Ajustes, estado restaurado) | ✅ | NavGraph (`saveState`/`restoreState`, `singleTop`) |
| Configuración (tema, notificaciones, respaldo) | ✅ | SettingsScreen + DataStore |
| Modo claro / oscuro / automático | ✅ | `AhoraTheme` + `ThemeMode` |
| Voz | ✅ | `SpeechInputManager` (libera el servicio al terminar/fallar; nunca inventa texto) |
| Recordatorios (exactos, degradación elegante) | ✅ | `ReminderScheduler` (`setExactAndAllowWhileIdle`, fallback inexacto) |
| Notificaciones | ✅ | `NotificationHelper` (canal v2, icono propio) |
| Cancelar recordatorio | ✅ | `clearReminder`, al completar, al eliminar |
| Tras reiniciar / cambio de hora / actualización | ✅ | `BootReceiver` + reconciliación en frío en `AhoraApplication` |
| Permisos (micrófono, notificaciones, alarmas exactas) | ✅ | Se piden en el momento de uso + aviso puntual no insistente |

Sin dispositivo no se pudo probar: el sonido/vibración real de la
notificación, la precisión de las alarmas exactas en Doze, el widget en el
launcher real, el reconocimiento de voz con micrófono, ni los tests
instrumentados (`TaskDaoTest`, `MigrationTest` — compilan OK).

---

## FASE 0.4 — INFORME

### 1. Qué funciona correctamente
Todo lo de la tabla 0.3. La app compila en debug y release, los 183 tests
pasan, lint no reporta errores. Las 5 últimas etapas están implementadas de
verdad en el código (verificado archivo por archivo, no por commits).

### 2. Qué fue implementado
Las 14 etapas del plan, cerradas en la 1.13.0: app offline completa con
prioridades, fechas límite, recurrencia, widget, lenguaje natural, búsqueda,
recordatorios a prueba de balas, respaldo JSON, temas, voz, animaciones
centralizadas y optimización de memoria/batería.

### 3. Qué quedó incompleto (documentado como decisión, no como bug)
- Fecha límite sin alarma propia (es visual; el recordatorio con hora es la
  vía para que "suene").
- Sin filtros por prioridad/fecha en Todas. Sin insignias en el icono.
- Sin intervalos de recurrencia personalizados ni límite de repeticiones.
- Sin creación rápida desde el widget. Sin normalización de tildes en la
  búsqueda ("cafe" no encuentra "café").
- Tests instrumentados sin ejecutar (sin dispositivo).

### 4. Problemas encontrados (no críticos)
- `TaskRow.kt` usa `MaterialTheme.typography.labelSmall` y
  `colorScheme.tertiary`, que `AhoraTheme` no define: caen a los valores por
  defecto de M3. Funciona, pero rompe la promesa de "tipografía centralizada".
- `TaskRow.kt` tiene valores `dp` sueltos (27.dp, 24.dp, 2.dp, 15.dp) fuera
  de `Spacing`, contradiciendo el comentario de la escala.
- `EmptyState` usa `headlineSmall` por defecto de M3 (no está en
  `AhoraTypography`).
- `HomeScreen` maneja 12 estados `rememberSaveable` de borrador (complejo pero
  funciona; oportunidad para el rediseño de la creación).
- Caso borde menor: si una alarma vieja dispara mientras la tarea ya tiene un
  recordatorio nuevo futuro, la notificación podría mostrarse antes de tiempo
  (el `PendingIntent` se sobrescribe con `FLAG_UPDATE_CURRENT`, así que en la
  práctica casi no ocurre).

### 5. Regresiones encontradas
Ninguna.

### 6. Deuda técnica
- Dependencias desactualizadas (AGP 8.5.2, Room 2.6.1, lifecycle 2.8.3,
  navigation 2.7.7, core-ktx 1.13.1...). Actualizar requiere re-verificar
  build+tests; no es urgente.
- Worker de tests de Gradle roto en este entorno (preexistente); el
  workaround JUnitCore está documentado aquí y en `MEJORAS.md`.
- `compileSdk`/`targetSdk` 34 (Android 15 disponible): subir el target es
  trabajo futuro con su propia verificación.

### 7. Elementos visuales actuales que deben conservarse
- Escala `Spacing` (2–32dp) y su test de invariantes.
- Objeto `Motion`: duraciones, springs y retardos centralizados.
- Roles tipográficos fijos (marca, títulos, etiquetas en mayúsculas,
  cuerpo protagonista).
- Paleta reducida: un solo acento (azul eléctrico), divisores sutiles en vez
  de tarjetas, pills neutras (color solo donde comunica: prioridad alta,
  vencida).
- Dark mode como experiencia propia (no inversión automática).
- Filas sin tarjeta sobre el fondo, checkbox circular con spring sutil.

### 8. Elementos visuales que necesitan rediseño (entrada para la FASE 3)
- Centralizar `labelSmall` y el terciario en el tema propio (hoy caen a
  defaults de M3).
- Eliminar los `dp` sueltos de `TaskRow`.
- Revisar `EmptyState` (usa `headlineSmall` genérico).
- Simplificar la creación rápida: 12 estados de borrador piden un diseño de
  "creación rápida vs configuración avanzada" más limpio.
- `SettingsScreen` (313 líneas): agrupar mejor, evitar lista larga.
- Revisar jerarquía y densidad de la pantalla Hoy con muchas tareas.

### 9. Riesgos técnicos que pueden afectar el rediseño
- Bajo: el rediseño toca UI; la lógica (Room, alarmas, parser, widget) está
  aislada y testeada. Mantener esa separación es la regla principal.
- Bajo: `enableEdgeToEdge` ya está activo; verificar insets en cada pantalla
  rediseñada.
- Medio: cualquier cambio en `Task.kt`/`TaskDao` exige migración; el
  rediseño no debería necesitar cambios de schema.
- Medio: el widget usa RemoteViews (no Compose); su estilo debe actualizarse
  a mano si cambia la paleta.
- Ningún riesgo bloquea el inicio del rediseño.

---

## Decisión
Sin regresiones críticas → se continúa con el rediseño premium por bloques
(autorizado por Daniel), empezando por la auditoría visual (FASE 3) y el
design system (FASE 4), con validación (build + tests) después de cada bloque.
