#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Smoke tests of java.base's jdk.internal.m3.indexstring on a host JDK 21, modelled on make/m3/intern/test-java-base.sh.
# The package's 19 product sources are staged with what they need from M3JDK's java.base that a host JDK lacks
# (jdk.m3.precompute, jdk.internal.m3.intern, java.util.concurrent.ProgressMonitor and ProgressMonitors) and compiled
# into a java.base patch with the java.base flags (-Xlint:all,-this-escape -Werror). jdk.m3.text.compat.M3Text stays in
# m3/ports/indexstring; it is compiled outside the patch and appended to the boot class path. The tests share the
# package, so they are patched in too. Runs the six mains the former Maven build ran; each throws on failure.
# usage: test-java-base.sh [work-dir]     (Linux, and Windows Git Bash for local runs)
set -euo pipefail
here=$(cd "$(dirname "$0")/../../.." && pwd)
case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*) sep=';'; root=$(cd "$here" && pwd -W) ;;
  *) sep=':'; root=$here ;;
esac
work=${1:-$(mktemp -d)}
classes=$root/src/java.base/share/classes
pkg=jdk.internal.m3.indexstring
dir=jdk/internal/m3/indexstring
rm -rf "$work"; mkdir -p "$work/src" "$work/tsrc" "$work/main" "$work/compat" "$work/test"
stage() { (cd "$classes" && find "$@" -name '*.java' -print0 | xargs -0 cp --parents -t "$work/src"); }
stage "$dir" jdk/m3/precompute jdk/internal/m3/intern java/util/concurrent/ProgressMonitor.java \
  java/util/concurrent/ProgressMonitors.java
(cd "$root/test/jdk" && find "$dir" -name '*.java' -print0 | xargs -0 cp --parents -t "$work/tsrc")
exports=(--add-exports java.base/$pkg=ALL-UNNAMED --add-exports java.base/$pkg.type=ALL-UNNAMED)
mapfile -t main_files < <(cd "$work/src" && find . -name '*.java' | sed "s#^\./#$work/src/#" | sort)
javac -Xlint:all,-this-escape -Werror --patch-module java.base="$work/src" -d "$work/main" "${main_files[@]}"
echo "M3_INDEXSTRING_MAIN compiled ${#main_files[@]} sources into the java.base patch"
javac -Xlint:all -Werror --patch-module java.base="$work/main" "${exports[@]}" -d "$work/compat" \
  "$root/m3/ports/indexstring/src/main/java/jdk/m3/text/compat/M3Text.java"
mapfile -t test_files < <(cd "$work/tsrc" && find . -name '*.java' | sed "s#^\./#$work/tsrc/#" | sort)
javac --patch-module java.base="$work/main$sep$work/tsrc" --add-reads java.base=ALL-UNNAMED "${exports[@]}" \
  -cp "$work/compat" -d "$work/test" "${test_files[@]}"
# the tests run in java.base (boot loader), so M3Text goes on the boot class path, as JUnit does in the intern script
run=(java -Xbootclasspath/a:"$work/compat" --patch-module java.base="$work/main$sep$work/test"
     --add-reads java.base=ALL-UNNAMED "${exports[@]}")
for main in ExistingSurfaceTest RetainedViewTest MIndexTrieCoreSmokeMain MIndexWordBreakSmokeMain \
    type.MIndexClassPoolSmokeMain M3IndexCharacterFlagsCombineSmokeMain; do
  "${run[@]}" "$pkg.$main" > "$work/$main.log" 2>&1 || { cat "$work/$main.log"; echo "M3_INDEXSTRING_FAIL $main"; exit 1; }
  echo "M3_INDEXSTRING_MAIN $main PASS $(tail -1 "$work/$main.log" | cut -c1-100)"
done
echo "M3_INDEXSTRING_JAVA_BASE_TESTS PASS"
