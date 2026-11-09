#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Native gate of recipe planner-native-20261107, for a Linux host (the 2026-11-07 run: WSL Ubuntu 24.04, gcc 13.3,
# cmake 3.28, OpenJDK 21). Inputs: the module tree after `mvn package` (target/classes, target/test-classes) and the
# donor side written by prepare-donor.sh (<donor-dir>/src, <donor-dir>/kernels).
#   1. javac -h of the donor JNI host and of the relocated host: the headers are identical modulo the mapped names
#      (4 prototypes).
#   2. gcc of each relocated kernel with the module's flags (-std=c11 -Wall -Wextra -Werror -Wconversion -Wshadow
#      -Wpedantic) through -include of the relocated javac -h header (a definition that disagrees with its prototype
#      fails), and of each donor kernel with its donor header.
#   3. the module's CMake build of libm3_precompute with the appended registration (Release, BUILD_TESTING), ctest, and
#      nm -D: the 4 M3NativePrecomputeOps symbols exported beside the module's existing ones.
#   4. M3IndexPrecomputePlannerDump (strict) under -Xcheck:jni with the library on java.library.path (NativeProbe:
#      available=true tuples=true) and with -Dm3.precompute.disableNative=true (the Java path): byte-identical on the JIT
#      (CASES) and under -Xint (XINT_CASES); 0 JNI warnings. The dump also checks every native batch against its model.
#   5. two mutant libraries (one corrupted result store in each kernel): the strict dump must abort on each.
# Prints NATIVE_GATE PASS|FAIL.
# Every JVM runs with -XX:-UsePerfData and java.io.tmpdir under <out-dir>, and TMPDIR/HOME should point under the
# scratch disk, so nothing is written to the Linux root image. CMake's FindJNI needs a full JDK (AWT headers).
# usage: native-gate.sh <donor-dir> <out-dir> [CASES=4000] [XINT_CASES=600]   (JDK: $JAVA_HOME, a full JDK 21)
set -uo pipefail
RECIPE_DIR=$(cd "$(dirname "$0")" && pwd)
MODULE=$(cd "$RECIPE_DIR/../.." && pwd)
DONOR=$1; OUT=$2; CASES=${3:-4000}; XINT_CASES=${4:-600}
JDK=${JAVA_HOME:?JAVA_HOME must point at a full JDK 21}
export JAVA_HOME=$JDK PATH=$JDK/bin:$PATH
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
FAMILY_MAP="$RECIPE_DIR/../planner-family-20261107/RELOCATION_MAP.tsv"
rm -rf "$OUT"; mkdir -p "$OUT"/{donor-classes,donor-hdr,m3-classes,m3-hdr,objects,tmp}
status=PASS
step() { echo "== $*"; }
fail() { echo "FAIL: $*"; status=FAIL; }
FLAGS="-std=c11 -O2 -fPIC -Wall -Wextra -Werror -Wconversion -Wshadow -Wpedantic"
INC="-I$JDK/include -I$JDK/include/linux"
java -XX:-UsePerfData -version 2>&1 | head -1; gcc --version | head -1; cmake --version | head -1
package=$(awk -F'\t' '$1=="PACKAGE" {print $3; exit}' "$MAP")
donor_symbol=$(awk -F'\t' '$1=="JNI" {print $2; exit}' "$MAP")
symbol=$(awk -F'\t' '$1=="JNI" {print $3; exit}' "$MAP")
donor_header="${donor_symbol#Java_}"; donor_header="${donor_header%_}.h"
header="${symbol#Java_}"; header="${header%_}.h"
host="${header%.h}"; host="${host##*_}"
domain=$(awk -F'\t' '$1=="LITERAL" && $2 ~ /:$/ {print $3; exit}' "$FAMILY_MAP")
CP="$MODULE/target/classes:$MODULE/target/test-classes"

step "1. javac -h headers (donor host vs relocated host)"
donor_host=$(find "$DONOR/src" -name "${host#M3}.java" | head -1)
javac -J-XX:-UsePerfData --release 21 -proc:none -h "$OUT/donor-hdr" -d "$OUT/donor-classes" -sourcepath "$DONOR/src" "$donor_host" || fail "donor javac -h"
javac -J-XX:-UsePerfData --release 21 -proc:none -h "$OUT/m3-hdr" -d "$OUT/m3-classes" -sourcepath "$MODULE/src/main/java" \
    "$MODULE/src/main/java/${package//.//}/$host.java" || fail "relocated javac -h"
prototypes=$(grep -c JNIEXPORT "$OUT/m3-hdr/$header" || true)
sed "s/${donor_header%.h}/${header%.h}/g" "$OUT/donor-hdr/$donor_header" | diff - "$OUT/m3-hdr/$header" > "$OUT/header.diff" \
    && echo "HEADERS IDENTICAL modulo names ($prototypes prototypes)" || { cat "$OUT/header.diff"; fail "javac -h headers differ"; }

step "2. gcc of every kernel with the module flags and its javac -h header"
while IFS=$'\t' read -r kind donor target _note; do
    [ "$kind" = FILE ] || continue
    name=$(basename "$target" .c)
    gcc $FLAGS $INC -I"$OUT/m3-hdr" -include "$header" -c "$MODULE/$target" -o "$OUT/objects/$name.o" \
        && echo "relocated $name OK" || fail "relocated $name gcc"
    gcc $FLAGS $INC -I"$OUT/donor-hdr" -include "$donor_header" -c "$DONOR/kernels/$name.c" -o "$OUT/objects/donor-$name.o" \
        && echo "donor $name OK" || fail "donor $name gcc"
done < "$MAP"

