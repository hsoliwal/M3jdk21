#!/bin/bash
# l7_gate.sh <master-sha> : M3AST_FASTNAMES_GATE v2 (census leaf L007), mirroring module-packs/parser/build.py's
# compile + test phases on stock JDK 21.0.9. live = master's parser pack (tree/); post = live + the crate's
# hash-pinned M3ASTIndex postimage + the new M3ASTBuilderOrdinalContract.
#   compiler-patch: javac -Xlint:all -Werror --patch-module jdk.compiler over master's five M3 javac owners
#                   (PC_FIX=<file> substitutes M3ASTPC.java)
#   parser-module:  javac -Xlint:all,-module -Werror the pack's src/main/java as module jdk.m3.parser
#   all-consumers:  javac -Xlint:all -Werror the pack's src/test/java against the module
#   test:           every CONTRACTS row of merged_build.py as its own JVM; PASS = its marker printed
# Pass = post compiles clean, no contract passing on live fails on post, and the new contract prints its marker.
cd "$(dirname "$0")"
J="C:/Program Files/Java/jdk-21/bin"; A=$(cygpath -m "$PWD"); P=m3/tooling/module-packs/parser
G=/h/m3-scratch/repos/M3jdk21-release.git; R=${1:?master}
M2=$(cygpath -m ~/.m2/repository); EXTRA="$(ls $M2/org/xerial/sqlite-jdbc/3.45.*/sqlite-jdbc-3.45.*.jar | tail -1);$(ls $M2/org/slf4j/slf4j-api/2.*/slf4j-api-2.*.jar | tail -1)"  # build.py: newest 3.45.x driver + slf4j-api
rm -rf cpatch-src && mkdir -p cpatch-src
for f in com/sun/source/util/M3AST.java com/sun/tools/javac/m3/M3ASTPC.java com/sun/tools/javac/m3/M3CompilerPatternHints.java com/sun/tools/javac/m3/M3PrimitiveImageCompiler.java com/sun/tools/javac/main/JavaCompiler.java; do
  mkdir -p cpatch-src/$(dirname $f); GIT_DIR=$G git show $R:src/jdk.compiler/share/classes/$f > cpatch-src/$f; done
[ -n "$PC_FIX" ] && cp "$PC_FIX" cpatch-src/com/sun/tools/javac/m3/M3ASTPC.java  # resolved pr2357 M3ASTPC (pc/M3ASTPC.java)
mkdir -p empty; rm -rf cpatch
"$J/javac" -source 21 -target 21 -proc:none -implicit:none -Xlint:all -Werror --patch-module jdk.compiler="$A/cpatch-src" \
  -sourcepath "$A/empty" -d cpatch $(find cpatch-src -name '*.java') > cpatch.log 2>&1
echo "compiler-patch rc=$? errors=$(grep -c ': error:' cpatch.log)"
python -I - <<'PY' > contracts.tsv
import ast, re
src = open("merged_build.py", encoding="utf-8").read()
body = re.search(r"^CONTRACTS = (\(.*?^\))", src, re.S | re.M).group(1)
for cls, marker in ast.literal_eval(body):
    print(f"{cls}\t{marker}")
PY
for side in live post; do
  S=$side; rm -rf $S; cp -r tree $S
  if [ $side = post ]; then
    cp M3ASTIndex.java.txt $S/$P/src/main/java/jdk/m3/parser/M3ASTIndex.java
    cp M3ASTBuilderOrdinalContract.java.txt $S/$P/src/test/java/jdk/m3/parser/test/M3ASTBuilderOrdinalContract.java
  fi
  "$J/javac" -source 21 -target 21 -proc:none -implicit:none -Xlint:all -Werror -Xlint:-module --patch-module jdk.compiler="$A/cpatch" \
    -d $S/modules/jdk.m3.parser $(find $S/$P/src/main/java -name '*.java') > $S/main.log 2>&1
  echo "$side parser-module rc=$? errors=$(grep -c ': error:' $S/main.log) warnings=$(grep -c ': warning:' $S/main.log)"
  "$J/javac" -source 21 -target 21 -proc:none -implicit:none -Xlint:all -Werror --patch-module jdk.compiler="$A/cpatch" \
    --module-path "$A/$S/modules" --add-modules jdk.m3.parser -d $S/tcls $(find $S/$P/src/test/java -name '*.java') > $S/test.log 2>&1
  echo "$side all-consumers rc=$? errors=$(grep -c ': error:' $S/test.log) warnings=$(grep -c ': warning:' $S/test.log)"
  : > $S/contracts.out
  while IFS=$'\t' read -r cls marker; do
    marker=${marker%$'\r'}  # python on Windows writes CRLF
    mkdir -p $S/work
    timeout 300 "$J/java" -ea -Dm3.parser.test.work="$A/$S/work" -Dorg.sqlite.tmpdir="$A/$S/work" --patch-module jdk.compiler="$A/cpatch" \
      --module-path "$A/$S/modules" --add-modules jdk.m3.parser -cp "$A/$S/tcls;$EXTRA" "$cls" > $S/run.log 2>&1
    if grep -q "$marker" $S/run.log; then r=PASS; else r="FAIL:$(grep -m1 -E 'Exception|Error' $S/run.log | cut -c1-100)"; fi
    printf '%s\t%s\n' "$cls" "$r" >> $S/contracts.out
  done < contracts.tsv
  echo "$side contracts pass=$(grep -c $'\tPASS' $S/contracts.out)/$(wc -l < $S/contracts.out)"
done
echo "regressions (PASS on live, not on post):"; join -t$'\t' <(sort live/contracts.out) <(sort post/contracts.out) | awk -F'\t' '$2=="PASS" && $3!="PASS"'
grep M3ASTBuilderOrdinalContract post/contracts.out
