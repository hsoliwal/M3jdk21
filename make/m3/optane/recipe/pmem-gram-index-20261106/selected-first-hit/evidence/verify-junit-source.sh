#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
crate="$(cd "$(dirname "$0")" && pwd)"
repo="${M3_REPO_ROOT:-$(cd "$crate/../../.." && pwd)}"
root="$repo/m3-pmem-sdk/campaign"
temp="$(mktemp -d)"
trap 'rm -rf "$temp"' EXIT
mkdir -p "$temp/classes"
# A locally executable shape/logic check only. This uses stand-in JUnit and SDK,
# not the actual Maven JUnit engine nor native POSIX/C17 JNI implementation.
javac --release 21 -Xlint:all -Werror -d "$temp/classes" \
 "$crate/proof/sdk_stubs/com/synexia/m3/pmem/"*.java \
 "$crate/proof/sdk_stubs/org/junit/jupiter/api/"*.java \
 "$crate/proof/sdk_stubs/org/junit/jupiter/api/io/"*.java \
 "$crate/proof/sdk_stubs/org/junit/jupiter/api/condition/"*.java \
 "$root/src/main/java/com/synexia/m3/pmem/campaign/PmemCorpusSearch.java" \
 "$root/src/main/java/com/synexia/m3/pmem/campaign/PmemCorpusSelectedSearch.java" \
 "$root/src/test/java/com/synexia/m3/pmem/campaign/PmemCorpusSelectedJUnitTest.java" \
 "$crate/proof/TestRunner.java"
java -Xmx256m -Dpmem.corpus.selected.scale=true -cp "$temp/classes" com.synexia.m3.pmem.campaign.TestRunner