step "3. module CMake build of libm3_precompute, ctest, nm -D"
cmake -S "$MODULE/src/main/native" -B "$OUT/native" -DCMAKE_BUILD_TYPE=Release -DBUILD_TESTING=ON -DCMAKE_C_COMPILER=gcc > "$OUT/cmake-configure.log" 2>&1 \
    || { tail -20 "$OUT/cmake-configure.log"; fail "cmake configure"; }
cmake --build "$OUT/native" --config Release > "$OUT/cmake-build.log" 2>&1 && echo "build OK ($(grep -c 'warning' "$OUT/cmake-build.log" || true) warning lines)" \
    || { tail -30 "$OUT/cmake-build.log"; fail "cmake build"; }
(cd "$OUT/native" && ctest --output-on-failure -C Release) > "$OUT/ctest.log" 2>&1 && echo "ctest: $(grep -E 'tests passed' "$OUT/ctest.log")" \
    || { tail -20 "$OUT/ctest.log"; fail "ctest"; }
LIB="$OUT/native/libm3_precompute.so"
exported=$(nm -D --defined-only "$LIB" | grep -c "$symbol" || true)
echo "nm -D: $exported $symbol* symbols; all Java_ exports: $(nm -D --defined-only "$LIB" | grep -c ' Java_' || true)"
nm -D --defined-only "$LIB" | grep "$symbol" | awk '{print "  " $3}'
[ "$exported" = "$prototypes" ] || fail "exported $exported != $prototypes prototypes"

step "4. native vs Java dump under -Xcheck:jni"
run() { # run <name> <mode: native|java> <cases> [jvm flag]
    local flags="-XX:-UsePerfData -Xcheck:jni -Djava.io.tmpdir=$OUT/tmp -Djava.library.path=$OUT/native"
    [ "$2" = java ] && flags="$flags -Dm3.precompute.disableNative=true"
    java $flags ${4:-} -cp "$CP" "$RECIPE_DIR/NativeProbe.java" > "$OUT/$1.probe" 2>&1
    java $flags ${4:-} -Xss1m -cp "$CP" "$package.M3IndexPrecomputePlannerDump" "$package" M3 "$domain" "$3" strict \
        > "$OUT/$1.txt" 2> "$OUT/$1.err"
    echo "$1: $(grep NATIVE_PROBE "$OUT/$1.probe") $(tail -n 1 "$OUT/$1.txt") jni_warnings=$(grep -c 'WARNING' "$OUT/$1.err" || true) stderr_lines=$(wc -l < "$OUT/$1.err")"
}
compare() {
    if cmp -s "$OUT/$1.txt" "$OUT/$2.txt"; then echo "$1 vs $2 IDENTICAL sha256=$(sha256sum < "$OUT/$1.txt" | cut -c1-16) lines=$(wc -l < "$OUT/$1.txt")"
    else echo "$1 vs $2 DIFFERENT"; diff "$OUT/$1.txt" "$OUT/$2.txt" | head -5; status=FAIL; fi
}
run native-dump native "$CASES"; run java-dump java "$CASES"; compare native-dump java-dump
run native-dump-xint native "$XINT_CASES" -Xint; run java-dump-xint java "$XINT_CASES" -Xint; compare native-dump-xint java-dump-xint
grep -q 'available=true tuples=true' "$OUT/native-dump.probe" || fail "native kernels not loaded"
grep -q 'available=false tuples=false' "$OUT/java-dump.probe" || fail "Java path not forced"
for f in native-dump native-dump-xint; do [ "$(grep -c WARNING "$OUT/$f.err" || true)" = 0 ] && [ ! -s "$OUT/$f.err" ] || fail "$f stderr/JNI warnings"; done

step "5. mutant kernels (the strict dump must abort)"
mutant() { # mutant <name> <kernel target> <old> <new>
    local dir="$OUT/mutant-$1"; mkdir -p "$dir/src"
    cp -r "$MODULE/src/main/native/." "$dir/src/"
    [ "$(grep -cF "$3" "$dir/src/${2#src/main/native/}")" = 1 ] || { fail "mutant $1 anchor"; return; }
    python3 - "$dir/src/${2#src/main/native/}" "$3" "$4" <<'PY'
import sys
from pathlib import Path
p = Path(sys.argv[1]); p.write_text(p.read_text().replace(sys.argv[2], sys.argv[3]))
PY
    cmake -S "$dir/src" -B "$dir/build" -DCMAKE_BUILD_TYPE=Release -DBUILD_TESTING=OFF > /dev/null 2>&1 && cmake --build "$dir/build" > /dev/null 2>&1 \
        || { fail "mutant $1 build"; return; }
    if java -XX:-UsePerfData -Xcheck:jni -Djava.io.tmpdir="$OUT/tmp" -Djava.library.path="$dir/build" -Xss1m -cp "$CP" "$package.M3IndexPrecomputePlannerDump" \
        "$package" M3 "$domain" 400 strict > "$dir/dump.txt" 2> "$dir/dump.err"; then
        echo "MUTANT $1: SURVIVED"; status=FAIL
    else
        echo "MUTANT $1: KILLED ($(grep -m1 -oE 'reference drift: [^>]*' "$dir/dump.err" | head -c 90))"
    fi
}
mutant pair-result src/main/native/src/m3_precompute_pair_jni.c "        result = values[row];" "        result = values[row] + 1;"
mutant tuple-result src/main/native/src/m3_precompute_tuple_jni.c "if (equal != 0) { result = values[row]; break; }" "if (equal != 0) { result = values[row] ^ 1; break; }"
echo "NATIVE_GATE $status"
[ "$status" = PASS ]
