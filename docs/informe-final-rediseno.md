# INFORME FINAL — Rediseño premium de la app «Ahora»

Fecha: 2026-09-29. Fases 24 (auditoría), 25 (corrección) y 26 (informe) del
prompt de Daniel (`docs/fase-post-etapa-rediseno-premium.md`). Inspección
hecha sobre el código real de la rama `master`, no sobre documentación.

Versiones del rediseño: 1.14.0 (A · design system) → 1.24.0 (K ·
rendimiento). Esta auditoría produjo la **1.25.0** (correcciones
post-auditoría). Filosofía intacta: «Sácalo de tu cabeza.»

---

## 1. Estado inicial encontrado

Al arrancar la fase (FASE 0, informe en `docs/informe-post-etapa.md`), la
app estaba en la 1.13.0: funcionalmente completa (14 etapas: prioridades,
fechas, recurrencia, widget, lenguaje natural, búsqueda, recordatorios a
prueba de balas, respaldo JSON, temas, voz), con 183 tests JVM verdes,
build debug+release OK y lint con 0 errores. Sin regresiones críticas.

La auditoría visual (FASE 3, `docs/auditoria-visual.md`) identificó qué
conservar (Spacing, Motion, roles tipográficos, paleta de un acento, filas
sin tarjeta, pills neutras, dark mode propio, callbacks estables) y qué
arreglar (`dp` sueltos en `TaskRow`, `labelSmall`/`tertiary` cayendo a
defaults de M3, spring con rebote, 12 estados de borrador sueltos, densidad
de la fila, jerarquía de creación, ritmo de Ajustes, estilo manual del
widget, contraste del divisor en dark, tamaños táctiles).

## 2. Problemas visuales encontrados

(Antes del rediseño — de la auditoría visual FASE 3:)
- `TaskRow` con `dp` sueltos (27/24/2/15dp) fuera del sistema.
- `MetaPill` usaba `labelSmall` y `tertiary` de M3 sin definir en el tema
  propio: dependía de la suerte del default.
- `EmptyState` usaba `headlineSmall` genérico de M3.
- Spring con rebote (`dampingRatio = 0.55`) en el checkbox.
- Dos botones de icono directos por fila (campana + papelera): densidad
  innecesaria.
- Barra de creación con dos modos y salto de animación visible.
- Ajustes: lista larga de 313 líneas, filas ad-hoc, mensajes de respaldo
  como texto suelto.
- Widget: colores a mano, riesgo de quedarse atrás si cambiaba la paleta.

## 3. Problemas técnicos encontrados

(Antes del rediseño — del informe post-etapa:) ninguno crítico. Deuda:
dependencias desactualizadas, worker de tests de Gradle roto en este
entorno (workaround JUnitCore documentado), `compileSdk`/`targetSdk` 34.

Durante el rediseño aparecieron y se corrigieron 2 regresiones reales
(bloque K): lambdas recreadas por fila en `TasksColumn` y lambdas por
recomposición en Hoy/Todas que invalidaban la lista al escribir.

## 4. Design system creado/modificado

Creado en el bloque A (1.14.0), vive en
`app/src/main/java/com/ahora/app/ui/theme/` y está documentado en
`docs/DESIGN_SYSTEM.md`:

- **Color:** un solo acento (azul eléctrico `#2F63E4` / `#5B93FF` en
  oscuro). Tokens propios: `primary`, `onPrimary`, `primaryContainer`,
  `background`, `surface`, `surfaceVariant`, `outlineVariant` (divisores),
  `error` (solo prioridad alta y fecha vencida). Dark mode como paleta
  propia, no inversión. En la 1.25.0 se añadieron `surfaceContainer` y
  `surfaceContainerHigh` (= `surface`) para que diálogos y menús no caigan
  al gris frío por defecto de M3.
- **Tipografía (`AhoraTypography`):** 7 roles fijos, sans del sistema, sin
  fuentes externas: marca `displayLarge` 40sp Bold; títulos `titleLarge`
  22sp; etiquetas de sección `titleMedium` 13sp en mayúsculas; cuerpo
  protagonista `bodyLarge` 17sp; secundario `bodyMedium` 15sp; acción
  `labelLarge` 14sp; metadato `labelSmall` 12sp.
