# Ahora — Mejora continua

Daniel ordenó el 2026-09-28: mejorar la app todos los días, poco a poco,
con parches y mejoras constantes, hasta poder llevarla a más personas.

## Reglas
- Cada día: 1 a 3 mejoras concretas (no más; calidad > cantidad).
- Cada mejora debe compilar limpio (`assembleDebug` + `lintDebug` sin errores).
- El feedback de Daniel tiene prioridad absoluta sobre el backlog.
- Versionado: parches diarios suben el patch (1.0.1, 1.0.2…); funciones
  grandes suben el minor (1.1.0). `versionCode` siempre +1.
- Cada versión: APK en `~/workspace/your_files/ahora-<version>-debug.apk`,
  commit en git con tag `v<version>` y entrada abajo en el Changelog.
- Nada se publica en tiendas sin aprobación de Daniel.

## Backlog (orden de prioridad)
1. Búsqueda de tareas en "Todas".
2. Prioridades (alta/media/baja) con color.
3. Fechas de vencimiento con selector de fecha visual.
4. Etiquetas/categorías con colores.
5. Widget para la pantalla de inicio.
6. Subtareas dentro de una tarea.
7. Estadísticas simples (completadas por semana, racha).
8. ~~Exportar/importar respaldo (JSON)~~ ✅ hecho en 1.4.0 (ETAPA 5).
9. Compartir tarea como texto.
10. Acceso directo (tile) de "nueva tarea por voz".
11. Recordatorios recurrentes (diario/semanal).
12. Modo compacto de lista (más tareas en pantalla).
13. Ordenar manual (arrastrar) en "Todas".

## Feedback de Daniel
- 2026-09-28: «La app no te notifica cuando se cumple el tiempo estimado para hacer la tarea o lo hace muy tenue» → parche de notificaciones en 1.1.0. ✅ resuelto
- 2026-09-28: «Que te salga en la barra de notificaciones del teléfono» → icono propio de campana + canal nuevo de alta importancia. ✅ resuelto
- 2026-09-28: «También la animación se ve muy sosa» → pasada completa de diseño y animación en 1.1.0. ✅ resuelto («Y eso es solo por arriba»: seguir puliendo en próximos parches según su prueba real)

