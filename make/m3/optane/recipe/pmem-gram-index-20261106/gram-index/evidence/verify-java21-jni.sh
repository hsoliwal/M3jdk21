#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
crate="$(cd "$(dirname "$0")" && pwd)"
repo="${M3_REPO_ROOT:-$(cd "$crate/../../.." && pwd)}"
owner="$repo/m3-pmem-sdk/campaign/src/main/java/com/synexia/m3/pmem/campaign/PmemCorpusGramIndex.java"
junit="$repo/m3-pmem-sdk/campaign/src/test/java/com/synexia/m3/pmem/campaign/PmemCorpusGramIndexJUnitTest.java"
public="$repo/m3-pmem-sdk/campaign/src/main/java/com/synexia/m3/pmem/campaign/PmemCorpusSearch.java"
selected="$repo/m3-pmem-sdk/campaign/src/main/java/com/synexia/m3/pmem/campaign/PmemCorpusSelectedSearch.java"
templates="$crate/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-pmem-gram-index-20261102"
# Freeze originals and both exact generated postimages before the compiler.
printf '%s  %s\n' \
 3493a0f911023d8863d86cdf1b8f19c08d25d4aa5a76be8cfacb3bdc1bc64deb "$public" \
 a88eb92d8cf00411171d20648f90ce51c7b472f6604d29073bcd82d976f9628f "$selected" \
 523a4a541952c163caf6614583d92421a35193b186656ff33b5dedccba0b6174 "$owner" \
 29110712516b787265c19a2622a37edcf07265dccbb87287e834782b79e29cc9 "$junit" \
 523a4a541952c163caf6614583d92421a35193b186656ff33b5dedccba0b6174 "$templates/PmemCorpusGramIndex.java.txt" \
 29110712516b787265c19a2622a37edcf07265dccbb87287e834782b79e29cc9 "$templates/PmemCorpusGramIndexJUnitTest.java.txt" \
 | sha256sum --check --status
cmp "$owner" "$templates/PmemCorpusGramIndex.java.txt"
cmp "$junit" "$templates/PmemCorpusGramIndexJUnitTest.java.txt"
temp="$(mktemp -d)"
trap 'rm -rf "$temp"' EXIT
mkdir -p "$temp/classes" "$temp/native" "$temp/sdkclasses"
javac --release 21 -Xlint:all -Werror -d "$temp/classes" \
 "$public" "$selected" "$owner" \
 "$crate/proof/stubs/com/synexia/m3/pmem/PmemSearch.java" \
 "$crate/proof/stubs/com/synexia/m3/pmem/PmemSourceImage.java" \
 "$crate/proof/stubs/com/synexia/m3/pmem/NativeByteOracle.java" \
 "$crate/proof/java/com/synexia/m3/pmem/campaign/PmemGramIndexSmoke.java" \
 "$repo/synexia-openrewrite-recipes/recipe-crates/m3-pmem-selected-first-hit-20261102/proof/PmemSelectedSearchSmoke.java" \
 "$repo/synexia-openrewrite-recipes/recipe-crates/m3-pmem-selected-first-hit-20261102/proof/PmemCorpusSearchSmoke.java"
java_home="$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")"
cc -std=c17 -O2 -Wall -Wextra -Wpedantic -Werror -fPIC -shared \
 -I"$java_home/include" -I"$java_home/include/linux" \
 "$crate/proof/m3gramoracle.c" -o "$temp/native/libm3gramoracle.so"
for mode in jit interpreted; do
  mode_flags=()
  if [[ "$mode" == interpreted ]]; then mode_flags=(-Xint); fi
  for engine in java jni; do
    engine_flags=()
    if [[ "$engine" == jni ]]; then
      engine_flags=(-Xcheck:jni -Dm3.gram.oracle.native=true -Djava.library.path="$temp/native")
    fi
    java "${mode_flags[@]}" -Xmx128m "${engine_flags[@]}" -cp "$temp/classes" \
      com.synexia.m3.pmem.campaign.PmemGramIndexSmoke > "$temp/$mode-$engine.log"
  done
  cmp "$temp/$mode-java.log" "$temp/$mode-jni.log"
done
cmp "$temp/jit-java.log" "$temp/interpreted-java.log"
java -Xmx128m -cp "$temp/classes" \
 com.synexia.m3.pmem.campaign.PmemSelectedSearchSmoke > "$temp/selected.log"
java -Xmx128m -cp "$temp/classes" \
 com.synexia.m3.pmem.campaign.PmemCorpusSearchSmoke > "$temp/corpus.log"
# The next step is API-compatible fixture JUnit, NOT actual native PMEMSDK.
javac --release 21 -Xlint:all -Werror -d "$temp/sdkclasses" \
 "$owner" "$selected" "$public" "$junit" \
 $(find "$crate/proof/sdk_stubs" -name '*.java' | sort) \
 "$crate/proof/java/com/synexia/m3/pmem/campaign/GramIndexTestRunner.java"
java -Xmx256m -cp "$temp/sdkclasses" \
 com.synexia.m3.pmem.campaign.GramIndexTestRunner > "$temp/junit-shaped.log"
cat "$temp/jit-java.log" "$temp/selected.log" "$temp/corpus.log" "$temp/junit-shaped.log"
printf 'PASS source/template hash, untouched public/native owner, strict Java21, checked C17/JNI differential and JIT/-Xint identity\n'
