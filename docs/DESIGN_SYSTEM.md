# DESIGN SYSTEM — App «Ahora»

Sistema de diseño propio, implementado en `app/src/main/java/com/ahora/app/ui/theme/`.
Filosofía: **«Sácalo de tu cabeza.»** La interfaz desaparece; solo queda lo que
tienes que hacer. Android nativo, Material 3 como base, sin librerías visuales
pesadas, fluido en un Galaxy A14 (4 GB).

## 1. Color

Un solo acento. El color comunica estado, no decora.

| Token (M3) | Claro | Oscuro | Rol |
|---|---|---|---|
| `primary` | `#2F63E4` azul eléctrico | `#5B93FF` | Acciones, checkbox marcado, marca |
| `onPrimary` | `#F6F4EE` | `#F5F3EE` | Sobre el acento |
| `primaryContainer` / `onPrimaryContainer` | `#DCE6FF` / `#0B2A6B` | `#2A4A8F` / `#DCE6FF` | Superficie de «seleccionado» (implementado en 1.18.0; primer uso: indicador de navegación) |
| `background` / `onBackground` | `#F6F4EE` / `#171714` | `#0B0B0D` / `#F5F3EE` | Fondo y texto protagonista |
| `surface` / `onSurface` | `#FFFFFF` / `#171714` | `#141417` / `#F5F3EE` | Superficies (diálogos, barra) |
| `surfaceVariant` / `onSurfaceVariant` | `#EDEAE2` / `#625F58` | `#1D1D22` / `#A8A6A0` | Pills neutras, metadatos |
| `outlineVariant` | tinta al 8% (`#14171714`) | papel al 10% (`#1AF5F3EE`) | Divisores sutiles entre filas |
| `error` / `onError` | `#B3261E` | `#F2B8B5` | Solo: prioridad alta y fecha vencida |

Nota: los tokens no listados usan el default de Material 3 (p. ej. `outline`,
`onError`, `secondaryContainer`).

Reglas:
- Las pills de metadatos son **neutras** (`surfaceVariant` + `onSurfaceVariant`).
  Solo la prioridad ALTA y la fecha VENCIDA usan `error`.
- La prioridad MEDIA no lleva color propio: es neutra como las demás.
- No usar `tertiary` (no está definido en el tema; caía al default de M3).
- Dark mode es una paleta propia, no una inversión.
- El widget (`res/values/colors.xml` + `values-night/colors.xml`) usa los
  mismos hex de la paleta de forma explícita — RemoteViews no lee el tema
  Compose —: superficie, texto, texto secundario, acento, peligro y
  divisor, en claro y en oscuro. Si la paleta cambia, hay que actualizarlo
  a mano (verificado en el bloque G, 1.20.0).

## 2. Tipografía (`AhoraTypography`)

Sans del sistema, sin fuentes externas. Pocos tamaños, roles fijos.

| Rol | Estilo | Uso |
|---|---|---|
| Marca | `displayLarge` 40sp Bold, tracking −0.5 | «AHORA» en la pantalla principal |
| Título de pantalla | `titleLarge` 22sp SemiBold, tracking −0.25 | «Todas», «Ajustes», vacío |
| Etiqueta de sección | `titleMedium` 13sp SemiBold, tracking 1.5, **siempre en mayúsculas** | «HOY», «APARIENCIA», «RESPALDO» |
| Cuerpo protagonista | `bodyLarge` 17sp Regular | Título de la tarea |
| Secundario | `bodyMedium` 15sp Regular | Subtítulos, descripciones |
| Acción | `labelLarge` 14sp Medium | Botones |
| Metadato | `labelSmall` 12sp Medium | Texto dentro de pills |

Regla: ningún `sp` suelto en componentes; todo sale de estos roles.

## 3. Espaciado (`Spacing`)

Escala base 4: `xxs=2, xs=4, s=8, m=12, l=16, xl=20, xxl=24, xxxl=32` (dp).
`screenHorizontal = 20dp`. Nada en la UI usa valores `dp` sueltos: el test
`SpacingTest` fija los invariantes.

