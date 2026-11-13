#!/bin/bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# spatial_gate.sh <master-sha> <donor-checkout> <donor-rev> <scratch-dir>
# CPU_TOOLS_PACK_GATE v2 (../cpu-structured-waves-20261106/waves_gate.sh) for one leaf, plus the split and differential
# gates of M3PackedSpatialIndex. Run from a checkout of the leaf (the working copy is the leaf side). Steps:
#   0 recipe (CRATE_CHAIN set): crate_chain2.py reproduces the class bytes; split_contract.py --check is a
#     fixed point; a tampered SPLIT.tsv pin refuses with exit 2 and writes nothing
#   1 live (master packs) and leaf (working copy packs), each: java.base Progress*/Monitored* patch, jdk.compiler M3
#     owner patch, modules javac -Xlint:all,-module -Werror, gcc -Werror m3textio/m3textops, build.py FIXTURES with
#     -Xcheck:jni; no fixture passing on live may fail on leaf
#   2 new fixture M3PackedSpatialIndexContract on leaf modules: JIT and -Xint, -Xcheck:jni
#   3 donor figure: the unsplit donor contract with the donor classes (scratch only), JIT and -Xint
#   4 differential: donor vs relocated dumps (JIT and -Xint) byte-identical; linear-scan reference and the point
#     filter M3SpatialQueries agree with the relocated class (0 mismatching lines)
#   5 mutants: one-line mutants of the relocated class against the contract and the differential
# Every JVM gets -Djava.io.tmpdir=$TMPDIR_M3 (default S:/m3-scratch/eb-conv/spatial/tmp); TMP/TEMP point there too.
set -u
R=${1:?master-sha}; DG=${2:?donor-checkout}; DR=${3:?donor-rev}; X=${4:?scratch}
HERE=$(cd "$(dirname "$0")" && pwd); ROOT=$(cd "$HERE/../../.." && pwd)
J="${JDK21_HOME:-C:/Program Files/Java/jdk-21}/bin"; JI="${JDK21_HOME:-C:/Program Files/Java/jdk-21}/include"
TD=${TMPDIR_M3:-S:/m3-scratch/eb-conv/spatial/tmp}; mkdir -p "$TD"; export TMP="$TD" TEMP="$TD" TMPDIR="$TD"
JT="-Djava.io.tmpdir=$TD"; JCT="-J-Djava.io.tmpdir=$TD"
T=m3/tooling/module-packs; CT=$T/cpu-tools; PKG=jdk/m3/tools/cpu
rm -rf "$X"; mkdir -p "$X"; A=$(cygpath -m "$X")
row() { awk -F'\t' -v r="$1" -v c="$2" '$1 == r {print $c}' "$HERE/PROVENANCE.tsv"; }

echo "== 0 recipe"
# crate_chain2.py read the crate over gh api from the donor's develop branch. It was retired on 2026-11-10 (standalone
# rule) to m3/qualification/master-landing-20261028/retained-variants/pr2932/m3/qualification/cpu-structured-waves-
# 20261106/crate_chain2.py; its verdict stays in VERDICTS.tsv. Step 0 runs only when CRATE_CHAIN names a copy of it.
if [ -n "${CRATE_CHAIN:-}" ]; then
CRATE=$(dirname "$(row CLASS 4)"); CRATE_TEXT=$(echo "$CRATE" | sed 's#/hash-pinned-java/#/hash-pinned-text/#')
python -I "$CRATE_CHAIN" "$CRATE,$CRATE_TEXT" \
  "$(git -C "$ROOT" rev-parse --path-format=absolute --git-common-dir)" "$R" \
  "$(git -C "$ROOT" rev-parse fe35ea98841^)" "$ROOT/m3/tooling/jdk-naming" "$X/chain" > "$X/chain.out"
grep -F "$(basename "$(row CLASS 7)")" "$X/chain.out"
cmp "$X/chain/$(row CLASS 7)" "$ROOT/$(row CLASS 7)" && echo "class bytes == chain output (NEW)"
CIN="$X/chain/$CT/fixtures/$(basename "$(row CONTRACT 4)" .txt)"
python -I "$HERE/split_contract.py" "$CIN" "$ROOT/$(row CONTRACT 7)" --check; echo "split --check rc=$?"
mkdir -p "$X/tamper"; cp "$HERE/split_contract.py" "$X/tamper/"
sed 's/^\(DROP\tD3_FLOW_CASES\t55-94\t1\t\)780d364480c5699a/\1780d364480c5699b/' "$HERE/SPLIT.tsv" > "$X/tamper/SPLIT.tsv"
python -I "$X/tamper/split_contract.py" "$CIN" "$X/tamper/out.java"; r=$?
echo "tampered pin rc=$r written=$([ -e "$X/tamper/out.java" ] && echo yes || echo no)"

