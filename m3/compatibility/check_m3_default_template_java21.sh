#!/usr/bin/env bash
# JDK21 isolated progress subset; NOT an M3/OpenJDK build or jtreg gate.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
python3 "$ROOT/m3/compatibility/verify_m3_default_template_20261017.py"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$TMP/patch/java/util/concurrent" "$TMP/classes"
for name in Callable ProgressMonitor ProgressMonitors MonitoredCallable MonitoredRunnable; do
  cp "$ROOT/src/java.base/share/classes/java/util/concurrent/$name.java" \
     "$TMP/patch/java/util/concurrent/$name.java"
done
javac --patch-module java.base="$TMP/patch" -d "$TMP/classes" \
  "$TMP/patch/java/util/concurrent/"*.java
javac --patch-module java.base="$TMP/classes" -d "$TMP/classes" \
  "$ROOT/test/jdk/java/lang/Object/M3MonitoredTemplateTest.java"
java --patch-module java.base="$TMP/classes" -cp "$TMP/classes" M3MonitoredTemplateTest
echo "PASS bounded Java21 template-method and progress subset; full JDK gates pending"
