#!/bin/bash
# gate_module.sh <tree> <out> <module dir> : a module on a host JDK 21 as its pom builds it (release 21, -Xlint:all -Werror,
# class path), with JNI headers by javac -h; then the JUnit console; then every C source under src/*/native compiled
# with -Werror against those headers, and every Java_ symbol a C file defines checked against a generated header
# declaration. One line per step, so the tree before the rename and the tree after can be compared.
set -u
T=$1; O=$2; MOD=$3; rm -rf "$O"; mkdir -p "$O/main" "$O/test" "$O/h" "$O/native"
M2=/mnt/s/m3-scratch/eb-conv/m2
JU=$M2/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar
JH=$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")
M=$T/$3; DSP=/mnt/s/m3-scratch/converge-20261106/srch/deps/m3/collections/src/main/java:/mnt/s/m3-scratch/converge-20261106/srch/deps/m3/algorithms/src/main/java
javac --release 21 -Xlint:all -Werror -proc:none -sourcepath "$DSP" -h "$O/h" -d "$O/main" \
  $(find "$M/src/main/java" -name '*.java' ! -name module-info.java) > "$O/javac_main.log" 2>&1
echo "javac main rc=$? classes=$(find "$O/main" -name '*.class' | wc -l) headers=$(ls "$O/h" | wc -l)"
javac --release 21 -Xlint:all -Werror -proc:none -cp "$O/main:$JU" -sourcepath "$DSP" -h "$O/h" -d "$O/test" \
  $(find "$M/src/test/java" -name '*.java') > "$O/javac_test.log" 2>&1
echo "javac test rc=$? classes=$(find "$O/test" -name '*.class' | wc -l)"
(cd "$M" && timeout 1800 java -cp "$O/test:$O/main:$JU" org.junit.platform.console.ConsoleLauncher execute \
  --scan-class-path "$O/test" --disable-banner --details=tree --disable-ansi-colors > "$O/junit.log" 2>&1)
echo "junit rc=$? $(grep -E 'tests (found|successful|failed|skipped|aborted)' "$O/junit.log" | tr -s ' []' ' ' | tr '\n' ' ')"
for c in $(find "$M/src" -path '*/native/*' -name '*.c' | sort); do
  n=$(basename "$c" .c)
  cc -std=c11 -Wall -Wextra -Werror -fPIC -shared -I"$JH/include" -I"$JH/include/linux" -I"$O/h" -I"$(dirname "$c")" -I"$M/include" \
     -o "$O/native/lib$n.so" "$c" > "$O/native/$n.log" 2>&1
  rc=$?
  missing=0
  for s in $(grep -oE 'Java_[A-Za-z0-9_]+' "$c" | sort -u); do grep -q "$s" "$O"/h/*.h || missing=$((missing + 1)); done
  echo "native $n rc=$rc java_symbols=$(grep -oE 'Java_[A-Za-z0-9_]+' "$c" | sort -u | wc -l) missing_in_headers=$missing"
done
