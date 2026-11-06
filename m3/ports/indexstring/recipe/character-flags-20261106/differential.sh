#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gate 3 of the relocation: the donor character-flags owner and its smoke oracle (compiled from the donor git tree with
# javac --release 21 -Xlint:all -Werror) and the relocated class (target/classes of m3/ports/indexstring) replay the same
# inputs.
# (a) Both smoke mains must print the same PASS line (pairs=256 triples=4096 rejectedInvalids=8); the donor's line names
#     the donor file and the relocated one the relocated file (the recipe renames the type inside the literal), so the
#     donor output is compared after the TYPE rows of RELOCATION_MAP.tsv are applied to it byte for byte (python re,
#     whole-word; sed under MSYS drops the CR of the println line), and both raw digests are printed.
# (b) M3IndexCharacterFlagsDump (test scope) replays the smoke inputs, the exhaustive 1,024 x 1,024 combine table,
#     labels over the 1,024 valid values, 1,000,000 seeded random pairs and 100,000 seeded random texts through both
#     classes; the dumps must be byte-identical.
# (c) of-only dumps (of/labels sections) of donor, relocated and, when present in the checkout, the sibling
#     jdk.m3.file.M3CharacterFlags must be byte-identical: the sibling is the same lineage (pre-combine until
#     m3/file/recipe/character-flags-20261106 aligned it; that recipe's differential.sh compares the full dumps). Its source
#     is found from SIBLING_CLASS through git ls-files, so a module move (m3/ports/file -> m3/file, #2580) needs no edit.
# Prints DIFFERENTIAL PASS|FAIL with the sha256 digests of every side. The donor package and type names come from
# RELOCATION_MAP.tsv next to this script, so no file in the naming scope names a donor package literally.
# usage: differential.sh <donor-git-dir> <donor-rev> <scratch-dir>   (JAVA_HOME = JDK 21, PYTHON optional; run after mvn package)
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../..")
ROOT=$(git -C "$MODULE" rev-parse --show-toplevel)
DONOR_GIT=$1
REV=$2
mkdir -p "$3/src" "$3/classes" "$3/sibling-classes"
SCRATCH=$(winpath "$3")
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
SIBLING_CLASS=jdk.m3.file.M3CharacterFlags
SIBLING_PATH=$(git -C "$ROOT" ls-files -- "*/src/main/java/${SIBLING_CLASS//.//}.java")
SIBLING="$ROOT/${SIBLING_PATH:-src/main/java/${SIBLING_CLASS//.//}.java (not tracked)}"
case "$SIBLING_PATH" in *$'\n'*) echo "DIFFERENTIAL FAIL: $SIBLING_CLASS is tracked more than once: $SIBLING_PATH"; exit 2 ;; esac
DUMP=M3IndexCharacterFlagsDump
donor_package=$(awk -F'\t' '$1=="PACKAGE" {print $2; exit}' "$MAP")
target_package=$(awk -F'\t' '$1=="PACKAGE" {print $3; exit}' "$MAP")
owner_path=$(awk -F'\t' '$1=="FILE" && $3 ~ /src\/main\/java\// {print $2; exit}' "$MAP")
oracle_path=$(awk -F'\t' '$1=="FILE" && $3 ~ /src\/test\/java\// {print $2; exit}' "$MAP")
donor_owner=$(basename "$owner_path" .java)
donor_oracle=$(basename "$oracle_path" .java)
target_owner=$(awk -F'\t' -v d="$donor_owner" '$1=="TYPE" && $2==d {print $3; exit}' "$MAP")
target_oracle=$(awk -F'\t' -v d="$donor_oracle" '$1=="TYPE" && $2==d {print $3; exit}' "$MAP")
PY=${PYTHON:-python}
donor_commit=$(git -C "$DONOR_GIT" rev-parse "$REV^{commit}")
git -C "$DONOR_GIT" show "$REV:$owner_path" > "$SCRATCH/src/$donor_owner.java"
git -C "$DONOR_GIT" show "$REV:$oracle_path" > "$SCRATCH/src/$donor_oracle.java"
"$JAVA_BIN/javac" --release 21 -encoding UTF-8 -Xlint:all -Werror -d "$SCRATCH/classes" "$SCRATCH/src/$donor_owner.java" "$SCRATCH/src/$donor_oracle.java"
echo "DONOR $donor_commit $owner_path $oracle_path"
RELOCATED_CP="$MODULE/target/classes$SEP$MODULE/target/test-classes"
DONOR_CP="$SCRATCH/classes$SEP$MODULE/target/test-classes"
"$JAVA_BIN/java" -cp "$SCRATCH/classes" "$donor_package.$donor_oracle" > "$SCRATCH/donor-smoke.txt"
"$JAVA_BIN/java" -cp "$RELOCATED_CP" "$target_package.$target_oracle" > "$SCRATCH/relocated-smoke.txt"
"$PY" -I - "$MAP" "$SCRATCH/donor-smoke.txt" "$SCRATCH/donor-smoke-mapped.txt" <<'PYEOF'
import re, sys
rows = [line.split("\t") for line in open(sys.argv[1], encoding="utf-8").read().splitlines() if line.startswith("TYPE\t")]
data = open(sys.argv[2], "rb").read()
for _kind, donor, target, *_note in rows:
    if donor != target:
        data = re.sub(rb"\b" + donor.encode() + rb"\b", target.encode(), data)
