#!/bin/bash
# leaf5_gate.sh <master-sha> : M3_SEARCH_MODULE_GATE v2 (LEAF_OUT selects the leaf tree; Leaf5Diff only for leaf 5). live = master's m3/search; leaf = live + the
# relocated leaf-5 files (${LEAF_OUT:-leaf5/out}). Per side: javac --release 21 -Xlint:all,-module -Werror over every m3/search
# main source with m3/collections + m3/ports/file + m3/file main sources on -sourcepath (implicit closure only);
# then the m3/search test sources against it (JUnit console 1.10.1); then the leaf's JUnit test and the
# differential (leaf5/Leaf5Diff) against the module build. Prints rc/error counts and the JUnit summary.
cd "$(dirname "$0")"
J="C:/Program Files/Java/jdk-21/bin"; G=/h/m3-scratch/repos/M3jdk21-release.git; R=${1:?master}
M=$(cygpath -m ~/.m2/repository); CON="$M/org/junit/platform/junit-platform-console-standalone/1.10.1/junit-platform-console-standalone-1.10.1.jar"
A=$(cygpath -m "$PWD"); W=l5g; rm -rf $W; mkdir -p $W/tree  # absolute classpaths: MSYS mangles relative ;-lists
for d in m3/search m3/collections/src/main/java m3/ports/file/src/main/java m3/file/src/main/java; do
  GIT_DIR=$G git ls-tree -r $R -- $d | awk '{print $3}' | GIT_DIR=$G git fetch -q --filter=blob:none --stdin origin 2>/dev/null
  GIT_DIR=$G git ls-tree -r $R -- $d | grep -E '\.java$' | while IFS=$'\t' read -r meta p; do
    mkdir -p "$W/tree/$(dirname "$p")"; GIT_DIR=$G git cat-file blob ${meta##* } > "$W/tree/$p"; done
done
find $W/tree -name module-info.java -delete  # a module-info on the sourcepath switches javac to module mode
for side in live leaf; do
  S=$W/$side; mkdir -p $S; cp -r $W/tree $S/t
  [ $side = leaf ] && cp -r ${LEAF_OUT:-leaf5/out}/m3/search/src/. $S/t/m3/search/src/
  SP="$S/t/m3/collections/src/main/java;$S/t/m3/ports/file/src/main/java;$S/t/m3/file/src/main/java"
  find $S/t/m3/search/src/main/java -name '*.java' > $S/main.lst
  "$J/javac" --release 21 -Xlint:all,-module -Werror -proc:none -encoding UTF-8 -sourcepath "$SP" -d $S/cls @$S/main.lst > $S/main.log 2>&1
  echo "$side main javac rc=$? errors=$(grep -c ': error:' $S/main.log) warnings=$(grep -c ': warning:' $S/main.log) files=$(wc -l < $S/main.lst)"
  find $S/t/m3/search/src/test/java -name '*.java' > $S/test.lst
  "$J/javac" --release 21 -nowarn -proc:none -encoding UTF-8 -cp "$A/$S/cls;$CON" -d $S/tcls @$S/test.lst > $S/test.log 2>&1
  echo "$side test javac rc=$? errors=$(grep -c ': error:' $S/test.log)"
done
S=$W/leaf
"$J/java" -jar "$CON" execute --class-path "$A/$S/cls;$A/$S/tcls" --select-class jdk.m3.search.DocumentDeduplicatorConcurrencyTest --disable-banner --details=summary 2>&1 | grep -E 'tests (successful|failed)' | tr -s ' ' | tr '\n' ' '; echo "<- leaf JUnit"
"$J/javac" -nowarn -proc:none -encoding UTF-8 -cp "$A/leaf5/dcls;$A/$S/cls" -d $W/hcls leaf5/Leaf5Diff.java && "$J/java" -cp "$A/$W/hcls;$A/leaf5/dcls;$A/$S/cls" Leaf5Diff 20261106 20000
