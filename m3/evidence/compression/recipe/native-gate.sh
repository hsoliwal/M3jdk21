#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Native gate of recipe compression-20261106 (native leaf), for a Linux host (the 2026-11-06 run: WSL Ubuntu 24.04,
# gcc 13.3, cmake 3.28, Temurin 21.0.13). Run after differential.sh, whose scratch holds the donor Java closure in
# package layout (classes/) and the donor C files (native/).
#   1. javac -h of the donor M3LzNative and of the relocated one: the two JNI headers are identical modulo the mapped
#      class name, and the JNI symbols the relocated library exports (nm -D) are exactly the header's prototypes.
#   2. gcc -Werror of the donor C file and of the relocated C file with the module's CMake flags (-std=c11 -Wall -Wextra
#      -Wconversion -Wsign-conversion -Wshadow -Wpedantic -Werror), the relocated one also through -include of its
#      javac -h header (a prototype mismatch is a compile error).
#   3. the module's own CMake build of src/main/native (configure, build, ctest): libm3_lz_compression.so and the
#      scalar C oracle m3_lz_test.
#   4. under -Xcheck:jni against the donor library (donor classes) and the relocated library (relocated classes):
#      M3LzSelfTest (m3.lz.library), M3LzNativeArenaSelfTest and M3LzFourWayNativeInteropSelfTest print byte-identical
#      output; no JNI warning.
#   5. M3LzNativeParityProbe under -Xcheck:jni: JNI and Java prefix kernels identical over the format oracle's
#      100,000 inputs (relocated library).
#   6. mutant: the scalar tail of m3_lz_prefix advancing by 2 (offset++ -> offset += 2), built as a library, must be
#      refused by M3LzNative.load (its load-time parity probe), for the donor and the relocated copy alike.
# usage: native-gate.sh <differential-scratch> <out-dir> [inputs]   (JDK: $JAVA_HOME or ~/jdk/jdk-21*)
set -uo pipefail
RECIPE_DIR=$(cd "$(dirname "$0")" && pwd)
WT=$(cd "$RECIPE_DIR/../.." && pwd)
DIFF=$1; OUT=$2; INPUTS=${3:-100000}
JDK=${JAVA_HOME:-$(ls -d "$HOME"/jdk/jdk-21* 2>/dev/null | head -1)}
export JAVA_HOME=$JDK; export PATH=$JDK/bin:$PATH
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
rm -rf "$OUT"; mkdir -p "$OUT"/{donor-hdr,m3-classes,m3-test-classes,m3-hdr,tmp,run}
# every temporary file (gcc, cmake, the JVMs and their perf data) stays under <out-dir>, off the host's disk image
export TMPDIR="$OUT/tmp"; export JAVA_TOOL_OPTIONS="-XX:-UsePerfData -Djava.io.tmpdir=$OUT/tmp"
status=PASS
step() { echo "== $*"; }
fail() { echo "FAIL: $*"; status=FAIL; }
FLAGS="-std=c11 -O2 -Wall -Wextra -Wconversion -Wsign-conversion -Wshadow -Wpedantic -Werror -fPIC -shared"
INC="-I$JDK/include -I$JDK/include/linux"
JAVA=(java "-Djava.io.tmpdir=$OUT/tmp" -Xcheck:jni)
java -version 2>&1 | head -1; gcc --version | head -1; cmake --version | head -1

target_package=$(awk -F'\t' '$1=="PACKAGE" {print $3; exit}' "$MAP")
donor_package=$(awk -F'\t' '$1=="PACKAGE" {print $2; exit}' "$MAP")
donor_symbol=$(awk -F'\t' '$1=="JNI" {print $2; exit}' "$MAP")
relocated_symbol=$(awk -F'\t' '$1=="JNI" {print $3; exit}' "$MAP")
relocated_c=$(awk -F'\t' '$1=="FILE" && $3 ~ /m3_lz_jni\.c$/ {print $3; exit}' "$MAP")
donor_header="${donor_symbol#Java_}"; donor_header="${donor_header%_}.h"
relocated_header="${relocated_symbol#Java_}"; relocated_header="${relocated_header%_}.h"
echo "DONOR C=$DIFF/native/m3_lz_jni.c RELOCATED C=$WT/$relocated_c"

