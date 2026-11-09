#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gate 4 (native) of recipe entryless-lane-kernel-20261106, for a Linux host (the 2026-11-06 run: WSL Ubuntu 24.04, gcc 13.3,
# Temurin 21.0.13 under ~/jdk). Run after differential.sh (its scratch holds the donor Java closure, the donor names and
# the donor kernels under native/, so no git access and no donor token is needed here).
#   1. javac -h of the donor closure and of the host owner (M3IndexMaskCompactor + the relocated proof): the five lane
#      prototypes of the two JNI headers must be identical modulo the class name.
#   2. gcc of the donor kernel (libdonor.so) and of the module library libm3_collections_mask.so (m3_index_mask_compactor.c
#      + m3_index_mask_window.c, the two sources of the library as m3/collections/qualification/full-java-jni-20261018/
#      verify.py and doops-entryless-verify.sh build it; the module has no CMake for it) with the donor's flags
#      -std=c17 -O2 -Wall -Wextra -Wpedantic -Werror -fPIC -shared, both through -include of their javac -h header
#      (prototype check); 5 lane exports + 16 host exports (15 existing + laneKernelAbiNative) in the module library.
#   3. the lane-kernel proof under -Xcheck:jni in the three VM modes of the donor's sharedscan verify.sh (default, -Xint,
#      -XX:TieredStopAtLevel=1) against the donor library (donor classes) and against the module library (relocated
#      classes): six executions, outputs byte-identical to each other and to "PASS cases=<N> native=true"; 0 JNI warnings.
#   4. the dump driver through the native path on both sides (20,000 ops), and the cross-check of the #2342 half:
#      M3IndexMaskPositionPageProof (Java and native) and M3IndexMaskCompactorSelfTest --native against the module library.
#   5. the retained-variant kernels of the develop tree compiled and run through the donor proof (results-only note).
# usage: native-gate.sh <differential-scratch> <out-dir>   (JDK: $JAVA_HOME or ~/jdk/jdk-21*)
set -uo pipefail
RECIPE_DIR=$(cd "$(dirname "$0")" && pwd)
WT=$(cd "$RECIPE_DIR/../.." && pwd)
DIFF=$1; OUT=$2
JDK=${JAVA_HOME:-$(ls -d "$HOME"/jdk/jdk-21* 2>/dev/null | head -1)}
export JAVA_HOME=$JDK; export PATH=$JDK/bin:$PATH
rm -rf "$OUT"; mkdir -p "$OUT"/{donor-classes,donor-hdr,m3-classes,m3-hdr,run,lib}
status=PASS
step() { echo "== $*"; }
fail() { echo "FAIL: $*"; status=FAIL; }
FLAGS="-std=c17 -O2 -Wall -Wextra -Wpedantic -Werror -fPIC -shared"
INC="-I$JDK/include -I$JDK/include/linux"
NATIVE="$WT/src/main/native/collections"
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
java -version 2>&1 | head -1; gcc --version | head -1
donor_kernel=$(sed -n 1p "$DIFF/donor-names.txt"); donor_proof=$(sed -n 2p "$DIFF/donor-names.txt")
donor_prefix=$(awk -F'\t' '$1=="JNI" {print $2; exit}' "$MAP"); m3_prefix=$(awk -F'\t' '$1=="JNI" {print $3; exit}' "$MAP")
donor_class=${donor_prefix#Java_}; donor_class=${donor_class%_}; m3_class=${m3_prefix#Java_}; m3_class=${m3_class%_}
DONOR_HDR="$donor_class.h"; M3_HDR="$m3_class.h"
donor_src=$(find "$DIFF/donor-src" -name '*.java' -path "*/${donor_kernel%%.*}/*" | sort)
echo "DONOR kernel=$donor_kernel proof=$donor_proof header=$DONOR_HDR"

step "javac donor closure (-h header)"
javac --release 21 -proc:none -Xlint:all -Werror -h "$OUT/donor-hdr" -d "$OUT/donor-classes" $donor_src "$DIFF/donor-src/M3IndexMaskLaneKernelDump.java" || fail "donor javac"
step "javac host owner + relocated oracles (-h header)"
javac --release 21 -proc:none -Xlint:all,-module -Werror -h "$OUT/m3-hdr" -d "$OUT/m3-classes" \
    "$WT/src/main/java/jdk/m3/collections/M3IndexMaskCompactor.java" \
    "$WT/src/test/java/jdk/m3/collections/M3IndexMaskLaneKernelProof.java" \
    "$WT/src/test/java/jdk/m3/collections/M3IndexMaskLaneKernelDump.java" \
    "$WT/src/test/java/jdk/m3/collections/M3IndexMaskPositionPageProof.java" || fail "relocated javac"
step "javac the compactor self test over the package sources of both modules (symlinked without module-info, as doops-entryless-verify.sh does)"
mkdir -p "$OUT/sources/jdk/m3"; ln -s "$WT/src/main/java/jdk/m3/collections" "$OUT/sources/jdk/m3/collections"; ln -s "$WT/../arrays/src/main/java/jdk/m3/arrays" "$OUT/sources/jdk/m3/arrays"
javac --release 21 -proc:none -Xlint:all,-module -Werror -d "$OUT/m3-classes" -sourcepath "$OUT/sources" \
    "$WT/src/test/java/jdk/m3/collections/M3IndexMaskCompactorSelfTest.java" || fail "self test javac"
step "header prototypes of the five lane natives (donor vs host header, class name mapped)"
lane_protos() { grep -A1 -E "JNICALL Java_[A-Za-z0-9_]+_(nativeSelect|nativeSelectInto|nativeCount|nativeFirst|nativeScanPageInto)$" "$1" | sed "s/$2/CLASS/g" | grep -v '^--$'; }
if diff <(lane_protos "$OUT/donor-hdr/$DONOR_HDR" "$donor_class") <(lane_protos "$OUT/m3-hdr/$M3_HDR" "$m3_class") > "$OUT/header.diff"; then
    echo "LANE PROTOTYPES IDENTICAL modulo class name ($(lane_protos "$OUT/m3-hdr/$M3_HDR" x | grep -c JNIEXPORT) prototypes)"
else fail "javac -h lane prototypes differ"; cat "$OUT/header.diff"; fi
echo "host header exports: $(grep -c JNIEXPORT "$OUT/m3-hdr/$M3_HDR")"

step "gcc donor kernel (prototypes checked through -include of its javac -h header)"
gcc $FLAGS $INC -I"$OUT/donor-hdr" -include "$DONOR_HDR" "$DIFF/native/kernel.c" -o "$OUT/lib/libdonor.so" && echo "donor C compiled" || fail "donor C gcc"
step "gcc module library libm3_collections_mask.so (compactor + window sources; compactor through -include of its header)"
gcc $FLAGS $INC -I"$OUT/m3-hdr" -include "$M3_HDR" "$NATIVE/m3_index_mask_compactor.c" "$NATIVE/m3_index_mask_window.c" -o "$OUT/lib/libm3_collections_mask.so" && echo "module library compiled" || fail "module library gcc"
gcc -std=c11 -O2 -Wall -Wextra -Wpedantic -Werror -fPIC -c $INC "$NATIVE/m3_index_mask_compactor.c" -o "$OUT/compactor-c11.o" && echo "compactor also compiles as c11 (the m3/native CMake standard)" || fail "c11 compile"
sed '/FLLDCIM: RELOCATE-MERGE begin/,$d' "$NATIVE/m3_index_mask_compactor.c" > "$OUT/compactor-host-only.c"
gcc -std=c17 -O2 -Wall -Wextra -Wpedantic -Wconversion -Wshadow -fPIC -c $INC "$NATIVE/m3_index_mask_compactor.c" -o "$OUT/compactor-wconv.o" 2> "$OUT/wconversion.log"
gcc -std=c17 -O2 -Wall -Wextra -Wpedantic -Wconversion -Wshadow -fPIC -c $INC "$OUT/compactor-host-only.c" -o "$OUT/host-wconv.o" 2> "$OUT/wconversion-host.log"
echo "-Wconversion -Wshadow warnings (information): merged file $(grep -c 'warning:' "$OUT/wconversion.log"), host before the block $(grep -c 'warning:' "$OUT/wconversion-host.log")"
echo "lane exports in libdonor.so: $(nm -D --defined-only "$OUT/lib/libdonor.so" | grep -c "$donor_prefix")"
echo "lane exports in libm3_collections_mask.so: $(nm -D --defined-only "$OUT/lib/libm3_collections_mask.so" | grep -cE "${m3_prefix}native(Select|SelectInto|Count|First|ScanPageInto)$"); host exports: $(nm -D --defined-only "$OUT/lib/libm3_collections_mask.so" | grep -c "$m3_prefix"); probe: $(nm -D --defined-only "$OUT/lib/libm3_collections_mask.so" | grep -c laneKernelAbiNative)"

step "lane-kernel proof under -Xcheck:jni, three VM modes, donor library vs module library"
first=""
for vm in default interpreted c1; do
    opts=(); [ "$vm" = interpreted ] && opts=(-Xint); [ "$vm" = c1 ] && opts=(-XX:TieredStopAtLevel=1)
    java -ea -Xcheck:jni "${opts[@]}" -cp "$OUT/donor-classes" -Dm3.doops.entryless.native.path="$OUT/lib/libdonor.so" \
        "$donor_proof" > "$OUT/run/donor-$vm.txt" 2> "$OUT/run/donor-$vm.err" || fail "donor proof $vm"
    java -ea -Xcheck:jni "${opts[@]}" -cp "$OUT/m3-classes" -Djava.library.path="$OUT/lib" \
        jdk.m3.collections.M3IndexMaskLaneKernelProof > "$OUT/run/m3-$vm.txt" 2> "$OUT/run/m3-$vm.err" || fail "relocated proof $vm"
    if cmp -s "$OUT/run/donor-$vm.txt" "$OUT/run/m3-$vm.txt"; then v=IDENTICAL; else v=DIFFERENT; status=FAIL; fi
    out=$(cat "$OUT/run/m3-$vm.txt"); case "$out" in 'PASS cases='*' native=true') ;; *) fail "unexpected proof output $vm: $out";; esac
    [ -z "$first" ] && first=$out; [ "$first" = "$out" ] || fail "proof output differs across VM modes"
    echo "$vm donor=relocated $v: $out"
