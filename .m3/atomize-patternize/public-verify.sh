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

command -v sha1sum >/dev/null 2>&1 || fail "SHA1SUM_REQUIRED" 2
a3_root="$repo/m3/vendor/synexia/synexia-openrewrite-recipes"
a3_manifest="$a3_root/src/main/resources/META-INF/m3/jdk-a3-recipe-export.tsv"
a3_expected_manifest_blob="8a3e3d6e802e95dbcc7b0bf83a347887b02d6717"
[[ -f "$a3_manifest" && ! -L "$a3_manifest" ]] || fail "PUBLIC_A3_EXPORT_MANIFEST_REQUIRED"

git_blob() {
  local file="$1" bytes
  bytes="$(wc -c < "$file" | tr -d '[:space:]')"
  { printf 'blob %s\0' "$bytes"; cat -- "$file"; } | sha1sum | awk '{print $1}'
}

a3_manifest_blob="$(git_blob "$a3_manifest")"
[[ "$a3_manifest_blob" == "$a3_expected_manifest_blob" ]] || \
  fail "PUBLIC_A3_EXPORT_MANIFEST_DRIFT expected=$a3_expected_manifest_blob actual=$a3_manifest_blob"

a3_files=0
while IFS=\t' read -r class_name source_path source_blob; do
  [[ -n "$class_name" ]] || continue
  [[ "$class_name" != \#* ]] || continue
  [[ "$class_name" != class ]] || continue
  [[ "$source_blob" =~ ^[0-9a-f]{40}$ ]] || fail "PUBLIC_A3_EXPORT_ROW_INVALID:$source_path"
  source_file="$a3_root/$source_path"
  [[ -f "$source_file" && ! -L "$source_file" ]] || fail "PUBLIC_A3_EXPORT_SOURCE_REQUIRED:$source_path"
  actual_blob="$(git_blob "$source_file")"
  [[ "$actual_blob" == "$source_blob" ]] || \
    fail "PUBLIC_A3_EXPORT_SOURCE_DRIFT:$source_path expected=$source_blob actual=$actual_blob"
  a3_files=$((a3_files + 1))
done < "$a3_manifest"

[[ "$a3_files" == 8 ]] || fail "PUBLIC_A3_EXPORT_FILE_COUNT:$a3_files"

{
  printf 'key\tvalue\n'
  printf 'mode\tPUBLIC_VERIFY_ONLY\n'
  printf 'manifest\t%s\n' 'm3/synexia-import/synexia-seed-export.tsv'
  printf 'manifestRoot\t%s\n' "$root"
  printf 'a3SourceRevision\t%s\n' 'a062661e91e769a00c2d027e1f04a7a0c8f9dc28'
  printf 'a3ExportManifestGitBlob\t%s\n' "$a3_manifest_blob"
  printf 'a3ExportFiles\t%s\n' "$a3_files"
  printf 'sourceMutationAuthority\tfalse\n'
  printf 'promotionAuthority\tfalse\n'
  printf 'privateSynexiaRequired\tfalse\n'
} > "$target/receipt.tsv"

printf 'M3_PUBLIC_ATOM_PATTERN_RECEIVER_PASS root=%s a3ExportFiles=%s a3ManifestBlob=%s\n' \
  "$root" "$a3_files" "$a3_manifest_blob"
