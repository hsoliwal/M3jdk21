#!/bin/bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Dependency-free Java 21 proof for the M3 atom coverage contract.
set -euo pipefail
here=$(cd "$(dirname "$0")" && pwd)
classes=$(mktemp -d)
trap 'rm -rf "$classes"' EXIT
mapfile -t sources < <({
  find "$here/src/main/java/jdk/m3/tooling/atoms/contract" -name '*.java' -print
  find "$here/src/test/java/jdk/m3/tooling/atoms/contract" -name '*SelfTest.java' -print
} | sort)
javac --release 21 -Xlint:all -Werror -d "$classes" "${sources[@]}"
java -Dm3.contract.dir="$here/contract" -cp "$classes" \
  jdk.m3.tooling.atoms.contract.M3AtomCoverageVerifierSelfTest
