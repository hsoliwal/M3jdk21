#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
crate="$(cd "$(dirname "$0")" && pwd)"
repo="${M3_REPO_ROOT:-$(cd "$crate/../../.." && pwd)}"
resources="$crate/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-pmem-gram-campaign-20261105"
owner="$repo/m3-pmem-sdk/campaign/src/main/java/com/synexia/m3/pmem/campaign/PmemGramCampaignAdmission.java"
unit="$repo/m3-pmem-sdk/campaign/src/test/java/com/synexia/m3/pmem/campaign/PmemGramCampaignAdmissionJUnitTest.java"
gram="$repo/m3-pmem-sdk/campaign/src/main/java/com/synexia/m3/pmem/campaign/PmemCorpusGramIndex.java"
selected="$repo/m3-pmem-sdk/campaign/src/main/java/com/synexia/m3/pmem/campaign/PmemCorpusSelectedSearch.java"
# Entire Java source and original-owner identity freezes, no unreviewed override.
printf '%s  %s\n' \
 '37ac0361c9dd49b4e29ed48464307aa32bee608b35b264987e896bf74e5321a9' "$owner" \
 'dc6b0274cf0bc0fce2b7f92da02a466c0a8df0ed1c835fd89813c2949ef9ed04' "$unit" \
 '37ac0361c9dd49b4e29ed48464307aa32bee608b35b264987e896bf74e5321a9' "$resources/PmemGramCampaignAdmission.java.txt" \
 'dc6b0274cf0bc0fce2b7f92da02a466c0a8df0ed1c835fd89813c2949ef9ed04' "$resources/PmemGramCampaignAdmissionJUnitTest.java.txt" \
 '85d83f286e49e69e3ada9e8f4e0f772e66f214964c7d332ef2107e20dcfbd5fd' "$gram" \
 'a88eb92d8cf00411171d20648f90ce51c7b472f6604d29073bcd82d976f9628f' "$selected" \
 | sha256sum -c -
cmp "$owner" "$resources/PmemGramCampaignAdmission.java.txt"
cmp "$unit" "$resources/PmemGramCampaignAdmissionJUnitTest.java.txt"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
mkdir -p "$tmp/main" "$tmp/tests"
javac --release 21 -Xlint:all -Werror -d "$tmp/main" \
 "$owner" "$gram" "$selected" \
 $(find "$crate/proof/stubs" -name '*.java' | sort) \
 "$crate/proof/PmemGramCampaignAdmissionSmoke.java"
java -Xmx128m -cp "$tmp/main" com.synexia.m3.pmem.campaign.PmemGramCampaignAdmissionSmoke > "$tmp/jit.log"
java -Xint -Xmx128m -cp "$tmp/main" com.synexia.m3.pmem.campaign.PmemGramCampaignAdmissionSmoke > "$tmp/int.log"
cmp "$tmp/jit.log" "$tmp/int.log"
cat "$tmp/jit.log"
# API-compatible stand-in test only; real original FILE/POSIX/JNI/JUnit run separately.
javac --release 21 -Xlint:all -Werror -d "$tmp/tests" \
 "$owner" "$unit" "$gram" "$selected" \
 $(find "$crate/proof/sdk_stubs" -name '*.java' | sort) \
 "$crate/proof/PmemGramCampaignJUnitFixtureRunner.java"
java -Xmx256m -cp "$tmp/tests" com.synexia.m3.pmem.campaign.PmemGramCampaignJUnitFixtureRunner
printf 'PASS strict Java21 source seals; real Maven/JNI NOT_RUN locally\n'
