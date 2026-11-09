#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gates 2, 3, 3b and 4 of the min-cost-flow relocation (GATES.tsv), after mvn package of the module:
#   2  the donor oracle figure (M3_MINCOST_FLOW_PASS checks=6671) from the donor oracle compiled out of the donor
#      tree and from the relocated oracle, each in JIT and -Xint;
#   3  the chosen donor solver (FILE row under src/main/java, compiled from the donor tree with javac --release 21
#      -Xlint:all -Werror) and the relocated solver (target/classes) replay the same seeded networks through the
#      reflective driver M3MinCostFlowDump (test scope); the two dumps (flow, cost, every edge flow, refusals) must
#      be byte-identical: the dump names no type, so no normalization is needed;
#   3b the same at a smaller random count under -Xint on both sides, equal to the relocated JIT dump of that count;
#   4  cross-solver agreement: the relocated solver against the unchosen donor solver (the SKIP row whose target
#      column is "differential oracle", compiled from the donor tree into the scratch directory, never committed)
#      on every network the chosen solver accepts, flow value and cost (the unchosen solver has no per-edge flow).
# Every JVM run is bounded by LIMIT seconds; counts and sha256 digests are printed.
# usage: differential.sh <donor-git-dir> <donor-rev> <scratch-dir> [random-networks] [xint-random-networks]
#        (JAVA_HOME = JDK 21)
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../..")
DONOR_GIT=$1
REV=$2
RANDOM_NETWORKS=${4:-100000}
XINT_NETWORKS=${5:-2000}
mkdir -p "$3"
SCRATCH=$(winpath "$3")
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
DRIVER=jdk.m3.algorithm.M3MinCostFlowDump
FIGURE="M3_MINCOST_FLOW_PASS checks=6671"
LIMIT=120
# implicit exceptions keep their messages (the families lesson), so refusal lines are deterministic
JVM_FLAGS="-XX:-OmitStackTraceInFastThrow"
donor_commit=$(git -C "$DONOR_GIT" rev-parse "$REV^{commit}")
fqn() { printf '%s.%s' "$(dirname "$1" | sed -E 's#^(.*/)?src/(main|test)/java/##; s#/#.#g')" "$(basename "$1" .java)"; }
run() { timeout "$LIMIT" "$JAVA_BIN/java" $JVM_FLAGS "$@"; }
digest() { sha256sum "$1" | cut -d' ' -f1; }

# 1. the donor sources from the donor tree: the chosen solver with its oracle, and the unchosen solver
rm -rf "$SCRATCH/chosen" "$SCRATCH/oracle"
mkdir -p "$SCRATCH/chosen/src" "$SCRATCH/chosen/classes" "$SCRATCH/oracle/src" "$SCRATCH/oracle/classes"
awk -F'\t' '$1=="FILE" {print $2}' "$MAP" | while read -r path; do
    git -C "$DONOR_GIT" show "$REV:$path" > "$SCRATCH/chosen/src/$(basename "$path")"
done
awk -F'\t' '$1=="SKIP" && $3=="differential oracle" {print $2}' "$MAP" | while read -r path; do
    git -C "$DONOR_GIT" show "$REV:$path" > "$SCRATCH/oracle/src/$(basename "$path")"
