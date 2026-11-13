#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
crate="$(cd "$(dirname "$0")" && pwd)"
repo="${M3_REPO_ROOT:-$(cd "$crate/../../.." && pwd)}"
legacy="$repo/synexia-openrewrite-recipes/recipe-crates/m3-pmem-gram-index-20261102"
resources="$crate/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-pmem-gram-conjunction-20261105"
owner="$repo/m3-pmem-sdk/campaign/src/main/java/com/synexia/m3/pmem/campaign/PmemCorpusGramIndex.java"
test="$repo/m3-pmem-sdk/campaign/src/test/java/com/synexia/m3/pmem/campaign/PmemCorpusGramIndexJUnitTest.java"
selected="$repo/m3-pmem-sdk/campaign/src/main/java/com/synexia/m3/pmem/campaign/PmemCorpusSelectedSearch.java"
original="$repo/m3-pmem-sdk/campaign/src/main/java/com/synexia/m3/pmem/campaign/PmemCorpusSearch.java"
source_pre=84cc411d7e6b4c9867517bd675fd1693f3e5cac1c7300567460cf408949c086e
source_post=85d83f286e49e69e3ada9e8f4e0f772e66f214964c7d332ef2107e20dcfbd5fd
test_pre=0e45bdf9f09712ea4acd7926242e80fed86da9f6bceecf9fd246225b92f05c57
test_post=ac7afd7cad94cc2c7c7a3fb63ac67216a879705518e370707ac7ff2a995adac9
printf '%s  %s\n' \
 "$source_pre" "$resources/pre/PmemCorpusGramIndex.java.txt" \
 "$source_post" "$resources/PmemCorpusGramIndex.java.txt" \
 "$test_pre" "$resources/pre/PmemCorpusGramIndexJUnitTest.java.txt" \
 "$test_post" "$resources/PmemCorpusGramIndexJUnitTest.java.txt" \
 "$source_post" "$owner" "$test_post" "$test" \
 'a88eb92d8cf00411171d20648f90ce51c7b472f6604d29073bcd82d976f9628f' "$selected" \
 | sha256sum -c -
if [[ "${M3_LIMITED_STANDIN_PROOF:-0}" != 1 ]]; then
  printf '%s  %s\n' '3493a0f911023d8863d86cdf1b8f19c08d25d4aa5a76be8cfacb3bdc1bc64deb' "$original" | sha256sum -c -
else
  echo 'LIMITED: skipped original public corpus source root check (stand-in proof only)'
fi
cmp "$owner" "$resources/PmemCorpusGramIndex.java.txt"
cmp "$test" "$resources/PmemCorpusGramIndexJUnitTest.java.txt"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
mkdir -p "$work/before" "$work/after" "$work/source/before" "$work/source/after" "$work/source/stub" "$work/junit"
cp "$resources/pre/PmemCorpusGramIndex.java.txt" "$work/source/before/PmemCorpusGramIndex.java"
cp "$resources/PmemCorpusGramIndex.java.txt" "$work/source/after/PmemCorpusGramIndex.java"
cp "$crate/proof/PmemCorpusSearchApiStub.java.txt" "$work/source/stub/PmemCorpusSearch.java"
for phase in before after; do
  javac --release 21 -Xlint:all -Werror -d "$work/$phase" \
   "$legacy/proof/stubs/com/synexia/m3/pmem/PmemSearch.java" \
   "$legacy/proof/stubs/com/synexia/m3/pmem/PmemSourceImage.java" \
   "$selected" "$work/source/stub/PmemCorpusSearch.java" \
   "$work/source/$phase/PmemCorpusGramIndex.java" \
   "$legacy/proof/PmemGramIndexSmoke.java" "$crate/proof/IntersectionProof.java"
  javap -p -classpath "$work/$phase" com.synexia.m3.pmem.campaign.PmemCorpusGramIndex > "$work/$phase.javap"
  java -Xmx128m -cp "$work/$phase" com.synexia.m3.pmem.campaign.PmemGramIndexSmoke > "$work/$phase-legacy-jit.log"
  # Inherited heavy corpus suite runs JIT. The bounded independent before/after
  # oracle below is qualified under JIT AND interpreted JVM.
  optimized=false; [[ "$phase" == after ]] && optimized=true
  java -Doptimized="$optimized" -Xmx128m -cp "$work/$phase" com.synexia.m3.pmem.campaign.IntersectionProof > "$work/$phase-ref-jit.log"
  java -Doptimized="$optimized" -Xint -Xmx128m -cp "$work/$phase" com.synexia.m3.pmem.campaign.IntersectionProof > "$work/$phase-ref-int.log"
  cmp "$work/$phase-ref-jit.log" "$work/$phase-ref-int.log"
  cat "$work/$phase-legacy-jit.log" "$work/$phase-ref-jit.log"
done
cmp "$work/before.javap" "$work/after.javap"
cmp <(grep '^SEMANTIC_SHA256=' "$work/before-ref-jit.log") <(grep '^SEMANTIC_SHA256=' "$work/after-ref-jit.log")
# Restricted JUnit-shaped proof with API-compatible stubs, not real JUnit/JNI.
javac --release 21 -Xlint:all -Werror -d "$work/junit" \
 "$selected" "$owner" "$work/source/stub/PmemCorpusSearch.java" "$test" \
 $(find "$legacy/proof/sdk_stubs" -name '*.java' | sort) \
 "$legacy/proof/GramJUnitFixtureRunner.java"
java -Xmx256m -cp "$work/junit" com.synexia.m3.pmem.campaign.GramJUnitFixtureRunner
if [[ "${M3_LIMITED_STANDIN_PROOF:-0}" == 1 ]]; then
  echo 'PASS LIMITED Java21 differential/source seal/6 stand-in methods; real Maven/JNI NOT RUN'
else
  echo 'PASS Java21 frozen-source differential and 6 stand-in JUnit methods; Maven/JNI NOT RUN'
fi
