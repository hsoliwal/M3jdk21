#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Native gate of qualification/mergeloss-s1-20261106 for a Linux host (2026-11-07 run: WSL Ubuntu 24.04, gcc 13.3,
# Temurin 21.0.13). Library m3_collections_pathmask = m3_index_path_masks.c alone, as the path-mask CI workflows build it
# (m3-five-provider-current-selection-20261017.yml, m3-index-path-mask-window-20261017.yml).
#   1. javac -h of M3IndexPathMasks with its three contracts and the digest driver; the two prototypes of the header
#      (selectNative, selectLongNative) must equal the C definitions (types, names, return type).
#   2. gcc -std=c17 -O2 -Wall -Wextra -Wpedantic -Werror -fPIC -shared through -include of the header: 0 diagnostics;
#      the UBSan build of the CI workflows (-O1 -g -fsanitize=undefined -fno-sanitize-recover=all) also compiles.
#   3. nm -D: selectNative + the restored selectLongNative.
#   4. contracts (M3IndexPathMasksSelfTest, M3IndexPathMasksLongSelfTest, M3IndexPathMasksAliasContract), default and
#      -Xint: Java path (no library) and native (-Xcheck:jni); 0 JNI warnings; the long contract also on the UBSan build.
#   5. M3PathMaskRestoreDigest: native vs Java byte-identical (default and -Xint, two seeds); with BASE given, master
#      classes + master library vs restored classes + restored library byte-identical on the four existing int ops.
#   6. L3 (merge-loss leaf 20): the origin 0e68366337be's own oracles, oracle/M3IndexPathNativeBatchContract.java
#      (--java-only and --native, default and -Xint, -Xcheck:jni) and the instrumented-JNI region test
#      oracle/m3_path_mask_output_regions.c (--candidate on this C; with BASE: --baseline on master's C, equal input
#      traces, and --candidate on master's C must fail).
#   7. mutants (sed on copies): C long-row output size, C long-row write index, C cached-path guard, Java long kernel
#      value, Java row-size preflight (the long contract must fail in the mutated mode); L3 final flush dropped (the
#      region test and the batch contract must fail).
# usage: TMPDIR=<tmp> native-gate.sh <out-dir> <head-module-dir> [<base-module-dir>]   (JDK: $JAVA_HOME or ~/jdk/jdk-21*)
set -uo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
OUT=$1
MODULE=$(cd "$2" && pwd)
BASE=${3:-}
JDK=${JAVA_HOME:-$(ls -d "$HOME"/jdk/jdk-21* 2>/dev/null | head -1)}
export JAVA_HOME=$JDK PATH=$JDK/bin:$PATH
# Temp files only under $TMPDIR (the caller points TMPDIR/HOME off the WSL system disk).
JTMP=${TMPDIR:-/tmp}
JOPT="-Djava.io.tmpdir=$JTMP"
rm -rf "$OUT"; mkdir -p "$OUT"/{classes,hdr,lib,ubsan,run,mut}
cd "$OUT" || exit 9   # hs_err/replay files of a crashing mutant land here
status=PASS
step() { echo "== $*"; }
fail() { echo "FAIL: $*"; status=FAIL; }
FLAGS="-std=c17 -O2 -Wall -Wextra -Wpedantic -Werror -fPIC -shared"
INC="-I$JDK/include -I$JDK/include/linux -I$OUT/hdr"
HDR="-include jdk_m3_collections_M3IndexPathMasks.h"
NAMES="selectNative selectLongNative"
J=src/main/java/jdk/m3/collections
T=src/test/java/jdk/m3/collections
CONTRACTS="M3IndexPathMasksSelfTest M3IndexPathMasksLongSelfTest M3IndexPathMasksAliasContract"
java -version 2>&1 | head -1; gcc --version | head -1
C="$MODULE/src/main/native/collections/m3_index_path_masks.c"
echo "C sha256=$(sha256sum "$C" | cut -c1-64)  Java sha256=$(sha256sum "$MODULE/$J/M3IndexPathMasks.java" | cut -c1-64)"

