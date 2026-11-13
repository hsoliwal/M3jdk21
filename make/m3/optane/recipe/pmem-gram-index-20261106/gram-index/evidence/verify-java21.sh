#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
crate="$(cd "$(dirname "$0")" && pwd)"
repo="${M3_REPO_ROOT:-$(cd "$crate/../../.." && pwd)}"
owner="$repo/m3-pmem-sdk/campaign/src/main/java/com/synexia/m3/pmem/campaign/PmemCorpusGramIndex.java"
test="$repo/m3-pmem-sdk/campaign/src/test/java/com/synexia/m3/pmem/campaign/PmemCorpusGramIndexJUnitTest.java"
original="$repo/m3-pmem-sdk/campaign/src/main/java/com/synexia/m3/pmem/campaign/PmemCorpusSearch.java"
selected="$repo/m3-pmem-sdk/campaign/src/main/java/com/synexia/m3/pmem/campaign/PmemCorpusSelectedSearch.java"
resources="$crate/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-pmem-gram-index-20261102"
printf '%s  %s\n' \
 '3493a0f911023d8863d86cdf1b8f19c08d25d4aa5a76be8cfacb3bdc1bc64deb' "$original" \
 'a88eb92d8cf00411171d20648f90ce51c7b472f6604d29073bcd82d976f9628f' "$selected" \
 '84cc411d7e6b4c9867517bd675fd1693f3e5cac1c7300567460cf408949c086e' "$owner" \
 '0e45bdf9f09712ea4acd7926242e80fed86da9f6bceecf9fd246225b92f05c57' "$test" \
 '84cc411d7e6b4c9867517bd675fd1693f3e5cac1c7300567460cf408949c086e' "$resources/PmemCorpusGramIndex.java.txt" \
 '0e45bdf9f09712ea4acd7926242e80fed86da9f6bceecf9fd246225b92f05c57' "$resources/PmemCorpusGramIndexJUnitTest.java.txt" \
 | sha256sum --check --status
cmp "$owner" "$resources/PmemCorpusGramIndex.java.txt"
cmp "$test" "$resources/PmemCorpusGramIndexJUnitTest.java.txt"
temp="$(mktemp -d)"
trap 'rm -rf "$temp"' EXIT
mkdir -p "$temp/classes" "$temp/junit"
javac --release 21 -Xlint:all -Werror -d "$temp/classes" \
 "$original" "$selected" "$owner" \
 "$crate/proof/stubs/com/synexia/m3/pmem/PmemSearch.java" \
 "$crate/proof/stubs/com/synexia/m3/pmem/PmemSourceImage.java" \
 "$crate/proof/PmemGramIndexSmoke.java"
java -Xmx128m -cp "$temp/classes" com.synexia.m3.pmem.campaign.PmemGramIndexSmoke > "$temp/jit.txt"
java -Xint -Xmx128m -cp "$temp/classes" com.synexia.m3.pmem.campaign.PmemGramIndexSmoke > "$temp/interpreted.txt"
cmp "$temp/jit.txt" "$temp/interpreted.txt"
cat "$temp/jit.txt"
# A restricted JUnit-shaped proof uses original-source API stand-ins. It is
# explicitly NOT proof of the actual FILE/JNI/Maven/JUnit product engines.
javac --release 21 -Xlint:all -Werror -d "$temp/junit" \
 "$original" "$selected" "$owner" "$test" \
 $(find "$crate/proof/sdk_stubs" -name '*.java' | sort) \
 "$crate/proof/GramJUnitFixtureRunner.java"
java -Xmx256m -cp "$temp/junit" com.synexia.m3.pmem.campaign.GramJUnitFixtureRunner
printf 'PASS exact Java21 code/test/template seals, original owners unchanged, JIT/-Xint parity\n'
