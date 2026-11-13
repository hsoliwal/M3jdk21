#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Native gate of qualification/lane-kernel-native-20261106 for a Linux host (2026-11-06 run: WSL Ubuntu 24.04, gcc 13.3,
# Temurin 21.0.13). Library m3_collections_address = m3_long_address_kernel.c alone, as full-java-jni-20261018/verify.py
# and .github/workflows/long-index-gather-contract.yml build it (the module has no CMake for it).
#   1. javac -h of the two owners (M3LaneAlgorithms, M3LongAddressKernel) with the three contracts and the digest driver;
#      the four prototypes of the javac -h headers must equal the C definitions (types, names, return type).
#   2. gcc -std=c17 -O2 -Wall -Wextra -Wpedantic -Werror -fPIC -shared, through -include of both headers: 0 diagnostics.
#   3. nm -D: gatherNative + the four restored exports.
#   4. the three contracts, default and -Xint: Java path (no library) and native-required (-Xcheck:jni); 0 JNI warnings.
#   5. M3LaneKernelDigest: native vs Java outputs byte-identical, default and -Xint, two seeds.
#   6. one mutant per kernel (sed on a copy): the kernel's contract must fail in native-required mode.
# usage: native-gate.sh <out-dir>   (JDK: $JAVA_HOME or ~/jdk/jdk-21*)
set -uo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
MODULE=$(cd "$HERE/../.." && pwd)
OUT=$1
JDK=${JAVA_HOME:-$(ls -d "$HOME"/jdk/jdk-21* 2>/dev/null | head -1)}
export JAVA_HOME=$JDK PATH=$JDK/bin:$PATH
rm -rf "$OUT"; mkdir -p "$OUT"/{classes,hdr,lib,run,mut,src/jdk/m3}
status=PASS
step() { echo "== $*"; }
fail() { echo "FAIL: $*"; status=FAIL; }
C="$MODULE/src/main/native/collections/m3_long_address_kernel.c"
FLAGS="-std=c17 -O2 -Wall -Wextra -Wpedantic -Werror -fPIC -shared"
INC="-I$JDK/include -I$JDK/include/linux -I$OUT/hdr"
HDRS="-include jdk_m3_collections_M3LaneAlgorithms.h -include jdk_m3_collections_M3LongAddressKernel.h"
NAMES="gatherSelectedNative slidingMaximumIndices0 partition3Long0 partition3Int0"
CONTRACTS="M3LanePartition3PreparedContract M3MaskedGatherContract M3LaneSlidingMaximumContract"
NATIVE_PROPS="-Dm3.collections.lane.native=true -Dm3.test.native.required=true -Dm3.collections.partition3.native.required=true"
java -version 2>&1 | head -1; gcc --version | head -1
echo "C sha256=$(sha256sum "$C" | cut -c1-64) git_blob=$(git hash-object "$C" 2>/dev/null)"

step "javac -h (owners via -sourcepath without module-info, contracts, digest driver)"
ln -s "$MODULE/src/main/java/jdk/m3/collections" "$OUT/src/jdk/m3/collections"
ln -s "$MODULE/../arrays/src/main/java/jdk/m3/arrays" "$OUT/src/jdk/m3/arrays"
T="$MODULE/src/test/java/jdk/m3/collections"
javac --release 21 -proc:none -Xlint:all,-module -Werror -implicit:class -sourcepath "$OUT/src" -h "$OUT/hdr" -d "$OUT/classes" \
    "$MODULE/src/main/java/jdk/m3/collections/M3LaneAlgorithms.java" "$MODULE/src/main/java/jdk/m3/collections/M3LongAddressKernel.java" \
    "$T/M3LanePartition3PreparedContract.java" "$T/M3MaskedGatherContract.java" "$T/M3LaneSlidingMaximumContract.java" \
    "$HERE/M3LaneKernelDigest.java" || fail "javac"
ls "$OUT/hdr"

step "javac -h prototypes vs C definitions (the four restored exports)"
python3 - "$OUT/hdr" "$C" $NAMES > "$OUT/prototypes.txt" <<'PY' || fail "prototype mismatch"
import re, sys, pathlib
hdr, src, names = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2]).read_text(), sys.argv[3:]
heads = ''.join(p.read_text() for p in sorted(hdr.glob('*.h')))
norm = lambda s: re.sub(r'\s+', ' ', s).replace(' *', '*').strip()
bad = 0
for name in names:
    h = re.search(r'JNIEXPORT (\w+) JNICALL (Java_\w+_' + name + r')\s*\(([^)]*)\);', heads)
    c = re.search(r'JNIEXPORT (\w+) JNICALL\s*(Java_\w+_' + name + r')\(([^)]*)\)\s*\{', src)
    ctypes = [norm(re.sub(r'\w+$', '', p.strip())) for p in c.group(3).split(',')] if c else []
    htypes = [norm(p) for p in h.group(3).split(',')] if h else []
    same = bool(h and c and h.group(1) == c.group(1) and h.group(2) == c.group(2) and htypes == ctypes)
    bad += not same
    print(('EQUAL ' if same else 'DIFFERENT ') + (f'{h.group(1)} {h.group(2)}({", ".join(htypes)})' if h else name + ' absent from header')
          + ('' if same else f' | C: {c.group(1) + " " + c.group(2) + str(ctypes) if c else "absent"}'))
sys.exit(1 if bad else 0)
PY
cat "$OUT/prototypes.txt"

