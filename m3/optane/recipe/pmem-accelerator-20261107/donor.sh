#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Extracts the donor blobs of every FILE row of RELOCATION_MAP.tsv from the donor git tree at the pin into one
# directory (file names = the donor base names), so that native-gate.sh (Linux) and javap_parity.sh compile the donor
# family from the same bytes the recipe relocated. Prints each blob id. A blobless clone is enough (one cat-file per
# row; nothing is checked out).
# usage: donor.sh <donor-git-dir> <donor-rev> <out-dir>
set -euo pipefail
RECIPE_DIR=$(cd "$(dirname "$0")" && pwd)
DONOR=$1
REV=$2
OUT=$3
commit=$(git -C "$DONOR" rev-parse "$REV^{commit}")
mkdir -p "$OUT"
while IFS=$'\t' read -r donor_path _target; do
    blob=$(git -C "$DONOR" rev-parse "$commit:$donor_path")
    git -C "$DONOR" cat-file blob "$blob" > "$OUT/$(basename "$donor_path")"
    echo "DONOR $blob $(basename "$donor_path")"
done < <(awk -F'\t' '$1=="FILE"{print $2 "\t" $3}' "$RECIPE_DIR/RELOCATION_MAP.tsv")
echo "DONOR commit=$commit files=$(ls "$OUT" | wc -l) out=$OUT"
