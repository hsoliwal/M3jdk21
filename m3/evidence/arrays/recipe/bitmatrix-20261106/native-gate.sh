#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gate 4 (native) of recipe bitmatrix-20261106, for a Linux host (the 2026-11-06 run: WSL, gcc 13.3, cmake 3.28,
# Temurin 21.0.13). Run after differential.sh (its scratch holds the donor Java closure in package layout and the
# donor kernel).
#   1. javac -h of the donor closure and of the relocated module sources: the two JNI headers must be identical
#      modulo the mapped class name (nativeAbi0, transposeNative0).
#   2. gcc of the donor kernel and of the relocated kernel with -std=c17 -O2 -Wall -Wextra -Wpedantic -Werror -fPIC
#      -shared (the relocated one also through -include of its javac -h header, and once more with the module's
#      CMake flags -std=c11 -Wconversion -Wshadow); 2 exported symbols each.
#   3. the oracles under -Xcheck:jni against the donor library and against the relocated library: the convergence
#      oracle with the JNI parity lane (MAT_BIT_MATRIX_BACKEND=JNI_PARITY), the native-required tile oracle
#      (MAT_TILE_*), and the mutant lane (a one-line corruption of each kernel must be refused:
#      MAT_BIT_MATRIX_NATIVE_MUTANT_REFUSED=PASS); outputs byte-identical, 0 JNI warnings.
#   4. M3BitMatrixDump through transpose(Backend.NATIVE_REQUIRED) (m3.bitmatrix.dumpBackend) against both libraries,
#      and against the Java lane dump of the differential scratch: all identical (the native dump replays the same
#      inputs; the NATIVE_REQUIRED path checks every native result against the Java result before publication).
#   5. the module's own CMake build of libm3_arrays with the appended registration, ctest, 2 bit-matrix symbols in the
#      library, and the relocated oracles run against libm3_arrays.so.
# usage: native-gate.sh <differential-scratch> <out-dir> [native-dump-rounds]   (JDK: $JAVA_HOME or ~/jdk/jdk-21*)
set -uo pipefail
RECIPE_DIR=$(cd "$(dirname "$0")" && pwd)
WT=$(cd "$RECIPE_DIR/../.." && pwd)
DIFF=$1; OUT=$2; ROUNDS=${3:-100000}
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
relocated_c=$(awk -F'\t' '$1=="FILE" && $2 ~ /\.c$/ {print $3; exit}' "$MAP")
donor_symbol=$(awk -F'\t' '$1=="JNI" {print $2; exit}' "$MAP")
relocated_symbol=$(awk -F'\t' '$1=="JNI" {print $3; exit}' "$MAP")
donor_property=$(awk -F'\t' '$1=="LITERAL" {print $2; exit}' "$MAP")
relocated_property=$(awk -F'\t' '$1=="LITERAL" {print $3; exit}' "$MAP")
donor_header="${donor_symbol#Java_}"; donor_header="${donor_header%_}.h"
relocated_header="${relocated_symbol#Java_}"; relocated_header="${relocated_header%_}.h"
fqn() { # donor simple name -> "donor_fqn relocated_fqn"
    local donor_path donor_pkg target_simple
    donor_path=$(awk -F'\t' -v s="/$1.java" '$1=="FILE" && index($2, s) {print $2; exit}' "$MAP")
    donor_pkg=$(dirname "$donor_path" | sed 's#.*/src/\(main\|test\)/java/##; s#/#.#g')
    target_simple=$(awk -F'\t' -v d="$1" '$1=="TYPE" && $2==d {print $3; exit}' "$MAP")
    echo "$donor_pkg.$1 $target_package.$target_simple"
}
cp "$DIFF/native/kernel.c" "$OUT/donor-native/kernel.c"
echo "DONOR kernel=$donor_c (from $DIFF/native) relocated=$relocated_c"

step "javac donor closure (-h header) from the differential scratch"
javac --release 21 -Xlint:all -Werror -h "$OUT/donor-hdr" -d "$OUT/donor-classes" -sourcepath "$DIFF/src" $(find "$DIFF/src" -name '*.java') || fail "donor javac"
step "javac relocated main (-h header) and contracts"
javac --release 21 -Xlint:all,-module -Werror -h "$OUT/m3-hdr" -d "$OUT/m3-classes" $(find "$WT/src/main/java" -name '*.java' ! -name module-info.java) || fail "relocated javac"
javac --release 21 -Xlint:all,-module -Werror -d "$OUT/m3-test-classes" -cp "$OUT/m3-classes" "$WT"/src/test/java/jdk/m3/arrays/M3BitMatrix*Contract.java "$WT"/src/test/java/jdk/m3/arrays/M3BitMatrixDump.java || fail "relocated contracts javac"
step "header prototypes (javac -h of donor vs relocated, class name mapped)"
sed "s/${donor_header%.h}/${relocated_header%.h}/g" "$OUT/donor-hdr/$donor_header" | diff - "$OUT/m3-hdr/$relocated_header" && echo "HEADERS IDENTICAL modulo names ($(grep -c JNIEXPORT "$OUT/m3-hdr/$relocated_header") prototypes)" || fail "javac -h headers differ"

