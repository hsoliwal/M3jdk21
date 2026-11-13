#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Oracle 3 (fixture gate, Linux: gcc/g++, nm, JDK 21 with include/jni.h) and the fixture half of oracle 4, donor and
# relocated side by side:
#   abi      javac --release 21 -Xlint:all -Werror -h of both fixtures: the two headers are identical once the mangled
#            package prefix (the JNI rows of RELOCATION_MAP.tsv) is mapped; both declare exactly the two natives
#   exports  both fixture libraries (C17 + C++20 objects, -Wall -Wextra -Werror, one shared object): nm -D
#            --defined-only Java_* symbol sets identical once the prefix is mapped
#   verify   verify.py of each side (its own SOURCE_SHA256.tsv custody, javac -h, compile/link, nm, javap, then exact
#            exit/stdout/stderr parity over interpreted / C1 / C2 / default tiered for its before and after JVM):
#            PASS on both; the receipts' mode results (stdout/stderr sha256, exit) identical between the sides
#   negative verify.py --negative-test on each side: the injected probe drift is refused (PASS: injected behavior
#            drift refused) on both
#   unittest test_verify.py of each side passes
# The donor crate comes from the donor git tree (its three FILE rows and its SOURCE_SHA256.tsv SEAL row).
# usage: fixture-gate.sh <donor-crate-dir> <scratch-dir>   (donor-crate-dir: a copy of the donor fixture crate with
#        verify.py, test_verify.py, SOURCE_SHA256.tsv and fixtures/; make it with differential.sh or git archive)
set -uo pipefail
export PYTHONDONTWRITEBYTECODE=1
RECIPE_DIR=$(cd "$(dirname "$0")" && pwd)
MODULE=$(cd "$RECIPE_DIR/../.." && pwd)
DONOR=$(cd "$1" && pwd)
mkdir -p "$2"
SCRATCH=$(cd "$2" && pwd)
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
RELOCATED="$MODULE/fixture-gate"
status=PASS
fail() { echo "FAIL $*"; status=FAIL; }
jni_from=$(awk -F'\t' '$1=="JNI" && $2 ~ /^Java_/ {print $2; exit}' "$MAP")
jni_to=$(awk -F'\t' '$1=="JNI" && $2 ~ /^Java_/ {print $3; exit}' "$MAP")
JAVAC=$(command -v javac); INCLUDE=$(cd "$(dirname "$(readlink -f "$JAVAC")")/../include" && pwd)
echo "TOOLS $(gcc --version | head -1) | $(java -version 2>&1 | head -1) | include=$INCLUDE"
rm -rf "$SCRATCH/abi" && mkdir -p "$SCRATCH/abi"
for side in donor relocated; do
    if [ "$side" = donor ]; then dir=$DONOR; else dir=$RELOCATED; fi
    out="$SCRATCH/abi/$side"; mkdir -p "$out/classes" "$out/headers"
    javac --release 21 -Xlint:all -Werror -d "$out/classes" -h "$out/headers" "$dir/fixtures/AtomJitProbe.java" || fail "javac -h $side"
    gcc -std=c17 -Wall -Wextra -Werror -O2 -fPIC -I"$INCLUDE" -I"$INCLUDE/linux" -c "$dir/fixtures/m3jni.c" -o "$out/c.o" || fail "cc $side"
    g++ -std=c++20 -Wall -Wextra -Werror -O2 -fPIC -I"$INCLUDE" -I"$INCLUDE/linux" -c "$dir/fixtures/m3jni.cpp" -o "$out/cpp.o" || fail "c++ $side"
    g++ -shared -o "$out/libm3_atom_jni.so" "$out/c.o" "$out/cpp.o" || fail "link $side"
    nm -D --defined-only "$out/libm3_atom_jni.so" | awk '{print $NF}' | grep '^Java_' | sort > "$out/exports.txt"
    cat "$out"/headers/*.h > "$out/header.h"
    echo "abi $side header=$(ls "$out/headers") prototypes=$(grep -c 'JNIEXPORT' "$out/header.h") exports=$(tr '\n' ' ' < "$out/exports.txt")"
done
sed "s/${jni_from}/${jni_to}/g; s/${jni_from#Java_}/${jni_to#Java_}/g" "$SCRATCH/abi/donor/header.h" > "$SCRATCH/abi/donor-mapped.h"
if diff -q "$SCRATCH/abi/donor-mapped.h" "$SCRATCH/abi/relocated/header.h" > /dev/null; then
    echo "abi javac-h IDENTICAL (prefix-mapped) sha256=$(sha256sum "$SCRATCH/abi/relocated/header.h" | cut -c1-16)"
else fail "abi javac-h headers differ"; diff "$SCRATCH/abi/donor-mapped.h" "$SCRATCH/abi/relocated/header.h" | head -10; fi
sed "s/^${jni_from}/${jni_to}/" "$SCRATCH/abi/donor/exports.txt" > "$SCRATCH/abi/donor-exports-mapped.txt"
if cmp -s "$SCRATCH/abi/donor-exports-mapped.txt" "$SCRATCH/abi/relocated/exports.txt"; then
    echo "exports nm-D IDENTICAL (prefix-mapped) symbols=$(wc -l < "$SCRATCH/abi/relocated/exports.txt")"
else fail "exports differ"; fi
for side in donor relocated; do
    if [ "$side" = donor ]; then dir=$DONOR; else dir=$RELOCATED; fi
    rm -rf "$SCRATCH/verify-$side" "$SCRATCH/negative-$side"
    timeout 900 python3 -I "$dir/verify.py" --evidence "$SCRATCH/verify-$side" --timeout-seconds 120 > "$SCRATCH/verify-$side.log" 2>&1
    code=$?; echo "verify $side exit=$code $(tail -1 "$SCRATCH/verify-$side.log")"
    [ "$code" = 0 ] || fail "verify $side"
    timeout 900 python3 -I "$dir/verify.py" --evidence "$SCRATCH/negative-$side" --timeout-seconds 120 --negative-test > "$SCRATCH/negative-$side.log" 2>&1
    code=$?; echo "negative $side exit=$code $(tail -1 "$SCRATCH/negative-$side.log")"
    [ "$code" = 0 ] && grep -q 'PASS: injected behavior drift refused' "$SCRATCH/negative-$side.log" || fail "negative $side"
    (cd "$dir" && timeout 300 python3 -m unittest -q test_verify > "$SCRATCH/unittest-$side.log" 2>&1); code=$?
    echo "unittest $side exit=$code $(grep -E '^(Ran|OK|FAILED)' "$SCRATCH/unittest-$side.log" | tr '\n' ' ')"
    [ "$code" = 0 ] || fail "unittest $side"
done
python3 - "$SCRATCH" <<'EOF' || status=FAIL
import json, sys
from pathlib import Path
scratch = Path(sys.argv[1])
receipts = {side: json.loads((scratch / ("verify-" + side) / "receipt.json").read_text()) for side in ("donor", "relocated")}
modes = {side: [(m["side"], m["mode"], m["exit_code"], m["stdout_sha256"], m["stderr_sha256"]) for m in r["mode_results"]]
         for side, r in receipts.items()}
same = modes["donor"] == modes["relocated"]
print("receipts mode_results %s runs=%d stdout_sha256=%s stderr_sha256=%s" % (
    "IDENTICAL" if same else "DIFFERENT", len(modes["relocated"]),
    sorted({m[3][:16] for m in modes["relocated"]}), sorted({m[4][:16] for m in modes["relocated"]})))
print("receipts relocated status=%s scope=%s native_gate=%s receipt_sha256=%s" % (
    receipts["relocated"]["status"], receipts["relocated"]["scope"], receipts["relocated"]["native_gate"],
    receipts["relocated"]["receipt_sha256"][:16]))
sys.exit(0 if same else 1)
EOF
echo "FIXTURE_GATE $status"
[ "$status" = PASS ]
