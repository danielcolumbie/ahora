# Evolución visual y de UX — App «Ahora»

Fase ordenada por Daniel el 2026-09-29. Auditoría hecha sobre el **código real**
(2026-09-30, 1.26.0, rama `master`), no sobre la documentación.

## 1. Punto de partida (verificado en código)

La base del rediseño premium A–K ya existe y está disciplinada:

- Un solo acento (`#2F63E4` / `#5B93FF` en oscuro); el color comunica estado,
  no decora. Contrastes AA verificados con tests (`ColorContrastTest`).
- Escalas centrales: `Spacing` (base 4), `Sizes`, `AhoraTypography`,
  `Motion`. **Cero `dp`/`sp` sueltos** en pantallas y componentes
  (verificado con grep: todos los literales viven en `theme/` y `adaptive/`).
- Filas de tarea sin tarjeta, divisores sutiles, pills neutras, jerarquía
  tipográfica con roles fijos.
- Navegación inferior simple (Hoy / Todas / Ajustes); dos paneles desde
  600dp; encabezado compacto en alturas < 480dp; ancho máximo de lectura
  560dp en detalle y 640dp en ajustes.
- Accesibilidad trabajada: áreas táctiles ≥ 48dp, etiquetas TalkBack,
  encabezados semánticos, `FlowRow` ante fuentes grandes, el estado nunca
  se comunica solo con color.
- Feedback: háptico solo como confirmación, snackbars con Deshacer,
  reintentos ante errores, borrador que sobrevive a la muerte del proceso.
- Rendimiento: sin dependencias nuevas por estética, animaciones
  centralizadas y moderadas, callbacks estables por fila.

Conclusión: **no hay que rediseñar, hay que afinar**. Los cambios de esta
fase son pequeños, cada uno con una razón funcional.

## 2. Hallazgos de la auditoría (problemas reales)

### H1. Los títulos de los diálogos no tienen jerarquía tipográfica
`AlertDialog` no aplica estilo al slot de título por sí solo; los títulos
("Opciones de la tarea", "Editar tarea", "Aviso a la hora exacta",
"Ahora 1.26.0", "Licencias") usan `Text()` plano = `bodyLarge` 17sp Regular.
Parecen cuerpo de texto, no títulos. El sistema de diseño ya define el rol
"Título de pantalla" (`titleLarge` 22sp SemiBold) que usan "Todas" y
"Ajustes": los diálogos deben usar el mismo rol.

### H2. La sección "Recordatorio" del diálogo de opciones avanzadas rompe el patrón
En `AdvancedCreationDialog`, "Recordatorio" usa `labelSmall` 12sp sin
mayúsculas, mientras todas las demás secciones (ahí y en el panel de
detalle) usan `FormSection` (`titleMedium` 13sp, mayúsculas, tracking 1.5).
El panel de detalle sí envuelve su recordatorio en `FormSection`: el
diálogo debe igualarlo.

### H3. En dos paneles no se indica qué tarea está seleccionada
En pantallas anchas, tocar una fila la lleva al panel de detalle, pero
`TaskRow` no tiene estado de selección: el usuario no sabe qué fila
corresponde al detalle. Es el patrón maestro–detalle estándar y hoy falta
la mitad visual.

### H4. Hoy duplica la recolección de eventos de UI
`HomeScreen` tiene su propio `LaunchedEffect` recolectando
`viewModel.events` con lógica idéntica a `CollectUiEvents` (que usa
Todas). Dos implementaciones del mismo comportamiento = riesgo de
divergencia.

### H5 (menor). Títulos de diálogo sin semántica de encabezado
Para TalkBack, los títulos de diálogo deberían marcarse como encabezados.

## 3. Decisiones conscientes de NO cambiar (con razón)

- **Marca "AHORA" 40sp + lema**: es la identidad. Ocupa ~150dp pero es el
  corazón de "Sácalo de tu cabeza"; el modo compacto ya la reduce en
  landscape.
