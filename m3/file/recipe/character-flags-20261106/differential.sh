#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gate of recipe ALIGN_FILE_CHARACTER_FLAGS v1: jdk.m3.file.M3CharacterFlags (target/classes of this module) and its
# sibling jdk.internal.m3.indexstring.M3IndexCharacterFlags (#2523) replay the inputs of #2523's differential through its
# reflective driver M3IndexCharacterFlagsDump (test-classes of the sibling's module): modifiers and constants, the 16
# edge strings, 256 pairs, 4,096 ordered triples, the exhaustive 1,024 x 1,024 combine table, labels over the 1,024
# valid values, 1,000,000 seeded random pairs and 100,000 seeded random texts (full), then the of/labels sections alone
# (of-only). The driver prints no type name, so "identical modulo type name" is byte-identical here; #2523 recorded
# efa7e69162f2... (full) and fb1aced4b0ee... (of-only) for the donor and the sibling. javap -public and javap -p of the
# two classes are compared after both type names are replaced by TYPE.
# The sibling's module is found from its class name through git ls-files (no module path literal).
# Prints DIFFERENTIAL PASS|FAIL with the sha256 digests of both sides.
# usage: differential.sh <scratch-dir>   (JAVA_HOME = JDK 21; run after mvn package of this module and of the sibling's)
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
MODULE=$(winpath "$(dirname "$0")/../..")
ROOT=$(git -C "$MODULE" rev-parse --show-toplevel)
TARGET=jdk.m3.file.M3CharacterFlags
SIBLING=jdk.internal.m3.indexstring.M3IndexCharacterFlags
DUMP=jdk.internal.m3.indexstring.M3IndexCharacterFlagsDump
sibling_source=$(git -C "$ROOT" ls-files -- "*/src/main/java/${SIBLING//.//}.java")
case "$sibling_source" in ""|*$'\n'*) echo "DIFFERENTIAL FAIL: $SIBLING must be tracked exactly once: '$sibling_source'"; exit 2 ;; esac
SIBLING_MODULE="$ROOT/${sibling_source%%/src/main/java/*}"
mkdir -p "$1"
SCRATCH=$(winpath "$1")
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
CP="$MODULE/target/classes$SEP$SIBLING_MODULE/target/classes$SEP$SIBLING_MODULE/target/test-classes"
status=PASS
compare() { # label target-file sibling-file
    local target_sha sibling_sha verdict
    target_sha=$(sha256sum "$2" | cut -d' ' -f1)
    sibling_sha=$(sha256sum "$3" | cut -d' ' -f1)
    if cmp -s "$2" "$3"; then verdict=IDENTICAL; else verdict=DIFFERENT; status=FAIL; fi
    echo "$1 $verdict target_sha256=$target_sha sibling_sha256=$sibling_sha bytes=$(wc -c < "$2") lines=$(wc -l < "$2")"
}
for mode in full of-only; do
    "$JAVA_BIN/java" -cp "$CP" "$DUMP" "$TARGET" "$mode" > "$SCRATCH/target-$mode.txt"
    "$JAVA_BIN/java" -cp "$CP" "$DUMP" "$SIBLING" "$mode" > "$SCRATCH/sibling-$mode.txt"
    compare "dump-$mode" "$SCRATCH/target-$mode.txt" "$SCRATCH/sibling-$mode.txt"
done
for view in -public -p; do
    target_view=$("$JAVA_BIN/javap" $view -cp "$CP" "$TARGET")
    sibling_view=$("$JAVA_BIN/javap" $view -cp "$CP" "$SIBLING")
    target_view=${target_view//$TARGET/TYPE}
    sibling_view=${sibling_view//$SIBLING/TYPE}
    printf '%s\n' "${target_view//${TARGET##*.}/TYPE}" | tr -d '\r' > "$SCRATCH/target-javap$view.txt"
    printf '%s\n' "${sibling_view//${SIBLING##*.}/TYPE}" | tr -d '\r' > "$SCRATCH/sibling-javap$view.txt"
    compare "javap$view" "$SCRATCH/target-javap$view.txt" "$SCRATCH/sibling-javap$view.txt"
done
head -n 1 "$SCRATCH/target-full.txt"
tail -n 1 "$SCRATCH/target-full.txt"
echo "DIFFERENTIAL $status"
[ "$status" = PASS ]