- **Espaciado (`Spacing`):** escala base 4 (2–32dp), `screenHorizontal =
  20dp`. Sin `dp` sueltos en la UI (fijado por `SpacingTest`).
- **Dimensiones (`Sizes`):** círculo de completado 24/27dp, borde 2dp,
  check 15dp, área táctil mínima 48dp, icono de pill 12dp, caja/icono de
  ajustes 36/20dp; en la 1.25.0, anillo del pulso del micrófono 40dp y su
  trazo 2dp.
- **Componentes:** fila de tarea sin tarjeta + divisor sutil; `MetaPill`
  neutra; barra de creación rápida (voz + campo + enviar, sin bordes);
  chips de feedback del lenguaje natural; diálogo «Opciones de la tarea»
  con `TaskFormFields` compartido; `FormSection`; `SettingRow` (+
  variantes link/switch); `EmptyState`; `NavigationBar` con indicador
  `primaryContainer`; `ahoraSelectedChipColors()` (1.25.0) para que los
  chips seleccionados usen el acento propio y no el `secondaryContainer`
  lila por defecto de M3.
- **Movimiento (`Motion`) y hápticos (`Haptics`):** duraciones y easings
  centralizados (fijados por `MotionTest`); un solo patrón háptico
  (`Haptics.tick()`), solo como confirmación, respetando los ajustes del
  sistema.
- **Accesibilidad y adaptabilidad:** documentadas en §9 y §10 del
  `DESIGN_SYSTEM.md` (ver secciones 14 y 15 de este informe).

## 5. Pantallas rediseñadas

Hoy (bloque B), creación de tareas (bloque C: barra rápida + diálogo de
opciones avanzadas), filas de tarea y diálogos (bloque D: editar,
recordatorio, `TaskFormFields` compartido), navegación (bloque E),
Ajustes (bloque F), dark mode (bloque G), microinteracciones y haptics
(bloque H), accesibilidad (bloque I), adaptabilidad (bloque J: dos
paneles en ≥600dp, encabezado compacto, insets), rendimiento (bloque K).
El widget RemoteViews se alineó a la paleta a mano (bloque G).

## 6. Cambios de Home

- Marca «AHORA» (`displayLarge`) + lema «Sácalo de tu cabeza.»
  (`bodyLarge`, `onSurfaceVariant`); encabezado compacto en pantallas
  bajas (< 480dp de alto).
- Barra de captura: `TextField` sin bordes, superficie `surfaceVariant`,
  voz + campo + enviar en una línea; el icono «Más opciones» y el de
  enviar aparecen solo al escribir; chips de feedback del lenguaje natural
  (`AssistChip` neutros) que abren el diálogo avanzado.
- Borrador centralizado en un solo holder (`CreationDraftState`, un único
  `rememberSaveable`): sobrevive a rotación y muerte del proceso; lo
  manual siempre gana sobre lo detectado.
- Sección «HOY» con etiqueta del sistema + contador discreto (al 90% de
  opacidad: 6.65:1 en oscuro, 4.62:1 en claro — AA).
- Filas sin tarjeta, divisor `outlineVariant`; al completar: tachado +
  opacidad 0.55 animada + tick háptico; menú ⋮ por fila (recordatorio /
  eliminar) en vez de dos iconos directos.
- Aviso puntual de alarmas exactas revocadas (superficie neutra,
  descartable, no insistente).
- Botón de micrófono con etiqueta que sigue al estado («Dictar tarea» /
  «Detener dictado») + pulso animado + texto «Escuchando…».

## 7. Cambios de creación de tareas

Separación clara entre **creación rápida** (abrir → escribir/hablar →
guardar) y **configuración avanzada** (diálogo «Opciones de la tarea»:
prioridad, fecha límite, recordatorio, recurrencia, con el `TaskFormFields`
compartido). «Cancelar» restaura el borrador; «Guardar» lo deja en el
borrador para aplicar al enviar. El recordatorio manual ofrece añadir con
el diálogo de fecha/hora o quitar el detectado. Nada del flujo de guardado
(`repository.add`) cambió.

