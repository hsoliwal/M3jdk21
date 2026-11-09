#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Replays every gate of the leaf hash-family-20261106 from the repository root: recipe fixed point (relocate.py
# --check: changed=0 provenance=UNCHANGED), JDK naming fixed point (jdk_naming.py plan --only m3/indexdb/: moves=0
# rewrites=0 refusals=0), the crate's oracle corpus (corpus.py, 403 files), the module build with its own lint flags
# (mvn package: -Xlint:all -Werror on main and test sources, no [WARNING] line, surefire totals), then differential.sh
# (crate figures, donor vs relocated dumps incl. -Xint, Python oracle, fixtures, NEC probes, javap -p parity).
# Logs go to the scratch directory; only summaries are printed. Exit 0 = VERIFY PASS.
# usage: verify.sh <donor-git-dir> <donor-rev> <scratch-dir> [maven-repo-local] [corpus-rev ...]   (JAVA_HOME = JDK 21)
# corpus-rev: extra donor revisions searched for the oracle-time corpus files (a shallow donor clone lacks the older
# ones; corpus.py then asks GitHub for them).
set -euo pipefail
RECIPE_DIR=$(cd "$(dirname "$0")" && pwd)
REPO=$(cd "$RECIPE_DIR/../../../.." && pwd)
DONOR_GIT=$1
REV=$2
SCRATCH=$3
REPO_LOCAL=${4:-}
shift 4 2>/dev/null || shift $#
mkdir -p "$SCRATCH"
cd "$REPO"
RECIPE=m3/indexdb/recipe/hash-family-20261106
python -I "$RECIPE/relocate.py" --donor-git "$DONOR_GIT" --donor-rev "$REV" --check | tee "$SCRATCH/relocate-check.txt" | tail -n 2
grep -q ' changed=0 provenance=UNCHANGED' "$SCRATCH/relocate-check.txt"
python -I m3/tooling/jdk-naming/jdk_naming.py plan --root . --only m3/indexdb/ | tee "$SCRATCH/naming-plan.txt" | tail -n 1
grep -q 'moves=0 rewrites=0 refusals=0' "$SCRATCH/naming-plan.txt"
revs=(--rev "$REV")
for rev in "$@"; do revs+=(--rev "$rev"); done
python -I "$RECIPE/corpus.py" --donor-git "$DONOR_GIT" --out "$SCRATCH/corpus" "${revs[@]}" --github hsoliwal/com.synexia \
    | tee "$SCRATCH/corpus.txt" | tail -n 1
if ! mvn -B -ntp ${REPO_LOCAL:+-Dmaven.repo.local=$REPO_LOCAL} -f m3/indexdb/pom.xml package > "$SCRATCH/mvn-package.log" 2>&1 \
    || ! grep -q 'BUILD SUCCESS' "$SCRATCH/mvn-package.log"; then
    tail -n 40 "$SCRATCH/mvn-package.log"
    echo "VERIFY FAIL mvn package"
    exit 1
fi
echo "MVN BUILD SUCCESS warnings=$(grep -c '\[WARNING\]' "$SCRATCH/mvn-package.log" || true)"
! grep -q '\[WARNING\]' "$SCRATCH/mvn-package.log"
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
bash "$RECIPE_DIR/differential.sh" "$DONOR_GIT" "$REV" "$SCRATCH/diff" "$SCRATCH/corpus" | tee "$SCRATCH/differential.txt" | grep -E '^(DONOR|figures|dump|minhash_bound|python_oracle|fixture|javap|FAIL|DIFFERENTIAL)'
grep -q '^DIFFERENTIAL PASS$' "$SCRATCH/differential.txt"
echo "VERIFY PASS"