step "1. javac -h headers (donor closure from the differential scratch, relocated module sources)"
javac --release 21 -Xlint:all -Werror -h "$OUT/donor-hdr" -d "$OUT/tmp/donor-h-classes" -sourcepath "$DIFF/src" \
    "$DIFF/src/${donor_package//.//}/M3LzNative.java" || fail "donor javac -h"
javac --release 21 -Xlint:all,-module -Werror -h "$OUT/m3-hdr" -d "$OUT/m3-classes" \
    $(find "$WT/src/main/java" -name '*.java' ! -name module-info.java) || fail "relocated javac"
javac --release 21 -Xlint:all,-module -Werror -d "$OUT/m3-test-classes" -cp "$OUT/m3-classes" \
    $(grep -L "org.junit" $(find "$WT/src/test/java" -name '*.java') | grep -v AllocationProbe) || fail "relocated test javac"
sed "s/${donor_header%.h}/${relocated_header%.h}/g" "$OUT/donor-hdr/$donor_header" | diff - "$OUT/m3-hdr/$relocated_header" \
    && echo "HEADERS IDENTICAL modulo names ($(grep -c JNIEXPORT "$OUT/m3-hdr/$relocated_header") prototypes)" \
    || fail "javac -h headers differ"

step "2. gcc -Werror (module CMake flags), donor and relocated; relocated through -include of its javac -h header"
gcc $FLAGS $INC "$DIFF/native/m3_lz_jni.c" -o "$OUT/libdonor.so" && echo "donor C compiled" || fail "donor gcc"
gcc $FLAGS $INC -I"$OUT/m3-hdr" -include "$relocated_header" "$WT/$relocated_c" -o "$OUT/librelocated.so" \
    && echo "relocated C compiled" || fail "relocated gcc"
header_symbols=$(grep -oE "Java_[A-Za-z0-9_]+" "$OUT/m3-hdr/$relocated_header" | sort -u)
library_symbols=$(nm -D --defined-only "$OUT/librelocated.so" | awk '{print $3}' | grep "^Java_" | sort -u)
[ "$header_symbols" = "$library_symbols" ] && echo "JNI SYMBOLS MATCH: $(echo $library_symbols)" \
    || fail "exported JNI symbols differ from the javac -h prototypes: [$library_symbols] vs [$header_symbols]"
echo "donor exported JNI symbols: $(nm -D --defined-only "$OUT/libdonor.so" | grep -c "$donor_symbol")"

step "3. module CMake build of src/main/native (configure, build, ctest)"
cmake -S "$WT/src/main/native" -B "$OUT/cmake" -DCMAKE_BUILD_TYPE=Release > "$OUT/cmake-configure.log" 2>&1 || fail "cmake configure"
cmake --build "$OUT/cmake" > "$OUT/cmake-build.log" 2>&1 || fail "cmake build"
grep -iE "warning|error" "$OUT/cmake-build.log" | head -5
(cd "$OUT/cmake" && ctest --output-on-failure > "$OUT/ctest.log" 2>&1) || fail "ctest"
grep -E "tests passed|Total Test" "$OUT/ctest.log"
MODULE_LIB="$OUT/cmake/libm3_lz_compression.so"
[ -f "$MODULE_LIB" ] && echo "module library: $(nm -D --defined-only "$MODULE_LIB" | grep -c "$relocated_symbol") JNI symbols" || fail "no module library"

