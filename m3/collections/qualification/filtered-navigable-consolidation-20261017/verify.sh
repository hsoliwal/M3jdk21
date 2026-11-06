#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../../../.." && pwd)
cd "$root"
check_blob() {
  local expected=$1 path=$2
  local actual
  actual=$(git hash-object "$path")
  [[ "$actual" == "$expected" ]] || { echo "blob mismatch $path: $actual != $expected" >&2; exit 1; }
}
check_blob 940de9e148adab4948aa9e53be1c45174511446f m3/collections/src/main/java/jdk/m3/collections/M3FilteredViews.java
check_blob 9dfd9ff60d68cd7a2d645bfdbfed4dae9f28176f m3/collections/src/main/java/jdk/m3/collections/M3FilteredNavigableSet.java
check_blob 129be6264889d79e93913aaae1815c9155748c60 m3/collections/src/test/java/jdk/m3/collections/M3FilteredNavigableFacadeSelfTest.java
build=$(mktemp -d)
trap 'rm -rf -- "$build"' EXIT
javac --release 21 -proc:none -Xlint:all -Werror -d "$build" \
  m3/collections/src/main/java/jdk/m3/collections/M3FilteredViews.java \
  m3/collections/src/main/java/jdk/m3/collections/M3FilteredNavigableSet.java \
  m3/collections/src/test/java/jdk/m3/collections/M3FilteredViewsContract.java \
  m3/collections/src/test/java/jdk/m3/collections/M3FilteredNavigableFacadeSelfTest.java
java -cp "$build" jdk.m3.collections.M3FilteredViewsContract
java -cp "$build" jdk.m3.collections.M3FilteredNavigableFacadeSelfTest
echo "PASS exact source receipt and selected filtered/sorted Java21 contracts; no phase promotion"
