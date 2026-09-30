# Firma de distribución — Ahora (1.26.0)

## Qué se generó

- **Archivo:** `~/workspace/secure/ahora-release.keystore` (FUERA del repositorio git)
- **Alias:** `ahora-release`
- **Algoritmo:** RSA de 3072 bits, firma SHA256withRSA
- **Validez:** 30 años (2026-09-29 → 2056-09-21). Google exige que la clave
  siga válida más allá de 2033; con 30 años hay margen de sobra.
- **Contraseñas actuales:** las de Daniel (cambiadas por él el 2026-09-29;
  no están registradas en ningún archivo). El keystore es PKCS12: la
  contraseña del keystore y la de la clave son la misma.

Esta es la clave de distribución de Ahora. **La misma clave debe firmar
todas las futuras versiones**: si se pierde o se genera otra, Google Play
y Android tratarán la app como una app distinta y los usuarios no podrán
actualizar sin desinstalar (perderían sus tareas).

## Cómo la usa Gradle

`app/build.gradle.kts` configura la firma release solo con variables de
entorno (nunca hay secretos en el repo):

```bash
export AHORA_KEYSTORE_PATH="$HOME/workspace/secure/ahora-release.keystore"
export AHORA_KEYSTORE_PASSWORD="tu-contraseña-del-keystore"
export AHORA_KEY_ALIAS="ahora-release"
export AHORA_KEY_PASSWORD="tu-contraseña-de-la-clave"
./gradlew assembleRelease bundleRelease
```

Sin esas variables, la release se firma con la clave de debug (como antes).

## Contraseña (ya configurada por Daniel el 2026-09-29)

Daniel ya puso su propia contraseña. No está escrita en ningún archivo:
guárdala tú en un lugar seguro junto con el respaldo del keystore.
El keystore es PKCS12, así que la contraseña del keystore y la de la clave
son la misma (no se pueden separar en este formato).

## Respaldo (obligatorio)

Guarda en un lugar SEGURO y APARTE de este ordenador (un USB, otro disco,
un gestor de contraseñas):

1. El archivo `ahora-release.keystore`
2. El alias: `ahora-release`
3. La contraseña del keystore
4. La contraseña de la clave
5. Este documento (para saber qué es cada cosa dentro de años)

PROHIBIDO:
- Subir el keystore a GitHub (el `.gitignore` ya ignora `*.jks`/`*.keystore`).
- Incluirlo en ningún APK, AAB o archivo público.
- Guardar la única copia en el ordenador de trabajo.
- Escribir las contraseñas reales en el repositorio o en este documento.

## Verificar la firma de un APK/AAB

```bash
~/workspace/android-sdk/build-tools/*/apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
~/workspace/android-sdk/jdk17/bin/keytool -printcert -jarfile app/build/outputs/bundle/release/app-release.aab
```

El SHA-256 del certificado debe coincidir con el de tu keystore:

```bash
~/workspace/android-sdk/jdk17/bin/keytool -list -v \
  -keystore ~/workspace/secure/ahora-release.keystore | grep SHA256
```

## Nota sobre F-Droid

F-Droid firma con su propia clave, así que este keystore no le afecta.