else echo "step 0 skipped: CRATE_CHAIN unset (standalone rule; verdict in VERDICTS.tsv)"; fi
echo "== 1 live/leaf"
W=$X/g; mkdir -p "$W/live" "$W/leaf/$T" "$W/jb/java/util/concurrent" "$W/csrc" "$W/empty"
git -C "$ROOT" archive "$R" $T/text-tools/src $T/text-tools/native $T/text-tools/fixtures $CT/src $CT/fixtures \
  | tar -x -C "$W/live"
for d in text-tools/src text-tools/native text-tools/fixtures cpu-tools/src cpu-tools/fixtures; do
  mkdir -p "$W/leaf/$T/$(dirname $d)"; cp -r "$ROOT/$T/$d" "$W/leaf/$T/$(dirname $d)/"; done
git -C "$ROOT" ls-tree --name-only "$R" src/java.base/share/classes/java/util/concurrent/ \
  | grep -E '/(Progress|Monitored)[A-Za-z]*\.java$' \
  | while read -r f; do git -C "$ROOT" show "$R:$f" > "$W/jb/java/util/concurrent/$(basename "$f")"; done
"$J/javac" $JCT -nowarn --patch-module java.base="$A/g/jb" -d "$W/jbcls" $(find "$W/jb" -name '*.java') \
  > "$W/jb.log" 2>&1 || { echo JB_FAIL; exit 1; }
for f in com/sun/source/util/M3AST.java com/sun/tools/javac/m3/M3ASTPC.java com/sun/tools/javac/m3/M3CompilerPatternHints.java \
    com/sun/tools/javac/m3/M3PrimitiveImageCompiler.java com/sun/tools/javac/main/JavaCompiler.java; do
  mkdir -p "$W/csrc/$(dirname $f)"; git -C "$ROOT" show "$R:src/jdk.compiler/share/classes/$f" > "$W/csrc/$f"; done
"$J/javac" $JCT -source 21 -target 21 -proc:none -implicit:none -nowarn --patch-module jdk.compiler="$A/g/csrc" \
  -sourcepath "$A/g/empty" -d "$W/cpatch" $(find "$W/csrc" -name '*.java') > "$W/cpatch.log" 2>&1 || { echo CPATCH_FAIL; exit 1; }
PJ="--patch-module java.base=$A/g/jbcls --patch-module jdk.compiler=$A/g/cpatch"
FIX=$(python -I -c "import ast,re,sys;s=open(sys.argv[1],encoding='utf-8').read();print(' '.join(ast.literal_eval(re.search(r'^FIXTURES = (\(.*?\))\n', s, re.S|re.M).group(1))))" "$ROOT/$CT/build.py")
for side in live leaf; do
  S=$W/$side; AS=$A/g/$side
  "$J/javac" $JCT -source 21 -target 21 -Xlint:all,-module -Werror $PJ \
    --module-source-path "$AS/$T/text-tools/src;$AS/$CT/src" -d "$S/modules" \
    $(find "$S/$T/text-tools/src" "$S/$CT/src" -name '*.java') > "$S/modules.log" 2>&1
  echo "$side modules javac rc=$? errors=$(grep -c ': error:' "$S/modules.log") warnings=$(grep -c ': warning:' "$S/modules.log") classes=$(find "$S/modules" -name '*.class' | wc -l)"
  mkdir -p "$S/nat"
  for c in m3textio m3textops; do
    gcc -std=c11 -Wall -Wextra -Werror -shared -I"$JI" -I"$JI/win32" -I"$S/$T/text-tools/native" -o "$S/nat/$c.dll" \
      "$S/$T/text-tools/native/$c.c" > "$S/nat/$c.log" 2>&1
    echo "$side gcc $c rc=$? diagnostics=$(grep -cE 'warning|error' "$S/nat/$c.log")"
  done
  MODS=$(ls "$S/modules" | tr '\n' ',' | sed 's/,$//')
  "$J/javac" $JCT -source 21 -target 21 -nowarn $PJ --module-path "$AS/modules" --add-modules "$MODS" -d "$S/tcls" \
    $(for f in $FIX; do printf '%s ' "$S/$CT/fixtures/$f.java"; done) > "$S/fix.log" 2>&1
  echo "$side fixtures javac rc=$? errors=$(grep -c ': error:' "$S/fix.log")"
  : > "$S/fixtures.out"
  for f in $FIX; do
    (cd "$S" && timeout 600 "$J/java" $JT -Xcheck:jni $PJ -Djava.library.path="$AS/nat" --module-path "$AS/modules" \
      --add-modules "$MODS" -cp "$AS/tcls" $f > "run-$f.log" 2>&1); rc=$?
    printf '%s\t%s\t%s\n' "$f" "$rc" "$(tail -1 "$S/run-$f.log" | cut -c1-100)" >> "$S/fixtures.out"
  done
  echo "$side build.py FIXTURES rc0=$(awk -F'\t' '$2==0' "$S/fixtures.out" | wc -l)/$(wc -l < "$S/fixtures.out") failing=$(awk -F'\t' '$2!=0{printf "%s ", $1}' "$S/fixtures.out")"
