#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# ADAPT existing four-file proof 71559043407cf6cc03883d722e327e8ea44ed42a.
set -euo pipefail
CRATE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$CRATE/../../.." && pwd)"
python3 -I -S -B "$CRATE/verify_source_seals.py" "$ROOT"
: "${TMPDIR:?explicit admitted TMPDIR is required}"
if [[ "$TMPDIR" != /* || ! -d "$TMPDIR" || -L "$TMPDIR" ]]; then
  printf '%s\n' 'HOLD_STORAGE: explicit existing non-symlink TMPDIR required' >&2
  exit 1
fi
proof_work="$(mktemp -d "$TMPDIR/m3-javac-regex-current-owner.XXXXXXXX")"
trap 'rm -rf -- "$proof_work"' EXIT

STRINGS="$ROOT/synexia-indexstring/src"
COMPILER="$ROOT/synexia-mindex/compiler/src"
# The existing external bounded worker owns aggregate storage/process admission.
# All javac/java temporary output, including the original facts contract, stays in proof_work.
javac --release 21 -Xlint:all -Werror -J-Djava.io.tmpdir="$proof_work" -d "$proof_work" \
  "$STRINGS/main/java/com/synexia/indexstring/MIndexJdkConstantRegexPlan.java" \
  "$CRATE/src/test/java/com/synexia/indexstring/MIndexJdkConstantRegexCurrentOwnerContract.java" \
  "$COMPILER/main/java/com/synexia/mindex/compiler/MIndexJavaConstantStringFacts.java" \
  "$COMPILER/test/java/com/synexia/mindex/compiler/MIndexJavaConstantStringFactsContract.java"
java -Djava.io.tmpdir="$proof_work" -ea -cp "$proof_work" com.synexia.indexstring.MIndexJdkConstantRegexCurrentOwnerContract
java -Djava.io.tmpdir="$proof_work" -ea -cp "$proof_work" com.synexia.mindex.compiler.MIndexJavaConstantStringFactsContract
printf '%s\n' 'M3_JAVAC_CURRENT_OWNER_REGEX_JDK21=PASS scope=JDK_ONLY_FOUR_FILE_CONTRACTS'
