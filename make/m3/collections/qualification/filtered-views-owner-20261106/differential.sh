#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Before/after differential of qualification/filtered-views-owner-20261106 (recipe FILTERED_VIEWS_OWNER v1):
#  1. compiles the two oracles (FilteredViewsOwnerDifferential, FilteredViewsTreeMapRepro) with -Xlint:all -Werror
#     against the base and the head module classes;
#  2. runs the seeded differential (SCENARIOS from SEED) and the B4 repro on both;
#  3. classifies every differing line with classify.py (exit 1 on any UNCLASSIFIED line) and counts the owners whose
#     final verifyInvariants fails on each side.
# usage: differential.sh <base classes> <head classes> <m3-arrays jar> <out dir>
# env:   JAVA_HOME = JDK 21; JVM_OPTS = extra options of every java run (javac gets them as -J options);
#        SCENARIOS (default 4000), SEED (default 20261106)
set -euo pipefail
export LC_ALL=C
BASE=$1; HEAD=$2; ARRAYS=$3; OUT=$4
HERE=$(cd "$(dirname "$0")" && (pwd -W 2>/dev/null || pwd))
J="${JAVA_HOME:?JAVA_HOME}/bin"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';' ;; *) SEP=':' ;; esac
JAVAC_J=$(for o in ${JVM_OPTS:-}; do printf -- "-J%s " "$o"; done)
SCENARIOS=${SCENARIOS:-4000}; SEED=${SEED:-20261106}
rm -rf "$OUT"; mkdir -p "$OUT/base" "$OUT/head"
for side in base head; do
  classes=$BASE; [ "$side" = head ] && classes=$HEAD
  "$J/javac" $JAVAC_J --release 21 -Xlint:all -Werror -proc:none -encoding UTF-8 -cp "$classes$SEP$ARRAYS" \
      -d "$OUT/$side" "$HERE"/oracle/jdk/m3/collections/*.java
  "$J/java" ${JVM_OPTS:-} -cp "$OUT/$side$SEP$classes$SEP$ARRAYS" jdk.m3.collections.FilteredViewsOwnerDifferential \
      "$SCENARIOS" "$SEED" | tr -d '\r' > "$OUT/$side.out"
  "$J/java" ${JVM_OPTS:-} -cp "$OUT/$side$SEP$classes$SEP$ARRAYS" jdk.m3.collections.FilteredViewsTreeMapRepro \
      | tr -d '\r' > "$OUT/repro-$side.out"
  echo "$side: lines=$(wc -l < "$OUT/$side.out") sha256=$(sha256sum < "$OUT/$side.out" | cut -c1-16)" \
       "damaged-owners=$(grep ' end ' "$OUT/$side.out" | grep -vc 'verify=valid' || true)"
done
echo "repro differences (base < > head):"; diff "$OUT/repro-base.out" "$OUT/repro-head.out" || true
python -I "$HERE/classify.py" "$OUT/base.out" "$OUT/head.out" "$OUT/DIFFERENTIAL.tsv"
