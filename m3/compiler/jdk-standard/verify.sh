#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Builds the M3JdkStandard javac plugin with plain javac, then compiles its own sources a second time with the
# plugin and -Xdoclint:all/protected (it must meet the standard it enforces), then runs the javac fixture self-test.
set -euo pipefail
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
main="$here/src/main/java/com/m3/compiler/standard/M3JdkStandardPlugin.java"
test="$here/src/test/java/com/m3/compiler/standard/M3JdkStandardPluginSelfTest.java"
mkdir -p "$work/classes/META-INF/services" "$work/self"
javac --release 21 -proc:none -Xlint:all -Werror -d "$work/classes" "$main" "$test"
cp "$here/src/main/resources/META-INF/services/com.sun.source.util.Plugin" \
  "$work/classes/META-INF/services/com.sun.source.util.Plugin"
javac --release 21 -proc:none -Xlint:all -Xdoclint:all/protected -Werror -d "$work/self" \
  -processorpath "$work/classes" -Xplugin:M3JdkStandard "$main" "$test"
java -cp "$work/classes" com.m3.compiler.standard.M3JdkStandardPluginSelfTest
