#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
# Runs every gate of leaf jbcoll-mask-compactor-perf-20261106 (recipe JBCOLL_MASK_COMPACTOR_PERF v1) and prints one
# GATE line per gate (GATES.tsv). Derived from make/m3/qualification/jbcoll-mask-compactor-20261106/verify.sh (L003), whose
# CompactorDifferential.java and tests.sh it runs in place.
#   recipe         apply.py --check: changed=0 plan=UNCHANGED (pr1908 provenance re-derived); a drifted owner (one
#                  appended byte, restored afterwards) is REFUSED with exit 2; variants.py checks pr1754 provenance
#   patch_compile  javac -g -Xlint:all -Werror -Xlint:-this-escape -XDstringConcat=inline --patch-module java.base=<dir>
#                  ArrayList + Vector (the callers) + the compactor, base and leaf: 0 warnings
#   differential   L003 CompactorDifferential diff (seed 0x4d334a424d534b31, 1,000,000 cases): the leaf class from the
#                  patched java.base vs renamed copies of fixed (control), master, postimage, statonly, threshold,
#                  threshold_prefix, threshold_words1024, fixed_prefix: 0 mismatches and the L003 outcome sha256;
#                  the L003 scalar mutant (on the leaf): mismatches > 0
#   tests_owner    L003 tests.sh, test/jdk/java/util/ArrayList/M3*.java as plain mains on the leaf patch: all pass
#   tests_arraylist L003 tests.sh, test/jdk/java/util/ArrayList/*.java (the L002 set) on the leaf patch: 0 fail
#   timing         timing.sh (3 runs, P and E CPU sets) + timing_table.py into the scratch dir: RULE FIX PASS, pr1908
#                  (bound dropped) ADOPT, prefix skip REJECTED on threshold and on fixed (host-specific; INFO elsewhere)
#   naming         jdk_naming.py apply, then plan, over the owner and the leaf dir (stage first): fixed point
#   scope          git diff --name-only <base> + staged + untracked: only the owner and the leaf dir
# base-rev defaults to merge-base HEAD origin/master (master moves while a leaf is open).
# usage: JAVA_HOME=<JDK 21> verify.sh <scratch-dir> [cases] [base-rev]
set -uo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
Q=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$Q/../../.." && pwd)
L3="$ROOT/make/m3/qualification/jbcoll-mask-compactor-20261106"
mkdir -p "$1"; S=$(winpath "$1"); CASES=${2:-1000000}
BASE=${3:-$(git -C "$ROOT" merge-base HEAD origin/master)}
J="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
SEED=0x4d334a424d534b31
L3_SHA=c5f342c12d3d6ae1ddb65f87f8a1514a9ad389f87155a8e2db4adc22b8ff4881
LEAF=make/m3/qualification/$(basename "$Q")/
U=src/java.base/share/classes/java/util
OWNER=src/java.base/share/classes/jdk/internal/mindex/M3CollectionMaskCompactor.java
FLAGS=(-g -Xlint:all -Werror -Xlint:-this-escape -XDstringConcat=inline)
EXPORT=(--add-exports java.base/jdk.internal.mindex=ALL-UNNAMED)
LABELS=(fixed master postimage statonly threshold threshold_prefix threshold_words1024 fixed_prefix mutant)
status=PASS
gate() { local name=$1 verdict=$2; shift 2; echo "GATE $name $verdict $*"; [ "$verdict" = PASS ] || status=FAIL; }

c=$(python -I "$Q/apply.py" --check 2>&1); crc=$?
cp "$ROOT/$OWNER" "$S/owner.bak"; printf ' ' >> "$ROOT/$OWNER"
d=$(python -I "$Q/apply.py" --check 2>&1); drc=$?; cp "$S/owner.bak" "$ROOT/$OWNER"
v=FAIL; case "$c|$drc|$d" in *"changed=0"*"plan=UNCHANGED|2|REFUSED drifted preimage"*) [ $crc = 0 ] && v=PASS ;; esac
gate recipe $v "$(printf '%s' "$c" | tail -1); drift: rc=$drc $(printf '%s' "$d" | cut -c1-90)"

rm -rf "$S/side"; for side in base leaf; do mkdir -p "$S/side/$side/src/java/util" "$S/side/$side/src/jdk/internal/mindex"; done
for f in ArrayList.java Vector.java; do git -C "$ROOT" show "$BASE:$U/$f" > "$S/side/base/src/java/util/$f"; cp "$ROOT/$U/$f" "$S/side/leaf/src/java/util/"; done
git -C "$ROOT" show "$BASE:$OWNER" > "$S/side/base/src/jdk/internal/mindex/M3CollectionMaskCompactor.java"
cp "$ROOT/$OWNER" "$S/side/leaf/src/jdk/internal/mindex/"
summary=""; v=PASS
for side in base leaf; do
    d="$S/side/$side"
    "$J/javac" "${FLAGS[@]}" --patch-module "java.base=$d/src" -d "$d/cls" "$d/src/java/util/ArrayList.java" \
        "$d/src/java/util/Vector.java" "$d/src/jdk/internal/mindex/M3CollectionMaskCompactor.java" > "$d/javac.log" 2>&1
    rc=$?; w=$(grep -c 'warning:' "$d/javac.log"); [ $rc = 0 ] && [ "${w:-0}" = 0 ] || v=FAIL
    summary="$summary $side:rc=$rc,warnings=${w:-0},classes=$(find "$d/cls" -name '*.class' | wc -l)"
