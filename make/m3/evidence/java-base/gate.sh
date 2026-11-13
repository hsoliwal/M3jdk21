#!/bin/bash
# gate_jb.sh <root> <out> <junit-console.jar> : java.base jdk.internal.m3 + jdk.internal.m3.hash on a host JDK 21, in the
# style of make/m3/test-local.sh and the m3 workflows. Product sources compile as a java.base patch (-implicit:none,
# JNI headers by -h); the libjava M3 JNI sources build as standalone libraries against those headers; tests:
#   - files declaring package jdk.internal.m3 are patched into java.base (source root test/jdk) and run by FQN;
#   - default-package files of each test directory compile together with the exports and JUnit on the class path;
#   every main runs, every JUnit class runs through the console launcher. One RUN/JUNIT line per test, so two trees
# can be compared after normalising class names.
set -u
R=$1; O=$2; JU=$3; rm -rf "$O"; mkdir -p "$O/classes" "$O/ptests" "$O/tests" "$O/native" "$O/headers"
JH=$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")
B=$R/src/java.base/share/classes
EXP=(--add-exports java.base/jdk.internal.m3=ALL-UNNAMED --add-exports java.base/jdk.internal.m3.hash=ALL-UNNAMED
     --add-exports java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED)
javac -implicit:none -Xlint:all,-serial --patch-module java.base="$B" -h "$O/headers" -d "$O/classes" \
  $(ls "$B"/jdk/internal/m3/*.java "$B"/jdk/internal/m3/hash/*.java "$B"/java/util/CollectionViews.java "$B"/java/util/concurrent/ProgressMonitor.java) > "$O/javac_main.log" 2>&1
echo "javac main rc=$? warnings=$(grep -c 'warning:' "$O/javac_main.log")"
for f in "$R"/src/java.base/share/native/libjava/*.c; do
  c=$(basename "$f" .c); grep -q "Java_jdk_internal_m3_" "$f" || continue
  cc -std=c11 -Wall -Wextra -Werror -fPIC -shared -I"$JH/include" -I"$JH/include/linux" -I"$O/headers" \
     -I"$R/src/java.base/share/native/libjava" -I"$R/src/java.base/unix/native/libjava" \
     -o "$O/native/lib$c.so" "$f" > "$O/native/$c.log" 2>&1
  echo "native $c rc=$?"
done
MET=$(ls "$O"/native/lib*ObjectMetrics.so 2>/dev/null | head -1)
WIN=$(ls "$O"/native/lib*IndexMaskWindow.so 2>/dev/null | head -1)
PROPS=(-Djdk.m3.native.path="$MET" -Dm3.mask.window.library="$WIN")
T0=$R/test/jdk/jdk/internal/m3
P=$(grep -l "^package jdk.internal.m3;" "$T0"/*.java 2>/dev/null | sort)
if [ -n "$P" ]; then
  javac -implicit:none --patch-module java.base="$O/classes:$R/test/jdk" -d "$O/ptests" $P > "$O/javac_ptests.log" 2>&1
  echo "javac patched tests rc=$?"
  for f in $P; do cls=jdk.internal.m3.$(basename "$f" .java); grep -q "static void main" "$f" || continue
    timeout 300 java --patch-module java.base="$O/classes:$O/ptests" "${PROPS[@]}" "$cls" > "$O/run_p_$(basename "$f").log" 2>&1
    echo "RUN patched/$(basename "$f" .java) rc=$? $(tail -1 "$O/run_p_$(basename "$f").log" | cut -c1-100)"; done
fi
for d in "" atoms hash io; do
  T=$R/test/jdk/jdk/internal/m3/$d; [ -d "$T" ] || continue
  F=$(grep -L "^package " "$T"/*.java | sort); [ -n "$F" ] || continue
  mkdir -p "$O/tests/$d"
  javac --patch-module java.base="$O/classes" "${EXP[@]}" -cp "$JU" -d "$O/tests/$d" $F > "$O/javac_test_${d:-root}.log" 2>&1
  echo "javac tests ${d:-root} rc=$?"
  RUN=(java --patch-module java.base="$O/classes" "${EXP[@]}" "${PROPS[@]}")
  for f in $F; do cls=$(basename "$f" .java)
    if grep -q "@Test" "$f"; then
      timeout 300 "${RUN[@]}" -cp "$O/tests/$d:$JU" org.junit.platform.console.ConsoleLauncher execute --select-class "$cls" \
        --disable-banner --details=summary > "$O/junit_${d:-root}_$cls.log" 2>&1
      echo "JUNIT ${d:-root}/$cls rc=$? $(grep -E 'tests (successful|failed)' "$O/junit_${d:-root}_$cls.log" | tr -s ' []' ' ' | tr '\n' ' ')"
    elif grep -q "static void main" "$f"; then
      timeout 300 "${RUN[@]}" -cp "$O/tests/$d" "$cls" > "$O/run_${d:-root}_$cls.log" 2>&1
      echo "RUN ${d:-root}/$cls rc=$? $(tail -1 "$O/run_${d:-root}_$cls.log" | cut -c1-100)"
    fi
  done
done
