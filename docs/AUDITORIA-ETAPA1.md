# Informe de auditoría — ETAPA 1 (solo lectura)

**Repo:** `~/workspace/apps/ahora/` · **Rama:** `master` · **HEAD:** `b2885ba` · **Fecha:** 2026-09-29
**Método:** lectura directa de los 30 archivos Kotlin + manifiesto + recursos + `PLAN-EVOLUCION.md` + `MEJORAS.md`; `lintDebug` ejecutado (0 errores, 43 warnings); una afirmación crítica verificada con prueba determinista; dos afirmaciones de plataforma verificadas contra documentación.

No se modificó ningún archivo en esta etapa.

## Estado del repo
- Commits recientes: `b2885ba`, `3b89169`, `a68935b`, `3382e5d`, `e29dff4`; tags `v1.0.0`, `v1.1.0`. Working tree limpio salvo `PLAN-EVOLUCION.md` sin seguimiento.
- Config confirmada: AGP 8.5.2, Kotlin 2.0.21, Compose BOM 2024.06.00, Room 2.6.1, DataStore 1.1.1, compile/target SDK 34, minSdk 26, v1.1.0 (versionCode 2). Room v1, `exportSchema=false`, sin migraciones. `allowBackup=true`; backup incluye BD Room + `datastore/`. Permisos: micrófono, notificaciones, alarmas exactas, boot. Canal real: `ahora_recordatorios_v2`. Sin Firebase/analytics/servidor.

---

## CRÍTICO

### C1. El recordatorio se programa UN DÍA ANTES en Cuba (y toda zona UTC−x)
**Archivo:** `app/src/main/java/com/ahora/app/ui/components/ReminderDialog.kt:35-40`
```kotlin
val dateMillis = datePickerState.selectedDateMillis ?: return@Button
val cal = Calendar.getInstance()          // zona local
cal.timeInMillis = dateMillis             // <- medianoche UTC
cal.set(Calendar.HOUR_OF_DAY, hour)       // fija hora sobre fecha local desplazada
```
`DatePicker.selectedDateMillis` es medianoche **UTC** (documentado en Material3). Al cargarla en un `Calendar` local, en Cuba (UTC−4) esa marca cae en las 20:00 **del día anterior**; luego se fija la hora elegida sobre ese día equivocado. **Prueba determinista ejecutada:** elegir 30-sep-2026 + 09:30 → alarma programada el **29-sep-2026 09:30** hora de Cuba. Afecta a la función central de la app para su propio usuario objetivo. Dirección de corrección (etapa posterior): desplazar por el offset local antes de fijar la hora, o convertir vía `Instant.atZone(systemDefault).toLocalDate()`.

## ALTO

### A1. Los interruptores de Sonido/Vibración no tienen efecto real (y el changelog afirma lo contrario)
**Archivos:** `notifications/NotificationHelper.kt:66-84` (`applyPreferences`), `ui/settings/SettingsScreen.kt:46-49` (`LaunchedEffect(sound, vibration)`), `MEJORAS.md` (changelog 1.1.0: *"los interruptores… ahora se aplican de verdad al canal del sistema"*).
Verificado en documentación: el sonido y la vibración de un canal **se fijan en su primera creación y no se pueden cambiar después**; re-llamar a `createNotificationChannel()` con el mismo ID solo actualiza nombre/descripción (la importancia solo puede bajarse). `applyPreferences()` es, por tanto, un no-op sobre el canal existente: los switches prometen algo que Android no permite. Requiere decisión de producto en ETAPA 4 (p. ej., eliminar los switches, o versionar el canal).

### A2. `notify()` sin comprobar el permiso del sistema → posible caída justo al sonar la alarma
**Archivos:** `notifications/ReminderReceiver.kt:23-39`, `notifications/NotificationHelper.kt:87-112`.
El receiver solo consulta el interruptor **interno** (`settingsRepository.notificationsEnabled`), nunca el permiso real de Android. En Android 13+, si el usuario revoca `POST_NOTIFICATIONS` a nivel de sistema, la llamada a `manager.notify()` es un error (la documentación oficial muestra el guard `checkSelfPermission` antes de `notify`; puede lanzar `SecurityException`) dentro de una corrutina **sin manejador** → caída del proceso exactamente cuando debe sonar el recordatorio. Falta comprobar `POST_NOTIFICATIONS`/`areNotificationsEnabled()` o envolver en try/catch.

### A3. Eliminar desde "Todas" pierde el "Deshacer" (y cuelga una corrutina)
**Archivos:** `ui/MainViewModel.kt:78-84`, `ui/screens/AllTasksScreen.kt`, `ui/screens/HomeScreen.kt:60-76`.
`deleteTask` emite `UiEvent.TaskDeleted` por `MutableSharedFlow` sin replay ni buffer, pero **solo `HomeScreen`** recoge eventos y muestra el Snackbar. En "Todas" no hay recolector: el evento se pierde (o llega tarde al volver a Inicio, porque `emit` suspende sin colectores), no hay "Deshacer", y la corrutina del `launch` queda suspendida indefinidamente. Pérdida de recuperación ante borrado accidental.

### A4. Sin estrategia de migraciones Room
**Archivo:** `data/AhoraDatabase.kt` — versión 1, `exportSchema = false`, sin schemas ni migraciones. Hoy no rompe nada, pero **bloquea ETAPA 7**: antes de tocar el modelo hay que activar `exportSchema`, guardar el schema v1 y añadir test de migración, o cualquier cambio futuro puede borrar las tareas de los usuarios.

