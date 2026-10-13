#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Standalone build and test of m3/ports/swing-viewport (no Maven): javac --release 21 -Xlint:all,
# then the JUnit Platform console, headless. Usage:
#   run-tests.sh compile                 # main + test sources only
#   run-tests.sh test [junit selector]   # default: --scan-classpath
#   run-tests.sh scale <args...>         # M3SwingScale (heap from M3_SCALE_XMX, default 256m)
# Env: M3_JDK21 (JDK 21 home), JUNIT_JAR (junit-platform-console-standalone 1.11.x), OUT (build dir).
set -euo pipefail
here=$(cd "$(dirname "$0")" && pwd)
jdk=${M3_JDK21:-"/c/Program Files/Java/jdk-21"}
junit=${JUNIT_JAR:-"$HOME/.m2/repository/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar"}
out=${OUT:-"$here/target/standalone"}
sep=':'; case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) sep=';' ;; esac
win() { if command -v cygpath >/dev/null 2>&1; then cygpath -m "$1"; else printf '%s' "$1"; fi; }

compile() {
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
    "$jdk/bin/java" -Xmx512m -Djava.awt.headless=true -jar "$(win "$junit")" execute \
      -cp "$(win "$out/main")$sep$(win "$out/test")" --disable-banner --details=tree ${@:-"--scan-classpath"} ;;
  scale)
    shift
    "$jdk/bin/java" -Xmx${M3_SCALE_XMX:-256m} -XX:+UseSerialGC -Djava.awt.headless=true \
      -cp "$(win "$out/main")$sep$(win "$out/test")" com.m3.swing.viewport.scale.M3SwingScale "$@" ;;
  *) echo "usage: $0 compile|test|scale" >&2; exit 2 ;;
esac
