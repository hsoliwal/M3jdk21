#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gate 4 (native) of recipe posting-index-20261106, for a Linux host (the 2026-11-06 run: WSL, gcc 13.3, cmake 3.28,
# Temurin 21.0.13). Run after differential.sh (its scratch holds the donor Java closure in package layout).
#   1. javac -h of the donor closure and of the relocated module sources: the two JNI headers must be identical
#      modulo the mapped class name (the prototype check that the donor's #include used to give).
#   2. gcc of the donor kernel and of the relocated kernel with -std=c17 -O2 -Wall -Wextra -Wpedantic -Werror -fPIC
#      -shared (the relocated one also through -include of its javac -h header, and once more with the module's
#      CMake flags -std=c11 -Wconversion -Wshadow); 7 exported symbols each.
#   3. the seven oracles under -Xcheck:jni against the donor library and against the relocated library: outputs
#      byte-identical, 0 JNI warnings; figures printed (MAT_GALLOP_SCENARIOS=38360, digest 1c7fb0a8..., skew 1034/5178).
#   4. the develop main-path kernel (the retained-variant path with its prefix stripped) compiled and run through the
#      donor galloping oracle: same digest (results only; it exports no count kernels).
#   5. the module's own CMake build of libm3_arrays with the appended registration, ctest, 7 posting symbols in the
#      library, and the seven relocated oracles run against libm3_arrays.so.
# usage: native-gate.sh <differential-scratch> <out-dir>   (JDK: $JAVA_HOME or ~/jdk/jdk-21*; the scratch of differential.sh
#        holds the donor Java closure under src/ and the two donor kernels under native/, so no git access is needed here)
set -uo pipefail
RECIPE_DIR=$(cd "$(dirname "$0")" && pwd)
WT=$(cd "$RECIPE_DIR/../.." && pwd)
DIFF=$1; OUT=$2
JDK=${JAVA_HOME:-$(ls -d "$HOME"/jdk/jdk-21* 2>/dev/null | head -1)}
export JAVA_HOME=$JDK; export PATH=$JDK/bin:$PATH
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
rm -rf "$OUT"; mkdir -p "$OUT"/{donor-classes,donor-hdr,m3-classes,m3-hdr,m3-test-classes,run,donor-native}
status=PASS
step() { echo "== $*"; }
fail() { echo "FAIL: $*"; status=FAIL; }
FLAGS="-std=c17 -O2 -Wall -Wextra -Wpedantic -Werror -fPIC -shared"
MODULE_FLAGS="-std=c11 -Wall -Wextra -Werror -Wconversion -Wshadow -Wpedantic -fPIC -c"
INC="-I$JDK/include -I$JDK/include/linux"
java -version 2>&1 | head -1; gcc --version | head -1; cmake --version | head -1

target_package=$(awk -F'\t' '$1=="PACKAGE" {print $3; exit}' "$MAP")
donor_c=$(awk -F'\t' '$1=="FILE" && $2 ~ /\.c$/ {print $2; exit}' "$MAP")
mainpath_c=$(echo "$donor_c" | sed 's#^m3/qualification/[^/]*/retained-variants/[^/]*/##')
donor_symbol=$(awk -F'\t' '$1=="JNI" {print $2; exit}' "$MAP")
relocated_symbol=$(awk -F'\t' '$1=="JNI" {print $3; exit}' "$MAP")
donor_header=$(awk -F'\t' '$1=="INCLUDE" {print $2; exit}' "$MAP" | tr -d '"')
relocated_header="${relocated_symbol#Java_}"; relocated_header="${relocated_header%_}.h"
fqn() { # donor simple name -> "donor_fqn relocated_fqn"
    local donor_path donor_pkg target_simple
    donor_path=$(awk -F'\t' -v s="/$1.java" '$1=="FILE" && index($2, s) {print $2; exit}' "$MAP")
    donor_pkg=$(dirname "$donor_path" | sed 's#.*/src/\(main\|test\)/java/##; s#/#.#g')
    target_simple=$(awk -F'\t' -v d="$1" '$1=="TYPE" && $2==d {print $3; exit}' "$MAP")
    echo "$donor_pkg.$1 $target_package.$target_simple"
}
cp "$DIFF/native/kernel.c" "$OUT/donor-native/kernel.c"; cp "$DIFF/native/mainpath.c" "$OUT/donor-native/mainpath.c"
echo "DONOR kernel=$donor_c mainpath=$mainpath_c (from $DIFF/native)"

