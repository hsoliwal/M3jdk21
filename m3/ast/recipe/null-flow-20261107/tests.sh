#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gate 2 of recipe null-flow-20261107: the module's test tree, compiled as #2573 (m3/ast-syntax-index-20261106a) did,
# because m3/ast/pom.xml cannot testCompile on master: the pre-existing M3SyntaxGrowthAgent needs
# --add-exports java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED, which javac refuses together with --release
# (failing set = baseline; fixing it is a separate leaf). So: javac -source 21 -target 21 -proc:none
# -Xlint:all,-options -Werror --add-exports ... over src/test/java against target/classes (the Maven main build) and the
# JUnit console launcher 1.11.4 jar, then the launcher over every class of both directories. Prints TESTS PASS|FAIL
# with the launcher's counts; exits 1 on a failure or when nothing was found.
# usage: tests.sh <scratch-dir>     env: JAVA_HOME (a JDK 21), JUNIT_CONSOLE (junit-platform-console-standalone-1.11.4.jar)
set -euo pipefail
RECIPE_DIR=$(cd "$(dirname "$0")" && pwd)
MODULE=$(cd "$RECIPE_DIR/../.." && pwd)
SCRATCH=$1
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
CONSOLE="${JUNIT_CONSOLE:?JUNIT_CONSOLE must name junit-platform-console-standalone-1.11.4.jar}"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; w() { cygpath -m "$1"; } ;; *) SEP=':'; w() { printf '%s' "$1"; } ;; esac
[ -d "$MODULE/target/classes" ] || { echo "TESTS FAIL: run mvn -f m3/ast/pom.xml -Dmaven.test.skip=true package first"; exit 1; }
rm -rf "$SCRATCH/test-classes"
mkdir -p "$SCRATCH/test-classes"
mapfile -t sources < <(find "$MODULE/src/test/java" -name '*.java' | sort)
"$JAVA_BIN/javac" -source 21 -target 21 -proc:none -Xlint:all,-options -Werror \
    --add-exports java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED \
    -cp "$(w "$MODULE/target/classes")$SEP$(w "$CONSOLE")" -d "$(w "$SCRATCH/test-classes")" "${sources[@]}"
echo "TESTS compiled ${#sources[@]} test sources into $SCRATCH/test-classes"
"$JAVA_BIN/java" -jar "$(w "$CONSOLE")" execute --class-path "$(w "$MODULE/target/classes")$SEP$(w "$SCRATCH/test-classes")" \
    --scan-class-path --details=summary --disable-banner > "$SCRATCH/tests.txt" 2>&1 || true
found=$(grep -oE '[0-9]+ tests found' "$SCRATCH/tests.txt" | grep -oE '^[0-9]+' || echo 0)
ok=$(grep -oE '[0-9]+ tests successful' "$SCRATCH/tests.txt" | grep -oE '^[0-9]+' || echo 0)
failed=$(grep -oE '[0-9]+ tests failed' "$SCRATCH/tests.txt" | grep -oE '^[0-9]+' || echo 0)
if [ "${found:-0}" -gt 0 ] && [ "${failed:-0}" -eq 0 ]; then
    echo "TESTS PASS found=$found successful=$ok failed=$failed (log $SCRATCH/tests.txt)"
else
    echo "TESTS FAIL found=$found successful=$ok failed=$failed (log $SCRATCH/tests.txt)"
    grep -E 'MethodSource|=>|Exception|Error' "$SCRATCH/tests.txt" | head -n 20
    exit 1
fi
