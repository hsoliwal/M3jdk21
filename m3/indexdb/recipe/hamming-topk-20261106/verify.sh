#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Replays every gate of the leaf hamming-topk-20261106 from the repository root: recipe fixed point
# (relocate.py --check: files=5 changed=0), JDK naming fixed point (jdk_naming.py plan --only m3/indexdb/:
# moves=0 rewrites=0 refusals=0), the module build with its own lint flags (mvn package: -Xlint:all -Werror on main
# and test sources, surefire test phase with the oracle wrapper M3HammingTopKContractTest printing
# M3_TOPK_HEAPSORT_DIFFERENTIAL_PASS cases=35032 comparisons=1439283 and the negative wrapper
# M3HammingTopKTieOrderMutantTest, jar), then differential.sh (donor vs relocated vs mutant, fingerprint_nec).
# Logs go to the scratch directory; only tails are printed. Exit 0 = VERIFY PASS.
# usage: verify.sh <donor-git-dir> <donor-rev> <scratch-dir> [maven-repo-local]   (JAVA_HOME = JDK 21)
set -euo pipefail
RECIPE_DIR=$(cd "$(dirname "$0")" && pwd)
REPO=$(cd "$RECIPE_DIR/../../../.." && pwd)
DONOR_GIT=$1
REV=$2
SCRATCH=$3
REPO_LOCAL=${4:-}
mkdir -p "$SCRATCH"
cd "$REPO"
python -I m3/indexdb/recipe/hamming-topk-20261106/relocate.py --donor-git "$DONOR_GIT" --donor-rev "$REV" --check | tee "$SCRATCH/relocate-check.txt"
grep -q ' changed=0 provenance=UNCHANGED' "$SCRATCH/relocate-check.txt"
python -I m3/tooling/jdk-naming/jdk_naming.py plan --root . --only m3/indexdb/ | tee "$SCRATCH/naming-plan.txt"
grep -q 'moves=0 rewrites=0 refusals=0' "$SCRATCH/naming-plan.txt"
if ! mvn -q -B -ntp ${REPO_LOCAL:+-Dmaven.repo.local=$REPO_LOCAL} -f m3/indexdb/pom.xml package > "$SCRATCH/mvn-package.log" 2>&1; then
    tail -n 40 "$SCRATCH/mvn-package.log"
    echo "VERIFY FAIL mvn package"
    exit 1
fi
grep -h 'M3_TOPK_HEAPSORT_DIFFERENTIAL_PASS' "$SCRATCH/mvn-package.log"
python -I - m3/indexdb/target/surefire-reports <<'PY'
import glob, re, sys
tests = failures = errors = skipped = 0
for path in glob.glob(sys.argv[1] + "/TEST-*.xml"):
    head = open(path, encoding="utf-8").read(4000)
    found = re.search(r'tests="(\d+)".*?errors="(\d+)".*?skipped="(\d+)".*?failures="(\d+)"', head, re.S)
    if found:
        tests += int(found[1]); errors += int(found[2]); skipped += int(found[3]); failures += int(found[4])
print("SUREFIRE tests=%d failures=%d errors=%d skipped=%d" % (tests, failures, errors, skipped))
sys.exit(1 if failures or errors or not tests else 0)
PY
bash "$RECIPE_DIR/differential.sh" "$DONOR_GIT" "$REV" "$SCRATCH/donor" | tee "$SCRATCH/differential.txt"
grep -q '^DIFFERENTIAL PASS$' "$SCRATCH/differential.txt"
echo "VERIFY PASS"
