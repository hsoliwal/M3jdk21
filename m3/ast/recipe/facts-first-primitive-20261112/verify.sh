#!/usr/bin/env sh
# SPDX-License-Identifier: Apache-2.0
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../../../../" && pwd -P)
AST="$ROOT/m3/ast"
OUT=$(mktemp -d)
trap 'rm -rf -- "$OUT"' EXIT HUP INT TERM
MAIN="$AST/src/main/java/jdk/m3/ast"
TEST="$AST/src/test/java/jdk/m3/ast"
EXPECTED=db14c75a439b3366517c2b344b41d8f4f7a390fc
ACTUAL=$(git -C "$ROOT" hash-object "$MAIN/SyntaxIndex.java")
if [ "$ACTUAL" != "$EXPECTED" ]; then
  echo "Refusing unknown SyntaxIndex postimage: $ACTUAL" >&2
  exit 4
fi
javac --release 21 -proc:none -Xlint:all -Werror -d "$OUT" \
  "$MAIN/SyntaxTree.java" "$MAIN/SyntaxIndex.java" \
  "$TEST/SyntaxIndexContract.java" \
  "$TEST/SyntaxIndexFactsFirstContract.java" "$TEST/ProbeGuardContract.java"
java -cp "$OUT" jdk.m3.ast.SyntaxIndexContract
java -cp "$OUT" jdk.m3.ast.SyntaxIndexFactsFirstContract
java -cp "$OUT" jdk.m3.ast.ProbeGuardContract