step "javac donor closure (-h header) from the differential scratch"
javac --release 21 -Xlint:all -Werror -h "$OUT/donor-hdr" -d "$OUT/donor-classes" -sourcepath "$DIFF/src" $(find "$DIFF/src" -name '*.java') || fail "donor javac"
step "javac relocated main (-h header) and contracts"
javac --release 21 -Xlint:all,-module -Werror -h "$OUT/m3-hdr" -d "$OUT/m3-classes" $(find "$WT/src/main/java" -name '*.java' ! -name module-info.java) || fail "relocated javac"
javac --release 21 -Xlint:all,-module -Werror -d "$OUT/m3-test-classes" -cp "$OUT/m3-classes" "$WT"/src/test/java/jdk/m3/arrays/M3IntPostingIndex*Contract.java "$WT"/src/test/java/jdk/m3/arrays/M3IntPostingIndexDump.java || fail "relocated contracts javac"
step "header prototypes (javac -h of donor vs relocated, class name mapped)"
sed "s/${donor_header%.h}/${relocated_header%.h}/g" "$OUT/donor-hdr/$donor_header" | diff - "$OUT/m3-hdr/$relocated_header" && echo "HEADERS IDENTICAL modulo names" || fail "javac -h headers differ"

step "gcc donor kernel (brief flags)"
gcc $FLAGS $INC -I"$OUT/donor-hdr" "$OUT/donor-native/kernel.c" -o "$OUT/libdonor.so" && echo "donor C compiled" || fail "donor C gcc"
step "gcc develop main-path kernel (brief flags; results-only comparison below)"
gcc $FLAGS $INC -I"$OUT/donor-hdr" "$OUT/donor-native/mainpath.c" -o "$OUT/libmainpath.so" && echo "main-path C compiled" || fail "main-path C gcc"
step "gcc relocated kernel (brief flags, prototype check through -include of the javac -h header)"
gcc $FLAGS $INC -I"$OUT/m3-hdr" -include "$relocated_header" "$WT/src/main/native/m3_posting_merge.c" -o "$OUT/librelocated.so" && echo "relocated C compiled" || fail "relocated C gcc"
step "gcc relocated kernel with the module's CMake flags (-Wconversion -Wshadow, c11)"
gcc $MODULE_FLAGS $INC "$WT/src/main/native/m3_posting_merge.c" -o "$OUT/m3_posting_merge.o" && echo "module flags OK" || fail "module flags"
echo "exported relocated symbols: $(nm -D --defined-only "$OUT/librelocated.so" | grep -c "$relocated_symbol")"
echo "exported donor symbols: $(nm -D --defined-only "$OUT/libdonor.so" | grep -c "$donor_symbol")"

