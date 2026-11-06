#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Runs every gate of recipe atom-jit-gate-20261106 in order and prints one line per gate (see GATES.tsv):
#   recipe     relocate.py --check is a fixed point (changed=0 provenance=UNCHANGED)
#   5_package  mvn install of m3/tooling/atom-census (the atomizer the gate binds), then mvn package of the gate on
#              JDK 21 with -Xlint:all -Werror (0 compiler warnings) and its JUnit 5 tests (surefire counts)
#   1_hotloop  jit-oracles.sh part 1: -Xint / forced C2 / -Xcomp digest equality, donor and relocated, hotLoop witness
#   2_longset  jit-oracles.sh part 2: M3LongHashSet add proof under -Xint, C1 tier 1, C2 tier 4, donor and relocated
#   4_verdicts differential.sh: 20,000 serial gate ledgers and 15 work-order queues, donor vs relocated verdicts
#   phase_a    phase-a-nec.sh: the NEC evidence of Phase A (donor pattern groups vs census groups)
#   naming     jdk_naming.py apply --only m3/tooling/atom-jit-gate/ (and the workflow): 0 moves, 0 rewrites, 0 refusals
#   3_fixture  fixture-gate.sh (Linux: gcc/g++, nm, JDK 21 headers): on Linux it runs directly; on Windows set
#              M3_ATOMJIT_FIXTURE_GATE=wsl to run it through WSL, otherwise SKIP
# usage: JAVA_HOME=<JDK 21> verify.sh <donor-git-dir> <donor-rev> <scratch-dir> [maven-repo-local]
set -uo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) WINDOWS=1 ;; *) WINDOWS= ;; esac
RECIPE_DIR=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$RECIPE_DIR/../../../../.." && pwd)   # recipe dir = <root>/m3/tooling/atom-jit-gate/recipe/<name>
DONOR_GIT=$1; REV=$2; SCRATCH=$3; REPO=${4:-}
MVN_ARGS=(-B -ntp)
[ -n "$REPO" ] && MVN_ARGS+=("-Dmaven.repo.local=$REPO")
mkdir -p "$SCRATCH"
status=PASS
gate() { local name=$1 verdict=$2; shift 2; echo "GATE $name $verdict $*"; [ "$verdict" = PASS ] || [ "$verdict" = SKIP ] || status=FAIL; }

out=$(python -I "$RECIPE_DIR/relocate.py" --donor-git "$DONOR_GIT" --donor-rev "$REV" --check 2>&1 | tail -1)
case "$out" in *"changed=0 provenance=UNCHANGED"*) gate recipe PASS "$out" ;; *) gate recipe FAIL "$out" ;; esac

(cd "$ROOT" && mvn "${MVN_ARGS[@]}" -q -f m3/tooling/atom-census/pom.xml -DskipTests install > "$SCRATCH/census-install.log" 2>&1)
census=$?
(cd "$ROOT" && mvn "${MVN_ARGS[@]}" -f m3/tooling/atom-jit-gate/pom.xml clean package > "$SCRATCH/mvn-package.log" 2>&1)
built=$?
warnings=$(grep -c -E '^\[WARNING\].*\.java' "$SCRATCH/mvn-package.log"); warnings=${warnings:-0}
if [ "$census" = 0 ] && [ "$built" = 0 ] && grep -q 'BUILD SUCCESS' "$SCRATCH/mvn-package.log" && [ "$warnings" = 0 ]; then
    gate 5_package PASS "census install exit 0; BUILD SUCCESS, java warnings=$warnings, $(grep -E '^\[INFO\] Tests run: [0-9]+, ' "$SCRATCH/mvn-package.log" | tail -1 | sed 's/^\[INFO\] //') log=$SCRATCH/mvn-package.log"
else
    gate 5_package FAIL "census=$census package=$built warnings=$warnings log=$SCRATCH/mvn-package.log"; tail -30 "$SCRATCH/mvn-package.log"
fi

