# Migración de Ahora a SDK 36 (Android 16)

**Fecha:** 2026-09-29
**Versión:** 1.26.0 (código 28)
**Cambio:** `compileSdk` 34 → 36, `targetSdk` 34 → 36. `minSdk` sigue en 26.
**Estado:** auditoría → plan → modificación → prueba → corrección → re-prueba → auditoría final. Completado sin commit (pendiente orden de Daniel).

## 1. Auditoría funcional (previa, API 34)

- **Notificaciones:** canal `ahora_recordatorios_v2`; `POST_NOTIFICATIONS` se pide durante el uso y se comprueba antes de `notify()`. Sin full-screen intents.
- **Recordatorios:** `AlarmManager` con exactas (`setExactAndAllowWhileIdle`) y fallback a inexactas (`setAndAllowWhileIdle`) si falta o se revoca `SCHEDULE_EXACT_ALARM`. `PendingIntent` inmutables.
- **Reinicio/hora:** `BootReceiver` reprograma con `BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED` y `MY_PACKAGE_REPLACED`.
- **Widget:** `RemoteViewsService`, `updatePeriodMillis=0`, Room acotado, actualización reactiva sin polling; `setColorFilter` protegido con `runCatching`.
- **Voz:** `SpeechRecognizer`, preferencia offline y un reintento normal; `cancel()`/`destroy()` terminales; `release()` desde `DisposableEffect`.
- **Room:** BD versión 2, migración explícita 1→2, sin fallback destructivo. Schemas exportados (`app/schemas/.../1.json`, `2.json`) intactos tras la migración.
- **UI:** `enableEdgeToEdge()` activo; sin handlers manuales de back.
- **Batería:** sin servicios en primer plano, sin polling, sin timers permanentes.
- **16 KB pages:** no hay bibliotecas `.so` en el proyecto.
- **Pantallas grandes:** sin bloqueo de orientación ni de aspect ratio.
- **Datos:** Room/DataStore locales, `allowBackup=false`.

## 2. Plan

1. Subir toolchain: AGP 8.5.2 → 8.10.1, Gradle 8.7 → 8.11.1.
2. Subir `compileSdk`/`targetSdk` a 36, conservar `minSdk = 26`, `versionName = 1.26.0`, `versionCode = 28`.
3. No agregar dependencias, ni servicios, ni rediseños, ni cambios de arquitectura.
4. Probar: tests JVM → lintRelease → assembleDebug → assembleRelease → bundleRelease → verificación con `aapt` y firma.
5. Corregir lo que rompa la migración y volver a probar.

## 3. Archivos cambiados (solo estos)

| Archivo | Cambio |
|---|---|
| `app/build.gradle.kts` | `compileSdk = 36`, `targetSdk = 36` |
| `gradle/libs.versions.toml` | AGP `8.5.2` → `8.10.1` |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle `8.7` → `8.11.1` |
| `build-android.sh` | PATH de Gradle `8.11.1` |
| `run-jvm-tests.sh` | PATH de Gradle `8.11.1` |
| `app/src/main/java/com/ahora/app/domain/NaturalLanguageParser.kt` | 3× `claimed.removeLast()` → `claimed.removeAt(claimed.lastIndex)` (único cambio de código funcional; ver §6) |

Sin commit, sin tag, sin push, sin release — pendiente orden de Daniel.

## 4. Comportamiento relevante de Android 15/16 (targetSdk 36)

- **Predictive back:** activo por defecto en Android 15+. La app no intercepta el back manualmente, así que usa el comportamiento del sistema sin cambios.
- **Edge-to-edge:** obligatorio en Android 15+. La app ya usa `enableEdgeToEdge()`; sin cambios.
- **Alarmas exactas:** `SCHEDULE_EXACT_ALARM` sigue siendo permiso especial; la app ya pide la exacta y cae a inexacta si se deniega o revoca. Sin cambios.
- **Notificaciones:** `POST_NOTIFICATIONS` en runtime ya implementado. Sin cambios.
- **16 KB pages:** Android 15+ exige compatibilidad; la app no trae `.so`, sin cambios.
- **Modo Doze / ahorro:** sin cambios en la estrategia (exactas con `setExactAndAllowWhileIdle` + reprogramación en boot).
- **Sin cambios de comportamiento observados** en el código para API 35/36: no se usaban APIs deprecadas que cambiaran de semántica.

## 5. Pruebas y resultados