## 4. Dimensiones de componentes (`Sizes`)

| Token | Valor | Uso |
|---|---|---|
| `checkCircle` | 24dp | Círculo de completado sin marcar |
| `checkCircleChecked` | 27dp | Círculo de completado marcado |
| `checkStroke` | 2dp | Borde del círculo |
| `checkIcon` | 15dp | Check dentro del círculo |
| `minTouchRow` | 48dp | Área táctil mínima de la fila |
| `pillIcon` | 12dp (`Spacing.m`) | Icono dentro de pills |
| `settingIconBox` | 36dp | Caja del icono en las filas de ajustes |
| `settingIcon` | 20dp | Icono dentro de la caja de ajustes |

## 5. Componentes

- **Fila de tarea:** sin tarjeta; checkbox circular + título (`bodyLarge`) +
  pills de metadatos + divisor `outlineVariant`. Altura mínima 48dp
  garantizada (`heightIn`, no depende del contenido). Al completar: tachado
  + opacidad 0.55 animada + `Haptics.tick()` (el sistema decide si vibra).
  Acciones (recordatorio, eliminar) en un solo menú de opciones (⋮) por
  fila: menos densidad, misma funcionalidad (Deshacer y diálogo de
  recordatorio intactos).
- **Pill (`MetaPill`):** forma totalmente redondeada, `surfaceVariant`,
  icono 12dp + `labelSmall`. Neutra salvo alta/vencida.
- **Barra de creación rápida:** voz + campo + enviar en una línea, sin bordes
  (la superficie la distingue del fondo). Mientras se escribe aparece el
  icono de ajustes («Más opciones») junto al de enviar. Capturar en dos
  toques.
- **Creación rápida vs. configuración avanzada:** la barra nunca es un
  formulario. Debajo, los **chips de feedback del lenguaje natural**
  (`AssistChip` neutros) muestran lo entendido —«Se pondrá para Hoy · 15:00»,
  «Vence: Mañana», «Alta», «Todos los días»— y abren el diálogo avanzado al
  tocarlos. Las opciones (prioridad, fecha, recordatorio, recurrencia) viven
  en el diálogo **«Opciones de la tarea»**, que reutiliza el componente
  compartido `TaskFormFields`; «Cancelar» restaura el borrador como estaba.
  El estado del borrador es un solo holder (`CreationDraftState`) con un
  único `rememberSaveable` propio: sobrevive a rotación y a muerte del
  proceso. Lo manual siempre gana sobre lo detectado.
- **Estado vacío:** título `titleLarge` + subtítulo `bodyMedium` +
  botón primario. Entra con fundido suave.
- **Etiqueta de sección con contador:** la etiqueta (`titleMedium` en
  mayúsculas) puede llevar un contador discreto a su derecha
  (`titleMedium`, `onSurfaceVariant` al 90%): informa la cantidad sin
  añadir ruido. Se usa en la sección «HOY» de la pantalla principal.
  (Bloque G: antes al 65%, que en oscuro daba 3.97:1 — bajo AA. Al 90%:
  6.65:1 en oscuro, 4.62:1 en claro.)
- **Diálogos:** `TaskFormFields` compartido (crear = editar); secciones del
  sistema con `Spacing.l` entre ellas («Prioridad», «Fecha límite»,
  «Recurrencia»). El diálogo de recordatorio es una sola pantalla con el
  mismo patrón: sección «Fecha» (atajos Hoy / Mañana / En 1 hora +
  calendario) y sección «Hora» (entrada de tiempo en 24h).
- **Sección de formulario (`FormSection`):** etiqueta `titleMedium` en
  mayúsculas (`onSurfaceVariant`) + contenido, separados por `Spacing.s`.
  La comparten el diálogo de edición, `TaskFormFields`, el diálogo de
  recordatorio y la pantalla de ajustes: una sola jerarquía en diálogos y
  ajustes.