step "4. oracles under -Xcheck:jni: donor library vs relocated (module) library, outputs compared"
jni_check() { local warnings; warnings=$(grep -ciE "WARNING in native method|FATAL ERROR in native method" "$1"); echo "${warnings:-0}"; }
run_pair() { # label oracle [args...]: donor run, relocated run, compare stdout, count JNI warnings
    local label=$1 oracle=$2; shift 2
    "${JAVA[@]}" -Dm3.lz.library="$OUT/libdonor.so" -cp "$DIFF/classes" "$donor_package.$oracle" "${@/LIB/$OUT/libdonor.so}" \
        > "$OUT/run/donor-$label.txt" 2> "$OUT/run/donor-$label.err" || fail "donor $label exit"
    "${JAVA[@]}" -Dm3.lz.library="$MODULE_LIB" -cp "$OUT/m3-classes:$OUT/m3-test-classes" "$target_package.$oracle" "${@/LIB/$MODULE_LIB}" \
        > "$OUT/run/relocated-$label.txt" 2> "$OUT/run/relocated-$label.err" || fail "relocated $label exit"
    if cmp -s "$OUT/run/donor-$label.txt" "$OUT/run/relocated-$label.txt"; then
        echo "$label IDENTICAL $(tr '\n' ' ' < "$OUT/run/relocated-$label.txt")jni_warnings=$(jni_check "$OUT/run/donor-$label.err")/$(jni_check "$OUT/run/relocated-$label.err")"
    else
        fail "$label differs: $(cat "$OUT/run/donor-$label.txt") vs $(cat "$OUT/run/relocated-$label.txt")"
    fi
    [ "$(jni_check "$OUT/run/donor-$label.err")" = 0 ] && [ "$(jni_check "$OUT/run/relocated-$label.err")" = 0 ] || fail "$label JNI warnings"
}
run_pair lzselftest M3LzSelfTest
run_pair arena M3LzNativeArenaSelfTest LIB
run_pair fourway_interop M3LzFourWayNativeInteropSelfTest

step "5. JNI vs Java prefix kernels over the format oracle's inputs (-Xcheck:jni, relocated module library)"
"${JAVA[@]}" -cp "$OUT/m3-classes:$OUT/m3-test-classes" "$target_package.M3LzNativeParityProbe" "$MODULE_LIB" "$INPUTS" \
    > "$OUT/run/parity.txt" 2> "$OUT/run/parity.err" || fail "parity probe exit"
echo "parity: $(tr '\n' ' ' < "$OUT/run/parity.txt")jni_warnings=$(jni_check "$OUT/run/parity.err")"
grep -q "RESULT=PASS" "$OUT/run/parity.txt" && [ "$(jni_check "$OUT/run/parity.err")" = 0 ] || fail "parity probe"

step "6. mutant kernels (scalar tail advancing by 2) must be refused by M3LzNative.load"
for side in donor relocated; do
    src="$DIFF/native/m3_lz_jni.c"; [ $side = relocated ] && src="$WT/$relocated_c"
    [ "$(grep -c '        offset++;' "$src")" = 1 ] || fail "$side mutant anchor count"
    sed 's/^        offset++;$/        offset += 2;/' "$src" > "$OUT/mutant-$side.c"
    gcc $FLAGS $INC "$OUT/mutant-$side.c" -o "$OUT/libmutant-$side.so" || fail "$side mutant gcc"
    cp=$OUT/m3-classes:$OUT/m3-test-classes; pkg=$target_package; [ $side = donor ] && { cp=$DIFF/classes; pkg=$donor_package; }
    if "${JAVA[@]}" -cp "$cp" "$pkg.M3LzNativeArenaSelfTest" "$OUT/libmutant-$side.so" > "$OUT/run/mutant-$side.txt" 2>&1; then
        fail "$side mutant ACCEPTED"
    elif grep -q "LinkageError: M3 LZ JNI parity probe failed" "$OUT/run/mutant-$side.txt"; then
        echo "mutant $side REFUSED: $(grep -m1 -o 'LinkageError: .*' "$OUT/run/mutant-$side.txt")"
    else
        fail "$side mutant failed otherwise: $(head -c 300 "$OUT/run/mutant-$side.txt")"
    fi
done
echo "NATIVE_GATE $status"
[ "$status" = PASS ]
