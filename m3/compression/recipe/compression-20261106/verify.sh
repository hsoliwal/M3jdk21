#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Runs every gate of recipe compression-20261106 in order and prints one line per gate (see GATES.tsv):
#   recipe   relocate.py --check is a fixed point (changed=0 provenance=UNCHANGED)
#   1        mvn package of m3/compression on JDK 21 (-Xlint:all -Werror on main and test sources), 0 compiler warnings
#   2        the oracles ran in the test phase: donor workspace oracle, candidate-index oracle, format oracle
#            (M3_COMPRESSION_FORMAT_RESULT=PASS), and the chunk-cache oracles when the module carries them
#   3        differential.sh: donor closure vs relocated classes byte-identical (oracle outputs, wire corpus, format
#            oracle digests, cache traces), mutants refused, allocation bytes/op equal
#   naming   jdk_naming.py apply --only m3/compression/ reports 0 moves, 0 rewrites, 0 refusals (new files staged first)
# Every JVM gets -Djava.io.tmpdir=<scratch>/tmp (surefire through -DargLine).
# usage: JAVA_HOME=<JDK 21> verify.sh <donor-git-dir> <donor-rev> <scratch-dir> [maven-repo-local [maven-repo-local-tail]]
set -uo pipefail
RECIPE_DIR=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$RECIPE_DIR/../../../.." && pwd)   # recipe dir = <root>/m3/compression/recipe/<name>
DONOR_GIT=$1; REV=$2; SCRATCH=$3; REPO=${4:-}; TAIL=${5:-}
mkdir -p "$SCRATCH/tmp"
TMPDIR_JVM=$(cd "$SCRATCH/tmp" && (pwd -W 2>/dev/null || pwd))
MVN_ARGS=(-B -ntp -f "$ROOT/m3/compression/pom.xml" "-DargLine=-Djava.io.tmpdir=$TMPDIR_JVM")
[ -n "$REPO" ] && MVN_ARGS+=("-Dmaven.repo.local=$REPO")
[ -n "$TAIL" ] && MVN_ARGS+=("-Dmaven.repo.local.tail=$TAIL")
export MAVEN_OPTS="${MAVEN_OPTS:-} -Djava.io.tmpdir=$TMPDIR_JVM"
status=PASS
gate() { local name=$1 verdict=$2; shift 2; echo "GATE $name $verdict $*"; [ "$verdict" = PASS ] || [ "$verdict" = SKIP ] || status=FAIL; }

out=$(python -I "$RECIPE_DIR/relocate.py" --donor-git "$DONOR_GIT" --donor-rev "$REV" --module "$ROOT/m3/compression" --check 2>&1 | tail -1)
case "$out" in *"changed=0 provenance=UNCHANGED"*) gate recipe PASS "$out" ;; *) gate recipe FAIL "$out" ;; esac

(cd "$ROOT" && mvn "${MVN_ARGS[@]}" clean package > "$SCRATCH/mvn-package.log" 2>&1)
warnings=$(grep -cE '^\[WARNING\].*\.java' "$SCRATCH/mvn-package.log"); warnings=${warnings:-0}
if grep -q "BUILD SUCCESS" "$SCRATCH/mvn-package.log" && ! grep -qE "^\[ERROR\]" "$SCRATCH/mvn-package.log" && [ "$warnings" = 0 ]; then
    gate 1_package_lint_werror PASS "BUILD SUCCESS, java warnings=$warnings log=$SCRATCH/mvn-package.log"
else
    gate 1_package_lint_werror FAIL "java warnings=$warnings log=$SCRATCH/mvn-package.log"; grep -E "ERROR|FAIL" "$SCRATCH/mvn-package.log" | head -20
fi
figures=$(grep -E "^(M3 compression reusable-workspace oracle checks|M3 LZ index oracle checks|M3 compressed chunk cache checks|M3_COMPRESSION_FORMAT_(INPUTS|LZ4_ROUNDTRIP|SNAPPY_ROUNDTRIP|RESULT)|M3_CHUNK_CACHE_(OPS|MODEL_MISMATCHES|RESULT))=" "$SCRATCH/mvn-package.log" | tr -d '\r' | tr '\n' ' ')
case "$figures" in *"M3_COMPRESSION_FORMAT_RESULT=PASS"*) gate 2_oracle_test_phase PASS "$figures" ;; *) gate 2_oracle_test_phase FAIL "$figures" ;; esac
python -I - "$ROOT/m3/compression/target/surefire-reports" <<'EOF'
import glob, re, sys
t = f = e = s = 0
for p in glob.glob(sys.argv[1] + "/TEST-*.xml"):
    m = re.search(r'tests="(\d+)" errors="(\d+)" skipped="(\d+)" failures="(\d+)"', open(p, encoding="utf-8").read(2000))
    if m: t += int(m[1]); e += int(m[2]); s += int(m[3]); f += int(m[4])
print("SUREFIRE tests=%d failures=%d errors=%d skipped=%d" % (t, f, e, s))
EOF

if bash "$RECIPE_DIR/differential.sh" "$DONOR_GIT" "$REV" "$SCRATCH/differential" > "$SCRATCH/differential.log" 2>&1; then
    gate 3_differential PASS "$(grep -E ' (IDENTICAL|DIFFERENT) |^mutant ' "$SCRATCH/differential.log" | awk '{ if ($1 == "mutant") print $2"="$3; else print $1"="$2 }' | tr '\n' ' ')"
else
    gate 3_differential FAIL "log=$SCRATCH/differential.log"; tail -8 "$SCRATCH/differential.log"
fi

naming=$(cd "$ROOT" && python -I m3/tooling/jdk-naming/jdk_naming.py apply --root . --only m3/compression/ 2>&1 | grep -E "^(JDK_NAMING|REFUSED)" | tail -1)
case "$naming" in *"moves=0 rewrites=0 refusals=0"*) gate naming PASS "$naming" ;; *) gate naming FAIL "$naming" ;; esac
echo "VERIFY $status"
[ "$status" = PASS ]
