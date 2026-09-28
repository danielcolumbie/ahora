# Ahora — «Sácalo de tu cabeza.»

**Ahora** es una aplicación Android nativa y minimalista para recordar las pequeñas
cosas de la vida cotidiana: piensas algo → lo capturas en segundos → Ahora se
encarga de recordártelo.

Es una memoria externa para las pequeñas cosas. Sin cuentas, sin publicidad,
sin analítica, sin servidores: tus tareas viven solo en tu teléfono y la app
funciona completamente offline.

## Características (V1)

- Crear, editar, completar y eliminar tareas
- Captura rápida con campo grande + botón de micrófono (voz a texto)
- Recordatorios locales con notificaciones (sin Firebase)
- Modo oscuro (principal) y modo claro
- Animaciones suaves (completar, añadir, eliminar con deshacer)
- Pantalla vacía elegante, accesibilidad básica
- Navegación mínima: Hoy · Todas · Ajustes
- Rendimiento pensado para gama baja/media

## Stack

- Kotlin + Jetpack Compose + Material 3
- Arquitectura limpia por capas: `ui` → `domain` → `data` (+ `notifications`, `speech`)
- Room (tareas) y DataStore (ajustes) — almacenamiento 100% local
- `StateFlow`/`Flow` + `ViewModel` (DI manual, sin frameworks)
- Voz a texto con la API gratuita de Android (`SpeechRecognizer`)
- Recordatorios con `AlarmManager` (exactos cuando el sistema lo permite) +
  `BroadcastReceiver`; se reprograman al reiniciar el teléfono

## Cómo compilarla

Requisitos: [Android Studio](https://developer.android.com/studio) (Ladybug o
superior) con JDK 17.

1. Abre Android Studio → **Open** → selecciona la carpeta `ahora/`.
2. Android Studio sincroniza Gradle y genera el wrapper automáticamente.
3. Pulsa **Run ▶** con un emulador o un teléfono conectado.

Desde terminal (con el wrapper incluido en el proyecto):

```bash
./gradlew assembleDebug      # APK de pruebas → app/build/outputs/apk/debug/
./gradlew bundleRelease      # AAB para publicar → app/build/outputs/bundle/release/
./gradlew lintDebug          # análisis estático (0 errores)
```

> Nota: el APK release se firma con la clave de debug por defecto. Para publicar
> en Google Play, configura tu propia clave de firma en `app/build.gradle.kts`.

## Cómo instalarla

- **Pruebas:** instala el APK debug en tu teléfono (permite "instalar apps
  desconocidas" para el instalador que uses) o pulsa Run desde Android Studio.
- **Distribución:** sube el AAB release firmado a Google Play Console.

## Estructura del proyecto

```
ahora/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/ahora/app/
│   │   ├── MainActivity.kt            # Única activity (Compose)
│   │   ├── AhoraApplication.kt        # Crea el contenedor DI + canal de notificaciones
│   │   ├── data/                      # Room + DataStore
│   │   │   ├── Task.kt                # Entidad (id, título, fechas, recordatorio, estado, recurrencia*)
│   │   │   ├── TaskDao.kt
│   │   │   ├── AhoraDatabase.kt
│   │   │   └── SettingsRepository.kt  # Apariencia, notificaciones, sonido, vibración
│   │   ├── domain/
│   │   │   └── TaskRepository.kt      # Lógica de negocio (sincroniza DB + alarmas)
│   │   ├── notifications/             # Infraestructura de recordatorios locales
│   │   │   ├── NotificationHelper.kt
│   │   │   ├── ReminderScheduler.kt   # AlarmManager
│   │   │   ├── ReminderReceiver.kt    # Muestra la notificación
│   │   │   └── BootReceiver.kt        # Reprograma tras reiniciar
│   │   ├── speech/
│   │   │   └── SpeechInputManager.kt  # Voz a texto (SpeechRecognizer)
│   │   ├── di/
│   │   │   └── AppContainer.kt        # Dependencias manuales
│   │   └── ui/
│   │       ├── theme/                 # Color.kt, Type.kt, Theme.kt (oscuro/claro)
│   │       ├── navigation/            # NavGraph: Hoy · Todas · Ajustes
│   │       ├── components/            # TaskRow, TasksColumn, diálogos, EmptyState
│   │       ├── screens/               # HomeScreen, AllTasksScreen
│   │       ├── settings/              # SettingsScreen + ViewModel
│   │       └── MainViewModel.kt
│   └── res/                           # Icono adaptativo, tema de ventana, backup rules
├── gradle/libs.versions.toml          # Catálogo de versiones
└── README.md
```

\* `recurrence` está reservado en el modelo para la futura función de tareas
recurrentes; la V1 no lo utiliza.

## Permisos

Solo los estrictamente necesarios, y se piden en el momento de uso:

| Permiso | Cuándo se pide |
|---|---|
| Micrófono | Al pulsar el botón de dictado |
| Notificaciones | Al activar notificaciones en Ajustes (Android 13+) |
| Alarmas exactas | Lo gestiona el sistema para los recordatorios |
| Recibir arranque completado | Automático, para reprogramar recordatorios |

## Privacidad

Privacy-first: no hay registro, no se pide correo, no hay publicidad ni
analítica, no se envía nada a ningún servidor. Todo permanece en el dispositivo.

## Licencia

MIT — ver [LICENSE](LICENSE). Los componentes de terceros (Kotlin, Jetpack
Compose, AndroidX) están bajo Apache License 2.0.

## Hoja de ruta (futuro, no implementado)

Interpretación de lenguaje natural, tareas recurrentes, widgets, sincronización
entre dispositivos, copia de seguridad, estadísticas, traducción al inglés y
versión para iOS. La arquitectura ya deja el espacio para todo eso.
