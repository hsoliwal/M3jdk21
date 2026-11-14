#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Tests of java.base's jdk.internal.m3.intern on a host JDK 21 (Linux), modelled on make/m3/compression/test-java-base.sh:
# the 8 product sources are staged alone and compiled into a java.base patch (-Xlint:all -Werror, the Maven module's
# flags); the tests share the package, so they are patched in too, with JUnit on the boot class path. Runs every JUnit
# class except the two whose oracle compiles fixtures with javax.tools: java.compiler belongs to the platform class
# loader and a class patched into java.base cannot link to it (NoClassDefFoundError), so ClassSpaceJUnitTest and
# ClassFileAtomsJUnitTest (and the JNI oracle of their self tests) run in the Maven bridge m3/intern/pom.xml on
# the class path. Both JNI oracles (test/jdk/jdk/internal/m3/intern/native) are built with -Wall -Wextra -Werror; the
# archive oracle runs here.
# usage: test-java-base.sh <junit-platform-console-standalone.jar> [work-dir]
set -euo pipefail
root=$(cd "$(dirname "$0")/../../.." && pwd)
junit=$1; work=${2:-$(mktemp -d)}
pkg=jdk.internal.m3.intern
dir=jdk/internal/m3/intern
main_src=$root/src/java.base/share/classes/$dir
test_src=$root/test/jdk/$dir
rm -rf "$work"; mkdir -p "$work/src/$dir" "$work/tsrc/$dir" "$work/main" "$work/test" "$work/native" "$work/h"
cp "$main_src"/*.java "$work/src/$dir/"
cp "$test_src"/*.java "$work/tsrc/$dir/"
# test scope only (class-space oracles): javax.tools in-memory javac fixtures and com.sun.management.ThreadMXBean
reads=(--add-reads java.base=ALL-UNNAMED,java.compiler,jdk.management --add-modules java.compiler,jdk.management)
javac -Xlint:all -Werror --patch-module java.base="$work/src" -d "$work/main" "$work/src/$dir"/*.java
javac --patch-module java.base="$work/main:$work/tsrc" "${reads[@]}" -cp "$junit" -h "$work/h" -d "$work/test" \
  "$work/tsrc/$dir"/*.java
java_home=$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")
for c in "$test_src"/native/*.c; do
  gcc -std=c11 -O2 -fPIC -shared -Wall -Wextra -Werror -I"$java_home/include" -I"$java_home/include/linux" -I"$work/h" \
    -o "$work/native/lib$(basename "$c" .c).so" "$c"
done
run=(java -Xbootclasspath/a:"$junit" --patch-module java.base="$work/main:$work/test" "${reads[@]}"
     --add-opens java.base/$pkg=ALL-UNNAMED -cp "$junit")
needs_compiler="ClassSpaceJUnitTest ClassFileAtomsJUnitTest"
junit_classes=$(cd "$test_src" && grep -l "@Test" *.java | sed "s/\.java$//" | grep -v -x -E "${needs_compiler// /|}" \
  | sed "s/^/--select-class=$pkg./")
for c in $needs_compiler; do echo "M3_INTERN_JAVA_BASE_SKIPPED $c (javax.tools fixture; runs in the Maven bridge)"; done
# shellcheck disable=SC2086
"${run[@]}" org.junit.platform.console.ConsoleLauncher execute $junit_classes \
  --disable-banner --details=summary --fail-if-no-tests
"${run[@]}" -Dm3.archive.native.library="$work/native/libm3_classspace_archive_native.so" \
  $pkg.ClassSpaceArchiveSelfTest > "$work/ClassSpaceArchiveSelfTest.log"
echo "M3_INTERN_MAIN native ClassSpaceArchiveSelfTest PASS $(tail -1 "$work/ClassSpaceArchiveSelfTest.log" | cut -c1-100)"
echo "M3_INTERN_JAVA_BASE_TESTS PASS"
