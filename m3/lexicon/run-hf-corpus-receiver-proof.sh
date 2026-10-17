#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# Source-sealed standalone target receiver gate. No network access, corpus bytes or JNI.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
JAVAC=javac
JAVA=java
if [[ -n "${M3_JDK:-}" ]]; then
  JAVAC="$M3_JDK/bin/javac"
  JAVA="$M3_JDK/bin/java"
fi
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
mkdir -p "$WORK/classes" "$WORK/test"
"$JAVAC" --release 21 -Xlint:all -Werror -d "$WORK/classes" \
  "$ROOT/m3/core/src/com/m3/text/M3CorpusProjectionDigest.java" \
  "$ROOT/m3/core/src/com/m3/text/M3CorpusSourceReceipt.java" \
  "$ROOT/m3/core/src/com/m3/text/M3LanguageCorpusManifest.java"
"$JAVAC" --release 21 -Xlint:all -Werror -cp "$WORK/classes" \
  -d "$WORK/test" "$ROOT/m3/core/test/M3LanguageCorpusManifestTest.java"
for mode in jit interpreted no-compact c2; do
  flags=()
  case "$mode" in
    interpreted) flags=(-Xint) ;;
    no-compact) flags=(-XX:-CompactStrings) ;;
    c2) flags=(-Xbatch -XX:-TieredCompilation) ;;
  esac
  "$JAVA" -ea -esa "${flags[@]}" -cp "$WORK/classes:$WORK/test" \
    M3LanguageCorpusManifestTest
done
echo "M3_HF_CORPUS_RECEIVER_GATE_PASS modes=4"
