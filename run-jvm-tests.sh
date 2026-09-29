#!/bin/bash
# Corre los tests JVM de la app "Ahora" sin usar el worker roto de Gradle
# (testDebugUnitTest da NPE en SuiteTestClassProcessor en este entorno).
# Usa JUnitCore directo sobre las clases compiladas, como se documentó en MEJORAS.md.
#
# Uso: ./run-jvm-tests.sh
# Sale con código 0 si todos los tests pasan, 1 si alguno falla.
set -e
set -o pipefail
unset http_proxy https_proxy HTTP_PROXY HTTPS_PROXY no_proxy NO_PROXY ALL_PROXY all_proxy
export JAVA_HOME="$HOME/workspace/android-sdk/jdk17"
export ANDROID_HOME="$HOME/workspace/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export GRADLE_USER_HOME="$HOME/workspace/gradle-home"
export GRADLE_OPTS="-Djava.net.preferIPv4Stack=true -Djavax.net.ssl.trustStore=$HOME/workspace/android-sdk/cacerts-proxy -Djavax.net.ssl.trustStorePassword=changeit"
export PATH="$JAVA_HOME/bin:$HOME/workspace/android-sdk/gradle-8.7/bin:$PATH"
cd "$HOME/workspace/apps/ahora"

echo "== Compilando clases main + test =="
gradle compileDebugUnitTestKotlin --console=plain -q

echo "== Resolviendo debugUnitTestRuntimeClasspath =="
cat > .test-work-print-cp.init.gradle <<'INIT_EOF'
gradle.projectsLoaded {
    def app = gradle.rootProject.project(':app')
    app.tasks.register('printTestCp') {
        doLast {
            def files = app.configurations.debugUnitTestRuntimeClasspath.files
            new File(app.projectDir, '../.test-work-cp.txt').text =
                files.collect { it.absolutePath }.join('\n')
        }
    }
}
INIT_EOF
gradle -I .test-work-print-cp.init.gradle printTestCp --console=plain -q
rm -f .test-work-print-cp.init.gradle

WORK="$HOME/workspace/apps/ahora/.test-work"
mkdir -p "$WORK/aar-classes"
MAIN_CLASSES="app/build/tmp/kotlin-classes/debug"
TEST_CLASSES="app/build/tmp/kotlin-classes/debugUnitTest"
[ -d "$TEST_CLASSES" ] || { echo "ERROR: no existen clases de test compiladas en $TEST_CLASSES"; exit 1; }

# Clases frescas PRIMERO (excluye el classes.jar viejo de runtime_app_classes_jar).
CP="$TEST_CLASSES:$MAIN_CLASSES"
while IFS= read -r f || [ -n "$f" ]; do
  case "$f" in
    *runtime_app_classes_jar*) continue ;;
    *.jar) CP="$CP:$f" ;;
    *.aar)
      key=$(printf '%s' "$f" | md5sum | cut -d' ' -f1)
      dest="$WORK/aar-classes/$key"
      if [ ! -f "$dest/classes.jar" ] || [ "$f" -nt "$dest/classes.jar" ]; then
        rm -rf "$dest"; mkdir -p "$dest"
        unzip -q -o "$f" classes.jar -d "$dest" 2>/dev/null || true
      fi
      [ -f "$dest/classes.jar" ] && CP="$CP:$dest/classes.jar"
      ;;
  esac
done < .test-work-cp.txt
rm -f .test-work-cp.txt

# android.jar AL FINAL: no debe opacar el org.json:json real con sus stubs.
ANDROID_JAR=$(ls -d "$ANDROID_HOME"/platforms/android-*/android.jar 2>/dev/null | sort -V | tail -1)
[ -n "$ANDROID_JAR" ] || { echo "ERROR: no se encontró android.jar"; exit 1; }
CP="$CP:$ANDROID_JAR"

cd "$TEST_CLASSES"
CLASSES=$(find . -name '*Test.class' ! -name '*$*' | sed 's|^\./||; s|\.class$||; s|/|.|g' | sort)
cd "$OLDPWD"
N=$(echo "$CLASSES" | grep -c . || true)
echo "== Ejecutando $N clases de test con JUnitCore =="
echo "$CLASSES" | sed 's/^/   /'
# shellcheck disable=SC2086
"$JAVA_HOME/bin/java" -cp "$CP" org.junit.runner.JUnitCore $CLASSES 2>&1 | tee "$WORK/last-run.log" | tail -25
