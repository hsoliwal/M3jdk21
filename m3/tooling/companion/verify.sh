#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Dependency-free Java 21 proof for the companion admission planner: strict compile, then every main proof.
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
classes="$(mktemp -d)"
trap 'rm -rf "$classes"' EXIT
mapfile -t sources < <(find "$here/src" -name '*.java' -type f -print | LC_ALL=C sort)
javac --release 21 -Xlint:all -Werror -d "$classes" "${sources[@]}"
for test in M3CompanionPlanTest M3ClassfileSealTest M3SystemApiCensusTest M3SystemApiReviewTest; do
  java -cp "$classes" "jdk.m3.tooling.companion.$test"
done
