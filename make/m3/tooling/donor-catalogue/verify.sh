#!/bin/bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Dependency-free Java 21 verification for the M3 donor catalogue.
set -euo pipefail
here=$(cd "$(dirname "$0")" && pwd)
repo=$(cd "$here/../../.." && pwd)
out=${M3_DONOR_BUILD_DIR:-"$repo/build/m3-donor-catalogue"}
classes=$(mktemp -d)
first=$(mktemp)
second=$(mktemp)
javadoc_out=$(mktemp -d)
trap 'rm -rf "$classes" "$first" "$second" "$javadoc_out"' EXIT

mapfile -t main_sources < <(find "$here/src/main/java" -name '*.java' -print | sort)
mapfile -t test_sources < <(find "$here/src/test/java" -name '*.java' -print | sort)

rm -rf "$out"
mkdir -p "$out"

javac --release 21 -Xlint:all -Werror -d "$classes" "${main_sources[@]}" "${test_sources[@]}"
javadoc -quiet -Werror -Xdoclint:all -d "$javadoc_out" "${main_sources[@]}" >"$out/javadoc.log" 2>&1
java -Dm3.catalogue.dir="$here" -cp "$classes" \
  jdk.m3.tooling.donors.DonorCatalogueSelfTest | tee "$out/self-test.log"
java -cp "$classes" jdk.m3.tooling.donors.DonorCatalogue validate "$here" \
  | tee "$out/catalogue-validation.log"
java -cp "$classes" jdk.m3.tooling.donors.DonorCatalogue plan "$here" "$first" \
  >"$out/plan-first.log"
java -cp "$classes" jdk.m3.tooling.donors.DonorCatalogue plan "$here" "$second" \
  >"$out/plan-second.log"
cmp "$first" "$second"
# Donor-delta planner and tree lanes (folded in from make/m3/tooling/donor-delta, 2026-11-08): self-tests and proof.
for test in DonorDeltaPlannerSelfTest DonorTreeLanesSelfTest DonorTreeLanesGrowthTest DonorSortedFastPathProof; do
  java -cp "$classes" "jdk.m3.tooling.donors.$test" | tee "$out/$test.log"
done
cp "$first" "$out/DONOR_PLAN.tsv"

{
  printf 'key\tvalue\n'
  printf 'catalogue_validation_sha256\t%s\n' "$(sha256sum "$out/catalogue-validation.log" | cut -d' ' -f1)"
  printf 'donor_plan_sha256\t%s\n' "$(sha256sum "$out/DONOR_PLAN.tsv" | cut -d' ' -f1)"
  printf 'javadoc_sha256\t%s\n' "$(sha256sum "$out/javadoc.log" | cut -d' ' -f1)"
  printf 'self_test_sha256\t%s\n' "$(sha256sum "$out/self-test.log" | cut -d' ' -f1)"
} >"$out/VERIFICATION_RECEIPT.tsv"

printf 'M3_DONOR_VERIFY\tresult=PASS\toutput=%s\n' "$out"
