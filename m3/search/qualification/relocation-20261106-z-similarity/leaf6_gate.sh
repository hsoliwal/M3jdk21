#!/bin/bash
# leaf6_gate.sh <master-sha> : Z_SIMILARITY_LEAF_GATE v1 (m3/search leaf 6).
#  1. search_gate.sh with LEAF_OUT=leaf6/out: javac -Xlint:all,-module -Werror over m3/search main (+ m3/algorithms,
#     m3/collections, m3/file on -sourcepath) and the test tree, live vs leaf.
#  2. native: gcc -shared -std=c11 -Wall -Wextra -Werror jni_search.c + jni_z_similarity.c -> m3search (as CMake does).
#  3. JUnit: M3ZSimilarityTest (Java) and the three *NativeTest classes with -Dm3.native.enabled=true
#     -Dm3.native.library.path=<m3search>, as the m3-search-native profile does.
#  4. ZDiff: brute force = donor sumJava (donor sources from bmh/develop/src) = relocated sumJava = relocated native.
cd "$(dirname "$0")/.."
J="C:/Program Files/Java/jdk-21/bin"; JI="C:/Program Files/Java/jdk-21/include"; A=$(cygpath -m "$PWD"); R=${1:?master}
CON="C:/Users/hsoliwal/.m2/repository/org/junit/platform/junit-platform-console-standalone/1.10.1/junit-platform-console-standalone-1.10.1.jar"
LEAF_OUT=leaf6/out bash search_gate.sh $R 2>&1 | grep -E 'javac'
S=l5g/leaf; N=leaf6/nat; rm -rf $N; mkdir -p $N
GIT_DIR=/h/m3-scratch/repos/M3jdk21-release.git git show $R:m3/search/src/main/native/jni_search.c > $N/jni_search.c  # master kernel (search_gate extracts Java only)
gcc -shared -O3 -std=c11 -Wall -Wextra -Werror -I"$JI" -I"$JI/win32" -o $N/m3search.dll \
  $N/jni_search.c leaf6/out/m3/search/src/main/native/jni_z_similarity.c > $N/gcc.log 2>&1
echo "gcc m3search (jni_search.c + jni_z_similarity.c) rc=$? diagnostics=$(grep -cE 'warning|error' $N/gcc.log)"
LIB="$A/$N/m3search.dll"
for t in M3ZSimilarityTest M3ZSimilarityNativeTest M3ZSimilarityPeriodTwoNativeTest M3ZSimilarityUniformNativeTest; do
  "$J/java" -Dm3.native.enabled=true -Dm3.native.library.path="$LIB" -jar "$CON" execute --class-path "$A/$S/cls;$A/$S/tcls" \
    --select-class jdk.m3.search.$t --disable-banner --details=summary 2>&1 | grep -E 'tests (successful|failed)' | tr -s ' ' | tr '\n' ' '
  echo "<- $t"
done
D=bmh/develop/src; rm -rf leaf6/dcls leaf6/zcls
LOMBOK=C:/Users/hsoliwal/.m2/repository/org/projectlombok/lombok/1.18.36/lombok-1.18.36.jar; SLF=C:/Users/hsoliwal/.m2/repository/org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9.jar
"$J/javac" -nowarn -encoding UTF-8 -cp "$LOMBOK;$SLF" -processorpath "$LOMBOK" -sourcepath "$A/$D/m3-fast-search/src/main/java;$A/$D/synexia-algo/src/main/java;$A/$D/synexia-native-interop/src/main/java;$A/$D/synexia-job/src/main/java;$A/$D/synexia-common/src/main/java;$A/$D/synexia-cop/src/main/java" \
  -d leaf6/dcls $D/m3-fast-search/src/main/java/com/synexia/m3/search/M3ZSimilarity.java > leaf6/donor.log 2>&1
echo "donor M3ZSimilarity javac rc=$? errors=$(grep -c ': error:' leaf6/donor.log)"
"$J/javac" -nowarn -proc:none -encoding UTF-8 -cp "$A/leaf6/dcls;$A/$S/cls" -d leaf6/zcls leaf6/ZDiff.java && \
for seed in 20261106 7; do
  "$J/java" -Dzdiff.native=true -Dm3.native.enabled=true -Dm3.native.library.path="$LIB" -cp "$A/leaf6/zcls;$A/leaf6/dcls;$A/$S/cls;$SLF" ZDiff $seed 20000
done