step "gcc donor kernel (brief flags)"
gcc $FLAGS $INC -I"$OUT/donor-hdr" -include "$donor_header" "$OUT/donor-native/kernel.c" -o "$OUT/libdonor.so" && echo "donor C compiled" || fail "donor C gcc"
step "gcc relocated kernel (brief flags, prototype check through -include of the javac -h header)"
gcc $FLAGS $INC -I"$OUT/m3-hdr" -include "$relocated_header" "$WT/$relocated_c" -o "$OUT/librelocated.so" && echo "relocated C compiled" || fail "relocated C gcc"
step "gcc relocated kernel with the module's CMake flags (-Wconversion -Wshadow, c11)"
gcc $MODULE_FLAGS $INC "$WT/$relocated_c" -o "$OUT/relocated.o" && echo "module flags OK" || fail "module flags"
echo "exported relocated symbols: $(nm -D --defined-only "$OUT/librelocated.so" | grep -c "$relocated_symbol")"
echo "exported donor symbols: $(nm -D --defined-only "$OUT/libdonor.so" | grep -c "$donor_symbol")"
step "mutant kernels (one corrupted scalar store each; the Java parity check must refuse the library)"
MUTANT_OLD='| row_mask);'
MUTANT_NEW='| (row_mask \& UINT64_C(0)));'
for side in donor relocated; do
    src="$OUT/donor-native/kernel.c"; [ $side = relocated ] && src="$WT/$relocated_c"
    [ "$(grep -c "$MUTANT_OLD" "$src")" = 1 ] || fail "$side mutant anchor count"
    sed "s/$MUTANT_OLD/$MUTANT_NEW/" "$src" > "$OUT/mutant-$side.c"
    hdr="$OUT/donor-hdr"; inc="$donor_header"; [ $side = relocated ] && { hdr="$OUT/m3-hdr"; inc="$relocated_header"; }
    gcc $FLAGS $INC -I"$hdr" -include "$inc" "$OUT/mutant-$side.c" -o "$OUT/libmutant-$side.so" && echo "$side mutant compiled" || fail "$side mutant gcc"
done

run_pair() { # label donor-simple-name [extra java args]
    local label=$1 simple=$2 donor_fqn relocated_fqn; shift 2
    read -r donor_fqn relocated_fqn < <(fqn "$simple")
    java -Xcheck:jni -D"$donor_property"="$OUT/libdonor.so" -Dm3.bitmatrix.requireNative=true "$@" \
        -cp "$OUT/donor-classes" "$donor_fqn" > "$OUT/run/donor-$label.txt" 2> "$OUT/run/donor-$label.err" || fail "donor $label native run"
    java -Xcheck:jni -D"$relocated_property"="$OUT/librelocated.so" -Dm3.bitmatrix.requireNative=true "$@" \
        -cp "$OUT/m3-classes:$OUT/m3-test-classes" "$relocated_fqn" > "$OUT/run/m3-$label.txt" 2> "$OUT/run/m3-$label.err" || fail "relocated $label native run"
    if cmp -s "$OUT/run/donor-$label.txt" "$OUT/run/m3-$label.txt"; then v=IDENTICAL; else v=DIFFERENT; status=FAIL; fi
    echo "$label $v sha256=$(sha256sum "$OUT/run/m3-$label.txt" | cut -c1-16) $(tr '\n' ' ' < "$OUT/run/m3-$label.txt")"
}
step "native oracle runs with -Xcheck:jni (donor library vs relocated library; outputs compared)"
run_pair convergence MatBitMatrixConvergenceContract
run_pair tile MatBitMatrixTileContract
step "mutant refusal with -Xcheck:jni (donor mutant vs relocated mutant)"
read -r donor_conv relocated_conv < <(fqn MatBitMatrixConvergenceContract)
java -Xcheck:jni -D"$donor_property"="$OUT/libmutant-donor.so" -Dm3.bitmatrix.expectMutant=true -cp "$OUT/donor-classes" "$donor_conv" > "$OUT/run/donor-mutant.txt" 2> "$OUT/run/donor-mutant.err" || fail "donor mutant run"
java -Xcheck:jni -D"$relocated_property"="$OUT/libmutant-relocated.so" -Dm3.bitmatrix.expectMutant=true -cp "$OUT/m3-classes:$OUT/m3-test-classes" "$relocated_conv" > "$OUT/run/m3-mutant.txt" 2> "$OUT/run/m3-mutant.err" || fail "relocated mutant run"
cmp -s "$OUT/run/donor-mutant.txt" "$OUT/run/m3-mutant.txt" && echo "mutant IDENTICAL $(tr '\n' ' ' < "$OUT/run/m3-mutant.txt")" || fail "mutant outputs differ"
grep -q "MAT_BIT_MATRIX_NATIVE_MUTANT_REFUSED=PASS" "$OUT/run/m3-mutant.txt" || fail "relocated mutant not refused"
echo "jni warnings total: $(cat "$OUT"/run/*.err | grep -ciE "warning")"

