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
8. Exportar/importar respaldo (JSON) — la BD Room ya lo permite.
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
  - Los interruptores de sonido/vibración de Ajustes ahora se aplican de verdad al canal del sistema (en Android 8+ el canal manda, no la notificación).
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
