#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
# run_atom_perf.sh <worktree> <out_dir> [pkg ...] : runs the generated perf harnesses M3{List,Table,Tree}AtomsPerf on
# stock JDK 21 with --patch-module java.desktop=<out>/patch (built by run_atom_tests.sh, rebuilt here when missing),
# full rounds (-Dperf.rounds=7 -Dperf.warmup=5 unless PERF_OPTS says otherwise) and -XX:+PrintCompilation sent to
# stderr (-XX:+DisplayVMOutputToStderr) so it cannot interleave with the PERF lines; one JVM per package, one after the
# other; stdout to <out>/perf_<pkg>.log, the compilation log to <out>/perf_<pkg>.comp, both read by perf_report.py.
set -euo pipefail
wt=$1; out=$2; shift 2
pkgs=${*:-"list table tree"}
jdk=${JDK:-"/c/Program Files/Java/jdk-21"}
win() { cygpath -m "$1"; }
patch="$out/patch/java.desktop"
if [ ! -d "$patch/sun/swing/tree" ]; then
  mkdir -p "$patch"
  ( cd "$wt/src/java.desktop/share/classes" && "$jdk/bin/javac" -J-Xmx512m --patch-module java.desktop=. \
      -d "$(win "$patch")" sun/swing/list/M3*.java sun/swing/table/M3*.java sun/swing/tree/M3*.java )
fi
for pkg in $pkgs; do
  case $pkg in list) cls=M3ListAtomsPerf ;; table) cls=M3TableAtomsPerf ;; tree) cls=M3TreeAtomsPerf ;; esac
  dir="$wt/test/jdk/sun/swing/$pkg"
  bin="$out/perf/$pkg"; rm -rf "$bin"; mkdir -p "$bin"
  module=$(grep -ho '@modules .*' "$dir/$cls.java" | sed 's/@modules //; s/:open//')
  flags=$(grep -h '@run main/othervm' "$dir/$cls.java" | sed 's/.*@run main\/othervm //' | tr ' ' '\n' | grep -v '^-Dperf\.' | grep -v "^$cls\$" | tr '\n' ' ')
  "$jdk/bin/javac" -J-Xmx512m -nowarn --patch-module java.desktop="$(win "$patch")" --add-exports "$module=ALL-UNNAMED" \
      -d "$(win "$bin")" "$(win "$dir/AtomSupport.java")" "$(win "$dir/PerfSupport.java")" "$(win "$dir/$cls.java")"
  echo "PERF $pkg start $(date +%H:%M:%S) flags: $flags"
  "$jdk/bin/java" -Xmx1g $flags ${PERF_OPTS:-"-Dperf.rounds=7 -Dperf.warmup=5"} -XX:+PrintCompilation -XX:+DisplayVMOutputToStderr \
      --patch-module java.desktop="$(win "$patch")" --add-opens "$module=ALL-UNNAMED" -cp "$(win "$bin")" "$cls" \
      > "$out/perf_$pkg.log" 2> "$out/perf_$pkg.comp"
  echo "PERF $pkg done $(date +%H:%M:%S) rows=$(grep -c '^PERF' "$out/perf_$pkg.log") $(grep '^LOAD' "$out/perf_$pkg.log" | tr '\t' ' ' | tr '\n' ';')"
done
