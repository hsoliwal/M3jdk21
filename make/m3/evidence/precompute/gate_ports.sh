#!/bin/bash
# gate_ports.sh <tree> <out> : m3/ports/precompute, m3/ports/regex-cursor and their consumer m3/ports/indexstring (with
# m3/intern built from source as its dependency) on a host JDK 21: javac main + test per module, then the JUnit console
# over each module's test classes. One summary line per module, so two trees can be compared.
set -u
T=$1; O=$2; rm -rf "$O"; mkdir -p "$O"
M2=/mnt/s/m3-scratch/eb-conv/m2; R2=/mnt/s/m3-scratch/eb-conv/reactor/m2
JU=$M2/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar
COLL=$R2/com/m3/m3-collections/0.1.0-SNAPSHOT/m3-collections-0.1.0-SNAPSHOT.jar
build() {  # <name> <module dir> <classpath>
  local n=$1 m=$2 cp=$3
  mkdir -p "$O/$n/main" "$O/$n/test"
  javac --release 21 -nowarn -cp "$cp" -d "$O/$n/main" $(find "$m/src/main/java" -name '*.java' ! -name module-info.java) > "$O/$n/javac_main.log" 2>&1
  local a=$?
  local tests=$(find "$m/src/test/java" -name '*.java' 2>/dev/null)
  local b=0; [ -n "$tests" ] && { javac --release 21 -nowarn -cp "$O/$n/main:$cp:$JU" -d "$O/$n/test" $tests > "$O/$n/javac_test.log" 2>&1; b=$?; }
  echo "$n javac main rc=$a test rc=$b"
}
run() {  # <name> <classpath> [props]
  local n=$1 cp=$2; shift 2
  java "$@" -cp "$O/$n/test:$O/$n/main:$cp:$JU" org.junit.platform.console.ConsoleLauncher execute \
    --scan-class-path "$O/$n/test" --disable-banner --details=summary > "$O/$n/junit.log" 2>&1
  echo "$n junit $(grep -E 'tests (found|successful|failed|skipped|aborted)' "$O/$n/junit.log" | tr -s ' []' ' ' | tr '\n' ' ')"
}
build intern "$T/m3/intern" ""
build precompute "$T/m3/ports/precompute" ""
build regexcursor "$T/m3/ports/regex-cursor" ""
build indexstring "$T/m3/ports/indexstring" "$COLL:$O/precompute/main:$O/intern/main"
run precompute ""
run regexcursor ""
run indexstring "$COLL:$O/precompute/main:$O/intern/main"