compile_java() { # <module> <classes> [<hdr>]
    local h=(); [ -n "${3:-}" ] && h=(-h "$3")
    javac -J$JOPT --release 21 -proc:none -Xlint:all,-module -Werror "${h[@]}" -d "$2" "$1/$J/M3IndexPathMasks.java" \
        "$1/$T/M3IndexPathMasksSelfTest.java" "$1/$T/M3IndexPathMasksAliasContract.java" \
        $( [ -f "$1/$T/M3IndexPathMasksLongSelfTest.java" ] && echo "$1/$T/M3IndexPathMasksLongSelfTest.java" ) \
        "$HERE/M3PathMaskRestoreDigest.java" "$HERE/oracle/M3IndexPathNativeBatchContract.java"
}
step "javac -h (owner, contracts, digest driver)"
compile_java "$MODULE" "$OUT/classes" "$OUT/hdr" || fail "javac"
ls "$OUT/hdr"

step "javac -h prototypes vs C definitions"
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

step "gcc libm3_collections_pathmask.so through -include of the javac -h header"
gcc $FLAGS $INC $HDR "$C" -o "$OUT/lib/libm3_collections_pathmask.so" 2> "$OUT/gcc.log" || fail "gcc"
echo "gcc diagnostics: $(grep -cE 'warning:|error:' "$OUT/gcc.log")"
gcc $FLAGS -I"$JDK/include" -I"$JDK/include/linux" "$C" -o "$OUT/plain.so" 2> "$OUT/gcc-plain.log" || fail "gcc without -include"
echo "gcc diagnostics without -include: $(grep -cE 'warning:|error:' "$OUT/gcc-plain.log")"
gcc -std=c17 -O1 -g -Wall -Wextra -Werror -fPIC -shared -fsanitize=undefined -fno-sanitize-recover=all $INC "$C" \
    -o "$OUT/ubsan/libm3_collections_pathmask.so" 2> "$OUT/gcc-ubsan.log" || fail "gcc ubsan"
echo "gcc ubsan diagnostics: $(grep -cE 'warning:|error:' "$OUT/gcc-ubsan.log")"
gcc -std=c17 -O2 -Wall -Wextra -Wpedantic -Wconversion -Wshadow -fPIC -c $INC "$C" -o "$OUT/wconv.o" 2> "$OUT/wconversion.log"
echo "-Wconversion -Wshadow warnings (information): $(grep -c 'warning:' "$OUT/wconversion.log")"

step "nm -D exports"
nm -D --defined-only "$OUT/lib/libm3_collections_pathmask.so" | grep ' Java_' | tee "$OUT/exports.txt"
for n in $NAMES; do grep -q "_$n\$" "$OUT/exports.txt" || fail "export $n missing"; done
echo "exports: $(wc -l < "$OUT/exports.txt")"

run_contract() { # <classes> <name> <vm> <mode> <libdir>
    local opts=(); [ "$3" = xint ] && opts=(-Xint)
    if [ "$4" = native ]; then
        java $JOPT -Xcheck:jni "${opts[@]}" -Djava.library.path="$5" -cp "$1" "jdk.m3.collections.$2" --native
    else
        java $JOPT "${opts[@]}" -Djava.library.path="$OUT/missing" -cp "$1" "jdk.m3.collections.$2"
    fi
}
step "contracts: Java path and native (-Xcheck:jni), default and -Xint"
for c in $CONTRACTS; do
    for vm in default xint; do
        for mode in java native; do
            run_contract "$OUT/classes" "$c" "$vm" "$mode" "$OUT/lib" > "$OUT/run/$c-$vm-$mode.out" 2> "$OUT/run/$c-$vm-$mode.err" || fail "$c $vm $mode"
            echo "$c $vm $mode: $(cat "$OUT/run/$c-$vm-$mode.out")"
        done
    done
