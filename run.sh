#!/usr/bin/env zsh
set -e

JAR="build/libs/WheresMyMoney-0.0.1-SNAPSHOT.jar"

#if [ ! -f "$JAR" ]; then
    echo "Building..."
    ./gradlew bootJar -q
#fi

exec java --enable-native-access=ALL-UNNAMED -jar "$JAR" "$@"