done
echo "regressions (rc 0 on live, not on leaf): $(join -t$'\t' <(cut -f1,2 "$W/live/fixtures.out" | sort) <(cut -f1,2 "$W/leaf/fixtures.out" | sort) | awk -F'\t' '$2==0 && $3!=0' | wc -l)"

echo "== 2 new fixture"
S=$W/leaf; AS=$A/g/leaf; MODS=$(ls "$S/modules" | tr '\n' ',' | sed 's/,$//'); NF=M3PackedSpatialIndexContract
"$J/javac" $JCT -source 21 -target 21 -Xlint:all -Werror $PJ --module-path "$AS/modules" --add-modules "$MODS" \
  -d "$S/newfx" "$S/$CT/fixtures/$NF.java" > "$S/newfx.log" 2>&1
echo "new fixture javac -Xlint:all -Werror rc=$? diagnostics=$(grep -cE ': (error|warning):' "$S/newfx.log")"
for mode in jit xint; do
  "$J/java" $JT -Xcheck:jni $([ $mode = xint ] && echo -Xint) $PJ --module-path "$AS/modules" --add-modules "$MODS" \
    -cp "$AS/newfx" $NF > "$S/newfx-$mode.log" 2>&1; echo "NEWFX $NF $mode rc=$? $(tail -1 "$S/newfx-$mode.log")"
done

echo "== 3 donor figure"
D=$X/donor; mkdir -p "$D/src" "$D/out"
for r in CLASS CONTRACT DONOR_ONLY; do
  p=$(row $r 4); git -C "$DG" show "$DR:$p" > "$D/src/$(basename "$p" .txt)"
  echo "donor $(basename "$p") blob=$(git -C "$DG" rev-parse "$DR:$p" | cut -c1-12) sha256=$(sha256sum "$D/src/$(basename "$p" .txt)" | cut -c1-12)"
