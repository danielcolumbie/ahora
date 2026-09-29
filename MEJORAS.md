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
1. ~~Búsqueda de tareas en "Todas".~~ ✅ hecho en 1.8.0 (ETAPA 9).
2. ~~Prioridades (alta/media/baja) con color.~~ ✅ hecho en 1.9.0 (ETAPA 10).
3. ~~Fechas de vencimiento con selector de fecha visual.~~ ✅ hecho en 1.9.0 (ETAPA 10).
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

### 1.12.0 (2026-09-29) — lenguaje natural (ETAPA 13)
- **Nuevo: la app entiende lo que escribes (o dictas) al crear una tarea.** Parser propio en Kotlin puro (`domain/NaturalLanguageParser.kt`), sin dependencias nuevas y 100% offline: extrae **fecha límite** («hoy», «mañana», «pasado mañana», días de semana —«el viernes», «este lunes», «el próximo martes»—, fechas explícitas —«3 de octubre», «15/10»— y plazos —«en 3 días», «en una semana»), **prioridad** («urgente», «importante», «alta/baja/media prioridad»), **recurrencia** («todos los días», «entre semana», «cada lunes», «cada semana», «cada mes») y **hora** («a las 3pm», «a las 15:30», «a las tres de la tarde»).
- **Lo detectado no queda pegado al título**: se remueve del texto («pagar la luz mañana» se guarda como «pagar la luz») y se refleja al instante en los chips bajo la barra de captura (fecha, prioridad, recurrencia) más un indicador de recordatorio con X para quitarlo: ese es el aviso de qué entendió la app. **Lo que tocas a mano siempre gana**: si cambias un chip, el parser deja de tocar ese campo hasta que vacíes el texto.
- **Las horas programan recordatorio de verdad**: «llamar a las 3pm» crea la tarea y le pone la alarma a las 15:00 (pasa por el sistema de recordatorios existente, con su permiso de alarmas exactas). Nunca nace en el pasado: si la hora ya pasó hoy, se corre a mañana.
- **Funciona con la entrada por voz**: el texto dictado pasa por el mismo parser (entiende números en palabras —«a las tres», «en tres días»— porque el reconocedor los devuelve así; la ñ se trata como n porque casi nadie la teclea).
- Decisiones documentadas en el código: «el próximo X» dicho un X = dentro de 7 días (si no, el X que viene, hoy incluido); fecha pasada este año → el año que viene; «a las 3» sin am/pm = la próxima vez que sean las 3; «cada lunes» fija además la fecha en el próximo lunes; «cada día»/«entre semana» sin fecha vencen hoy; «setiembre» se entiende.
- 50 pruebas nuevas (172 JVM en total, todas verdes): 48 del parser (fechas relativas, días de semana, fechas explícitas, plazos, prioridades, recurrencia, horas, combinaciones, y casos que NO deben coincidir: «urgencia», «importancia», «cada tanto», «45 de octubre») y 2 del repositorio (crear con recordatorio futuro programa la alarma; con recordatorio pasado no programa ni guarda).
- **Qué NO se hizo**: horas relativas («en una hora»), momentos del día sin hora («por la mañana», «al mediodía»), fechas como «el 15» a secas (ambiguo: puede ser cantidad), intervalos personalizados de repetición («cada 3 días»), ni parser en el diálogo de edición (solo en la creación rápida).
- **Próxima etapa pendiente**: ETAPA 14 — Optimización y estabilización final.
- Nota: el worker de tests de Gradle (`testDebugUnitTest`) sigue roto en este entorno (NPE en `SuiteTestClassProcessor`); los tests JVM se ejecutaron vía JUnitCore directo sobre las clases compiladas (classpath = `debugUnitTestRuntimeClasspath` con los `classes.jar` extraídos de los AAR; el `android.jar` va AL FINAL para no opacar el `org.json:json` real con sus stubs). OJO para la próxima: hay que excluir el `classes.jar` de `intermediates/runtime_app_classes_jar` (copia vieja de las clases propias) y poner las clases frescas (`tmp/kotlin-classes`) PRIMERO en el classpath, o salen `NoSuchMethodError` fantasma.
### 1.11.0 (2026-09-29) — widget de pantalla de inicio (ETAPA 12)
- **Nuevo: widget "Tareas pendientes de hoy"** (`AppWidgetProvider` + `RemoteViews`, sin Glance ni dependencias nuevas): encabezado con la marca y la fecha, lista de hasta 7 tareas con prioridad y fecha límite, estado vacío "Sin tareas pendientes". Tocar una fila abre la app; el círculo de cada fila **marca la tarea como hecha sin abrir la app** (pasa por `TaskRepository.toggleDone`, así que respeta recurrencia y alarmas igual que en la app); el botón "+" abre la app.
- **Selección de tareas**: las vencidas y las de hoy van primero (estables), luego las próximas con el mismo orden de la pantalla "Hoy" (prioridad, fecha). La lógica es pura (`widget/WidgetContent.kt`) y la consulta está acotada en SQL (`getPendingForWidget(limit)`): no se materializa toda la tabla.
- **Sin polling ni gasto de batería**: `updatePeriodMillis = 0`. El refresco es reactivo — `AhoraApplication` observa `observePending()` y refresca el widget solo cuando cambia la BD (crear, completar, editar, borrar, importar). Sin servicios permanentes ni temporizadores.
- Estilo acorde a la app: paleta propia en claro y oscuro (`values-night`), sin tarjetas ni sombras.
- 8 pruebas nuevas (122 JVM en total, todas verdes): selección/orden/cap, etiquetas de fecha, overdue, prioridad.
- **Qué NO se hizo**: creación rápida de tareas desde el widget (el "+" solo abre la app; duplicar la captura en RemoteViews complicaba sin aportar), ni insignia de conteo en el icono. Si el proceso está muerto y pasa la medianoche, las etiquetas "Hoy" se actualizan en el próximo evento (no hay despertador periódico por diseño, para no gastar batería).
- **Próxima etapa pendiente**: ETAPA 13 — Lenguaje natural.
- Nota: el worker de tests de Gradle (`testDebugUnitTest`) sigue roto en este entorno (NPE en `SuiteTestClassProcessor`); los tests JVM se ejecutaron vía JUnitCore directo sobre las clases compiladas (classpath = `debugUnitTestRuntimeClasspath` con los `classes.jar` extraídos de los AAR; el `android.jar` va AL FINAL para no opacar el `org.json:json` real con sus stubs).
### 1.10.0 (2026-09-29) — recurrencia (ETAPA 11)
- **Nuevo: tareas recurrentes** (Sin repetición / Todos los días / Entre semana / Semanal / Mensual). Se elige al crear (campos bajo la barra de captura mientras escribes, junto a prioridad y fecha) y al editar (mismo componente `TaskFormFields` ampliado). En la fila se muestra como pill con icono de repetición, neutra como las demás.
- **Al completar una tarea recurrente, la ocurrencia queda marcada como hecha y se genera la siguiente automáticamente**, con su fecha límite y su recordatorio desplazados según la regla. La siguiente ocurrencia **conserva el recordatorio**: si la tarea sonaba cada día a las 8:00, la próxima también suena a las 8:00 (la alarma se reprograma con el sistema, sin duplicados: la vieja se cancela primero).
- **Nunca nace vencida**: si completas tarde una tarea atrasada, la siguiente ocurrencia avanza hasta quedar en el futuro. El avance usa la hora local (java.time), no 24h fijas: sobrevive a los cambios de horario (probado con el día de 25 horas).
- **Sin cambios de base de datos**: la columna `recurrence` ya existía desde el schema v1 reservada para esto; se guarda el código (`DAILY`, `WEEKDAYS`, `WEEKLY`, `MONTHLY`) o NULL si no se repite. Sin migración, ningún dato previo cambia de significado. El respaldo JSON ya la incluía.
- 17 pruebas nuevas (114 JVM en total, todas verdes): enum y mapeo de códigos (3), cálculo de siguiente ocurrencia con zona fija America/Havana — diaria, entre semana (salta sábado/domingo), semanal, mensual (31 ene → 28 feb), completar tarde, cambio de horario con America/New_York (10), y repositorio — completar genera la siguiente, conserva recordatorio y lo reprograma, sin fechas también regenera, no recurrente no duplica, editar recurrencia (7).
- Sin dependencias nuevas, todo offline. Sin polling ni servicios: la recurrencia vive en el evento de completar, no en un worker periódico.
- **Qué NO se hizo**: intervalos personalizados (cada N días, días específicos de la semana), "repetir X veces y parar", ni deshacer la regeneración al desmarcar (desmarcar solo revive la ocurrencia marcada; la siguiente ya generada sigue ahí). Las ocurrencias completadas se acumulan en la lista de hechas como cualquier tarea completada.
- **Próxima etapa pendiente**: ETAPA 12 — Widget.
- Nota: el worker de tests de Gradle (`testDebugUnitTest`) sigue roto en este entorno (NPE en `SuiteTestClassProcessor`); los tests JVM se ejecutaron vía JUnitCore directo sobre las clases compiladas (classpath = `debugUnitTestRuntimeClasspath` con los `classes.jar` extraídos de los AAR; el `android.jar` va AL FINAL para no opacar el `org.json:json` real con sus stubs).

