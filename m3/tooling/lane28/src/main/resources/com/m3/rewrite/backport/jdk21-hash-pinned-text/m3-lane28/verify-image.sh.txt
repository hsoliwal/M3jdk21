#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Linux image qualification. No patch-module: use the built fork's java.base.
set -euo pipefail
root=$(cd "$(dirname "$0")/../../.." && pwd)
: "${M3_TEST_JDK:?Set the absolute path of the freshly built fastdebug M3JDK image}"
image=$(cd "$M3_TEST_JDK" && pwd)
out="$root/m3/tooling/lane28/target/image"
mkdir -p "$out/classes" "$out/headers"
"$image/bin/java" -version > "$out/version.log" 2>&1
if ! grep -q fastdebug "$out/version.log"; then
  echo 'A matching fastdebug fork image is required' >&2
  exit 2
fi
testdir="$root/test/jdk/jdk/internal/mindex"
"$image/bin/javac" -source 21 -target 21 -proc:none -Xlint:all -Werror \
  --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED \
  -d "$out/classes" "$testdir/M3Lane28Test.java" "$testdir/M3Lane28Cost.java" \
  > "$out/javac.log" 2>&1
# The production symbol must be exported from this image's own libjava.
nm -D "$image/lib/libjava.so" > "$out/libjava-symbols.log"
grep -q Java_jdk_internal_mindex_M3BitLane28_cardinality0 "$out/libjava-symbols.log"
for mode in interpreter mixed c1 c2 m3-c1 m3-c2 m3-c2-nocompact; do
  flags=()
  compiler=''
  case "$mode" in
    interpreter) flags+=(-Xint);;
    c1|m3-c1) compiler=c1; flags+=(-XX:TieredStopAtLevel=1);;
    c2|m3-c2|m3-c2-nocompact) compiler=c2; flags+=(-XX:-TieredCompilation);;
  esac
  if [[ -n "$compiler" ]]; then
    flags+=(-Xbatch -XX:CompileThreshold=100 -XX:+UnlockDiagnosticVMOptions
      -XX:+LogCompilation "-XX:LogFile=$out/$mode-compilation.xml")
  fi
  if [[ "$mode" == m3-* ]]; then
    flags+=(-XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage)
  fi
  if [[ "$mode" == *-nocompact ]]; then flags+=(-XX:-CompactStrings); fi
  for main in M3Lane28Test; do
    runflags=("${flags[@]}")
    timeout 240 "$image/bin/java" -Xmx256m -ea -esa -Xcheck:jni \
      -XX:-CreateCoredumpOnCrash "${runflags[@]}" \
      --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED \
      --add-opens java.base/jdk.internal.mindex=ALL-UNNAMED -cp "$out/classes" "$main" \
      > "$out/$mode-$main.log" 2>&1
  done
  grep -q 'maintainedCounts=true' "$out/$mode-M3Lane28Test.log"
  if [[ -n "$compiler" ]]; then
    python3 - "$out/$mode-compilation.xml" "$compiler" <<'PY'
import pathlib, sys
lines = pathlib.Path(sys.argv[1]).read_text().splitlines()
assert any("<nmethod" in line and "compiler='" + sys.argv[2] + "'" in line
           and "method='jdk.internal.mindex.M3BitLane28 " in line for line in lines), \
    "No compiled Lane28 kernel evidence"
PY
  fi
done
echo 'PASS: bounded fork-image Lane28 and native peer tests in seven modes'
timeout 240 "$image/bin/java" -Xbatch -XX:-TieredCompilation -XX:CompileThreshold=100 \
  --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED \
  -cp "$out/classes" M3Lane28Cost > "$out/cost.log" 2>&1
