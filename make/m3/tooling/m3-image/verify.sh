#!/bin/bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
set -euo pipefail
here=$(cd "$(dirname "$0")" && pwd)
build=$(mktemp -d)
trap 'rm -rf "$build"' EXIT
bash -n "$here/run.sh" "$here/verify.sh"
mapfile -d '' main_sources < <(find "$here/src/main/java" -name '*.java' -type f -print0 | sort -z)
mkdir -p "$build/classes" "$build/test-classes" "$build/jni-headers"
javac --release 21 -Xlint:all -Werror -h "$build/jni-headers" -d "$build/classes" "${main_sources[@]}"
javac --release 21 -Xlint:all -Werror -cp "$build/classes" -d "$build/test-classes" \
  "$here/src/test/java/jdk/m3/tooling/image/M3ImageSelfTest.java" \
  "$here/src/test/java/jdk/m3/tooling/image/M3ReachabilityKernelSelfTest.java"

header="$build/jni-headers/jdk_m3_tooling_image_M3JniReachabilityKernel.h"
test -s "$header"
grep -Fq 'Java_jdk_m3_tooling_image_M3JniReachabilityKernel_close0' "$header"
javap -classpath "$build/classes" -s -p jdk.m3.tooling.image.M3JniReachabilityKernel \
  | grep -Fq 'descriptor: (I[I[I[I)[I'

java_home=${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")}
case "$(uname -s)" in
  Linux*) jni_platform=linux; library="$build/libm3image_reachability.so"; shared_flags=(-shared) ;;
  Darwin*) jni_platform=darwin; library="$build/libm3image_reachability.dylib"; shared_flags=(-dynamiclib) ;;
  *) echo "Unsupported JNI verification platform: $(uname -s)" >&2; exit 1 ;;
esac
cc=${CC:-cc}
command -v "$cc" >/dev/null
"$cc" -std=c11 -O2 -fPIC -Wall -Wextra -Wpedantic -Werror "${shared_flags[@]}" \
  -I"$java_home/include" -I"$java_home/include/$jni_platform" -I"$build/jni-headers" \
  "$here/src/main/native/libm3image/M3ReachabilityKernel.c" -o "$library"

java -cp "$build/classes:$build/test-classes" jdk.m3.tooling.image.M3ReachabilityKernelSelfTest
java -cp "$build/classes:$build/test-classes" jdk.m3.tooling.image.M3ReachabilityKernelSelfTest "$library"
java -cp "$build/classes:$build/test-classes" jdk.m3.tooling.image.M3ImageSelfTest
javadoc -quiet -Werror -Xdoclint:all,-missing --release 21 -d "$build/javadoc" "${main_sources[@]}"
echo M3_IMAGE_VERIFY_OK