step "native dump (transpose through Backend.NATIVE_REQUIRED, $ROUNDS random rounds) vs the Java lane dump of gate 3"
read -r donor_owner relocated_owner < <(fqn MatBitMatrix)
java -Xcheck:jni -D"$donor_property"="$OUT/libdonor.so" -Dm3.bitmatrix.dumpBackend=NATIVE_REQUIRED -cp "$OUT/donor-classes:$OUT/m3-test-classes" "$target_package.M3BitMatrixDump" "$donor_owner" "$ROUNDS" > "$OUT/run/donor-native-dump.txt" 2> "$OUT/run/donor-native-dump.err" || fail "donor native dump"
java -Xcheck:jni -D"$relocated_property"="$OUT/librelocated.so" -Dm3.bitmatrix.dumpBackend=NATIVE_REQUIRED -cp "$OUT/m3-classes:$OUT/m3-test-classes" "$target_package.M3BitMatrixDump" "$relocated_owner" "$ROUNDS" > "$OUT/run/m3-native-dump.txt" 2> "$OUT/run/m3-native-dump.err" || fail "relocated native dump"
cmp -s "$OUT/run/donor-native-dump.txt" "$OUT/run/m3-native-dump.txt" && echo "native dumps IDENTICAL donor vs relocated" || fail "native dumps differ"
if [ -f "$DIFF/out/relocated-dump.txt" ]; then
    # The Java lane dump of gate 3 replays the same inputs (its random rounds may exceed $ROUNDS): the native dump
    # must equal its prefix line for line, TOTAL lines excluded (they name the backend and the round count).
    grep -v '^TOTAL' "$OUT/run/m3-native-dump.txt" > "$OUT/run/native-body.txt"
    grep -v '^TOTAL' "$DIFF/out/relocated-dump.txt" | head -n "$(wc -l < "$OUT/run/native-body.txt")" > "$OUT/run/java-body.txt"
    cmp -s "$OUT/run/native-body.txt" "$OUT/run/java-body.txt" && echo "native dump IDENTICAL to the Java lane dump ($(wc -l < "$OUT/run/native-body.txt") lines compared)" || fail "native dump differs from the Java lane dump"
fi
echo "native dump: sha256=$(sha256sum "$OUT/run/m3-native-dump.txt" | cut -c1-16) $(tail -1 "$OUT/run/m3-native-dump.txt")"
echo "jni warnings (dumps): $(cat "$OUT"/run/*-dump.err | grep -ciE "warning")"

step "cmake build of the module native library (registration check) + ctest"
cmake -S "$WT/src/main/native" -B "$OUT/cmake" -DCMAKE_BUILD_TYPE=Release > "$OUT/cmake-configure.log" 2>&1 && cmake --build "$OUT/cmake" --config Release > "$OUT/cmake-build.log" 2>&1 && echo "cmake build OK (warnings: $(grep -ci warning "$OUT/cmake-build.log"))" || { fail "cmake"; tail -20 "$OUT/cmake-build.log"; }
(cd "$OUT/cmake" && ctest --output-on-failure -C Release > "$OUT/ctest.log" 2>&1 && grep "tests passed" "$OUT/ctest.log" || { fail ctest; tail -20 "$OUT/ctest.log"; })
echo "bit-matrix symbols in libm3_arrays.so: $(nm -D --defined-only "$OUT/cmake/libm3_arrays.so" 2>/dev/null | grep -c "$relocated_symbol")"
step "relocated oracles against libm3_arrays.so (the module library)"
for simple in MatBitMatrixConvergenceContract MatBitMatrixTileContract; do
    read -r _ relocated_fqn < <(fqn "$simple")
    java -Xcheck:jni -D"$relocated_property"="$OUT/cmake/libm3_arrays.so" -Dm3.bitmatrix.requireNative=true \
        -cp "$OUT/m3-classes:$OUT/m3-test-classes" "$relocated_fqn" > "$OUT/run/lib-${relocated_fqn##*.}.txt" 2>/dev/null \
        && echo "${relocated_fqn##*.} $(tr '\n' ' ' < "$OUT/run/lib-${relocated_fqn##*.}.txt")" || fail "libm3_arrays ${relocated_fqn##*.}"
done
echo "sources: donor_c_sha256=$(sha256sum "$OUT/donor-native/kernel.c" | cut -c1-64) relocated_c_sha256=$(sha256sum "$WT/$relocated_c" | cut -c1-64)"
echo "NATIVE_GATE $status"
[ "$status" = PASS ]
