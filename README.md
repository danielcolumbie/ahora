# Ahora — «Sácalo de tu cabeza.»

**Versión actual: 1.26.0** (Release Candidate).

**Ahora** es una aplicación Android nativa y minimalista para recordar las pequeñas
cosas de la vida cotidiana: piensas algo → lo capturas en segundos → Ahora se
encarga de recordártelo.

Es una memoria externa para las pequeñas cosas. Sin cuentas, sin publicidad,
sin analítica: tus tareas viven solo en tu teléfono.

## Características (1.26.0)

- Crear, editar, completar y eliminar tareas (con deshacer)
- Captura rápida con campo grande + botón de micrófono (voz a texto)
- Interpretación de lenguaje natural («llamar a mamá mañana a las 3pm»)
- Prioridades, fechas límite y tareas recurrentes
- Recordatorios locales con notificaciones (sin Firebase)
- Widget de lista con completar desde el propio widget
- Búsqueda de tareas y respaldo local (exportar/importar)
- Modo oscuro (principal) y modo claro
- Animaciones suaves, pantalla vacía elegante, accesibilidad básica
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

> Nota: el APK release se firma con la clave de distribución configurada por
> variables de entorno (`AHORA_KEYSTORE_PATH`, `AHORA_KEYSTORE_PASSWORD`,
> `AHORA_KEY_ALIAS`, `AHORA_KEY_PASSWORD`); sin ellas usa la clave de debug.
> Ver [docs/firma-distribucion.md](docs/firma-distribucion.md). La clave nunca
> se sube al repositorio.

## Cómo instalarla

- **Pruebas:** instala el APK debug en tu teléfono (permite "instalar apps
  desconocidas" para el instalador que uses) o pulsa Run desde Android Studio.
- **Distribución:** el APK release firmado se publica en
  [GitHub Releases](https://github.com/danielcolumbie/ahora/releases);
  el AAB release firmado queda listo para Google Play Console.

## Estructura del proyecto

```
ahora/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/ahora/app/
│   │   ├── MainActivity.kt            # Única activity (Compose)
│   │   ├── AhoraApplication.kt        # Crea el contenedor DI + canal de notificaciones
│   │   ├── data/                      # Room + DataStore
│   │   │   ├── Task.kt                # Entidad (id, título, fechas, recordatorio, estado, recurrencia)
│   │   │   ├── TaskDao.kt
│   │   │   ├── AhoraDatabase.kt       # v2 + MIGRATION_1_2
│   │   │   ├── Migrations.kt
│   │   │   └── SettingsRepository.kt  # Apariencia, notificaciones, sonido, vibración
│   │   ├── domain/
│   │   │   ├── TaskRepository.kt      # Lógica de negocio (sincroniza DB + alarmas)
│   │   │   └── NaturalLanguageParser.kt  # Interpretación de lenguaje natural
│   │   ├── notifications/             # Infraestructura de recordatorios locales
│   │   │   ├── NotificationHelper.kt
│   │   │   ├── ReminderScheduler.kt   # AlarmManager
│   │   │   ├── ReminderReceiver.kt    # Muestra la notificación
│   │   │   └── BootReceiver.kt        # Reprograma tras reiniciar
│   │   ├── widget/                    # Widget de lista (RemoteViews)
│   │   │   ├── AhoraWidgetProvider.kt
│   │   │   ├── WidgetTaskService.kt
│   │   │   ├── WidgetContent.kt
│   │   │   └── WidgetRefresher.kt
│   │   ├── speech/
│   │   │   └── SpeechInputManager.kt  # Voz a texto (SpeechRecognizer, preferencia offline)
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
├── app/schemas/                       # Schemas Room exportados (validar migraciones)
├── gradle/libs.versions.toml          # Catálogo de versiones
└── README.md
```

## Permisos

Solo los estrictamente necesarios, y se piden en el momento de uso:

| Permiso | Cuándo se pide |
|---|---|
| Micrófono | Al pulsar el botón de dictado |
| Notificaciones | Al crear un recordatorio (Android 13+) o en Ajustes |
| Alarmas exactas | Lo gestiona el sistema para los recordatorios |
| Recibir arranque completado | Automático, para reprogramar recordatorios |

## Privacidad

Privacy-first: no hay registro, no se pide correo, no hay publicidad ni
analítica. Tus tareas y ajustes permanecen en el dispositivo.

Excepción honesta: el dictado por voz usa el `SpeechRecognizer` de Android.
La app pide preferencia por el reconocimiento offline, pero si tu teléfono
no tiene el paquete de voz sin conexión, ese audio puede procesarse en los
servidores de Google. Todo lo demás es 100% local.

## Licencia

MIT — ver [LICENSE](LICENSE). Los componentes de terceros (Kotlin, Jetpack
Compose, AndroidX) están bajo Apache License 2.0.

## Hoja de ruta (futuro, no implementado)

Sincronización entre dispositivos, estadísticas, traducción al inglés y
versión para iOS. La arquitectura ya deja el espacio para todo eso.
