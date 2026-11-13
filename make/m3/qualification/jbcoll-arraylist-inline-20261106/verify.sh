#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
# Runs every gate of leaf jbcoll-arraylist-inline-20261106 (census L002) and prints one GATE line per gate (GATES.tsv):
#   recipe        apply.py --check --donor-git: pinned owner preimage, crate pre/post/test blobs, changed=0 plan=UNCHANGED
#   patch_compile javac -g -Xlint:all -Werror -Xlint:-this-escape -XDstringConcat=inline --patch-module java.base=<dir>
#                 of ArrayList.java + jdk/internal/mindex/M3CollectionMaskCompactor.java, leaf and base: 0 warnings
#   differential  ArrayListDifferential (seed, 1,000,000 ops): stock JDK 21 vs base (master ArrayList) vs leaf, outputs
#                 byte-identical; leaf under -Xint (200,000 ops) identical to the stock prefix; a mutant of the inline
#                 path (ordinal <= Long.SIZE) must differ and must fail M3ArrayListInlineMaskTest
#   tests         test/jdk/java/util/ArrayList (+ the new M3ArrayListInlineMaskTest) as plain mains (tests.sh) on the
#                 leaf and on the base: pass=14 fail=0 skip=2 (testng ArrayManagement, manual Bug8146568) on both
#   (INFO)        InlineMaskAllocationProbe: removeIf allocated bytes per span on stock, base and leaf (not a gate)
#   naming        jdk_naming.py plan over the three touched paths: 0 moves, 0 rewrites, 0 refusals (stage first)
#   scope         git diff --name-only <base> + staged + untracked: only the three touched paths
# usage: JAVA_HOME=<JDK 21> verify.sh <donor-git-dir> <donor-rev> <scratch-dir> [ops] [base-rev]
set -uo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) winpath() { (cd "$1" && pwd -W); } ;; *) winpath() { (cd "$1" && pwd); } ;; esac
Q=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$Q/../../.." && pwd)
DONOR=$1; REV=$2; mkdir -p "$3"; S=$(winpath "$3"); OPS=${4:-1000000}; BASE=${5:-origin/master}
J="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
SEED=0x4d334a42414c4931
LEAF=make/m3/qualification/$(basename "$Q")/
OWNER=src/java.base/share/classes/java/util/ArrayList.java
COMPACTOR=src/java.base/share/classes/jdk/internal/mindex/M3CollectionMaskCompactor.java
NEWTEST=test/jdk/java/util/ArrayList/M3ArrayListInlineMaskTest.java
SCOPE=("$OWNER" "$NEWTEST" "$LEAF")
FLAGS=(-g -Xlint:all -Werror -Xlint:-this-escape -XDstringConcat=inline)
status=PASS
gate() { local name=$1 verdict=$2; shift 2; echo "GATE $name $verdict $*"; [ "$verdict" = PASS ] || status=FAIL; }

out=$(python -I "$Q/apply.py" --check --donor-git "$DONOR" --donor-rev "$REV" 2>&1 | tail -1)
case "$out" in *"changed=0 donor=VERIFIED plan=UNCHANGED"*) gate recipe PASS "$out" ;; *) gate recipe FAIL "$out" ;; esac

rm -rf "$S/side"
for side in base leaf mutant; do mkdir -p "$S/side/$side/src/java/util" "$S/side/$side/src/jdk/internal/mindex"
    cp "$ROOT/$COMPACTOR" "$S/side/$side/src/jdk/internal/mindex/"; done
git -C "$ROOT" show "$BASE:$OWNER" > "$S/side/base/src/java/util/ArrayList.java"
cp "$ROOT/$OWNER" "$S/side/leaf/src/java/util/"
sed 's/if (ordinal < Long.SIZE) deletionLow |= 1L << ordinal;/if (ordinal <= Long.SIZE) deletionLow |= 1L << ordinal;/' \
    "$ROOT/$OWNER" > "$S/side/mutant/src/java/util/ArrayList.java"
summary=""; v=PASS
for side in base leaf mutant; do
    d="$S/side/$side"
    "$J/javac" "${FLAGS[@]}" --patch-module "java.base=$d/src" -d "$d/cls" "$d/src/java/util/ArrayList.java" \
        "$d/src/jdk/internal/mindex/M3CollectionMaskCompactor.java" > "$d/javac.log" 2>&1
    rc=$?; w=$(grep -c 'warning:' "$d/javac.log")
    [ "$side" = mutant ] || { [ $rc = 0 ] && [ "$w" = 0 ] || v=FAIL; summary="$summary $side:rc=$rc,warnings=$w,classes=$(find "$d/cls" -name '*.class' | wc -l)"; }
done
mutated=$(diff "$S/side/leaf/src/java/util/ArrayList.java" "$S/side/mutant/src/java/util/ArrayList.java" | grep -c '^>')
gate patch_compile $v "$summary (mutant lines=$mutated)"

