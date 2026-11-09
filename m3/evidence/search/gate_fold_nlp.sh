#!/bin/bash
# gate_search.sh <tree> <deps> <out> : m3/search and (when present) m3/nlp on a host JDK 21, as their poms build them:
# javac --release 21 -Xlint:all,-module -Werror, main then test (m3-collections, m3-algorithms and m3-file by
# -sourcepath from <deps>), JUnit console over each module's test classes (class path, as surefire useModulePath=false).
# One summary line per module and one line per test, so the tree before a fold and the tree after can be compared.
set -u
T=$1; D=$2; O=$3; rm -rf "$O"; mkdir -p "$O"
M2=/mnt/s/m3-scratch/eb-conv/m2
JU=$M2/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar
DSP=$D/m3/collections/src/main/java:$D/m3/algorithms/src/main/java:$D/m3/file/src/main/java
build() {  # <name> <module dir> <sourcepath>
  local n=$1 m=$2 sp=$3
  mkdir -p "$O/$n/main" "$O/$n/test"
  javac --release 21 -Xlint:all,-module,-processing -Werror -proc:none ${sp:+-sourcepath "$sp"} -d "$O/$n/main" \
    $(find "$m/src/main/java" -name '*.java' ! -name module-info.java) > "$O/$n/javac_main.log" 2>&1
  local a=$?
  javac --release 21 -nowarn -proc:none -cp "$O/$n/main:$JU" ${sp:+-sourcepath "$sp"} -d "$O/$n/test" \
    $(find "$m/src/test/java" -name '*.java') > "$O/$n/javac_test.log" 2>&1
  local b=$?
  for k in main test; do [ -d "$m/src/$k/resources" ] && cp -r "$m/src/$k/resources/." "$O/$n/$k/"; done
  echo "$n javac main rc=$a test rc=$b classes=$(find "$O/$n/main" -name '*.class' | wc -l)/$(find "$O/$n/test" -name '*.class' | wc -l)"
  (cd "$m" && timeout 1800 java -cp "$O/$n/test:$O/$n/main:$JU" org.junit.platform.console.ConsoleLauncher execute \
    --scan-class-path "$O/$n/test" --disable-banner --details=tree --disable-ansi-colors > "$O/$n/junit.log" 2>&1)
  echo "$n junit rc=$? $(grep -E 'tests (found|successful|failed|skipped|aborted)' "$O/$n/junit.log" | tr -s ' []' ' ' | tr '\n' ' ')"
}
build search "$T/m3/search" "$DSP"
[ -d "$T/m3/nlp/src" ] && build nlp "$T/m3/nlp" ""
true
