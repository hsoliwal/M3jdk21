#!/usr/bin/env bash
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
arrays="$(cd "$here/../.." && pwd)"
mkdir -p "$arrays/target/adaptive"
javac --release 21 -Xlint:all,-module -Werror -d "$arrays/target/adaptive" \
  "$arrays/src/main/java/jdk/m3/arrays/packed/AdaptivePackedIntArray.java"
cc -std=c11 -Wall -Wextra -Werror -Wconversion -Wshadow -Wpedantic \
  -I "$arrays/src/main/native/include" \
  "$arrays/src/main/native/m3_adaptive_bitpack.c" \
  "$arrays/src/main/native/tests/m3_adaptive_bitpack_checks.c" \
  -o "$arrays/target/adaptive/native-check"
"$arrays/target/adaptive/native-check"
echo M3_ADAPTIVE_BITPACK_PORTABLE_VERIFY_PASS
