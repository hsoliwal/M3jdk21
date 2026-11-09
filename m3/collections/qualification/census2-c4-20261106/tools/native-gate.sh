#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Native gate of census2-c4-20261106 for a Linux host (2026-11-06 run: WSL Ubuntu 24.04, gcc 13.3, Temurin 21.0.13).
#   1. javac -h of the edited M3LongRankBatchNative (package sources symlinked without module-info) -> JNI header;
#   2. gcc of the edited m3_long_rank_batch_jni.c through -include of that header (prototype check) with
#      -std=c17 -O2 -Wall -Wextra -Wpedantic -Werror -fPIC -shared -> libm3rankbatch.so (7 exports);
#   3. the master source of the base revision as the legacy library (4 exports; it needs SIZE_MAX from <stdint.h>,
#      which it does not include, so it is built with -include stdint.h and its strict build is reported);
#   4. the relocated contracts under -Xcheck:jni: LC 315 --native (default and -Xint), --old-native (legacy library),
#      --missing-native; LC 719 k-th --native (default, -Xint) and with the legacy library (Java fallback); LC 719
#      pair distance with -Dm3.collections.rank.pairdistance.native=true (default, -Xint).
# usage: native-gate.sh <repo root> <classpath: main classes:test classes> <out dir> <base revision | master .c file>
set -uo pipefail
WT=$1; CP=$2; O=$3; BASE=$4
JDK=${JAVA_HOME:-$(ls -d "$HOME"/jdk/jdk-21* 2>/dev/null | head -1)}
export JAVA_HOME=$JDK; export PATH=$JDK/bin:$PATH
rm -rf "$O"; mkdir -p "$O"/{hdr,new,old,missing,src/jdk/m3}
NAT=$WT/m3/collections/src/main/native/collections
FLAGS="-std=c17 -O2 -Wall -Wextra -Wpedantic -Werror -fPIC -shared"; INC="-I$JDK/include -I$JDK/include/linux"
java -version 2>&1 | head -1; gcc --version | head -1
ln -s "$WT/m3/collections/src/main/java/jdk/m3/collections" "$O/src/jdk/m3/collections"
ln -s "$WT/m3/arrays/src/main/java/jdk/m3/arrays" "$O/src/jdk/m3/arrays"
javac --release 21 -proc:none -Xlint:all,-module -Werror -h "$O/hdr" -d "$O/hdr-classes" -sourcepath "$O/src" \
    "$WT/m3/collections/src/main/java/jdk/m3/collections/M3LongRankBatchNative.java" \
    && echo "javac -h OK: $(grep -c JNIEXPORT "$O/hdr/jdk_m3_collections_M3LongRankBatchNative.h") prototypes"
gcc $FLAGS $INC -I"$O/hdr" -include jdk_m3_collections_M3LongRankBatchNative.h "$NAT/m3_long_rank_batch_jni.c" \
    -o "$O/new/libm3rankbatch.so" && echo "edited C compiled (prototype-checked)"
if [ -f "$BASE" ]; then cp "$BASE" "$O/master.c"; else
    git -C "$WT" show "$BASE:m3/collections/src/main/native/collections/m3_long_rank_batch_jni.c" > "$O/master.c"; fi
gcc $FLAGS $INC "$O/master.c" -o "$O/master-strict.so" > "$O/master-strict.log" 2>&1
echo "master C strict compile rc=$? ($(grep -c 'error:' "$O/master-strict.log") errors)"
gcc $FLAGS $INC -include stdint.h "$O/master.c" -o "$O/old/libm3rankbatch.so" && echo "master C compiled with -include stdint.h"
echo "exports new=$(nm -D --defined-only "$O/new/libm3rankbatch.so" | grep -c Java_jdk_m3_collections_M3LongRankBatchNative_)" \
     "old=$(nm -D --defined-only "$O/old/libm3rankbatch.so" | grep -c Java_jdk_m3_collections_M3LongRankBatchNative_)"
J="java -ea -Djava.io.tmpdir=/tmp -Dm3.collections.rank.native=true"
for vm in "" "-Xint"; do
    echo "LC315 --native $vm: $($J -Xcheck:jni $vm -Djava.library.path="$O/new" -cp "$CP" jdk.m3.collections.M3LongCountSmallerRightContract --native 2>&1 | tail -1)"
    echo "KTH --native $vm: $($J -Xcheck:jni $vm -Djava.library.path="$O/new" -cp "$CP" jdk.m3.collections.M3KthPairDistanceContract --native 2>&1 | tail -1)"
    echo "PAIR native $vm: $($J -Xcheck:jni $vm -Dm3.collections.rank.pairdistance.native=true -Djava.library.path="$O/new" -cp "$CP" jdk.m3.collections.M3LongPairDistanceContract 2>&1 | tail -1)"
done
echo "LC315 --old-native: $($J -Xcheck:jni -Djava.library.path="$O/old" -cp "$CP" jdk.m3.collections.M3LongCountSmallerRightContract --old-native 2>&1 | tail -1)"
echo "LC315 --missing-native: $($J -Djava.library.path="$O/missing" -cp "$CP" jdk.m3.collections.M3LongCountSmallerRightContract --missing-native 2>&1 | tail -1)"
echo "KTH legacy library: $($J -Xcheck:jni -Djava.library.path="$O/old" -cp "$CP" jdk.m3.collections.M3KthPairDistanceContract 2>&1 | tail -1)"
for m in "M3LongCountSmallerRightContract --native" "M3KthPairDistanceContract --native" "M3LongPairDistanceContract"; do
    set -- $m
    echo "$1 -Xcheck:jni warnings: $($J -Xcheck:jni -Dm3.collections.rank.pairdistance.native=true -Djava.library.path="$O/new" -cp "$CP" "jdk.m3.collections.$1" ${2:-} 2>&1 | grep -c '^WARNING')"
done