## 8. Cambios en tareas

- `TaskRow`: checkbox circular con `Role.Checkbox` y etiqueta = título de
  la tarea; pills de metadatos neutras (`MetaPill`: icono 12dp +
  `labelSmall`), color `error` solo en prioridad alta y fecha vencida;
  altura mínima 48dp; entrada escalonada (tope 360ms), salida con
  fundido + colapso en 200ms; `animateItemPlacement` con `key(id)`.
- `TasksColumn`: diálogos de editar y recordatorio integrados; callbacks
  estables por fila (instancias `remember` compartidas); scroll-to-top al
  tocar la pestaña activa vía flujo del ViewModel.
- Diálogos con entrada fundido + escala 0.97→1 en 200ms; al cerrar
  desaparecen al instante.
- `TaskDetailPanel` (pantallas anchas): edición en vivo sin estado
  pendiente (el título nunca se guarda en blanco), completar y eliminar
  integrados, estado vacío «Sin selección».

## 9. Cambios en navegación

`NavigationBar` de M3 con 3 destinos (Hoy / Todas / Ajustes); selección =
indicador `primaryContainer` + icono relleno (`onPrimaryContainer`);
etiquetas neutras; `singleTop` + `saveState`/`restoreState`; transiciones
desde `Motion` (entrada 280ms / salida 240ms, con dirección); tocar la
pestaña activa sube la lista visible al inicio con desplazamiento suave.

## 10. Cambios en Settings

Secciones con `FormSection` (misma etiqueta del sistema que los
diálogos), filas reutilizables `SettingRow` (`SettingLinkRow` /
`SettingSwitchRow` con anuncio único en TalkBack), separadores
`outlineVariant`, selector de tema con `FilterChip` en `primaryContainer`
propio (unificado en la 1.25.0 al helper compartido), resultados del
respaldo en `Snackbar` con acción «Reintentar», contenido centrado a
720dp en tablets.

## 11. Cambios en dark mode

Paleta propia (fondo `#0B0B0D`, superficie `#141417`, superficie variante
`#1D1D22`, texto papel `#F5F3EE`, secundario `#A8A6A0`, acento
`#5B93FF`, error `#F2B8B5`, divisores papel al 10%). Contraste AA
verificado por cálculo y fijado por `ColorContrastTest` (227 tests):
`onSurfaceVariant`/fondo 8.08:1, pill 6.90:1, contador 6.65:1,
error/pill 9.83:1, primario/fondo 6.61:1. Widget con colores explícitos
en `values-night`. En la 1.25.0, diálogos y menús usan `surface` también
en oscuro (antes, gris frío por defecto de M3).

## 12. Microinteracciones

Centralizadas en `Motion`: entrada de fila 280ms (fundido + desliz +
expansión, stagger 45ms/ítem con tope 360ms), salida de fila 200ms
(fundido + colapso), entrada de pantalla 280/240ms, diálogos 200ms
(fundido + escala sutil, cierre instantáneo), estado vacío 350ms, chips
de feedback 240/200ms, pulso de micrófono 1600ms. Spring del checkbox
suave (`dampingRatio = 0.8`), sin rebotes exagerados en ningún lado.
Cero valores sueltos (fijado por `MotionTest`).

## 13. Haptics

Un único patrón: `Haptics.tick()` (tick corto y ligero;
`TextHandleMove`, el correcto en este BOM de Compose donde `ClockTick`
no existe). Solo como confirmación: marcar/desmarcar, enviar desde la
creación rápida, guardar edición, guardar recordatorio. Deliberadamente
sin háptico: eliminar, deshacer, quitar recordatorio, elegir chips,
cambio de tema, navegación, micrófono. Respeta los ajustes del sistema
(`performHapticFeedback` no vibra si está desactivado).

## 14. Accesibilidad

