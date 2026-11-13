#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Runs every gate of recipe posting-index-20261106 in order and prints one line per gate (see GATES.tsv):
#   recipe   relocate.py --check is a fixed point (files=12 changed=0 provenance=UNCHANGED)
#   1        mvn package of m3/arrays on JDK 21 with the module's flags (-Xlint:all,-module -Werror)
#   2        the oracles ran in the test phase: MAT_GALLOP_*, MAT_POSTING_SKEW_*, MAT_POSTING_*, POSTING_CARDINALITY_PASS,
#            BOUNDED_COUNT_PASS lines in the surefire output
#   3        differential.sh: donor closure vs relocated classes, oracle outputs and the 100,000-list dump byte-identical
#   naming   jdk_naming.py apply --only m3/arrays/ reports 0 moves, 0 rewrites, 0 refusals
#   4        native (optional, WSL or any Linux with gcc + JDK 21 headers): run the C gate script named by
#            M3_POSTING_NATIVE_GATE (defaults to skipping) and require NATIVE_GATE PASS
# usage: JAVA_HOME=<JDK 21> verify.sh <donor-git-dir> <donor-rev> <scratch-dir> [maven-repo-local]
set -uo pipefail
RECIPE_DIR=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$RECIPE_DIR/../../../.." && pwd)   # recipe dir = <root>/m3/arrays/recipe/<name>
DONOR_GIT=$1; REV=$2; SCRATCH=$3; REPO=${4:-}
MVN_ARGS=(-q -B -ntp -f "$ROOT/m3/arrays/pom.xml")
[ -n "$REPO" ] && MVN_ARGS+=("-Dmaven.repo.local=$REPO")
mkdir -p "$SCRATCH"
status=PASS
gate() { local name=$1 verdict=$2; shift 2; echo "GATE $name $verdict $*"; [ "$verdict" = PASS ] || [ "$verdict" = SKIP ] || status=FAIL; }

out=$(python -I "$RECIPE_DIR/relocate.py" --donor-git "$DONOR_GIT" --donor-rev "$REV" --module "$ROOT/m3/arrays" --check 2>&1 | tail -1)
case "$out" in *"changed=0 provenance=UNCHANGED"*) gate recipe PASS "$out" ;; *) gate recipe FAIL "$out" ;; esac

if (cd "$ROOT" && mvn "${MVN_ARGS[@]}" package > "$SCRATCH/mvn-package.log" 2>&1); then
    gate 1_package_lint_werror PASS "BUILD SUCCESS (exit 0), warnings=$(grep -c 'WARNING.*\.java' "$SCRATCH/mvn-package.log") log=$SCRATCH/mvn-package.log"
else
    gate 1_package_lint_werror FAIL "exit $? log=$SCRATCH/mvn-package.log"; tail -30 "$SCRATCH/mvn-package.log"
fi
figures=$(grep -E "^(MAT_GALLOP_(SCENARIOS|DIGEST|BACKEND)|MAT_POSTING_SKEW_(SCENARIOS|ASSERTIONS)|MAT_POSTING_(SCENARIOS|ASSERTIONS)|POSTING_CARDINALITY_PASS|BOUNDED_COUNT_PASS)" "$SCRATCH/mvn-package.log" | tr '\n' ' ')
case "$figures" in *MAT_GALLOP_SCENARIOS=*MAT_POSTING_SKEW_SCENARIOS=1034*) gate 2_oracle_test_phase PASS "$figures" ;; *) gate 2_oracle_test_phase FAIL "$figures" ;; esac
python - "$ROOT/m3/arrays/target/surefire-reports" <<'EOF'
import glob, re, sys
t = f = e = s = 0
for p in glob.glob(sys.argv[1] + "/TEST-*.xml"):
    m = re.search(r'tests="(\d+)" errors="(\d+)" skipped="(\d+)" failures="(\d+)"', open(p, encoding="utf-8").read(2000))
    if m: t += int(m[1]); e += int(m[2]); s += int(m[3]); f += int(m[4])
print("SUREFIRE tests=%d failures=%d errors=%d skipped=%d" % (t, f, e, s))
EOF

if bash "$RECIPE_DIR/differential.sh" "$DONOR_GIT" "$REV" "$SCRATCH/differential" > "$SCRATCH/differential.log" 2>&1; then
    gate 3_differential PASS "$(grep -E 'IDENTICAL|DIFFERENT' "$SCRATCH/differential.log" | awk '{print $1"="$2}' | tr '\n' ' ')$(tail -2 "$SCRATCH/differential.log" | head -1)"
else
    gate 3_differential FAIL "log=$SCRATCH/differential.log"; tail -5 "$SCRATCH/differential.log"
fi

naming=$(cd "$ROOT" && python -I make/m3/tooling/jdk-naming/jdk_naming.py apply --root . --only m3/arrays/ 2>&1 | grep -E "^(JDK_NAMING|REFUSED)" | tail -1)
case "$naming" in *"moves=0 rewrites=0 refusals=0"*) gate naming PASS "$naming" ;; *) gate naming FAIL "$naming" ;; esac

if [ -n "${M3_POSTING_NATIVE_GATE:-}" ]; then
    if bash "$RECIPE_DIR/native-gate.sh" "$SCRATCH/differential" "$SCRATCH/native" > "$SCRATCH/native-gate.log" 2>&1 && grep -q "NATIVE_GATE PASS" "$SCRATCH/native-gate.log"; then
        gate 4_native PASS "$(grep -E '^gallop |^skew |posting symbols' "$SCRATCH/native-gate.log" | cut -c1-160 | tr '\n' ' ')"
    else
        gate 4_native FAIL "log=$SCRATCH/native-gate.log"
    fi
else
    gate 4_native SKIP "set M3_POSTING_NATIVE_GATE=1 on a Linux host with gcc, cmake and JDK 21 headers to run native-gate.sh (the 2026-11-06 run used WSL; the differential scratch of gate 3 feeds it)"
fi
echo "VERIFY $status"
[ "$status" = PASS ]
