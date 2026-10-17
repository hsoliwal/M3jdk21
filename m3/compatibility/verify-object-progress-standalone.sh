#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
python3 "$ROOT/m3/compatibility/check_object_adapter_progress_receiver_20261017.py"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$TMP/patch/java/util/concurrent" "$TMP/classes"
cp "$ROOT/src/java.base/share/classes/java/util/concurrent/ProgressMonitor.java" "$TMP/patch/java/util/concurrent/"
cp "$ROOT/src/java.base/share/classes/java/util/concurrent/ProgressMonitors.java" "$TMP/patch/java/util/concurrent/"
javac --patch-module java.base="$TMP/patch" -d "$TMP/classes" \
    "$TMP/patch/java/util/concurrent/ProgressMonitor.java" \
    "$TMP/patch/java/util/concurrent/ProgressMonitors.java"
javac --patch-module java.base="$TMP/classes" -d "$TMP/classes" \
    "$ROOT/m3/compatibility/M3ObjectProgressStandaloneProbe.java"
java --patch-module java.base="$TMP/classes" -cp "$TMP/classes" M3ObjectProgressStandaloneProbe