- Área táctil mínima 48dp en filas, botones de icono, checkbox y filas de
  ajustes (fijado por `AccessibilityTokensTest`). Excepción documentada:
  los chips de M3 quedan en 32dp (estándar Material; ampliar el área
  deformaría el chip): cumplen WCAG 2.2 AA (mínimo 24px).
- `contentDescription` en todo icono con acción; `null` en decorativos.
- `Role.Checkbox` con el título como etiqueta; `mergeDescendants` para un
  solo anuncio por fila de tarea y de ajuste; `Switch` solo visual en
  ajustes (semántica limpia); etiquetas de sección como encabezados.
- Contraste AA en ambas paletas (fijado por `ColorContrastTest`).
- La UI no depende solo del color: prioridad alta lleva icono + texto, la
  fecha vencida lleva texto («Ayer», «hace 2 días»).
- `FlowRow` donde el texto grande del sistema podría recortar (chips de
  formularios, selector de tema).

## 15. Adaptabilidad

- Una columna hasta 600dp; dos paneles (lista + detalle) desde 600dp
  (`AdaptiveLayout` con `BoxWithConstraints`, funciones puras probadas en
  `AdaptiveLayoutTest`; sin `material3-window-size-class`).
- Encabezado compacto bajo 480dp de alto (landscape de teléfono).
- Ajustes centrado a 720dp y panel de detalle topado a 560dp en tablets.
- `enableEdgeToEdge` con insets respetados (corregido el doble padding de
  los `Scaffold` interiores en el bloque J); teclado con `adjustResize`.
- Sin `sp` fijos fuera del sistema (verificado: solo `AhoraTypography`).

## 16. Rendimiento

Referencia Galaxy A14 (4 GB): callbacks estables por fila (bloque K),
`collectAsStateWithLifecycle` + `WhileSubscribed(5_000)`, búsqueda con
debounce de 300ms, sin polling (widget reactivo), `tween` creados en
composición (no por frame), animación infinita del micrófono solo al
escuchar, sin sombras ni elevaciones, sin fondos duplicados, APK debug
~16 MB, sin dependencias visuales pesadas. Sin dispositivo no se pudo
medir con Layout Inspector ni GPU overdraw (declarado en cada bloque).

## 17. Tests ejecutados

Suite JVM vía `run-jvm-tests.sh` (JUnitCore directo; el worker
`testDebugUnitTest` de Gradle sigue roto en este entorno — NPE
preexistente en `SuiteTestClassProcessor`, documentado): **238/238 en
verde** (la suite incluye `ColorContrastTest` con 227 aserciones de
contraste, `SpacingTest`, `MotionTest`, `DesignSystemTest`,
`AccessibilityTokensTest`, `AdaptiveLayoutTest`, `CreationDraftTest`,
`NaturalLanguageParserTest`, `TaskRepositoryTest`,
`ReminderSchedulerTest`, etc.). Tests instrumentados (`TaskDaoTest`,
`MigrationTest`): compilan, sin dispositivo para ejecutarlos.

## 18. Build/lint realizados

| Comprobación | Resultado |
|---|---|
| `build-android.sh assembleDebug` | ✅ OK (1m 31s; APK ~16 MB) |
| `run-jvm-tests.sh` (suite JVM completa) | ✅ 238/238 verdes |
| `build-android.sh lintDebug` | ✅ 0 errores |
| Avisos de lint | 48, todos «hay versión más nueva» de dependencias (AGP, Room, lifecycle…) — deuda opcional, ningún warning de código |

## 19. Problemas encontrados en la auditoría post-rediseño

Inspección del código tras los bloques A–K, comparando contra
`DESIGN_SYSTEM.md` y contra lo que cada bloque dijo implementar.

**CRÍTICO:** ninguno.

**IMPORTANTE:**
1. Los `FilterChip`/`InputChip` seleccionados usaban el
   `secondaryContainer` por defecto de M3 (lila) en vez del
   `primaryContainer` propio: `AhoraTheme` no define `secondaryContainer`
   y ningún bloque lo había detectado. Afectaba a todos los formularios
   (prioridad, fecha límite, recurrencia, recordatorio) y rompía la regla
   de «un solo acento».
