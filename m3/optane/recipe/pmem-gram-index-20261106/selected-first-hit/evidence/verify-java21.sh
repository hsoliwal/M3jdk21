#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
crate="$(cd "$(dirname "$0")" && pwd)"
repo="${M3_REPO_ROOT:-$(cd "$crate/../../.." && pwd)}"
root="$repo/m3-pmem-sdk/campaign"
public="$root/src/main/java/com/synexia/m3/pmem/campaign/PmemCorpusSearch.java"
owner="$root/src/main/java/com/synexia/m3/pmem/campaign/PmemCorpusSelectedSearch.java"
unit="$root/src/test/java/com/synexia/m3/pmem/campaign/PmemCorpusSelectedJUnitTest.java"
resources="$crate/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-pmem-selected-first-hit-20261102"
# Frozen original incumbent ABI and exact recipe outputs; no public API edits.
printf '%s  %s\n' \
 '3493a0f911023d8863d86cdf1b8f19c08d25d4aa5a76be8cfacb3bdc1bc64deb' "$public" \
 'a88eb92d8cf00411171d20648f90ce51c7b472f6604d29073bcd82d976f9628f' "$owner" \
 '5cec3d16e533b6630598b35dfa61c0ce74ec43f1083b6d6c492fe3ca7696ebb3' "$unit" \
 'a88eb92d8cf00411171d20648f90ce51c7b472f6604d29073bcd82d976f9628f' "$resources/PmemCorpusSelectedSearch.java.txt" \
 '5cec3d16e533b6630598b35dfa61c0ce74ec43f1083b6d6c492fe3ca7696ebb3' "$resources/PmemCorpusSelectedJUnitTest.java.txt" \
 | sha256sum --check --status
cmp "$owner" "$resources/PmemCorpusSelectedSearch.java.txt"
cmp "$unit" "$resources/PmemCorpusSelectedJUnitTest.java.txt"
temp="$(mktemp -d)"
trap 'rm -rf "$temp"' EXIT
mkdir -p "$temp/classes"
# This restricted fixture deliberately uses interface-shaped stub owners. Native
# PMEMSDK and real Maven/OpenRewrite JUnit run via the focused CI gate, separately.
javac --release 21 -Xlint:all -Werror -d "$temp/classes" \
 "$public" "$owner" \
 "$crate/proof/stubs/com/synexia/m3/pmem/PmemSearch.java" \
 "$crate/proof/stubs/com/synexia/m3/pmem/PmemSourceImage.java" \
 "$crate/proof/PmemSelectedSearchSmoke.java" \
 "$crate/proof/PmemCorpusSearchSmoke.java"
java -Xmx64m -cp "$temp/classes" com.synexia.m3.pmem.campaign.PmemSelectedSearchSmoke > "$temp/selected-jit.txt"
java -Xint -Xmx64m -cp "$temp/classes" com.synexia.m3.pmem.campaign.PmemSelectedSearchSmoke > "$temp/selected-int.txt"
cmp "$temp/selected-jit.txt" "$temp/selected-int.txt"
java -Xmx64m -cp "$temp/classes" com.synexia.m3.pmem.campaign.PmemCorpusSearchSmoke > "$temp/legacy-jit.txt"
java -Xint -Xmx64m -cp "$temp/classes" com.synexia.m3.pmem.campaign.PmemCorpusSearchSmoke > "$temp/legacy-int.txt"
cmp "$temp/legacy-jit.txt" "$temp/legacy-int.txt"
cat "$temp/selected-jit.txt" "$temp/legacy-jit.txt"
printf 'PASS source hashes, original public API sealed, strict Java21, JIT/-Xint equality\n'