## Changelog
### 1.6.0 (2026-09-29) — optimización interna (ETAPA 7)
- **Menos memoria**: saber si hay recordatorios pendientes ahora es una consulta `EXISTS` (un booleano) en vez de cargar todas las tareas en memoria solo para ver si la lista está vacía.
- **Menos batería en la importación**: el respaldo entra a la base de datos en una sola transacción (lote), no con una transacción por tarea.
- **Menos objetos en la UI**: la lista de tareas reutiliza el programador de alarmas único de la app en vez de construir uno nuevo (con su `getSystemService`) en cada confirmación de recordatorio.
- Verificado: sin trabajo periódico oculto, sin servicios permanentes, sin polling; la reconciliación de alarmas sigue siendo solo al arrancar y ante eventos del sistema. Sin cambios visibles.
- 1 prueba nueva (59 JVM en total, todas verdes): la importación usa un solo lote.
### 1.5.0 (2026-09-29) — fluidez de la interfaz (ETAPA 6)
- **Animaciones centralizadas** en `ui/theme/Motion.kt`: duraciones, retardos escalonados y springs en un solo lugar. Sin cambios visibles — la app se ve y se siente igual, pero el código de movimiento ya no está repetido en cinco archivos.
- **Menos recomposiciones en la lista**: `TaskRow` ahora recibe callbacks estables `(Task) -> Unit` (una sola instancia para todas las filas en vez de una lambda nueva por fila), así Compose salta las filas que no cambiaron.
- El estado del scroll (`rememberLazyListState`) ya no se recrea: la posición de la lista sobrevive a los cambios de la UI.
- 5 pruebas nuevas (58 JVM en total, todas verdes) que fijan los valores de animación para que no cambien sin querer.
### 1.4.0 (2026-09-29) — privacidad y respaldo (ETAPA 5)
- **Respaldo en la nube de Android desactivado** (`allowBackup=false` + reglas de extracción vacías): nada sale del teléfono, ni a la nube ni en transferencia entre dispositivos. La afirmación "tus tareas viven solo en tu teléfono" ahora es 100% cierta.
- **Nuevo: Exportar/Importar en Ajustes → Respaldo.** Guarda tus tareas en un archivo JSON donde tú elijas y restáuralas cuando quieras (p. ej. tras reinstalar). La importación es idempotente (no duplica) y reprograma los recordatorios automáticamente.
- Textos de privacidad honestos en "Acerca de Ahora".
- 12 pruebas nuevas (53 JVM en total, todas verdes): serialización del respaldo (9) e importación en el repositorio (3).
### 1.3.0 (2026-09-29) — recordatorios a prueba de balas (ETAPA 4)
- Los recordatorios se reconcilian con la BD en más casos: además del reinicio, también al cambiar la hora o la zona horaria y al actualizar la app (`BootReceiver` ahora escucha `TIME_SET`, `TIMEZONE_CHANGED` y `MY_PACKAGE_REPLACED`).
- Al abrir la app se verifica en frío que ninguna alarma se haya perdido: se reprograman las futuras (idempotente) y se limpian los recordatorios vencidos para que el pill no muestre horas del pasado. Sin polling ni servicios permanentes.
- Elegir una fecha/hora pasada en el diálogo ya no se descarta en silencio: la app avisa "Esa hora ya pasó, elige una futura".
- Si el sistema revoca el permiso de alarmas exactas (Android 12+), aparece un aviso puntual en la pantalla principal con acceso directo a los ajustes del sistema. No insistente: se descarta y solo vuelve si el permiso se concede y se revoca de nuevo; no aparece si no hay recordatorios pendientes.
- Confirmado: las alarmas ya usan `setExactAndAllowWhileIdle`, lo máximo que Android permite en Doze/ahorro de batería sin servicio permanente.
- 12 pruebas nuevas (41 JVM en total, todas verdes): condición del aviso de permiso (5), validación de fecha pasada (3), limpieza de vencidos y detección de pendientes en el repositorio (2), persistencia del descarte del aviso (2).
### ETAPA 3 (2026-09-29) — tests fundamentales (sin cambios en la app)
- **29 pruebas JVM, todas verdes** (ejecutadas con JUnitCore directo, ver nota abajo):
- `TaskRepositoryTest`: 12 pruebas de la lógica de negocio con DAO y programador falsos (crear, completar/desmarcar, eliminar, deshacer, recordatorios, reprogramación). Reloj inyectable para tiempos deterministas.
- `ReminderSchedulerTest`: 4 pruebas — el código de request del PendingIntent es distinto por tarea (las alarmas no se pisan).
- `SettingsRepositoryTest`: 4 pruebas con DataStore real sobre archivo temporal (valores por defecto y persistencia entre instancias).
- `TaskDateFormatTest` reescrito: 5 pruebas con instantes fijos y zona `America/Havana` — ya no es frágil cerca de la medianoche UTC.
- `ReminderDateTest` (ETAPA 2) sigue verde: 4 pruebas de regresión de la fecha en Cuba.
- Pruebas instrumentadas (compiladas OK, requieren dispositivo/emulador — no se ejecutaron aquí): `TaskDaoTest` con 6 pruebas (insertar, leer, actualizar, borrar, `observeAll`, `observePending`, `getPendingReminders`) sobre Room en memoria, y scaffold de migración `MigrationTest` que valida la v1 contra el schema exportado (punto de partida para la migración 1→2).
- Cambios mínimos en producción solo para hacer el código testeable: interfaz `AlarmScheduler`, reloj inyectable en `TaskRepository`, `SettingsRepository` recibe el `DataStore` ya construido, `requestCodeFor` extraído como función pura.
- Nota: el worker de tests de Gradle (`testDebugUnitTest`) sigue roto en este entorno (NPE en `SuiteTestClassProcessor`, también con tests preexistentes); los tests JVM se ejecutan vía JUnitCore directo sobre las clases compiladas por Gradle (classpath = runtimeClasspath del test, con los `classes.jar` extraídos de los AAR).
- Verificación: `assembleDebug` OK, `lintDebug` OK (49 avisos, 0 errores). Sin bump de versión, sin APK nuevo, sin tag.
### 1.2.1 (2026-09-29) — ajustes honestos (A1)
- Decisión de producto de Daniel: se eliminan los interruptores de Sonido y Vibración de Ajustes (Android no permite cambiarlos en un canal ya creado, así que prometían algo falso).
- En su lugar, la fila "Sonido y vibración" abre los ajustes de notificación del sistema para el canal de la app, que sí puede cambiarlos (con fallback a los ajustes generales de notificaciones).
- Limpieza de código muerto: `NotificationHelper.applyPreferences()` y todo el plumbing de preferencias de sonido/vibración (`SettingsRepository`, `SettingsViewModel`, parámetros de `showReminder`). Se conserva el interruptor maestro de notificaciones, que sí es real.
### 1.2.0 (2026-09-29) — estabilidad (ETAPA 2)
- CRÍTICO: los recordatorios ya se programan en el día local correcto. Antes, en Cuba (y otras zonas al oeste de UTC), elegir una fecha la programaba el día anterior. Pruebas unitarias deterministas para `America/Havana` y `Asia/Tokyo`.
- La app ya no se cae al mostrar una notificación si el permiso fue revocado: comprueba `POST_NOTIFICATIONS` / `areNotificationsEnabled()` y protege `notify()`.
- Las alarmas exactas degradan a inexactas en vez de tumbar la app si el permiso `SCHEDULE_EXACT_ALARM` cambia a mitad de camino.
- El "Deshacer" tras eliminar ya funciona también en la pantalla "Todas" (antes el aviso se perdía).
- Los recordatorios vencidos se limpian solos: al sonar la alarma y al completar la tarea (el pill ya no queda obsoleto).
- La base de datos Room exporta su schema v1 (`app/schemas/`): base para futuras migraciones sin perder datos.
- El reconocimiento de voz libera el servicio del sistema al terminar o fallar (ya no queda reservado en segundo plano).
- Los fallos del ViewModel (marcar, eliminar, deshacer, quitar recordatorio) muestran un aviso en vez de dejar la app en estado roto.
- Corrección del changelog 1.1.0: los interruptores de sonido/vibración NO cambian un canal ya creado (limitación de Android); el texto anterior era inexacto. Los interruptores quedan como están hasta que Daniel decida el producto.
### 1.1.0 (2026-09-28) — diseño y animación
- Transiciones suaves entre pantallas (fundido + deslizamiento).
- Checkbox circular con rebote spring al marcar/desmarcar; el check entra con escala.
- La lista se reordena con suavidad (animateItem) y las tareas entran de forma escalonada.
- Tarjetas de tarea sutiles: esquinas redondeadas, elevación tonal mínima, 8dp de separación.
- Recordatorio como pill con campana en vez de texto plano.
- Anillos pulsantes en el micrófono mientras escucha.
- Estado vacío con mejor jerarquía tipográfica y entrada animada.
- Parche de notificaciones (feedback de Daniel):
  - Icono propio de campana para la barra de estado (el icono del launcher ahí se veía tenue/invisible).
  - Canal nuevo `ahora_recordatorios_v2` de alta importancia con sonido, vibración, luz y visibilidad en pantalla de bloqueo explícitos; se elimina el canal viejo.
  - Los interruptores de sonido/vibración de Ajustes se aplican al crear el canal. Corrección (auditoría ETAPA 1): Android NO permite cambiar el sonido ni la vibración de un canal ya creado, así que los interruptores no tienen efecto real sobre el canal existente — la afirmación anterior era inexacta. Pendiente de decisión de producto (quitarlos o llevar a los ajustes del sistema).
  - El permiso de notificaciones se pide al guardar un recordatorio, no solo en Ajustes.
  - Alarmas exactas en Android 11 y anteriores (antes caían en la vía inexacta); en Android 12+ se avisa con un diálogo para permitirlas en ajustes del sistema.
### 1.0.0 (2026-09-28) — versión inicial
- Crear, editar, completar y eliminar tareas (con deshacer).
- Vistas Hoy / Todas / Ajustes.
- Recordatorios y notificaciones locales.
- Entrada por voz (sin inventar texto si falla).
- Temas claro / oscuro / automático.
- Persistencia local con Room, preferencias con DataStore.
- 100% offline, sin cuentas ni rastreo.
