#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Donor-vs-restored differential of recipe retained-port-20261107 (m3/llm).
# usage: bash differential.sh <repo root> <scratch dir> [java home]
# Exports the donor module (PR #2108 head 58bc0d2d297e) with git archive, compiles donor and restored main trees with
# JDK 21 (-Xlint:all,-module -Werror), compiles differential/LlmDifferentialDump.java.tpl once per package, runs both on
# the seeded corpus (Random 20261107: 300 documents, 60 BM25 queries) and diffs the dumps. PASS = the dumps differ in
# exactly one line pair and that line is "capabilities ..." (the declared adaptation: the CONST_KEBAB schema id).
set -euo pipefail
ROOT="$(cd "$1" && pwd)"; SCRATCH="$2"; JH="${3:-${JAVA_HOME:-C:/Program Files/Java/jdk-21}}"
DONOR_REV=58bc0d2d297e
HERE="$(cd "$(dirname "$0")" && pwd)"
mkdir -p "$SCRATCH/donor" "$SCRATCH/tmp"
TMP="-Djava.io.tmpdir=$SCRATCH/tmp"
git -C "$ROOT" archive "$DONOR_REV" m3/llm | tar -x -C "$SCRATCH/donor"
compile() { # <src root> <out dir>
  rm -rf "$2"; mkdir -p "$2"
  "$JH/bin/javac" "-J$TMP" -d "$2" -Xlint:all,-module -Werror $(find "$1" -name '*.java')
}
compile "$SCRATCH/donor/m3/llm/src/main" "$SCRATCH/donor-classes"
compile "$ROOT/m3/llm/src/main" "$SCRATCH/restored-classes"
run() { # <side> <package> <classes>
  mkdir -p "$SCRATCH/driver-$1/src" "$SCRATCH/driver-$1/classes"
  sed "s/__PKG__/$2/g" "$HERE/differential/LlmDifferentialDump.java.tpl" > "$SCRATCH/driver-$1/src/LlmDifferentialDump.java"
  "$JH/bin/javac" "-J$TMP" -Xlint:all -Werror -cp "$3" -d "$SCRATCH/driver-$1/classes" "$SCRATCH/driver-$1/src/LlmDifferentialDump.java"
  "$JH/bin/java" "$TMP" -cp "$3;$SCRATCH/driver-$1/classes" "$2.differential.LlmDifferentialDump" "$SCRATCH/dump-$1.txt"
  echo "$1 lines=$(wc -l < "$SCRATCH/dump-$1.txt") sha256=$(sha256sum "$SCRATCH/dump-$1.txt" | cut -c1-64)"
}
run donor "com.m3.llm" "$SCRATCH/donor-classes"
run restored jdk.m3.llm "$SCRATCH/restored-classes"
diff "$SCRATCH/dump-donor.txt" "$SCRATCH/dump-restored.txt" > "$SCRATCH/differential-diff.log" || true
CHANGED=$(grep -c '^[<>] ' "$SCRATCH/differential-diff.log" || true); CHANGED=${CHANGED:-0}
DECLARED=$(grep -c '^[<>] capabilities ' "$SCRATCH/differential-diff.log" || true); DECLARED=${DECLARED:-0}
echo "differential changed_lines=$CHANGED declared_lines=$DECLARED"
if [ "$CHANGED" = 2 ] && [ "$DECLARED" = 2 ]; then echo "DIFFERENTIAL PASS (identical except the declared capabilities schema id)"; exit 0; fi
echo "DIFFERENTIAL FAIL"; exit 1
