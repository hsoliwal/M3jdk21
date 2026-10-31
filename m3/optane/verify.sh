#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors.
set -euo pipefail
ROOT=$(cd -- "$(dirname -- "$0")" && pwd)
JAVA_HOME=${M3_JDK:-${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")}}
export JAVA_HOME
OUT=${M3_OPTANE_BUILD:-"$ROOT/target"}
if [[ -e "$OUT" ]]; then echo "Refusing existing output: $OUT" >&2; exit 2; fi
mkdir -p "$OUT/classes" "$OUT/test" "$OUT/headers" "$OUT/lib" "$OUT/jmods"
"$JAVA_HOME/bin/java" -version
mapfile -t SOURCES < <(find "$ROOT/src" -name '*.java' -type f | LC_ALL=C sort)
"$JAVA_HOME/bin/javac" --release 21 -Xlint:all -Werror -h "$OUT/headers" -d "$OUT/classes" "${SOURCES[@]}"
"$JAVA_HOME/bin/javac" --release 21 -Xlint:all -Werror --module-path "$OUT/classes" --add-modules com.m3.optane -d "$OUT/test" "$ROOT/test/OptaneContractTest.java"
"$JAVA_HOME/bin/java" -Dm3.optane.disableNative=true --module-path "$OUT/classes" --add-modules com.m3.optane -cp "$OUT/test" OptaneContractTest
"$JAVA_HOME/bin/java" -Xint -Dm3.optane.disableNative=true --module-path "$OUT/classes" --add-modules com.m3.optane -cp "$OUT/test" OptaneContractTest
NATIVE_ARGS=()
if [[ $(uname -s) == Linux && ${M3_OPTANE_NO_NATIVE:-0} != 1 ]]; then
    if [[ $(uname -m) != x86_64 ]]; then
        echo "Native packaging currently qualified only for Linux x86_64; set M3_OPTANE_NO_NATIVE=1" >&2
        exit 2
    fi
    "${CC:-gcc}" -std=c17 -O2 -fPIC -shared -Wall -Wextra -Wpedantic -Wconversion -Werror \
      -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" -I"$OUT/headers" \
      "$ROOT/native/m3optane.c" -o "$OUT/lib/libm3optane.so"
    "$JAVA_HOME/bin/java" -Xcheck:jni -Djava.library.path="$OUT/lib" --module-path "$OUT/classes" --add-modules com.m3.optane -cp "$OUT/test" OptaneContractTest native
    "$JAVA_HOME/bin/java" -Xint -Xcheck:jni -Djava.library.path="$OUT/lib" --module-path "$OUT/classes" --add-modules com.m3.optane -cp "$OUT/test" OptaneContractTest native
    NATIVE_ARGS=(--libs "$OUT/lib" --target-platform linux-amd64)
fi
"$JAVA_HOME/bin/jar" --create --file "$OUT/com.m3.optane.jar" --date=2026-10-15T00:00:00Z --main-class com.m3.optane.OptaneDiagnostics --module-version 0.1.0 -C "$OUT/classes" . -C "$ROOT/legal" LICENSE -C "$ROOT/legal" NOTICE
"$JAVA_HOME/bin/jmod" create --class-path "$OUT/classes" --legal-notices "$ROOT/legal" --date=2026-10-15T00:00:00Z --module-version 0.1.0 --main-class com.m3.optane.OptaneDiagnostics "${NATIVE_ARGS[@]}" "$OUT/jmods/com.m3.optane.jmod"
"$JAVA_HOME/bin/jmod" describe "$OUT/jmods/com.m3.optane.jmod"
"$JAVA_HOME/bin/jlink" --module-path "$JAVA_HOME/jmods:$OUT/jmods" --add-modules com.m3.optane --launcher m3-optane=com.m3.optane/com.m3.optane.OptaneDiagnostics --output "$OUT/runtime"
"$OUT/runtime/bin/m3-optane" 64 2048 2 40
"$OUT/runtime/bin/java" --add-modules com.m3.optane -cp "$OUT/test" -Dm3.optane.disableNative=true OptaneContractTest
if [[ ${#NATIVE_ARGS[@]} -gt 0 ]]; then
    "$OUT/runtime/bin/java" -Xcheck:jni --add-modules com.m3.optane -cp "$OUT/test" OptaneContractTest native
fi
printf 'OPTANE_JMOD_BUILD_PASS\n'
