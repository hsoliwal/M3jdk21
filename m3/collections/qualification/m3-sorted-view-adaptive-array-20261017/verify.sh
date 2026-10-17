#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Candidate proof: exact M3SortedView Java21 source closure and array metadata; no JNI changes.
set -euo pipefail
[[ $# == 1 ]] || { echo 'usage: verify.sh /absolute/new-evidence' >&2; exit 2; }
ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)
OUT=$1
[[ "$OUT" == /* && ! -e "$OUT" ]] || { echo 'evidence directory must be new and absolute' >&2; exit 2; }
mkdir -p "$OUT"
BASE=m3/collections
OWNER="$BASE/src/main/java/com/m3/collections/M3SortedView.java"
TEST="$BASE/src/test/java/com/m3/collections/M3SortedViewAdaptiveSelectionTest.java"
BEFORE="$BASE/qualification/m3-sorted-view-adaptive-array-20261017/preimage/M3SortedView.java.txt"
(cd "$ROOT" && sha256sum -c <<'SEALS'
59ba6c44528a6c0925a6492515f0ecb7c1f3c64355212c381ae63c81454cb575  m3/collections/qualification/m3-sorted-view-adaptive-array-20261017/preimage/M3SortedView.java.txt
931fd33c7fafc075c4e58f624fda1cf734106e34adcce413209fbf399d52bdf1  m3/collections/src/main/java/com/m3/collections/M3SortedView.java
25d1b37ce07daed68e433b9a79935dac9c6384f07ca3a6a48f1e401c6f00621a  m3/collections/src/test/java/com/m3/collections/M3SortedViewAdaptiveSelectionTest.java
SEALS
) | tee "$OUT/seal.log"
java -version > "$OUT/java-version.txt" 2>&1
mkdir -p "$OUT"/{before-source/com/m3/collections,before-classes,after-classes,test-classes}
cp "$ROOT/$BEFORE" "$OUT/before-source/com/m3/collections/M3SortedView.java"
SRCPATH="$ROOT/$BASE/src/main/java"
# Serial before/after compilation. Other package owners are read-only dependencies;
# the explicit one-file source path is the only changing implementation.
javac --release 21 -proc:none -Xlint:all,-module -Werror \
   -sourcepath "$SRCPATH" -d "$OUT/before-classes" \
   "$OUT/before-source/com/m3/collections/M3SortedView.java"
javac --release 21 -proc:none -Xlint:all,-module -Werror \
   -sourcepath "$SRCPATH" -d "$OUT/after-classes" "$ROOT/$OWNER"
javac --release 21 -proc:none -Xlint:all,-module -Werror \
   -cp "$OUT/after-classes" -d "$OUT/test-classes" "$ROOT/$TEST"
javap -public -s -classpath "$OUT/before-classes" com.m3.collections.M3SortedView > "$OUT/abi-before.txt"
javap -public -s -classpath "$OUT/after-classes" com.m3.collections.M3SortedView > "$OUT/abi-after.txt"
diff -u "$OUT/abi-before.txt" "$OUT/abi-after.txt" > "$OUT/public-abi.diff"
for mode in default interpreter c1 c2; do
  flags=()
  case "$mode" in
    interpreter) flags=(-Xint) ;;
    c1) flags=(-XX:TieredStopAtLevel=1) ;;
    c2) flags=(-XX:-TieredCompilation -Xbatch) ;;
  esac
  java -Xmx128m "${flags[@]}" -cp "$OUT/after-classes:$OUT/test-classes" \
    com.m3.collections.M3SortedViewAdaptiveSelectionTest > "$OUT/test-$mode.log" 2>&1
  grep -q 'M3SortedViewAdaptiveSelectionTest PASS' "$OUT/test-$mode.log"
  cat "$OUT/test-$mode.log"
done
echo 'PASS bounded Java21 owner and index proof. Not a full reactor/JDK/JNI performance result.' | tee "$OUT/summary.txt"
