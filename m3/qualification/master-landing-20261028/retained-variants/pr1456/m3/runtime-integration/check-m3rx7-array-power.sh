#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Copyright (c) 2026, Hitesh Soliwal and contributors.
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
: "${M3_JAVA_HOME:?set M3_JAVA_HOME to a Java 21 host JDK}"
CC="${CC:-cc}"
[[ "$(uname -s)" == Linux ]] || { echo 'This executed host proof is Linux-only; other platforms remain unqualified.' >&2; exit 2; }
WORK="$(mktemp -d "${TMPDIR:-/tmp}/m3rx7-array-power.XXXXXX")"
trap 'rm -rf -- "$WORK"' EXIT
(cd "$ROOT" && sha256sum -c - <<'SEALED'
fa5ecdb1a0c28813397343ee62d4ef5924d57f2738bb786e6e3af6aa99289821  src/java.base/share/classes/jdk/internal/mindex/M3RX7.java
4242a99703fc40e351030603a8faffebe03fb5509ae615c63744ed1c88693018  src/java.base/share/classes/jdk/internal/mindex/M3RX7Transitions.java
7a4967472aad46ac83d2862a53a5a8ee463be6750eb7a0b4ebcff3be0c4db9c1  src/java.base/share/native/libjava/M3RX7.c
0c7732f3836b65060763b12c7671063f1f32337ed98545e4449a444ce70f2af8  test/jdk/jdk/internal/mindex/M3RX7ArrayPowerContract.java
SEALED
)
mkdir -p "$WORK/source/jdk/internal/mindex" "$WORK/classes" "$WORK/headers"
for name in M3RX7.java M3RX7Transitions.java; do
 cp "$ROOT/src/java.base/share/classes/jdk/internal/mindex/$name" "$WORK/source/jdk/internal/mindex/"
done
"$M3_JAVA_HOME/bin/java" -version
"$CC" --version
"$M3_JAVA_HOME/bin/javac" -Xlint:all -Werror --patch-module "java.base=$WORK/source" -h "$WORK/headers" -d "$WORK/classes" "$WORK/source/jdk/internal/mindex/"*.java
"$CC" -std=c17 -O2 -Wall -Wextra -Werror -fPIC -shared -I"$M3_JAVA_HOME/include" -I"$M3_JAVA_HOME/include/linux" -I"$WORK/headers" "$ROOT/src/java.base/share/native/libjava/M3RX7.c" -o "$WORK/libm3rx7.so"
cp "$ROOT/test/jdk/jdk/internal/mindex/M3RX7ArrayPowerContract.java" "$WORK/source/jdk/internal/mindex/"
"$M3_JAVA_HOME/bin/javac" -Xlint:all -Werror --patch-module "java.base=$WORK/classes:$WORK/source" -d "$WORK/classes" "$WORK/source/jdk/internal/mindex/M3RX7ArrayPowerContract.java"
"$M3_JAVA_HOME/bin/java" -Xmx256m -Xcheck:jni --patch-module "java.base=$WORK/classes" jdk.internal.mindex.M3RX7ArrayPowerContract
"$M3_JAVA_HOME/bin/java" -Xint -Xmx256m -Xcheck:jni --patch-module "java.base=$WORK/classes" jdk.internal.mindex.M3RX7ArrayPowerContract
"$M3_JAVA_HOME/bin/java" -Xmx256m -Xcheck:jni --patch-module "java.base=$WORK/classes" jdk.internal.mindex.M3RX7ArrayPowerContract "$WORK/libm3rx7.so"
printf '%s\n' 'HOST_COMPONENT_PASS; source-built M3JDK21/jtreg/production libjava/SDK/reactor/platform gates NOT_RUN'