### A5. `setExact*` sin comprobar `canScheduleExactAlarms()` en 3 rutas sin try/catch
**Archivos:** `notifications/ReminderScheduler.kt:53-64`, `notifications/BootReceiver.kt`, `ui/MainViewModel.kt:64-76,96-102`.
En Android 12+, llamar a `setExactAndAllowWhileIdle` sin el permiso de alarmas exactas lanza `SecurityException`. Solo la ruta `setReminder` está protegida (`runCatching`); `BootReceiver.rescheduleAll`, `toggleDone` (al desmarcar) y `restore` (deshacer) llaman a `schedule()` sin red. Si el usuario niega el permiso en ajustes del sistema: caída en boot y al restaurar tareas.

## MEDIO

### M1. Afirmación de privacidad inexacta frente a backup real
`AndroidManifest.xml` (`allowBackup="true"`) + `backup_rules.xml`/`data_extraction_rules.xml` incluyen la BD Room y DataStore en el backup en la nube y transferencia D2D; pero `SettingsRepository.kt:15` ("Todo permanece en el dispositivo") y el diálogo "Acerca de" (`SettingsScreen.kt:118`, "Tus tareas viven solo en tu teléfono") afirman lo contrario. No hay servidor propio (cierto), pero Android sí puede copiar esos datos fuera del teléfono. Decisión consciente pendiente en ETAPA 5 (plan §9).

### M2. `reminderAt` nunca se limpia: pill obsoleta para siempre
Nada limpia `reminderAt` al disparar (`ReminderReceiver` no lo toca) ni al completar (`toggleDone` cancela la alarma pero conserva el campo). Resultado: `TaskRow.kt:86-89` muestra el pill ("Hoy · 14:30") eternamente en tareas ya avisadas o completadas. Además `rescheduleAll` filtra por futuro, así que tras reinicio esos recordatorios muertos simplemente quedan como texto confuso.

### M3. Operaciones del ViewModel sin manejo de fallos
`toggleDone`, `deleteTask`, `undoDelete`, `clearReminder` (`MainViewModel.kt:56-102`) lanzan Room/AlarmManager sin `runCatching` (solo `add`, `updateTitle` y `setReminder` lo tienen). Una excepción en `viewModelScope` cancela el scope y puede tumbar la app.

### M4. `SpeechRecognizer` no se destruye en estados terminales
`voice/SpeechInputManager.kt:96-108` — `onResults`/`onError` no llaman a `stopInternal()`; el reconocedor queda vinculado al servicio del sistema hasta el próximo uso o hasta salir de la pantalla. No es un leak permanente (`DisposableEffect` sí libera en `HomeScreen.kt`), pero retiene un recurso del sistema innecesariamente.

### M5. Reprogramación incompleta ante eventos del sistema
Solo se escucha `BOOT_COMPLETED` (`BootReceiver.kt`, manifiesto). Falta evaluar `TIME_SET`/`TIMEZONE_CHANGED` (cambio manual de hora/zona — relevante en Cuba), `MY_PACKAGE_REPLACED` (actualización) y revocación de alarmas exactas. No hay polling (bien), pero hay huecos reales de reprogramación.

### M6. Cobertura de tests casi nula
Solo existe `app/src/test/.../TaskDateFormatTest.kt` (4 tests de formato de fecha, correctos). Sin tests de DAO, repositorio, scheduler, receiver ni migraciones. ETAPA 3 deberá cubrir al menos C1, A2/A5 y A4.

### M7. Fecha pasada en el diálogo se descarta en silencio
`ReminderDialog.kt:44-48` — si la fecha/hora elegida ya pasó, el diálogo se cierra sin ningún mensaje. El usuario no sabe por qué no se guardó nada.

## BAJO
- **B1.** `NotificationHelper.kt:66` — chequeo `SDK_INT < O` obsoleto (minSdk 26); único warning de código real del lint.
- **B2.** `ReminderDialog.kt:82` — `is24Hour = true` hardcodeado; debería respetar el ajuste del sistema.
- **B3.** `ReminderScheduler.kt:71-74` — `cancel()` crea el `PendingIntent` (con `FLAG_UPDATE_CURRENT`) antes de cancelarlo; deja un PI huérfano en el sistema.
- **B4.** `TasksColumn.kt:118-130` — lógica de stagger con `shownIds` redundante/frágil (`isNew && shownIds.isEmpty()`); funciona pero merece simplificación cuando se toque esa lista.
- **B5.** `MainActivity.kt` — `enableEdgeToEdge()` + `setDecorFitsSystemWindows(window, false)` duplicado (el primero ya lo hace); inofensivo.
- **B6.** 42 warnings de lint son solo "hay versión más nueva" de dependencias — **decisión: no tocar** durante estabilidad (plan §2).
- **B7.** `material-icons-extended` — posible peso extra; no afirmo impacto sin medir el AAB release.

## Lo que está bien (no cambiar por moda)
DI manual con `AppContainer` (suficiente para este tamaño; **no** Hilt), sin queries en main thread, `stateIn`+`WhileSubscribed(5_000)`, `collectAsStateWithLifecycle`, alarmas exactas sin polling ni servicio permanente, `PendingIntent` por tarea (reprogramar sustituye, no duplica), cancelar alarma al eliminar/completar, `goAsync()`+`Dispatchers.IO` en receivers, voz con revisión previa (no autoguardado), claves estables en la lista, canal viejo eliminado al migrar, ProGuard mínimo razonable, sin rastreo.

## Recomendación para ETAPA 2 (estabilidad)
Orden sugerido por impacto/riesgo: **C1** (corrige fecha + test de regresión con zona `America/Havana`) → **A2+A5** (guards de permiso antes de `notify`/`setExact`, `canScheduleExactAlarms()`) → **A3** (canal de eventos con buffer o Snackbar también en "Todas") → **A1** (decisión de producto sobre los switches) → **A4** (exportSchema + schema v1 como prerrequisito de ETAPA 7) → M2/M3/M4.
