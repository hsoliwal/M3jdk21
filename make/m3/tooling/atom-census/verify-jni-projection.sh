#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
SRC="$HERE/src/main/java/jdk/m3/tooling/atoms"
TEST="$HERE/src/test/java/jdk/m3/tooling/atoms"
JDK="${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")}" 
mkdir -p "$WORK/classes"
javac --release 21 -encoding UTF-8 -Xlint:all -Werror -d "$WORK/classes" \
  "$SRC/contract/AtomKind.java" "$SRC/JniDeclarationProjection.java" \
  "$SRC/AtomCensus.java" "$TEST/JniDeclarationProjectionSelfTest.java"
java -cp "$WORK/classes" jdk.m3.tooling.atoms.JniDeclarationProjectionSelfTest