### 1.9.0 (2026-09-29) — prioridades y fechas (ETAPA 10)
- **Nuevo: prioridad por tarea** (Sin prioridad / Baja / Media / Alta). Se elige al crear (campos que aparecen bajo la barra de captura mientras escribes) y al editar (diálogo de edición ampliado con el mismo componente `TaskFormFields`). En la fila se muestra como pill con bandera: Alta en rojo, Media en terciario, Baja neutra.
- **Nuevo: fecha límite por tarea** (solo día, sin hora). Atajos Hoy/Mañana + selector de día de Material3. En la fila se muestra como pill con calendario ("Hoy", "Mañana", "Ayer", "3 oct"); en rojo solo si ya venció.
- **Orden sensato en "Todas" y en la búsqueda**: pendientes primero, luego mayor prioridad, luego fecha más cercana (sin fecha al final), desempate por creación reciente. La regla vive en SQL (`TaskDao`) y se replica en Kotlin puro (`TaskOrdering.kt`) para poder probarla en JVM.
- **Migración Room 1→2** (`MIGRATION_1_2`, `ALTER TABLE` con valores por defecto): las tareas existentes quedan con prioridad "Sin prioridad" y sin fecha; ningún dato previo cambia de significado. Schema v2 exportado en `app/schemas/`.
- **Respaldo JSON** incluye prioridad y fecha; los respaldos viejos (1.8.0) se importan sin errores con valores por defecto (campos opcionales, sin bump de versión del formato).
- 25 pruebas nuevas (97 JVM en total, todas verdes): enum de prioridad (4), orden canónico (5), etiquetas de fecha límite y vencimiento con zona fija America/Havana (10), repositorio add/updateDetails (5 netas) y respaldo con campos nuevos + compatibilidad hacia atrás (2).
- Prueba de migración 1→2 añadida a `MigrationTest` (instrumentada: compila OK, se ejecuta en dispositivo/emulador; aquí no hay).
- Sin dependencias nuevas, todo offline. APK apenas crece (solo código propio).
- **Qué NO se hizo**: notificaciones al vencer (la fecha límite no programa alarma; solo es visual), filtros por prioridad/fecha en "Todas", insignias en el icono, ni recurrencia (eso es ETAPA 11). El recordatorio con hora sigue siendo la vía para que algo "suene".
- **Próxima etapa pendiente**: ETAPA 11 — Recurrencia (tareas que se repiten: diario/semanal; la entidad ya reserva el campo `recurrence`).
- Nota: el worker de tests de Gradle (`testDebugUnitTest`) sigue roto en este entorno (NPE en `SuiteTestClassProcessor`); los tests JVM se ejecutaron vía JUnitCore directo sobre las clases compiladas (classpath = `debugUnitTestRuntimeClasspath` con los `classes.jar` extraídos de los AAR; el `android.jar` va AL FINAL para no opacar el `org.json:json` real con sus stubs).

