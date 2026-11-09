#!/bin/bash
# SPDX-License-Identifier: Apache-2.0
# gate.sh <work-dir> : INDEXSTRING_IX2_GATE v1 (recipe wordbreak-20261107).
# <work-dir> holds: tree/ (master m3/ports/indexstring plus the main sources of m3/collections, m3/arrays,
# m3/ports/precompute and m3/algorithms), main/ (relocate.py output), donor/ (donor MIndexVocabularySearchTree,
# MIndexWordBreakPlan, MIndexTrieNavigation, MIndexLiteralSearchTree, MIndexStringBitOps, M3Utf16Spans,
# M3SpanUnicode, MIndexStringEditDistancePlan at the PROVENANCE.tsv commit). Steps:
#   1. per side (live = master module; leaf = master module + main/*.java + MIndexWordBreakSmokeMain): javac --release 21
#      -Xlint:all,-module -Werror with the dependency sources on -sourcepath (module-info removed), the module's test
#      sources with -Xlint:all -Werror, then its exec mains;
#   2. donor side: stub/*.java (donor precomputation row rules, spelling-equal MIndexString, range view, monitor and
#      native-loader shapes; FrozenChars/FrozenBytes are the module's own copies repackaged) + donor/*.java;
#   3. Ix2Diff (donor vs relocated) over two seeds x 20,000 rounds;
#   4. NecWordBreak: relocated canSegment vs jdk.m3.algorithm.M3DynamicProgrammingAlgorithms.wordBreak (NEC row).
set -u
H=$(cygpath -m "$(cd "$(dirname "$0")" && pwd)"); X=$(cygpath -m "$(cd "${1:?work-dir}" && pwd)")  # Windows javac: S:/ paths
J="${JDK21:-C:/Program Files/Java/jdk-21}/bin"; W=$X/g; rm -rf "$W"; mkdir -p "$W"
find "$X/tree" -name module-info.java -delete
P=jdk/internal/m3/indexstring
for side in live leaf; do
  S=$W/$side; mkdir -p "$S"; cp -r "$X/tree" "$S/t"; M=$S/t/m3/ports/indexstring
  MAINS=$(grep -o 'jdk\.internal\.m3\.indexstring\.[A-Za-z0-9]*' "$M/pom.xml" | sort -u)
  if [ $side = leaf ]; then
    cp "$X"/main/*.java "$M/src/main/java/$P/"
    cp "$H/../../src/test/java/$P/MIndexWordBreakSmokeMain.java" "$M/src/test/java/$P/"
    MAINS="$MAINS jdk.internal.m3.indexstring.MIndexWordBreakSmokeMain"
  fi
  SP="$S/t/m3/collections/src/main/java;$S/t/m3/arrays/src/main/java;$S/t/m3/ports/precompute/src/main/java"
  find "$M/src/main/java" -name '*.java' > "$S/main.lst"
  "$J/javac" --release 21 -Xlint:all,-module -Werror -proc:none -encoding UTF-8 -implicit:class -sourcepath "$SP" -d "$S/cls" @"$S/main.lst" > "$S/main.log" 2>&1
  echo "$side main javac rc=$? errors=$(grep -c ': error:' "$S/main.log") warnings=$(grep -c ': warning:' "$S/main.log") files=$(wc -l < "$S/main.lst")"
  find "$M/src/test/java" -name '*.java' > "$S/test.lst"
  "$J/javac" --release 21 -Xlint:all -Werror -proc:none -encoding UTF-8 -cp "$S/cls" -d "$S/tcls" @"$S/test.lst" > "$S/test.log" 2>&1
  echo "$side test javac rc=$? errors=$(grep -c ': error:' "$S/test.log") warnings=$(grep -c ': warning:' "$S/test.log")"
  for m in $MAINS; do
    "$J/java" -cp "$S/cls;$S/tcls" $m > "$S/${m##*.}.out" 2>&1
    echo "$side ${m##*.} rc=$? $(tail -1 "$S/${m##*.}.out" | cut -c1-110)"
  done
done
"$J/javac" --release 21 -nowarn -proc:none -encoding UTF-8 -d "$W/dcls" "$H"/stub/*.java "$X"/donor/*.java > "$W/donor.log" 2>&1
echo "donor javac rc=$? errors=$(grep -c ': error:' "$W/donor.log")"
"$J/javac" --release 21 -nowarn -proc:none -encoding UTF-8 -cp "$W/dcls;$W/leaf/cls" -d "$W/hcls" "$H/Ix2Diff.java" > "$W/h.log" 2>&1 || { cat "$W/h.log"; exit 1; }
for seed in 20261107 7; do "$J/java" -cp "$W/hcls;$W/dcls;$W/leaf/cls" Ix2Diff $seed 20000; done
"$J/javac" --release 21 -nowarn -proc:none -encoding UTF-8 -implicit:class -cp "$W/leaf/cls" \
  -sourcepath "$X/tree/m3/algorithms/src/main/java;$X/tree/m3/collections/src/main/java" -d "$W/ncls" "$H/NecWordBreak.java" > "$W/n.log" 2>&1 || { cat "$W/n.log"; exit 1; }
"$J/java" -cp "$W/ncls;$W/leaf/cls" NecWordBreak
