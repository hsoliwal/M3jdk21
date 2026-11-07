#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Standalone bounded host-JDK 21 verification (not source-built M3JDK).
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
classes="$(mktemp -d)"
trap 'rm -rf "$classes"' EXIT
# The first version compiled only the planner and its self-test. The planner now uses M3DonorTreeLanes
# (crate postimages v2-v4), so every source compiles and every self-test and proof main runs.
mapfile -t sources < <(find "$here/src" -name '*.java' -type f -print | sort)
javac --release 21 -Xlint:all -Werror -d "$classes" "${sources[@]}"
for test in M3DonorDeltaPlannerSelfTest M3DonorTreeLanesSelfTest M3DonorTreeLanesGrowthTest M3DonorSortedFastPathProof; do
  java -cp "$classes" "jdk.m3.tooling.donors.$test"
done