### 1.8.0 (2026-09-29) — búsqueda de tareas (ETAPA 9)
- **Nuevo: buscar en "Todas".** Campo de búsqueda sobre la lista: escribe y filtra por título al instante (insensible a mayúsculas). Muestra el conteo ("3 resultados") y un estado elegante de "Sin resultados" cuando nada coincide; la X limpia la búsqueda.
- **Rendimiento**: la consulta es un `LIKE` en Room (corre en el hilo de la BD, no en la UI); el texto se consulta con debounce de 300 ms y `flatMapLatest` cancela la búsqueda anterior si sigues escribiendo. Sin FTS ni columnas nuevas: sin migración de base de datos, sin peso extra en el APK (mismo tamaño que 1.7.0).
- Los caracteres `%`, `_` y `\` se buscan como texto literal (escapados con `ESCAPE '\'`): buscar "100%" ya no devuelve "1000 correos" por accidente.
- Sin texto, la pantalla se comporta exactamente igual que antes (un único flujo sirve ambos estados).
- 10 pruebas nuevas (72 JVM en total, todas verdes): construcción del patrón LIKE (6) y búsqueda en el repositorio con DAO falso (4: vacía, mayúsculas, comodines literales, orden pendientes-primero). Pruebas instrumentadas del `search` real añadidas a `TaskDaoTest` (se ejecutan en dispositivo; compilan OK).
- Nota: las tildes no se normalizan ("cafe" no encuentra "café"); documentado como limitación conocida, no como bug.

### 1.7.0 (2026-09-29) — rediseño visual minimalista (ETAPA 8)
- Nuevo sistema de espaciado (`ui/theme/Spacing`): toda la UI usa una escala única (2/4/8/12/16/20/24/32dp); se eliminaron los paddings arbitrarios. Test de invariantes `SpacingTest`.
- Jerarquía tipográfica con roles fijos: marca "AHORA", títulos de pantalla, etiquetas de sección en mayúsculas ("HOY", "APARIENCIA"...), título de tarea (17sp, protagonista), texto secundario y acciones.
- Paleta reducida: fondo, superficies, texto principal/secundario, acento azul (reservado para acciones y estados) y divisores sutiles (`outlineVariant`) en claro y oscuro.
- Las tareas ya no van en tarjetas: filas limpias sobre el fondo, separadas por divisores finos. Sin sombras, sin neumorfismo, sin gradientes.
- Pill de recordatorio neutra (antes teñida de azul en cada fila).
- Barra de captura unificada en Hoy: escribir, dictar (micrófono con pulso mientras escucha) y enviar (botón circular de acento) en un solo campo sin bordes.
- Transiciones entre pantallas centralizadas en `Motion` (antes valores sueltos en el navegador).
- Ajustes reorganizados con encabezados de sección uniformes y divisores; filas táctiles más cómodas (48dp+).
- Sin cambios de funcionalidad: todo lo que funcionaba sigue funcionando igual.
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
