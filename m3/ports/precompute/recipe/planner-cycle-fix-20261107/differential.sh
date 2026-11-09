#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Differential gate of recipe planner-cycle-fix-20261107: the module before the recipe (a built copy of the base commit's
# m3/ports/precompute, its own dump driver) against this module (the patched planner, the extended dump in strict mode).
# (a) The acyclic corpus is unchanged: the PLAN, TUPLE and HASH lines of both dumps are byte-identical on the JIT
#     (CASES) and under -Xint (XINT_CASES); the base dump runs in its default mode, the head dump in strict mode.
# (b) The head dump reports no stack overflow and plans cyclic catalogues on both paths (CYCLIC lines against the
#     recomputed greedy pass, cyclic brute-force plans inside the PLAN lines), printed from its TOTAL line.
# The tuple hash domain comes from the LITERAL row of recipe planner-family-20261107.
# Derived from m3/ports/precompute/recipe/planner-family-20261107/differential.sh (dump runner and comparison).
# usage: differential.sh <built-base-module-dir> <scratch-dir> [CASES=4000] [XINT_CASES=600]   (JAVA_HOME = JDK 21)
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../..")
BASE=$(winpath "$1")
mkdir -p "$2"
SCRATCH=$(winpath "$2")
CASES=${3:-4000}
XINT_CASES=${4:-600}
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
FAMILY_MAP="$RECIPE_DIR/../planner-family-20261107/RELOCATION_MAP.tsv"
package=$(awk -F'\t' '$1=="PACKAGE" {print $3; exit}' "$FAMILY_MAP")
domain=$(awk -F'\t' '$1=="LITERAL" && $2 ~ /:$/ {print $3; exit}' "$FAMILY_MAP")
DUMP="$package.M3IndexPrecomputePlannerDump"
status=PASS
dump() { # dump <name> <module dir> <cases> <mode> [jvm flag]
    "$JAVA_BIN/java" -Djava.io.tmpdir="$SCRATCH" ${5:-} -Xss1m -cp "$2/target/classes$SEP$2/target/test-classes" "$DUMP" \
        "$package" M3 "$domain" "$3" $4 | tr -d '\r' > "$SCRATCH/$1.full.txt"
    grep -E '^(PLAN|TUPLE|HASH) ' "$SCRATCH/$1.full.txt" > "$SCRATCH/$1.txt"
}
compare() {
    local left right verdict
    left=$(sha256sum "$SCRATCH/$1.txt" | cut -d' ' -f1)
    right=$(sha256sum "$SCRATCH/$2.txt" | cut -d' ' -f1)
    if cmp -s "$SCRATCH/$1.txt" "$SCRATCH/$2.txt"; then verdict=IDENTICAL; else verdict=DIFFERENT; status=FAIL; fi
    echo "$1 vs $2 (PLAN/TUPLE/HASH) $verdict sha256=$left/$right bytes=$(wc -c < "$SCRATCH/$1.txt") lines=$(wc -l < "$SCRATCH/$1.txt")"
}
dump base-dump "$BASE" "$CASES" ""
dump head-dump "$MODULE" "$CASES" strict
compare base-dump head-dump
dump base-dump-xint "$BASE" "$XINT_CASES" "" -Xint
dump head-dump-xint "$MODULE" "$XINT_CASES" strict -Xint
compare base-dump-xint head-dump-xint
echo "base:  $(grep '^CYCLE_PROBE' "$SCRATCH/base-dump.full.txt") | $(tail -n 1 "$SCRATCH/base-dump.full.txt")"
echo "head:  $(grep '^CYCLE_PROBE' "$SCRATCH/head-dump.full.txt") | $(tail -n 1 "$SCRATCH/head-dump.full.txt")"
echo "head -Xint: $(tail -n 1 "$SCRATCH/head-dump-xint.full.txt")"
echo "head CYCLIC lines: $(grep -c '^CYCLIC ' "$SCRATCH/head-dump.full.txt") (refused: $(grep '^CYCLIC ' "$SCRATCH/head-dump.full.txt" | grep -c 'Exception' || true))"
grep -q ' overflows=0 ' "$SCRATCH/head-dump.full.txt" || status=FAIL
echo "DIFFERENTIAL $status"
[ "$status" = PASS ]
