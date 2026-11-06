#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Runs the gates of GATES.tsv in order: recipe fixed point, Maven package (-Xlint:all -Werror; the oracle and the
# admission test in the test phase), the oracle line of the test report, module description, and the
# donor-vs-relocated differential.
# usage: verify.sh <donor-git-dir> <donor-rev> <scratch-dir>
# env:   JAVA_HOME (a JDK 21), MAVEN_REPO_LOCAL (optional -Dmaven.repo.local), PYTHON (default python)
set -euo pipefail
MODULE=$(cd "$(dirname "$0")" && pwd)
PY=${PYTHON:-python}
REPO=${MAVEN_REPO_LOCAL:+-Dmaven.repo.local=$MAVEN_REPO_LOCAL}
"$PY" -I "$MODULE/recipe/relocate.py" --donor-git "$1" --donor-rev "$2" --check
mvn -q -B -ntp $REPO -f "$MODULE/pom.xml" package
grep -ho 'M3_TOPK_ORACLE_PASS[^<]*' "$MODULE"/target/surefire-reports/TEST-*M3ScopedNgramsOracleTest.xml | head -n 1
"${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin/jar" --describe-module \
    --file "$MODULE/target/m3-nlp-0.1.0-SNAPSHOT.jar" | head -n 1
bash "$MODULE/recipe/differential.sh" "$1" "$2" "$3"