- **Fila de ajustes (`SettingRow`, `ui/components/SettingRow.kt`):** icono
  en caja neutra de 36dp (`surfaceVariant`, radio `Spacing.s`, icono 20dp en
  `onSurfaceVariant`) + título (`bodyLarge`) + subtítulo opcional
  (`bodyMedium`, `onSurfaceVariant`) + control a la derecha. Altura mínima
  48dp (`Spacing.minTouchRow`); sin `dp`/`sp` sueltos. Variantes:
  `SettingLinkRow` (chevron, `Role.Button`) y `SettingSwitchRow` (toda la
  fila alterna el interruptor; el `Switch` es solo el indicador visual y su
  semántica se limpia para que TalkBack anuncie la fila una sola vez, con
  estado, vía `Role.Switch`). El icono es decorativo: el título ya describe
  la fila. Las filas de una sección se separan con `HorizontalDivider`
  (`outlineVariant`), como las filas de tarea.
- **Ajustes:** secciones con `FormSection` (la misma etiqueta del sistema que
  los diálogos: `titleMedium` en mayúsculas) y filas reutilizables
  `SettingRow` (ver abajo). Los resultados del respaldo salen en un
  `Snackbar` del sistema, con acción «Reintentar» cuando el fallo lo
  permite. El selector de tema usa `FilterChip` con el `primaryContainer`
  propio al seleccionar (antes, el contenedor por defecto de M3).
- **Barra de navegación (1.18.0):** 3 destinos (Hoy / Todas / Ajustes),
  `NavigationBar` de M3 sin cambiar el patrón. Selección = indicador
  `primaryContainer` + icono relleno (`onPrimaryContainer`); sin seleccionar =
  icono outlined + `onSurfaceVariant`. Etiquetas neutras (`onSurface` /
  `onSurfaceVariant`): el color comunica solo la selección. `singleTop` +
  `saveState`/`restoreState` conservan el scroll y el estado de cada pestaña;
  transiciones desde `Motion` (entrada 280ms / salida 240ms).

## 6. Movimiento (`Motion`) y hápticos (`Haptics`)

Duraciones: entrada de fila 280ms, entrada de pantalla 280ms, salida 240ms,
stagger 45ms por ítem (tope 360ms), pulso de micrófono 1600ms, entrada de
estado vacío 350ms, expansión suave 240ms, salidas cortas 200ms, entrada de
diálogo 200ms.

Reglas:
- Easing `FastOutSlowIn` para transiciones; springs solo donde el gesto lo
  pide (checkbox).
- **Prohibidos los rebotes exagerados.** El círculo de completado usa un
  spring suave (`dampingRatio = 0.8`), sin overshoot visible.
- Listas: `key(id)` estable, `animateItemPlacement`, entrada escalonada solo
  la primera vez. Al salir de la lista (eliminar, o completar en «Hoy»), la
  fila se desvanece y se colapsa en 200ms (`Motion.softExit()`): discreta,
  sin pedir atención.
- Diálogos (crear/editar/recordatorio): entran con fundido + escala sutil
  0.97→1 en 200ms (`Motion.dialogEnter()`); al cerrar desaparecen al
  instante — salir es la acción y debe sentirse inmediata.
- Los chips de feedback del lenguaje natural entran con
  `Motion.softExpand()` (240ms) y se recogen con `Motion.softExit()`
  (200ms): la barra ya no salta entre dos modos.
- Tocar la pestaña ya activa sube la lista visible al inicio con
  desplazamiento suave: la barra pide el scroll por un flujo del
  `MainViewModel` (`scrollToTopEvents`) que cada pantalla recolecta; no se
  toca el estado restaurado de las demás pestañas.
- Cero valores sueltos: toda duración/easing nuevo vive en `Motion`
  (fijado por `MotionTest`).

