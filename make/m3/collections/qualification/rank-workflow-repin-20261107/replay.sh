#!/bin/bash
# replay.sh : replays the build/run steps of m3-long-rank-bounds-jni-java21.yml and m3-topk-frequency-jni-java21.yml
# (Ubuntu 24.04, OpenJDK 21, gcc + UBSan) on the extracted master files under t/; prints one line per step.
set -uo pipefail
cd "$(dirname "$0")/t"
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64; export PATH=$JAVA_HOME/bin:$PATH
O=/tmp/pins-replay; rm -rf $O; mkdir -p $O/m3rank/classes $O/m3rank/headers $O/m3rank/native $O/m3rank/legacy $O/m3topk/classes $O/m3topk/native
S=m3/collections/src/main/java/jdk/m3/collections; T=m3/collections/src/test/java/jdk/m3/collections; C=m3/collections/src/main/native/collections/m3_long_rank_batch_jni.c
step() { echo "$1 rc=$2"; }
# rank-bounds
javac --release 21 -Xlint:all -Werror -h $O/m3rank/headers -d $O/m3rank/classes $S/M3LaneSearch.java $S/M3LongRankBatchNative.java $T/M3LongRankBoundsContract.java; step rank_javac $?
grep -Fq Java_jdk_m3_collections_M3LongRankBatchNative_lowerUpperBounds0 $O/m3rank/headers/jdk_m3_collections_M3LongRankBatchNative.h && grep -Fq Java_jdk_m3_collections_M3LongRankBatchNative_capabilities0 $O/m3rank/headers/jdk_m3_collections_M3LongRankBatchNative.h; step rank_headers $?
cc -std=c17 -Wall -Wextra -Werror -O2 -fPIC -shared -fsanitize=undefined -fno-sanitize-recover=undefined -I$JAVA_HOME/include -I$JAVA_HOME/include/linux -include $O/m3rank/headers/jdk_m3_collections_M3LongRankBatchNative.h $C -o $O/m3rank/native/libm3rankbatch.so; step rank_cc $?
for s in lowerBounds0 lowerUpperBounds0 capabilities0; do nm -D $O/m3rank/native/libm3rankbatch.so | grep -Fq Java_jdk_m3_collections_M3LongRankBatchNative_$s || echo "missing $s"; done
cc -DM3_RANK_LEGACY_ABI_TEST -std=c17 -Wall -Wextra -Werror -O2 -fPIC -shared -fsanitize=undefined -fno-sanitize-recover=undefined -I$JAVA_HOME/include -I$JAVA_HOME/include/linux -include $O/m3rank/headers/jdk_m3_collections_M3LongRankBatchNative.h $C -o $O/m3rank/legacy/libm3rankbatch.so; step rank_cc_legacy $?
nm -D $O/m3rank/legacy/libm3rankbatch.so | grep -Fq Java_jdk_m3_collections_M3LongRankBatchNative_lowerBounds0; step legacy_has_lowerBounds0 $?
! nm -D $O/m3rank/legacy/libm3rankbatch.so | grep -Fq Java_jdk_m3_collections_M3LongRankBatchNative_capabilities0; step legacy_lacks_capabilities0 $?
export UBSAN_OPTIONS=halt_on_error=1:print_stacktrace=1
java -ea -cp $O/m3rank/classes jdk.m3.collections.M3LongRankBoundsContract > $O/m3rank/fallback.txt; step rank_fallback $?
java -ea -Dm3.collections.rank.native=true -Djava.library.path=$O/m3rank/missing -cp $O/m3rank/classes jdk.m3.collections.M3LongRankBoundsContract > $O/m3rank/missing.txt; step rank_missing $?
java -ea -Xcheck:jni -Dm3.collections.rank.native=true -Dm3.rank.expectLegacy=true -Djava.library.path=$O/m3rank/legacy -cp $O/m3rank/classes jdk.m3.collections.M3LongRankBoundsContract > $O/m3rank/legacy-jit.txt; step rank_legacy_jit $?
java -ea -Xint -Xcheck:jni -Dm3.collections.rank.native=true -Dm3.rank.expectLegacy=true -Djava.library.path=$O/m3rank/legacy -cp $O/m3rank/classes jdk.m3.collections.M3LongRankBoundsContract > $O/m3rank/legacy-int.txt; step rank_legacy_int $?
java -ea -Xcheck:jni -Dm3.collections.rank.native=true -Dm3.rank.requireNative=true -Djava.library.path=$O/m3rank/native -cp $O/m3rank/classes jdk.m3.collections.M3LongRankBoundsContract > $O/m3rank/native-jit.txt; step rank_native_jit $?
java -ea -Xint -Xcheck:jni -Dm3.collections.rank.native=true -Dm3.rank.requireNative=true -Djava.library.path=$O/m3rank/native -cp $O/m3rank/classes jdk.m3.collections.M3LongRankBoundsContract > $O/m3rank/native-int.txt; step rank_native_int $?
echo "rank_marker: $(head -1 $O/m3rank/fallback.txt)"
for f in missing legacy-jit legacy-int native-jit native-int; do cmp -s $O/m3rank/fallback.txt $O/m3rank/$f.txt; step cmp_$f $?; done
# topk
javac --release 21 -Xlint:all,-module -Werror -d $O/m3topk/classes $S/M3LongCollection.java $S/M3LongIterator.java $S/M3LaneSearch.java $S/M3LongRankBatchNative.java $S/M3LongRankIndex.java $T/M3LongTopKFrequencyContract.java; step topk_javac $?
cc -std=c17 -O2 -Wall -Wextra -Werror -fsanitize=undefined -fno-sanitize-recover=undefined -fPIC -shared -I$JAVA_HOME/include -I$JAVA_HOME/include/linux $C -o $O/m3topk/native/libm3rankbatch.so; step topk_cc $?
nm -D $O/m3topk/native/libm3rankbatch.so | grep -Fq Java_jdk_m3_collections_M3LongRankBatchNative_topKFrequent0; step topk_symbol $?
java -ea -cp $O/m3topk/classes jdk.m3.collections.M3LongTopKFrequencyContract > $O/m3topk/fallback.txt; step topk_fallback $?
echo "topk_fallback_marker: $(tail -1 $O/m3topk/fallback.txt)"
java -ea -Xcheck:jni -Dm3.collections.rank.native=true -Djava.library.path=$O/m3topk/native -cp $O/m3topk/classes jdk.m3.collections.M3LongTopKFrequencyContract --native-required > $O/m3topk/native.txt; step topk_native $?
java -ea -Xint -Xcheck:jni -Dm3.collections.rank.native=true -Djava.library.path=$O/m3topk/native -cp $O/m3topk/classes jdk.m3.collections.M3LongTopKFrequencyContract --native-required > $O/m3topk/interpreter.txt; step topk_interpreter $?
echo "topk_native_marker: $(tail -1 $O/m3topk/native.txt)"
cmp -s $O/m3topk/native.txt $O/m3topk/interpreter.txt; step topk_cmp $?
