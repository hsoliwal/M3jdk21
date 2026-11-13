#!/bin/bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# gate103.sh <tree> <fix-dir> <out> : MERGELOSS2_LEAF103_GATE v1, the phases of m3/tooling/module-packs/parser/build.py
# (after m3/qualification/loss-guard-restore-20261106/m3astpc-pr2357/parser_pack_gate.sh) on stock JDK 21.
#   <tree>    archive of master: src/jdk.compiler/share/classes, m3/tooling/module-packs/parser and
#             m3/qualification/master-landing-20261028/retained-variants/pr1559
#   <fix-dir> M3AST.java, M3ASTPC.java, JavaCompiler.java as written by apply.py
# Per side (live = master, fix = live + the three restored owners):
#   compiler-patch  javac -proc:none -implicit:none -Xlint:all -Werror --patch-module jdk.compiler over build.py's
#                   five COMPILER_SOURCES (empty sourcepath)
#   parser-module   javac -Xlint:all -Xlint:-module -Werror of the pack's src/main/java as module jdk.m3.parser
#   all-consumers   javac -Xlint:all -Werror of the pack's src/test/java against the module
#   contracts       every build.py CONTRACTS row in its own JVM; PASS = its marker printed
# fix only: the origin's own M3ASTScopeContract (retained-variants/pr1559, not committed) compiled and run the same way.
set -u
J="${JAVA21:-C:/Program Files/Java/jdk-21}/bin"; T=$(cygpath -m "$1"); FIX=$(cygpath -m "$2"); O=$(cygpath -m "$3")
P=m3/tooling/module-packs/parser; C=src/jdk.compiler/share/classes
M2=S:/m3-scratch/eb-conv/m2; EXTRA="$M2/org/xerial/sqlite-jdbc/3.45.1.0/sqlite-jdbc-3.45.1.0.jar;$M2/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar"
OWNERS="com/sun/source/util/M3AST.java com/sun/tools/javac/m3/M3ASTPC.java com/sun/tools/javac/m3/M3CompilerPatternHints.java com/sun/tools/javac/m3/M3PrimitiveImageCompiler.java com/sun/tools/javac/main/JavaCompiler.java"
TMPD=S:/m3-scratch/eb-conv/ml2singles/tmp
mkdir -p "$O/empty"
python -I - "$T/$P/build.py" > "$O/contracts.tsv" <<'PY'
import ast, re, sys
src = open(sys.argv[1], encoding="utf-8").read()
for cls, marker in ast.literal_eval(re.search(r"^CONTRACTS = (\(.*?^\))", src, re.S | re.M).group(1)):
    print(f"{cls}\t{marker}")
PY
for side in live fix; do
  S="$O/$side"; mkdir -p "$S/src"
  for f in $OWNERS; do mkdir -p "$S/src/$(dirname $f)"; cp "$T/$C/$f" "$S/src/$f"; done
  if [ $side = fix ]; then
    cp "$FIX/M3AST.java" "$S/src/com/sun/source/util/M3AST.java"
    cp "$FIX/M3ASTPC.java" "$S/src/com/sun/tools/javac/m3/M3ASTPC.java"
    cp "$FIX/JavaCompiler.java" "$S/src/com/sun/tools/javac/main/JavaCompiler.java"
  fi
  "$J/javac" -source 21 -target 21 -proc:none -implicit:none -Xlint:all -Werror --patch-module jdk.compiler="$S/src" \
    -sourcepath "$O/empty" -d "$S/cpatch" $(for f in $OWNERS; do printf '%s ' "$S/src/$f"; done) > "$S/cpatch.log" 2>&1
  echo "$side compiler-patch rc=$? errors=$(grep -c ': error:' "$S/cpatch.log") warnings=$(grep -c ': warning:' "$S/cpatch.log") classes=$(find "$S/cpatch" -name '*.class' | wc -l)"
  "$J/javac" -source 21 -target 21 -proc:none -implicit:none -Xlint:all -Werror -Xlint:-module --patch-module jdk.compiler="$S/cpatch" \
    -d "$S/modules/jdk.m3.parser" $(find "$T/$P/src/main/java" -name '*.java') > "$S/main.log" 2>&1
  echo "$side parser-module rc=$? errors=$(grep -c ': error:' "$S/main.log") warnings=$(grep -c ': warning:' "$S/main.log")"
  "$J/javac" -source 21 -target 21 -proc:none -implicit:none -Xlint:all -Werror --patch-module jdk.compiler="$S/cpatch" \
    --module-path "$S/modules" --add-modules jdk.m3.parser -d "$S/tcls" $(find "$T/$P/src/test/java" -name '*.java') > "$S/test.log" 2>&1
  echo "$side all-consumers rc=$? errors=$(grep -c ': error:' "$S/test.log") warnings=$(grep -c ': warning:' "$S/test.log")"
  : > "$S/contracts.out"
  while IFS=$'\t' read -r cls marker; do
    marker=${marker%$'\r'}; mkdir -p "$S/work"
    timeout 300 "$J/java" -ea -Djava.io.tmpdir=$TMPD -Dm3.parser.test.work="$S/work" -Dorg.sqlite.tmpdir="$S/work" --patch-module jdk.compiler="$S/cpatch" \
      --module-path "$S/modules" --add-modules jdk.m3.parser -cp "$S/tcls;$EXTRA" "$cls" > "$S/run.log" 2>&1
    if grep -q "$marker" "$S/run.log"; then r=PASS; else r="FAIL:$(grep -m1 -E 'Exception|Error' "$S/run.log" | cut -c1-120)"; fi
    printf '%s\t%s\n' "$cls" "$r" >> "$S/contracts.out"
  done < "$O/contracts.tsv"
  echo "$side contracts pass=$(grep -c $'\tPASS' "$S/contracts.out")/$(wc -l < "$S/contracts.out")"
done
echo "regressions (PASS on live, not on fix):"; join -t$'\t' <(sort "$O/live/contracts.out") <(sort "$O/fix/contracts.out") | awk -F'\t' '$2=="PASS" && $3!="PASS"'
S="$O/fix"; RV="$T/m3/qualification/master-landing-20261028/retained-variants/pr1559/$P/src/test/java/jdk/m3/parser/test"
"$J/javac" -source 21 -target 21 -proc:none -Xlint:all -Werror --patch-module jdk.compiler="$S/cpatch" --module-path "$S/modules" \
  --add-modules jdk.m3.parser -cp "$S/tcls" -d "$S/scope" "$RV/M3ASTScopeContract.java" > "$S/scope-javac.log" 2>&1
echo "fix origin M3ASTScopeContract javac rc=$? errors=$(grep -c ': error:' "$S/scope-javac.log") warnings=$(grep -c ': warning:' "$S/scope-javac.log")"
mkdir -p "$S/work"
timeout 600 "$J/java" -ea -Djava.io.tmpdir=$TMPD -Dm3.parser.test.work="$S/work" --patch-module jdk.compiler="$S/cpatch" --module-path "$S/modules" \
  --add-modules jdk.m3.parser -cp "$S/scope;$S/tcls;$EXTRA" jdk.m3.parser.test.M3ASTScopeContract > "$S/scope-run.log" 2>&1
echo "fix origin M3ASTScopeContract rc=$? last=$(tail -1 "$S/scope-run.log" | cut -c1-160)"
