#!/usr/bin/env sh
# SPDX-License-Identifier: Apache-2.0
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../../../../" && pwd -P)
AST="$ROOT/m3/ast"
MAIN="$AST/src/main/java/jdk/m3/ast"
TEST="$AST/recipe/facts-first-performance-20261112/src/test/java/jdk/m3/ast"
EXPECTED=db14c75a439b3366517c2b344b41d8f4f7a390fc
ACTUAL=$(git -C "$ROOT" hash-object "$MAIN/SyntaxIndex.java")
if [ "$ACTUAL" != "$EXPECTED" ]; then
  echo "Refusing unqualified SyntaxIndex source: $ACTUAL" >&2
  exit 4
fi
OUT=$(mktemp -d)
trap 'rm -rf -- "$OUT"' EXIT HUP INT TERM
javac --release 21 -proc:none -Xlint:all -Werror -d "$OUT" \
  "$MAIN/SyntaxTree.java" "$MAIN/SyntaxIndex.java" \
  "$TEST/FactsFirstPerformanceAdmission.java"
java -Xms128m -Xmx512m -cp "$OUT" \
  jdk.m3.ast.FactsFirstPerformanceAdmission "$MAIN/SyntaxIndex.java"