done
"$JAVA_BIN/javac" --release 21 -proc:none -Xlint:all -Werror -d "$SCRATCH/chosen/classes" "$SCRATCH"/chosen/src/*.java
"$JAVA_BIN/javac" --release 21 -proc:none -Xlint:all -Werror -d "$SCRATCH/oracle/classes" "$SCRATCH"/oracle/src/*.java
donor_solver=$(fqn "$(awk -F'\t' '$1=="FILE" && $3 ~ /^src\/main\/java\// {print $2; exit}' "$MAP")")
donor_oracle=$(fqn "$(awk -F'\t' '$1=="FILE" && $3 ~ /^src\/test\/java\// {print $2; exit}' "$MAP")")
target_solver=$(fqn "$(awk -F'\t' '$1=="FILE" && $3 ~ /^src\/main\/java\// {print $3; exit}' "$MAP")")
target_oracle=$(fqn "$(awk -F'\t' '$1=="FILE" && $3 ~ /^src\/test\/java\// {print $3; exit}' "$MAP")")
unchosen=$(fqn "$(awk -F'\t' '$1=="SKIP" && $3=="differential oracle" {print $2; exit}' "$MAP")")
DONOR_CP="$SCRATCH/chosen/classes$SEP$MODULE/target/test-classes"
TARGET_CP="$MODULE/target/classes$SEP$MODULE/target/test-classes"
echo "DONOR $donor_commit chosen=$donor_solver oracle=$donor_oracle unchosen=$unchosen classes=$(find "$SCRATCH/chosen/classes" "$SCRATCH/oracle/classes" -name '*.class' | wc -l | tr -d ' ')"

# 2. the oracle figure: donor and relocated, JIT and -Xint
figures=()
for side in donor relocated; do
    for mode in jit xint; do
        cp=$DONOR_CP; main=$donor_oracle
        [ "$side" = relocated ] && { cp=$TARGET_CP; main=$target_oracle; }
        flag=; [ "$mode" = xint ] && flag=-Xint
        printed=$(run $flag -cp "$cp" "$main" | tr -d '\r')
        [ "$printed" = "$FIGURE" ] || { echo "ORACLE FAIL $side $mode: $printed"; exit 1; }
        figures+=("${side}_$mode=${printed#M3_MINCOST_FLOW_PASS }")
    done
done
echo "ORACLE PASS ${figures[*]}"

# 3. the dumps: donor vs relocated, JIT
run -cp "$DONOR_CP" "$DRIVER" --solver "$donor_solver" --random "$RANDOM_NETWORKS" > "$SCRATCH/donor.txt"
run -cp "$TARGET_CP" "$DRIVER" --solver "$target_solver" --random "$RANDOM_NETWORKS" > "$SCRATCH/relocated.txt"
tail -n 1 "$SCRATCH/relocated.txt"
if cmp -s "$SCRATCH/donor.txt" "$SCRATCH/relocated.txt"; then
    echo "DIFFERENTIAL PASS donor_sha256=$(digest "$SCRATCH/donor.txt") relocated_sha256=$(digest "$SCRATCH/relocated.txt") lines=$(wc -l < "$SCRATCH/relocated.txt" | tr -d ' ') bytes=$(wc -c < "$SCRATCH/relocated.txt" | tr -d ' ') random=$RANDOM_NETWORKS"
else
    echo "DIFFERENTIAL FAIL donor_sha256=$(digest "$SCRATCH/donor.txt") relocated_sha256=$(digest "$SCRATCH/relocated.txt")"
    diff "$SCRATCH/donor.txt" "$SCRATCH/relocated.txt" | head -n 20
    exit 1
fi

# 3b. -Xint on both sides at a smaller random count, against the relocated JIT dump of that count
run -cp "$TARGET_CP" "$DRIVER" --solver "$target_solver" --random "$XINT_NETWORKS" > "$SCRATCH/relocated.small.txt"
run -Xint -cp "$DONOR_CP" "$DRIVER" --solver "$donor_solver" --random "$XINT_NETWORKS" > "$SCRATCH/donor.xint.txt"
run -Xint -cp "$TARGET_CP" "$DRIVER" --solver "$target_solver" --random "$XINT_NETWORKS" > "$SCRATCH/relocated.xint.txt"
if cmp -s "$SCRATCH/donor.xint.txt" "$SCRATCH/relocated.xint.txt" && cmp -s "$SCRATCH/relocated.small.txt" "$SCRATCH/relocated.xint.txt"; then
    echo "XINT PASS sha256=$(digest "$SCRATCH/relocated.xint.txt") lines=$(wc -l < "$SCRATCH/relocated.xint.txt" | tr -d ' ') random=$XINT_NETWORKS (donor -Xint = relocated -Xint = relocated JIT)"
else
    echo "XINT FAIL donor_xint=$(digest "$SCRATCH/donor.xint.txt") relocated_xint=$(digest "$SCRATCH/relocated.xint.txt") relocated_jit=$(digest "$SCRATCH/relocated.small.txt")"
    exit 1
fi

# 4. cross-solver agreement: flow value and cost against the unchosen donor solver
run -cp "$TARGET_CP$SEP$SCRATCH/oracle/classes" "$DRIVER" --solver "$target_solver" --oracle "$unchosen" \
    --random "$RANDOM_NETWORKS" > "$SCRATCH/cross.txt"
cat "$SCRATCH/cross.txt"
if grep -q ' differ=0 ' "$SCRATCH/cross.txt"; then
    echo "CROSS PASS random=$RANDOM_NETWORKS"
else
    echo "CROSS FAIL"
    exit 1
fi
rm -f "$SCRATCH/donor.txt" "$SCRATCH/relocated.small.txt" "$SCRATCH/donor.xint.txt"