### Tests JVM (`./run-jvm-tests.sh`)
- **244/244 en verde** con el toolchain nuevo (AGP 8.10.1 + Gradle 8.11.1), tras la corrección de `§6`.
- Antes de la corrección: 238/244 (5 fallos del parser por el bug de `removeLast()`, ver §6) + 1 fallo preexistente intermitente de `ScrollToTopTest` (dispatcher Main en el harness JVM; falla igual con el toolchain viejo, no es regresión de la migración; en la corrida final pasó).

### Lint (`lintRelease`)
- BUILD SUCCESSFUL. 0 errores, 20 warnings (15 `GradleDependency`, 3 `UseKtx`, 2 `AndroidGradlePluginVersion`: solo avisos de versiones disponibles, preexistentes).

### Compilación
- `assembleDebug`: OK.
- `assembleRelease`: OK.
- `bundleRelease`: OK.

### Verificación con `aapt` (APK release)
- paquete: `com.ahora.app`
- `versionCode`: 28 · `versionName`: 1.26.0
- `minSdk`: 26 · `targetSdk`: 36 · `compileSdk`: 36

### Room
- BD sigue en versión 2, migración 1→2 intacta, schemas JSON sin cambios. La actualización conserva los datos (sin `fallbackToDestructiveMigration`).

### Firma
- El APK release se generó **unsigned** (como en el RC); la firma oficial con el keystore de producción queda pendiente de las credenciales definitivas de Daniel (ver `docs/firma-distribucion.md`). No se firmó nada en esta tarea.

## 6. Hallazgo importante: bug real introducido por el toolchain nuevo

Con el toolchain nuevo, `claimed.removeLast()` en `NaturalLanguageParser.kt` (3 sitios: fechas imposibles, fechas numéricas inválidas, horas/minutos imposibles) **compilaba a `java.util.List.removeLast()` de JDK 21** (`invokeinterface`), en vez de a la extensión `kotlin.collections.CollectionsKt.removeLast` del toolchain viejo.

Consecuencias:
- En el harness JVM (JDK 17): `NoSuchMethodError` → 5 tests en rojo.
- **En teléfonos reales con Android API < 36 sería un crash** (`NoSuchMethodError`) al escribir, por ejemplo, «45 de octubre» o «25:99» — o sea, en el Galaxy A14 de Daniel (API 34) y en cualquier gama de entrada con minSdk 26.

Se corrigió con `claimed.removeAt(claimed.lastIndex)` (equivalente: la lista nunca está vacía en esos puntos porque `firstMatch` acaba de añadir el span). Bytecode verificado con `javap`: ahora emite `List.remove(int)` en los 3 sitios, portable a todos los API. Tras la corrección: 244/244.

## 7. Builds y tamaños

| Artefacto | RC API 34 | API 36 | Diferencia |
|---|---|---|---|
| `app-release-unsigned.apk` | 1.764.755 bytes | 1.715.607 bytes | −49 KB |
| `app-release.aab` | 3.712.219 bytes | 3.635.018 bytes | −77 KB |
| `app-debug.apk` | — | 16.048.260 bytes | (debug, con símbolos) |

El APK release de API 36 es **más ligero** que el del RC. APK ligero y sin servicios en segundo plano: se mantiene el perfil de bajo consumo.

## 8. Rendimiento y batería

- Sin cambios de arquitectura, sin servicios persistentes, sin polling, sin timers: el perfil de batería/RAM es el mismo del RC.
- Tamaño reducido (~3% menos): mejor para gama de entrada.
- `minSdk 26` conservado: la app sigue instalándose en todos los teléfonos desde Android 8.0.

## 9. Riesgos y pendientes (prueba física)

**No hubo pruebas físicas en esta tarea** (sin dispositivo ni emulador en el sandbox). Daniel probará en su Galaxy A14 (4 GB). Pendiente de validar en físico:

- predictive back y edge-to-edge en Android 15/16;
- alarmas exactas, Doze y comportamiento OEM Samsung (los Samsung suelen matar alarmas agresivamente);
- widget y reconocimiento de voz;
- conservación real de datos al actualizar desde la 1.25.x/RC;
- rendimiento real en gama de entrada y pantallas grandes.

## 10. Declaración

Esta migración se validó **únicamente** con compilación, tests JVM (244/244), lint, `aapt` y revisión de bytecode. **No se realizaron pruebas en ningún teléfono físico ni emulador.** El APK release quedó sin firmar y **no se hizo commit, tag, push ni release**; todo queda en el árbol de trabajo a la espera de la orden de Daniel.