done
gate patch_compile $v "$summary (ArrayList + Vector + M3CollectionMaskCompactor)"

rm -rf "$S/var" "$S/drv"
python -I "$Q/variants.py" --root "$ROOT" --out "$S/var/src" "${LABELS[@]}" > "$S/variants.log" 2>&1; prc=$?
"$J/javac" -Xlint:all -Werror -d "$S/var/cls" "$S"/var/src/jbcoll/*.java > "$S/var.javac.log" 2>&1; vrc=$?
"$J/javac" -Xlint:all -Werror --patch-module "java.base=$S/side/leaf/cls" "${EXPORT[@]}" -d "$S/drv" \
    "$L3/CompactorDifferential.java" > "$S/drv.javac.log" 2>&1; drc=$?
RUN=(--patch-module "java.base=$S/side/leaf/cls" "${EXPORT[@]}" -cp "$S/drv$SEP$S/var/cls")
d=$(timeout 300 "$J/java" -Xmx2g "${RUN[@]}" CompactorDifferential diff $SEED "$CASES" "${LABELS[@]}" 2>&1)
v=PASS; [ $prc = 0 ] && [ $vrc = 0 ] && [ $drc = 0 ] || v=FAIL
for l in live "${LABELS[@]:0:8}"; do case "$d" in *"| $l mismatches=0 sha256=$L3_SHA"*) ;; *) v=FAIL ;; esac; done
case "$d" in *"| mutant mismatches="[1-9]*) ;; *) v=FAIL ;; esac
gate differential $v "variants rc=$prc javac rc=$vrc driver rc=$drc; $d"

for set in owner arraylist; do
    if [ $set = owner ]; then T=$(ls "$ROOT"/test/jdk/java/util/ArrayList/M3*.java); else T=$(ls "$ROOT"/test/jdk/java/util/ArrayList/*.java); fi
    for side in leaf base; do bash "$L3/tests.sh" "$S/side/$side/cls" "$S/tests-$set-$side" $T > "$S/tests-$set-$side.txt" 2>&1; done
    a=$(tail -1 "$S/tests-$set-leaf.txt"); b=$(tail -1 "$S/tests-$set-base.txt")
    v=FAIL; case "$a" in "TESTS pass="*" fail=0 "*) v=PASS ;; esac
    gate tests_$set $v "leaf: $a ($(grep '^TEST ' "$S/tests-$set-leaf.txt" | awk '{print $2 "=" $3}' | tr '\n' ' ')); base: $b"
done

bash "$Q/timing.sh" "$S/timing" 3 > "$S/timing.progress" 2>&1
r=$(python -I "$Q/timing_table.py" "$S/timing" --out "$S/timing/TIMING.tsv" 2>&1)
v=FAIL
case "$r" in *"RULE FIX PASS"*"RULE IDEA REJECTED threshold_prefix"*"RULE IDEA ADOPT fixed vs threshold"*"RULE IDEA REJECTED fixed_prefix"*) v=PASS ;; esac
gate timing $v "$(printf '%s\n' "$r" | grep -E '^RULE (FIX|IDEA)' | cut -c1-200 | tr '\n' ';')"

naming=$(cd "$ROOT" && git add -- "$OWNER" "$LEAF" && python -I make/m3/tooling/jdk-naming/jdk_naming.py apply --root . --only "$OWNER" --only "$LEAF" 2>&1 | grep -E "^(JDK_NAMING|REFUSED)" | tail -1; \
    python -I make/m3/tooling/jdk-naming/jdk_naming.py plan --root . --only "$OWNER" --only "$LEAF" 2>&1 | grep -E "^(JDK_NAMING|REFUSED)" | tail -1)
case "$naming" in *"moves=0 rewrites=0 refusals=0"*"moves=0 rewrites=0 refusals=0"*) gate naming PASS "$(printf '%s' "$naming" | tr '\n' ';')" ;;
    *) gate naming FAIL "$(printf '%s' "$naming" | tr '\n' ';')" ;; esac

changed=$(cd "$ROOT" && { git diff --name-only "$BASE"; git diff --name-only --cached; git ls-files --others --exclude-standard; } | sort -u)
outside=$(printf '%s\n' "$changed" | grep -v -e "^$LEAF" -e "^$OWNER\$" | grep -c .)
v=FAIL; [ "${outside:-0}" = 0 ] && v=PASS
gate scope $v "paths=$(printf '%s\n' "$changed" | grep -c .) outside=${outside:-0} base=$(git -C "$ROOT" rev-parse "$BASE")"
echo "VERIFY $status"
[ "$status" = PASS ]
