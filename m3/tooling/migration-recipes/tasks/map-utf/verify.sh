#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Exact kernel proof only. Maven/OpenRewrite/JUnit/coverage/full product remain distinct gates.
set -euo pipefail
if test "$#" -ne 3; then echo "Usage: $0 JDK21 REPOSITORY FRESH_OUTPUT" >&2; exit 2; fi
jdk=$(realpath "$1"); repo=$(realpath "$2"); out=$(realpath -m "$3")
case "$out/" in "$repo/"*|"$jdk/"*) echo 'Output overlaps inputs' >&2; exit 2;; esac
test ! -e "$out" && test ! -L "$out"
(cd "$repo" && sha256sum --check m3/tooling/migration-recipes/tasks/map-utf/inputs.sha256)
bash "$repo/m3/tooling/migration-recipes/tasks/map-facts/verify.sh" "$jdk" "$repo" "$out"
"$jdk/bin/javac" -source 21 -target 21 -Xlint:all -Werror \
  --patch-module "java.base=$out/classes" --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED \
  -cp "$out/tests" -d "$out/tests" "$repo/test/jdk/java/lang/String/MapUtfTest.java" > "$out/utf-compile.log" 2>&1
common=(-Xmx64m --patch-module "java.base=$out/classes" --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED -cp "$out/tests")
for mode in default interpreter noncompact c2; do
  flags=()
  case "$mode" in interpreter) flags=(-Xint);; noncompact) flags=(-XX:-CompactStrings);; c2) flags=(-XX:-TieredCompilation -Xbatch);; esac
  "$jdk/bin/java" "${flags[@]}" "${common[@]}" MapUtfTest > "$out/utf-$mode.log" 2>&1
  cat "$out/utf-$mode.log"
done
"$jdk/bin/java" -XX:-TieredCompilation -Xbatch "${common[@]}" MapFactsTest > "$out/facts-c2.log" 2>&1
(cd "$repo" && sha256sum --check m3/tooling/migration-recipes/tasks/map-utf/inputs.sha256)
echo 'SCOPED_MAP_UTF_PASS; not full JDK or framework acceptance'
