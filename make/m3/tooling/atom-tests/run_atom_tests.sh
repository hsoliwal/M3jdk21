#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
#
# run_atom_tests.sh <worktree> <out_dir> [test-class ...] : ATOM-TEST RUNNER v1 (no JDK build).
# Compiles the M3 sources of one java.base package against a stock JDK 21 with --patch-module (the
# patch path also lists the full M3 java.base source root of a read-only checkout so references to M3-only
# java.base API resolve; -implicit:none keeps every class but the package itself out of the patch), compiles
# the generated per-atom JUnit tests and runs them with the JUnit console launcher. Writes
# <out_dir>/reports/TEST-junit-jupiter.xml and prints a one-line summary per stage.
# Environment: JDK (default C:/Program Files/Java/jdk-21), JUNIT_JAR, M3_SRC_ROOT, PKG_DIR, TEST_DIR, JVM_OPTS.
set -euo pipefail
WT=${1:?worktree}
OUT=${2:?out_dir}
shift 2
JDK=${JDK:-"C:/Program Files/Java/jdk-21"}
JUNIT_JAR=${JUNIT_JAR:-"$(cygpath -m "$HOME")/.m2/repository/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar"}
M3_SRC_ROOT=${M3_SRC_ROOT:-"C:/m3-superset/vendored-atoms/m3jdk21-vendored/src/java.base/share/classes"}
PKG_DIR=${PKG_DIR:-"src/java.base/share/classes/jdk/internal/mindex"}
TEST_DIR=${TEST_DIR:-"test/jdk/jdk/internal/mindex/atoms"}
PKG=${PKG:-"jdk.internal.mindex"}
export MSYS_NO_PATHCONV=1
mkdir -p "$OUT/patch" "$OUT/tests" "$OUT/reports"

stamp=$(cat "$WT/$PKG_DIR"/*.java | sha256sum | cut -c1-16)
if [ ! -f "$OUT/patch/.stamp" ] || [ "$(cat "$OUT/patch/.stamp")" != "$stamp" ]; then
  rm -rf "$OUT/patch"; mkdir -p "$OUT/patch"
  "$JDK/bin/javac" -nowarn -implicit:none -XDignore.symbol.file \
    --patch-module "java.base=$WT/src/java.base/share/classes;$M3_SRC_ROOT" \
    -d "$OUT/patch" "$WT/$PKG_DIR"/*.java
  echo "$stamp" > "$OUT/patch/.stamp"
fi
echo "PATCH classes=$(find "$OUT/patch" -name '*.class' | wc -l) stamp=$stamp"

rm -rf "$OUT/tests"; mkdir -p "$OUT/tests"
"$JDK/bin/javac" -nowarn -Xlint:none -g \
  --patch-module "java.base=$OUT/patch" --add-exports "java.base/$PKG=ALL-UNNAMED" \
  -cp "$JUNIT_JAR" -d "$OUT/tests" "$WT/$TEST_DIR"/*.java
echo "TESTS classes=$(find "$OUT/tests" -name '*.class' | wc -l)"

select=()
if [ $# -eq 0 ]; then
  select=(--scan-classpath "$OUT/tests" --include-classname '.*AtomTest$')
else
  for c in "$@"; do select+=(--select-class "$c"); done
fi
set +e
"$JDK/bin/java" ${JVM_OPTS:-} -Djava.awt.headless=true \
  --patch-module "java.base=$OUT/patch" \
  --add-exports "java.base/$PKG=ALL-UNNAMED" --add-opens "java.base/$PKG=ALL-UNNAMED" \
  -jar "$JUNIT_JAR" execute --class-path "$OUT/tests" "${select[@]}" \
  --reports-dir "$OUT/reports" --details=summary --disable-banner 2>&1 | tee "$OUT/junit.log" | tail -40
status=${PIPESTATUS[0]}
set -e
echo "JUNIT exit=$status"
exit "$status"
