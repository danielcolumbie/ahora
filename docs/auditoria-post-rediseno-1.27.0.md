# Auditoría post-rediseño — Ahora 1.27.0

**Fecha:** 2026-09-30 · **Versión auditada:** 1.27.0 (código 29), tag `v1.27.0`
**Alcance:** solo lectura, sin modificar código. Cubre los dos cambios del rediseño
reciente más el estado general de la app.

**Cambios auditados:**
1. **Sección «Completadas» colapsable** en Hoy (commit `547a6b5`): fila táctil de
   ancho completo con chevron, texto en semibold gris y pill neutra con contador;
   colapsada por defecto; animación 150 ms; estado tras rotación; TalkBack.
2. **Marca AHORA con tamaño visual fijo** (commits `1b241ee`, `74fb928`): con
   encabezado compacto (landscape) y escala de fuente > 1.3, la marca conserva
   sus 40dp visuales (`brandTextStyle()` en `ui/theme/Type.kt`); el contenido sí escala.

## Resumen ejecutivo

No se encontró ningún hallazgo **crítico** ni **alto**. La app está en buen estado:
el design system es coherente y disciplinado (tokens, sin dp/sp sueltos), los dos
cambios del rediseño implementan fielmente los mockups aprobados por Daniel, el
lint está limpio (0 errores) y el APK firmado 1.27.0 es instalable sobre la 1.26.0
(misma firma, `8fcc9103…015ae5a`… ver §3).

**Hallazgos:** 0 críticos · 0 altos · 1 medio (infraestructura de tests del
sandbox, no del código) · 4 bajos (detalles de UX/visual) · ver §7.

**Lo más importante:** el worker de tests JVM de Gradle no corre en este sandbox
ahora mismo (falla de infraestructura, §3.1). El código bajo prueba es idéntico
al del último run verde (256/256) salvo el bump de versión, y se verificó por vía
manual: 251/256 tests pasan con **0 fallos de aserción** (los 5 restantes son
limitaciones del classpath manual, no del código). Los 14 tests que cubren los
dos cambios del rediseño pasan al 100%.

---

## 1. Visual

### 1.1 Coherencia del design system
- **Colores:** paleta cerrada y documentada (`Color.kt`). El acento único
  (azul eléctrico) se reserva para acciones/estados; la sección nueva usa
  `surfaceVariant`/`onSurfaceVariant` igual que las pills de metadatos de las
  filas — coherente. Los contrastes están verificados contra AA en el propio
  código (p. ej. `#2F63E4` 4.74:1, pill clara 5.30:1).
- **Tipografía:** `AhoraTypography` con roles documentados; sin fuentes externas.
  La sección usa `bodyLarge` semibold + `labelSmall` en la pill, igual que el
  resto de la lista.
- **Espaciados:** escala `Spacing` como única fuente de verdad; `Sizes` para
  medidas de componente (`minTouchRow` 48dp, `sectionChevron` 24dp). Grep
  confirma **cero `dp`/`sp` sueltos** fuera de `theme/`.
- **Divisores:** `outlineVariant` del tema mapea a los colores de divisor
  diseñados (`LightDivider`/`DarkDivider`, tinta 8% / papel 10%). La fila de
  «Completadas» y su divisor usan el mismo lenguaje que el resto de la lista.

### 1.2 Cambio 1 — «Completadas» colapsable
Comparado contra el mockup aprobado (`docs/mockups/completadas-colapsadas.png`):
chevron + «Completadas» + pill con contador, colapsada por defecto. **Fiel al
mockup.** Diferencia menor: el mockup dibuja un chevron de trazo fino y la
implementación usa `Icons.Filled.KeyboardArrowDown` (ver hallazgo B-3).

