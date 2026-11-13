#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Replays every gate of the leaf delta-generator-20261106 from the repository root: recipe fixed point
# (relocate.py --check: files=12 changed=0 provenance=UNCHANGED), JDK naming fixed point (jdk_naming.py plan
# --only m3/file/: moves=0 rewrites=0 refusals=0), the module build with its own lint flags (mvn package:
# -Xlint:all,-module -Werror on main and test sources, surefire test phase with the relocated JUnit proof
# M3ChunkDeltaGeneratorContractTest (8 tests, two of them -Xmx48m subprocess probes), the exec-maven-plugin run of
# verify.py (host and linked-image contracts, among them the bounded probe M3ChunkDeltaWorkspaceProbe), jar), then
# differential.sh (donor postimage vs preimage vs relocated, 48 MiB probes, javap).
# MVN_ARGS passes extra Maven arguments through (this leaf's Windows host needs -Dmaven.test.failure.ignore=true
# -Dexec.skip=true because the pre-existing M3FileDeltaSelfTest CRLF receipt assertion fails there in surefire and in
# verify.py alike; GATES.tsv records the evidence-copy run of verify.py with that assertion neutralized).
# Logs go to the scratch directory; only tails are printed. Exit 0 = VERIFY PASS.
# usage: verify.sh <donor-git-dir> <donor-rev> <scratch-dir> [maven-repo-local]   (JAVA_HOME = JDK 21; python3 on PATH)
set -euo pipefail
RECIPE_DIR=$(cd "$(dirname "$0")" && pwd)
REPO=$(cd "$RECIPE_DIR/../../../.." && pwd)
DONOR_GIT=$1
REV=$2
SCRATCH=$3
REPO_LOCAL=${4:-}
mkdir -p "$SCRATCH"
cd "$REPO"
python -I m3/file/recipe/delta-generator-20261106/relocate.py --donor-git "$DONOR_GIT" --donor-rev "$REV" --check | tee "$SCRATCH/relocate-check.txt"
grep -q ' changed=0 provenance=UNCHANGED' "$SCRATCH/relocate-check.txt"
python -I make/m3/tooling/jdk-naming/jdk_naming.py plan --root . --only m3/file/ | tee "$SCRATCH/naming-plan.txt"
grep -q 'moves=0 rewrites=0 refusals=0' "$SCRATCH/naming-plan.txt"
echo "mvn package ${MVN_ARGS:-} (extra arguments: '${MVN_ARGS:-none}')"
if ! mvn -q -B -ntp ${REPO_LOCAL:+-Dmaven.repo.local=$REPO_LOCAL} ${MVN_ARGS:-} -f m3/file/pom.xml package > "$SCRATCH/mvn-package.log" 2>&1; then
    tail -n 40 "$SCRATCH/mvn-package.log"
    echo "VERIFY FAIL mvn package"
    exit 1
fi
python -I - m3/file/target/surefire-reports <<'PY'
import glob, re, sys
tests = failures = errors = skipped = 0
for path in glob.glob(sys.argv[1] + "/TEST-*.xml"):
    head = open(path, encoding="utf-8").read(4000)
    found = re.search(r'tests="(\d+)".*?errors="(\d+)".*?skipped="(\d+)".*?failures="(\d+)"', head, re.S)
    if found:
        tests += int(found[1]); errors += int(found[2]); skipped += int(found[3]); failures += int(found[4])
print("SUREFIRE tests=%d failures=%d errors=%d skipped=%d" % (tests, failures, errors, skipped))
sys.exit(1 if errors or not tests else 0)
PY
grep -h 'testcase' m3/file/target/surefire-reports/TEST-jdk.m3.file.M3ChunkDeltaGeneratorContractTest.xml | sed -e 's/^ *//' -e 's/ classname=.*//' | tee "$SCRATCH/proof-testcases.txt"
bash "$RECIPE_DIR/differential.sh" "$DONOR_GIT" "$REV" "$SCRATCH/donor" | tee "$SCRATCH/differential.txt"
grep -q '^DIFFERENTIAL PASS$' "$SCRATCH/differential.txt"
echo "VERIFY PASS"
