#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
# run_atom_tests.sh <worktree> <out_dir> [pkg ...] : runs the generated per-atom JUnit tests without a JDK build.
# 1. javac the M3 classes of sun.swing.{list,table,tree} from <worktree>/src into <out>/patch (--patch-module).
# 2. javac each test/jdk/sun/swing/<pkg> directory against the patched java.desktop and JUnit 5.
# 3. One JVM per distinct `@run junit/othervm <flags>` line: the JUnit console launcher on stock JDK 21 with
#    --patch-module java.desktop=<patch> and the jtreg @modules lines turned into --add-opens/--add-exports.
# Env: JDK (default C:/Program Files/Java/jdk-21), JUNIT_JAR (console standalone 1.11.4), EXTRA_JAVA_OPTS, EXTRA_CP
# (more classpath for the launcher, e.g. the REACH listener), JUNIT_DETAILS (summary|tree).
set -euo pipefail
wt=$1; out=$2; shift 2
pkgs=${*:-"list table tree"}
jdk=${JDK:-"/c/Program Files/Java/jdk-21"}
junit=${JUNIT_JAR:-"$HOME/.m2/repository/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar"}
win() { cygpath -m "$1"; }
mkdir -p "$out"
patch="$out/patch/java.desktop"
rm -rf "$out/patch"; mkdir -p "$patch"
( cd "$wt/src/java.desktop/share/classes" && "$jdk/bin/javac" -J-Xmx512m -Xlint:all -Werror --patch-module java.desktop=. \
    -d "$(win "$patch")" sun/swing/list/M3*.java sun/swing/table/M3*.java sun/swing/tree/M3*.java )
echo "PATCH classes=$(find "$patch" -name '*.class' | wc -l)"
status=0
for pkg in $pkgs; do
  dir="$wt/test/jdk/sun/swing/$pkg"
  cls="$out/tests/$pkg"; rm -rf "$cls"; mkdir -p "$cls"
  exports=""
  for m in $(grep -ho '@modules .*' "$dir"/*AtomsTest.java | sed 's/@modules //' | tr ' ' '\n' | sort -u); do
    exports="$exports --add-exports ${m%:open}=ALL-UNNAMED"
  done
  "$jdk/bin/javac" -J-Xmx512m -Xlint:all,-serial -Xmaxerrs 50 --patch-module java.desktop="$(win "$patch")" $exports \
      -cp "$(win "$junit")" -d "$(win "$cls")" $(for f in "$dir"/*.java; do win "$f"; done)
  echo "COMPILED $pkg classes=$(find "$cls" -name '*.class' | wc -l)"
  mapfile -t flagsets < <(grep -h '@run junit/othervm' "$dir"/*.java | sed 's/.*@run junit\/othervm //' | awk '{$NF=""; print}' | sed 's/ *$//' | sort -u)
  for flags in "${flagsets[@]}"; do
    classes=$(grep -l "@run junit/othervm $flags [A-Za-z0-9]*AtomsTest" "$dir"/*.java | xargs -n1 basename | sed 's/\.java$//')
    opens=""
    for f in $classes; do
      for m in $(grep -ho '@modules .*' "$dir/$f.java" | sed 's/@modules //'); do
        case "$m" in *:open) opens="$opens --add-opens=${m%:open}=ALL-UNNAMED" ;; *) opens="$opens --add-exports=$m=ALL-UNNAMED" ;; esac
      done
    done
    sel=""; for f in $classes; do sel="$sel --select-class $f"; done
    tag=$(echo "$pkg $flags" | tr -c 'A-Za-z0-9\n' '_')
    echo "RUN $pkg [$flags]:$sel"
    "$jdk/bin/java" -Xmx768m $flags ${EXTRA_JAVA_OPTS:-} --patch-module java.desktop="$(win "$patch")" $(echo "$opens" | tr ' ' '\n' | sort -u | tr '\n' ' ') \
        -jar "$(win "$junit")" execute -cp "$(win "$cls")${EXTRA_CP:+;$EXTRA_CP}" --disable-banner --details=${JUNIT_DETAILS:-summary} --details-theme=ascii \
        --reports-dir "$(win "$out/reports/$tag")" $sel > "$out/junit_$tag.log" 2>&1 || status=1
    grep -E 'tests (successful|failed|found)|containers failed' "$out/junit_$tag.log" | sed "s/^/  $tag /"
  done
done
exit $status