### 1.3 Cambio 2 — marca con tamaño fijo
Comparado contra `docs/mockups/ahora-landscape-arreglado.png`: con encabezado
compacto + fuente 200%, la marca se mantiene en 40dp visuales y el contenido
escala. **Fiel al mockup.** La matemática es correcta: a escala 2.0,
`40.sp / 2.0 = 20.sp` → renderiza a 40dp. El `letterSpacing` se compensa igual
para no deformar la marca.

---

## 2. UX

### 2.1 Flujos principales
- **Hoy:** activas siempre visibles → fila «Completadas» (colapsada por defecto)
  → completadas ocultas hasta expandir. Al completar una tarea, la fila sale con
  `softExit()` (200 ms) y cae en la sección colapsada; el contador del encabezado
  se actualiza. No hay confirmación explícita del destino más allá del contador
  (por diseño del mockup; ver observación B-4).
- **Todas:** lista plana, sin cambios (la bandera `collapsibleCompleted` solo la
  activa Hoy). Verificado en código.
- **Navegación:** `NavigationBar` M3 con 3 destinos; sin cambios en esta versión.
- **Diálogos de edición/recordatorio** dentro de `TasksColumn` intactos tras la
  extracción de `TaskListItem` (la refactorización conserva `LaunchedEffect`,
  `AnimatedVisibility` y `animateItemPlacement` por ítem).

### 2.2 Comportamiento de la sección colapsable (revisión de código)
- Partición `splitActiveCompleted`: pura, conserva el orden de cada grupo. ✓
- Claves estables por `task.id` en ambos grupos; al mover una tarea de grupo se
  reproduce salida + entrada, sin re-animación espuria (el `shownIds` compartido
  lo evita). ✓
- El estado expandido/colapsado sobrevive a la rotación (`rememberSaveable`). ✓
- **Hallazgo B-1:** si la sección se vacía (se desmarcan/eliminan todas las
  completadas) el flag `completedExpanded` no se resetea; al reaparecer la
  sección lo hace expandida, no colapsada por defecto.
- El divisor separa la última activa del encabezado, y no hay divisor colgando
  tras la última completada. ✓
- **Hallazgo B-2:** el toggle del encabezado no emite tick háptico, a diferencia
  del checkbox de completar tarea (`Haptics.tick`). Inconsistencia menor.

---

## 3. Técnica

### 3.1 Tests JVM — ⚠️ hallazgo M-1 (infraestructura, no código)
El task `testDebugUnitTest` de Gradle **no corre en este sandbox ahora mismo**:
el worker de tests muere al arrancar con
`NullPointerException` en `SuiteTestClassProcessor.stop` (`resultProcessor` nulo),
sin ejecutar ni un test. Ocurre incluso con un solo test trivial y con memoria
libre; es un fallo de la infraestructura del worker, no de los tests.

**Por qué no es un problema del código:**
- `git diff` entre el último run verde verificado (256/256 en `74fb928`) y HEAD
  muestra **un único cambio**: `versionCode` 28→29 y `versionName` 1.26.0→1.27.0.
  Ningún test referencia la versión (verificado por grep).
- Verificación manual: se corrieron los 29 clases de test directamente con
  `JUnitCore` (classpath armado del caché de Gradle + `android.jar`):
  **251/256 pasan, 0 fallos de aserción**. Los 5 restantes fallan solo por el
  classpath manual (mismatch de variante `$app_debug` vs `$app_release` en
  `ReminderSchedulerTest`, y `Main dispatcher` sin inicializar en
  `ScrollToTopTest`) — artefactos del método, no del código.
- Los **14 tests que cubren los dos cambios del rediseño**
  (`CompletedSectionTest` 6 + `BrandTextStyleTest` 5 + `SpacingTest` 3) **pasan
  al 100%**.

Acción recomendada: cuando el sandbox esté estable, correr
`./build-android.sh testDebugUnitTest` de nuevo; si el worker sigue fallando,
investigar el daemon de Gradle (había un daemon viejo colgado con 2.3 GB que se
terminó durante esta auditoría).

