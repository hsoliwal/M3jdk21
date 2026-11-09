#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Successor receipt for the latest filtered/sorted source; do not alter historical seals.
set -euo pipefail
root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../../../.." && pwd)
cd "$root"
check_blob() {
  local expected=$1 path=$2 actual
  actual=$(git hash-object "$path")
  [[ "$actual" == "$expected" ]] || {
    echo "source receipt mismatch $path: $actual != $expected" >&2
    exit 1
  }
}
check_blob df618603df0d88bb6bab5270e0e0891671224e14 m3/collections/src/main/java/jdk/m3/collections/M3FilteredViews.java
check_blob 4f416b849012c273f5d2c8adcbcd850f4c9e0df3 m3/collections/src/main/java/jdk/m3/collections/M3FilteredNavigableSet.java
check_blob f6606e83e016baa889692ac05826cdd14ee2b5d3 m3/collections/src/main/java/jdk/m3/collections/M3SlotSpliterators.java
check_blob 129be6264889d79e93913aaae1815c9155748c60 m3/collections/src/test/java/jdk/m3/collections/M3FilteredNavigableFacadeSelfTest.java

# Keep the inherited JNI owner and its region oracle precisely pinned.
check_blob 521eb6170fd89f3bc62eb810aa6202236a2708e2 m3/collections/src/main/c/m3maskviewport.c
check_blob 22f8eaf891304f2fd35b6198003c5f57ab73e444 m3/collections/src/test/native/m3_mask_viewport_regions.c

build=$(mktemp -d)
trap 'rm -rf -- "$build"' EXIT
javac --release 21 -proc:none -Xlint:all -Werror -d "$build" \
  m3/collections/src/main/java/jdk/m3/collections/M3FilteredViews.java \
  m3/collections/src/main/java/jdk/m3/collections/M3FilteredNavigableSet.java \
  m3/collections/src/main/java/jdk/m3/collections/M3SlotSpliterators.java \
  m3/collections/src/test/java/jdk/m3/collections/M3FilteredViewsContract.java \
  m3/collections/src/test/java/jdk/m3/collections/M3FilteredNavigableFacadeSelfTest.java \
  m3/collections/src/test/java/jdk/m3/collections/M3FilteredSortedIteratorPlanSelfTest.java

java -cp "$build" jdk.m3.collections.M3FilteredViewsContract
java -cp "$build" jdk.m3.collections.M3FilteredNavigableFacadeSelfTest
java -cp "$build" jdk.m3.collections.M3FilteredSortedIteratorPlanSelfTest
# JNI instrumentation verifies bounded region transfers, aliases and late-invalid writes.
# It is a C test harness, not a full checked-JVM JNI parity run.
cc -std=c11 -O1 -Wall -Wextra -Werror -Wpedantic \
  -fsanitize=undefined -fno-sanitize-recover=all \
  -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" \
  -o "$build/jni-regions" m3/collections/src/test/native/m3_mask_viewport_regions.c
"$build/jni-regions"
echo "PASS sealed successor Java21 and inherited JNI region contracts; no full-image or phase promotion"
