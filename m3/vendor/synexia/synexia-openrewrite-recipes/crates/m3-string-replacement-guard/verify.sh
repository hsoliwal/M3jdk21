#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
set -euo pipefail
crate="$(cd "$(dirname "$0")" && pwd)"
module="$(cd "$crate/../.." && pwd)"
classes="$(mktemp -d)"
trap 'rm -rf "$classes"' EXIT
java_bin="${JAVA_HOME:+$JAVA_HOME/bin/}java"
javac_bin="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
"$javac_bin" --release 21 -Xlint:all -Werror -d "$classes"   "$module/src/main/java/com/synexia/rewrite/M3GpuToolJoinManifest.java"   "$module/src/main/java/com/synexia/rewrite/M3StringGuardRepair.java"   "$crate/src/test/java/M3StringGuardRepairTest.java"
"$java_bin" -cp "$classes" com.synexia.rewrite.M3StringGuardRepairTest
if [[ $# -eq 0 ]]; then
  exit 0
fi
if [[ $# -ne 2 || ( "$1" != --apply && "$1" != --check ) ]]; then
  printf '%s\n' 'usage: verify.sh [--apply|--check TARGET_ROOT]' >&2
  exit 2
fi
"$java_bin" -cp "$classes" com.synexia.rewrite.M3StringGuardRepair "$1" "$2" "$crate"