### 3.2 Lint
`lintRelease`: **0 errores, 20 warnings** — todos preexistentes y ninguno en
archivos tocados por el rediseño (son avisos de versión del plugin/dependencias
de Gradle y `UseKtx` en `notifications/` y `widget/`).

### 3.3 Compilación y APK
- `assembleDebug`: **BUILD SUCCESSFUL** (verificado en esta auditoría).
- `assembleRelease` + firma: APK `app-release.apk` (1.74 MB), `versionCode 29`,
  `versionName 1.27.0`, `compileSdk/targetSdk 36`.
- `apksigner verify`: firma válida; certificado SHA-256
  `8fcc9103da738f01d19c807eb2be70c1dc1fc9f1c5d1ab87973eba71f80a394d`,
  **idéntico al de la 1.26.0** → se instala encima conservando las tareas. ✓

### 3.4 Revisión de código de los cambios
- `brandTextStyle()`: función pura, bien testeada. Casos borde: `fontScale ≤ 0`
  no ocurre en la práctica (`LocalDensity` siempre > 0); `letterSpacing`
  `Unspecified` seguiría siendo `Unspecified` tras dividir (no se corrompe).
  **Hallazgo B-4:** discontinuidad en el umbral 1.3 (a 1.30x la marca se ve a
  52dp; a 1.31x salta a 40dp) — por diseño, solo visible al cambiar el ajuste
  del sistema.
- `CompletedSectionHeader`: `Role.Button`, `onClickLabel` («Expandir»/«Contraer»),
  `mergeDescendants` con `contentDescription` + `stateDescription`, chevron
  decorativo (`contentDescription = null`). Área táctil ≥ 48dp
  (`Sizes.minTouchRow`). Orden de modifiers correcto (el ripple queda acotado
  por el `clip`).
- Sin fugas de estado: `remember(tasks, collapsibleCompleted)` invalida la
  partición solo cuando cambia la lista o la bandera.

---

## 4. Rendimiento (referencia: Galaxy A14, 4 GB)

- **Tamaño:** APK release 1.74 MB — diminuto; R8 + `proguard-android-optimize`
  activos, más baseline profiles empaquetados. Instalación y arranque livianos.
- **Layouts:** `LazyColumn` sin tarjetas ni sombras (cero overdraw por
  elevación); divisores de 1px; transformaciones baratas por GPU
  (`graphicsLayer` para rotar el chevron, `animateItemPlacement`).
- **Recomposiciones:** lambdas estables por fila (bloque K); la partición
  activas/completadas se memoiza; los ítems colapsados no componen su contenido
  (`AnimatedVisibility` con `visible=false`), solo ocupan slots baratos.
- **Animaciones:** 150 ms con `FastOutSlowInEasing`, física ya usada en la app
  (sin físicas nuevas). Al expandir, solo las filas visibles en pantalla animan
  (el resto del `LazyColumn` no está compuesto).
- Sin trabajo en el hilo principal fuera de UI: Room + coroutines ya
  establecidos; los cambios no añaden I/O.

---

## 5. Accesibilidad

- **TalkBack:** el encabezado se anuncia como un solo botón:
  «Completadas, N tareas» + estado «Expandida»/«Contraída» + «doble toque para
  Expandir/Contraer». Las tareas completadas colapsadas no aparecen en el árbol
  (no se componen). El checkbox de cada fila conserva `Role.Checkbox` y etiqueta
  con el título de la tarea. ✓ (Verificación con lector real: §8.)
- **Contraste:** documentado en código y dentro de AA (píldora clara 5.30:1;
  en oscuro, `#A8A6A0` sobre `#1D1D22` ≈ 8:1).
- **Escala de fuente hasta 2.0x:** el contenido escala; la marca se fija solo en
  encabezado compacto (decisión de identidad aprobada). La fila táctil usa
  `heightIn(min=48dp)` así que crece con el texto; no hay alturas fijas que
  recorten. La lista conserva su scroll y nunca se queda sin espacio útil.
