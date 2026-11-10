#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Reuse the preceding actual kernel gate. This is not Maven/JUnit/jtreg or a product image.
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
task=m3/tooling/migration-recipes/tasks/map-facts
(cd "$repo" && sha256sum --check "$task/inputs.sha256")
bash "$repo/m3/tooling/migration-recipes/tasks/map-guard/verify.sh" "$jdk" "$repo" "$out"
tests="$repo/test/jdk/java/lang/String"
"$jdk/bin/javac" -source 21 -target 21 -Xlint:all -Werror \
  --patch-module "java.base=$out/classes" \
  --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED \
  -cp "$out/tests" -d "$out/tests" "$tests/MapFactsTest.java" > "$out/facts-compile.log" 2>&1
common=(-Xmx64m --patch-module "java.base=$out/classes"
  --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED -cp "$out/tests")
"$jdk/bin/java" "${common[@]}" MapFactsTest > "$out/facts-runtime.log" 2>&1
"$jdk/bin/java" -Xint "${common[@]}" MapFactsTest > "$out/facts-interpreter.log" 2>&1
"$jdk/bin/java" -XX:-CompactStrings "${common[@]}" MapFactsTest > "$out/facts-no-compact.log" 2>&1
cmp "$out/facts-runtime.log" "$out/facts-interpreter.log"
cmp "$out/facts-runtime.log" "$out/facts-no-compact.log"
(cd "$repo" && sha256sum --check "$task/inputs.sha256")
cat "$out/facts-runtime.log" "$out/facts-interpreter.log" "$out/facts-no-compact.log"
printf 'SCOPED_MAP_FACTS_PASS; Maven/JUnit/coverage/full-JDK gates remain separate\n'
