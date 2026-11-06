#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Current-master Java21 qualification. Historical source receipts are not modified.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/../../../.."
source_file=m3/collections/src/main/java/jdk/m3/collections/M3FilteredViews.java
expected=45ad6df3a51e83c67dc70de7c7acd82bec2305d3
actual=$(git hash-object "$source_file")
if [[ "$actual" != "$expected" ]]; then
  echo "REFUSAL: unreviewed filtered-owner source $actual != $expected" >&2
  exit 1
fi
# These tests were received byte-exactly from the previously passing selected suite.
[[ "$(git hash-object m3/collections/src/test/java/jdk/m3/collections/M3FilteredSortedIteratorPlanTest.java)" == 81dd2b286610ad03f9a317167c3a61e92ed8512c ]] || exit 1
[[ "$(git hash-object m3/collections/src/test/java/jdk/m3/collections/M3FilteredSortedIteratorPlanSelfTest.java)" == 1c92ff79dd9d13271caaf2c51b515f57c1c4fa7f ]] || exit 1

[[ "$(git hash-object m3/collections/src/test/java/jdk/m3/collections/M3FilteredMasterTreePlanTest.java)" == ccedb1f8016e7175f06ea89155e857d43911fc7f ]] || { echo "M3TreeMap contract test source drift" >&2; exit 1; }

[[ "$(git hash-object m3/collections/src/test/java/jdk/m3/collections/M3FilteredMasterPredicatePlanLifecycleTest.java)" == 202ebf6ca1210374c379be576b7cf1121d829005 ]] || { echo "Predicate lifecycle JUnit source drift" >&2; exit 1; }

[[ "$(git hash-object m3/collections/src/test/java/jdk/m3/collections/M3ReadOnlyMapDefaultsTest.java)" == c770f8970b410fe60efbb35748346f9338b9f67b ]] || { echo "Contract source drift: M3ReadOnlyMapDefaultsTest.java" >&2; exit 1; }
[[ "$(git hash-object m3/collections/src/test/java/jdk/m3/collections/M3FilteredViewsTest.java)" == 25170efa291aa22f7912771dcd543727a0ed409e ]] || { echo "Contract source drift: M3FilteredViewsTest.java" >&2; exit 1; }
[[ "$(git hash-object m3/collections/src/test/java/jdk/m3/collections/M3FilteredNavigableSetTest.java)" == 690a558cdd316d2af42da746894169926ebadfa1 ]] || { echo "Contract source drift: M3FilteredNavigableSetTest.java" >&2; exit 1; }

mvn -B -ntp -f m3/arrays/pom.xml -Dmaven.test.skip=true install
mvn -B -ntp -f m3/collections/pom.xml -Dtest=M3FilteredSortedIteratorPlanTest,M3FilteredMasterTreePlanTest,M3FilteredMasterPredicatePlanLifecycleTest,M3ReadOnlyMapDefaultsTest,M3FilteredViewsTest,M3FilteredNavigableSetTest test
java -cp "m3/collections/target/classes:m3/collections/target/test-classes:m3/arrays/target/classes" \
  jdk.m3.collections.M3FilteredSortedIteratorPlanSelfTest
echo "PASS selected current-master filtered predicate plan contracts; full JDK image remains OPEN"
