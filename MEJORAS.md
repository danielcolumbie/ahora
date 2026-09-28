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

## Feedback de Daniel (pendiente)
_(sus reportes van aquí arriba del backlog)_

## Changelog
### 1.0.0 (2026-09-28) — versión inicial
- Crear, editar, completar y eliminar tareas (con deshacer).
- Vistas Hoy / Todas / Ajustes.
- Recordatorios y notificaciones locales.
- Entrada por voz (sin inventar texto si falla).
- Temas claro / oscuro / automático.
- Persistencia local con Room, preferencias con DataStore.
- 100% offline, sin cuentas ni rastreo.
