#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Bounded Java 21 java.base patch qualification; exact pristine output directory.
set -euo pipefail
[[ $# == 1 ]] || { echo 'usage: verify.sh <new absolute evidence directory>' >&2; exit 2; }
ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)
OUT=$1
[[ "$OUT" == /* && ! -e "$OUT" ]] || { echo 'evidence output must be a new absolute path' >&2; exit 2; }
mkdir "$OUT"
SOURCE=src/java.base/share/classes/java/util/CollectionViews.java
TEST=test/jdk/java/util/CollectionViews/StoredRepresentativeOracle.java
BEFORE=m3/runtime-integration/java-util-stored-occurrence/preimage/CollectionViews.java.txt
(cd "$ROOT" && sha256sum --check <<'SEALED'
888504880eab30995556dc06e17295041b99ac4217141823d99286304c2a3dc4  src/java.base/share/classes/java/util/CollectionViews.java
5248d72231f7a55df8bee51e5cc68aff2c19fbe907e3f75ee0434ef832dac244  m3/runtime-integration/java-util-stored-occurrence/preimage/CollectionViews.java.txt
f32eeb5079bcd6322e597303a937614931b7f33779a58d0ed03b53ae0a8302c6  test/jdk/java/util/CollectionViews/StoredRepresentativeOracle.java
SEALED
) | tee "$OUT/seals.log"
java -version > "$OUT/java-version.txt" 2>&1
javac -version > "$OUT/javac-version.txt" 2>&1
mkdir -p "$OUT"/{before-src,after-src,before-classes,after-classes,test-classes}
mkdir -p "$OUT/before-src/java/util" "$OUT/after-src/java/util"
cp "$ROOT/$BEFORE" "$OUT/before-src/java/util/CollectionViews.java"
cp "$ROOT/$SOURCE" "$OUT/after-src/java/util/CollectionViews.java"
javac --patch-module java.base="$OUT/before-src" -Xlint:all -Werror -d "$OUT/before-classes" "$OUT/before-src/java/util/CollectionViews.java"
javac --patch-module java.base="$OUT/after-src" -Xlint:all -Werror -d "$OUT/after-classes" "$OUT/after-src/java/util/CollectionViews.java"
javac --patch-module java.base="$OUT/after-classes" -Xlint:all -Werror -d "$OUT/test-classes" "$ROOT/$TEST"
javap -public -s -classpath "$OUT/before-classes" java.util.CollectionViews > "$OUT/abi-before.txt"
javap -public -s -classpath "$OUT/after-classes" java.util.CollectionViews > "$OUT/abi-after.txt"
diff -u "$OUT/abi-before.txt" "$OUT/abi-after.txt" > "$OUT/api-diff.txt"
set +e
java -Xint --patch-module java.base="$OUT/before-classes" -cp "$OUT/test-classes" StoredRepresentativeOracle > "$OUT/baseline-red.log" 2>&1
RED=$?
set -e
[[ $RED -ne 0 ]] && grep -q 'AssertionError: check=1' "$OUT/baseline-red.log" || { echo 'original defect was not reproduced' >&2; exit 1; }
for mode in default interpreter c1 c2; do
  flags=()
  case "$mode" in
    interpreter) flags=(-Xint) ;;
    c1) flags=(-XX:TieredStopAtLevel=1) ;;
    c2) flags=(-XX:-TieredCompilation -Xbatch) ;;
  esac
  java "${flags[@]}" --patch-module java.base="$OUT/after-classes" -cp "$OUT/test-classes" StoredRepresentativeOracle > "$OUT/contract-$mode.log" 2>&1
  grep -q 'StoredRepresentativeOracle PASS checks=736018' "$OUT/contract-$mode.log"
  cat "$OUT/contract-$mode.log"
done
echo 'PASS source-sealed java.base stored-occurrence receiver; no JNI ABI modified; full JDK not built' | tee "$OUT/summary.log"
