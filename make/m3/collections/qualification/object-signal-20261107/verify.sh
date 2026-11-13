#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Runs the gates of GATES.tsv in order: recipe fixed points (relocate.py --check, apply.py --check), Maven package with
# the module's real pom (-Xlint:all,-module -Werror on main and test sources) and its full test run (the log is grepped
# for BUILD SUCCESS because mvn's exit code is not reliable under Git Bash), the surefire totals against a base totals
# file, the relocated oracle and the reference model, the donor-vs-relocated differential (oracle, dump under the
# default JIT and -Xint, javap -p), the mutants, the untouched-class gate (every class file of a base build compared
# with this build) and the JDK naming plan over the leaf's files (0 rewrites).
# usage: verify.sh <donor-git-dir> <donor-rev> <scratch-dir> [base-classes-dir [base-totals]] [queries] [seed]
#   base-classes-dir: target/classes of m3/collections built from the base commit (skip the gate with -)
#   base-totals: "tests failures errors skipped" of the base surefire run (skip with -)
# env:   JAVA_HOME (a JDK 21), MAVEN_ARGS (extra mvn flags: repo.local, tail, -DargLine), PYTHON (default python)
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../..")
ROOT=$(winpath "$(dirname "$0")/../../../..")
DONOR_GIT=$1
REV=$2
mkdir -p "$3"
SCRATCH=$(winpath "$3")
BASE_CLASSES=${4:--}
BASE_TOTALS=${5:--}
QUERIES=${6:-20000}
SEED=${7:-20261107}
PY=${PYTHON:-python}
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
JAVA_OPTS="-Djava.io.tmpdir=${M3_TMPDIR:-$SCRATCH}"
echo "GATE recipe"
"$PY" -I "$RECIPE_DIR/relocate.py" --donor-git "$DONOR_GIT" --donor-rev "$REV" --check | tail -n 1
"$PY" -I "$RECIPE_DIR/apply.py" --check | tail -n 1
echo "GATE package+tests"
# shellcheck disable=SC2086
mvn -B -ntp ${MAVEN_ARGS:-} -f "$MODULE/pom.xml" clean package -Dmaven.test.failure.ignore=true \
    > "$SCRATCH/mvn-package.log" 2>&1 || true
grep -E "BUILD (SUCCESS|FAILURE)|Total time" "$SCRATCH/mvn-package.log" | sed 's/^\[INFO\] //'
grep -q "BUILD SUCCESS" "$SCRATCH/mvn-package.log"
echo "compiler warnings: $(grep -c 'WARNING.*\.java' "$SCRATCH/mvn-package.log" || true)"
totals=$("$PY" -I - "$MODULE/target/surefire-reports" <<'EOF'
import glob, re, sys
t = [0, 0, 0, 0]
failing = []
for path in sorted(glob.glob(sys.argv[1] + "/TEST-*.xml")):
    head = re.search(r"<testsuite[^>]*", open(path, encoding="utf-8").read(4096)).group(0)
    row = [int(re.search(key + r'="(\d+)"', head).group(1)) for key in ("tests", "failures", "errors", "skipped")]
    t = [a + b for a, b in zip(t, row)]
    if row[1] or row[2]:
        failing.append(path.rsplit("TEST-", 1)[1][:-4])
print(" ".join(map(str, t)), "failing_suites=" + (",".join(failing) or "none"))
EOF
)
echo "surefire tests failures errors skipped: $totals"
if [ "$BASE_TOTALS" != - ]; then
    read -r bt bf be bs <<< "$BASE_TOTALS"
    read -r ht hf he hs _rest <<< "$totals"
    [ "$hf" = "$bf" ] && [ "$he" = "$be" ] && [ "$ht" -ge "$bt" ] \
        && echo "TESTS PASS base $BASE_TOTALS -> head $ht $hf $he $hs (failing set unchanged, count grows by $((ht - bt)))" \
        || { echo "TESTS FAIL base $BASE_TOTALS head $totals"; exit 1; }
fi
grep -h "PASS: " "$MODULE/target/surefire-reports/jdk.m3.collections.M3ObjectSignalTest-output.txt" 2>/dev/null || true
echo "GATE differential"
bash "$RECIPE_DIR/differential.sh" "$DONOR_GIT" "$REV" "$SCRATCH/diff" "$QUERIES" "$SEED"
echo "GATE mutants"
bash "$RECIPE_DIR/mutants.sh" "$SCRATCH/diff" "$SCRATCH/mutants" "$QUERIES" "$SEED"
if [ "$BASE_CLASSES" != - ]; then
    echo "GATE untouched classes"
    "$PY" -I - "$BASE_CLASSES" "$MODULE/target/classes" <<'EOF'
import sys
from pathlib import Path
base, head = Path(sys.argv[1]), Path(sys.argv[2])
old = {p.relative_to(base).as_posix() for p in base.rglob("*.class")}
new = {p.relative_to(head).as_posix() for p in head.rglob("*.class")}
changed = sorted(f for f in old if f not in new or (base / f).read_bytes() != (head / f).read_bytes())
for f in changed:
    print("    CHANGED " + f)
for f in sorted(new - old):
    print("    ADDED " + f)
print("untouched classes: identical=%d changed=%d added=%d" % (len(old) - len(changed), len(changed), len(new - old)))
sys.exit(1 if changed else 0)
EOF
fi
echo "GATE naming"
(cd "$ROOT" && "$PY" -I m3/tooling/jdk-naming/jdk_naming.py plan --root . \
    --only m3/collections/src/main/java/jdk/m3/collections/M3ObjectSignal \
    --only m3/collections/src/main/java/jdk/m3/collections/M3ObjectProj \
    --only m3/collections/src/test/java/jdk/m3/collections/M3ObjectSignal \
    --only m3/collections/qualification/object-signal-20261107/ | tail -n 1)
echo "VERIFY PASS"
