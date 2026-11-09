#!/bin/bash
# SPDX-License-Identifier: Apache-2.0
# gate.sh <work-dir> : INDEXSTRING_IX1_GATE v2 (recipe trie-core-20261107).
# <work-dir> holds: tree/ (master m3/ports/indexstring plus the main sources of m3/collections, m3/arrays and
# m3/ports/precompute), main/ (relocate.py output), donor/ (the donor MIndexTrieNavigation, MIndexLiteralSearchTree
# and MIndexStringEditDistancePlan at the PROVENANCE.tsv commit). Steps:
#   1. per side (live = master module; leaf = master module + main/*.java): javac --release 21 -Xlint:all,-module
#      -Werror over the module's main sources with the dependency sources on -sourcepath (module-info removed), the
#      module's test sources with -Xlint:all -Werror, then its exec mains;
#   2. the donor side: stub/*.java (donor monitor, native loader and MIndexString shapes only) + donor/*.java;
#   3. Ix1Diff (donor vs relocated) over two seeds x 20,000 rounds.
# Maven (the module's own flags) is gate 1 in GATES.tsv; this script is the differential host.
set -u
H=$(cygpath -m "$(cd "$(dirname "$0")" && pwd)"); X=$(cygpath -m "$(cd "${1:?work-dir}" && pwd)")  # Windows javac: S:/ paths, not /s/
J="${JDK21:-C:/Program Files/Java/jdk-21}/bin"; A=$(cygpath -m "$X"); W=$X/g; rm -rf "$W"; mkdir -p "$W"
find "$X/tree" -name module-info.java -delete
P=src/main/java/jdk/internal/m3/indexstring
MAINS="ExistingSurfaceTest RetainedViewTest M3IndexCharacterFlagsCombineSmokeMain"
for side in live leaf; do
  S=$W/$side; mkdir -p "$S"; cp -r "$X/tree" "$S/t"
  if [ $side = leaf ]; then
    cp "$X"/main/*.java "$S/t/m3/ports/indexstring/$P/"
    cp "$H/../../src/test/java/jdk/internal/m3/indexstring/MIndexTrieCoreSmokeMain.java" "$S/t/m3/ports/indexstring/src/test/java/jdk/internal/m3/indexstring/"
    MAINS="$MAINS MIndexTrieCoreSmokeMain"
  fi
  SP="$A/g/$side/t/m3/collections/src/main/java;$A/g/$side/t/m3/arrays/src/main/java;$A/g/$side/t/m3/ports/precompute/src/main/java"
  find "$S/t/m3/ports/indexstring/src/main/java" -name '*.java' > "$S/main.lst"
  "$J/javac" --release 21 -Xlint:all,-module -Werror -proc:none -encoding UTF-8 -implicit:class -sourcepath "$SP" -d "$S/cls" @"$S/main.lst" > "$S/main.log" 2>&1
  echo "$side main javac rc=$? errors=$(grep -c ': error:' "$S/main.log") warnings=$(grep -c ': warning:' "$S/main.log") files=$(wc -l < "$S/main.lst")"
  find "$S/t/m3/ports/indexstring/src/test/java" -name '*.java' > "$S/test.lst"
  "$J/javac" --release 21 -Xlint:all -Werror -proc:none -encoding UTF-8 -cp "$A/g/$side/cls" -d "$S/tcls" @"$S/test.lst" > "$S/test.log" 2>&1
  echo "$side test javac rc=$? errors=$(grep -c ': error:' "$S/test.log") warnings=$(grep -c ': warning:' "$S/test.log")"
  for m in $MAINS; do
    "$J/java" -cp "$A/g/$side/cls;$A/g/$side/tcls" jdk.internal.m3.indexstring.$m > "$S/$m.out" 2>&1
    echo "$side $m rc=$? $(tail -1 "$S/$m.out" | cut -c1-120)"
  done
done
"$J/javac" --release 21 -nowarn -proc:none -encoding UTF-8 -d "$W/dcls" "$H"/stub/*.java "$X"/donor/*.java > "$W/donor.log" 2>&1
echo "donor javac rc=$? errors=$(grep -c ': error:' "$W/donor.log")"
"$J/javac" --release 21 -nowarn -proc:none -encoding UTF-8 -cp "$A/g/dcls;$A/g/leaf/cls" -d "$W/hcls" "$H/Ix1Diff.java" > "$W/h.log" 2>&1 || { cat "$W/h.log"; exit 1; }
for seed in 20261107 7; do "$J/java" -cp "$A/g/hcls;$A/g/dcls;$A/g/leaf/cls" Ix1Diff $seed 20000; done