step "gcc libm3_collections_address.so through -include of both javac -h headers"
gcc $FLAGS $INC $HDRS "$C" -o "$OUT/lib/libm3_collections_address.so" 2> "$OUT/gcc.log" || fail "gcc"
echo "gcc diagnostics: $(grep -cE 'warning:|error:' "$OUT/gcc.log")"
gcc $FLAGS -I"$JDK/include" -I"$JDK/include/linux" "$C" -o "$OUT/plain.so" 2> "$OUT/gcc-plain.log" || fail "gcc without -include"
echo "gcc diagnostics without -include: $(grep -cE 'warning:|error:' "$OUT/gcc-plain.log")"
gcc -std=c17 -O2 -Wall -Wextra -Wpedantic -Wconversion -Wshadow -fPIC -c $INC "$C" -o "$OUT/wconv.o" 2> "$OUT/wconversion.log"
echo "-Wconversion -Wshadow warnings (information, flags of long-index-gather-contract.yml): $(grep -c 'warning:' "$OUT/wconversion.log")"

step "nm -D exports"
nm -D --defined-only "$OUT/lib/libm3_collections_address.so" | grep ' Java_' | tee "$OUT/exports.txt"
for n in $NAMES; do grep -q "_$n\$" "$OUT/exports.txt" || fail "export $n missing"; done
echo "exports: $(wc -l < "$OUT/exports.txt") (restored: $(grep -cE "_($(echo $NAMES | tr ' ' '|'))\$" "$OUT/exports.txt"))"

run_contract() { # <name> <vm> <mode> <libdir>
    local opts=(); [ "$2" = xint ] && opts=(-Xint)
    if [ "$3" = native ]; then
        java -Xcheck:jni "${opts[@]}" -Djava.library.path="$4" $NATIVE_PROPS -cp "$OUT/classes" "jdk.m3.collections.$1"
    else
        java "${opts[@]}" -Djava.library.path="$OUT/missing" -cp "$OUT/classes" "jdk.m3.collections.$1"
    fi
}
step "contracts: Java path and native-required (-Xcheck:jni), default and -Xint"
for c in $CONTRACTS; do
    for vm in default xint; do
        for mode in java native; do
            run_contract "$c" "$vm" "$mode" "$OUT/lib" > "$OUT/run/$c-$vm-$mode.out" 2> "$OUT/run/$c-$vm-$mode.err" || fail "$c $vm $mode"
            echo "$c $vm $mode: $(cat "$OUT/run/$c-$vm-$mode.out")"
        done
    done
done
echo "jni warnings (native-required stderr): $(cat "$OUT"/run/*-native.err | grep -ciE 'warning')"
echo "stderr bytes (all contract runs): $(cat "$OUT"/run/*.err | wc -c)"

step "digest: native vs Java outputs, default and -Xint, two seeds"
for seed in 20261106 75239; do
    for vm in default xint; do
        opts=(); [ "$vm" = xint ] && opts=(-Xint)
        java "${opts[@]}" -Djava.library.path="$OUT/missing" -cp "$OUT/classes" jdk.m3.collections.M3LaneKernelDigest java $seed > "$OUT/run/digest-$seed-$vm-java.txt" || fail "digest java $seed $vm"
        java -Xcheck:jni "${opts[@]}" -Djava.library.path="$OUT/lib" -Dm3.collections.lane.native=true -cp "$OUT/classes" jdk.m3.collections.M3LaneKernelDigest native $seed > "$OUT/run/digest-$seed-$vm-native.txt" 2> "$OUT/run/digest-$seed-$vm-native.err" || fail "digest native $seed $vm"
        if cmp -s "$OUT/run/digest-$seed-$vm-java.txt" "$OUT/run/digest-$seed-$vm-native.txt"; then v=IDENTICAL; else v=DIFFERENT; status=FAIL; fi
        echo "seed=$seed $vm native=java $v ($(wc -l < "$OUT/run/digest-$seed-$vm-native.txt") kernels)"
    done
    cmp -s "$OUT/run/digest-$seed-default-java.txt" "$OUT/run/digest-$seed-xint-java.txt" || fail "java digest differs across VM modes"
    cat "$OUT/run/digest-$seed-default-java.txt"
done
echo "digest jni warnings: $(cat "$OUT"/run/digest-*-native.err | grep -ciE 'warning')"

step "mutants: one per kernel, the kernel's contract in native-required mode must fail"
mutant() { # <id> <function> <from> <to> <contract>
    local dir="$OUT/mut/$1"; mkdir -p "$dir"
    sed "/_$2(\$/,/^}/ s/$3/$4/" "$C" > "$dir/m.c"
    if cmp -s "$C" "$dir/m.c"; then fail "mutant $1 did not apply"; return; fi
    gcc $FLAGS $INC $HDRS "$dir/m.c" -o "$dir/libm3_collections_address.so" 2> "$dir/gcc.log" || { fail "mutant $1 gcc"; return; }
    if run_contract "$5" default native "$dir" > "$dir/run.out" 2> "$dir/run.err"; then
        fail "mutant $1 SURVIVED"; echo "$1 $2: SURVIVED"
    else
        echo "$1 $2: CAUGHT by $5 ($(grep -m1 -oE 'AssertionError: .{0,70}' "$dir/run.err"))"
    fi
}
mutant M1 gatherSelectedNative 'gathered\[selected++\] = values\[indices\[i\]\];' 'gathered[selected++] = values[indices[i]] ^ 1;' M3MaskedGatherContract
mutant M2 slidingMaximumIndices0 'if (candidate\[tail\] >= value) break;' 'if (candidate[tail] > value) break;' M3LaneSlidingMaximumContract
mutant M3 partition3Long0 'if (value < pivot) {' 'if (value <= pivot) {' M3LanePartition3PreparedContract
mutant M4 partition3Int0 'if (value < pivot) {' 'if (value <= pivot) {' M3LanePartition3PreparedContract
echo "NATIVE_GATE $status"
[ "$status" = PASS ]
