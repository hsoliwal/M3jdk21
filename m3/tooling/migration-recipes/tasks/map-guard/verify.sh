#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Scoped mapped-backing proof, NOT a full JDK build or jtreg-harness invocation.
set -euo pipefail
if test "$#" -ne 3; then
  printf 'Usage: %s JDK21 REPOSITORY FRESH_OUTPUT\n' "$0" >&2
  exit 2
fi
jdk=$(realpath "$1")
repo=$(realpath "$2")
out=$(realpath -m "$3")
case "$out/" in "$repo/"*|"$jdk/"*) echo 'Output must be outside repository and JDK' >&2; exit 2;; esac
test ! -e "$out" && test ! -L "$out"
grep -Eq '^JAVA_VERSION="21([.+-]|")' "$jdk/release"
mkdir -p "$out/src/jdk/internal/mindex" "$out/classes" "$out/tests"
src="$repo/src/java.base/share/classes/jdk/internal/mindex"
tests="$repo/test/jdk/java/lang/String"
# Stage ONLY the two layout-independent owners. Do not let javac discover
# modified String.java/HotSpot-dependent sources from the complete source path.
cp "$src/MIndexStringBacking.java" "$src/MIndexMappedStringBacking.java" "$out/src/jdk/internal/mindex/"
sha256sum "$src/MIndexStringBacking.java" "$src/MIndexMappedStringBacking.java" \
  "$tests/MIndexMappedStringBackingTest.java" "$tests/MapGuardTest.java" > "$out/inputs.sha256"
"$jdk/bin/javac" --release 21 -Xlint:all -Werror \
  --patch-module "java.base=$out/src" -d "$out/classes" \
  "$out/src/jdk/internal/mindex/"*.java > "$out/compile.log" 2>&1
"$jdk/bin/javac" -source 21 -target 21 -Xlint:all -Werror \
  --patch-module "java.base=$out/classes" \
  --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED -d "$out/tests" \
  "$tests/MIndexMappedStringBackingTest.java" "$tests/MapGuardTest.java" > "$out/test-compile.log" 2>&1
common=(-Xmx32m --patch-module "java.base=$out/classes"
  --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED
  --add-opens java.base/jdk.internal.mindex=ALL-UNNAMED -cp "$out/tests")
"$jdk/bin/java" "${common[@]}" MapGuardTest > "$out/runtime.log" 2>&1
"$jdk/bin/java" -Xint "${common[@]}" MapGuardTest > "$out/interpreter.log" 2>&1
sha256sum --check "$out/inputs.sha256"
cat "$out/runtime.log" "$out/interpreter.log"
printf 'SCOPED_BACKING_PROOF_PASS; full-JDK/Maven/jtreg/coverage not implied\n'
