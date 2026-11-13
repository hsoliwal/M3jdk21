#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
crate="$(cd "$(dirname "$0")" && pwd)"
repo="$(cd "$crate/../../.." && pwd)"
main="$repo/m3-pmem-sdk/campaign/src/main/java/com/synexia/m3/pmem/campaign"
tests="$repo/m3-pmem-sdk/campaign/src/test/java/com/synexia/m3/pmem/campaign"
resources="$crate/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-pmem-three-gram-20261105"
stubs="$repo/synexia-openrewrite-recipes/recipe-crates/m3-pmem-gram-index-20261102/proof/stubs/com/synexia/m3/pmem"
check() { printf '%s  %s\n' "$1" "$2" | sha256sum -c -; }
check 85d83f286e49e69e3ada9e8f4e0f772e66f214964c7d332ef2107e20dcfbd5fd "$resources/pre/PmemCorpusGramIndex.java.txt"
check ac7afd7cad94cc2c7c7a3fb63ac67216a879705518e370707ac7ff2a995adac9 "$resources/pre/PmemCorpusGramIndexJUnitTest.java.txt"
check 86c79682e7c373034a5f086c7241602b2203459ca188b32a02f60e42321780b5 "$resources/PmemCorpusGramIndex.java.txt"
check 6ef88217cba20945cdab10fac70d60e55abc398587b786c1af2b288a24e3cb62 "$resources/PmemCorpusGramIndexJUnitTest.java.txt"
check 86c79682e7c373034a5f086c7241602b2203459ca188b32a02f60e42321780b5 "$main/PmemCorpusGramIndex.java"
check 6ef88217cba20945cdab10fac70d60e55abc398587b786c1af2b288a24e3cb62 "$tests/PmemCorpusGramIndexJUnitTest.java"
cmp "$main/PmemCorpusGramIndex.java" "$resources/PmemCorpusGramIndex.java.txt"
cmp "$tests/PmemCorpusGramIndexJUnitTest.java" "$resources/PmemCorpusGramIndexJUnitTest.java.txt"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
mkdir -p "$tmp/original-source" "$tmp/before" "$tmp/after"
cp "$resources/pre/PmemCorpusGramIndex.java.txt" "$tmp/original-source/PmemCorpusGramIndex.java"
for state in before after; do
  if [[ "$state" == before ]]; then
    owner="$tmp/original-source/PmemCorpusGramIndex.java"
    flag=false
  else
    owner="$main/PmemCorpusGramIndex.java"
    flag=true
  fi
  javac --release 21 -Xlint:all -Werror -d "$tmp/$state" \
    "$stubs/PmemSearch.java" "$stubs/PmemSourceImage.java" \
    "$main/PmemCorpusSearch.java" "$main/PmemCorpusSelectedSearch.java" \
    "$owner" "$crate/proof/ThreeGramPublicationProof.java"
  java -Xmx128m -Dtriple="$flag" -cp "$tmp/$state" \
    com.synexia.m3.pmem.campaign.ThreeGramPublicationProof > "$tmp/$state.jit"
  java -Xint -Xmx128m -Dtriple="$flag" -cp "$tmp/$state" \
    com.synexia.m3.pmem.campaign.ThreeGramPublicationProof > "$tmp/$state.int"
  cmp "$tmp/$state.jit" "$tmp/$state.int"
  javap -classpath "$tmp/$state" -p -s \
    com.synexia.m3.pmem.campaign.PmemCorpusGramIndex > "$tmp/$state.api"
  cat "$tmp/$state.jit"
done
cmp "$tmp/before.api" "$tmp/after.api"
diff -u <(grep '^SEMANTIC_SHA256=' "$tmp/before.jit") \
        <(grep '^SEMANTIC_SHA256=' "$tmp/after.jit")
sdk_stubs="$repo/synexia-openrewrite-recipes/recipe-crates/m3-pmem-gram-index-20261102/proof/sdk_stubs"
mkdir -p "$tmp/junit"
javac --release 21 -Xlint:all -Werror -d "$tmp/junit" \
  "$main/PmemCorpusSearch.java" "$main/PmemCorpusSelectedSearch.java" \
  "$main/PmemCorpusGramIndex.java" "$tests/PmemCorpusGramIndexJUnitTest.java" \
  $(find "$sdk_stubs" -name '*.java' | sort) \
  "$crate/proof/ThreeGramJUnitFixtureRunner.java"
java -Xmx256m -cp "$tmp/junit" com.synexia.m3.pmem.campaign.ThreeGramJUnitFixtureRunner
printf 'PASS exact Java21 scalar oracle, unchanged method descriptors, source/JUnit templates and FILE-shaped fixture\n'