rm -rf "$S/drv" "$S/runs"; mkdir -p "$S/runs"
"$J/javac" -Xlint:all -Werror -d "$S/drv" "$Q/ArrayListDifferential.java" > "$S/drv.log" 2>&1; drc=$?
timeout 120 "$J/java" -Xmx1g -cp "$S/drv" ArrayListDifferential $SEED "$OPS" "$S/runs/stock.txt" > "$S/runs/stock.out" 2>&1
for side in base leaf mutant; do
    timeout 120 "$J/java" -Xmx1g --patch-module "java.base=$S/side/$side/cls" -cp "$S/drv" ArrayListDifferential $SEED "$OPS" \
        "$S/runs/$side.txt" > "$S/runs/$side.out" 2>&1
done
timeout 120 "$J/java" -Xint -Xmx1g --patch-module "java.base=$S/side/leaf/cls" -cp "$S/drv" ArrayListDifferential $SEED 200000 \
    "$S/runs/leaf-xint.txt" > "$S/runs/leaf-xint.out" 2>&1
sb=$(cmp -s "$S/runs/stock.txt" "$S/runs/base.txt" && echo identical || echo DIFFERENT)
sl=$(cmp -s "$S/runs/stock.txt" "$S/runs/leaf.txt" && echo identical || echo DIFFERENT)
xi=$(head -200000 "$S/runs/stock.txt" | cmp -s - "$S/runs/leaf-xint.txt" && echo identical || echo DIFFERENT)
mu=$(diff "$S/runs/stock.txt" "$S/runs/mutant.txt" | grep -c '^<')
d=$(for f in stock base leaf mutant leaf-xint; do echo "$f:$(grep -o 'sha256=[0-9a-f]*' "$S/runs/$f.out" | cut -c8-23)"; done | tr '\n' ' ')
cov=$(grep -o 'removeIf_spans[^ ]* [^ ]*' "$S/runs/stock.out")
v=FAIL; [ $drc = 0 ] && [ "$sb$sl$xi" = identicalidenticalidentical ] && [ "${mu:-0}" -gt 0 ] && v=PASS
gate differential $v "stock_vs_base=$sb stock_vs_leaf=$sl xint200k=$xi mutant_differing_lines=$mu; $cov; $(grep -o 'callbacks=[0-9]* cme=[0-9]*' "$S/runs/stock.out"); sha256 $d"

T=$(ls "$ROOT"/test/jdk/java/util/ArrayList/*.java)
a=$(bash "$Q/tests.sh" "$S/side/leaf/cls" "$S/tests-leaf" $T | tail -1)
b=$(bash "$Q/tests.sh" "$S/side/base/cls" "$S/tests-base" $T | tail -1)
"$J/java" --patch-module "java.base=$S/side/mutant/cls" -cp "$S/tests-leaf/classes" M3ArrayListInlineMaskTest > "$S/runs/mutant-test.log" 2>&1
mrc=$?
v=FAIL; [ "$a" = "TESTS pass=14 fail=0 skip=2" ] && [ "$b" = "$a" ] && [ $mrc != 0 ] && v=PASS
gate tests $v "leaf: $a; base: $b; mutant M3ArrayListInlineMaskTest rc=$mrc ($(head -1 "$S/runs/mutant-test.log" | cut -c1-90))"

rm -rf "$S/probe"; "$J/javac" -Xlint:all -Werror -d "$S/probe" "$Q/InlineMaskAllocationProbe.java" > "$S/probe.log" 2>&1
info=$(for side in stock base leaf; do P=(); [ $side = stock ] || P=(--patch-module "java.base=$S/side/$side/cls")
    echo "$side: $(timeout 120 "$J/java" "${P[@]}" -cp "$S/probe" InlineMaskAllocationProbe | cut -d' ' -f2-)"; done | tr '\n' ';')
echo "INFO allocation $info"

naming=$(cd "$ROOT" && python -I make/m3/tooling/jdk-naming/jdk_naming.py plan --root . $(printf -- '--only %s ' "${SCOPE[@]}") 2>&1 | grep -E "^(JDK_NAMING|REFUSED)" | tail -1)
case "$naming" in *"moves=0 rewrites=0 refusals=0"*) gate naming PASS "$naming" ;; *) gate naming FAIL "$naming" ;; esac

changed=$(cd "$ROOT" && { git diff --name-only "$BASE"; git diff --name-only --cached; git ls-files --others --exclude-standard; } | sort -u)
outside=$(printf '%s\n' "$changed" | grep -v -e "^${SCOPE[0]}$" -e "^${SCOPE[1]}$" -e "^${SCOPE[2]}" | grep -c .)
v=FAIL; [ "${outside:-0}" = 0 ] && v=PASS
gate scope $v "paths=$(printf '%s\n' "$changed" | grep -c .) outside=${outside:-0} base=$BASE"
echo "VERIFY $status"
[ "$status" = PASS ]
