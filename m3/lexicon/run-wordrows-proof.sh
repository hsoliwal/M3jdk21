#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
cd "$(dirname "$0")/.."
: "${M3_JDK:?Set M3_JDK to a Java 21 JDK directory}"
cc=${CC:-cc}
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
"$M3_JDK/bin/javac" --release 21 -Xlint:all -Werror -h "$work" \
    -d "$work" core/src/com/m3/text/M3WordRows.java \
    core/src/com/m3/text/M3WordRowsNative.java \
    core/test/M3WordRowsContractTest.java
"$cc" -std=c11 -Wall -Wextra -Werror -fPIC -shared \
    -I"$M3_JDK/include" -I"$M3_JDK/include/linux" \
    lexicon/native/m3_wordrows_jni.c -o "$work/libm3wordrows.so"
for mode in default interpreter compact-off c2; do
    flags=()
    case "$mode" in
        interpreter) flags=(-Xint);;
        compact-off) flags=(-XX:-CompactStrings);;
        c2) flags=(-Xbatch -XX:-TieredCompilation);;
    esac
    "$M3_JDK/bin/java" -Xcheck:jni -ea -esa "${flags[@]}" \
        -cp "$work" M3WordRowsContractTest "$work/libm3wordrows.so"
done
