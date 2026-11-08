#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Tests of the arrays area of the JDK module jdk.m3.collections on a host JDK 21 (approved layout 2026-11-08):
# the module sources compile as a module (module-info included, -Xlint:all -Werror), the same-package JUnit 5 tests
# of test/jdk/jdk/m3/arrays compile and run on the class path exactly as the Maven module m3/arrays ran them
# (useModulePath=false), first without the native library, then, when a C toolchain is present, with the opt-in
# library built from src/jdk.m3.collections/share/native/libm3arrays through make/m3/arrays/CMakeLists.txt
# (-Dm3.arrays.native.path, as the Maven native-linux profile). Prints one summary line per lane and
# M3_ARRAYS_MODULE_TESTS PASS.
# usage: test-module.sh <junit-platform-console-standalone.jar> [work-dir]
set -euo pipefail
root=$(cd "$(dirname "$0")/../../.." && pwd)
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) root=$(cd "$root" && pwd -W) ;; esac   # Windows javac wants drive paths
junit=$1; work=${2:-$(mktemp -d)}
module=jdk.m3.collections
module_src=$root/src/$module/share/classes
test_src=$root/test/jdk/jdk/m3/arrays
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) sep=';'; junit=$(cd "$(dirname "$junit")" && pwd -W)/$(basename "$junit") ;; *) sep=':' ;; esac
rm -rf "$work"; mkdir -p "$work/module" "$work/main" "$work/test" "$work/native"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) work=$(cd "$work" && pwd -W) ;; esac
# 1. the module compiles as a module (descriptor + every package of the module)
find "$module_src" -name '*.java' > "$work/module-sources.txt"
javac -Xlint:all,-module -Werror -d "$work/module" @"$work/module-sources.txt"
echo "M3_ARRAYS_MODULE_COMPILE PASS $(wc -l < "$work/module-sources.txt" | tr -d ' ') sources"
# 2. the arrays packages on the class path (as Maven compiled them), then the tests against them
find "$module_src/jdk/m3/arrays" -name '*.java' > "$work/main-sources.txt"
javac -Xlint:all,-module -Werror -d "$work/main" @"$work/main-sources.txt"
find "$test_src" -name '*.java' > "$work/test-sources.txt"
javac -Xlint:all,-module -Werror -cp "$work/main$sep$junit" -d "$work/test" @"$work/test-sources.txt"
run=(java -cp "$work/main$sep$work/test$sep$junit")
# 3. the native lane is opt-in: a C toolchain builds the library; without one the native tests stay skipped
native_lib=""
if command -v cmake > /dev/null 2>&1 && { command -v gcc > /dev/null 2>&1 || command -v cc > /dev/null 2>&1; }; then
    # BUILD_TESTING=ON: the C unit tests (ctest) and the JNI fault fixtures the Java tests load from the library
    # directory; MinGW drops the lib prefix so System.mapLibraryName finds the fixtures on Windows too
    gen=(); case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) gen=(-G "MinGW Makefiles" -DCMAKE_SHARED_LIBRARY_PREFIX=) ;; esac
    cmake -S "$root/make/m3/arrays" -B "$work/native" "${gen[@]}" -DBUILD_TESTING=ON > "$work/native/configure.log" 2>&1 \
        && cmake --build "$work/native" > "$work/native/build.log" 2>&1 \
        && (cd "$work/native" && ctest --output-on-failure > ctest.log 2>&1) \
        && echo "M3_ARRAYS_NATIVE_CTEST PASS $(grep -c 'Passed' "$work/native/ctest.log") checks" \
        && { case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) for dll in "$work"/native/lib*.dll; do cp "$dll" "${dll%/*}/${dll##*/lib}"; done ;; esac; } \
        && native_lib=$(find "$work/native" -maxdepth 2 \( -name 'libm3_arrays.so' -o -name 'libm3_arrays.dylib' -o -name 'm3_arrays.dll' -o -name 'libm3_arrays.dll' \) | head -n 1) \
        || echo "M3_ARRAYS_NATIVE_BUILD SKIPPED (see $work/native/*.log)"
fi
for lane in none native; do
    props=()
    if [ "$lane" = native ]; then
        [ -n "$native_lib" ] || { echo "M3_ARRAYS_TESTS native SKIPPED (no library)"; continue; }
        props=("-Dm3.arrays.native.path=$native_lib")
    fi
    "${run[@]}" "${props[@]}" org.junit.platform.console.ConsoleLauncher execute --select-package jdk.m3.arrays \
        --disable-banner --details=summary --fail-if-no-tests > "$work/$lane.log" 2>&1 || { cat "$work/$lane.log"; exit 1; }
    echo "M3_ARRAYS_TESTS $lane $(grep -E 'tests (successful|failed|skipped|found)' "$work/$lane.log" | tr -s ' ' | tr '\n' ' ' | sed 's/\[//g; s/\]//g')"
done
echo "M3_ARRAYS_MODULE_TESTS PASS"
