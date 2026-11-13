#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# R1 decision run (Linux, gcc, a JDK 21 for jni.h): (1) the retired test unmodified against master: compile;
# (2) retargeted REGION_SIZE -> M3_VIEWPORT_CHUNK: gcc -std=c17 -O2 -Wall -Wextra -Wpedantic -Werror, then run;
# (3) probe copy (make_probe.py: non-fatal assertions that print the failing line with the mock's counters, plus the
# DeleteLocalRef slot master's raise() needs and the retired mock table lacks): which assertions master violates.
# usage: TMPDIR=<tmp> JAVA_HOME=<jdk21> probe.sh <staged-dir>
set -uo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
cd "$1" || exit 9
JDK=${JAVA_HOME:?}
INC="-I$JDK/include -I$JDK/include/linux"
F="-std=c17 -O2 -Wall -Wextra -Wpedantic -Werror $INC"
mkdir -p out orig/src/test/native orig/src/main/c probe/src/test/native probe/src/main/c
cp regions.retired.c orig/src/test/native/m3_mask_viewport_regions.c; cp src/main/c/m3maskviewport.c orig/src/main/c/
echo "== 1. retired test unmodified, against master"
gcc $F orig/src/test/native/m3_mask_viewport_regions.c -o out/orig 2> out/orig.log && echo compiled || echo "COMPILE FAILED: $(grep -m1 -oE 'error: .*' out/orig.log)"
echo "== 2. retargeted to M3_VIEWPORT_CHUNK, against master"
gcc $F src/test/native/m3_mask_viewport_regions.c -o out/retargeted 2> out/retargeted.log; echo "gcc rc=$? diagnostics=$(grep -cE 'warning:|error:' out/retargeted.log)"
out/retargeted > out/retargeted.out 2> out/retargeted.err; echo "run rc=$? $(cat out/retargeted.out out/retargeted.err | head -2 | tr '\n' ' ')"
echo "== 3. probe: assertions master violates"
cp src/main/c/m3maskviewport.c probe/src/main/c/
python3 "$HERE/make_probe.py" src/test/native/m3_mask_viewport_regions.c probe/src/test/native/m3_mask_viewport_regions.c
gcc -std=c17 -O2 -Wall -Wextra $INC probe/src/test/native/m3_mask_viewport_regions.c -o out/probe 2> out/probe.log; echo "gcc rc=$?"
out/probe
