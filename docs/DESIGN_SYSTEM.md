# DESIGN SYSTEM — App «Ahora»

Sistema de diseño propio, implementado en `app/src/main/java/com/ahora/app/ui/theme/`.
Filosofía: **«Sácalo de tu cabeza.»** La interfaz desaparece; solo queda lo que
tienes que hacer. Android nativo, Material 3 como base, sin librerías visuales
pesadas, fluido en un Galaxy A14 (4 GB).

## 1. Color

Un solo acento. El color comunica estado, no decora.

| Token (M3) | Claro | Oscuro | Rol |
|---|---|---|---|
| `primary` | `#2E6BE6` azul eléctrico | `#7DA6FF` | Acciones, checkbox marcado, marca |
| `onPrimary` | `#FFFFFF` | `#0B1526` | Sobre el acento |
| `primaryContainer` / `onPrimaryContainer` | `#DCE6FF` / `#0B2A6B` | `#2A4A8F` / `#DCE6FF` | Chips seleccionados |
| `background` / `onBackground` | `#F7F7F5` / `#171714` | `#121210` / `#F3EBDD` | Fondo y texto protagonista |
| `surface` / `onSurface` | `#FFFFFF` / `#171714` | `#1C1C19` / `#F3EBDD` | Superficies (diálogos, barra) |
| `surfaceVariant` / `onSurfaceVariant` | `#E8E6E1` / `#55534E` | `#2A2925` / `#A8A49B` | Pills neutras, metadatos |
| `outline` | `#C9C6BF` | `#3A3934` | Bordes |
| `outlineVariant` | `#E3E1DB` | `#26251F` | Divisores sutiles entre filas |
| `error` / `onError` | `#C62828` / `#FFFFFF` | `#FF8A80` / `#3D0A0A` | Solo: prioridad alta y fecha vencida |

Reglas:
- Las pills de metadatos son **neutras** (`surfaceVariant` + `onSurfaceVariant`).
  Solo la prioridad ALTA y la fecha VENCIDA usan `error`.
- La prioridad MEDIA no lleva color propio: es neutra como las demás.
- No usar `tertiary` (no está definido en el tema; caía al default de M3).
- Dark mode es una paleta propia, no una inversión.

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

## 5. Componentes

- **Fila de tarea:** sin tarjeta; checkbox circular + título (`bodyLarge`) +
  pills de metadatos + divisor `outlineVariant`. Al completar: tachado +
  opacidad 0.55 animada. Acciones (recordatorio, eliminar) como iconos
  secundarios; el diseño futuro debe reducir su densidad.
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
  (`titleMedium`, `onSurfaceVariant` al 65%): informa la cantidad sin
  añadir ruido. Se usa en la sección «HOY» de la pantalla principal.
- **Diálogos:** `TaskFormFields` compartido (crear = editar).
- **Ajustes:** secciones con `titleMedium` en mayúsculas; filas reutilizables
  (objetivo del rediseño: componente `SettingRow`).

## 6. Movimiento (`Motion`)

Duraciones: entrada de fila 280ms, entrada de pantalla 280ms, salida 240ms,
stagger 45ms por ítem (tope 360ms), pulso de micrófono 1600ms.

Reglas:
- Easing `FastOutSlowIn` para transiciones; springs solo donde el gesto lo
  pide (checkbox).
- **Prohibidos los rebotes exagerados.** El círculo de completado usa un
  spring suave (`dampingRatio = 0.8`), sin overshoot visible.
- Listas: `key(id)` estable, `animateItemPlacement`, entrada escalonada solo
  la primera vez.

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

- Área táctil mínima 48dp en filas y controles.
- `contentDescription` en todo icono con acción; `Role.Checkbox` en el
  círculo personalizado.
- Contraste AA en ambas paletas (auditar `onSurfaceVariant` y divisores en
  dark en un dispositivo real).
- La UI no depende solo del color: la prioridad alta lleva icono + texto,
  la fecha vencida lleva texto («Ayer», «hace 2 días»).

## 10. Adaptabilidad

- Una sola columna hasta 600dp; a partir de ahí, dos columnas (lista +
  detalle) en tablets.
- Texto escalable: sin `sp` fijos fuera del sistema; probar a 130% de escala
  de fuente del sistema.
- `enableEdgeToEdge`: respetar insets (status bar, navegación, teclado) en
  cada pantalla.

## 11. Rendimiento (referencia: Galaxy A14, 4 GB)

- Callbacks estables por fila (sin lambdas recreadas por ítem).
- `collectAsStateWithLifecycle` + `WhileSubscribed(5_000)`.
- Sin polling: widget y UI reaccionan a cambios de la BD.
- APK ~15 MB; sin dependencias visuales pesadas.