done
echo "jni warnings total: $(cat "$OUT"/run/*.err | grep -ciE "warning")"

step "dump driver through the native path (donor library vs module library), 20000 ops"
java -Xcheck:jni -cp "$OUT/donor-classes" -Dm3.doops.entryless.native.path="$OUT/lib/libdonor.so" jdk.m3.collections.M3IndexMaskLaneKernelDump "$donor_kernel" 20000 20261106 > "$OUT/run/donor-dump.txt" 2> "$OUT/run/donor-dump.err" || fail "donor dump"
java -Xcheck:jni -cp "$OUT/m3-classes" -Djava.library.path="$OUT/lib" jdk.m3.collections.M3IndexMaskLaneKernelDump jdk.m3.collections.M3IndexMaskCompactor 20000 20261106 > "$OUT/run/m3-dump.txt" 2> "$OUT/run/m3-dump.err" || fail "relocated dump"
if cmp -s "$OUT/run/donor-dump.txt" "$OUT/run/m3-dump.txt"; then echo "dump IDENTICAL: $(cat "$OUT/run/m3-dump.txt")"; else fail "native dump differs"; fi
java -cp "$OUT/m3-classes" jdk.m3.collections.M3IndexMaskLaneKernelDump jdk.m3.collections.M3IndexMaskCompactor 20000 20261106 > "$OUT/run/m3-dump-java.txt" 2>/dev/null
cmp -s "$OUT/run/m3-dump-java.txt" "$OUT/run/m3-dump.txt" && echo "native dump = Java-path dump (same digest)" || fail "native dump differs from the Java path"

