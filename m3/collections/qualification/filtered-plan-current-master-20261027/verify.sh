#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Current-master Java21 qualification. Historical source receipts are not modified.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/../../../.."
source_file=m3/collections/src/main/java/com/m3/collections/M3FilteredViews.java
expected=45ad6df3a51e83c67dc70de7c7acd82bec2305d3
actual=$(git hash-object "$source_file")
if [[ "$actual" != "$expected" ]]; then
  echo "REFUSAL: unreviewed filtered-owner source $actual != $expected" >&2
  exit 1
fi
# These tests were received byte-exactly from the previously passing selected suite.
[[ "$(git hash-object m3/collections/src/test/java/com/m3/collections/M3FilteredSortedIteratorPlanTest.java)" == e9895cf852bd875a70a19d441cc6e055238c1b1f ]] || exit 1
[[ "$(git hash-object m3/collections/src/test/java/com/m3/collections/M3FilteredSortedIteratorPlanSelfTest.java)" == b58068775c83b7a67f75d059eefe0cf07f4af1ba ]] || exit 1

[[ "$(git hash-object m3/collections/src/test/java/com/m3/collections/M3FilteredMasterTreePlanTest.java)" == 586e5ab0832f23a32a529aeb9bb557320c45695d ]] || { echo "M3TreeMap contract test source drift" >&2; exit 1; }

mvn -B -ntp -f m3/arrays/pom.xml -Dmaven.test.skip=true install
mvn -B -ntp -f m3/collections/pom.xml -Dtest=M3FilteredSortedIteratorPlanTest,M3FilteredMasterTreePlanTest test
java -cp "m3/collections/target/classes:m3/collections/target/test-classes:m3/arrays/target/classes" \
  com.m3.collections.M3FilteredSortedIteratorPlanSelfTest
echo "PASS selected current-master filtered predicate plan contracts; full JDK image remains OPEN"
