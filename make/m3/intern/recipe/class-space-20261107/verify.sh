#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Runs every gate of recipe class-space-20261107 in order and prints one line per gate (see GATES.tsv):
#   recipe     relocate.py --check is a fixed point (changed=0 provenance=UNCHANGED)
#   1          mvn clean package of m3/intern on JDK 21 (-Xlint:all -Werror on main and test sources), 0 [WARNING] lines
#   2          the class-space oracles ran in the test phase with their figures, surefire totals
#   3          differential.sh: donor closure vs relocated classes byte-identical (oracle outputs, dump default and
#              -Xint, JNI lane, allocation bytes), fault replay, mutants refused, javap identical
#   untouched  (with a base revision) every class file the module built at the base is byte-identical in this build
#   naming     jdk_naming.py apply --only m3/intern/ reports 0 moves, 0 rewrites, 0 refusals (new files staged first)
# Every JVM gets -Djava.io.tmpdir=<scratch>/tmp (surefire through -DargLine, Maven through MAVEN_OPTS).
# usage: JAVA_HOME=<JDK 21> verify.sh <donor-git-dir> <donor-rev> <scratch-dir> [maven-repo-local [maven-repo-local-tail [base-rev]]]
set -uo pipefail
RECIPE_DIR=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$RECIPE_DIR/../../../.." && pwd)   # recipe dir = <root>/m3/intern/recipe/<name>
DONOR_GIT=$1; REV=$2; SCRATCH=$3; REPO=${4:-}; TAIL=${5:-}; BASE=${6:-}
mkdir -p "$SCRATCH/tmp"
TMPDIR_JVM=$(cd "$SCRATCH/tmp" && (pwd -W 2>/dev/null || pwd))
MVN_COMMON=(-B -ntp "-DargLine=-Djava.io.tmpdir=$TMPDIR_JVM")
[ -n "$REPO" ] && MVN_COMMON+=("-Dmaven.repo.local=$REPO")
[ -n "$TAIL" ] && MVN_COMMON+=("-Dmaven.repo.local.tail=$TAIL")
export MAVEN_OPTS="${MAVEN_OPTS:-} -Djava.io.tmpdir=$TMPDIR_JVM"
status=PASS
gate() { local name=$1 verdict=$2; shift 2; echo "GATE $name $verdict $*"; [ "$verdict" = PASS ] || [ "$verdict" = SKIP ] || status=FAIL; }

out=$(python -I "$RECIPE_DIR/relocate.py" --donor-git "$DONOR_GIT" --donor-rev "$REV" --module "$ROOT/m3/intern" --check 2>&1 | tail -1)
case "$out" in *"changed=0 provenance=UNCHANGED"*) gate recipe PASS "$out" ;; *) gate recipe FAIL "$out" ;; esac

(cd "$ROOT" && mvn "${MVN_COMMON[@]}" -f "$ROOT/m3/intern/pom.xml" clean package > "$SCRATCH/mvn-package.log" 2>&1)
warnings=$(grep -c '^\[WARNING\]' "$SCRATCH/mvn-package.log"); warnings=${warnings:-0}
if grep -q "BUILD SUCCESS" "$SCRATCH/mvn-package.log" && ! grep -qE "^\[ERROR\]" "$SCRATCH/mvn-package.log" && [ "$warnings" = 0 ]; then
    gate 1_package_lint_werror PASS "BUILD SUCCESS, [WARNING] lines=$warnings log=$SCRATCH/mvn-package.log"
else
    gate 1_package_lint_werror FAIL "[WARNING] lines=$warnings log=$SCRATCH/mvn-package.log"; grep -E "ERROR|FAIL" "$SCRATCH/mvn-package.log" | head -20
