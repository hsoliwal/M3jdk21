#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Differential of qualification/census2-t2-20261106 (run after mvn package of m3/collections, tests compiled):
#  P2-122  the crate's mutable-list model probe (java.util.ArrayList<Long> reference, FNV trace digest) on the donor
#          inline list postimage vs the relocated probe on M3PackedLongInlineList: byte-identical stdout, JIT 650
#          rounds and -Xint 100 rounds (the crate's full_owner_parity.py modes);
#  P2-128  each donor policy main (java -ea) on the donor family vs the relocated proof main on the module family:
#          identical exit status and output;
#  P2-135  the crate oracles on the donor postimages (all PASS), then the same oracles as owner probes: the view
#          constructor becomes OwnerProbe.view(...) = <owner>.<factory>(Collection, Predicate) for M3FilteredViews.filter
#          and M3CollectionViews.filtered, check() records instead of throwing; the observed per-oracle verdicts must
#          equal OWNER_VERDICTS.tsv.
# Every donor path is read from RELOCATION_MAP.tsv (FILE rows and SKIP CONTEXT rows); the donor package names are
# read from the donor sources.
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
JAVAC_J=$(for o in ${JVM_OPTS:-}; do printf -- "-J%s " "$o"; done)
HOST_PKG=$(awk -F'\t' '$1=="PACKAGE" && $2!="-" {print $3; exit}' "$MAP")
MODCP="$MODULE/target/test-classes$SEP$MODULE/target/classes$SEP${M3_ARRAYS_JAR:-}"
blob() { git -C "$DONOR" cat-file blob "$REV:$1"; }
javac21() { "$J/javac" $JAVAC_J --release 21 -nowarn -proc:none -encoding UTF-8 "$@"; }
pkg_of() { sed -n 's/^package \([A-Za-z0-9_.]*\);.*/\1/p' "$1" | head -1; }
rm -rf "$OUT"; mkdir -p "$OUT"
status=0

# ---- P2-122 + P2-128: donor closure in the donor package ----
D="$OUT/donor-family"; mkdir -p "$D/src" "$D/classes"
awk -F'\t' '$1=="FILE" {print $2}' "$MAP" | while read -r p; do
  b=$(basename "$p"); blob "$p" > "$D/src/${b%.txt}"
done
awk -F'\t' '$1=="SKIP" && $4 ~ /^CONTEXT P2-122 P2-128:/ {print $2}' "$MAP" | while read -r p; do
  blob "$p" > "$D/src/$(basename "$p")"
