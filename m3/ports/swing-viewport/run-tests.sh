#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Standalone, self-contained build and test of m3/ports/swing-viewport (no Maven, no ~/.m2, nothing outside the
# M3JDK 21 repository but the JDK): javac --release 21 -Xlint:all, then the JUnit Platform and Jupiter engine
# compiled once from the vendored sources in m3/vendor/third_party, run headless by the port's own launcher
# jdk.m3.swing.testkit.M3JUnitLauncher. Usage:
#   run-tests.sh compile                    # vendored JUnit (once) + main + test sources
#   run-tests.sh test [launcher selectors]  # default: --scan <test classes>; or --select-package <name> ...
#   run-tests.sh scale <args...>            # M3SwingScale (heap from M3_SCALE_XMX, default 256m)
# Env: M3JDK 21 (JDK 21 home), OUT (build dir), JUNIT_OUT (vendored JUnit classes, default $OUT.junit).
set -euo pipefail
here=$(cd "$(dirname "$0")" && pwd)
vendor=$(cd "$here/../../vendor/third_party" && pwd)
jdk=${M3JDK21:-"/c/Program Files/Java/jdk-21"}
out=${OUT:-"$here/target/standalone"}
junit=${JUNIT_OUT:-"$out.junit"}
sep=':'; case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) sep=';' ;; esac
win() { if command -v cygpath >/dev/null 2>&1; then cygpath -m "$1"; else printf '%s' "$1"; fi; }
# JUnit Platform 1.10.2 + Jupiter 5.10.2 (the family M3JDK 21 keeps, M3jdk21 PR 2199) and their API dependencies, as vendored sources (classpath mode).
JUNIT_SOURCES="opentest4j apiguardian-api junit-platform-commons-1.10.2 junit-platform-engine-1.10.2
  junit-platform-launcher-1.10.2 junit-jupiter-api-5.10.2 junit-jupiter-engine-5.10.2"

vendored_junit() {
  [ -f "$junit/.complete" ] && return 0
  rm -rf "$junit"; mkdir -p "$junit"
  for d in $JUNIT_SOURCES; do
    [ -d "$vendor/$d/src/main/java" ] || { echo "missing vendored source: m3/vendor/third_party/$d" >&2; exit 3; }
    find "$vendor/$d/src/main/java" -name '*.java' ! -name module-info.java
  done | while read -r f; do win "$f"; echo; done > "$junit.txt"
  # JUnit 5.10 targets Java 8: its ObjectInputStream.GetField uses predate the JDK 21 throws clause, hence --release 17
  "$jdk/bin/javac" -J-Xmx768m --release 17 -nowarn -encoding UTF-8 -d "$(win "$junit")" @"$(win "$junit.txt")"
  for d in $JUNIT_SOURCES; do
    if [ -d "$vendor/$d/src/main/resources" ]; then cp -r "$vendor/$d/src/main/resources/." "$junit/"; fi
  done
  touch "$junit/.complete"
}

compile() {
  vendored_junit
  rm -rf "$out"; mkdir -p "$out/main" "$out/test"
  find "$here/src/main/java" -name '*.java' | while read -r f; do win "$f"; echo; done > "$out/main.txt"
  "$jdk/bin/javac" -J-Xmx512m --release 21 -Xlint:all,-serial,-this-escape -Werror -d "$(win "$out/main")" @"$(win "$out/main.txt")"
  if [ -d "$here/src/test/java" ]; then
    find "$here/src/test/java" -name '*.java' | while read -r f; do win "$f"; echo; done > "$out/test.txt"
    "$jdk/bin/javac" -J-Xmx512m --release 21 -Xlint:all,-serial,-try,-this-escape -d "$(win "$out/test")" \
      -cp "$(win "$out/main")$sep$(win "$junit")" @"$(win "$out/test.txt")"
  fi
  echo "COMPILED main=$(find "$out/main" -name '*.class' | wc -l) test=$(find "$out/test" -name '*.class' 2>/dev/null | wc -l)"
}

case "${1:-test}" in
  compile) compile ;;
  test)
    compile; shift || true
    if [ $# -eq 0 ]; then set -- --scan "$(win "$out/test")"; fi
    "$jdk/bin/java" -Xmx768m -Djava.awt.headless=true -cp "$(win "$out/main")$sep$(win "$out/test")$sep$(win "$junit")" \
      jdk.m3.swing.testkit.M3JUnitLauncher "$@" ;;
  scale)
    shift
    "$jdk/bin/java" -Xmx${M3_SCALE_XMX:-256m} -XX:+UseSerialGC -Djava.awt.headless=true \
      -cp "$(win "$out/main")$sep$(win "$out/test")" jdk.m3.swing.viewport.scale.M3SwingScale "$@" ;;
  *) echo "usage: $0 compile|test|scale" >&2; exit 2 ;;
esac
