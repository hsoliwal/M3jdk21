#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Differential of qualification/census2-t1-20261106 (run after mvn package of m3/collections, tests compiled):
#  1. donor: each crate contract main, compiled in the donor package with its crate's owner postimage (the
#     masked-gather owner also needs M3IndexMaskCompactor of its crate base: the CONTEXT row of RELOCATION_MAP.tsv,
#     <commit>:<path> of this repository's own history) -> stdout;
#  2. relocated: the same contract main from the module test classes on the module owner classes -> stdout;
#  3. byte-identical stdout per contract (same check counts, same native flag), and the three JUnit wrappers pass.
# usage: differential.sh <donor git dir> <donor rev> <out dir>
# env:   JAVA_HOME = JDK 21; MAVEN_REPO_LOCAL = local repository holding junit-platform-console-standalone and the
#        junit-jupiter jars (default ~/.m2/repository); M3_ARRAYS_JAR = the m3-arrays jar the module was built with;
#        JVM_OPTS = extra options of every java run (e.g. -Djava.io.tmpdir=<scratch>; javac gets them as -J options).
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
JAVAC_J=$(for o in ${JVM_OPTS:-}; do printf -- "-J%s " "$o"; done)
CONTEXT=$(awk -F'\t' '$1=="SKIP" && $4 ~ /^CONTEXT:/ {print $3; exit}' "$MAP")
rm -rf "$OUT"; mkdir -p "$OUT"
status=0
for contract in $(awk -F'\t' '$1=="FILE" && $3 ~ /Contract\.java$/ {print $2}' "$MAP"); do
  crate=$(basename "$(dirname "$contract")"); type=$(basename "$contract" .java.txt)
  D="$OUT/$crate"; mkdir -p "$D/src" "$D/classes"
  git -C "$DONOR" cat-file blob "$REV:$contract" > "$D/src/$type.java"
  # the crate's owner postimages: SKIP rows naming a main source of this crate
  awk -F'\t' -v c="/$crate/" '$1=="SKIP" && index($2, c) && $2 !~ /\/pre\// && $3 ~ /^src\/main\/java\// {print $2}' "$MAP" | while read -r p; do
    git -C "$DONOR" cat-file blob "$REV:$p" > "$D/src/$(basename "$p" .java.txt).java"
  done
  if grep -q 'M3IndexMaskCompactor\.' "$D/src/"*.java && [ ! -f "$D/src/M3IndexMaskCompactor.java" ]; then
    git -C "$MODULE" cat-file blob "$CONTEXT" > "$D/src/M3IndexMaskCompactor.java"
  fi
  "$J/javac" $JAVAC_J --release 21 -nowarn -proc:none -encoding UTF-8 -d "$D/classes" "$D/src/"*.java
  "$J/java" ${JVM_OPTS:-} -cp "$D/classes" "$DONOR_PKG.$type" > "$D/donor.out" 2>&1 || true
  "$J/java" ${JVM_OPTS:-} -cp "$MODULE/target/test-classes$SEP$MODULE/target/classes$SEP${M3_ARRAYS_JAR:-}" "$HOST_PKG.$type" > "$D/relocated.out" 2>&1 || true
  if cmp -s "$D/donor.out" "$D/relocated.out" && grep -q '^PASS ' "$D/donor.out"; then v=IDENTICAL; else v=DIFFERS; status=1; fi
  echo "$type: donor [$(tr -d '\r\n' < "$D/donor.out")] relocated [$(tr -d '\r\n' < "$D/relocated.out")] $v"
done
"$J/java" ${JVM_OPTS:-} -jar "$CONSOLE" execute -cp "$MODULE/target/test-classes$SEP$MODULE/target/classes$SEP${M3_ARRAYS_JAR:-}$SEP$JUNIT" \
  $(awk -F'\t' '$1=="FILE" && $3 ~ /Test\.java$/ {n=$3; sub(/^.*\//, "", n); sub(/\.java$/, "", n); print "--select-class=" p "." n}' p="$HOST_PKG" "$MAP") \
  --disable-banner --details=summary > "$OUT/junit.log" 2>&1 || status=1
grep -E 'tests (successful|failed)' "$OUT/junit.log" | tr -s ' ' | sed 's/^/junit: /'
[ "$status" -eq 0 ] && echo "DIFFERENTIAL PASS" || { echo "DIFFERENTIAL FAIL"; exit 1; }