step "cross-check: position-page proof and compactor self test against the module library (#2342 half)"
java -ea -cp "$OUT/m3-classes" jdk.m3.collections.M3IndexMaskPositionPageProof > "$OUT/run/page-java.txt" && cat "$OUT/run/page-java.txt" || fail "position page java"
java -ea -Xcheck:jni -Djava.library.path="$OUT/lib" -cp "$OUT/m3-classes" jdk.m3.collections.M3IndexMaskPositionPageProof native > "$OUT/run/page-native.txt" 2> "$OUT/run/page-native.err" && cat "$OUT/run/page-native.txt" || fail "position page native"
java -ea -Xcheck:jni -Djava.library.path="$OUT/lib" -cp "$OUT/m3-classes" jdk.m3.collections.M3IndexMaskCompactorSelfTest --native > "$OUT/run/selftest-native.txt" 2> "$OUT/run/selftest-native.err" && cat "$OUT/run/selftest-native.txt" || fail "compactor self test native"
echo "cross-check jni warnings: $(cat "$OUT"/run/page-native.err "$OUT"/run/selftest-native.err | grep -ciE warning)"

step "retained-variant kernels of the develop tree through the donor proof (results-only note)"
for f in "$DIFF"/native/retained-*.c; do
    [ -s "$f" ] || continue
    v=$(basename "$f" .c)
    if gcc $FLAGS $INC -I"$OUT/donor-hdr" "$f" -o "$OUT/lib/lib$v.so" 2> "$OUT/run/$v.gcc"; then
        echo "$v: $(java -ea -Xcheck:jni -cp "$OUT/donor-classes" -Dm3.doops.entryless.native.path="$OUT/lib/lib$v.so" "$donor_proof" 2>/dev/null || echo 'proof FAILED') sha256=$(sha256sum "$f" | cut -c1-12)"
    else echo "$v: does not compile against the develop Java ($(head -1 "$OUT/run/$v.gcc"))"; fi
done
echo "sources: donor_c_sha256=$(sha256sum "$DIFF/native/kernel.c" | cut -c1-64) relocated_c_sha256=$(sha256sum "$NATIVE/m3_index_mask_compactor.c" | cut -c1-64)"
echo "NATIVE_GATE $status"
[ "$status" = PASS ]
