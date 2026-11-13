#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Mutant gate of recipe object-signal-20261107. Each mutant call changes exactly one line of one relocated owner (a
# scratch copy; the module is never written), compiles that owner over target/classes, and runs three detectors on the
# mutated class set: the relocated donor oracle (M3ObjectSignalSelfTest), the naive per-row reference model
# (M3ObjectSignalReferenceModel) and the dump driver against the donor dump of differential.sh. A mutant is KILLED
# when at least one detector fails or differs; the gate passes only when every mutant is killed.
# usage: mutants.sh <differential-scratch-dir> <scratch-dir> [queries] [seed]   (run differential.sh first; JAVA_HOME)
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
MODULE=$(winpath "$(dirname "$0")/../..")
DIFF=$(winpath "$1")
mkdir -p "$2"
SCRATCH=$(winpath "$2")
QUERIES=${3:-20000}
SEED=${4:-20261107}
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
JAVA_OPTS="-Djava.io.tmpdir=${M3_TMPDIR:-$SCRATCH}"
SRC="$MODULE/src/main/java/jdk/m3/collections"
killed=0
total=0

# mutant <id> <owner> <exact line (fixed string)> <replacement line> <what it breaks>
mutant() {
    local id=$1 owner=$2 before=$3 after=$4 what=$5
    local work="$SCRATCH/$id" hits detectors="" cp
    total=$((total + 1))
    rm -rf "$work"
    mkdir -p "$work/src" "$work/classes"
    hits=$(grep -cxF -- "$before" "$SRC/$owner.java" || true)
    if [ "$hits" != 1 ]; then
        echo "$id REFUSED: the mutated line occurs $hits times in $owner"
        exit 2
    fi
    BEFORE="$before" AFTER="$after" awk '$0 == ENVIRON["BEFORE"] {print ENVIRON["AFTER"]; next} {print}' \
        "$SRC/$owner.java" > "$work/src/$owner.java"
    "$JAVA_BIN/javac" -J"$JAVA_OPTS" --release 21 -proc:none -nowarn -cp "$MODULE/target/classes" \
        -d "$work/classes" "$work/src/$owner.java"
    cp="$work/classes$SEP$MODULE/target/classes$SEP$MODULE/target/test-classes$SEP$DIFF/relocated-classes"
    "$JAVA_BIN/java" "$JAVA_OPTS" -cp "$cp" jdk.m3.collections.M3ObjectSignalSelfTest \
        > "$work/oracle.txt" 2>&1 || detectors="$detectors oracle"
    "$JAVA_BIN/java" "$JAVA_OPTS" -cp "$cp" jdk.m3.collections.M3ObjectSignalReferenceModel "$SEED" \
        > "$work/model.txt" 2>&1 || detectors="$detectors model"
    "$JAVA_BIN/java" "$JAVA_OPTS" -cp "$cp" jdk.m3.collections.M3ObjectSignalDump "$QUERIES" "$SEED" \
        > "$work/dump.txt" 2>&1 || true
    cmp -s "$work/dump.txt" "$DIFF/donor-dump-jit.txt" || detectors="$detectors dump"
    if [ -n "$detectors" ]; then
        killed=$((killed + 1))
        echo "$id KILLED by$detectors ($owner: $what)"
        echo "    oracle: $(head -n 1 "$work/oracle.txt" | cut -c1-120)"
        echo "    model:  $(head -n 1 "$work/model.txt" | cut -c1-120)"
    else
        echo "$id SURVIVED ($owner: $what)"
    fi
}

mutant M1 M3ObjectSignalOps \
    '                selected[word] &= ~featurePlanes[base + word];' \
    '                selected[word] &= featurePlanes[base + word];' \
    'forbidden plane applied as a required plane'
mutant M2 M3ObjectSignalQuery \
    '            if ((candidateFlags & M3ObjectSignal.HAS_HASH) == 0 || candidateHash != exactHash64) return false;' \
    '            if (candidateHash != exactHash64) return false;' \
    'REQUIRE_HASH no longer requires the HAS_HASH flag'
mutant M3 M3ObjectSignalTable \
    '        for (int feature = 0; feature < FEATURE_BITS; feature++) {' \
    '        for (int feature = 0; feature < FEATURE_BITS - 1; feature++) {' \
    'featureBitsAt / signalAt lose feature bit 63'
mutant M4 M3ObjectSignalBitmap \
    '        words[words.length - 1] &= (1L << (size & 63)) - 1L;' \
    '        words[words.length - 1] &= (1L << (size & 63));' \
    'tail trim keeps the wrong bit (all() and copies)'
mutant M5 M3ObjectSignalMatcher \
    '                    exactChecks++;' \
    '                    exactChecks += 0;' \
    'exact-stage counter not counted'
mutant M6 M3ObjectSignalMath \
    '                bits |= bit(mix64(0xa7L ^ pair));' \
    '                bits |= 0L;' \
    'String bigram features dropped (the containment law still holds)'
mutant M7 M3ObjectSignalQuery \
    '            needle.hasLength() ? needle.logicalLength() : UNBOUNDED,' \
    '            UNBOUNDED,' \
    'maybeContains loses its minimum length'

echo "MUTANTS killed=$killed of $total"
[ "$killed" = "$total" ]
