#!/bin/bash
# coe_gate.sh <master-sha> : CPU_TOOLS_PACK_GATE v2 (OVERLAY=<leaf overlay dir>, default coe/), mirroring module-packs/cpu-tools/build.py on stock JDK 21.
# live = master's text-tools + cpu-tools packs; leaf = live + coe/ (the HASH_PINNED_CHAIN v2 outputs of crate
# m3-cpu-owner-extensions-20261105, through jdk_naming). Per side:
#   jb:       java.base patched with master's java.util.concurrent Progress*/Monitored* sources (the M3 API both packs use)
#   cpatch:   jdk.compiler patched with master's five M3 javac owners (M3AST is used by the source-facts tools)
#   modules:  javac -source/-target 21 -Xlint:all,-module -Werror --module-source-path text-tools/src;cpu-tools/src
#   native:   gcc -std=c11 -Wall -Wextra -Werror -shared m3textio.c (+ its new headers) and m3textops.c
#   fixtures: javac the build.py FIXTURES (+ M3TextSmoke), run each with -Xcheck:jni and the side's natives
# Pass = leaf compiles clean, natives build clean, and no fixture that passes on live fails on leaf.
cd "$(dirname "$0")"
J="C:/Program Files/Java/jdk-21/bin"; JI="C:/Program Files/Java/jdk-21/include"; A=$(cygpath -m "$PWD")
G=/h/m3-scratch/repos/M3jdk21-release.git; R=${1:?master}; T=m3/tooling/module-packs
W=coeg; rm -rf $W; mkdir -p $W/tree $W/jb/java/util/concurrent
GIT_DIR=$G git ls-tree -r $R -- $T/text-tools/src $T/text-tools/native $T/text-tools/fixtures $T/cpu-tools/src $T/cpu-tools/fixtures | awk '{print $3}' | GIT_DIR=$G git fetch -q --filter=blob:none --stdin origin 2>/dev/null
GIT_DIR=$G git ls-tree -r $R -- $T/text-tools/src $T/text-tools/native $T/text-tools/fixtures $T/cpu-tools/src $T/cpu-tools/fixtures | while IFS=$'\t' read -r meta p; do
  mkdir -p "$W/tree/$(dirname "$p")"; GIT_DIR=$G git cat-file blob ${meta##* } > "$W/tree/$p"; done
GIT_DIR=$G git ls-tree --name-only $R src/java.base/share/classes/java/util/concurrent/ | grep -E '/(Progress|Monitored)[A-Za-z]*\.java$' |
  while read -r f; do GIT_DIR=$G git show "$R:$f" > $W/jb/java/util/concurrent/$(basename $f); done
"$J/javac" -nowarn --patch-module java.base=$W/jb -d $W/jbcls $(find $W/jb -name '*.java') > $W/jb.log 2>&1 || { echo JB_FAIL; exit 1; }
# cpatch: master's five M3 javac owners (M3AST and friends) as a jdk.compiler patch, as parser/build.py compiles them
mkdir -p $W/csrc $W/empty
for f in com/sun/source/util/M3AST.java com/sun/tools/javac/m3/M3ASTPC.java com/sun/tools/javac/m3/M3CompilerPatternHints.java com/sun/tools/javac/m3/M3PrimitiveImageCompiler.java com/sun/tools/javac/main/JavaCompiler.java; do
  mkdir -p $W/csrc/$(dirname $f); GIT_DIR=$G git show $R:src/jdk.compiler/share/classes/$f > $W/csrc/$f; done
"$J/javac" -source 21 -target 21 -proc:none -implicit:none -nowarn --patch-module jdk.compiler="$A/$W/csrc" -sourcepath "$A/$W/empty"   -d $W/cpatch $(find $W/csrc -name '*.java') > $W/cpatch.log 2>&1 || { echo CPATCH_FAIL; exit 1; }
PJ="--patch-module java.base=$A/$W/jbcls --patch-module jdk.compiler=$A/$W/cpatch"
FIX=$(python -I -c "import ast,re;s=open('cpu_build.py',encoding='utf-8').read();print(' '.join(ast.literal_eval(re.search(r'^FIXTURES = (\(.*?\))\n', s, re.S|re.M).group(1))))")
for side in live leaf; do
  S=$W/$side; cp -r $W/tree $S; [ $side = leaf ] && cp -r "${OVERLAY:-coe}"/. $S/
  "$J/javac" -source 21 -target 21 -Xlint:all,-module -Werror $PJ \
    --module-source-path "$A/$S/$T/text-tools/src;$A/$S/$T/cpu-tools/src" -d $S/modules \
    $(find $S/$T/text-tools/src $S/$T/cpu-tools/src -name '*.java') > $S/modules.log 2>&1
  echo "$side modules javac rc=$? errors=$(grep -c ': error:' $S/modules.log) warnings=$(grep -c ': warning:' $S/modules.log)"
  mkdir -p $S/nat
  for c in m3textio m3textops; do
    gcc -std=c11 -Wall -Wextra -Werror -shared -I"$JI" -I"$JI/win32" -I$S/$T/text-tools/native -o $S/nat/$c.dll $S/$T/text-tools/native/$c.c > $S/nat/$c.log 2>&1
    echo "$side gcc $c rc=$? diagnostics=$(grep -cE 'warning|error' $S/nat/$c.log)"
  done
  MODS=$(ls $S/modules | tr '\n' ',' | sed 's/,$//')
  "$J/javac" -source 21 -target 21 -nowarn $PJ --module-path "$A/$S/modules" --add-modules "$MODS" \
    -d $S/tcls $(for f in $FIX; do printf '%s ' "$S/$T/cpu-tools/fixtures/$f.java"; done) > $S/fix.log 2>&1
  echo "$side fixtures javac rc=$? errors=$(grep -c ': error:' $S/fix.log)"
  : > $S/fixtures.out; mkdir -p $S/work  # build.py points java.io.tmpdir at an existing test-work dir
  for f in $FIX; do
    (cd $S && timeout 600 "$J/java" -Xcheck:jni $PJ -Djava.library.path="$A/$S/nat" -Djava.io.tmpdir="$A/$S/work" \
      --module-path "$A/$S/modules" --add-modules "$MODS" -cp "$A/$S/tcls" $f > run-$f.log 2>&1); rc=$?
    printf '%s\t%s\t%s\n' "$f" "$rc" "$(tail -1 $S/run-$f.log | cut -c1-100)" >> $S/fixtures.out
  done
  echo "$side fixtures rc0=$(awk -F'\t' '$2==0' $S/fixtures.out | wc -l)/$(wc -l < $S/fixtures.out)"
done
echo "regressions (rc 0 on live, not on leaf):"; join -t$'\t' <(cut -f1,2 $W/live/fixtures.out | sort) <(cut -f1,2 $W/leaf/fixtures.out | sort) | awk -F'\t' '$2==0 && $3!=0'
# v2 extra, leaf only: every new native kernel gcc -Werror (+ its kernel oracle under native/tests when present),
# then every overlay fixture main run with the six wave libraries wired through their properties.
S=$W/leaf; N=$S/wavenat; mkdir -p $N
for c in $(ls "${OVERLAY:-coe}"/m3/tooling/module-packs/cpu-tools/native/*.c 2>/dev/null); do
  n=$(basename $c .c)
  gcc -std=c11 -Wall -Wextra -Werror -shared -O2 -I"$JI" -I"$JI/win32" -o $N/$n.dll $c > $N/$n.log 2>&1
  echo "wave gcc $n rc=$? diagnostics=$(grep -cE 'warning|error' $N/$n.log)"
done
for c in $(ls "${OVERLAY:-coe}"/m3/tooling/module-packs/cpu-tools/native/tests/*.c 2>/dev/null); do
  n=$(basename $c .c)
  gcc -std=c11 -Wall -Wextra -Werror -O2 -I"$JI" -I"$JI/win32" -I"${OVERLAY:-coe}"/m3/tooling/module-packs/cpu-tools/native -o $N/$n.exe $c > $N/$n.log 2>&1
  r=$?; [ $r = 0 ] && { timeout 300 $N/$n.exe > $N/$n.run 2>&1; r2=$?; } || r2=-
  echo "kernel-oracle $n build=$r run=$r2 $(tail -1 $N/$n.run 2>/dev/null | cut -c1-80)"
done
P="-Dm3.cpu.radix.native.path=$A/$N/m3_byte_radix_trie.dll -Dm3.cpu.suffix.native.path=$A/$N/m3_byte_suffix_index.dll"
P="$P -Dm3.cpu.multi.ranked.native.path=$A/$N/m3_multi_ranked_substrings.dll -Dm3.cpu.ranked.native.path=$A/$N/m3_ranked_byte_substrings.dll"
P="$P -Dm3.cpu.multiset.native.path=$A/$N/m3_sorted_int_multiset.dll -Dm3.cpu.sorted.native.path=$A/$N/m3_sorted_int_set.dll"
FXDIR="${OVERLAY:-coe}"/m3/tooling/module-packs/cpu-tools/fixtures
PKGFX=$(grep -l '^package jdk.m3.tools.cpu;' $FXDIR/*.java 2>/dev/null | xargs -n1 basename 2>/dev/null | sed 's/\.java$//')
NEWFX=$(grep -L '^package ' $FXDIR/*.java 2>/dev/null | xargs -n1 basename | sed 's/\.java$//')
"$J/javac" -source 21 -target 21 -nowarn $PJ --module-path "$A/$S/modules" --add-modules "$MODS" -d $S/newfx \
  $(for f in $NEWFX; do printf '%s ' "$S/$T/cpu-tools/fixtures/$f.java"; done) > $S/newfx.log 2>&1
echo "new fixtures javac rc=$? errors=$(grep -c ': error:' $S/newfx.log)"
# in-package oracles (package jdk.m3.tools.cpu) compile and run as a patch of that module, as the wave verify scripts do
if [ -n "$PKGFX" ]; then
  "$J/javac" -source 21 -target 21 -nowarn $PJ --module-path "$A/$S/modules" --add-modules "$MODS" \
    --patch-module jdk.m3.tools.cpu="$A/$S/$T/cpu-tools/fixtures" -d $S/pkgfx \
    $(for f in $PKGFX; do printf '%s ' "$S/$T/cpu-tools/fixtures/$f.java"; done) > $S/pkgfx.log 2>&1
  echo "in-package fixtures javac rc=$? errors=$(grep -c ': error:' $S/pkgfx.log)"
fi
for f in $NEWFX; do
  (cd $S && timeout 600 "$J/java" -Xcheck:jni $PJ $P -Djava.io.tmpdir="$A/$S/work" --module-path "$A/$S/modules" \
     --add-modules "$MODS" -cp "$A/$S/newfx" $f > newfx-$f.log 2>&1); rc=$?
  printf 'NEWFX\t%s\trc=%s\t%s\n' "$f" "$rc" "$(tail -1 $S/newfx-$f.log | cut -c1-110)"
done
for f in $PKGFX; do
  (cd $S && timeout 600 "$J/java" -Xcheck:jni $PJ $P -Djava.io.tmpdir="$A/$S/work" --module-path "$A/$S/modules" \
     --patch-module jdk.m3.tools.cpu="$A/$S/pkgfx" --add-modules "$MODS" -m jdk.m3.tools.cpu/jdk.m3.tools.cpu.$f > pkgfx-$f.log 2>&1); rc=$?
  printf 'NEWFX\t%s\trc=%s\t%s\n' "$f" "$rc" "$(tail -1 $S/pkgfx-$f.log | cut -c1-110)"
done) > $S/newfx.log 2>&1
echo "new fixtures javac rc=$? errors=$(grep -c ': error:' $S/newfx.log)"
for f in $NEWFX; do
  (cd $S && timeout 600 "$J/java" -Xcheck:jni $PJ $P -Djava.io.tmpdir="$A/$S/work" --module-path "$A/$S/modules" \
     --add-modules "$MODS" -cp "$A/$S/newfx" $f > newfx-$f.log 2>&1); rc=$?
  printf 'NEWFX\t%s\trc=%s\t%s\n' "$f" "$rc" "$(tail -1 $S/newfx-$f.log | cut -c1-110)"
done