done
run_contract "$OUT/classes" M3IndexPathMasksLongSelfTest default native "$OUT/ubsan" > "$OUT/run/long-ubsan.out" 2> "$OUT/run/long-ubsan.err" || fail "long contract on the UBSan build"
echo "M3IndexPathMasksLongSelfTest ubsan native: $(cat "$OUT/run/long-ubsan.out") stderr_bytes=$(wc -c < "$OUT/run/long-ubsan.err")"
echo "jni warnings (native stderr): $(cat "$OUT"/run/*-native.err | grep -ciE 'warning')"
echo "stderr bytes (all contract runs): $(cat "$OUT"/run/*.err | wc -c)"

digest() { # <classes> <libdir> <mode> <seed> <vm> <file>
    local opts=(); [ "$5" = xint ] && opts=(-Xint)
    if [ "$3" = native ]; then
        java $JOPT -Xcheck:jni "${opts[@]}" -Djava.library.path="$2" -cp "$1" jdk.m3.collections.M3PathMaskRestoreDigest native "$4" > "$6" 2> "$6.err"
    else
        java $JOPT "${opts[@]}" -Djava.library.path="$OUT/missing" -cp "$1" jdk.m3.collections.M3PathMaskRestoreDigest java "$4" > "$6" 2> "$6.err"
    fi
}
step "digest: native vs Java outputs, default and -Xint, two seeds"
for seed in 20261106 75239; do
    for vm in default xint; do
        digest "$OUT/classes" "$OUT/lib" java $seed $vm "$OUT/run/digest-$seed-$vm-java.txt" || fail "digest java $seed $vm"
        digest "$OUT/classes" "$OUT/lib" native $seed $vm "$OUT/run/digest-$seed-$vm-native.txt" || fail "digest native $seed $vm"
        if cmp -s "$OUT/run/digest-$seed-$vm-java.txt" "$OUT/run/digest-$seed-$vm-native.txt"; then v=IDENTICAL; else v=DIFFERENT; status=FAIL; fi
        echo "seed=$seed $vm native=java $v"
    done
    cmp -s "$OUT/run/digest-$seed-default-java.txt" "$OUT/run/digest-$seed-xint-java.txt" || fail "java digest differs across VM modes"
    cat "$OUT/run/digest-$seed-default-java.txt"
done
echo "digest jni warnings: $(cat "$OUT"/run/digest-*-native.txt.err | grep -ciE 'warning')"

if [ -n "$BASE" ]; then
    step "master vs restored: the four existing int ops, Java and native"
    mkdir -p "$OUT/base/classes" "$OUT/base/lib"
    compile_java "$BASE" "$OUT/base/classes" || fail "javac base"
    gcc $FLAGS -I"$JDK/include" -I"$JDK/include/linux" "$BASE/src/main/native/collections/m3_index_path_masks.c" \
        -o "$OUT/base/lib/libm3_collections_pathmask.so" 2> "$OUT/base/gcc.log" || fail "gcc base"
    nm -D --defined-only "$OUT/base/lib/libm3_collections_pathmask.so" | grep ' Java_' > "$OUT/base/exports.txt"
    echo "base exports: $(wc -l < "$OUT/base/exports.txt")"
    for seed in 20261106 75239; do
        for mode in java native; do
            for vm in default xint; do
                digest "$OUT/base/classes" "$OUT/base/lib" $mode $seed $vm "$OUT/run/base-$seed-$vm-$mode.txt" || fail "base digest $mode $seed $vm"
                if diff <(head -4 "$OUT/run/base-$seed-$vm-$mode.txt") <(head -4 "$OUT/run/digest-$seed-$vm-$mode.txt") > /dev/null; then v=IDENTICAL; else v=DIFFERENT; status=FAIL; fi
                echo "seed=$seed $mode $vm master=restored (4 int ops) $v; master selectLong: $(sed -n 5p "$OUT/run/base-$seed-$vm-$mode.txt" | grep -o 'sha256=.*')"
            done
        done
    done
