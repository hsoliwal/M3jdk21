#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Runs every gate of the leaf recipe/scoped-ngram-perplexity-20261106 in order, one GATE line each (see GATES.tsv):
#   recipe     relocate.py --check is a fixed point at the REPO pin (files=2 held=3 changed=0 provenance=UNCHANGED)
#   1          mvn package of m3/nlp on JDK 21 (-Xlint:all -Werror), skipped when M3_NLP_PACKAGED=1 (m3/nlp/verify.sh
#              has just built it); SUREFIRE totals of the reports
#   2          M3NgramPerplexityTest (the relocated sealed JUnit 5 test) ran in the test phase with 7 tests, 0 failures
#   3+4        differential.sh: donor tests (sealed 7/7, develop 7/8), donor/relocated/-Xint dumps byte-identical,
#              javap -p parity under the map
#   5          untouched class files: when M3_NLP_BASELINE_CLASSES names a sha256sum list of a master build of m3/nlp,
#              every class file of that list is byte-identical in target/ (SKIP otherwise)
#   naming     jdk_naming.py apply --only m3/nlp/ reports 0 moves, 0 rewrites, 0 refusals
# usage: JAVA_HOME=<JDK 21> verify.sh <donor-git-dir> <scratch-dir>   (MAVEN_REPO_LOCAL, PYTHON optional)
set -uo pipefail
RECIPE_DIR=$(cd "$(dirname "$0")" && pwd)
MODULE=$(cd "$RECIPE_DIR/../.." && pwd)
ROOT=$(cd "$MODULE/../.." && pwd)
DONOR_GIT=$1; SCRATCH=$2
PY=${PYTHON:-python}
mkdir -p "$SCRATCH"
status=PASS
gate() { local name=$1 verdict=$2; shift 2; echo "GATE $name $verdict $*"; [ "$verdict" = PASS ] || [ "$verdict" = SKIP ] || status=FAIL; }

out=$("$PY" -I "$RECIPE_DIR/relocate.py" --donor-git "$DONOR_GIT" --check 2>&1 | tail -1)
case "$out" in *"changed=0 provenance=UNCHANGED"*) gate recipe PASS "$out" ;; *) gate recipe FAIL "$out" ;; esac

if [ "${M3_NLP_PACKAGED:-0}" = 1 ]; then
    gate 1_package_lint_werror SKIP "built by m3/nlp/verify.sh"
elif mvn -q -B -ntp ${MAVEN_REPO_LOCAL:+-Dmaven.repo.local=$MAVEN_REPO_LOCAL} -f "$MODULE/pom.xml" package > "$SCRATCH/mvn-package.log" 2>&1; then
    gate 1_package_lint_werror PASS "BUILD SUCCESS (exit 0), warnings=$(grep -c 'WARNING' "$SCRATCH/mvn-package.log") log=$SCRATCH/mvn-package.log"
else
    gate 1_package_lint_werror FAIL "log=$SCRATCH/mvn-package.log"; tail -30 "$SCRATCH/mvn-package.log"
fi
reports="$MODULE/target/surefire-reports"
"$PY" - "$reports" <<'EOF'
import glob, re, sys
t = f = e = s = 0
for p in glob.glob(sys.argv[1] + "/TEST-*.xml"):
    m = re.search(r'tests="(\d+)" errors="(\d+)" skipped="(\d+)" failures="(\d+)"', open(p, encoding="utf-8").read(2000))
    if m: t += int(m[1]); e += int(m[2]); s += int(m[3]); f += int(m[4])
print("SUREFIRE tests=%d failures=%d errors=%d skipped=%d" % (t, f, e, s))
EOF
counts=$(grep -ho 'tests="[0-9]*" errors="[0-9]*" skipped="[0-9]*" failures="[0-9]*"' "$reports"/TEST-jdk.m3.nlp.M3NgramPerplexityTest.xml 2>/dev/null | head -n 1)
case "$counts" in 'tests="7" errors="0" skipped="0" failures="0"') gate 2_relocated_test PASS "M3NgramPerplexityTest $counts" ;; *) gate 2_relocated_test FAIL "M3NgramPerplexityTest $counts" ;; esac

if bash "$RECIPE_DIR/differential.sh" "$DONOR_GIT" "$SCRATCH/differential" > "$SCRATCH/differential.log" 2>&1; then
    gate 3_differential PASS "$(grep -E '^(DONOR_TEST|DUMP|TOTAL|JAVAP)' "$SCRATCH/differential.log" | cut -c1-120 | tr '\n' ' ')"
else
    gate 3_differential FAIL "log=$SCRATCH/differential.log"; tail -8 "$SCRATCH/differential.log"
fi

if [ -n "${M3_NLP_BASELINE_CLASSES:-}" ]; then
    result=$(cd "$MODULE/target" && sed 's/ \*/  /' "$M3_NLP_BASELINE_CLASSES" | sha256sum -c --quiet - 2>&1 | grep -c -v '^$')
    result=${result:-0}
    [ "$result" = 0 ] && gate 5_untouched_classes PASS "$(wc -l < "$M3_NLP_BASELINE_CLASSES") master class files byte-identical" \
        || gate 5_untouched_classes FAIL "$result mismatching lines"
else
    gate 5_untouched_classes SKIP "set M3_NLP_BASELINE_CLASSES to a sha256sum list (paths relative to target/) of a master build"
fi

naming=$(cd "$ROOT" && "$PY" -I make/m3/tooling/jdk-naming/jdk_naming.py apply --root . --only m3/nlp/ 2>&1 | grep -E "^(JDK_NAMING|REFUSED)" | tail -1)
case "$naming" in *"moves=0 rewrites=0 refusals=0"*) gate naming PASS "$naming" ;; *) gate naming FAIL "$naming" ;; esac
echo "VERIFY $status"
[ "$status" = PASS ]
