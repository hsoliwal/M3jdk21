#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
#
# Owning generator for m3/tooling/migration-recipes/src/test/resources/com/m3/rewrite/atom/spectrum/CORPUS.tsv
# (consumed by com.m3.rewrite.synexia.M3PureIntRepositoryCorpusTest.verifyPin).
#
# Keeps the pinned path and category columns of the existing CORPUS.tsv and recomputes
# bytes, git blob SHA-1 and SHA-256 from the working tree at ROOT with CPU tools only.
#
# usage: regenerate_spectrum_corpus.sh <repository-root> [commit-sha] [existing-CORPUS.tsv] > CORPUS.tsv
set -euo pipefail
ROOT="${1:?repository root}"
COMMIT="${2:-$(git -C "$ROOT" rev-parse HEAD)}"
PINS="${3:-$ROOT/m3/tooling/migration-recipes/src/test/resources/com/m3/rewrite/atom/spectrum/CORPUS.tsv}"
printf '# repository=hsoliwal/M3jdk21\tcommit=%s\n' "$COMMIT"
printf 'path\tcategory\tbytes\tgit_blob_sha1\tsha256\n'
awk -F'\t' '!/^#/ && $1!="path" {print $1"\t"$2}' "$PINS" | while IFS=$'\t' read -r path category; do
  file="$ROOT/$path"
  [[ -f "$file" ]] || { echo "missing pinned file: $path" >&2; exit 1; }
  bytes=$(wc -c < "$file" | tr -d ' ')
  blob=$(git hash-object "$file")
  sha=$(sha256sum "$file" | cut -d' ' -f1)
  printf '%s\t%s\t%s\t%s\t%s\n' "$path" "$category" "$bytes" "$blob" "$sha"
done