Hápticos (FASE 9): el objeto `Haptics` centraliza el único patrón —
`Haptics.tick()`, un tick corto y ligero. Revisión holística del bloque H:
en este BOM de Compose (2024.06) solo existen `LongPress` (largo y fuerte,
no sirve para confirmaciones rápidas) y `TextHandleMove` (el tick sutil
correcto); `ClockTick` no existe aquí. `performHapticFeedback` no vibra si
el usuario desactivó la respuesta háptica a nivel de sistema: no hace falta
comprobar nada a mano. Se usa SOLO como confirmación: marcar/desmarcar,
enviar desde la creación rápida, guardar edición y guardar recordatorio.
Deliberadamente SIN háptico: eliminar (la confirmación es el snackbar con
Deshacer), deshacer, quitar recordatorio, elegir chips de
prioridad/fecha/recurrencia, cambio de tema (el cambio visual ya confirma),
navegación entre pestañas (M3 no vibra) y pulsar el micrófono (ya tiene el
pulso animado).

## 7. Iconografía

Material Symbols/Icons outlined para acciones, filled solo donde el relleno
comunica (check dentro del círculo, banderas de prioridad). Tamaños desde el
sistema (`Sizes`, `Spacing`).

## 8. Estados

- Vacío: ilustración + mensaje cálido, nunca un placeholder genérico.
- Cargando: la lista es local y rápida; sin spinners innecesarios.
- Error: mensajes en lenguaje humano («No se pudo guardar: ...»), con acción
  cuando aplica (Deshacer al eliminar).
- Permisos: se piden en el momento de uso, una sola vez, sin insistir.

## 9. Accesibilidad

- Área táctil mínima 48dp en filas y controles (botones de icono, filas de
  tarea y de ajustes, checkbox). Fijado por `AccessibilityTokensTest`
  (`Sizes.minTouchRow` / `Spacing.minTouchRow` = 48dp). Excepción
  verificada (bloque I, 1.22.0): los chips de M3 (`AssistChip`,
  `FilterChip`) miden 32dp por estándar de Material; ampliar su área
  táctil sin cambiar su visual es imposible (cualquier mínimo de altura se
  propaga al fondo del chip), así que se conservan a 32dp: cumplen
  WCAG 2.2 AA (mínimo 24px) y no se deforman los diálogos por perseguir
  los 48dp. El botón de enviar de la creación rápida sí se corrigió de
  40dp a 48dp (1.22.0).
- `contentDescription` en todo icono con acción; `null` en los
  decorativos (icono de la lupa del buscador, iconos de navegación con
  etiqueta, iconos dentro de pills y filas). `Role.Checkbox` en el
  círculo personalizado, con el título de la tarea como etiqueta
  (1.22.0): sin ella TalkBack anunciaba el estado sin decir de qué tarea.
- Anuncio único por fila (1.22.0): `SettingLinkRow`/`SettingSwitchRow` y
  la columna del título de la tarea usan `mergeDescendants = true`, así
  TalkBack lee título + subtítulo/pills + rol + estado en un solo gesto
  (antes el nodo accionable quedaba sin texto y el título se leía
  aparte). El `Switch` de ajustes sigue siendo solo visual (semántica
  limpia). Etiquetas de sección (`FormSection`, «HOY», «Todas»,
  «Ajustes») marcadas como encabezados.
- Contraste AA en ambas paletas, verificado por cálculo sobre los hex de
  los tokens y fijado por el test `ColorContrastTest` (227 tests JVM):
  ratios clave en oscuro — `onSurfaceVariant`/fondo 8.08:1, texto de
  pill/fondo de pill 6.90:1, contador (90%) 6.65:1, error/pill 9.83:1,
  primario/fondo 6.61:1; en claro — `onSurfaceVariant`/fondo 5.79:1,
  texto de pill/fondo de pill 5.30:1, contador (90%) 4.62:1, error/pill
  5.44:1, primario/fondo 4.74:1. Los divisores (`outlineVariant`) son
  decorativos: se fija que sigan sutiles (< 2.0:1), no AA.
