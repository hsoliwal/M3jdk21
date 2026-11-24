#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# Provider-dependent JUnit contracts run against the complete source-built JDK module.
set -euo pipefail
if [[ $# -ne 3 ]]; then
  echo 'usage: test-provider-junit.sh /absolute/source-built-jdk /absolute/junit-console.jar /fresh/output' >&2
  exit 64
fi
jdk="$1"
junit="$2"
out="$3"
repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
testroot="$repo/test/jdk/jdk/m3/collections"
sources="$testroot/providers"
manifest="$repo/m3/collections/qualification/provider-tests-20261123.tsv"
if [[ ! -x "$jdk/bin/java" || ! -x "$jdk/bin/javac" ||
      ! -x "$jdk/bin/javap" || ! -s "$jdk/lib/modules" ||
      ! -s "$jdk/jmods/jdk.m3.collections.jmod" || ! -s "$junit" ]]; then
  echo 'missing installed M3 JDK module or JUnit launcher' >&2
  exit 65
fi
if [[ -e "$out" || -L "$out" ]]; then
  echo 'fresh output directory required' >&2
  exit 66
fi
for family in apache eclipse guava vavr; do
  [[ -d "$sources/$family" && ! -L "$sources/$family" ]] || {
    echo "missing provider test family $family" >&2
    exit 67
  }
done
[[ -f "$manifest" && ! -L "$manifest" ]] || exit 68
checks=0
declare -A witnessed=()
while IFS=$'\t' read -r old new expected; do
  [[ -n "$old" && "$old" != \#* ]] || continue
  if [[ -e "$repo/$old" || -L "$repo/$old" || ! -f "$repo/$new" ||
        -L "$repo/$new" || "$(git -C "$repo" hash-object -- "$new")" != "$expected" ]]; then
    echo "provider test move/source identity failure: $old => $new" >&2
    exit 69
  fi
  if [[ -n "${witnessed[$new]:-}" ]]; then
    echo "duplicate provider source identity: $new" >&2
    exit 76
  fi
  witnessed[$new]=1
  checks=$((checks + 1))
done < "$manifest"
if [[ $checks -ne 54 ]]; then
  echo "expected 54 exact provider test/helper source identities, found $checks" >&2
  exit 70
fi
# These shared originals were already under providers/; they are not relocation rows.
shared_source_files=()
for binding in \
  'CollectionUtilsBulkFixtures.java:04d92bd1ba3016ec6466d887f75a52e83f7be025' \
  'CollectionUtilsBulkOracleTest.java:1cf0795874d000df952c1d1cb4ff8aae413cc78a' \
  'CollectionUtilsBulkReference.java:790b1d59392aeee2ac6ec40894f708ffa6241329'; do
  shared_name="${binding%%:*}"
  shared_expected="${binding#*:}"
  shared_source="$sources/$shared_name"
  if [[ ! -f "$shared_source" || -L "$shared_source" ||
        "$(git -C "$repo" hash-object -- "$shared_source")" != "$shared_expected" ]]; then
    echo "shared provider source identity failure: $shared_name" >&2
    exit 77
  fi
  shared_source_files+=("$shared_source")
done
for cls in jdk.m3.collections.apache.map.LinkedMap \
           jdk.m3.collections.eclipse.api.list.MutableList \
           jdk.m3.collections.guava.collect.ImmutableRangeSet; do
  "$jdk/bin/javap" --module jdk.m3.collections "$cls" >/dev/null || {
    echo "complete source-built module lacks $cls" >&2
    exit 71
  }
done
mkdir -p "$out/classes"
mapfile -d '' source_files < <(find "$sources"/apache "$sources"/eclipse \
  "$sources"/guava "$sources"/vavr -type f -name '*.java' -print0 | sort -z)
source_files+=("${shared_source_files[@]}")
if [[ ${#source_files[@]} -ne 57 ]]; then
  echo 'unexpected provider suite Java source count' >&2
  exit 72
fi
printf '%s\n' "${source_files[@]}" > "$out/java-sources.txt"
sha256sum "$jdk/lib/modules" "$jdk/jmods/jdk.m3.collections.jmod" "$junit" \
  "$manifest" "$0" "${source_files[@]}" > "$out/inputs-sha256.txt"
"$jdk/bin/javac" -source 21 -target 21 -proc:none -implicit:none \
  -Xlint:all,-module -Werror --add-modules jdk.m3.collections \
  --patch-module "jdk.m3.collections=$sources" \
  --add-reads jdk.m3.collections=ALL-UNNAMED -cp "$junit" \
  -d "$out/classes" @"$out/java-sources.txt" \
  2>&1 | tee "$out/compile.log"
mapfile -d '' cases < <(find "$sources"/apache "$sources"/eclipse \
  "$sources"/guava "$sources"/vavr -type f -name '*Test.java' -print0 | sort -z)
cases+=("$sources/CollectionUtilsBulkOracleTest.java")
if [[ ${#cases[@]} -ne 15 ]]; then
  echo 'expected exactly 15 JUnit test classes' >&2
  exit 73
fi
selects=()
selected_classes=()
opens=()
declare -A seen=()
for source in "${cases[@]}"; do
  pkg="$(sed -nE 's/^package ([[:alnum:]_.]+);/\1/p' "$source" | head -n 1)"
  [[ "$pkg" =~ ^jdk[.]m3[.]collections[.][A-Za-z0-9_.]+$ ]] || {
    echo "invalid provider package in $source" >&2
    exit 74
  }
  name="$(basename "$source" .java)"
  selects+=(--select-class "$pkg.$name")
  selected_classes+=("$pkg.$name")
  if [[ -z "${seen[$pkg]:-}" ]]; then
    opens+=("--add-opens=jdk.m3.collections/$pkg=ALL-UNNAMED")
    seen[$pkg]=1
  fi
done
printf '%s\n' "${cases[@]}" > "$out/test-classes.txt"
printf '%s\n' "${selected_classes[@]}" > "$out/selected-classes.txt"
runtime=(--add-modules jdk.m3.collections
  --patch-module "jdk.m3.collections=$out/classes"
  --add-reads jdk.m3.collections=ALL-UNNAMED
  "${opens[@]}")
if [[ -n "${M3_ARRAYS_NATIVE_PATH:-}" ]]; then
  [[ -s "$M3_ARRAYS_NATIVE_PATH" && ! -L "$M3_ARRAYS_NATIVE_PATH" ]] || exit 75
  runtime+=("-Dm3.arrays.native.path=$M3_ARRAYS_NATIVE_PATH")
  sha256sum "$M3_ARRAYS_NATIVE_PATH" >> "$out/inputs-sha256.txt"
fi
# Test classes belong to a JDK module loaded by the platform class loader.
# Merely adding a readability edge to ALL-UNNAMED does not let that loader
# resolve JUnit annotations from the application class loader. Put the exact
# SHA-256-pinned Console Standalone JAR on the bootstrap append search path.
# This corrects the observed 0-tests-discovered failure without moving or
# weakening the original module-patched JUnit tests.
mkdir -p "$out/validator"
sha256sum "$repo/make/m3/collections/ProviderJUnitReportsCheck.java" \
  >> "$out/inputs-sha256.txt"
"$jdk/bin/javac" --release 21 -proc:none -Xlint:all -Werror \
  -d "$out/validator" "$repo/make/m3/collections/ProviderJUnitReportsCheck.java"
"$jdk/bin/java" "-Xbootclasspath/a:$junit" -ea -Xmx2g "${runtime[@]}" -cp "$junit" \
  org.junit.platform.console.ConsoleLauncher execute \
  "${selects[@]}" --include-engine junit-jupiter \
  --disable-banner --details=summary --reports-dir "$out/junit-reports" \
  --fail-if-no-tests 2>&1 | tee "$out/provider-junit.log"
"$jdk/bin/java" -ea -cp "$out/validator" ProviderJUnitReportsCheck \
  "$out/selected-classes.txt" "$out/junit-reports/TEST-junit-jupiter.xml" \
  | tee "$out/report-verification.log"
printf 'M3_PROVIDER_JUNIT_PASS classes=15 sourceFiles=57\n' \
  | tee "$out/status.txt"
