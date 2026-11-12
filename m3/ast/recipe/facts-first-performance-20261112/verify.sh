#!/usr/bin/env sh
# SPDX-License-Identifier: Apache-2.0
set -eu
HERE=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
ROOT=$(CDPATH= cd -- "$HERE/../../../.." && pwd -P)
MAIN="$ROOT/m3/ast/src/main/java/jdk/m3/ast"
TEST="$ROOT/m3/ast/src/test/java/jdk/m3/ast"
OUT=$(mktemp -d)
trap 'rm -rf -- "$OUT"' EXIT HUP INT TERM
for spec in \
  "$MAIN/SyntaxIndex.java db14c75a439b3366517c2b344b41d8f4f7a390fc" \
  "$MAIN/SyntaxTree.java 05e399e63fb011630c7d38a9f7132f78526a1a6c" \
  "$TEST/FactsFirstPerformanceProof.java c995054c582991eefbbe995d9079292eb766cb97"; do
  set -- $spec
  got=$(git -C "$ROOT" hash-object "$1")
  if [ "$got" != "$2" ]; then
    echo "Unknown M3AST source: $1 expected=$2 actual=$got" >&2
    exit 3
  fi
done
javac --release 21 -proc:none -Xlint:all -Werror -d "$OUT" \
  "$MAIN/SyntaxTree.java" "$MAIN/SyntaxIndex.java" "$TEST/FactsFirstPerformanceProof.java"
java -Xms256m -Xmx512m -cp "$OUT" jdk.m3.ast.FactsFirstPerformanceProof
