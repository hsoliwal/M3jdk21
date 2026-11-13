#!/bin/bash
# gate_compiler.sh <tree> <deps> <out> <module dir>[@<main lint>[@<test lint>]] ... : jdk.m3.compiler family on a host JDK 21
# as the poms build it (release 21, -Werror, -proc:none for main, class path as surefire useModulePath=false; m3-collections
# by -sourcepath from <deps>; resources copied); then the JUnit console from the module directory. One summary line per
# module and a per-test log, so the 6 modules before the fold and the folded module after can be compared.
set -u
T=$1; D=$2; O=$3; shift 3; rm -rf "$O"; mkdir -p "$O"
M2=/mnt/s/m3-scratch/eb-conv/m2
JU=$M2/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar
DSP=$D/m3/collections/src/main/java:$D/m3/algorithms/src/main/java:$D/m3/file/src/main/java
COLL=/mnt/s/m3-scratch/eb-conv/reactor/m2/com/m3/m3-collections/0.1.0-SNAPSHOT/m3-collections-0.1.0-SNAPSHOT.jar
for spec in "$@"; do
  IFS=@ read -r m ml tl <<< "$spec"; ml=${ml:--Xlint:all}; tl=${tl:-$ml}
  n=$(echo "$m" | tr '/' '_'); mkdir -p "$O/$n/main" "$O/$n/test"
  javac --release 21 "$ml" -Werror -proc:none -sourcepath "$DSP" -d "$O/$n/main" \
    $(find "$T/$m/src/main/java" -name '*.java' ! -name module-info.java) > "$O/$n/javac_main.log" 2>&1
  a=$?
  [ -d "$T/$m/src/main/resources" ] && cp -r "$T/$m/src/main/resources/." "$O/$n/main/"
  javac --release 21 "$tl" -Werror -proc:none -cp "$O/$n/main:$JU" -sourcepath "$DSP" -d "$O/$n/test" \
    $(find "$T/$m/src/test/java" -name '*.java') > "$O/$n/javac_test.log" 2>&1
  b=$?
  [ -d "$T/$m/src/test/resources" ] && cp -r "$T/$m/src/test/resources/." "$O/$n/test/"
  echo "$m javac main rc=$a test rc=$b classes=$(find "$O/$n/main" -name '*.class' | wc -l)/$(find "$O/$n/test" -name '*.class' | wc -l)"
  (cd "$T/$m" && timeout 1800 java -cp "$O/$n/test:$O/$n/main:$COLL:$JU" org.junit.platform.console.ConsoleLauncher execute \
    --scan-class-path "$O/$n/test" --disable-banner --details=tree --disable-ansi-colors > "$O/$n/junit.log" 2>&1)
  echo "$m junit rc=$? $(grep -E 'tests (found|successful|failed|skipped|aborted)' "$O/$n/junit.log" | tr -s ' []' ' ' | tr '\n' ' ')"
done