- **Barra de captura sin borde**: el diseño evita bordes a propósito ("la
  superficie la distingue del fondo con calma"). No se añade borde de foco.
- **Estado vacío solo con texto**: el encargo prohíbe imágenes decorativas
  sin función. El vacío tipográfico actual es minimalismo intencional.
- **Edición en vivo en el panel ancho vs. Guardar/Cancelar en el diálogo**:
  inconsistencia aparente pero justificada por el contexto (el diálogo
  necesita confirmación explícita; el panel es edición directa).
- **Colapsar tareas completadas**: cambio de comportamiento no pedido;
  queda como propuesta futura (ver §6).
- **Skeletons de carga**: Room es local y rápido; no hay estado de carga
  real que representar. Añadirlo sería decoración.
- **Modo oscuro por defecto**: decisión de producto de Daniel, no de esta fase.

## 4. Dirección visual (exploración en Canva)

Carpeta: "Ahora — dirección visual" (`FAHWoPbYceQ`). Tres exploraciones:

1. `01-pantalla-hoy` — pantalla Hoy en teléfono: valida paleta cálida,
   marca, barra de captura, filas sin tarjeta, pills.
2. `02-componentes` — hoja de componentes: pills, chips, botones,
   checkbox, estado vacío, diálogo con título jerárquico.
3. `03-dos-paneles` — tablet horizontal: valida el tratamiento de la
   fila seleccionada (fondo `surfaceVariant`, radio 12dp).

Decisiones tomadas con ayuda de Canva (mockups revisados el 2026-09-30):

- **Selección en dos paneles (mockup 03)**: la fila seleccionada lleva un
  fondo `surfaceVariant` con esquinas redondeadas de 12dp (`Spacing.m`) e
  inset horizontal respecto a la lista. Implementado en `TaskRow`.
- **Títulos de diálogo con jerarquía (mockup 02)**: el diálogo muestra su
  título en estilo grande semibold, no como cuerpo de texto. Implementado
  con el componente `DialogTitle` (rol `titleLarge` + semántica de
  encabezado).
- **Dirección general (mockup 01)**: la pantalla Hoy del mockup replica la
  dirección actual (fondo crema, marca grande, barra blanca, filas sin
  tarjeta, pills neutras + acento). **No se cambia nada estructural**: la
  exploración confirma que la base es la correcta.
- **Contador de sección (mockup 01)**: el mockup muestra "2/6"
  (hechas/total) junto a "HOY" en vez de solo el total. Se adopta: comunica
  progreso de un vistazo, con razón funcional.

Los mockups son exploraciones generadas por IA: se toma la dirección, no
los píxeles literales.

## 5. Plan de implementación (incremental)

1. **Títulos de diálogo**: aplicar `titleLarge` a los títulos de
   `AlertDialog` (avanzadas, edición, aviso de alarmas, acerca de,
   licencias) + semántica de encabezado. (H1, H5)
2. **Sección Recordatorio** en `AdvancedCreationDialog`: envolver en
   `FormSection`. (H2)
3. **Selección en dos paneles**: `TaskRow(selected: Boolean)` con fondo
   `surfaceVariant` redondeado (radio 12dp = `Spacing.m`); `TasksColumn`
   propaga `selectedTaskId`; `AdaptiveListDetail`/`HomeScreen` pasan el id
   seleccionado. Solo visible en modo dos paneles. (H3)
4. **Unificar eventos**: `HomeScreen` usa `CollectUiEvents`. (H4)
5. Tests: invariantes nuevos en `DesignSystemTest`/`AccessibilityTokensTest`
   donde aplique (p. ej. que el título de diálogo use el rol correcto es
   difícil de testear en unit; se cubre con revisión de código + build).
6. Verificación: suite completa + lint + `assembleDebug/Release` +
   `bundleRelease` + `aapt`. Sin cambio de versión (sigue 1.26.0/28).

## 6. Pendiente / futuro (no entra en esta fase)

- Colapsar "Completadas" en la lista de Hoy (cambio de comportamiento;
  proponer a Daniel con mockup).
- Revisar el costo real de las animaciones escalonadas en un gama baja
  (medición en dispositivo físico, no teoría).
- Revisar navegación inferior con escalas de fuente muy grandes en
  landscape (probar en dispositivo).
- Estado de carga explícito si algún día la fuente deja de ser Room local.

## 7. Restricciones que se respetan

- Sin cambios de `versionName`/`versionCode` (1.26.0 / 28).
- Sin dependencias nuevas. Sin gradientes, blur, sombras ni imágenes
  decorativas. Sin copiar a Apple.
- `minSdk 26`: no usar `MutableList.removeLast()` (ya verificado: no hay
  usos en `main`).
- Cada decisión visual con razón funcional (documentada arriba).

## 8. Resultado de la implementación (2026-09-30)

## 9. Ronda 2 (2026-09-30, orden "avanza" de Daniel)

### 9.1. Mockup: colapsar "Completadas" en Hoy (PROPUESTA, no implementado)

Cambio de comportamiento → primero mockup para aprobación de Daniel.

- Diseño Canva: `04-completadas-colapsadas` (ID `DAHWoR2o5iE`), en la
  carpeta "Ahora — dirección visual" (`FAHWoPbYceQ`).
- Enlace editable: https://canva.link/nuuebpalgg8seqg
- Exportación: `docs/mockups/completadas-colapsadas.png`
- Lo que propone: bajo las tareas activas, una fila táctil de ancho
  completo con chevron hacia abajo (affordance clara de expandir),
  texto "Completadas" en semibold gris oscuro y pill neutra con el
  contador (3). Las completadas quedan ocultas tras el colapso.
  Mantiene la identidad: fondo crema, sin tarjetas, sin sombras,
  divisores finos, pills neutras.
- Decisión explícita: NO implementado hasta que Daniel apruebe el
  mockup. Al aprobar, la implementación sería una sección colapsable
  en `TasksColumn` (o en `HomeScreen`), con el estado de
  colapso/expandido recordado y TalkBack anunciando el estado.

### 9.2. Auditoría: navegación y layouts con fuentes muy grandes en landscape

Revisión sobre el código real (sin dispositivo). Conclusión: **no hay
nada que corregir**; la app degrada con gracia por diseño.

Verificado:

- **Barra inferior** (`NavigationBar` M3, 3 destinos): etiquetas cortas
  ("Hoy", "Todas", "Ajustes"); M3 elide con puntos suspensivos si
  hiciera falta. En landscape hay ancho de sobra. Áreas táctiles
  ≥48dp garantizadas por el componente. Sin problema real.
- **Encabezado compacto** (`AdaptiveLayout.isCompactHeader`, <480dp):
  en landscape se oculta el lema y se reducen los espaciados, en Hoy
  y en Todas (verificado en código).
- **La lista siempre conserva su espacio**: la columna exterior no
  hace scroll, pero `TasksColumn` (LazyColumn) lleva `weight(1f)`:
  el encabezado queda fijo y la lista ocupa lo que quede. Nada se
  solapa ni se recorta; en el peor caso (fuente 2x en landscape) la
  lista queda baja pero funcional y desplazable.
- **Cero alturas fijas en filas**: grep confirma que no hay
  `.height(Ndp)` literales fuera de tests; todo es `heightIn(min=…)`.
- **El texto se envuelve, no se recorta**: `TaskRow` y `SettingRow`
  usan `weight(1f)` en la columna de texto; las pills van en
  `FlowRow` (se envuelven ante fuentes grandes).
- **Dos paneles en landscape de teléfono** (≥600dp): 50/50, ~370dp
  por panel; las filas se adaptan sin cambios.
- **Diálogos**: M3 hace scroll interno del contenido.
- **Campos de texto de una línea**: con fuente al 200%, el texto
  (17sp→34sp) sigue cabiendo en la altura por defecto del `TextField`
  M3. Caso límite aceptable.

Decisiones de diseño que quedan para Daniel (no se tocan sin su
aprobación):

1. En `compactHeader` + fuente máxima, la marca "AHORA"
   (`displayLarge` 40sp → ~80sp) domina la poca altura del landscape.
   ¿Reducirla u ocultarla en esa combinación extrema? Toca la
   identidad de la app: lo decide él.
2. Colapsar "Completadas": ver mockup §9.1.

### 9.3. Pendiente (sin cambios)

- Medir el costo real de las animaciones escalonadas en un gama baja:
  lo hace Daniel en su Galaxy A14, no teoría desde aquí.
- Sin cambios de versión (sigue 1.26.0/28), sin tags, sin releases.

### 9.4. Verificación de esta ronda

- No se tocó código de la app (solo `docs/` + mockup PNG): no se
  requieren tests ni builds nuevos. La base verificada sigue siendo
  la del commit `a9b2858` (245 tests: 244 OK + 1 fallo preexistente;
  lint 0 errores; builds OK).

Plan §5 ejecutado completo:

1. `DialogTitle` nuevo (`ui/components/DialogTitle.kt`): `titleLarge` +
   semántica `heading()`. Aplicado a los 5 diálogos: opciones de la tarea,
   editar tarea, aviso de alarmas exactas, acerca de, licencias.
2. Recordatorio en `AdvancedCreationDialog` envuelto en
   `FormSection("Recordatorio")` con estado vacío "Sin recordatorio".
3. `TaskRow(selected: Boolean)`: banda `surfaceVariant` dibujada con
   `drawBehind` (radio `Spacing.m`, inset `Spacing.s`), fundido de
   150 ms (`Motion.selectionFade()`), TalkBack anuncia "Seleccionada".
   `TasksColumn` y `HomeScreen` propagan `selectedTaskId`.
4. `HomeScreen` usa `CollectUiEvents` (se eliminó la recolección
   duplicada). Contador HOY: `hechas/total`.
5. Test nuevo en `MotionTest`: el fundido de selección existe y dura
   150 ms.

Verificación real:

- Tests JVM (`./run-jvm-tests.sh`; el worker de Gradle
  `testDebugUnitTest` está roto en este entorno — NPE en
  `SuiteTestClassProcessor`, preexistente y documentado): **245 tests,
  244 pasan, 1 falla**. El fallo es
  `SettingsRepositoryTest.descartar el aviso de alarmas exactas persiste`
  (dispatcher de corrutinas + stub de Android), **preexistente**:
  falla igual con el árbol limpio (244 tests, 1 fallo). El test nuevo
  de `MotionTest` pasa.
- `lintRelease`: **0 errores, 20 warnings** (igual que la base 1.26.0;
  ningún warning en los archivos tocados).
- `assembleDebug` + `assembleRelease` + `bundleRelease`:
  **BUILD SUCCESSFUL**.
- `aapt` sobre el APK release: `versionCode='28'`
  `versionName='1.26.0'`, `sdkVersion:'26'`, `targetSdkVersion:'36'`
  — versión intacta.
