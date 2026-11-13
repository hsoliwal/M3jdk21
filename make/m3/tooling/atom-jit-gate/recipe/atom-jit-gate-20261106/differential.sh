#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Oracle 4: donor and relocated gate verdicts identical on the same inputs (Java side; the fixture half of oracle 4 is
# fixture-gate.sh, which this script prepares).
#   serial gate   differential/GateVerdictDiff.java: 20,000 seeded receipt ledgers (1..4 targets, Java and JNI lanes,
#                 up to 3 mutations: status, JDK root, behavior root, dropped / swapped rows, envelope drift, compiled
#                 flag, held suffix, unsealed proof) reviewed by the donor gate (plan from the donor planner) and the
#                 relocated gate (Plan from the donor plan's root and order): every verdict line identical
#   work orders   differential/work_orders.py: an admissible SOURCE_WORK_QUEUE.tsv built from this checkout's tree
#                 (bounded source families) and 14 forged variants, each in the donor schema and the relocated schema
#                 (NAMING row 1 of RELOCATION_MAP.tsv); the donor and relocated M3OpenJdkWorkOrderCli must write
#                 byte-identical work orders, or refuse with the identical message
#   fixture crate the donor fixture crate (verify.py, test_verify.py, SOURCE_SHA256.tsv, fixtures/) written from donor
#                 blobs (LF bytes, as its custody manifest pins them) to <scratch>/donor-fixture-crate for fixture-gate.sh
# usage: JAVA_HOME=<JDK 21> differential.sh <donor-git-dir> <donor-rev> <scratch-dir> [scenarios]   (after mvn package)
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../..")
ROOT=$(winpath "$(dirname "$0")/../../../../../..")
DONOR_GIT=$1
REV=$2
SCENARIOS=${4:-20000}
mkdir -p "$3"
SCRATCH=$(winpath "$3")
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
TIMEOUT=${M3_ATOMJIT_CHILD_TIMEOUT:-300}
status=PASS
fail() { echo "FAIL $*"; status=FAIL; }
row() { awk -F'\t' -v k="$1" -v s="$2" '$1==k && index($2, s) {print $2; exit}' "$MAP"; }
show() { git -C "$DONOR_GIT" show "$REV:$1"; }
rm -rf "$SCRATCH/donor-src" "$SCRATCH/donor-classes" "$SCRATCH/driver" "$SCRATCH/donor-fixture-crate" "$SCRATCH/orders"
mkdir -p "$SCRATCH/donor-src" "$SCRATCH/donor-classes" "$SCRATCH/driver" "$SCRATCH/donor-fixture-crate/fixtures" "$SCRATCH/orders"
echo "DONOR $(git -C "$DONOR_GIT" rev-parse "$REV^{commit}")"

# fixture crate from blobs (the donor tree's text conversion would change the C bytes its manifest pins)
for target in verify.py test_verify.py fixtures/AtomJitProbe.java fixtures/m3jni.c fixtures/m3jni.cpp; do
    path=$(awk -F'\t' -v t="fixture-gate/$target" '$1=="FILE" && $3==t {print $2; exit}' "$MAP")
    show "$path" > "$SCRATCH/donor-fixture-crate/$target"
done
show "$(awk -F'\t' '$1=="SEAL" {print $2; exit}' "$MAP")" > "$SCRATCH/donor-fixture-crate/SOURCE_SHA256.tsv"
echo "FIXTURE_CRATE $SCRATCH/donor-fixture-crate (run fixture-gate.sh <it> <scratch> on Linux)"

