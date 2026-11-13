#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Dependency-free Java 21 proof for the companion admission planner (census family): strict compile, then every main proof.
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
classes="$(mktemp -d)"
trap 'rm -rf "$classes"' EXIT
p="$here/src/main/java/jdk/m3/tooling/atoms"; t="$here/src/test/java/jdk/m3/tooling/atoms"
javac --release 21 -Xlint:all -Werror -d "$classes" "$p/ClassfileSeal.java" "$p/CompanionPlan.java" \
  "$p/SystemApiCensus.java" "$t/ClassfileSealTest.java" "$t/CompanionPlanTest.java" \
  "$t/SystemApiCensusTest.java" "$t/SystemApiReviewTest.java"
for test in CompanionPlanTest ClassfileSealTest SystemApiCensusTest SystemApiReviewTest; do
  java -cp "$classes" "jdk.m3.tooling.atoms.$test"
done