fi
figures=$(grep -E "^(M3ClassSpace Java21 self-test PASS|M3ClassFileAtoms PASS|M3_(REGION_INTERN|CLASS_BATCH_ATOMIC|CLASSSPACE_ARCHIVE|PINNED_MAPPED|SNAPSHOT_SCOPED|ARCHIVE_COPYTO)_PASS|M3_ARCHIVE_FREEZE_PARITY)" "$SCRATCH/mvn-package.log" | tr -d '\r' | tr '\n' ' ')
surefire=$(python -I - "$ROOT/m3/intern/target/surefire-reports" <<'EOF'
import glob, re, sys
t = f = e = s = 0
for p in glob.glob(sys.argv[1] + "/TEST-*.xml"):
    m = re.search(r'tests="(\d+)" errors="(\d+)" skipped="(\d+)" failures="(\d+)"', open(p, encoding="utf-8").read(2000))
    if m: t += int(m[1]); e += int(m[2]); s += int(m[3]); f += int(m[4])
print("SUREFIRE tests=%d failures=%d errors=%d skipped=%d" % (t, f, e, s))
EOF
)
count=$(printf '%s' "$figures" | grep -oE "PASS|PARITY" | wc -l)
case "$surefire" in *"failures=0 errors=0 skipped=0"*) [ "$count" -ge 9 ] && gate 2_oracle_test_phase PASS "$surefire; $figures" || gate 2_oracle_test_phase FAIL "$surefire; $figures" ;;
    *) gate 2_oracle_test_phase FAIL "$surefire; $figures" ;; esac

if bash "$RECIPE_DIR/differential.sh" "$DONOR_GIT" "$REV" "$SCRATCH/differential" > "$SCRATCH/differential.log" 2>&1; then
    gate 3_differential PASS "$(grep -E ' (IDENTICAL|DIFFERENT) |^mutant |^fault |^javap |^JNI SKIP' "$SCRATCH/differential.log" | awk '{ if ($1 == "mutant" || $1 == "fault") print $2"="$3; else if ($1 == "javap") print $0; else print $1"="$2 }' | tr '\n' ' ')"
else
    gate 3_differential FAIL "log=$SCRATCH/differential.log"; tail -8 "$SCRATCH/differential.log"
fi

if [ -n "$BASE" ]; then
    rm -rf "$SCRATCH/base"; mkdir -p "$SCRATCH/base"
    git -C "$ROOT" archive "$BASE" m3/intern | tar -x -C "$SCRATCH/base"
    (cd "$SCRATCH/base" && mvn "${MVN_COMMON[@]}" -DskipTests -f m3/intern/pom.xml clean package > "$SCRATCH/base-package.log" 2>&1)
    same=0; different=0
    while IFS= read -r cls; do
        if cmp -s "$SCRATCH/base/m3/intern/target/classes/$cls" "$ROOT/m3/intern/target/classes/$cls"; then same=$((same + 1)); else different=$((different + 1)); echo "untouched DIFFERENT $cls"; fi
    done < <(cd "$SCRATCH/base/m3/intern/target/classes" && find . -name '*.class' | sed 's#^\./##' | sort)
    added=$(cd "$ROOT/m3/intern/target/classes" && find . -name '*.class' | sed 's#^\./##' | sort | comm -13 <(cd "$SCRATCH/base/m3/intern/target/classes" && find . -name '*.class' | sed 's#^\./##' | sort) - | tr '\n' ' ')
    if [ "$different" = 0 ] && [ "$same" -gt 0 ]; then gate untouched PASS "base=$BASE identical=$same different=0 added: $added"; else gate untouched FAIL "identical=$same different=$different"; fi
else
    gate untouched SKIP "no base revision given"
fi

naming=$(cd "$ROOT" && python -I make/m3/tooling/jdk-naming/jdk_naming.py apply --root . --only m3/intern/ 2>&1 | grep -E "^(JDK_NAMING|REFUSED)" | tail -1)
case "$naming" in *"moves=0 rewrites=0 refusals=0"*) gate naming PASS "$naming" ;; *) gate naming FAIL "$naming" ;; esac
echo "VERIFY $status"
[ "$status" = PASS ]
