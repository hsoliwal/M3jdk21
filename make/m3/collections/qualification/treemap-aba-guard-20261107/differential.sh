#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Before/after differential of qualification/treemap-aba-guard-20261107 (recipe TREEMAP_ABA_GUARD v1):
#  1. compiles the filtered-views-owner-20261106 oracles (FilteredViewsOwnerDifferential, FilteredViewsTreeMapRepro)
#     and this leaf's M3TreeMapReentrantLookupSelfTest with -Xlint:all -Werror against the base and the head classes;
#  2. runs the seeded differential (SCENARIOS from SEED), the B4 repro and the re-entrant lookup shapes (report mode)
#     on both;
#  3. classifies every differing differential line with classify.py (exit 1 on any UNCLASSIFIED line, which includes
#     any JDK-owner difference) and prints the shape verdict counts of both sides.
# usage: differential.sh <base classes> <head classes> <m3-arrays jar> <out dir>
# env:   JAVA_HOME = JDK 21; JVM_OPTS = extra options of every java run (javac gets them as -J options);
#        SCENARIOS (default 4000), SEED (default 20261106)
set -euo pipefail
export LC_ALL=C
BASE=$1; HEAD=$2; ARRAYS=$3; OUT=$4
HERE=$(cd "$(dirname "$0")" && (pwd -W 2>/dev/null || pwd))
ORACLE="$HERE/../filtered-views-owner-20261106/oracle/jdk/m3/collections"
SHAPES="$HERE/../../src/test/java/jdk/m3/collections/M3TreeMapReentrantLookupSelfTest.java"
J="${JAVA_HOME:?JAVA_HOME}/bin"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';' ;; *) SEP=':' ;; esac
JAVAC_J=$(for o in ${JVM_OPTS:-}; do printf -- "-J%s " "$o"; done)
SCENARIOS=${SCENARIOS:-4000}; SEED=${SEED:-20261106}
rm -rf "$OUT"; mkdir -p "$OUT/base" "$OUT/head"
for side in base head; do
  classes=$BASE; [ "$side" = head ] && classes=$HEAD
  "$J/javac" $JAVAC_J --release 21 -Xlint:all -Werror -proc:none -encoding UTF-8 -cp "$classes$SEP$ARRAYS" \
      -d "$OUT/$side" "$ORACLE"/*.java "$SHAPES"
  run() { "$J/java" ${JVM_OPTS:-} -cp "$OUT/$side$SEP$classes$SEP$ARRAYS" "$@" | tr -d '\r'; }
  run jdk.m3.collections.FilteredViewsOwnerDifferential "$SCENARIOS" "$SEED" > "$OUT/$side.out"
  run jdk.m3.collections.FilteredViewsTreeMapRepro > "$OUT/repro-$side.out"
  run jdk.m3.collections.M3TreeMapReentrantLookupSelfTest report > "$OUT/shapes-$side.tsv"
  echo "$side: lines=$(wc -l < "$OUT/$side.out") sha256=$(sha256sum < "$OUT/$side.out" | cut -c1-16)" \
       "damaged-owners=$(grep ' end ' "$OUT/$side.out" | grep -vc 'verify=valid' || true)" \
       "shapes: $(tail -1 "$OUT/shapes-$side.tsv" | sed 's/^M3TreeMapReentrantLookupSelfTest //')" \
       "$(awk -F'\t' 'NF>3 {print $4}' "$OUT/shapes-$side.tsv" | sort | uniq -c | tr -s ' ' | tr '\n' ' ')"
done
echo "repro differences (base < > head):"; diff "$OUT/repro-base.out" "$OUT/repro-head.out" || true
python -I "$HERE/classify.py" "$OUT/base.out" "$OUT/head.out" "$OUT/DIFFERENTIAL.tsv"