open(sys.argv[3], "wb").write(data)
PYEOF
"$JAVA_BIN/java" -cp "$DONOR_CP" "$target_package.$DUMP" "$donor_package.$donor_owner" > "$SCRATCH/donor-dump.txt"
"$JAVA_BIN/java" -cp "$RELOCATED_CP" "$target_package.$DUMP" "$target_package.$target_owner" > "$SCRATCH/relocated-dump.txt"
"$JAVA_BIN/java" -cp "$DONOR_CP" "$target_package.$DUMP" "$donor_package.$donor_owner" of-only > "$SCRATCH/donor-ofonly.txt"
"$JAVA_BIN/java" -cp "$RELOCATED_CP" "$target_package.$DUMP" "$target_package.$target_owner" of-only > "$SCRATCH/relocated-ofonly.txt"
status=PASS
compare() { # label donor-file relocated-file
    local donor_sha relocated_sha verdict
    donor_sha=$(sha256sum "$2" | cut -d' ' -f1)
    relocated_sha=$(sha256sum "$3" | cut -d' ' -f1)
    if cmp -s "$2" "$3"; then verdict=IDENTICAL; else verdict=DIFFERENT; status=FAIL; fi
    echo "$1 $verdict donor_sha256=$donor_sha relocated_sha256=$relocated_sha bytes=$(wc -c < "$2") lines=$(wc -l < "$2")"
}
echo "smoke-raw $(cmp -s "$SCRATCH/donor-smoke.txt" "$SCRATCH/relocated-smoke.txt" && echo IDENTICAL || echo DIFFERENT_BY_TYPE_NAME_ONLY) donor_sha256=$(sha256sum "$SCRATCH/donor-smoke.txt" | cut -d' ' -f1) relocated_sha256=$(sha256sum "$SCRATCH/relocated-smoke.txt" | cut -d' ' -f1)"
compare smoke-type-mapped "$SCRATCH/donor-smoke-mapped.txt" "$SCRATCH/relocated-smoke.txt"
compare dump "$SCRATCH/donor-dump.txt" "$SCRATCH/relocated-dump.txt"
compare of-only "$SCRATCH/donor-ofonly.txt" "$SCRATCH/relocated-ofonly.txt"
if [ -f "$SIBLING" ]; then
    "$JAVA_BIN/javac" --release 21 -encoding UTF-8 -Xlint:all -Werror -d "$SCRATCH/sibling-classes" "$SIBLING"
    "$JAVA_BIN/java" -cp "$SCRATCH/sibling-classes$SEP$MODULE/target/test-classes" "$target_package.$DUMP" "$SIBLING_CLASS" of-only > "$SCRATCH/sibling-ofonly.txt"
    compare sibling-of-only "$SCRATCH/sibling-ofonly.txt" "$SCRATCH/relocated-ofonly.txt"
else
    echo "sibling-of-only SKIPPED ($SIBLING not in this checkout)"
fi
tr '\n' ' ' < "$SCRATCH/relocated-smoke.txt"; echo
head -n 1 "$SCRATCH/relocated-dump.txt"
tail -n 1 "$SCRATCH/relocated-dump.txt"
echo "DIFFERENTIAL $status"
[ "$status" = PASS ]
