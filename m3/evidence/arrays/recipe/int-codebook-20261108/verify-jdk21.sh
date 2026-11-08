#!/usr/bin/env bash
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
arrays="$(cd "$here/../.." && pwd)"
out="$arrays/target/int-codebook-proof"
mkdir -p "$out/classes"
javac --release 21 -Xlint:all,-module -Werror -d "$out/classes" \
    "$arrays/src/main/java/jdk/m3/arrays/M3ArrayNative.java" \
    "$arrays/src/main/java/jdk/m3/arrays/packed/IntCodebookArray.java" \
    "$arrays/src/main/java/jdk/m3/arrays/packed/IntCodebookNative.java" \
    "$arrays/src/test/java/jdk/m3/arrays/packed/IntCodebookSelfTestMain.java"
cc -std=c11 -Wall -Wextra -Werror -Wconversion -Wshadow -Wpedantic \
    -I "$arrays/src/main/native/include" \
    "$arrays/src/main/native/m3_int_codebook.c" \
    "$arrays/src/main/native/tests/m3_int_codebook_checks.c" \
    -o "$out/codebook-checks"
"$out/codebook-checks"
java -cp "$out/classes" jdk.m3.arrays.packed.IntCodebookSelfTestMain
if [ "$(uname -s)" = Linux ]; then
    java_home="${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")}" 
    cc -std=c11 -fPIC -shared -Wall -Wextra -Werror -Wconversion \
        -Wshadow -Wpedantic -I "$java_home/include" -I "$java_home/include/linux" \
        -I "$arrays/src/main/native/include" \
        "$arrays/src/main/native/m3_int_codebook.c" \
        "$arrays/src/main/native/m3_int_codebook_jni.c" \
        -o "$out/libm3_arrays.so"
    java -Dm3.arrays.native.path="$out/libm3_arrays.so" \
        -cp "$out/classes" jdk.m3.arrays.packed.IntCodebookSelfTestMain
fi
echo M3_INT_CODEBOOK_VERIFY_PASS
