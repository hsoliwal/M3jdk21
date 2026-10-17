#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
SELF="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd -- "$SELF/../../../.." && pwd)"
MAIN="$ROOT/m3/collections/src/main/java/com/m3/collections/M3CollectionViews.java"
BATCH="$ROOT/m3/collections/src/test/java/com/m3/collections/FilteredBatchContracts.java"
REP="$ROOT/m3/collections/wrappers/src/test/java/com/m3/collections/M3StoredRepresentativeProof.java"
BASE="c5e5e90e2dcc2298a372ab39640120dd60a2edb7"
CANONICAL_ARGS=()
if [[ -n "${M3_CANONICAL_CHECKOUT:-}" ]]; then
  CANONICAL_ARGS=(--canonical "$M3_CANONICAL_CHECKOUT")
fi
python3 "$SELF/verify.py" "${CANONICAL_ARGS[@]}"
command -v javac >/dev/null
command -v java >/dev/null
command -v javap >/dev/null
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
mkdir -p "$OUT/after" "$OUT/before/com/m3/collections" "$OUT/before-classes"
# Materialized Java21 owner and two independent regression corpora.
javac --release 21 -Xlint:all -Werror -d "$OUT/after" "$MAIN" "$BATCH" "$REP"
java -ea -Xmx128m -Xss256k -cp "$OUT/after" com.m3.collections.FilteredBatchContracts
java -ea -Xmx128m -Xss256k -cp "$OUT/after" com.m3.collections.M3StoredRepresentativeProof
java -ea -Xint -Xmx128m -Xss256k -cp "$OUT/after" com.m3.collections.FilteredBatchContracts stable
java -ea -Xint -Xmx128m -Xss256k -cp "$OUT/after" com.m3.collections.M3StoredRepresentativeProof

# Distinguish public descriptor preservation from private behavioral repair.
if git -C "$ROOT" cat-file -e "$BASE^{commit}" 2>/dev/null; then
  git -C "$ROOT" show "$BASE:m3/collections/src/main/java/com/m3/collections/M3CollectionViews.java" \
    > "$OUT/before/com/m3/collections/M3CollectionViews.java"
  javac --release 21 -Xlint:all -Werror -d "$OUT/before-classes" \
    "$OUT/before/com/m3/collections/M3CollectionViews.java"
  javap -classpath "$OUT/before-classes" -public -s com.m3.collections.M3CollectionViews \
    > "$OUT/before-api"
  javap -classpath "$OUT/after" -public -s com.m3.collections.M3CollectionViews \
    > "$OUT/after-api"
  diff -u "$OUT/before-api" "$OUT/after-api"
  echo "PUBLIC_JAVAP_DESCRIPTORS_UNCHANGED"
else
  echo "REQUIRED_BASE_REVISION_UNAVAILABLE: $BASE" >&2
  exit 3
fi
echo "M3_FILTERED_SORTED_STORED_BATCH_PROOF_COMPLETE"