done
DONOR=$(sed -n 's/^package \(.*\);$/\1/p' "$D/src/M3PackedSpatialIndex.java").M3PackedSpatialIndex
"$J/javac" $JCT --release 21 -proc:none -d "$D/out" "$D"/src/*.java > "$D/javac.log" 2>&1; echo "donor javac rc=$?"
DC=$(basename "$(row CONTRACT 4)" .java.txt)
for mode in jit xint; do
  echo "DONOR_$mode $("$J/java" $JT $([ $mode = xint ] && echo -Xint) -cp "$A/donor/out" "$DC" 2>&1 | tail -1)"
done

echo "== 4 differential"
F=$X/diff; mkdir -p "$F/out"; DIFF=M3PackedSpatialIndexDifferential
"$J/javac" $JCT --release 21 -Xlint:all -Werror -d "$F/out" "$HERE/oracle/$DIFF.java" > "$F/javac.log" 2>&1
echo "differential javac -Xlint:all -Werror rc=$? diagnostics=$(grep -cE ': (error|warning):' "$F/javac.log")"
MP="$PJ --module-path $AS/modules --add-modules jdk.m3.tools.cpu"
for mode in jit xint; do
  XI=$([ $mode = xint ] && echo -Xint)
  echo "donor_$mode     $("$J/java" $JT $XI -cp "$A/donor/out;$A/diff/out" $DIFF "$DONOR" "$F/donor-$mode.dump")"
  echo "relocated_$mode $("$J/java" $JT $XI $MP -cp "$A/diff/out" $DIFF jdk.m3.tools.cpu.M3PackedSpatialIndex "$F/relocated-$mode.dump")"
done
echo "brute          $("$J/java" $JT -cp "$A/diff/out" $DIFF BRUTE "$F/brute.dump")"
echo "point_filter   $("$J/java" $JT $MP -cp "$A/diff/out" $DIFF jdk.m3.tools.cpu.M3SpatialQueries "$F/points.dump")"
echo "distinct dump sha256 over donor/relocated x jit/xint: $(sha256sum "$F"/donor-*.dump "$F"/relocated-*.dump | cut -c1-64 | sort -u | wc -l)"
grep -E '^[CA] ' "$F/relocated-jit.dump" > "$F/relocated-A.dump"
echo "relocated vs linear scan: lines=$(wc -l < "$F/brute.dump") mismatching=$(diff "$F/relocated-A.dump" "$F/brute.dump" | grep -c '^[<>]')"
grep -E '^[CA] POINT1 ' "$F/relocated-jit.dump" > "$F/relocated-P.dump"
echo "relocated vs M3SpatialQueries (POINT1): lines=$(wc -l < "$F/points.dump") mismatching=$(diff "$F/relocated-P.dump" "$F/points.dump" | grep -c '^[<>]')"
echo "B lines: $(grep -c '^B ' "$F/relocated-jit.dump") (refusals: $(grep '^B ' "$F/relocated-jit.dump" | grep -c 'throw:'))"

echo "== 5 mutants"
OWN="$ROOT/$(row CLASS 7)"; REF=$(sha256sum "$F/donor-jit.dump" | cut -c1-64)
mutant() {  # name old new: one exact occurrence, else refuse
  local M=$X/mut/$1; mkdir -p "$M/src" "$M/out"
  python -I -c 'import sys; s=open(sys.argv[1],encoding="utf-8").read(); n=s.count(sys.argv[2]); assert n==1, n; open(sys.argv[3],"w",encoding="utf-8",newline="").write(s.replace(sys.argv[2],sys.argv[4]))' \
    "$OWN" "$2" "$M/src/M3PackedSpatialIndex.java" "$3" || { echo "MUTANT $1 REFUSED"; return 1; }
  "$J/javac" $JCT --release 21 -proc:none -d "$M/out" "$M/src/M3PackedSpatialIndex.java" "$ROOT/$CT/fixtures/$NF.java" "$HERE/oracle/$DIFF.java" > "$M/javac.log" 2>&1
  local AM; AM=$(cygpath -m "$M")
  local c d; c=$("$J/java" $JT -cp "$AM/out" $NF 2>&1 | grep -oE 'PASS checks=[0-9]+|AssertionError[^[:cntrl:]]{0,50}|[A-Za-z]+Exception[^[:cntrl:]]{0,50}' | head -1)
  "$J/java" $JT -cp "$AM/out" $DIFF jdk.m3.tools.cpu.M3PackedSpatialIndex "$M/m.dump" > /dev/null 2>&1
  d=$([ -f "$M/m.dump" ] && [ "$(sha256sum "$M/m.dump" | cut -c1-64)" = "$REF" ] && echo same || echo differs)
  echo "MUTANT $1 contract=[$c] differential=$d"
}
mutant M1_touching_edge "&& maxX[id] > queryMinX" "&& maxX[id] >= queryMinX"
mutant M2_search_bound "if (minX[sorted[middle]] < queryMaxX)" "if (minX[sorted[middle]] <= queryMaxX)"
mutant M3_unsorted_ids "Arrays.sort(matching, 0, used);" ""
mutant M4_zero_width "if (minX[id] < maxX[id] && minY[id] < maxY[id]" "if (minY[id] < maxY[id]"
mutant M5_aliased_lanes "return new Index(minX.clone()," "return new Index(minX,"
echo "== done"
