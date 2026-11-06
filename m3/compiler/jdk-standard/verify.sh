#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Builds the javac plugin and parse-only gates, recompiles them under their own checks and doclint,
# then runs the real javac, census and changed-line self-tests.
set -euo pipefail
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
mapfile -t main_sources < <(find "$here/src/main/java" -type f -name '*.java' | sort)
mapfile -t self_tests < <(find "$here/src/test/java" -type f -name '*SelfTest.java' | sort)
mkdir -p "$work/classes/META-INF/services" "$work/self"
javac --release 21 -proc:none -Xlint:all -Werror -d "$work/classes" \
  "${main_sources[@]}" "${self_tests[@]}"
cp "$here/src/main/resources/META-INF/services/com.sun.source.util.Plugin" \
  "$work/classes/META-INF/services/com.sun.source.util.Plugin"
javac --release 21 -proc:none -Xlint:all -Xdoclint:all/protected -Werror -d "$work/self" \
  -processorpath "$work/classes" -Xplugin:M3JdkStandard \
  "${main_sources[@]}" "${self_tests[@]}"
java -cp "$work/classes" jdk.m3.compiler.standard.M3JdkStandardPluginSelfTest
java -cp "$work/classes" jdk.m3.compiler.standard.M3JdkStandardCensusSelfTest
java -cp "$work/classes" jdk.m3.compiler.standard.M3ChangedNameGateSelfTest
