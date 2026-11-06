#!/bin/bash
# cb_gate.sh : RETAINED_PATCH_MODULE_GATE v1 for the pr1456 M3CompositeBytes twin (jdk.internal.m3).
# For side in live|rest: java.base patch sources = master's jdk/internal/m3/*.java (+ io/, hash/) with
# M3CompositeBytes taken from master (live) or from retained-variants/pr1456 (rest); javac --patch-module
# java.base -Xlint:all -Werror; then the retained oracle test M3CompositeLiteralChecks is compiled and run
# on stock JDK 21 with the patched classes (expected: live fails to compile it, rest compiles and passes).
cd "$(dirname "$0")"
J="C:/Program Files/Java/jdk-21/bin"; G=/h/m3-scratch/repos/M3jdk21-release.git
R=${1:?master sha}; P=m3/qualification/master-landing-20261028/retained-variants/pr1456
B=src/java.base/share/classes/jdk/internal/m3
W=cb; rm -rf $W; mkdir -p $W/test
GIT_DIR=$G git ls-tree -r --name-only $R -- $B | grep '\.java$' > $W/files.lst
for side in live rest; do
  S=$W/$side/src/jdk/internal/m3
  while read -r f; do d=$W/$side/src/${f#src/java.base/share/classes/}; mkdir -p "$(dirname "$d")"; GIT_DIR=$G git show "$R:$f" > "$d"; done < $W/files.lst
  [ $side = rest ] && GIT_DIR=$G git show "$R:$P/$B/M3CompositeBytes.java" > $S/M3CompositeBytes.java
  SRC=$(cygpath -m "$PWD/$W/$side/src"); OUT=$(cygpath -m "$PWD/$W/$side/cls")
  "$J/javac" --release 21 -Xlint:all -Werror -encoding UTF-8 -proc:none --patch-module java.base="$SRC" -d "$OUT" \
    "$SRC/jdk/internal/m3/M3CompositeBytes.java" "$SRC/jdk/internal/m3/M3ByteSlices.java" > $W/$side-base.log 2>&1
  echo "$side base javac rc=$? errors=$(grep -c ': error:' $W/$side-base.log) warnings=$(grep -c ': warning:' $W/$side-base.log)"
done
GIT_DIR=$G git show "$R:$P/test/jdk/jdk/internal/m3/M3CompositeLiteralChecks.java" > $W/test/M3CompositeLiteralChecks.java
for side in live rest; do
  OUT=$(cygpath -m "$PWD/$W/$side/cls"); TC=$(cygpath -m "$PWD/$W/$side/tcls")
  "$J/javac" -encoding UTF-8 -proc:none --patch-module java.base="$OUT" --add-exports java.base/jdk.internal.m3=ALL-UNNAMED \
    -d "$TC" $W/test/M3CompositeLiteralChecks.java > $W/$side-test.log 2>&1
  rc=$?; echo "$side test javac rc=$rc errors=$(grep -c ': error:' $W/$side-test.log)"
  [ $rc = 0 ] || continue
  "$J/java" -ea -esa --patch-module java.base="$OUT" --add-exports java.base/jdk.internal.m3=ALL-UNNAMED -cp "$TC" \
    M3CompositeLiteralChecks > $W/$side-run.log 2>&1
  echo "$side run rc=$? tail=$(tail -1 $W/$side-run.log | cut -c1-160)"
done
