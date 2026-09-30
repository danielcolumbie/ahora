# IzzyOnDroid — paquete de solicitud listo

> ⏳ **CONDICIÓN DE DANIEL (2026-09-29): NO ENVIAR todavía.** La publicación en tiendas va DESPUÉS de que termine el rediseño visual de la app. Cuando Daniel confirme que el rediseño está terminado, se publica un release final y ANTES DE ENVIAR se actualizan en el texto de abajo: número de versión, versionCode, URL del APK y tamaño con los del release final. Nada se envía sin su confirmación.

## Estado de los metadatos fastlane (verificado 2026-09-29)

`fastlane/metadata/android/` — completo para la solicitud:

| Recurso | en-US | es |
|---|---|---|
| title.txt | ✓ «Ahora» | ✓ «Ahora» |
| short_description.txt | ✓ | ✓ |
| full_description.txt | ✓ | ✓ |
| changelogs/28.txt | ✓ | ✓ |
| images/icon.png (512×512) | ✓ | — |

Screenshots: IzzyOnDroid no los exige para aceptar la app; se agregarán después del rediseño si se quieren.

## Release verificado en GitHub (ejemplo — reemplazar por el final)

- Tag: `v1.26.0` — «Ahora 1.26.0 — migración a SDK 36»
- APK firmado: `ahora-1.26.0-sdk36.apk`
- Tamaño: 1.740.926 bytes (~1,7 MB)
- URL: https://github.com/danielcolumbie/ahora/releases/download/v1.26.0/ahora-1.26.0-sdk36.apk
- Package ID: `com.ahora.app` · versionCode 28 · licencia MIT · sin rastreadores · sin permiso de internet

## Texto de la solicitud (listo para pegar)

**Título del issue:**
```
[AppRequest] Ahora
```

**Cuerpo:**
```markdown
## App request: Ahora

- **App name:** Ahora
- **Package name:** `com.ahora.app`
- **Source code:** https://github.com/danielcolumbie/ahora
- **License:** MIT
- **Latest version:** 1.26.0 (versionCode 28)  ← actualizar con el release final post-rediseño
- **APK:** https://github.com/danielcolumbie/ahora/releases/download/v1.26.0/ahora-1.26.0-sdk36.apk (~1.7 MB)  ← actualizar
- **Category:** Productivity
- **Summary:** Sácalo de tu cabeza. Tus tareas, claras y sin ruido. / Get it out of your head. Your tasks, clear and noise-free.
- **Description:** App de tareas minimalista, 100% offline: sin cuentas, sin publicidad, sin rastreo. Tus datos nunca salen de tu teléfono (la app ni siquiera tiene permiso de internet).
- **Fastlane metadata:** presente en el repo (`fastlane/metadata/android/`, en-US + es)
- **Build instructions:** build estándar de Gradle: `./gradlew assembleRelease` con Android SDK 36 y JDK 17. Sin dependencias privadas ni binarios precompilados.

## AI tools usage (declaración honesta)

La app se desarrolló con asistencia de IA (Muse, de Meta) como herramienta de producción, bajo dirección y revisión humana completa en cada paso: el dueño del proyecto (Daniel Columbié) define cada funcionalidad, revisa cada cambio y aprueba cada versión antes de publicarla. Todo el código del repositorio fue revisado por un humano; la IA no publica nada por su cuenta.

## Guidelines

- [x] Soy el desarrollador / publico en su nombre con autorización
- [x] La app cumple la App Inclusion Policy
- [x] No está ya listada en IzzyOnDroid
- [x] La carpeta Fastlane está presente en el repo
```

## Guía para Daniel (3 pasos, desde su teléfono)

1. Crea una cuenta en **codeberg.org** (es gratis, solo email y usuario).
2. Entra a **https://codeberg.org/IzzyOnDroid/repodata/issues** → «New Issue» → elige la plantilla **«App Inclusion Request»**.
3. Pega el texto de arriba (con la versión/URL/tamaño ACTUALIZADOS al release final post-rediseño) y envíalo. Suelen responder en 1–3 días; si piden algo, me lo pasas y lo resolvemos.

⏳ Recuerda: el paso 3 solo cuando me confirmes que el rediseño está terminado y publiquemos el release final.
