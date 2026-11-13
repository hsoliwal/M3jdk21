#!/bin/bash
# cow_gate.sh : RETAINED_PATCH_MODULE_GATE v1 for the pr1908 CopyOnWriteArrayList twin (java.util.concurrent).
# live = master's CopyOnWriteArrayList.java, rest = retained-variants/pr1908 copy; each compiled alone with
# javac --patch-module java.base -Xlint:all -Werror, then CowDiff (removeIf / subList.removeIf vs ArrayList,
# reentrant CME) runs on stock JDK 21 with the patched class. live and rest must both PASS with equal digests.
cd "$(dirname "$0")"
J="C:/Program Files/Java/jdk-21/bin"; G=/h/m3-scratch/repos/M3jdk21-release.git
R=${1:?master sha}; F=src/java.base/share/classes/java/util/concurrent/CopyOnWriteArrayList.java
P=m3/qualification/master-landing-20261028/retained-variants/pr1908
for side in live rest; do
  W=cow/$side; rm -rf $W; mkdir -p $W/src/java/util/concurrent
  if [ $side = live ]; then GIT_DIR=$G git show "$R:$F"; else GIT_DIR=$G git show "$R:$P/$F"; fi > $W/src/java/util/concurrent/CopyOnWriteArrayList.java
  SRC=$(cygpath -m "$PWD/$W/src"); OUT=$(cygpath -m "$PWD/$W/cls")
  "$J/javac" --release 21 -Xlint:all -Werror -encoding UTF-8 -proc:none --patch-module java.base="$SRC" -d "$OUT" \
    "$SRC/java/util/concurrent/CopyOnWriteArrayList.java" > cow/$side-base.log 2>&1
  echo "$side base javac rc=$? errors=$(grep -c ': error:' cow/$side-base.log) warnings=$(grep -c ': warning:' cow/$side-base.log)"
  "$J/javac" -encoding UTF-8 -proc:none -d cow/$side/t cow/CowDiff.java > cow/$side-t.log 2>&1 || { echo "$side harness javac FAIL"; continue; }
  "$J/java" --patch-module java.base="$OUT" -cp cow/$side/t CowDiff ${2:-20261106} ${3:-200000} > cow/$side-run.log 2>&1
  echo "$side run rc=$? $(tail -1 cow/$side-run.log | cut -c1-160)"
done
