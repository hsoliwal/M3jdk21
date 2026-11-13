#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Runs the gates of GATES.tsv in order: recipe fixed point (--check), Maven package (-Xlint:all -Werror, the donor
# oracle in the test phase, 0 warnings), donor oracle figure + differential + -Xint + cross-solver agreement
# (differential.sh), javap parity (javap_parity.sh), the families differential digest, class files of the master
# build unchanged (when BASELINE_CLASSES names a copy of target/ from a master build), and the naming fixed point.
# usage: verify.sh <donor-git-dir> <donor-rev> <scratch-dir> [random-networks] [xint-random-networks]
# env:   JAVA_HOME (a JDK 21), MAVEN_REPO_LOCAL (optional -Dmaven.repo.local, a drive path on Windows),
#        PYTHON (default python), FAMILIES_DONOR_GIT + FAMILIES_DONOR_REV (optional: the families differential),
#        BASELINE_CLASSES (optional: a directory holding classes/ and test-classes/ of the master build)
set -euo pipefail
RECIPE_DIR=$(cd "$(dirname "$0")" && pwd)
MODULE=$(cd "$RECIPE_DIR/../.." && pwd)
ROOT=$(cd "$MODULE/../.." && pwd)
PY=${PYTHON:-python}
REPO=${MAVEN_REPO_LOCAL:+-Dmaven.repo.local=$MAVEN_REPO_LOCAL}
FAMILIES_DIGEST=f85280ac94b2fc25bf7f10ac17193bf7b788dc1aea731a6a2b4c3da04fc392f9
mkdir -p "$3"
"$PY" -I "$RECIPE_DIR/relocate.py" --donor-git "$1" --donor-rev "$2" --check
mvn -B -ntp $REPO -f "$MODULE/pom.xml" package > "$3/mvn-package.log" 2>&1 || true
grep -q 'BUILD SUCCESS' "$3/mvn-package.log" || { echo "PACKAGE FAIL (log $3/mvn-package.log)"; exit 1; }
warnings=$(grep -c 'WARNING' "$3/mvn-package.log" || true)
echo "PACKAGE PASS warnings=${warnings:-0} $(grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$' "$3/mvn-package.log" | tail -n 1 | sed 's/^\[INFO\] //')"
[ "${warnings:-0}" -eq 0 ] || exit 1
bash "$RECIPE_DIR/differential.sh" "$1" "$2" "$3/differential" "${4:-100000}" "${5:-2000}"
bash "$RECIPE_DIR/javap_parity.sh" "$3/differential"
if [ -n "${FAMILIES_DONOR_GIT:-}" ]; then
    case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';' ;; *) SEP=':' ;; esac
    M2=${MAVEN_REPO_LOCAL:-$HOME/.m2/repository}
    GROUP=com.m3   # spelled through the variable, as in families verify.sh (the naming recipe rewrites a package path)
    export DEP_CP="$M2/${GROUP//.//}/m3-collections/0.1.0-SNAPSHOT/m3-collections-0.1.0-SNAPSHOT.jar$SEP$M2/${GROUP//.//}/m3-arrays/0.1.0-SNAPSHOT/m3-arrays-0.1.0-SNAPSHOT.jar"
    families=$(bash "$MODULE/recipe/families-20261106/differential.sh" "$FAMILIES_DONOR_GIT" "$FAMILIES_DONOR_REV" "$3/families" 100000 | tail -n 1)
    case "$families" in
        "DIFFERENTIAL PASS"*"relocated_sha256=$FAMILIES_DIGEST"*) echo "FAMILIES PASS relocated_sha256=$FAMILIES_DIGEST" ;;
        *) echo "FAMILIES FAIL $families"; exit 1 ;;
    esac
fi
if [ -n "${BASELINE_CLASSES:-}" ]; then
    same=0
    total=0
    while read -r class; do
        total=$((total + 1))
        cmp -s "$BASELINE_CLASSES/$class" "$MODULE/target/$class" && same=$((same + 1)) || echo "CHANGED $class"
    done < <(cd "$BASELINE_CLASSES" && find classes test-classes -name '*.class' | sort)
    [ "$same" -eq "$total" ] || { echo "UNTOUCHED FAIL $same/$total"; exit 1; }
    echo "UNTOUCHED PASS master_classes=$total identical=$same"
fi
"$PY" -I "$ROOT/m3/tooling/jdk-naming/jdk_naming.py" plan --root "$ROOT" --only m3/algorithms/ | tail -n 1
