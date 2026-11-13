#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Differential of qualification/census-g7-20261106 (run after mvn package of m3/collections, tests compiled):
#  1. donor: the full donor contract test class (44 tests) on the donor residency postimage of the same step, both in
#     the donor package -> per-test verdicts (every test must pass there);
#  2. probe: the same full donor class on the module owner (TYPE + MEMBER rows applied by sed, nothing dropped) ->
#     per-test verdicts; the failing set must equal the DROP_METHOD rows of RELOCATION_MAP.tsv exactly;
#  3. relocated: M3ObjectResidencyReuseContractTest (module test classes) -> every kept test passes.
# usage: differential.sh <donor git dir> <donor rev> <out dir>
# env:   JAVA_HOME = JDK 21; MAVEN_REPO_LOCAL = local repository holding junit-platform-console-standalone and the
#        junit-jupiter jars (default ~/.m2/repository); M3_ARRAYS_JAR = the m3-arrays jar the module was built with.
set -euo pipefail
DONOR=$1; REV=$2; OUT=$3
HERE=$(cd "$(dirname "$0")" && (pwd -W 2>/dev/null || pwd))
MODULE=$(cd "$HERE/../.." && (pwd -W 2>/dev/null || pwd))
MAP="$HERE/RELOCATION_MAP.tsv"
J="${JAVA_HOME:?JAVA_HOME}/bin"
M2=${MAVEN_REPO_LOCAL:-$HOME/.m2/repository}
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';' ;; *) SEP=':' ;; esac
CONSOLE=$(find "$M2/org/junit/platform/junit-platform-console-standalone" -name "*.jar" | sort | tail -1)
JUNIT=$(find "$M2/org/junit" "$M2/org/opentest4j" "$M2/org/apiguardian" -name "*.jar" | grep -v console-standalone | tr '\n' "$SEP")
DONOR_PKG=$(awk -F'\t' '$1=="PACKAGE" {print $2; exit}' "$MAP"); HOST_PKG=$(awk -F'\t' '$1=="PACKAGE" {print $3; exit}' "$MAP")
DONOR_TYPE=$(awk -F'\t' '$1=="TYPE" {print $2; exit}' "$MAP"); HOST_TYPE=$(awk -F'\t' '$1=="TYPE" {print $3; exit}' "$MAP")
DONOR_MEMBER=$(awk -F'\t' '$1=="MEMBER" {print $2; exit}' "$MAP"); HOST_MEMBER=$(awk -F'\t' '$1=="MEMBER" {print $3; exit}' "$MAP")
TEST=$(awk -F'\t' '$1=="FILE" {print $2; exit}' "$MAP")
OWNER=$(awk -F'\t' '$1=="SKIP" && $4 ~ /^L066 \(publication-budget main step/ {print $2; exit}' "$MAP")
rm -rf "$OUT"; mkdir -p "$OUT"/{donor-src,donor,probe-src,probe}
git -C "$DONOR" cat-file blob "$REV:$TEST" > "$OUT/donor-src/$DONOR_TYPE.java"
git -C "$DONOR" cat-file blob "$REV:$OWNER" > "$OUT/donor-src/M3ObjectResidency.java"
awk -F'	' '$1=="SKIP" && $4 ~ /^CONTEXT:/ {print $2}' "$MAP" | while read -r p; do
  git -C "$DONOR" cat-file blob "$REV:$p" > "$OUT/donor-src/$(basename "$p")"
done
"$J/javac" --release 21 -nowarn -proc:none -encoding UTF-8 -cp "$JUNIT" -d "$OUT/donor" "$OUT"/donor-src/*.java
sed "s/^package $DONOR_PKG;/package $HOST_PKG;/; s/\b$DONOR_TYPE\b/${HOST_TYPE}Probe/g; s/\b$DONOR_MEMBER\b/$HOST_MEMBER/g" \
    "$OUT/donor-src/$DONOR_TYPE.java" > "$OUT/probe-src/${HOST_TYPE}Probe.java"
"$J/javac" --release 21 -nowarn -proc:none -encoding UTF-8 -cp "$MODULE/target/classes$SEP${M3_ARRAYS_JAR:-}$SEP$JUNIT" -d "$OUT/probe" "$OUT/probe-src/${HOST_TYPE}Probe.java"
run() { # label classpath class
  "$J/java" -jar "$CONSOLE" execute -cp "$2" --select-class "$3" --disable-banner --details=none --reports-dir "$OUT/rep-$1" > "$OUT/$1.log" 2>&1 || true
  python -I - "$OUT/rep-$1" > "$OUT/$1.tsv" <<'PY'
import glob, sys, xml.etree.ElementTree as ET
rows = []
for p in glob.glob(sys.argv[1] + "/*.xml"):
    for tc in ET.parse(p).getroot().iter("testcase"):
        bad = tc.find("failure") if tc.find("failure") is not None else tc.find("error")
        rows.append((tc.get("name").split("(")[0], "PASS" if bad is None else "FAIL"))
for name, verdict in sorted(rows):
    print(name + "\t" + verdict)
PY
}
run donor "$OUT/donor$SEP$JUNIT" "$DONOR_PKG.$DONOR_TYPE"
run probe "$OUT/probe$SEP$MODULE/target/classes$SEP${M3_ARRAYS_JAR:-}" "$HOST_PKG.${HOST_TYPE}Probe"
run relocated "$MODULE/target/test-classes$SEP$MODULE/target/classes$SEP${M3_ARRAYS_JAR:-}" "$HOST_PKG.$HOST_TYPE"
awk -F'\t' '$1=="DROP_METHOD" {sub(/^[^.]*\./, "", $2); print $2}' "$MAP" | sort > "$OUT/dropped.txt"
awk -F'\t' '$2=="FAIL" {print $1}' "$OUT/probe.tsv" | sort > "$OUT/probe-fail.txt"
echo "donor: $(grep -c PASS "$OUT/donor.tsv") pass / $(wc -l < "$OUT/donor.tsv") tests"
echo "probe on owner: $(grep -c PASS "$OUT/probe.tsv") pass, $(grep -c FAIL "$OUT/probe.tsv" || true) fail; DROP rows $(wc -l < "$OUT/dropped.txt")"
echo "relocated: $(grep -c PASS "$OUT/relocated.tsv") pass / $(wc -l < "$OUT/relocated.tsv") tests"
if grep -q FAIL "$OUT/donor.tsv" || ! cmp -s "$OUT/dropped.txt" "$OUT/probe-fail.txt" || grep -q FAIL "$OUT/relocated.tsv" \
    || [ "$(wc -l < "$OUT/relocated.tsv")" -ne $(( $(wc -l < "$OUT/donor.tsv") - $(wc -l < "$OUT/dropped.txt") )) ]; then
  echo "DIFFERENTIAL FAIL"; exit 1; fi
echo "DIFFERENTIAL PASS kept=$(wc -l < "$OUT/relocated.tsv") dropped=$(wc -l < "$OUT/dropped.txt") (failing set on the owner = DROP rows)"
