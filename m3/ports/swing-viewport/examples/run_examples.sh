#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
#
# Runs every M3 Swing viewport example headless with -Dm3.swing.viewport off and on, on a JDK 21 with the
# patched java.desktop, and writes <mode>-<off|on>.png plus examples.tsv to OUT.
# Usage: JAVA_HOME=<jdk21> PATCH=<patched java.desktop classes> OUT=<dir> bash run_examples.sh
set -u
J=${JAVA_HOME:?set JAVA_HOME to a JDK 21}/bin
PATCH=${PATCH:?set PATCH to the patched java.desktop classes}
OUT=${OUT:?set OUT to the output folder}
HERE=$(cd "$(dirname "$0")" && pwd)
mkdir -p "$OUT/classes"
rm -f "$OUT/examples.tsv"
"$J/javac" -J-Xmx256m -Xlint:all -d "$OUT/classes" "$HERE/M3SwingViewportDemo.java" || exit 3
for mode in tree-variable tree-fixed list; do
  for flag in false true; do
    "$J/java" -Djava.awt.headless=true -XX:+UseSerialGC -Xmx1500m -Xss4m \
      --patch-module java.desktop="$PATCH" -Dm3.swing.viewport=$flag \
      -cp "$OUT/classes" M3SwingViewportDemo $mode --capture "$OUT" || echo "failed: $mode $flag" >&2
  done
done
cat "$OUT/examples.tsv"