done
javac21 -d "$D/classes" "$D/src/"*.java
DONOR_PKG=$(pkg_of "$D/src/ForwardingLongSequence.java")
PROBE=$(awk -F'\t' '$1=="WRAP" && $4!="-" {print $2; exit}' "$MAP")
for mode in "650" "-Xint 100"; do
  set -- $mode; flags=""; [ $# -eq 2 ] && { flags=$1; shift; }
  "$J/java" ${JVM_OPTS:-} $flags -cp "$D/classes" "$PROBE" "$1" > "$D/probe-donor-$1.out" 2>&1 || true
  "$J/java" ${JVM_OPTS:-} $flags -cp "$MODCP" "$HOST_PKG.$PROBE" "$1" > "$D/probe-relocated-$1.out" 2>&1 || true
  if cmp -s "$D/probe-donor-$1.out" "$D/probe-relocated-$1.out" && grep -q '_PASS ' "$D/probe-donor-$1.out"; then v=IDENTICAL; else v=DIFFERS; status=1; fi
  echo "P2-122 probe ${flags:-jit} $1: donor [$(tr -d '\r\n' < "$D/probe-donor-$1.out")] relocated [$(tr -d '\r\n' < "$D/probe-relocated-$1.out")] $v"
done
awk -F'\t' '$1=="FILE" && $3 ~ /Proof\.java$/ && $2 ~ /Test\.java\.txt$/ {print $2 "\t" $3}' "$MAP" | while IFS=$'\t' read -r p t; do
  dt=$(basename "$p" .java.txt); ht=$(basename "$t" .java)
  set +e
  "$J/java" ${JVM_OPTS:-} -ea -cp "$D/classes" "$DONOR_PKG.$dt" > "$D/$dt.donor.out" 2>&1; de=$?
  "$J/java" ${JVM_OPTS:-} -ea -cp "$MODCP" "$HOST_PKG.$ht" > "$D/$dt.relocated.out" 2>&1; re=$?
  set -e
  if [ "$de" -eq 0 ] && [ "$re" -eq 0 ] && cmp -s "$D/$dt.donor.out" "$D/$dt.relocated.out"; then v=IDENTICAL; else v=DIFFERS; echo FAIL > "$D/p128.fail"; fi
  echo "P2-128 $dt -> $ht: donor exit=$de relocated exit=$re output_bytes=$(wc -c < "$D/$dt.donor.out")/$(wc -c < "$D/$dt.relocated.out") $v"
done
[ -f "$D/p128.fail" ] && status=1

# ---- P2-135: crate oracles on the donor postimages, then owner probes ----
P="$OUT/p135"; mkdir -p "$P/src" "$P/donor"
awk -F'\t' '$1=="SKIP" && $4 ~ /^CONTEXT P2-135 / {print $2}' "$MAP" | while read -r p; do
  b=$(basename "$p"); blob "$p" > "$P/src/${b%.txt}"
done
javac21 -d "$P/donor" "$P/src/"*.java
VIEW_PKG=$(pkg_of "$P/src/FilteredCollectionView.java")
PROOFS=$(awk -F'\t' '$1=="SKIP" && $4 ~ /^CONTEXT P2-135 PROOF:/ {n=$2; sub(/^.*\//, "", n); sub(/\.java\.txt$/, "", n); print n}' "$MAP")
HISTORY=$(awk -F'\t' '$1=="SKIP" && $4 ~ /^CONTEXT P2-135 HISTORY:/ {n=$2; sub(/^.*\//, "", n); sub(/\.java$/, "", n); printf "%s|", n}' "$MAP")
for proof in $PROOFS; do
  "$J/java" ${JVM_OPTS:-} -Xss512k -cp "$P/donor" "$VIEW_PKG.$proof" > "$P/$proof.donor.out" 2>&1 || true
  echo "P2-135 donor $proof: $(grep -E '_PASS checks=' "$P/$proof.donor.out" | tail -1 | tr -d '\r')"
  grep -q 'Exception\|Error' "$P/$proof.donor.out" && status=1
done
summarize() { # out file -> verdict<TAB>details (normalised, sorted)
  python -I - "$1" <<'PY'
import re, sys
text = open(sys.argv[1], encoding="utf-8", errors="replace").read().splitlines()
fails = {}
for line in text:
    if line.startswith("PROBE_FAIL "):
        key = re.sub(r"depth=\d+ seed=\d+ query=-?\d+", "depth=* seed=* query=*", line[len("PROBE_FAIL "):].strip())
        fails[key] = fails.get(key, 0) + 1
thrown = sorted({m.group(1) for line in text for m in [re.match(r"Exception in thread \"main\" ([\w.$]+)", line)] if m})
passed = [line.strip() for line in text if re.search(r"_PASS checks=\d+$", line.strip())]
details = ["%dx %s" % (count, key) for key, count in sorted(fails.items())] + ["thrown " + name for name in thrown]
verdict = "PASS" if passed and not details else "FAIL"
print(verdict + "\t" + ("; ".join(details) if details else passed[-1] if passed else "no output"))
PY
}
: > "$P/observed.tsv"
for owner in "M3FilteredViews filter" "M3CollectionViews filtered"; do
  set -- $owner; O="$P/probe-$1"; mkdir -p "$O/src" "$O/classes"
  printf 'package %s;\nfinal class OwnerProbe {\n    private OwnerProbe() { }\n    static <E> java.util.Collection<E> view(java.util.Collection<E> source, java.util.function.Predicate<? super E> predicate) {\n        return %s.%s.%s(source, predicate);\n    }\n}\n' \
    "$VIEW_PKG" "$HOST_PKG" "$1" "$2" > "$O/src/OwnerProbe.java"
  for proof in $PROOFS; do
    sed -e "s/\b$proof\b/${proof}Probe/g" \
        -e 's/throw new AssertionError(message)/System.out.println("PROBE_FAIL " + message)/' \
        -e '/overridden = new FilteredCollectionView/!s/new FilteredCollectionView<>(/OwnerProbe.view(/' \
        -e 's/FilteredCollectionView<Integer> \(custom\|sortedView\) =/java.util.Collection<Integer> \1 =/' \
        -e "/^        \(${HISTORY%|}\)\.main(new String\[0\]);\$/d" \
        -e '/^        Filtered[A-Za-z]*Proof\.main(new String\[0\]);$/d' \
        "$P/src/$proof.java" > "$O/src/${proof}Probe.java"
  done
  javac21 -cp "$P/donor$SEP$MODULE/target/classes" -d "$O/classes" "$O/src/"*.java
  for proof in $PROOFS; do
    "$J/java" ${JVM_OPTS:-} -Xss512k -cp "$O/classes$SEP$P/donor$SEP$MODULE/target/classes" "$VIEW_PKG.${proof}Probe" > "$O/$proof.out" 2>&1 || true
    printf '%s\t%s\t%s\n' "$1" "$proof" "$(summarize "$O/$proof.out")" >> "$P/observed.tsv"
  done
done
grep -v '^#' "$HERE/OWNER_VERDICTS.tsv" | tail -n +2 > "$P/expected.tsv"
tr -d '\r' < "$P/observed.tsv" > "$P/observed.lf.tsv"
cut -f1-3 "$P/observed.lf.tsv" | sed 's/^/P2-135 owner probe: /'
if cmp -s "$P/expected.tsv" "$P/observed.lf.tsv"; then echo "P2-135 owner verdicts = OWNER_VERDICTS.tsv"; else echo "P2-135 owner verdicts DIFFER from OWNER_VERDICTS.tsv"; diff "$P/expected.tsv" "$P/observed.lf.tsv" || true; status=1; fi

# ---- reference model (java.util.ArrayList) of the adapted boolean add/addAll answers ----
R="$OUT/return-model"; mkdir -p "$R/src" "$R/classes"
cp "$HERE/differential/M3LongSequenceReturnModel.java.tmpl" "$R/src/M3LongSequenceReturnModel.java"
javac21 -cp "$MODULE/target/classes" -d "$R/classes" "$R/src/M3LongSequenceReturnModel.java"
"$J/java" ${JVM_OPTS:-} -cp "$R/classes$SEP$MODULE/target/classes" "$HOST_PKG.M3LongSequenceReturnModel" > "$R/out.txt" 2>&1 || true
echo "reference model: $(tr -d '\r' < "$R/out.txt" | head -3 | tr '\n' ' ')"
grep -q '_PASS checks=' "$R/out.txt" || status=1

# ---- JUnit wrappers of this group ----
"$J/java" ${JVM_OPTS:-} -jar "$CONSOLE" execute -cp "$MODCP$SEP$JUNIT" \
  $(awk -F'\t' '$1=="WRAP" {n=$3; sub(/^.*\//, "", n); sub(/\.java$/, "", n); print "--select-class=" p "." n}' p="$HOST_PKG" "$MAP") \
  --disable-banner --details=summary > "$OUT/junit.log" 2>&1 || status=1
grep -E 'tests (successful|failed)' "$OUT/junit.log" | tr -s ' ' | sed 's/^/junit: /'
[ "$status" -eq 0 ] && echo "DIFFERENTIAL PASS" || { echo "DIFFERENTIAL FAIL"; exit 1; }
