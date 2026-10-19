#!/usr/bin/env bash
# SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
# Builds the enhanced Swing API as a java.desktop patch and tests it on JDK 21 (no full JDK build).
#   run-tests.sh compile    patch classes (PATCH_FILES.txt, compiled alone against the JDK's java.desktop) + tests
#   run-tests.sh test       JUnit, headless, --patch-module java.desktop=<patch>
#   run-tests.sh coverage   the same under JaCoCo; report of the new code (NEW_CODE.txt)
#   run-tests.sh diff       behaviour probe against master's classes and against this branch's; outputs must be equal
# Env: M3_JDK21 (JDK 21 home), OUT (output dir, default S:/m3-scratch/enhanced-api/swing), BASE (git ref, default origin/master).
set -euo pipefail
here=$(cd "$(dirname "$0")" && { pwd -W 2>/dev/null || pwd; })
wt=$(cd "$here/../../.." && { pwd -W 2>/dev/null || pwd; })
jdk=${M3_JDK21:-"C:/Program Files/Java/jdk-21"}
out=${OUT:-S:/m3-scratch/enhanced-api/swing}
base=${BASE:-origin/master}
m2=$(cygpath -m ~/.m2/repository 2>/dev/null || echo ~/.m2/repository)
junit="$m2/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar"
agent="$m2/org/jacoco/org.jacoco.agent/0.8.15/org.jacoco.agent-0.8.15-runtime.jar"
cli="$m2/org/jacoco/org.jacoco.cli/0.8.15/org.jacoco.cli-0.8.15-nodeps.jar"
src="$wt/src/java.desktop/share/classes"
files=$(grep -v '^#' "$here/PATCH_FILES.txt")

# stage <dir> <work|base>: the patch sources, from the worktree or from the base ref (files new on this branch are skipped for base)
stage() {
  rm -rf "$1"; mkdir -p "$1"
  for f in $files; do
    mkdir -p "$1/$(dirname "$f")"
    if [ "$2" = work ]; then cp "$src/$f" "$1/$f"
    else git -C "$wt" show "$base:src/java.desktop/share/classes/$f" > "$1/$f" 2>/dev/null || rm -f "$1/$f"; fi
  done
}

# patch <stage> <classes>: javac of the staged files only (-implicit:none), against the JDK's own java.desktop
patch() {
  rm -rf "$2"; mkdir -p "$2"
  (cd "$1" && find . -name '*.java' > "$1.list" && "$jdk/bin/javac" -J-Xmx768m --patch-module java.desktop=. \
    -implicit:none -Xlint:all,-serial,-deprecation,-removal,-rawtypes,-unchecked,-cast,-static,-fallthrough,-dep-ann,-this-escape,-missing-explicit-ctor \
    -d "$2" @"$1.list")
}

compile() {
  stage "$out/stage" work
  patch "$out/stage" "$out/patch"
  rm -rf "$out/test-classes"; mkdir -p "$out/test-classes"
  find "$here/src/test/java" -name '*.java' > "$out/tests.list"
  "$jdk/bin/javac" -J-Xmx768m --patch-module java.desktop="$out/patch" -Xlint:all,-serial -cp "$junit" \
    -d "$out/test-classes" @"$out/tests.list"
  echo "COMPILED patch=$(find "$out/patch" -name '*.class' | wc -l) tests=$(find "$out/test-classes" -name '*.class' | wc -l)"
}

junit_run() {
  "$jdk/bin/java" -Xmx1g -Djava.awt.headless=true --patch-module java.desktop="$out/patch" "$@" \
    -jar "$junit" execute -cp "$out/test-classes" --disable-banner --details=tree --scan-classpath \
    --exclude-classname '.*Probe.*'
}

case "${1:-test}" in
  compile) compile ;;
  test) compile; junit_run ;;
  coverage)
    compile
    rm -f "$out/cov.exec"
    junit_run "-javaagent:$agent=destfile=$out/cov.exec,inclbootstrapclasses=true,includes=javax.swing.M3*:javax.swing.JList*:javax.swing.JTable*:javax.swing.JTree*:javax.swing.DefaultListSelectionModel:javax.swing.DefaultListModel*:javax.swing.table.AbstractTableModel*:javax.swing.table.DefaultTableModel*:javax.swing.tree.DefaultTreeModel*"
    "$jdk/bin/java" -jar "$cli" report "$out/cov.exec" --classfiles "$out/patch" --sourcefiles "$out/stage" \
      --xml "$out/cov.xml" --csv "$out/cov.csv" --html "$out/cov-html" > /dev/null
    bash "$here/new-code-coverage.sh" "$out/cov.xml" "$here/NEW_CODE.txt" ;;
  diff)
    compile
    stage "$out/stage-base" base
    patch "$out/stage-base" "$out/patch-base"
    for side in base work; do
      p="$out/patch"; [ "$side" = base ] && p="$out/patch-base"
      "$jdk/bin/java" -Xmx1g -Djava.awt.headless=true --patch-module java.desktop="$p" \
        -cp "$out/test-classes" m3.swing.api.M3SwingBehaviourProbe "$out/probe-$side.txt"
    done
    if cmp -s "$out/probe-base.txt" "$out/probe-work.txt"; then
      echo "DIFF EQUAL $(wc -l < "$out/probe-work.txt") lines, sha256 $(sha256sum "$out/probe-work.txt" | cut -c1-16)"
    else
      diff "$out/probe-base.txt" "$out/probe-work.txt" | head -40; exit 1
    fi ;;
  *) echo "usage: $0 compile|test|coverage|diff" >&2; exit 2 ;;
esac