# donor classes: the serial gate with its recipe package (-sourcepath resolves the planner closure), the work-order crate
gate=$(row FILE /M3OpenJdkAtomJitPatternGate.java)
gate_dir=$(dirname "$gate")
git -C "$DONOR_GIT" archive "$REV" "$gate_dir" | tar -x -C "$SCRATCH/donor-src"
crate_gate=$(row FILE /M3OpenJdkAtomPatternGate.java)
crate_cli=$(row FILE /M3OpenJdkWorkOrderCli.java)
crate_dir="$SCRATCH/donor-src/$(dirname "$crate_gate")"
mkdir -p "$crate_dir"
show "$crate_gate" > "$crate_dir/$(basename "$crate_gate")"
show "$crate_cli" > "$crate_dir/$(basename "$crate_cli")"
gate_root=${gate_dir%%/src/main/java/*}/src/main/java
"$JAVA_BIN/javac" --release 21 -proc:none -nowarn -d "$SCRATCH/donor-classes" -sourcepath "$SCRATCH/donor-src/$gate_root" \
    "$SCRATCH/donor-src/$gate" "$crate_dir/$(basename "$crate_gate")" "$crate_dir/$(basename "$crate_cli")"
donor_pkg=$(echo "$gate_dir" | sed 's#.*/src/main/java/##; s#/#.#g')
crate_pkg=$(dirname "$crate_gate" | sed 's#.*/src/main/java/##; s#/#.#g')
target_pkg=jdk.m3.tooling.atomjit
"$JAVA_BIN/javac" --release 21 -Xlint:all -Werror -d "$SCRATCH/driver" "$RECIPE_DIR/differential/GateVerdictDiff.java"

# serial gate verdicts (the donor test's recipe class and crate pom are the planner inputs the donor accepts)
gate_test=$(row FILE /M3OpenJdkAtomJitPatternGateTest.java)
recipe_class=$(show "$gate_test" | grep -o '"[a-z.]*\.M3AtomizeRecipe"' | head -1 | tr -d '"')
crate_pom=$(show "$gate_test" | grep -o '"[^"]*/pom\.xml"' | head -1 | tr -d '"')
if timeout "$TIMEOUT" "$JAVA_BIN/java" -cp "$SCRATCH/driver$SEP$SCRATCH/donor-classes$SEP$MODULE/target/classes" GateVerdictDiff \
        "$donor_pkg" "$target_pkg" "$recipe_class" "$crate_pom" "$SCENARIOS" 20261106 "$SCRATCH/verdicts" > "$SCRATCH/gate-verdicts.log" 2>&1; then
    echo "serial-gate IDENTICAL $(tail -1 "$SCRATCH/gate-verdicts.log")"
else
    fail "serial-gate $(tail -3 "$SCRATCH/gate-verdicts.log" | tr '\n' ' ')"
fi

# work orders
donor_token=$(awk -F'\t' '$1=="NAMING" {print $2; exit}' "$MAP")
target_token=$(awk -F'\t' '$1=="NAMING" {print $3; exit}' "$MAP")
git -C "$ROOT" ls-tree -r HEAD > "$SCRATCH/orders/tree.txt"
python -I "$RECIPE_DIR/differential/work_orders.py" "$SCRATCH/orders/tree.txt" "$donor_token" "$target_token" "$SCRATCH/orders/in"
identical=0; refused=0
for donor_queue in "$SCRATCH"/orders/in/*-donor.tsv; do
    name=$(basename "$donor_queue" -donor.tsv)
    for side in donor relocated; do
        queue="$SCRATCH/orders/in/$name-$side.tsv"
        if [ "$side" = donor ]; then cp="$SCRATCH/donor-classes"; main="$crate_pkg.M3OpenJdkWorkOrderCli"; else cp="$MODULE/target/classes"; main="$target_pkg.M3OpenJdkWorkOrderCli"; fi
        sha=$(sha256sum "$queue" | cut -d' ' -f1)
        set +e
        timeout "$TIMEOUT" "$JAVA_BIN/java" -cp "$cp" "$main" hsoliwal/M3jdk21 "$queue" "$sha" "$SCRATCH/orders/out-$name-$side.tsv" \
            > "$SCRATCH/orders/$name-$side.stdout" 2> "$SCRATCH/orders/$name-$side.stderr"
        echo $? > "$SCRATCH/orders/$name-$side.exit"
        set -e
        grep -m1 -E '^Exception|^Caused by' "$SCRATCH/orders/$name-$side.stderr" | sed 's/^Exception in thread "main" //' > "$SCRATCH/orders/$name-$side.refusal" || true
    done
    if cmp -s "$SCRATCH/orders/$name-donor.exit" "$SCRATCH/orders/$name-relocated.exit" \
            && cmp -s "$SCRATCH/orders/$name-donor.stdout" "$SCRATCH/orders/$name-relocated.stdout" \
            && cmp -s "$SCRATCH/orders/$name-donor.refusal" "$SCRATCH/orders/$name-relocated.refusal"; then
        if [ -f "$SCRATCH/orders/out-$name-donor.tsv" ] || [ -f "$SCRATCH/orders/out-$name-relocated.tsv" ]; then
            if cmp -s "$SCRATCH/orders/out-$name-donor.tsv" "$SCRATCH/orders/out-$name-relocated.tsv"; then
                identical=$((identical + 1))
                echo "orders $name IDENTICAL exit=$(cat "$SCRATCH/orders/$name-donor.exit") $(cat "$SCRATCH/orders/$name-donor.stdout") lines=$(wc -l < "$SCRATCH/orders/out-$name-donor.tsv") sha256=$(sha256sum "$SCRATCH/orders/out-$name-donor.tsv" | cut -c1-16)"
            else fail "orders $name work orders differ"; fi
        else
            refused=$((refused + 1))
            echo "orders $name IDENTICAL exit=$(cat "$SCRATCH/orders/$name-donor.exit") $(cat "$SCRATCH/orders/$name-donor.refusal")"
        fi
    else
        fail "orders $name donor [$(cat "$SCRATCH/orders/$name-donor.exit") $(cat "$SCRATCH/orders/$name-donor.refusal")] relocated [$(cat "$SCRATCH/orders/$name-relocated.exit") $(cat "$SCRATCH/orders/$name-relocated.refusal")]"
    fi
done
echo "work-orders admitted_identical=$identical refusals_identical=$refused"
# the relocated gate refuses a donor-named queue (the renamed schema token is part of its admission contract)
set +e
timeout "$TIMEOUT" "$JAVA_BIN/java" -cp "$MODULE/target/classes" "$target_pkg.M3OpenJdkWorkOrderCli" hsoliwal/M3jdk21 \
    "$SCRATCH/orders/in/admissible-donor.tsv" "$(sha256sum "$SCRATCH/orders/in/admissible-donor.tsv" | cut -d' ' -f1)" \
    "$SCRATCH/orders/out-cross.tsv" > /dev/null 2> "$SCRATCH/orders/cross.stderr"
cross=$?
set -e
echo "orders cross (relocated gate, donor-named queue) exit=$cross $(grep -m1 '^Exception' "$SCRATCH/orders/cross.stderr" | sed 's/^Exception in thread "main" //')"
[ "$cross" != 0 ] || fail "relocated gate admitted the donor-named queue"
echo "DIFFERENTIAL $status"
[ "$status" = PASS ]