fi

step "L3 (merge-loss leaf 20): origin 0e68366337be oracles"
batch() { # <classes> <vm> <mode> <libdir>
    local opts=(); [ "$2" = xint ] && opts=(-Xint)
    if [ "$3" = native ]; then
        java $JOPT -Xcheck:jni "${opts[@]}" -Djava.library.path="$4" -cp "$1" jdk.m3.collections.M3IndexPathNativeBatchContract --native
    else
        java $JOPT "${opts[@]}" -Djava.library.path="$OUT/missing" -cp "$1" jdk.m3.collections.M3IndexPathNativeBatchContract --java-only
    fi
}
for vm in default xint; do
    for mode in java native; do
        batch "$OUT/classes" $vm $mode "$OUT/lib" > "$OUT/run/batch-$vm-$mode.out" 2> "$OUT/run/batch-$vm-$mode.err" || fail "batch contract $vm $mode"
        echo "M3IndexPathNativeBatchContract $vm $mode: $(tail -1 "$OUT/run/batch-$vm-$mode.out") jni_warnings=$(grep -ci warning "$OUT/run/batch-$vm-$mode.err")"
    done
done
regions() { # <c-file> <exe>
    gcc -std=c17 -O2 -Wall -Wextra -I"$JDK/include" -I"$JDK/include/linux" -I"$(dirname "$1")" -I"$(dirname "$C")" \
        "$HERE/oracle/m3_path_mask_output_regions.c" "$1" -o "$2" 2> "$2.gcc.log"
}
regions "$C" "$OUT/regions-head" || fail "regions gcc"
echo "region test gcc diagnostics: $(grep -cE 'warning:|error:' "$OUT/regions-head.gcc.log")"
"$OUT/regions-head" --candidate > "$OUT/run/regions-head.out" 2> "$OUT/run/regions-head.err" || fail "regions --candidate on this C"
echo "regions --candidate (this C): $(cat "$OUT/run/regions-head.out") $(head -1 "$OUT/run/regions-head.err")"
if [ -n "$BASE" ]; then
    regions "$BASE/src/main/native/collections/m3_index_path_masks.c" "$OUT/regions-base" || fail "regions gcc base"
    "$OUT/regions-base" --baseline > "$OUT/run/regions-base.out" 2> "$OUT/run/regions-base.err" || fail "regions --baseline on master C"
    echo "regions --baseline (master C): $(cat "$OUT/run/regions-base.out")"
    tb=$(grep -o 'input_trace=[0-9a-f]*' "$OUT/run/regions-base.out"); th=$(grep -o 'input_trace=[0-9a-f]*' "$OUT/run/regions-head.out")
    [ -n "$tb" ] && [ "$tb" = "$th" ] && echo "input traces equal: $th" || fail "input traces differ ($tb vs $th)"
    if "$OUT/regions-base" --candidate > "$OUT/run/regions-base-cand.out" 2> "$OUT/run/regions-base-cand.err"; then
        fail "regions --candidate passed on master C (no discrimination)"
    else
        echo "regions --candidate (master C): FAILS as expected ($(head -1 "$OUT/run/regions-base-cand.err"))"
    fi
fi

