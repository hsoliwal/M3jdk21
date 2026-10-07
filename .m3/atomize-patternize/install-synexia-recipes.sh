#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo="$(cd "$here/../.." && pwd)"
target="$repo/.m3/target/atomize-patternize"
receipt="$target/synexia-recipe-install-receipt.tsv"

readonly SYNEXIA_REPOSITORY_URL="https://github.com/hsoliwal/com.synexia.git"
readonly SYNEXIA_PINNED_REVISION="2ddd629bcc5f037e26afe75a7655773abc79a138"
readonly SYNEXIA_PINNED_VERSION="1.0.0-SNAPSHOT"
readonly SYNEXIA_LICENSE_BLOB="29f81d812f3e768fa89638d1f72920dbfd1413a8"
readonly SYNEXIA_NOTICE_BLOB="97bb5d49f8bd3bc573c19afe4ea2392a3d02704e"
readonly SYNEXIA_ROOT_POM_BLOB="b0168b6bc0c88826571980b7252977801025543f"

fail() {
  printf '%s\n' "$1" >&2
  exit "${2:-4}"
}

command -v git >/dev/null 2>&1 || fail "GIT_REQUIRED" 2
mkdir -p "$target"

revision="${SYNEXIA_SOURCE_REVISION:-$SYNEXIA_PINNED_REVISION}"
[[ "$revision" == "$SYNEXIA_PINNED_REVISION" ]] ||   fail "M3_SYNEXIA_REVISION_NOT_PINNED expected=$SYNEXIA_PINNED_REVISION actual=$revision"

source_checkout="${SYNEXIA_SOURCE_CHECKOUT:-$target/synexia-source}"
if [[ ! -d "$source_checkout/.git" ]]; then
  [[ ! -e "$source_checkout" ]] || fail "M3_SYNEXIA_SOURCE_NOT_GIT:$source_checkout"
  mkdir -p "$source_checkout"
  git -C "$source_checkout" init -q
  git -C "$source_checkout" remote add origin "$SYNEXIA_REPOSITORY_URL"
  git -C "$source_checkout" fetch -q --depth 1 origin "$revision"
  git -C "$source_checkout" checkout -q --detach FETCH_HEAD
fi

actual_revision="$(git -C "$source_checkout" rev-parse HEAD)"
[[ "$actual_revision" == "$revision" ]] ||   fail "M3_SYNEXIA_SOURCE_REVISION_DRIFT expected=$revision actual=$actual_revision"
git -C "$source_checkout" diff --quiet HEAD -- || fail "M3_SYNEXIA_TRACKED_SOURCE_DIRTY"
git -C "$source_checkout" diff --cached --quiet HEAD -- || fail "M3_SYNEXIA_INDEX_DIRTY"

license_blob="$(git -C "$source_checkout" rev-parse HEAD:LICENSE)"
notice_blob="$(git -C "$source_checkout" rev-parse HEAD:NOTICE)"
pom_blob="$(git -C "$source_checkout" rev-parse HEAD:pom.xml)"
[[ "$license_blob" == "$SYNEXIA_LICENSE_BLOB" ]] || fail "M3_SYNEXIA_LICENSE_BLOB_DRIFT"
[[ "$notice_blob" == "$SYNEXIA_NOTICE_BLOB" ]] || fail "M3_SYNEXIA_NOTICE_BLOB_DRIFT"
[[ "$pom_blob" == "$SYNEXIA_ROOT_POM_BLOB" ]] || fail "M3_SYNEXIA_ROOT_POM_BLOB_DRIFT"

grep -Fq '<name>Apache License, Version 2.0</name>' "$source_checkout/pom.xml" ||   fail "M3_SYNEXIA_APACHE_LICENSE_DECLARATION_REQUIRED"
grep -Fq '<revision>1.0.0-SNAPSHOT</revision>' "$source_checkout/pom.xml" ||   fail "M3_SYNEXIA_VERSION_DRIFT"

if [[ -x "$source_checkout/mvnw" ]]; then
  maven=("$source_checkout/mvnw")
elif command -v mvn >/dev/null 2>&1; then
  maven=("$(command -v mvn)")
else
  fail "MAVEN_REQUIRED" 2
fi

local_repo="${SYNEXIA_MAVEN_REPO_LOCAL:-$target/m2}"
mkdir -p "$local_repo"

"${maven[@]}" -B -ntp   -f "$source_checkout/pom.xml"   -Dmaven.repo.local="$local_repo"   -DskipTests   -Dmaven.test.skip=true   -pl synexia-openrewrite-recipes   -am   install

jar="$local_repo/com/synexia/synexia-openrewrite-recipes/$SYNEXIA_PINNED_VERSION/synexia-openrewrite-recipes-$SYNEXIA_PINNED_VERSION.jar"
[[ -f "$jar" && ! -L "$jar" ]] || fail "M3_SYNEXIA_RECIPE_JAR_MISSING:$jar"
jar_sha="$(sha256sum -- "$jar" | awk '{print $1}')"
[[ "$jar_sha" =~ ^[0-9a-f]{64}$ ]] || fail "M3_SYNEXIA_RECIPE_JAR_SHA_INVALID"

tmp="$receipt.new"
{
  printf 'kind\tvalue\n'
  printf 'sourceRepository\t%s\n' "$SYNEXIA_REPOSITORY_URL"
  printf 'sourceRevision\t%s\n' "$revision"
  printf 'sourceLicense\tApache-2.0\n'
  printf 'sourceLicenseBlob\t%s\n' "$license_blob"
  printf 'sourceNoticeBlob\t%s\n' "$notice_blob"
  printf 'sourceRootPomBlob\t%s\n' "$pom_blob"
  printf 'recipeVersion\t%s\n' "$SYNEXIA_PINNED_VERSION"
  printf 'recipeArtifact\tcom.synexia:synexia-openrewrite-recipes:%s\n' "$SYNEXIA_PINNED_VERSION"
  printf 'recipeJarSha256\t%s\n' "$jar_sha"
  printf 'mavenRepository\t%s\n' "$local_repo"
  printf 'buildStatus\tPASS\n'
} > "$tmp"
mv -f -- "$tmp" "$receipt"

printf 'M3_SYNEXIA_RECIPE_INSTALL_PASS revision=%s version=%s jarSha256=%s\n'   "$revision" "$SYNEXIA_PINNED_VERSION" "$jar_sha"
