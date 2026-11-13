#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Owner probe of qualification/census2-t3-20261106 (run after mvn package of m3/collections):
#  1. as-is: the held self test (donor bytes, package line mapped by the PACKAGE row) on the module owners -> its
#     first failure (the JUnit wrapper would fail the module suite);
#  2. probe: the same bytes with check/same/expect recording instead of throwing, run per mode (readonly-ready,
#     mutable-ready, readonly-reentrant, mutable-reentrant) -> every failing check, counted, plus any uncaught throw;
#  3. the observed rows must equal OWNER_VERDICTS.tsv (exit 1 otherwise: an owner change that makes the test pass
#     shows up here, and then the HOLD rows can be relocated).
# usage: owner_probe.sh <donor git dir> <donor rev> <out dir>
# env:   JAVA_HOME = JDK 21; M3_ARRAYS_JAR = the m3-arrays jar the module was built with; JVM_OPTS = extra options of
#        every java run (e.g. -Djava.io.tmpdir=<scratch>; javac gets them as -J options).
set -euo pipefail
export LC_ALL=C
DONOR=$1; REV=$2; OUT=$3
HERE=$(cd "$(dirname "$0")" && (pwd -W 2>/dev/null || pwd))
MODULE=$(cd "$HERE/../.." && (pwd -W 2>/dev/null || pwd))
MAP="$HERE/RELOCATION_MAP.tsv"
J="${JAVA_HOME:?JAVA_HOME}/bin"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';' ;; *) SEP=':' ;; esac
JAVAC_J=$(for o in ${JVM_OPTS:-}; do printf -- "-J%s " "$o"; done)
DONOR_PKG=$(awk -F'\t' '$1=="PACKAGE" {print $2; exit}' "$MAP"); HOST_PKG=$(awk -F'\t' '$1=="PACKAGE" {print $3; exit}' "$MAP")
SELF=$(awk -F'\t' '$1=="SKIP" && $4 ~ /^HOLD_OWNER_EDIT/ && $3 ~ /SelfTest\.java$/ {print $2; exit}' "$MAP")
TYPE=$(basename "$SELF" .java.txt)
OWNERCP="$MODULE/target/classes$SEP${M3_ARRAYS_JAR:-}"
rm -rf "$OUT"; mkdir -p "$OUT/asis/src" "$OUT/asis/classes" "$OUT/probe/src" "$OUT/probe/classes"
git -C "$DONOR" cat-file blob "$REV:$SELF" | sed "s/^package $DONOR_PKG;/package $HOST_PKG;/" > "$OUT/asis/src/$TYPE.java"
sed -e "s/\b$TYPE\b/${TYPE}Probe/g" \
    -e 's/^\(\s*\)if (!value) throw new AssertionError(label + " check=" + checks);/\1if (!value) System.out.println("PROBE_FAIL " + label);/' \
    -e 's/^\(\s*\)throw new AssertionError(label + " expected=" + expected + " actual=" + actual);/\1System.out.println("PROBE_FAIL " + label + " expected=" + expected + " actual=" + actual);/' \
    -e 's/^\(\s*\)throw new AssertionError(label + " expected=" + type + " actual=" + failure, failure);/\1{ System.out.println("PROBE_FAIL " + label + " expected=" + type.getSimpleName() + " actual=" + failure.getClass().getSimpleName()); return failure; }/' \
    -e 's/^\(\s*\)throw new AssertionError(label + " expected=" + type);/\1System.out.println("PROBE_FAIL " + label + " expected=" + type.getSimpleName() + " actual=none"); return null;/' \
    "$OUT/asis/src/$TYPE.java" > "$OUT/probe/src/${TYPE}Probe.java"
[ "$(grep -c 'PROBE_FAIL' "$OUT/probe/src/${TYPE}Probe.java")" -eq 4 ] || { echo "probe template drift: 4 recording helpers expected"; exit 2; }
for side in asis probe; do
  "$J/javac" $JAVAC_J --release 21 -nowarn -proc:none -encoding UTF-8 -cp "$OWNERCP" -d "$OUT/$side/classes" "$OUT/$side/src/"*.java
done
"$J/java" ${JVM_OPTS:-} -cp "$OUT/asis/classes$SEP$OWNERCP" "$HOST_PKG.$TYPE" > "$OUT/asis.out" 2>&1 || true
echo "as-is: $(grep -m1 -E 'Exception in thread|checks=' "$OUT/asis.out" | tr -d '\r')"
: > "$OUT/observed.tsv"
for mode in readonly-ready mutable-ready readonly-reentrant mutable-reentrant; do
  "$J/java" ${JVM_OPTS:-} -cp "$OUT/probe/classes$SEP$OWNERCP" "$HOST_PKG.${TYPE}Probe" "$mode" > "$OUT/$mode.out" 2>&1 || true
  tr -d '\r' < "$OUT/$mode.out" | awk -v m="$mode" '
    /^PROBE_FAIL / { sub(/^PROBE_FAIL /, ""); n[$0]++ }
    /^Exception in thread "main" / { sub(/^Exception in thread "main" /, ""); n["thrown " $0]++ }
    / checks=[0-9]+$/ { done = 1 }
    END { for (k in n) printf "%s\t%d\t%s\n", m, n[k], k; if (!done) printf "%s\t1\tno completion line\n", m }' | sort >> "$OUT/observed.tsv"
  echo "$mode: $(grep -c "^$mode	" "$OUT/observed.tsv") distinct failing checks"
done
grep -v '^#' "$HERE/OWNER_VERDICTS.tsv" | tail -n +2 > "$OUT/expected.tsv"
if cmp -s "$OUT/expected.tsv" "$OUT/observed.tsv"; then echo "OWNER PROBE = OWNER_VERDICTS.tsv (HOLD_OWNER_EDIT stands)"; else
  echo "OWNER PROBE DIFFERS from OWNER_VERDICTS.tsv"; diff "$OUT/expected.tsv" "$OUT/observed.tsv" || true; exit 1; fi