run_pair() { # label donor-simple-name
    local label=$1 donor_fqn relocated_fqn
    read -r donor_fqn relocated_fqn < <(fqn "$2")
    java -Xcheck:jni -Dmatposting.native.path="$OUT/libdonor.so" -Dmatposting.requireNative=true -Dmatposting.expectGalloping=true \
        -cp "$OUT/donor-classes" "$donor_fqn" > "$OUT/run/donor-$label.txt" 2> "$OUT/run/donor-$label.err" || fail "donor $label native run"
    java -Xcheck:jni -Dmatposting.native.path="$OUT/librelocated.so" -Dmatposting.requireNative=true -Dmatposting.expectGalloping=true \
        -cp "$OUT/m3-classes:$OUT/m3-test-classes" "$relocated_fqn" > "$OUT/run/m3-$label.txt" 2> "$OUT/run/m3-$label.err" || fail "relocated $label native run"
    if cmp -s "$OUT/run/donor-$label.txt" "$OUT/run/m3-$label.txt"; then v=IDENTICAL; else v=DIFFERENT; status=FAIL; fi
    echo "$label $v sha256=$(sha256sum "$OUT/run/m3-$label.txt" | cut -c1-16) $(tr '\n' ' ' < "$OUT/run/m3-$label.txt")"
}
step "native oracle runs with -Xcheck:jni (donor library vs relocated library; outputs compared)"
run_pair gallop MatPostingGallopingContract
run_pair skew MatPostingSkewSearchContract
run_pair convergence MatPostingIndexConvergenceContract
run_pair cardinality MatPostingCardinalityContract
run_pair boundedcount MatPostingBoundedCountContract
run_pair interval MatPostingIntervalIntersectionContract
run_pair sparseunion MatPostingSparseUnionContract
echo "jni warnings total: $(cat "$OUT"/run/*.err | grep -ciE "warning")"
step "develop main-path kernel results (donor Java, galloping oracle only; it exports no count kernels)"
read -r gallop_donor _ < <(fqn MatPostingGallopingContract)
java -Xcheck:jni -Dmatposting.native.path="$OUT/libmainpath.so" -Dmatposting.requireNative=true -cp "$OUT/donor-classes" "$gallop_donor" > "$OUT/run/mainpath-gallop.txt" 2>/dev/null && tr '\n' ' ' < "$OUT/run/mainpath-gallop.txt" || echo "main-path gallop run failed"; echo
cmp -s <(grep MAT_GALLOP_DIGEST "$OUT/run/mainpath-gallop.txt") <(grep MAT_GALLOP_DIGEST "$OUT/run/m3-gallop.txt") && echo "main-path digest IDENTICAL to the relocated kernel's" || echo "main-path digest DIFFERENT (results-only note, not a gate)"

step "cmake build of the module native library (registration check) + ctest"
cmake -S "$WT/src/main/native" -B "$OUT/cmake" -DCMAKE_BUILD_TYPE=Release > "$OUT/cmake-configure.log" 2>&1 && cmake --build "$OUT/cmake" --config Release > "$OUT/cmake-build.log" 2>&1 && echo "cmake build OK (warnings: $(grep -ci warning "$OUT/cmake-build.log"))" || { fail "cmake"; tail -20 "$OUT/cmake-build.log"; }
(cd "$OUT/cmake" && ctest --output-on-failure -C Release > "$OUT/ctest.log" 2>&1 && grep "tests passed" "$OUT/ctest.log" || { fail ctest; tail -20 "$OUT/ctest.log"; })
echo "posting symbols in libm3_arrays.so: $(nm -D --defined-only "$OUT/cmake/libm3_arrays.so" 2>/dev/null | grep -c "$relocated_symbol")"
step "relocated oracles against libm3_arrays.so (the module library)"
for simple in MatPostingGallopingContract MatPostingSkewSearchContract MatPostingIndexConvergenceContract MatPostingCardinalityContract MatPostingBoundedCountContract MatPostingIntervalIntersectionContract MatPostingSparseUnionContract; do
    read -r _ relocated_fqn < <(fqn "$simple")
    java -Xcheck:jni -Dmatposting.native.path="$OUT/cmake/libm3_arrays.so" -Dmatposting.requireNative=true -Dmatposting.expectGalloping=true \
        -cp "$OUT/m3-classes:$OUT/m3-test-classes" "$relocated_fqn" > "$OUT/run/lib-${relocated_fqn##*.}.txt" 2>/dev/null \
        && echo "${relocated_fqn##*.} $(tail -2 "$OUT/run/lib-${relocated_fqn##*.}.txt" | tr '\n' ' ')" || fail "libm3_arrays ${relocated_fqn##*.}"
done
echo "sources: donor_c_sha256=$(sha256sum "$OUT/donor-native/kernel.c" | cut -c1-64) relocated_c_sha256=$(sha256sum "$WT/src/main/native/m3_posting_merge.c" | cut -c1-64)"
echo "NATIVE_GATE $status"
[ "$status" = PASS ]
