# AUDITORÍA VISUAL — App «Ahora» (FASE 3)

Auditoría pantalla por pantalla del estado real a 2026-09-29 (1.13.0),
hecha sobre el código. Filosofía: «Sácalo de tu cabeza.» — la interfaz debe
desaparecer y dejar solo lo que tienes que hacer.

---

## Pantalla: Hoy (`HomeScreen`)

**Conservar:**
- Marca «Ahora» fija arriba a la izquierda; tipografía sans humana, peso normal.
- Barra de creación rápida abajo (una sola línea: voz + campo + enviar). Es
  el corazón de «sácalo de tu cabeza»: capturar en dos toques.
- Chips debajo del campo que muestran lo detectado por lenguaje natural
  («Se pondrá para hoy · 15:00»).
- Filas sin tarjeta, separadas por divisor sutil; texto de la tarea como
  protagonista absoluto.
- Vacío con ilustración suave («Tu cabeza, despejada») en vez de un
  placeholder genérico.

**Eliminar:**
- Nada estructural. Pero la gestión de 12 estados de borrador
  (`rememberSaveable` x 12) es frágil: si el proceso muere entre editar y
  guardar, el borrador se pierde. El rediseño debe centralizar ese estado.

**Mejorar:**
- La barra rápida crece en dos modos (mínimo / expandido) con un salto de
  animación que se nota. El expandido muestra prioridad + fecha + recurrencia
  a la vez: demasiadas decisiones para un acto que debe ser automático.
  Rediseño: creación rápida (texto → guardar) vs. configuración avanzada
  (diálogo completo) con separación clara.
- El aviso de alarmas exactas aparece una vez y ya; bien. No insistir.
- `TasksColumn`: las filas se separan con `HorizontalDivider` en
  `outlineVariant` — tono correcto y sutil; conservar. En dark mode,
  verificar contraste AA en un dispositivo real.

## Componente: fila de tarea (`TaskRow`)

**Conservar:**
- Checkbox circular con spring sutil; check que se dibuja con trazo.
- Tachado + opacidad 0.55 al completar, sin desaparecer de golpe.
- Callbacks estables por fila (no recrear lambdas) — conservar el patrón de
  rendimiento.
- Pills neutras: el color solo comunica (prioridad alta, vencida).

**Eliminar:**
- Los `dp` sueltos: 27.dp / 24.dp (checkbox), 2.dp (borde), 15.dp (radio),
  13.sp (fuente) en `CircularCheckButton`. Deben venir del design system.

**Mejorar:**
- `MetaPill` usa `labelSmall` y `tertiary` de MaterialTheme, que `AhoraTheme`
  NO define: caen a los defaults de M3. Centralizar en el tema propio para
  que la app no dependa de la suerte del default.
- Dos botones de icono a la derecha (campana + papelera) hacen la fila más
  densa de lo necesario. Considerar: papelera solo en modo edición o swipe;
  la campana puede vivir en el diálogo de edición.

## Componente: columna de tareas (`TasksColumn`)

**Conservar:** `key(task.id)`, animación `slideIn` + `fadeIn` sutil al
aparecer, sin overshoot.

**Mejorar:** entrada con `initialOffsetY = 12.dp` — verificar que no se siente
mecánica en listas largas; el rediseño puede probar stagger por índice con
`Motion.delayPerItem` (ya existe en el sistema, no se usa aquí).

## Pantalla: Todas (`AllTasksScreen`)

**Conservar:** buscador arriba con debounce; el Deshacer también funciona
aquí (`CollectUiEvents`).

**Mejorar:** es funcionalmente correcta pero visualmente es «Hoy sin
secciones»: el rediseño puede darle identidad propia (filtros por
prioridad/fecha, agrupación) sin romper nada.

## Diálogos: edición y recordatorio (`EditTaskDialog`, `ReminderDialog`)

**Conservar:**
- `TaskFormFields` compartido (un solo componente para crear y editar).
- Atajos Hoy / Mañana / Elegir fecha; chips de prioridad con iconos de
  bandera; recurrencia con iconos claros.
- Corrección de zona horaria Cuba en el DatePicker (documentada y testeada).
- El recordatorio valida «no en el pasado» y ofrece «en 1 hora».

**Mejorar:**
- El diálogo de edición es largo (título + prioridad + fecha + recurrencia +
  recordatorio). Rediseño: jerarquía más clara, secciones con etiquetas
  `labelLarge` del sistema, espaciado consistente.
- `ReminderDialog` duplica lógica de fecha/hora con `TaskFormFields`
  (DatePicker + TimePicker vs. atajos). Unificar patrones.

## Pantalla: Ajustes (`SettingsScreen`)

**Conservar:**
- Secciones con etiquetas en mayúsculas (`labelLarge`).
- Enlaces a ajustes del sistema para sonido/vibración (honesto: Android no
  deja cambiar un canal ya creado desde la app).
- Exportar/importar con Storage Access Framework (sin permisos extra).

**Mejorar:**
- Lista larga de 313 líneas con muchos `Spacer`: necesita ritmo visual
  (agrupación en tarjetas sutiles o secciones colapsables) y un componente
  `SettingRow` reutilizable en vez de filas ad-hoc.
- Los mensajes de respaldo (`backupMessage`) aparecen como texto suelto;
  usar Snackbar del sistema.

## Widget (`WidgetContent` / RemoteViews)

**Conservar:** máx. 7 filas, pendientes de hoy primero, sin polling, completar
desde el widget.

**Mejorar:** el layout RemoteViews usa estilos del sistema; al cambiar la
paleta hay que actualizarlo a mano. El rediseño debe incluir sus colores
explícitamente (fondo, acento, texto secundario) para que no se quede atrás.

## Modo oscuro

**Conservar:** paletas propias (no inversión automática), superficies
elevadas sutilmente más claras, mismo acento.

**Mejorar:** auditar el divisor al 50% de alpha y el `surfaceVariant` de las
pills en dark: verificar contraste AA en un A14 real.

## Microinteracciones (`Motion`)

**Conservar:** duraciones y easings centralizados; `MediumNoBounce` para UI.

**Eliminar:** `springMediumBouncy` con `dampingRatio = 0.55f` — el prompt
premium prohíbe rebotes exagerados. Hoy solo se usa en el checkbox; evaluar
si incluso ahí sobra.

## Accesibilidad

**Conservar:** `contentDescription` en iconos, `Role` donde aplica.

**Mejorar:** tamaño táctil mínimo 48dp en filas de tarea (hoy la fila depende
del contenido); contraste de `onSurfaceVariant` en pills; `semantics` en el
checkbox circular personalizado.

---

## Resumen: conservar / eliminar / mejorar

**Conservar:** Spacing, Motion (sin el bouncy), roles tipográficos, paleta
reducida con un acento, filas sin tarjeta, pills neutras con color solo donde
comunica, dark mode propio, patrones de rendimiento (callbacks estables,
keys, `collectAsStateWithLifecycle`), componentes compartidos
(`TaskFormFields`), honestidad con el sistema (canal de notificación,
permisos en el momento de uso).

**Eliminar:** dp/sp sueltos en `TaskRow`/`CircularCheckButton`; dependencia
de `labelSmall`/`tertiary` por defecto de M3; spring bouncy; 12 estados de
borrador dispersos.

**Mejorar:** jerarquía de creación (rápida vs. avanzada), densidad de la fila
(dos iconos a la derecha), identidad de «Todas», ritmo visual de Ajustes,
estilo manual del widget, contraste del divisor en dark, tamaños táctiles.
