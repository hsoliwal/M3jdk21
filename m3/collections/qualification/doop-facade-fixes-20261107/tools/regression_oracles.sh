#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Unchanged-oracle gate of recipe qualification/doop-facade-fixes-20261107: replays the existing oracles of the
# neighbouring owners on MASTER and HEAD classes of m3/collections and requires byte-identical outputs:
#   #2565 CensusResidencyDifferential 20000 50, #2610 vpdiff.sh 2000 40, #2614 dsdiff.sh 20000 30,
#   #2653 psdiff.sh 14000 40 (all M3ObjectResidency), and #2566 differential.sh (transient-facade family: donor mains
#   and the 9-family M3TransientFacadeDump, JIT and -Xint) against the pinned donor head of leaf B (PROVENANCE.tsv).
# usage: regression_oracles.sh <module dir> <master classes> <head classes> <out dir> <donor-git>
# env: JAVA_HOME = a JDK 21; JTMP = java.io.tmpdir (also exported as JAVA_TOOL_OPTIONS for scripts that pass none);
#      M3_ARRAYS_JAR = the m3-arrays jar the module was built with.
set -uo pipefail
MOD=$1; MC=$2; HC=$3; OUT=$4; DONOR=$5
HERE=$(cd "$(dirname "$0")/.." && (pwd -W 2>/dev/null || pwd))
Q="$MOD/qualification"
J="${JAVA_HOME:?JAVA_HOME}/bin"
export JAVA_TOOL_OPTIONS="-Djava.io.tmpdir=${JTMP:?JTMP}"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';' ;; *) SEP=':' ;; esac
status=PASS
rm -rf "$OUT"; mkdir -p "$OUT/census"
same() { if cmp -s "$2" "$3"; then echo "$1 IDENTICAL sha256=$(sha256sum < "$2" | cut -c1-12) lines=$(wc -l < "$2")"; else echo "$1 DIFFERENT"; status=FAIL; fi; }
"$J/javac" --release 21 -Xlint:all -Werror -encoding UTF-8 -cp "$MC$SEP${M3_ARRAYS_JAR:-}" -d "$OUT/census" \
    "$Q/census-small-batch-20261106/oracle/jdk/m3/collections/CensusResidencyDifferential.java" 2>/dev/null || status=FAIL
for side in master head; do
    C=$MC; [ $side = head ] && C=$HC
    "$J/java" -cp "$OUT/census$SEP$C$SEP${M3_ARRAYS_JAR:-}" jdk.m3.collections.CensusResidencyDifferential \
        "$OUT/census-$side.txt" 20000 50 > /dev/null 2>&1 || status=FAIL
    bash "$Q/residency-victim-progress-20261106/oracle/vpdiff.sh" "$Q/residency-victim-progress-20261106/oracle" "$C" \
        "$OUT/vp-$side.txt" 2000 40 > "$OUT/vp-$side.log" 2>&1 || status=FAIL
done
same "#2565 CensusResidencyDifferential 20000x50" "$OUT/census-master.txt" "$OUT/census-head.txt"
same "#2610 vpdiff 2000x40" "$OUT/vp-master.txt" "$OUT/vp-head.txt"
bash "$Q/residency-donor-semantics-20261106/oracle/dsdiff.sh" "$MC" "$HC" "$OUT/ds" 20000 30 > "$OUT/ds.log" 2>&1
for f in "$OUT"/ds/*before*.txt; do [ -f "$f" ] && same "#2614 dsdiff 20000x30 $(basename "$f")" "$f" "${f/before/after}"; done
bash "$Q/residency-fold-20261107/oracle/psdiff.sh" "$MC" "$HC" "$OUT/ps" 14000 40 > "$OUT/ps.log" 2>&1
grep -E '^trace ' "$OUT/ps.log" | grep -E 'IDENTICAL|DIFFERENT' | sed 's/^/#2653 psdiff 14000x40 /'
grep -q 'trace normal IDENTICAL' "$OUT/ps.log" && grep -q 'trace xint IDENTICAL' "$OUT/ps.log" || status=FAIL
REV=$(awk -F'\t' '$1=="B.src" {print $6}' "$HERE/PROVENANCE.tsv")
bash "$MOD/recipe/transient-facade-20261106/differential.sh" "$DONOR" "$REV" "$OUT/facade" 100000 20261106 \
    > "$OUT/facade.log" 2>&1 || status=FAIL
grep -E 'IDENTICAL|DIFFERENT|DIFFERENTIAL' "$OUT/facade.log" | sed 's/ donor_sha256=.*//; s/^/#2566 differential.sh donor-head-vs-head /'
echo "REGRESSION_ORACLES $status"
[ "$status" = PASS ]
