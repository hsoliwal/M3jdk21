#!/bin/sh
# Compile M3JDK 21 java.base patches and the JNI/JVMTI probe against local JDK 21.
# This tests a subset, not a complete M3JDK21/HotSpot build.
set -eu
ROOT=$(CDPATH='' cd -- "$(dirname "$0")/../.." && pwd)
JAVA_HOME=${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")}
CLASSES="$ROOT/out/classes"
TESTS="$ROOT/out/tests"
mkdir -p "$CLASSES" "$TESTS"
javac --patch-module java.base="$ROOT/src/java.base/share/classes" -d "$CLASSES" \
    "$ROOT/src/java.base/share/classes/jdk/internal/m3/IndexedArray.java" \
    "$ROOT/src/java.base/share/classes/jdk/internal/m3/IndexedView.java" \
    "$ROOT/src/java.base/share/classes/jdk/internal/m3/IntAlgorithms.java" \
    "$ROOT/src/java.base/share/classes/jdk/internal/m3/ObjectMetrics.java" \
    "$ROOT/src/java.base/share/classes/jdk/internal/m3/ByteSlices.java" \
    "$ROOT/src/java.base/share/classes/jdk/internal/m3/CompositeBytes.java"
cc -std=c11 -Wall -Wextra -Werror -fPIC -shared \
   -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" \
   -o "$ROOT/out/libm3metrics.so" \
   "$ROOT/src/java.base/share/native/libjava/ObjectMetrics.c"
javac --patch-module java.base="$CLASSES" \
    --add-exports java.base/jdk.internal.m3=ALL-UNNAMED \
    -d "$TESTS" "$ROOT"/test/jdk/jdk/internal/m3/*.java
java --patch-module java.base="$CLASSES" \
    --add-exports java.base/jdk.internal.m3=ALL-UNNAMED \
    -cp "$TESTS" IndexedArrayTest
java --patch-module java.base="$CLASSES" \
    --add-exports java.base/jdk.internal.m3=ALL-UNNAMED \
    -cp "$TESTS" IntAlgorithmsTest
java --patch-module java.base="$CLASSES" \
    --add-exports java.base/jdk.internal.m3=ALL-UNNAMED \
    -cp "$TESTS" ByteSlicesTest
java --patch-module java.base="$CLASSES" \
    --add-exports java.base/jdk.internal.m3=ALL-UNNAMED \
    -cp "$TESTS" CompositeBytesTest
java --patch-module java.base="$CLASSES" \
    --add-exports java.base/jdk.internal.m3=ALL-UNNAMED \
    -Djdk.m3.native.path="$ROOT/out/libm3metrics.so" -cp "$TESTS" ObjectMetricsTest
