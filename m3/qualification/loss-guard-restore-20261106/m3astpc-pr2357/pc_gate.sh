#!/bin/bash
# pc_gate.sh <master-sha> : M3ASTPC_RELATIONSHIPS_GATE v1. live = master's src/jdk.compiler/share/classes; fix = live
# with M3ASTPC.java replaced by the resolved pr2357 3-way (resolve_pc.py). Per side: javac -proc:none -Xlint:all
# -Werror --patch-module jdk.compiler=<tree> over the five M3 javac owners (M3AST, M3ASTPC, M3CompilerPatternHints,
# M3PrimitiveImageCompiler, JavaCompiler) with their implicit closure from the tree; then the parser pack
# (module jdk.m3.parser) against the patched classes, as module-packs/parser/build.py does.
cd "$(dirname "$0")"
J="C:/Program Files/Java/jdk-21/bin"; G=/h/m3-scratch/repos/M3jdk21-release.git; R=${1:?master}; A=$(cygpath -m "$PWD")
P=src/jdk.compiler/share/classes; F="com/sun/source/util/M3AST.java com/sun/tools/javac/m3/M3ASTPC.java com/sun/tools/javac/m3/M3CompilerPatternHints.java com/sun/tools/javac/m3/M3PrimitiveImageCompiler.java com/sun/tools/javac/main/JavaCompiler.java"
GIT_DIR=$G git ls-tree -r $R -- $P m3/tooling/module-packs/parser/src/main | awk '{print $3}' | GIT_DIR=$G git fetch -q --filter=blob:none --stdin origin 2>/dev/null
rm -rf jc.tar; GIT_NO_LAZY_FETCH=1 GIT_DIR=$G git archive --format=tar $R:$P > jc.tar
rm -rf parser && mkdir parser && GIT_NO_LAZY_FETCH=1 GIT_DIR=$G git archive --format=tar $R:m3/tooling/module-packs/parser/src/main/java | (cd parser && tar -xf -)
for side in live fix; do
  rm -rf $side && mkdir -p $side/tree && (cd $side/tree && tar -xf ../../jc.tar)
  [ $side = fix ] && cp M3ASTPC.java $side/tree/com/sun/tools/javac/m3/M3ASTPC.java
  "$J/javac" -source 21 -target 21 -proc:none -Xlint:all -Werror --patch-module jdk.compiler="$A/$side/tree" -d $side/cpatch \
    $(for f in $F; do printf '%s ' "$side/tree/$f"; done) > $side/jc.log 2>&1
  echo "$side jdk.compiler M3 owners javac rc=$? errors=$(grep -c ': error:' $side/jc.log) warnings=$(grep -c ': warning:' $side/jc.log) classes=$(find $side/cpatch -name '*.class' 2>/dev/null | wc -l)"
  "$J/javac" -source 21 -target 21 -proc:none -implicit:none -Xlint:all -Xlint:-module -Werror --patch-module jdk.compiler="$A/$side/cpatch" \
    -d $side/modules/jdk.m3.parser $(find parser -name '*.java') > $side/parser.log 2>&1
  echo "$side parser module javac rc=$? errors=$(grep -c ': error:' $side/parser.log) warnings=$(grep -c ': warning:' $side/parser.log)"
done
