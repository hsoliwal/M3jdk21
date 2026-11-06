#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Runs the gates of GATES.tsv in order (derived from recipe/families-20261106/verify.sh): recipe fixed point (--check),
# Maven package (-Xlint:all -Werror, the seven oracles of both leaves in the test phase), module description, the
# donor-vs-relocated graph and trie differential, javap -p parity of the relocated classes, the
# IntUnionFind-vs-M3IntDisjointSet partition differential (owner
# check 1), and the families differential of leaf 1 (its recorded digest must still hold).
# usage: verify.sh <donor-git-dir> <donor-rev> <scratch-dir> [graph-cases] [trie-ops] [unionfind-ops]
# env:   JAVA_HOME (a JDK 21), MAVEN_REPO_LOCAL (optional -Dmaven.repo.local), PYTHON (default python)
set -euo pipefail
RECIPE_DIR=$(cd "$(dirname "$0")" && pwd)
MODULE=$(cd "$RECIPE_DIR/../.." && pwd)
PY=${PYTHON:-python}
REPO=${MAVEN_REPO_LOCAL:+-Dmaven.repo.local=$MAVEN_REPO_LOCAL}
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';' ;; *) SEP=':' ;; esac
M2=${MAVEN_REPO_LOCAL:-$HOME/.m2/repository}   # on Windows pass MAVEN_REPO_LOCAL as a drive path
GROUP=com.m3   # the group directory is spelled through the variable: the naming recipe rewrites it as a package path
# the relocated side of the differentials loads jdk.m3.collections.M3ProgressMonitor (the progress-monitor owner) and
# the union-find owner jdk.m3.collections.M3IntDisjointSet
export DEP_CP="$M2/${GROUP//.//}/m3-collections/0.1.0-SNAPSHOT/m3-collections-0.1.0-SNAPSHOT.jar$SEP$M2/${GROUP//.//}/m3-arrays/0.1.0-SNAPSHOT/m3-arrays-0.1.0-SNAPSHOT.jar"
"$PY" -I "$RECIPE_DIR/relocate.py" --donor-git "$1" --donor-rev "$2" --check
mvn -q -B -ntp $REPO -f "$MODULE/pom.xml" package
"${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin/jar" --describe-module \
    --file "$MODULE/target/m3-algorithms-0.1.0-SNAPSHOT.jar" | head -n 1
bash "$RECIPE_DIR/differential.sh" "$1" "$2" "$3" "${4:-100000}" "${5:-100000}"
bash "$RECIPE_DIR/javap_parity.sh" "$3"
bash "$RECIPE_DIR/unionfind_differential.sh" "$1" "$2" "$3" "${6:-1000000}"
bash "$MODULE/recipe/families-20261106/differential.sh" "$1" "$2" "$3/families" 100000
