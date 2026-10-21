#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
cd "$(dirname "$0")/../../../.."
classes="$(mktemp -d)"
trap 'rm -rf "$classes"' EXIT
java_dir="m3/collections/src/main/java"
# Compiles against ACTUAL product interfaces and underlying M3LongArrayList / M3LongArrayDeque.
javac --release 21 -proc:none -Xlint:all,-module -Werror \
  -sourcepath "$java_dir:m3/collections/src/test/java" -d "$classes" \
  "$java_dir/com/m3/collections/M3PrimitiveFilteredSequences.java" \
  "m3/collections/src/test/java/com/m3/collections/M3PrimitiveFilteredSequencesRuntimeContract.java"
java -Xmx256m -cp "$classes" com.m3.collections.M3PrimitiveFilteredSequencesRuntimeContract
