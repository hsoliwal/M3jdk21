#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Before/after differential of qualification/mergeloss2-filtered-views-20261107 (recipe MERGELOSS2_FILTERED_VIEWS v1):
#  1. compiles oracle/jdk/m3/collections/MergelossFilteredViewsDifferential.java with -Xlint:all -Werror against the
#     base and the head module classes;
#  2. runs the seeded differential (SCENARIOS from SEED) on both;
#  3. classifies every differing line with classify.py (exit 1 on any UNCLASSIFIED line).
# The single-layer scenarios of ../filtered-views-owner-20261106/differential.sh cover M3MutableFilteredViews and the
# one-layer read-only views; run it with the same base/head classes as well (GATES.tsv).
# usage: differential.sh <base classes> <head classes> <m3-arrays jar> <out dir>
# env:   JAVA_HOME = JDK 21; JVM_OPTS = extra options of every java run (javac gets them as -J options);
#        SCENARIOS (default 2000), SEED (default 20261107)
set -euo pipefail
export LC_ALL=C
BASE=$1; HEAD=$2; ARRAYS=$3; OUT=$4
HERE=$(cd "$(dirname "$0")" && (pwd -W 2>/dev/null || pwd))
J="${JAVA_HOME:?JAVA_HOME}/bin"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';' ;; *) SEP=':' ;; esac
JAVAC_J=$(for o in ${JVM_OPTS:-}; do printf -- "-J%s " "$o"; done)
SCENARIOS=${SCENARIOS:-2000}; SEED=${SEED:-20261107}
rm -rf "${OUT:?}"; mkdir -p "$OUT/base" "$OUT/head"
for side in base head; do
  classes=$BASE; [ "$side" = head ] && classes=$HEAD
  "$J/javac" $JAVAC_J --release 21 -Xlint:all -Werror -proc:none -encoding UTF-8 -cp "$classes$SEP$ARRAYS" \
      -d "$OUT/$side" "$HERE"/oracle/jdk/m3/collections/MergelossFilteredViewsDifferential.java
  "$J/java" ${JVM_OPTS:-} -cp "$OUT/$side$SEP$classes$SEP$ARRAYS" jdk.m3.collections.MergelossFilteredViewsDifferential \
      "$SCENARIOS" "$SEED" | tr -d '\r' > "$OUT/$side.out"
  echo "$side: lines=$(wc -l < "$OUT/$side.out") sha256=$(sha256sum < "$OUT/$side.out" | cut -c1-16)"
done
python -I "$HERE/classify.py" "$OUT/base.out" "$OUT/head.out" "$OUT/DIFFERENTIAL.tsv"
