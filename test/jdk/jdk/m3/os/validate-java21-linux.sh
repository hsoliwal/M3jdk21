#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../../.." && pwd)"
cd "$repo"
sha256sum -c test/jdk/jdk/m3/os/SOURCE_SHA256.txt
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
mkdir -p "$work/module" "$work/headers" "$work/native"
module=src/jdk.m3.io/share/classes
javac --release 21 -proc:none -Xlint:all,-module -Werror \
    -h "$work/headers" -d "$work/module" \
    "$module/module-info.java" \
    "$module/jdk/m3/optane/PmemCapabilities.java" \
    "$module/jdk/m3/os/OSCapabilities.java" \
    "$module/jdk/m3/os/NativeProbe.java" \
    test/jdk/jdk/m3/os/OSCapabilitiesTest.java
jdk_home="$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")"
"${CC:-gcc}" -std=c17 -O2 -fPIC -shared -Wall -Wextra -Wpedantic -Werror \
    -I"$jdk_home/include" -I"$jdk_home/include/linux" -I"$work/headers" \
    "$module/../native/libm3osprobe/m3osprobe.c" \
    -o "$work/native/libm3osprobe.so"
nm -D "$work/native/libm3osprobe.so" | grep -q 'Java_jdk_m3_os_NativeProbe_probe0'
for mode in jit interpreter disabled missing; do
    opts=(-Djava.library.path="$work/native")
    if [[ "$mode" == interpreter ]]; then opts=(-Xint "${opts[@]}"); fi
    if [[ "$mode" == disabled ]]; then opts=(-Dm3.os.disableNative=true "${opts[@]}"); fi
    if [[ "$mode" == missing ]]; then opts=(-Djava.library.path="$work/headers"); fi
    java "${opts[@]}" --module-path "$work/module" \
        -m jdk.m3.io/jdk.m3.os.OSCapabilitiesTest > "$work/$mode.txt"
    grep -q 'M3_OS_CAPABILITIES_JAVA21_PASS' "$work/$mode.txt"
    case "$mode" in
        jit|interpreter) grep -q 'native=true platform=LINUX' "$work/$mode.txt";;
        disabled|missing) grep -q 'native=false platform=LINUX pageBytes=0 io=UNPROBED' "$work/$mode.txt";;
    esac
    cat "$work/$mode.txt"
done
javap -v "$work/module/module-info.class" | grep -q 'jdk.m3.os'
grep -q 'BUILD_LIBM3OSPROBE' make/modules/jdk.m3.io/Lib.gmk
printf 'M3_OS_PROBE_JAVA21_MODULE_AND_NATIVE_PASS platformsSimulated=13 jdkDefaultPreserved=true fullJdkImage=NOT_RUN\n'