step "mutants: the long contract must fail in the mutated mode"
cmutant() { # <id> <sed> <description>
    local dir="$OUT/mut/$1"; mkdir -p "$dir"
    sed "$2" "$C" > "$dir/m.c"
    if cmp -s "$C" "$dir/m.c"; then fail "mutant $1 did not apply"; return; fi
    gcc $FLAGS $INC -I"$(dirname "$C")" $HDR "$dir/m.c" -o "$dir/libm3_collections_pathmask.so" 2> "$dir/gcc.log" || { fail "mutant $1 gcc"; return; }
    if run_contract "$OUT/classes" M3IndexPathMasksLongSelfTest default native "$dir" > "$dir/run.out" 2> "$dir/run.err"; then
        fail "mutant $1 SURVIVED"; echo "$1 $3: SURVIVED"
    else
        echo "$1 $3: CAUGHT ($(grep -h -m1 -E 'AssertionError|Exception:|FATAL ERROR' "$dir/run.err" "$dir/run.out" | head -1 | cut -c1-140))"
    fi
}
jmutant() { # <id> <sed> <description>
    local dir="$OUT/mut/$1"; mkdir -p "$dir/src/$J" "$dir/classes"
    mkdir -p "$dir/src/src"; cp -r "$MODULE/src/test" "$dir/src/src/"
    sed "$2" "$MODULE/$J/M3IndexPathMasks.java" > "$dir/src/$J/M3IndexPathMasks.java"
    if cmp -s "$MODULE/$J/M3IndexPathMasks.java" "$dir/src/$J/M3IndexPathMasks.java"; then fail "mutant $1 did not apply"; return; fi
    compile_java "$dir/src" "$dir/classes" > "$dir/javac.log" 2>&1 || { fail "mutant $1 javac"; return; }
    if run_contract "$dir/classes" M3IndexPathMasksLongSelfTest default java "$OUT/lib" > "$dir/run.out" 2> "$dir/run.err"; then
        fail "mutant $1 SURVIVED"; echo "$1 $3: SURVIVED"
    else
        echo "$1 $3: CAUGHT ($(grep -m1 -E 'AssertionError|Exception:' "$dir/run.err" | cut -c1-140))"
    fi
}
cmutant M1 's/(jarray)(\*env)->NewLongArray(env, matching)/(jarray)(*env)->NewLongArray(env, matching + 1)/' 'long output one longer'
cmutant M2 's/SetLongArrayRegion(env, (jlongArray)output, written, 1, &row)/SetLongArrayRegion(env, (jlongArray)output, written \/ 2, 1, \&row)/' 'long write index halved'
cmutant M3 's/if (!long_rows \&\& stages <= M3_CACHED_STAGE_LIMIT/if (stages <= M3_CACHED_STAGE_LIMIT/' 'long rows routed to the int-only cached path'
jmutant M4 '/static long\[\] selectLongJava/,/^    }/ s/selected\[written++\] = rows\[index\];/selected[written++] = rows[index] ^ 1L;/' 'Java long kernel value'
jmutant M5 's/if (rowSize != positions.length) {/if (rowSize < positions.length) {/' 'row-size preflight'
l3mutant() { # <id> <sed> <description>: the region test and the batch contract must both fail
    local dir="$OUT/mut/$1"; mkdir -p "$dir"
    sed "$2" "$C" > "$dir/m.c"
    if cmp -s "$C" "$dir/m.c"; then fail "mutant $1 did not apply"; return; fi
    gcc $FLAGS $INC -I"$(dirname "$C")" $HDR "$dir/m.c" -o "$dir/libm3_collections_pathmask.so" 2> "$dir/gcc.log" || { fail "mutant $1 gcc"; return; }
    regions "$dir/m.c" "$dir/regions" || { fail "mutant $1 regions gcc"; return; }
    local r=CAUGHT b=CAUGHT
    "$dir/regions" --candidate > "$dir/regions.out" 2> "$dir/regions.err" && { r=SURVIVED; fail "mutant $1 SURVIVED the region test"; }
    batch "$OUT/classes" default native "$dir" > "$dir/batch.out" 2> "$dir/batch.err" && { b=SURVIVED; fail "mutant $1 SURVIVED the batch contract"; }
    echo "$1 $3: regions $r ($(head -1 "$dir/regions.err" | cut -c1-90)); batch contract $b ($(grep -h -m1 -E 'AssertionError|Exception:' "$dir/batch.err" "$dir/batch.out" | head -1 | cut -c1-90))"
}
l3mutant M6 's/            if (pending != 0) {/            if (0 \&\& pending != 0) {/' 'L3 final partial flush dropped'
echo "NATIVE_GATE $status"
[ "$status" = PASS ]
