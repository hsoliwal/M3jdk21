#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors.
set -euo pipefail
RX7_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
: "${M3_JAVA_HOME:?set M3_JAVA_HOME to a Java 21 JDK}"
export JAVA_HOME="$M3_JAVA_HOME"
export PATH="$M3_JAVA_HOME/bin:$PATH"
RX7_WORK="$(mktemp -d "${TMPDIR:-/tmp}/m3rx7-convergence.XXXXXX")"
trap 'rm -rf -- "$RX7_WORK"' EXIT
cd "$RX7_ROOT"
python3 m3/compatibility/check_synexia_full_borrow_receiver.py
bash m3/runtime-integration/check-m3rx7-v7-receiver.sh
bash m3/runtime-integration/check-m3rx7-v7-image-codec.sh
bash m3/runtime-integration/check-m3rx7-transition-native.sh
mkdir -p "$RX7_WORK/source/jdk/internal/mindex" "$RX7_WORK/patch" "$RX7_WORK/tests" "$RX7_WORK/collections"
cp src/java.base/share/classes/jdk/internal/mindex/M3RX7.java "$RX7_WORK/source/jdk/internal/mindex/"
cp src/java.base/share/classes/jdk/internal/mindex/M3RX7Transitions.java "$RX7_WORK/source/jdk/internal/mindex/"
javac -Xlint:all -Werror --patch-module "java.base=$RX7_WORK/source" -d "$RX7_WORK/patch" \
  "$RX7_WORK/source/jdk/internal/mindex/"*.java
javac -Xlint:all -Werror --patch-module "java.base=$RX7_WORK/patch" \
  --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED -d "$RX7_WORK/tests" \
  test/jdk/jdk/internal/mindex/M3RX7LexicalContract.java \
  test/jdk/jdk/internal/mindex/M3RX7TokenIndexContract.java
for RX7_TEST in M3RX7LexicalContract M3RX7TokenIndexContract; do
  java -ea -Xmx96m -Xcheck:jni --patch-module "java.base=$RX7_WORK/patch" \
    --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED -cp "$RX7_WORK/tests" "$RX7_TEST"
  java -ea -Xint -Xmx96m -Xcheck:jni --patch-module "java.base=$RX7_WORK/patch" \
    --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED -cp "$RX7_WORK/tests" "$RX7_TEST"
done
RX7_COLLECTION_PROD=m3/collections/src/main/java/com/m3/collections
RX7_COLLECTION_TEST=m3/collections/src/test/java/com/m3/collections
javac -Xlint:all -Werror -d "$RX7_WORK/collections" \
  "$RX7_COLLECTION_PROD/M3FilteredViews.java" "$RX7_COLLECTION_PROD/M3MutableFilteredViews.java" \
  "$RX7_COLLECTION_PROD/M3CollectionViews.java" "$RX7_COLLECTION_PROD/M3FilteredNavigableSet.java" \
  "$RX7_COLLECTION_TEST/M3FilteredSortedEdgesContract.java" \
  "$RX7_COLLECTION_TEST/M3FilteredViewsContract.java" "$RX7_COLLECTION_TEST/M3MutableFilteredViewsContract.java" \
  "$RX7_COLLECTION_TEST/M3BoundedFilteredNavigationContract.java" \
  "$RX7_COLLECTION_TEST/M3FilteredNavigableFacadeSelfTest.java" \
  "$RX7_COLLECTION_TEST/M3FilteredNavigableSetRangeContract.java" \
  "$RX7_COLLECTION_TEST/M3StoredRepresentativeContract.java"
for RX7_TEST in M3FilteredSortedEdgesContract M3FilteredViewsContract M3MutableFilteredViewsContract \
  M3BoundedFilteredNavigationContract M3FilteredNavigableFacadeSelfTest \
  M3FilteredNavigableSetRangeContract M3StoredRepresentativeContract; do
  java -ea -Xmx96m -cp "$RX7_WORK/collections" "com.m3.collections.$RX7_TEST"
done
printf '%s\n' 'M3_RX7_CONVERGENCE_HOST_PASS; full source-built SDK/reactor/M3JDK21/jtreg/JIT/GC/CDS/platform gates NOT_RUN'
