#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo="$(cd "$here/../.." && pwd)"
target="$repo/.m3/target/atomize-patternize/public-receiver"
classes="$target/classes"
manifest="$repo/m3/synexia-import/synexia-seed-export.tsv"
src="$repo/m3/synexia-import/src/main/java/com/m3/synexia/importer"

fail() {
  printf '%s\n' "$1" >&2
  exit "${2:-4}"
}

command -v javac >/dev/null 2>&1 || fail "JAVAC_REQUIRED" 2
command -v java >/dev/null 2>&1 || fail "JAVA_REQUIRED" 2
[[ -f "$manifest" && ! -L "$manifest" ]] || fail "SYNEXIA_PUBLIC_MANIFEST_REQUIRED"

rm -rf -- "$target"
mkdir -p "$classes"

sources=(
  "$src/SynexiaCanonicalFamilyPolicy.java"
  "$src/SynexiaImportManifest.java"
  "$src/SynexiaImporter.java"
  "$src/SynexiaImportCli.java"
)

for file in "${sources[@]}"; do
  [[ -f "$file" && ! -L "$file" ]] || fail "PUBLIC_RECEIVER_SOURCE_REQUIRED:$file"
done

javac --release 21 -proc:none -implicit:none -encoding UTF-8 -Xlint:all -Werror \
  -d "$classes" "${sources[@]}"

root="$(
  java -cp "$classes" com.m3.synexia.importer.SynexiaImportCli \
    verify-target "$manifest" "$repo" "$repo"
)"

[[ "$root" =~ ^[0-9a-f]{64}$ ]] || fail "PUBLIC_RECEIVER_ROOT_INVALID"

{
  printf 'key\tvalue\n'
  printf 'mode\tPUBLIC_VERIFY_ONLY\n'
  printf 'manifest\t%s\n' 'm3/synexia-import/synexia-seed-export.tsv'
  printf 'manifestRoot\t%s\n' "$root"
  printf 'sourceMutationAuthority\tfalse\n'
  printf 'promotionAuthority\tfalse\n'
  printf 'privateSynexiaRequired\tfalse\n'
} > "$target/receipt.tsv"

printf 'M3_PUBLIC_ATOM_PATTERN_RECEIVER_PASS root=%s\n' "$root"
