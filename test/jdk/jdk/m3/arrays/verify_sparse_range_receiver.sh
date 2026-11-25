#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Qualify exact M3JDK21 sparse-range receiver source using stock Java 21/Linux.
# No install-image, Maven/OpenRewrite or multi-platform acceptance is claimed.
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
owner='src/jdk.m3.collections/share/classes/jdk/m3/arrays/IntLaneAlgorithms.java'
loader='src/jdk.m3.collections/share/classes/jdk/m3/arrays/ArrayNative.java'
native='src/jdk.m3.collections/share/native/libm3arrays'
selftest='test/jdk/jdk/m3/arrays/IntLaneAlgorithmsSelfTest.java'
sparse_test='test/jdk/jdk/m3/arrays/IntLaneSparseRangeJtreg.java'
base='e48f465bd1050834e53ad699e1e45c181f867660'
original_hash='8834efb874f3a474af8b611a05abc9ad6211f3a1746ed7d37a29f9b0ddd1b95a'
new_hash='d3f78a046a3ff1d1bc27dfebdfd0e82f34de3bc8e9b604fb722c9a728ab77fd8'
want_native='M3_LEETCODIFY_ARRAY_KERNELS PASS checks=175009 native=true'
want_java='M3_LEETCODIFY_ARRAY_KERNELS PASS checks=151009 native=false'
want_sparse='M3_SPARSE_RANGE_JTREG_PASS cases=5539 checks=5542 digest=-6172280771964338784 huge=true'
want_sparse_baseline='M3_SPARSE_RANGE_JTREG_PASS cases=5539 checks=5539 digest=-6172280771964338784 huge=false'
root="$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")"
[[ -f "$root/include/jni.h" && -f "$root/include/linux/jni_md.h" ]] || {
  echo 'HOLD: Java 21 JNI headers absent' >&2; exit 1;
}
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
out="$PWD/build/m3jdk-sparse-receiver-evidence"
mkdir -p "$out"
rm -f "$out"/*.log "$out"/*.txt
# The original is the exact pinned product preimage, never reconstructed by hand.
git cat-file -e "$base^{commit}"
mkdir -p "$work/original/src/jdk/m3/arrays" "$work/candidate/src/jdk/m3/arrays"
git show "$base:$owner" > "$work/original/src/jdk/m3/arrays/IntLaneAlgorithms.java"
cp "$owner" "$work/candidate/src/jdk/m3/arrays/IntLaneAlgorithms.java"
cat <<SEALS | sha256sum -c --status
$original_hash  $work/original/src/jdk/m3/arrays/IntLaneAlgorithms.java
$new_hash  $work/candidate/src/jdk/m3/arrays/IntLaneAlgorithms.java
632dc2d759d608c98ac1d44e441d03afe9a0d454b9dd6c7601eb22e7c8c49b43  $loader
18976d82ac37e49990b7c62790e98b38dd8c634260b0791617c10133350f9811  $selftest
6b65303601e9a23543eb97cd7aff5f67cd4034da946dc5e07cf4b04e7201624e  $sparse_test
ffaf942ac913dc19b19205225cb1f77b86008acddc3811e3aea90cf924efea3c  $native/m3_int_lane_search.c
40b1fe498c4a1440c64cd407a341a7e749e55dc3f893acb3b8c99cfbb97e1929  $native/m3_int_lane_search_jni.c
8806108f778f7dfee32047dc6e36890b9a5ed15bd718254c38992b7127e69e08  $native/include/m3_int_lane_search.h
SEALS
printf '%s\n' 'RECEIVER_SOURCE_SEAL_PASS original/candidate/product tests and native C headers'
cc -std=c11 -O2 -fPIC -shared -Wall -Wextra -Werror -Wpedantic \
  -I"$root/include" -I"$root/include/linux" -I"$native/include" \
  "$native/m3_int_lane_search.c" "$native/m3_int_lane_search_jni.c" \
  -o "$work/libm3_int_lane_search_jni.so"
cc -std=c11 -O1 -fPIC -shared -Wall -Wextra -Werror -Wpedantic \
  -fsanitize=undefined -fno-sanitize-recover=all \
  -I"$root/include" -I"$root/include/linux" -I"$native/include" \
  "$native/m3_int_lane_search.c" "$native/m3_int_lane_search_jni.c" \
  -o "$work/libm3_int_lane_search_ubsan.so"
nm -D "$work/libm3_int_lane_search_jni.so" | \
  grep -Eq '[[:space:]]T[[:space:]]Java_jdk_m3_arrays_IntLaneAlgorithms_nativeBound$'
for lane in original candidate; do
  dir="$work/$lane"
  cp "$loader" "$dir/src/jdk/m3/arrays/ArrayNative.java"
  mkdir -p "$dir/classes" "$dir/headers"
  javac --release 21 -Xlint:all -Werror -h "$dir/headers" -d "$dir/classes" \
    "$dir/src/jdk/m3/arrays/IntLaneAlgorithms.java" \
    "$dir/src/jdk/m3/arrays/ArrayNative.java" \
    "$selftest" "$sparse_test"
  javap -classpath "$dir/classes" -public jdk.m3.arrays.IntLaneAlgorithms > "$out/$lane-public-api.txt"
  javap -classpath "$dir/classes" -s -p jdk.m3.arrays.IntLaneAlgorithms | \
    grep -A1 '^  private static native int nativeBound' > "$out/$lane-native-jni.txt"
  grep -Fq 'Java_jdk_m3_arrays_IntLaneAlgorithms_nativeBound' \
    "$dir/headers/jdk_m3_arrays_IntLaneAlgorithms.h"
  grep -Fq 'Signature: ([IIIIZ)I' \
    "$dir/headers/jdk_m3_arrays_IntLaneAlgorithms.h"
  java -Xmx256m -Dm3.arrays.native.path= -cp "$dir/classes" \
    jdk.m3.arrays.IntLaneAlgorithmsSelfTest | tee "$out/$lane-java.log"
  [[ "$(cat "$out/$lane-java.log")" == "$want_java" ]] || {
    echo "HOLD: $lane Java-only selftest receipt" >&2; exit 1;
  }
  for mode in jit xint serial g1; do
    case "$mode" in
      jit) flags=(-Xcheck:jni -Xverify:all -Xmx256m) ;;
      xint) flags=(-Xint -Xcheck:jni -Xverify:all -Xmx256m) ;;
      serial) flags=(-Xcheck:jni -Xverify:all -XX:+UseSerialGC -Xmx128m) ;;
      g1) flags=(-Xcheck:jni -Xverify:all -XX:+UseG1GC -Xmx128m) ;;
    esac
    java "${flags[@]}" -Dm3.arrays.native.path="$work/libm3_int_lane_search_jni.so" \
      -cp "$dir/classes" jdk.m3.arrays.IntLaneAlgorithmsSelfTest | tee "$out/$lane-native-$mode.log"
    [[ "$(cat "$out/$lane-native-$mode.log")" == "$want_native" ]] || {
      echo "HOLD: $lane native $mode receipt" >&2; exit 1;
    }
  done
  UBSAN_OPTIONS=halt_on_error=1:print_stacktrace=1 \
    java -Xcheck:jni -Xverify:all -Xmx256m \
    -Dm3.arrays.native.path="$work/libm3_int_lane_search_ubsan.so" \
    -cp "$dir/classes" jdk.m3.arrays.IntLaneAlgorithmsSelfTest | tee "$out/$lane-ubsan.log"
  [[ "$(cat "$out/$lane-ubsan.log")" == "$want_native" ]] || {
    echo "HOLD: $lane UBSan native receipt" >&2; exit 1;
  }
  if [[ "$lane" == candidate ]]; then
    java -Xmx256m -cp "$dir/classes" IntLaneSparseRangeJtreg --huge | tee "$out/$lane-jtreg.log"
    [[ "$(cat "$out/$lane-jtreg.log")" == "$want_sparse" ]] || {
      echo 'HOLD: sparse-range receiving jtreg oracle failed' >&2; exit 1;
    }
  else
    java -Xmx512m -cp "$dir/classes" IntLaneSparseRangeJtreg | tee "$out/$lane-jtreg.log"
    [[ "$(cat "$out/$lane-jtreg.log")" == "$want_sparse_baseline" ]] || {
      echo 'HOLD: baseline bounded-range jtreg oracle failed' >&2; exit 1;
    }
  fi
done
cmp "$out/original-public-api.txt" "$out/candidate-public-api.txt"
cmp "$out/original-native-jni.txt" "$out/candidate-native-jni.txt"
cmp "$out/original-java.log" "$out/candidate-java.log"
for mode in jit xint serial g1; do
  cmp "$out/original-native-$mode.log" "$out/candidate-native-$mode.log"
done
cmp "$out/original-ubsan.log" "$out/candidate-ubsan.log"
printf '%s\n' 'M3_SPARSE_RECEIVER_JAVA21_JNI_PASS 10 native selftests including UBSan, 2 Java-only tests; original/candidate bounded and huge-universe jtreg; public and JNI ABI equal'
