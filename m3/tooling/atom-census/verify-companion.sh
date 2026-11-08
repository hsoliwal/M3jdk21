#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Dependency-free Java 21 proof for the companion admission planner (census family): strict compile, then every main proof.
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
classes="$(mktemp -d)"
trap 'rm -rf "$classes"' EXIT
p="$here/src/main/java/jdk/m3/tooling/atoms"; t="$here/src/test/java/jdk/m3/tooling/atoms"
javac --release 21 -Xlint:all -Werror -d "$classes" "$p/M3ClassfileSeal.java" "$p/M3CompanionPlan.java" \
  "$p/M3SystemApiCensus.java" "$t/M3ClassfileSealTest.java" "$t/M3CompanionPlanTest.java" \
  "$t/M3SystemApiCensusTest.java" "$t/M3SystemApiReviewTest.java"
for test in M3CompanionPlanTest M3ClassfileSealTest M3SystemApiCensusTest M3SystemApiReviewTest; do
  java -cp "$classes" "jdk.m3.tooling.atoms.$test"
done
