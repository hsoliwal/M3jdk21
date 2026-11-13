#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
crate="$(cd "$(dirname "$0")" && pwd)"
repo="${M3_REPO_ROOT:-$(cd "$crate/../../.." && pwd)}"
owner="$repo/m3-pmem-sdk/campaign/src/main/java/com/synexia/m3/pmem/campaign"
tests="$repo/m3-pmem-sdk/campaign/src/test/java/com/synexia/m3/pmem/campaign"
parent="$repo/synexia-openrewrite-recipes/recipe-crates/m3-pmem-gram-campaign-20261105/proof"
resources="$crate/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-pmem-gram-source-revalidate-20261105"
check_sha() { printf '%s  %s\n' "$1" "$2" | sha256sum -c -; }
check_sha b6f7ad4fc485e9c6b8bf0f54d5a5eb85e0f9edc959144db407e3639872f12905 "$resources/pre/PmemGramCampaignAdmission.java.txt"
check_sha 88e8b29fd1639c66f01d8ae97ee597fca09d796091385a6be340e5e120d32c63 "$resources/pre/PmemGramCampaignAdmissionJUnitTest.java.txt"
check_sha 9804dbbbb5b8b27ee811064a8367a395fd7881f92fc9f9992fdf4d4c55e6b44d "$resources/PmemGramCampaignAdmission.java.txt"
check_sha f7b04b3214e0da586864a86a5d2a79d1569678847de485a90f9922aed3e997a3 "$resources/PmemGramCampaignAdmissionJUnitTest.java.txt"
check_sha 9804dbbbb5b8b27ee811064a8367a395fd7881f92fc9f9992fdf4d4c55e6b44d "$owner/PmemGramCampaignAdmission.java"
check_sha f7b04b3214e0da586864a86a5d2a79d1569678847de485a90f9922aed3e997a3 "$tests/PmemGramCampaignAdmissionJUnitTest.java"
check_sha 85d83f286e49e69e3ada9e8f4e0f772e66f214964c7d332ef2107e20dcfbd5fd "$owner/PmemCorpusGramIndex.java"
check_sha a88eb92d8cf00411171d20648f90ce51c7b472f6604d29073bcd82d976f9628f "$owner/PmemCorpusSelectedSearch.java"
cmp "$resources/PmemGramCampaignAdmission.java.txt" "$owner/PmemGramCampaignAdmission.java"
cmp "$resources/PmemGramCampaignAdmissionJUnitTest.java.txt" "$tests/PmemGramCampaignAdmissionJUnitTest.java"
tmp="$(mktemp -d)"; trap 'rm -rf "$tmp"' EXIT
mkdir -p "$tmp/pre-src/com/synexia/m3/pmem/campaign" "$tmp/pre" "$tmp/post" "$tmp/junit"
cp "$resources/pre/PmemGramCampaignAdmission.java.txt" "$tmp/pre-src/com/synexia/m3/pmem/campaign/PmemGramCampaignAdmission.java"
# Actual new production source and PRE source against the same test-only SDK contract.
javac --release 21 -Xlint:all -Werror -d "$tmp/pre" \
  "$tmp/pre-src/com/synexia/m3/pmem/campaign/PmemGramCampaignAdmission.java" \
  "$owner/PmemCorpusGramIndex.java" "$owner/PmemCorpusSelectedSearch.java" \
  $(find "$parent/stubs" -name '*.java' | sort) \
  "$parent/PmemGramCampaignAdmissionSmoke.java" "$crate/proof/SourceGenerationDriftProof.java"
javac --release 21 -Xlint:all -Werror -d "$tmp/post" \
  "$owner/PmemGramCampaignAdmission.java" "$owner/PmemCorpusGramIndex.java" \
  "$owner/PmemCorpusSelectedSearch.java" \
  $(find "$parent/stubs" -name '*.java' | sort) \
  "$parent/PmemGramCampaignAdmissionSmoke.java" "$crate/proof/SourceGenerationDriftProof.java"
set +e
java -Xmx128m -cp "$tmp/pre" com.synexia.m3.pmem.campaign.SourceGenerationDriftProof > "$tmp/red.log" 2>&1
status=$?
set -e
if [[ $status -eq 0 ]] || ! grep -q 'RED: accepted source-image mutation' "$tmp/red.log"; then
  cat "$tmp/red.log" >&2; echo 'Required original-source RED regression was not reproduced' >&2; exit 1
fi
printf 'EXPECTED RED original source: stale gram-pruned source was silently admitted\n'
java -Xmx128m -cp "$tmp/post" com.synexia.m3.pmem.campaign.SourceGenerationDriftProof > "$tmp/green.log"
cat "$tmp/green.log"
java -Xmx128m -cp "$tmp/pre" com.synexia.m3.pmem.campaign.PmemGramCampaignAdmissionSmoke > "$tmp/before.log"
java -Xmx128m -cp "$tmp/post" com.synexia.m3.pmem.campaign.PmemGramCampaignAdmissionSmoke > "$tmp/after.log"
cmp "$tmp/before.log" "$tmp/after.log"
cat "$tmp/after.log"
for kind in pre post; do
  javap -classpath "$tmp/$kind" -p -s com.synexia.m3.pmem.campaign.PmemGramCampaignAdmission > "$tmp/$kind.desc"
done
cmp "$tmp/pre.desc" "$tmp/post.desc"
printf 'PASS identical original Java/JNI independent method descriptors\n'
# The FILE-image stand-in persists actual source.bin bytes and original digests,
# but it is NOT the original PMEM SDK, real JUnit runner or JNI shared library.
javac --release 21 -Xlint:all -Werror -d "$tmp/junit" \
  "$owner/PmemGramCampaignAdmission.java" "$owner/PmemCorpusGramIndex.java" \
  "$owner/PmemCorpusSelectedSearch.java" "$tests/PmemGramCampaignAdmissionJUnitTest.java" \
  $(find "$parent/sdk_stubs" -name '*.java' ! -path '*/com/synexia/m3/pmem/PmemSourceImage.java' | sort) \
  "$crate/proof/sdk_stubs/com/synexia/m3/pmem/PmemSourceImage.java" \
  "$crate/proof/SourceGenerationJUnitFixtureRunner.java"
java -Xmx256m -cp "$tmp/junit" com.synexia.m3.pmem.campaign.SourceGenerationJUnitFixtureRunner
if [[ "${M3_EXTENDED:-false}" == true ]]; then
  java -Xint -Xmx128m -cp "$tmp/post" com.synexia.m3.pmem.campaign.SourceGenerationDriftProof > "$tmp/interpreted.log"
  cmp "$tmp/green.log" "$tmp/interpreted.log"
  java -Xint -Xmx128m -cp "$tmp/post" com.synexia.m3.pmem.campaign.PmemGramCampaignAdmissionSmoke > "$tmp/interpreted-smoke.log"
  cmp "$tmp/after.log" "$tmp/interpreted-smoke.log"
fi
printf 'PASS source revalidation: RED/GREEN, Java21 strict compile, 20k source, test-only FILE/JUnit, ABI descriptors; Maven/JNI NOT RUN\n'
