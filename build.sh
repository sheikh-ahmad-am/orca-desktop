#!/bin/bash
# Orca Desktop — reproducible build (run on the Linux build VM).
#
# Produces dist/ (Orca.exe + orca-desktop.jar + jre/) and
# ./Orca-Desktop.zip.
#
# One-time prerequisites (already fetched under downloads/ if present):
#   - JDK 17 at ~/jdk17
#   - downloads/sqlite-jdbc.jar   (org.xerial:sqlite-jdbc:3.36.0.3, Maven Central)
#   - downloads/jre-win64.zip     (Temurin JRE 17 Windows x64, Adoptium)
#   - mingw-w64 installed         (x86_64-w64-mingw32-gcc + windres)
set -e
cd "$(dirname "$0")"
JAVAC=~/jdk17/bin/javac
JAR=~/jdk17/bin/jar
JAVA=~/jdk17/bin/java

echo "== compile app (Engine.java + Problems.java are verbatim copies of the APK sources) =="
rm -rf build/classes build/testclasses
mkdir -p build/classes build/testclasses
$JAVAC -encoding UTF-8 -cp downloads/sqlite-jdbc.jar -d build/classes $(find src -name '*.java')

echo "== verification harnesses (47 audit cases + 40 desktop cases) =="
$JAVAC -encoding UTF-8 -cp build/classes:downloads/sqlite-jdbc.jar -d build/testclasses $(find testharness -name '*.java')
$JAVA -cp build/classes:build/testclasses:downloads/sqlite-jdbc.jar com.ahmad.sqlchallenge.Harness2 | tail -2
$JAVA -cp build/classes:build/testclasses:downloads/sqlite-jdbc.jar com.ahmad.sqlchallenge.Harness3 "$PWD/build/harness-data" | tail -2

echo "== fat jar (sqlite-jdbc shaded; natives kept: Windows x86_64 + Linux x86_64 for tests) =="
rm -rf build/fatjar build/orca-desktop.jar
mkdir -p build/fatjar
(cd build/fatjar && $JAR xf ../../downloads/sqlite-jdbc.jar \
  && rm -f META-INF/*.SF META-INF/*.DSA META-INF/*.RSA \
  && find org/sqlite/native -mindepth 2 -maxdepth 2 -type d \
       ! -path 'org/sqlite/native/Windows/x86_64' ! -path 'org/sqlite/native/Linux/x86_64' \
       -exec rm -rf {} + 2>/dev/null || true)
cp -r build/classes/* build/fatjar/
cp icon-orca.png build/fatjar/icon-orca.png
printf 'Main-Class: desktop.OrcaApp\n' > build/manifest.mf
(cd build/fatjar && $JAR cfm ../orca-desktop.jar ../manifest.mf .)

echo "== launcher exe (MinGW cross-compile, icon + version info embedded) =="
(cd launcher && x86_64-w64-mingw32-windres orca.rc -O coff -o orca.res \
  && x86_64-w64-mingw32-gcc -mwindows -static -O2 -o Orca.exe launcher.c orca.res)
file launcher/Orca.exe

echo "== assemble dist + zip =="
rm -rf dist downloads/jre-extract
mkdir -p dist downloads/jre-extract
cp launcher/Orca.exe build/orca-desktop.jar dist/
(cd downloads/jre-extract && unzip -q ../jre-win64.zip)
cp -r downloads/jre-extract/*/ dist/jre
cp dist-readme.txt dist/Orca-README.txt 2>/dev/null || true
rm -f ./Orca-Desktop.zip
(cd dist && zip -q -9 -r ./Orca-Desktop.zip .)
ls -l ./Orca-Desktop.zip
sha256sum ./Orca-Desktop.zip
echo "BUILD OK"
