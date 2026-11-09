#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
crate="$(cd "$(dirname "$0")" && pwd)"
repo="${M3_REPO_ROOT:-$(cd "$crate/../../.." && pwd)}"
owner="$repo/m3-pmem-sdk/campaign/src/main/java/com/synexia/m3/pmem/campaign/PmemCorpusGramIndex.java"
selected="$repo/m3-pmem-sdk/campaign/src/main/java/com/synexia/m3/pmem/campaign/PmemCorpusSelectedSearch.java"
public="$repo/m3-pmem-sdk/campaign/src/main/java/com/synexia/m3/pmem/campaign/PmemCorpusSearch.java"
printf '%s  %s\n' \
 523a4a541952c163caf6614583d92421a35193b186656ff33b5dedccba0b6174 "$owner" \
 a88eb92d8cf00411171d20648f90ce51c7b472f6604d29073bcd82d976f9628f "$selected" \
 3493a0f911023d8863d86cdf1b8f19c08d25d4aa5a76be8cfacb3bdc1bc64deb "$public" \
 | sha256sum --check --status
temp="$(mktemp -d)"
trap 'rm -rf "$temp"' EXIT
mkdir -p "$temp/classes"
javac --release 21 -Xlint:all -Werror -d "$temp/classes" \
 "$owner" "$selected" "$public" \
 "$crate/proof/mmap_stubs/com/synexia/m3/pmem/PmemSearch.java" \
 "$crate/proof/mmap_stubs/com/synexia/m3/pmem/PmemSourceImage.java" \
 "$crate/proof/mmap/com/synexia/m3/pmem/campaign/PmemOneGigabyteMappedProof.java"
if [[ -x /usr/bin/time ]]; then
  /usr/bin/time -f 'PEAK_RSS_KB=%M ELAPSED_S=%e' \
    java -Xmx128m -cp "$temp/classes" \
      com.synexia.m3.pmem.campaign.PmemOneGigabyteMappedProof
else
  java -Xmx128m -cp "$temp/classes" \
      com.synexia.m3.pmem.campaign.PmemOneGigabyteMappedProof
fi