- **Tamaños táctiles:** fila «Completadas» ≥ 48dp; checkbox 48dp; `IconButton`s
  de la barra de creación 48dp por defecto de M3.

---

## 6. Adaptabilidad

- **Portrait/landscape:** `AdaptiveLayout.isCompactHeader` (< 480dp) compacta el
  encabezado en ambas pantallas (Hoy y Todas); en landscape la lista y la
  creación no pelean por el alto. Verificado en código.
- **Dos paneles** (≥ 600dp, tablets): la sección colapsable también aplica en el
  panel de lista; el detalle conserva su ancho máximo de 560dp.
- **Pantallas pequeñas:** sin scroll vertical global — la lista (`weight=1f`)
  absorbe el espacio; con fuente enorme el viewport se achica pero nada se
  recorta.
- **Rotación:** `completedExpanded` y `selectedTaskId` sobreviven
  (`rememberSaveable`); la posición de scroll no se restaura (comportamiento
  preexistente, sin regresión).

---

## 7. Hallazgos clasificados

| ID | Sev. | Área | Hallazgo |
|----|------|------|----------|
| M-1 | **Media** | Técnica/Proceso | El worker de tests JVM de Gradle no corre en este sandbox (NPE en `SuiteTestClassProcessor.stop`). No es fallo de código: verificado 251/256 manual con 0 fallos de aserción; el diff desde el último verde es solo el bump de versión. Re-correr cuando el entorno esté estable. |
| B-1 | Baja | UX | `completedExpanded` no se resetea al vaciarse la sección: al reaparecer lo hace expandida, no colapsada por defecto. Sugerencia: `LaunchedEffect(completedTasks.isEmpty()) { if (empty) expanded = false }`. |
| B-2 | Baja | UX | El encabezado «Completadas» no emite tick háptico al alternar, a diferencia del checkbox de tarea. |
| B-3 | Baja | Visual | Chevron implementado (`Filled.KeyboardArrowDown`) vs. chevron de trazo del mockup. Detalle menor; Daniel lo verá en el teléfono. |
| B-4 | Baja | Visual | Discontinuidad en el umbral 1.3 de `brandTextStyle` (52dp → 40dp al cruzar el umbral). Por diseño; solo visible al cambiar el ajuste de fuente del sistema. |

**Críticos: 0 · Altos: 0.**

---

## 8. Verificación física pendiente (solo en el Galaxy A14 real)

Sin inventar resultados — esto solo se puede comprobar en el teléfono:

1. **Fluidez de la animación** de expandir/colapsar «Completadas» (150 ms,
   fundido + despliegue) y del giro del chevron en el A14.
2. **Comportamiento real de TalkBack** al tocar el encabezado: anuncio
   «Completadas, N tareas, Contraída/Expandida» y navegación por la lista
   expandida/colapsada.
3. **Rotación** con la sección expandida: conserva el estado y no hay saltos
   visuales.
4. **Marca en landscape + fuente al máximo** (Ajustes → Accesibilidad → Tamaño
   de fuente al máximo, girar el teléfono): la marca se ve a 40dp y el
   contenido escala sin recortes.
5. **Instalación del APK 1.27.0 sobre la 1.26.0**: conserva las tareas
   (misma firma verificada, pero la prueba real es en el equipo).
6. **Rendimiento con muchas completadas**: expandir una sección con decenas de
   tareas y hacer scroll; observar jank.
7. **Modo claro y oscuro** en el dispositivo: contraste y legibilidad de la
   fila «Completadas» y la pill.
8. **Flujo completo de completar/desmarcar** una tarea en Hoy: la fila sale con
   animación, el contador se actualiza y al expandir aparece la tarea.

---

*Auditoría de solo lectura. Ningún archivo de código fue modificado.*
