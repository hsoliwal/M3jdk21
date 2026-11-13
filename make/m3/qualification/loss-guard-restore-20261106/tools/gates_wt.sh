#!/bin/bash
# gates on the restored worktree (stock JDK 21; Maven lanes in gates_mvn.sh)
set -u
WT=/s/m3-scratch/eb-conv/wt-restore; WTW=S:/m3-scratch/eb-conv/wt-restore
G=/s/m3-scratch/eb-conv/restore/gateWT; GW=S:/m3-scratch/eb-conv/restore/gateWT
J="C:/Program Files/Java/jdk-21/bin"; JH="C:/Program Files/Java/jdk-21"
CS=S:/m3-scratch/eb-conv/m2/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar
API=S:/m3-scratch/eb-conv/m2/org/junit/jupiter/junit-jupiter-api/5.11.4/junit-jupiter-api-5.11.4.jar
log() { echo "$(date +%H:%M:%S) $*" >> $G/gate.log; }
: > $G/gate.log; log start
# A. hash lane
mkdir -p $G/hash/oracle $G/hash/psrc/jdk/internal/m3/hash $G/hash/patch $G/hash/testc
V=$WT/m3/vendor/third_party
"$J/javac" -proc:none -nowarn -XDsuppressNotes --add-exports java.base/sun.nio.ch=ALL-UNNAMED -sourcepath "$WTW/m3/vendor/third_party/annotations/src/main/java;$WTW/m3/vendor/third_party/jsr305/src/main/java" -d $G/hash/oracle $(find $V/zero-allocation-hashing/src/main/java -name '*.java') > $G/hash/oracle.log 2>&1; log "hash_oracle rc=$? classes=$(find $G/hash/oracle -name '*.class' | wc -l)"
cp $WT/src/java.base/share/classes/jdk/internal/m3/hash/*.java $G/hash/psrc/jdk/internal/m3/hash/
"$J/javac" -g -Xlint:all -Werror -Xlint:-this-escape -XDstringConcat=inline --patch-module "java.base=$GW/hash/psrc" -d $G/hash/patch $(find $G/hash/psrc -name '*.java') > $G/hash/patch.log 2>&1; log "hash_patch rc=$? classes=$(find $G/hash/patch -name '*.class' | wc -l) warnings=$(grep -c warning $G/hash/patch.log)"
T=$WT/test/jdk/jdk/internal/m3/hash
"$J/javac" -g -Xlint:all -Xlint:-serial --patch-module "java.base=$GW/hash/patch" --add-exports java.base/jdk.internal.m3.hash=ALL-UNNAMED -cp "$API" -d $G/hash/testc $T/M3HashOracle.java $T/M3HashAtomsTest.java $T/M3HashJitAtomsTest.java > $G/hash/testc.log 2>&1; log "hash_testc rc=$?"
RUN="--patch-module java.base=$GW/hash/patch --add-opens java.base/jdk.internal.m3.hash=ALL-UNNAMED --add-exports java.base/sun.nio.ch=ALL-UNNAMED"
"$J/java" -Xmx1g $RUN "-Dm3.hash.oracle=$GW/hash/oracle" -jar "$CS" execute -cp "$GW/hash/testc" --select-class M3HashAtomsTest --select-class M3HashJitAtomsTest --details=summary --disable-banner > $G/hash/junit_prebuilt.txt 2>&1; log "hash_junit_prebuilt rc=$? $(grep -E 'tests (successful|failed)' $G/hash/junit_prebuilt.txt | tr -s ' ' | tr '\n' ' ')"
"$J/java" -Xmx1g $RUN "-Dtest.src=$WTW/test/jdk/jdk/internal/m3/hash" -jar "$CS" execute -cp "$GW/hash/testc" --select-class M3HashAtomsTest --select-class M3HashJitAtomsTest --details=summary --disable-banner > $G/hash/junit_testsrc.txt 2>&1; log "hash_junit_testsrc rc=$? $(grep -E 'tests (successful|failed)' $G/hash/junit_testsrc.txt | tr -s ' ' | tr '\n' ' ')"
# B. topK
mkdir -p $G/topk/classes $G/topk/native $G/topk/testclasses
S=$WT/m3/collections/src/main/java/jdk/m3/collections; TS=$WT/m3/collections/src/test/java/jdk/m3/collections
"$J/javac" --release 21 -Xlint:all,-module -Werror -d $G/topk/classes $S/M3LongCollection.java $S/M3LongIterator.java $S/M3LaneSearch.java $S/M3LongRankBatchNative.java $S/M3LongRankIndex.java $TS/M3LongTopKFrequencyContract.java > $G/topk/javac.log 2>&1; log "topk_javac rc=$?"
gcc -std=c17 -O2 -Wall -Wextra -Werror -shared -I"$JH/include" -I"$JH/include/win32" $WT/m3/collections/src/main/native/collections/m3_long_rank_batch_jni.c -o $G/topk/native/m3rankbatch.dll > $G/topk/gcc.log 2>&1; log "topk_gcc rc=$? symbol=$(nm $G/topk/native/m3rankbatch.dll 2>/dev/null | grep -c Java_jdk_m3_collections_M3LongRankBatchNative_topKFrequent0)"
"$J/java" -ea -cp $GW/topk/classes jdk.m3.collections.M3LongTopKFrequencyContract > $G/topk/fallback.txt 2>&1; log "topk_java rc=$? $(tail -1 $G/topk/fallback.txt)"
"$J/java" -ea -Xcheck:jni -Dm3.collections.rank.native=true -Djava.library.path=$GW/topk/native -cp $GW/topk/classes jdk.m3.collections.M3LongTopKFrequencyContract --native-required > $G/topk/native.txt 2>&1; log "topk_native rc=$? $(tail -1 $G/topk/native.txt)"
"$J/java" -ea -Xint -Xcheck:jni -Dm3.collections.rank.native=true -Djava.library.path=$GW/topk/native -cp $GW/topk/classes jdk.m3.collections.M3LongTopKFrequencyContract --native-required > $G/topk/interp.txt 2>&1; log "topk_interp rc=$? identical=$(cmp -s $G/topk/native.txt $G/topk/interp.txt && echo yes || echo no)"
"$J/javac" --release 21 -d $G/topk/testclasses -cp "$GW/topk/classes;$API" $TS/M3LongTopKFrequencyTest.java > $G/topk/javac_test.log 2>&1; log "topk_junit_javac rc=$?"
"$J/java" -ea -Dm3.collections.rank.native=true -Djava.library.path=$GW/topk/native -jar "$CS" execute -cp "$GW/topk/classes;$GW/topk/testclasses" --select-class jdk.m3.collections.M3LongTopKFrequencyTest --details=summary --disable-banner > $G/topk/junit.txt 2>&1; log "topk_junit rc=$? $(grep -E 'tests (successful|failed)' $G/topk/junit.txt | tr -s ' ' | tr '\n' ' ')"
# C. case-liang lane (its verify script in Windows form: win32 JNI include, .dll)
mkdir -p $G/liang/classes; L=$WT/m3/lexicon/receiver
(cd $L && grep -oE "^'[0-9a-f]{64}  [^']+'" verify-case-liang-20261103.sh | tr -d "'" | while read -r sum path; do printf '%s  %s\n' "$sum" "$path" | sha256sum --check --status && echo "pin ok $path" || echo "pin DRIFT $path"; done) > $G/liang/pins.txt 2>&1; log "liang_pins $(grep -c 'pin ok' $G/liang/pins.txt) ok, $(grep -c DRIFT $G/liang/pins.txt) drift"
"$J/javac" --release 21 -encoding UTF-8 -Xlint:all -Werror -d $G/liang/classes $L/src/main/java/jdk/m3/lexicon/*.java $L/src/test/java/jdk/m3/lexicon/M3CaseLiangParityProof.java $L/src/test/java/jdk/m3/lexicon/M3CaseLiangJniParityProof.java > $G/liang/javac.log 2>&1; log "liang_javac rc=$? $(grep -c error $G/liang/javac.log) errors"
gcc -std=c17 -O2 -shared -Wall -Wextra -Wpedantic -Wconversion -Wshadow -Werror -I"$JH/include" -I"$JH/include/win32" $L/src/test/native/M3CaseLiangJniOracle.c -o $G/liang/m3-case-liang-jni.dll > $G/liang/gcc.log 2>&1; log "liang_gcc rc=$?"
"$J/java" -ea -Xmx512m -cp $GW/liang/classes jdk.m3.lexicon.M3CaseLiangParityProof > $G/liang/proof.txt 2>&1; log "liang_java rc=$? $(tail -1 $G/liang/proof.txt | cut -c1-120)"
"$J/java" -ea -Xcheck:jni -Xmx512m -cp $GW/liang/classes jdk.m3.lexicon.M3CaseLiangJniParityProof "$GW/liang/m3-case-liang-jni.dll" > $G/liang/jni.txt 2>&1; log "liang_jni rc=$? $(tail -1 $G/liang/jni.txt | cut -c1-120)"
"$J/java" -ea -Xint -Xcheck:jni -Xmx512m -cp $GW/liang/classes jdk.m3.lexicon.M3CaseLiangJniParityProof "$GW/liang/m3-case-liang-jni.dll" > $G/liang/jni_int.txt 2>&1; log "liang_jni_int rc=$? $(tail -1 $G/liang/jni_int.txt | cut -c1-120)"
# D. collections tests restored into m3/collections/src/test (compile against the module's main sources + junit api)
mkdir -p $G/coll/main $G/coll/test
"$J/javac" --release 21 -nowarn -d $G/coll/main $(find $WT/m3/collections/src/main/java -name '*.java') > $G/coll/javac_main.log 2>&1; log "coll_main rc=$? classes=$(find $G/coll/main -name '*.class' | wc -l) errors=$(grep -c 'error:' $G/coll/javac_main.log)"
"$J/javac" --release 21 -nowarn -d $G/coll/test -cp "$GW/coll/main;$API" $TS/M3DeepReadOnlyCollectionSelfTest.java $TS/M3DeepReadOnlyCollectionTest.java $TS/M3FilteredNavigationBoundarySelfTest.java $TS/M3FilteredNavigationBoundaryTest.java $TS/M3IndexPathMasksLongSelfTest.java $TS/M3IndexPathMasksLongTest.java $TS/M3ReadOnlyViewBoundarySelfTest.java $TS/M3ReadOnlyViewBoundaryTest.java $TS/M3LongHashSetAddAtomTest.java $TS/M3ArrayOnlyFacadeSelfTest.java > $G/coll/javac_test.log 2>&1; log "coll_test rc=$? errors=$(grep -c 'error:' $G/coll/javac_test.log)"
"$J/java" -ea -jar "$CS" execute -cp "$GW/coll/main;$GW/coll/test" --select-package jdk.m3.collections --include-classname ".*(DeepReadOnlyCollection|FilteredNavigationBoundary|IndexPathMasksLong|ReadOnlyViewBoundary|LongHashSetAddAtom|ArrayOnlyFacade).*" --details=summary --disable-banner > $G/coll/junit.txt 2>&1; log "coll_junit rc=$? $(grep -E 'tests (successful|failed)' $G/coll/junit.txt | tr -s ' ' | tr '\n' ' ')"
for c in M3DeepReadOnlyCollectionSelfTest M3FilteredNavigationBoundarySelfTest M3IndexPathMasksLongSelfTest M3ReadOnlyViewBoundarySelfTest M3ArrayOnlyFacadeSelfTest; do "$J/java" -ea -cp "$GW/coll/main;$GW/coll/test" jdk.m3.collections.$c > $G/coll/$c.txt 2>&1; log "coll_self $c rc=$? $(tail -1 $G/coll/$c.txt | cut -c1-100)"; done
log end
