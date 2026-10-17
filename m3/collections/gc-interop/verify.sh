#!/bin/sh
# SPDX-License-Identifier: Apache-2.0
set -eu
module=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$module"
sha256sum -c SOURCES.sha256
build=$(mktemp -d)
trap 'rm -rf "$build"' EXIT HUP INT TERM
javac --release 21 -Xlint:all -Werror -d "$build" \
    src/main/java/com/m3/gc/*.java src/test/java/com/m3/gc/*.java
for collector in Serial Parallel G1 Z Shenandoah; do
    java -Xmx64m "-XX:+Use${collector}GC" -ea -cp "$build" \
        com.m3.gc.M3GcIntegrationProof
done
