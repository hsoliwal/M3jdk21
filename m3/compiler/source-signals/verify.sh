#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
mkdir -p "$work/classes/META-INF/services"
javac --release 21 -proc:none -Xlint:all -Werror -d "$work/classes" \
  "$here/src/main/java/com/m3/compiler/signals/M3SourceSignalPlugin.java" \
  "$here/src/test/java/com/m3/compiler/signals/M3SourceSignalPluginSelfTest.java"
cp "$here/src/main/resources/META-INF/services/com.sun.source.util.Plugin" \
  "$work/classes/META-INF/services/com.sun.source.util.Plugin"
java -cp "$work/classes" com.m3.compiler.signals.M3SourceSignalPluginSelfTest