2. `AlertDialog`, `DatePickerDialog` y `DropdownMenu` usaban
   `surfaceContainerHigh`/`surfaceContainer` por defecto de M3 (gris
   frío): los diálogos no vivían en el `surface` de la paleta como dice
   el design system, en claro ni en oscuro.

**MENOR (con impacto real):**
3. El buscador de «Todas» usaba `surfaceContainerLow` (default de M3,
   gris frío) mientras la barra de captura de Hoy usa `surfaceVariant`:
   inconsistencia visible entre pantallas.
4. `labelMedium` usado en 2 lugares (`AllTasksScreen`, línea del contador
   de resultados; `AdvancedCreationDialog`, etiqueta «Recordatorio») sin
   estar definido en `AhoraTypography`: caía al default de M3 y rompía la
   regla «todo sale de estos roles».
5. `PulseRing` (HomeScreen) con `40.dp`/`2.dp` literales: únicos `dp`
   sueltos que quedaban fuera de `Spacing`/`Sizes`/`AdaptiveLayout`.
6. UX: si el usuario negaba el permiso de notificaciones justo al guardar
   un recordatorio, la elección se descartaba en silencio — el
   recordatorio nunca sonaría y nadie se lo decía.
7. Docs: `DESIGN_SYSTEM.md` §8 prometía «ilustración» en los estados
   vacíos; el código no tiene ninguna (texto + botón).

**OPCIONAL (no implementado, a conciencia):**
- Ilustración para los estados vacíos (decoración; el minimalismo gana).
- Mapear `surfaceContainerLowest`/`Low` a la paleta (ningún componente
  los usa tras la corrección 3).
- Personalizar colores del `Snackbar` (usa `inverseSurface` por defecto
  de M3; transitorio y legible).
- Reducir el padding vertical de `TaskRow` (12dp por lado): es la
  intención de «espacio respirable», no un bug.
- `outline` por defecto de M3 en el borde del checkbox sin marcar
  (decorativo; documentado como default aceptado).

**No verificable sin dispositivo (declarado, no inventado):** renderizado
real de los dos paneles en tablet, encabezado compacto rotando un
teléfono físico, resultado visual final de los insets, contraste AA
medido en pantalla, TalkBack con lector real, alarmas exactas en Doze,
sonido/vibración de notificaciones, widget en launcher real, voz con
micrófono, tests instrumentados.

## 20. Problemas corregidos

1. **Chips seleccionados** → nuevo `ui/components/AhoraChips.kt` con
   `ahoraSelectedChipColors()` (`primaryContainer`/`onPrimaryContainer`
   propios); aplicado en `TaskFormFields.kt`, `ReminderDialog.kt`,
   `AdvancedCreationDialog.kt`, `TaskDetailPanel.kt`; `SettingsScreen.kt`
   (selector de tema) unificado al helper (antes colores inline).
2. **Diálogos/menús en `surface`** → `ui/theme/Theme.kt`: `surfaceContainer`
   y `surfaceContainerHigh` = `surface` en ambas paletas.
3. **Buscador de «Todas»** → `surfaceVariant` como la barra de Hoy.
4. **`labelMedium` → `labelSmall`** en los 2 usos fuera del sistema.
5. **`dp` sueltos** → `Sizes.pulseRing` (40dp) y `Sizes.pulseRingStroke`
   (2dp); import `dp` eliminado de `HomeScreen.kt`.
6. **Permiso denegado al guardar recordatorio** → `ReminderFlowHost`
   acepta `onNotifPermissionDenied`; `MainViewModel.notifPermissionDenied()`
   emite `UiEvent.Message("Sin permiso de notificaciones, el recordatorio
   no te avisará")`; cableado en `TasksColumn` y `TaskDetailPanel` (Hoy y
   Todas). El recordatorio no se guarda porque sin el permiso no podría
   avisar (decisión documentada en el código).
7. **Docs** → `DESIGN_SYSTEM.md` actualizado (tokens nuevos, helper de
   chips, §8 sin la ilustración inexistente).

## 21. Problemas pendientes

- Los opcionales de la sección 19 (decisión consciente: no aportan sin
  añadir complejidad).