- La UI no depende solo del color: la prioridad alta lleva icono + texto,
  la fecha vencida lleva texto («Ayer», «hace 2 días»).

## 10. Adaptabilidad

- Una sola columna hasta 600dp; a partir de ahí, dos columnas (lista +
  detalle) en tablets. Implementado en 1.23.0 (bloque J):
  - `ui/adaptive/AdaptiveLayout.kt`: umbrales y decisiones como funciones
    puras (`isTwoPane`, `isCompactHeader`), probadas en JVM
    (`AdaptiveLayoutTest`). Sin dependencias nuevas: las pantallas usan
    `BoxWithConstraints` (mide el espacio real disponible) en vez de
    `WindowSizeClass` (exigiría añadir `material3-window-size-class`).
  - `ui/components/AdaptiveListDetail.kt`: en ≥600dp divide el espacio en
    dos paneles iguales (lista | detalle) con un divisor vertical sutil;
    por debajo, solo la lista (el detalle se abre como diálogo, como
    antes). Lo usan Hoy y Todas.
  - `ui/components/TaskDetailPanel.kt`: el panel de detalle muestra y
    edita la tarea seleccionada sin diálogos (título en vivo —nunca se
    guarda en blanco—, prioridad/fecha/recurrencia con `TaskFormFields`,
    recordatorio con el flujo completo, completar y eliminar). Edición sin
    estado pendiente: no hay nada que perder al rotar o cambiar de tarea.
    Sin selección, estado vacío elegante («Sin selección»).
  - La selección (`selectedTaskId`) vive en cada pantalla con
    `rememberSaveable`: sobrevive a la rotación; se limpia si la tarea
    sale de la lista (completada/eliminada).
  - En dos paneles, tocar una fila selecciona en vez de abrir el diálogo
    (`TasksColumn.onEditRequest`); en teléfonos el diálogo se conserva.
  - El flujo de recordatorio (diálogo + permiso de notificaciones + aviso
    de alarmas exactas) se extrajo de `TasksColumn` a
    `ui/components/ReminderFlowHost.kt` sin cambiar su conducta: lo
    comparten la lista y el panel de detalle.
  - Ajustes en tablets: contenido centrado con ancho máximo 720dp
    (`AdaptiveLayout.singleColumnMaxWidth`); el panel de detalle se topa
    en 560dp. En teléfonos no tiene efecto.
- Encabezado compacto en pantallas bajas (< 480dp de alto, landscape en
  teléfono): se oculta el lema «Sácalo de tu cabeza.» y se reducen los
  espacios; la creación y la lista no pelean por el alto.
- Texto escalable: sin `sp` fijos fuera del sistema; probar a 130% de escala
  de fuente del sistema.
- `enableEdgeToEdge`: respetar insets (status bar, navegación, teclado) en
  cada pantalla. Verificado en 1.23.0 (bloque J):
  - El `Scaffold` de `NavGraph` ya aplica los insets del sistema al
    contenido; los `Scaffold` interiores de Hoy y Todas (que solo alojan
    el snackbar) usaban el `contentWindowInsets` por defecto y los
    reaplicaban → doble padding superior e inferior. Ahora usan
    `contentWindowInsets = WindowInsets(0, 0, 0, 0)`.
  - Teclado: `android:windowSoftInputMode="adjustResize"` en el manifest;
    la barra de creación está arriba y la lista (`weight(1f)`) cede el
    espacio; los diálogos M3 gestionan el IME solos.
  - Sin dispositivo no se pudo comprobar visualmente el resultado final
    de los insets (ver «Qué NO se pudo verificar» en el changelog 1.23.0).

## 11. Rendimiento (referencia: Galaxy A14, 4 GB)

- Callbacks estables por fila (sin lambdas recreadas por ítem).
- `collectAsStateWithLifecycle` + `WhileSubscribed(5_000)`.
- Sin polling: widget y UI reaccionan a cambios de la BD.
- APK ~15 MB; sin dependencias visuales pesadas.
