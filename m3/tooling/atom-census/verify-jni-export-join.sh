#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
TARGET="$HERE/target/jni-export-join-verification"
mkdir -p "$TARGET/classes" "$TARGET/headers" "$TARGET/native-gcc" "$TARGET/native-clang"
MAIN="$HERE/src/main/java/jdk/m3/tooling/atoms"
TEST="$HERE/src/test/java/jdk/m3/tooling/atoms"
FIXTURE="$HERE/test-fixtures/jni-export-join"
javac --release 21 -Xlint:all -Werror -d "$TARGET/classes" \
  "$MAIN/contract/M3AtomKind.java" \
  "$MAIN/M3AtomCensus.java" "$MAIN/M3JniDeclarationProjection.java" \
  "$MAIN/M3JniExportJoin.java" "$TEST/M3JniExportJoinSelfTest.java"
java -cp "$TARGET/classes" jdk.m3.tooling.atoms.M3JniExportJoinSelfTest \
  | tee "$TARGET/selftest.log"
grep -q $'M3_JNI_EXPORT_JOIN_PROOF\tchecks=28\tstatus=PASS' "$TARGET/selftest.log"
javac --release 21 -Xlint:all -Werror -d "$TARGET/classes" \
  "$FIXTURE/demo/pkg/Outer.java" "$FIXTURE/JniJoinRuntime.java"
javac -h "$TARGET/headers" -d "$TARGET/classes" "$FIXTURE/demo/pkg/Outer.java"
JDK="${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")}";
for compiler in cc clang; do
  command -v "$compiler" >/dev/null || { echo "M3_JNI_MISSING_COMPILER:$compiler" >&2; exit 2; }
  label=$([ "$compiler" = cc ] && echo gcc || echo clang)
  "$compiler" -std=c17 -Wall -Wextra -Werror -pedantic -fPIC -shared \
    -I"$JDK/include" -I"$JDK/include/linux" \
    "$FIXTURE/m3jni_export_join.c" -o "$TARGET/native-$label/libm3jni_projection.so"
  nm -D --defined-only "$TARGET/native-$label/libm3jni_projection.so" \
    | grep -F 'Java_demo_pkg_Outer_overloaded___3B' >/dev/null
  for mode in default -Xint -Xcomp; do
    args=(); [ "$mode" = default ] || args=("$mode")
    java -Xcheck:jni "${args[@]}" \
      -Djava.library.path="$TARGET/native-$label" -cp "$TARGET/classes" JniJoinRuntime
    printf 'M3_JNI_MODE_PASS|compiler=%s|mode=%s\n' "$label" "$mode"
  done
done
