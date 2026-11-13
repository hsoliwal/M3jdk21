#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
: "${M3_JAVA_HOME:?set M3_JAVA_HOME to a Java 21 host JDK}"
CXX="${CXX:-g++}"
[[ "$(uname -s)" == Linux ]] || { echo 'This component proof is Linux-only.' >&2; exit 2; }
WORK="$(mktemp -d "${TMPDIR:-/tmp}/m3-tq-array.XXXXXX")"
trap 'rm -rf -- "$WORK"' EXIT
EVIDENCE="$ROOT/m3/runtime-integration/evidence/tq-primitive-composition-20261021"
(cd "$ROOT" && sha256sum -c "$EVIDENCE/INPUTS.sha256")
mkdir -p "$WORK/source/jdk/internal/mindex" "$WORK/classes" "$WORK/tests" "$WORK/headers"
for name in M3TQ.java M3StringBacking.java; do
 cp "$ROOT/src/java.base/share/classes/jdk/internal/mindex/$name" "$WORK/source/jdk/internal/mindex/"
done
"$M3_JAVA_HOME/bin/java" -version
"$CXX" --version
"$M3_JAVA_HOME/bin/javac" -Xlint:all -Werror --patch-module "java.base=$WORK/source" \
 -d "$WORK/classes" "$WORK/source/jdk/internal/mindex/"*.java
"$M3_JAVA_HOME/bin/javac" -Xlint:all -Werror --patch-module "java.base=$WORK/classes" \
 --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED -h "$WORK/headers" -d "$WORK/tests" \
 "$ROOT/test/jdk/java/lang/String/M3TQArrayCompositionTest.java" \
 "$ROOT/test/jdk/java/lang/String/TQSeamCounterexample.java"
"$CXX" -std=c++20 -O2 -Wall -Wextra -Werror -fPIC -shared \
 -fsanitize=undefined -fno-sanitize-recover=all \
 -I"$M3_JAVA_HOME/include" -I"$M3_JAVA_HOME/include/linux" \
 -include "$WORK/headers/M3TQArrayCompositionTest.h" \
 "$ROOT/test/jdk/java/lang/String/libM3TQTest.cpp" -o "$WORK/libM3TQTest.so"
JAVA=("$M3_JAVA_HOME/bin/java" -Xmx256m -Xcheck:jni --patch-module "java.base=$WORK/classes" \
 --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED -cp "$WORK/tests" "-Djava.library.path=$WORK")
"${JAVA[@]}" M3TQArrayCompositionTest
"${JAVA[@]}" M3TQArrayCompositionTest native
"${JAVA[@]}" -Xint M3TQArrayCompositionTest native
"${JAVA[@]}" TQSeamCounterexample
printf '%s\n' 'TQ_ARRAY_HOST_PASS; JNI is a test oracle; full JDK/jtreg/SDK/reactor/platform/pressure gates NOT_RUN'
