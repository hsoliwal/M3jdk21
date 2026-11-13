#!/bin/bash
# gate_dag_tools.sh <tree> <deps> <out> : the six jdk.m3.tooling modules of the prefix-drop leaf on a host JDK 21.
#   backport-dag      javac main+test (a3, migration-recipes and the vendored recipe sources by -sourcepath, OpenRewrite
#                     8.17.1 on the class path), resources copied, JUnit console from the module directory;
#   atom-jit-gate     javac main+test with atom-census by -sourcepath, JUnit with m3.atomjit.patternRules as in the pom;
#   recipe-agent-api, smart-build   javac main+test, JUnit;
#   cds-training, lazy-priority-frontier   their own verify.sh.
# One summary line per module, so two trees can be compared after normalising class names.
set -u
T=$1; D=$2; O=$3; rm -rf "$O"; mkdir -p "$O"
M2=/mnt/s/m3-scratch/eb-conv/m2
JU=$M2/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar
RW=$(find $M2/org/openrewrite -path '*/8.17.1/*.jar' ! -name '*sources*' ! -name '*javadoc*'; \
     find $M2/com/fasterxml/jackson $M2/io/micrometer $M2/org/jspecify $M2/org/jetbrains/annotations $M2/com/google/code/findbugs \
          -name '*.jar' ! -name '*sources*' ! -name '*javadoc*' 2>/dev/null)
RW=$(echo "$RW" | tr '\n' ':')
DSP=$D/make/m3/tooling/a3/src/main/java:$D/make/m3/tooling/migration-recipes/src/main/java:$D/m3/vendor/m3/m3-openrewrite-recipes/src/main/java
EXCL=""  # regex of type stems left out of a build, with every file that names one (closure), on both trees alike
excluded() {  # <files...> : the files to leave out under EXCL, closed over references
  [ -n "$EXCL" ] || return 0
  local set="$EXCL" prev=""
  while [ "$set" != "$prev" ]; do prev=$set
    set=$(grep -lE "\b(M3)?($set)\b" "$@" | sed -E 's#.*/##; s#\.java$##; s#^M3##' | sort -u | paste -sd'|')
    set=$(printf '%s|%s' "$EXCL" "$set" | tr '|' '\n' | sort -u | paste -sd'|')
  done
  printf '%s\n' "$@" | grep -E "/(M3)?($set)\.java$"
}
keep() { local all=("$@") ex; ex=$(excluded "${all[@]}"); printf '%s\n' "${all[@]}" | grep -vxF -f <(printf '%s\n' "$ex" | grep .) || true; }
build() {  # <name> <module dir> <classpath> <sourcepath>
  local n=$1 m=$2 cp=$3 sp=$4
  mkdir -p "$O/$n/main" "$O/$n/test"
  local every=($(find "$m/src/main/java" "$m/src/test/java" -name '*.java' ! -name module-info.java 2>/dev/null))
  local kept=$(keep "${every[@]}")
  [ -n "$EXCL" ] && excluded "${every[@]}" | sed -E 's#.*/##; s#^M3##' | sort > "$O/$n/excluded.txt"
  javac --release 21 -nowarn -proc:none -cp "$cp" ${sp:+-sourcepath "$sp"} -d "$O/$n/main" \
    $(echo "$kept" | grep '/src/main/java/') > "$O/$n/javac_main.log" 2>&1
  local a=$?
  [ -d "$m/src/main/resources" ] && cp -r "$m/src/main/resources/." "$O/$n/main/"
  local tests=$(echo "$kept" | grep '/src/test/java/')
  local b=0; [ -n "$tests" ] && { javac --release 21 -nowarn -proc:none -cp "$O/$n/main:$cp:$JU" ${sp:+-sourcepath "$sp"} \
    -d "$O/$n/test" $tests > "$O/$n/javac_test.log" 2>&1; b=$?; }
  [ -d "$m/src/test/resources" ] && cp -r "$m/src/test/resources/." "$O/$n/test/"
  echo "$n javac main rc=$a errors=$(grep -c 'error:' "$O/$n/javac_main.log") test rc=$b"
}
run() {  # <name> <module dir> <classpath> [props]
  local n=$1 m=$2 cp=$3; shift 3
  (cd "$m" && timeout 900 java "$@" -cp "$O/$n/test:$O/$n/main:$cp:$JU" org.junit.platform.console.ConsoleLauncher execute \
    --scan-class-path "$O/$n/test" --disable-banner --details=tree > "$O/$n/junit.log" 2>&1)
  echo "$n junit rc=$? $(grep -E 'tests (found|successful|failed|skipped|aborted)' "$O/$n/junit.log" | tr -s ' []' ' ' | tr '\n' ' ')"
}
M=$T/make/m3/tooling
build dag "$M/backport-dag" "$RW" "$DSP"
# master does not compile these two (stale A3Apply.run signature, record accessor shadowing path()); the rest is gated
EXCL="A3BackportPreparation|PacketDagComposer" build dagx "$M/backport-dag" "$RW" "$DSP"
build atomjit "$M/atom-jit-gate" "" "$M/atom-census/src/main/java"
build recipe "$M/recipe-agent-api" "" ""
build smartbuild "$M/smart-build" "" ""
run dagx "$M/backport-dag" "$RW"
echo "dagx excluded $(wc -l < "$O/dagx/excluded.txt") files: $(tr '\n' ' ' < "$O/dagx/excluded.txt")"
run atomjit "$M/atom-jit-gate" "" -Dm3.atomjit.patternRules="$M/atom-census/PATTERN_RULES.tsv"
run recipe "$M/recipe-agent-api" ""
run smartbuild "$M/smart-build" ""
for v in cds-training lazy-priority-frontier; do
  W=$(mktemp -d /tmp/gate_tool.XXXXXX)  # Linux file system: the CDS archive write is refused on /mnt
  TMPDIR=$W bash "$M/$v/verify.sh" > "$O/verify_$v.log" 2>&1
  echo "$v verify rc=$? $(tail -1 "$O/verify_$v.log" | cut -c1-120)"
done