bash "$RECIPE_DIR/jit-oracles.sh" "$DONOR_GIT" "$REV" "$SCRATCH/jit" > "$SCRATCH/jit-oracles.log" 2>&1
if grep -q '^ORACLE1 PASS' "$SCRATCH/jit-oracles.log"; then gate 1_hotloop PASS "$(grep '^ORACLE1' "$SCRATCH/jit-oracles.log")"; else gate 1_hotloop FAIL "log=$SCRATCH/jit-oracles.log"; fi
if grep -q '^ORACLE2 PASS' "$SCRATCH/jit-oracles.log"; then
    gate 2_longset PASS "$(grep -E '^longset relocated (c1|c2)' "$SCRATCH/jit-oracles.log" | sed 's/ | JIT_WARMUP_PASS entries=266240 |/,/' | tr -s ' ' | tr '\n' ';')"
else gate 2_longset FAIL "$(grep -E '^FAIL' "$SCRATCH/jit-oracles.log" | head -3 | tr '\n' ' ') log=$SCRATCH/jit-oracles.log"; fi

if bash "$RECIPE_DIR/differential.sh" "$DONOR_GIT" "$REV" "$SCRATCH/differential" > "$SCRATCH/differential.log" 2>&1; then
    gate 4_verdicts PASS "$(grep -E '^serial-gate|^work-orders' "$SCRATCH/differential.log" | cut -c1-200 | tr '\n' ' ')"
else gate 4_verdicts FAIL "$(grep -E '^FAIL' "$SCRATCH/differential.log" | head -3 | tr '\n' ' ') log=$SCRATCH/differential.log"; fi

if bash "$RECIPE_DIR/phase-a-nec.sh" "$DONOR_GIT" "$REV" "$SCRATCH/phase-a" > "$SCRATCH/phase-a.log" 2>&1; then
    gate phase_a PASS "NEC evidence: $(grep -E '^FILES=' "$SCRATCH/phase-a.log" | tr '\n' ' ' | cut -c1-260)"
else gate phase_a FAIL "log=$SCRATCH/phase-a.log"; fi

naming=$(cd "$ROOT" && python -I m3/tooling/jdk-naming/jdk_naming.py apply --root . --only m3/tooling/atom-jit-gate/ \
    --only .github/workflows/m3-tooling-atom-jit-gate-20261106.yml 2>&1 | grep -E "^(JDK_NAMING|REFUSED)" | tail -1)
case "$naming" in *"moves=0 rewrites=0 refusals=0"*) gate naming PASS "$naming" ;; *) gate naming FAIL "$naming" ;; esac

crate="$SCRATCH/differential/donor-fixture-crate"
if [ -z "$WINDOWS" ]; then
    if bash "$RECIPE_DIR/fixture-gate.sh" "$crate" "$SCRATCH/fixture" > "$SCRATCH/fixture-gate.log" 2>&1; then
        gate 3_fixture PASS "$(grep -E '^(abi javac-h|exports|receipts mode_results|negative relocated)' "$SCRATCH/fixture-gate.log" | cut -c1-120 | tr '\n' ' ')"
    else gate 3_fixture FAIL "log=$SCRATCH/fixture-gate.log"; fi
elif [ "${M3_ATOMJIT_FIXTURE_GATE:-}" = wsl ]; then
    wslpath() { (cd "$1" && pwd -W) | sed -E 's#^([A-Za-z]):#/mnt/\L\1#'; }
    if MSYS_NO_PATHCONV=1 wsl -e bash "$(wslpath "$RECIPE_DIR")/fixture-gate.sh" "$(wslpath "$crate")" "$(wslpath "$SCRATCH")/fixture" > "$SCRATCH/fixture-gate.log" 2>&1; then
        gate 3_fixture PASS "(WSL) $(grep -E '^(abi javac-h|exports|receipts mode_results|negative relocated)' "$SCRATCH/fixture-gate.log" | cut -c1-120 | tr '\n' ' ')"
    else gate 3_fixture FAIL "log=$SCRATCH/fixture-gate.log"; fi
else
    gate 3_fixture SKIP "Windows: set M3_ATOMJIT_FIXTURE_GATE=wsl (WSL with gcc, g++, nm, JDK 21) or run fixture-gate.sh on Linux"
fi
echo "VERIFY $status"
[ "$status" = PASS ]
