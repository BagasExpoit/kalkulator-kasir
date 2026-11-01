#!/bin/sh
# Gradle wrapper launcher. gradle-wrapper.jar diunduh otomatis jika belum ada.
APP_HOME=$(cd "$(dirname "$0")" && pwd -P)
JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
URL="https://raw.githubusercontent.com/gradle/gradle/v8.10.2/gradle/wrapper/gradle-wrapper.jar"

if [ ! -s "$JAR" ]; then
  echo "Mengunduh gradle-wrapper.jar ..."
  mkdir -p "$(dirname "$JAR")"
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL -o "$JAR" "$URL" || rm -f "$JAR"
  elif command -v wget >/dev/null 2>&1; then
    wget -q -O "$JAR" "$URL" || rm -f "$JAR"
  fi
  if [ ! -s "$JAR" ]; then
    echo "Gagal mengunduh gradle-wrapper.jar. Jalankan 'gradle wrapper' sekali, atau unduh manual dari $URL" >&2
    exit 1
  fi
fi

if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then JAVACMD="$JAVA_HOME/bin/java"; else JAVACMD=java; fi
exec "$JAVACMD" -Xmx64m -Xms64m "-Dorg.gradle.appname=gradlew" -classpath "$JAR" org.gradle.wrapper.GradleWrapperMain "$@"
