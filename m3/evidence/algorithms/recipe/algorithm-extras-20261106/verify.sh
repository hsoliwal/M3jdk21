#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Runs the gates of GATES.tsv in order (derived from recipe/graph-trie-20261106/verify.sh): the fixed points of the
# three recipes and of the ledger rows, the jdk-naming plan of the module, Maven package (-Xlint:all -Werror; the
# oracles of all three leaves, the reference oracles of this leaf included, in the test phase), the module
# description, the donor-vs-relocated differential (also -Xint), javap -p parity, the overlap agreement table (its
# digest must not change), the census check, the donor's own self-test, and the families and graph-trie
# differentials (their recorded digests must still hold).
# usage: verify.sh <donor-git-dir> <donor-rev> <scratch-dir> [cases-per-member]
# env:   JAVA_HOME (a JDK 21), MAVEN_REPO_LOCAL (optional -Dmaven.repo.local; on Windows a drive path), PYTHON
set -euo pipefail
RECIPE_DIR=$(cd "$(dirname "$0")" && pwd)
MODULE=$(cd "$RECIPE_DIR/../.." && pwd)
ROOT=$(cd "$MODULE/../.." && pwd)
PY=${PYTHON:-python}
REPO=${MAVEN_REPO_LOCAL:+-Dmaven.repo.local=$MAVEN_REPO_LOCAL}
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';' ;; *) SEP=':' ;; esac
M2=${MAVEN_REPO_LOCAL:-$HOME/.m2/repository}
GROUP=com.m3   # spelled through the variable: the naming recipe rewrites a dotted group as a package path
export DEP_CP="$M2/${GROUP//.//}/m3-collections/0.1.0-SNAPSHOT/m3-collections-0.1.0-SNAPSHOT.jar$SEP$M2/${GROUP//.//}/m3-arrays/0.1.0-SNAPSHOT/m3-arrays-0.1.0-SNAPSHOT.jar"
FAMILIES_SHA=f85280ac94b2fc25bf7f10ac17193bf7b788dc1aea731a6a2b4c3da04fc392f9
GRAPH_TRIE_SHA=59954d0f9f92dfa47363c8e3abeab10ede067de0350440c1b8303a668c25c503
mkdir -p "$3"

for recipe in algorithm-extras-20261106 graph-trie-20261106 families-20261106; do
    "$PY" -I "$MODULE/recipe/$recipe/relocate.py" --donor-git "$1" --donor-rev "$2" --check | tail -n 1
done
"$PY" -I "$RECIPE_DIR/converge_rows.py" --check
(cd "$ROOT" && "$PY" -I m3/tooling/jdk-naming/jdk_naming.py plan --root . --only m3/algorithms/ | tail -n 2)
mvn -q -B -ntp $REPO -f "$MODULE/pom.xml" package
"${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin/jar" --describe-module \
    --file "$MODULE/target/m3-algorithms-0.1.0-SNAPSHOT.jar" | head -n 1
bash "$RECIPE_DIR/differential.sh" "$1" "$2" "$3/diff" "${4:-2000}"
"$PY" -I "$RECIPE_DIR/javap_parity.py" "$3/diff" --javap "$JAVA_HOME/bin/javap" | tail -n 1
before=$(sha256sum "$RECIPE_DIR/OVERLAP.tsv" | cut -d' ' -f1)
bash "$RECIPE_DIR/overlap.sh" "$1" "$2" "$3"
after=$(sha256sum "$RECIPE_DIR/OVERLAP.tsv" | cut -d' ' -f1)
[ "$before" = "$after" ] || { echo "OVERLAP table changed: $before -> $after"; exit 1; }
"$PY" -I "$RECIPE_DIR/census.py" --donor-classes "$3/overlap/donor" --landed-classes "$MODULE/target/classes" \
    --owner-cp "$3/overlap/cpu$SEP$3/overlap/javabase$SEP$DEP_CP" --javap "$JAVA_HOME/bin/javap" | tail -n 1
# the donor's plain-main self-test (its path is the second SKIP row of the map, its package the PACKAGE row: no donor
# package name is spelled here)
mkdir -p "$3/selftest"
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
selftest=$(awk -F'\t' '$1=="SKIP" && $2 ~ /SelfTest\.java$/ {print $2; exit}' "$MAP")
donor_pkg=$(awk -F'\t' '$1=="PACKAGE" && $2 ~ /algorithms$/ {print $2; exit}' "$MAP")
git -C "$1" show "$2:$selftest" > "$3/selftest/$(basename "$selftest")"
"$JAVA_HOME/bin/javac" --release 21 -cp "$3/overlap/donor" -d "$3/selftest" "$3/selftest/$(basename "$selftest")"
timeout 120 "$JAVA_HOME/bin/java" -cp "$3/overlap/donor$SEP$3/selftest" "$donor_pkg.$(basename "$selftest" .java)"
bash "$MODULE/recipe/families-20261106/differential.sh" "$1" "$2" "$3/families" 100000 | tee "$3/families.log" | tail -n 1
grep -q "relocated_sha256=$FAMILIES_SHA" "$3/families.log" || { echo "families digest changed"; exit 1; }
bash "$MODULE/recipe/graph-trie-20261106/differential.sh" "$1" "$2" "$3/graphtrie" 100000 100000 | tee "$3/graphtrie.log" | tail -n 1
grep -q "relocated_sha256=$GRAPH_TRIE_SHA" "$3/graphtrie.log" || { echo "graph-trie digest changed"; exit 1; }
echo "VERIFY PASS"
