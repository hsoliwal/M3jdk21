#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
#
# mutant_check.sh <repo root> <make/m3/collections build dir>
#
# NATIVE_PROTOTYPE_MUTANT v1 (2026-11-09). Shows that the force-included javac -h header rejects a C definition whose
# signature drifted from its Java declaration. It copies m3_disjoint_index.c, changes `jint logicalSize` to
# `jlong logicalSize` in the JNI entry point, and compiles the copy twice with the build's flags: without the header
# (expected to compile, so the drift is invisible) and with it (expected "conflicting types"). Exit 0 only when both
# outcomes hold. Needs gcc or clang and the JDK 21 JNI headers under JAVA_HOME.
set -euo pipefail
root=$1
build=$2
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
source="$root/src/jdk.m3.collections/share/native/libm3collections/collections/m3_disjoint_index.c"
header="$build/jni/jdk.m3.collections/jdk_internal_m3_collections_DisjointIndex.h"
sed 's/jintArray parents, jint logicalSize,/jintArray parents, jlong logicalSize,/' "$source" > "$work/mutant.c"
if cmp -s "$source" "$work/mutant.c"; then echo "NATIVE_PROTOTYPE_MUTANT not applied"; exit 1; fi
platform=linux
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) platform=win32 ;; esac
flags=(-std=c11 -c -Wall -Wextra -Werror -Wshadow -Wpedantic -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/$platform")
without=killed
${CC:-gcc} "${flags[@]}" -o "$work/a.o" "$work/mutant.c" > "$work/without.log" 2>&1 && without=survived
with=survived
if ! ${CC:-gcc} "${flags[@]}" -include "$header" -o "$work/b.o" "$work/mutant.c" > "$work/with.log" 2>&1; then
  grep -q 'conflicting types' "$work/with.log" && with=killed
fi
echo "NATIVE_PROTOTYPE_MUTANT without_header $without with_header $with"
[ "$without" = survived ] && [ "$with" = killed ]
