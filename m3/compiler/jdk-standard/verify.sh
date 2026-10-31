#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Builds the plugin with plain javac, recompiles it under its own checks and doclint,
# then runs the real javac fixture self-test.
set -euo pipefail
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
main="$here/src/main/java/com/m3/compiler/standard/M3JdkStandardPlugin.java"
quality="$here/src/main/java/com/m3/compiler/standard/M3NameQuality.java"
test="$here/src/test/java/com/m3/compiler/standard/M3JdkStandardPluginSelfTest.java"
mkdir -p "$work/classes/META-INF/services" "$work/self"
javac --release 21 -proc:none -Xlint:all -Werror -d "$work/classes" \
  "$quality" "$main" "$test"
cp "$here/src/main/resources/META-INF/services/com.sun.source.util.Plugin" \
  "$work/classes/META-INF/services/com.sun.source.util.Plugin"
javac --release 21 -proc:none -Xlint:all -Xdoclint:all/protected -Werror -d "$work/self" \
  -processorpath "$work/classes" -Xplugin:M3JdkStandard "$quality" "$main" "$test"
java -cp "$work/classes" com.m3.compiler.standard.M3JdkStandardPluginSelfTest
