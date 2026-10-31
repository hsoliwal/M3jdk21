#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Host JDK21 JNI UTF-16 / regex oracle only; not a matched M3JDK runtime pass.
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
source_dir="$root/test/jdk/java/lang/String"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
javac -Xlint:all -Werror -d "$work" -h "$work" "$source_dir/M3StringRegexNativeBridgeTest.java"
java_home="$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")"
cc="${CC:-cc}"
"$cc" -std=c17 -Wall -Wextra -Werror -O2 -fPIC -shared \
    -I"$java_home/include" -I"$java_home/include/linux" \
    -o "$work/libM3StringRegexNativeBridgeTest.so" \
    "$source_dir/M3StringRegexNativeBridgeTest.c"
java -cp "$work" -Djava.library.path="$work" -Xcheck:jni M3StringRegexNativeBridgeTest
java -cp "$work" -Djava.library.path="$work" -Xint -Xcheck:jni M3StringRegexNativeBridgeTest
