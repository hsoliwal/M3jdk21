#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Tests of M3DB (java.base's jdk.m3.db and jdk.internal.m3.db) on a host JDK 21, modelled on
# make/m3/compression/test-java-base.sh and make/m3/intern/test-java-base.sh: the 21 product sources are staged
# alone and compiled into a java.base patch with the flags of the Maven module m3/indexdb/pom.xml
# (-Xlint:all -Werror); the tests share the package jdk.m3.db (they use package-private types), so they are
# patched in too, with JUnit on the boot class path and the two hash-family fixtures beside them (they are read with
# Class.getResource). Runs every JUnit class, then the build-state contract main that
# m3-indexdb-build-state-20261020.yml runs on the class path.
# usage: test-java-base.sh <junit-platform-console-standalone.jar> [work-dir]
set -euo pipefail
root=$(cd "$(dirname "$0")/../../.." && pwd)
junit=$1; work=${2:-$(mktemp -d)}
pkg=jdk.m3.db
classes=$root/src/java.base/share/classes
test_src=$root/test/jdk/jdk/m3/db
# Git Bash on Windows: the JDK takes Windows paths and ';' between patch directories.
sep=:; native() { printf '%s' "$1"; }
if command -v cygpath > /dev/null; then sep=';'; native() { cygpath -m "$1"; }; fi
rm -rf "$work"
mkdir -p "$work/src/jdk/m3" "$work/src/jdk/internal/m3" "$work/tsrc/jdk/m3/db" "$work/main" "$work/test"
# The patch source root holds only the product packages, so upstream classes resolve from the host JDK.
cp -R "$classes/jdk/m3/db" "$work/src/jdk/m3/"
cp -R "$classes/jdk/internal/m3/db" "$work/src/jdk/internal/m3/"
cp "$test_src"/*.java "$work/tsrc/jdk/m3/db/"
# test scope only: DbAtomTest reads java.lang.management (module java.management, a boot module)
reads=(--add-reads java.base=ALL-UNNAMED,java.management --add-modules java.management)
javac -Xlint:all -Werror --patch-module java.base="$(native "$work/src")" -d "$(native "$work/main")" \
  "$work/src/jdk/m3/db"/*.java "$work/src/jdk/internal/m3/db"/*.java
javac --patch-module java.base="$(native "$work/main")$sep$(native "$work/tsrc")" "${reads[@]}" \
  -cp "$(native "$junit")" -d "$(native "$work/test")" "$work/tsrc/jdk/m3/db"/*.java
cp -R "$test_src/resources/." "$work/test/"
opts=(-Xbootclasspath/a:"$(native "$junit")"
      --patch-module java.base="$(native "$work/main")$sep$(native "$work/test")"
      "${reads[@]}" --add-opens java.base/$pkg=ALL-UNNAMED --add-opens java.base/jdk.internal.m3.db=ALL-UNNAMED)
# The options travel in JDK_JAVA_OPTIONS, so the child JVMs that DbParseTest and DbAtomTest start
# (java -cp <class path> jdk.m3.db.DbProbe / DbDigest) load the same patched java.base.
export JDK_JAVA_OPTIONS="${opts[*]}"
run=(java -cp "$(native "$junit")")
junit_classes=$(cd "$test_src" && grep -l "@Test" *.java | sed "s/\.java$//; s/^/--select-class=$pkg./")
# shellcheck disable=SC2086
"${run[@]}" org.junit.platform.console.ConsoleLauncher execute $junit_classes \
  --disable-banner --details=summary --fail-if-no-tests
"${run[@]}" $pkg.BuildStateContract > "$work/BuildStateContract.log"
echo "M3DB_MAIN BuildStateContract PASS $(tail -1 "$work/BuildStateContract.log" | cut -c1-100)"
echo "M3DB_JAVA_BASE_TESTS PASS"
