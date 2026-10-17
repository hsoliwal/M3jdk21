#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Bounded exact-file Java21/C17/JNI proof. No live default-branch mutation.
set -euo pipefail
[[ $# -eq 1 && "$1" = /* && ! -e "$1" ]] || { echo "usage: $0 /absolute/new-evidence" >&2; exit 2; }
ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)
OUT="$1"
mkdir "$OUT"
src="m3/collections/src/main/java"
test="m3/collections/src/test/java/com/m3/collections"
pre="m3/collections/qualification/m3-object-index-adaptive-filter-20261017/preimage/M3ObjectIndexSnapshot.java.txt"
(cd "$ROOT" && sha256sum --check <<'SEALED'
ed19c7b34e23f61ea5bfa808550a1189ccc07900b7022f3572d78c23c5929f0c  m3/collections/qualification/m3-object-index-adaptive-filter-20261017/preimage/M3ObjectIndexSnapshot.java.txt
4076dd246b04dd6eaecfddd94325030660d65633088e51df7c1076ef8b03b1ab  m3/collections/src/main/java/com/m3/collections/M3ObjectIndexSnapshot.java
81e09bd63502a17e912ab326b4ad4be9bf08bfd35e4e478cecd938914f6badb9  m3/collections/src/test/java/com/m3/collections/M3ObjectIndexAdaptiveFilterTest.java
SEALED
) | tee "$OUT/sealed-source.log"
java -version > "$OUT/java-version.txt" 2>&1
gcc --version > "$OUT/gcc-version.txt" 2>&1
mkdir -p "$OUT"/{old-src/com/m3/collections,old-classes,new-classes,native}
cp "$ROOT/$pre" "$OUT/old-src/com/m3/collections/M3ObjectIndexSnapshot.java"
SRC="$ROOT/$src"
TST="$ROOT/$test"
# Explicit old production owner wins over the new-source searchpath, other owners are shared.
javac --release 21 -proc:none -Xlint:all -Werror -sourcepath "$SRC" \
  -d "$OUT/old-classes" "$OUT/old-src/com/m3/collections/M3ObjectIndexSnapshot.java" \
  "$TST/M3ObjectSlotIndexSelfTest.java" "$TST/M3ObjectIndexAdaptiveFilterTest.java" \
  > "$OUT/old-compile.log" 2>&1
javac --release 21 -proc:none -Xlint:all -Werror -sourcepath "$SRC" \
  -d "$OUT/new-classes" "$SRC/com/m3/collections/M3ObjectIndexSnapshot.java" \
  "$TST/M3ObjectSlotIndexSelfTest.java" "$TST/M3ObjectIndexAdaptiveFilterTest.java" \
  > "$OUT/new-compile.log" 2>&1
javap -public -s -classpath "$OUT/old-classes" com.m3.collections.M3ObjectIndexSnapshot > "$OUT/old-api.txt"
javap -public -s -classpath "$OUT/new-classes" com.m3.collections.M3ObjectIndexSnapshot > "$OUT/new-api.txt"
diff -u "$OUT/old-api.txt" "$OUT/new-api.txt" > "$OUT/api-diff.txt"
for mode in default interpreter c1 c2; do
  flags=()
  case "$mode" in interpreter) flags=(-Xint);; c1) flags=(-XX:TieredStopAtLevel=1);; c2) flags=(-XX:-TieredCompilation -Xbatch);; esac
  for stage in old new; do
    for proof in M3ObjectSlotIndexSelfTest M3ObjectIndexAdaptiveFilterTest; do
      java "${flags[@]}" -cp "$OUT/$stage-classes" "com.m3.collections.$proof" > "$OUT/$stage-$mode-$proof.log" 2>&1
      grep -q 'PASS' "$OUT/$stage-$mode-$proof.log"
    done
  done
  for proof in M3ObjectSlotIndexSelfTest M3ObjectIndexAdaptiveFilterTest; do
    cmp "$OUT/old-$mode-$proof.log" "$OUT/new-$mode-$proof.log"
  done
done
# JNI code is the *existing* bounded mask/window selector and signal-order kernel.
gcc -std=c11 -Wall -Wextra -Werror -Wconversion -Wshadow -pedantic \
  -fPIC -shared -I "$JAVA_HOME/include" -I "$JAVA_HOME/include/linux" \
  "$ROOT/m3/collections/src/main/native/collections/m3_index_mask_compactor.c" \
  "$ROOT/m3/collections/src/main/native/collections/m3_index_mask_window.c" \
  -o "$OUT/native/libm3_collections_mask.so"
gcc -std=c11 -Wall -Wextra -Werror -Wconversion -Wshadow -pedantic \
  -fPIC -shared -I "$JAVA_HOME/include" -I "$JAVA_HOME/include/linux" \
  "$ROOT/m3/collections/src/main/native/collections/m3_signal_order_jni.c" \
  -o "$OUT/native/libm3signalorder.so"
for stage in old new; do
  java -Xcheck:jni -Dm3.collections.signal.native=true \
    -Djava.library.path="$OUT/native" -cp "$OUT/$stage-classes" \
    com.m3.collections.M3ObjectSlotIndexSelfTest > "$OUT/$stage-jni.log" 2>&1
  grep -q 'PASS' "$OUT/$stage-jni.log"
done
cmp "$OUT/old-jni.log" "$OUT/new-jni.log"
echo 'PASS exact source/ABI/Java21/previous JNI contract; standalone selected source closure only' | tee "$OUT/SUMMARY.txt"
