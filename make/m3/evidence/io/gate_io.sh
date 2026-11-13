#!/bin/bash
# gate_io.sh <tree> <out> : the optane area in either layout. Maven layout (m3/optane/src/...) or JDK-module layout
# (src/jdk.m3.io/share/classes + test/jdk/jdk/{,internal/}m3/optane): main sources with -h headers (as make/modules/<module>/Java.gmk: -Xlint:all,-module; in the JDK layout
# also compiled as the named module jdk.m3.io from its module-info), tests on the class path, the JUnit console from
# m3/optane (where Maven runs them), and every C source compiled -Werror against the headers.
set -u
T=$1; O=$2; rm -rf "$O"; mkdir -p "$O/main" "$O/test" "$O/h" "$O/native" "$O/mod"
JU=/mnt/s/m3-scratch/eb-conv/m2/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar
JH=$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")
if [ -d "$T/src/jdk.m3.io" ]; then
  MAIN=$T/src/jdk.m3.io/share/classes; TESTS="$T/test/jdk/jdk/m3/optane $T/test/jdk/jdk/internal/m3/optane"
  NAT="$T/src/jdk.m3.io/share/native $T/test/jdk/jdk/m3/optane/native"
  javac --release 21 -Xlint:all,-module -Werror -proc:none -d "$O/mod" $(find "$MAIN" -name '*.java') > "$O/javac_module.log" 2>&1
  echo "javac named module jdk.m3.io rc=$? classes=$(find "$O/mod" -name '*.class' | wc -l)"
else
  MAIN=$T/m3/optane/src/main/java; TESTS=$T/m3/optane/src/test/java; NAT="$T/m3/optane/src/main/native $T/m3/optane/src/test/native"
fi
javac --release 21 -Xlint:all -Werror -proc:none -h "$O/h" -d "$O/main" $(find "$MAIN" -name '*.java' ! -name module-info.java) > "$O/javac_main.log" 2>&1
echo "javac main rc=$? classes=$(find "$O/main" -name '*.class' | wc -l) headers=$(ls "$O/h" | wc -l)"
javac --release 21 -Xlint:all -Werror -proc:none -cp "$O/main:$JU" -h "$O/h" -d "$O/test" $(find $TESTS -name '*.java') > "$O/javac_test.log" 2>&1
echo "javac test rc=$? classes=$(find "$O/test" -name '*.class' | wc -l)"
(cd "$T/m3/optane" && timeout 1800 java -cp "$O/test:$O/main:$JU" org.junit.platform.console.ConsoleLauncher execute \
  --scan-class-path "$O/test" --disable-banner --details=tree --disable-ansi-colors > "$O/junit.log" 2>&1)
echo "junit $(grep -E 'tests (found|successful|failed|skipped|aborted)' "$O/junit.log" | tr -s ' []' ' ' | tr '\n' ' ')"
for c in $(find $NAT -name '*.c' | sort); do
  n=$(basename "$c" .c)
  cc -std=c11 -Wall -Wextra -Werror -fPIC -shared -I"$JH/include" -I"$JH/include/linux" -I"$O/h" -I"$(dirname "$c")" -o "$O/native/lib$n.so" "$c" > "$O/native/$n.log" 2>&1
  rc=$?; missing=0
  for s in $(grep -oE 'Java_[A-Za-z0-9_]+' "$c" | sort -u); do grep -q "$s" "$O"/h/*.h || missing=$((missing + 1)); done
  echo "native $n rc=$rc java_symbols=$(grep -oE 'Java_[A-Za-z0-9_]+' "$c" | sort -u | wc -l) missing_in_headers=$missing"
done
