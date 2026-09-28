#!/bin/bash
# Build helper para proyectos Android en este sandbox.
# Uso: ./build-android.sh [tarea-gradle...]  (por defecto: assembleDebug)
set -e
unset http_proxy https_proxy HTTP_PROXY HTTPS_PROXY no_proxy NO_PROXY ALL_PROXY all_proxy
export JAVA_HOME="$HOME/workspace/android-sdk/jdk17"
export ANDROID_HOME="$HOME/workspace/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export GRADLE_USER_HOME="$HOME/workspace/gradle-home"
# El cliente Gradle necesita IPv4 para el handshake con su daemon (localhost);
# el daemon NO debe llevarlo (necesita IPv6 para salir por el proxy).
export GRADLE_OPTS="-Djava.net.preferIPv4Stack=true -Djavax.net.ssl.trustStore=$HOME/workspace/android-sdk/cacerts-proxy -Djavax.net.ssl.trustStorePassword=changeit"
export PATH="$JAVA_HOME/bin:$HOME/workspace/android-sdk/gradle-8.7/bin:$PATH"
cd "$HOME/workspace/apps/ahora"
gradle "${@:-assembleDebug}" --console=plain