- Deuda previa: dependencias desactualizadas (AGP 8.5.2, Room 2.6.1,
  lifecycle 2.8.3…), worker de tests de Gradle roto en este entorno,
  `targetSdk` 34.
- Todo lo «no verificable sin dispositivo» de la sección 19: la primera
  prueba en un teléfono real (idealmente un gama de entrada) debería
  cubrir TalkBack, contraste en pantalla, insets finales, alarmas en
  Doze y el widget en el launcher.
- Funcionalidad documentada como pendiente desde las etapas: fecha límite
  sin alarma propia, sin filtros en «Todas», sin insignias en el icono,
  sin intervalos de recurrencia personalizados, sin creación rápida desde
  el widget, búsqueda sin normalizar tildes.

## 22. Archivos modificados

Fase completa (bloques A–K + auditoría 1.25.0): ver `git log` 1.14.0 →
1.25.0. En la pasada de auditoría/corrección (1.25.0) cambiaron:

- `app/src/main/java/com/ahora/app/ui/components/AhoraChips.kt` (nuevo)
- `app/src/main/java/com/ahora/app/ui/components/TaskFormFields.kt`
- `app/src/main/java/com/ahora/app/ui/components/ReminderDialog.kt`
- `app/src/main/java/com/ahora/app/ui/components/AdvancedCreationDialog.kt`
- `app/src/main/java/com/ahora/app/ui/components/TaskDetailPanel.kt`
- `app/src/main/java/com/ahora/app/ui/components/TasksColumn.kt`
- `app/src/main/java/com/ahora/app/ui/components/ReminderFlowHost.kt`
- `app/src/main/java/com/ahora/app/ui/theme/Theme.kt`
- `app/src/main/java/com/ahora/app/ui/theme/Sizes.kt`
- `app/src/main/java/com/ahora/app/ui/screens/HomeScreen.kt`
- `app/src/main/java/com/ahora/app/ui/screens/AllTasksScreen.kt`
- `app/src/main/java/com/ahora/app/ui/settings/SettingsScreen.kt`
- `app/src/main/java/com/ahora/app/ui/MainViewModel.kt`
- `app/build.gradle.kts` (versionCode 27, versionName 1.25.0)
- `MEJORAS.md` (changelog 1.25.0)
- `docs/DESIGN_SYSTEM.md` (tokens y §8)
- `docs/informe-final-rediseno.md` (este informe)

## 23. Dependencias añadidas o modificadas

Ninguna. El rediseño completo (bloques A–K) y la auditoría (1.25.0) no
añadieron ni actualizaron dependencias: todo con el BOM y las
bibliotecas que ya tenía el proyecto (Compose, M3, Room, DataStore,
Navigation, Lifecycle). `BoxWithConstraints` evitó añadir
`material3-window-size-class`; el widget sigue siendo RemoteViews sin
Glance.

## 24. Recomendación para la siguiente fase

1. **Probar en un teléfono real** (idealmente gama de entrada, p. ej. el
   Galaxy A14 de referencia): TalkBack de punta a punta, contraste en
   pantalla, insets finales con gestos y teclado, alarmas exactas en Doze,
   widget en el launcher, voz con micrófono. Varias verificaciones de
   esta fase quedaron declaradas como «sin dispositivo».
2. **Deuda técnica programada:** actualizar dependencias (AGP, Room,
   lifecycle, navigation) con su propia pasada de build+tests; evaluar
   `targetSdk` 35 con verificación aparte.
3. **Funcionalidad pendiente de las etapas** (decisiones de producto, no
   bugs): filtros en «Todas», fecha límite con alarma propia o sin ella
   (definirlo), normalización de tildes en la búsqueda.
4. **Diseño:** con el sistema ya cerrado, el próximo trabajo visual
   debería venir del uso real (feedback de Daniel), no de más rondas de
   auditoría: el sistema está completo y fijado por tests.
5. **No reabrir** decisiones cerradas: paleta de un acento, filas sin
   tarjeta, creación rápida vs. avanzada, sin ilustraciones decorativas.
