#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Runs every gate of recipe canonical-literals-20261106 in order and prints one line per gate (see GATES.tsv):
#   recipe   relocate.py --check is a fixed point (files=2 held=11 pins=9 rewrites=8 changed=0 provenance=UNCHANGED)
#   1        mvn package of make/m3/intern on JDK 21 (-Xlint:all -Werror, no lint exclusion), 0 compiler warnings, 17 tests
#            green (the 7 intern-space wrappers and the 10 relocated donor tests)
#   2        the donor test 10/10 on both sides: surefire report of the relocated class, JUnit console on the donor tree
#   3        differential.sh: donor pool over its own MIndexAtomStore vs relocated pool over M3InternSpace, dumps
#            byte-identical (default, -Xint), reference checks failures=0, javap -public API parity
#   4        the intern-space differential of PR #2549 (recipe intern-space-20261106) still prints its recorded dump digest
#   5        every class file of the base revision's make/m3/intern build is byte-identical in this build (new files listed)
#   6        naming: jdk_naming.py apply --only make/m3/intern/ reports 0 moves, 0 rewrites, 0 refusals (files staged first),
#            and relocate.py --check stays changed=0 afterwards
#   7        scope: every path that differs from the base revision (tracked or untracked, build output excluded) is
#            under make/m3/intern/
# usage: JAVA_HOME=<JDK 21> verify.sh <donor-git-dir> <scratch-dir> <maven-repo-local> [base-rev]
set -uo pipefail
RECIPE_DIR=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$RECIPE_DIR/../../../.." && pwd)   # recipe dir = <root>/m3/intern/recipe/<name>
DONOR_GIT=$1; SCRATCH=$2; REPO=$3; BASE=${4:-origin/master}
MVN=(mvn -B -ntp "-Dmaven.repo.local=$REPO")
MODULE="$ROOT/make/m3/intern"
INTERN_SPACE_REV=fd9a233680b43197eb8c5745a729c342ca56c4e2       # donor rev of recipe intern-space-20261106 (PR #2549)
INTERN_SPACE_DIGEST=0e6743bb88e2e2319a528ce24f3ba990aff5cf414f5e1ca1ccd783104b6b2952   # its GATES.tsv dump digest
DONOR_REV=$(awk -F'\t' '$1=="REPO" {print $3}' "$RECIPE_DIR/RELOCATION_MAP.tsv")
mkdir -p "$SCRATCH"
status=PASS
gate() { local name=$1 verdict=$2; shift 2; echo "GATE $name $verdict $*"; [ "$verdict" = PASS ] || status=FAIL; }
recipe_check() { python -I "$RECIPE_DIR/relocate.py" --donor-git "$DONOR_GIT" --donor-rev "$DONOR_REV" --module "$MODULE" --check 2>&1 | tail -1; }
started=$(date +%s)

out=$(recipe_check)
case "$out" in *"changed=0 provenance=UNCHANGED"*) gate recipe PASS "$out" ;; *) gate recipe FAIL "$out" ;; esac

if (cd "$ROOT" && "${MVN[@]}" -f make/m3/intern/pom.xml clean package > "$SCRATCH/mvn-package.log" 2>&1) && grep -q "BUILD SUCCESS" "$SCRATCH/mvn-package.log"; then
    warnings=$(grep -c "WARNING.*\.java" "$SCRATCH/mvn-package.log")
    tests=$(grep -E "Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$" "$SCRATCH/mvn-package.log" | tail -1 | sed 's/.*Tests run/Tests run/')
    case "$warnings $tests" in "0 Tests run: 17, Failures: 0, Errors: 0, Skipped: 0") gate 1_package_lint_werror PASS "BUILD SUCCESS warnings=0 $tests" ;;
        *) gate 1_package_lint_werror FAIL "warnings=$warnings $tests" ;; esac
else
    gate 1_package_lint_werror FAIL "log=$SCRATCH/mvn-package.log"; grep -E "ERROR|BUILD" "$SCRATCH/mvn-package.log" | head -10
fi

if bash "$RECIPE_DIR/differential.sh" "$DONOR_GIT" "$SCRATCH/differential" "$REPO" > "$SCRATCH/differential.log" 2>&1; then differential=PASS; else differential=FAIL; fi
report="$MODULE/target/surefire-reports/TEST-jdk.m3.intern.M3CanonicalLiteralPoolTest.xml"
attr() { grep -o '<testsuite [^>]*>' "$report" 2>/dev/null | grep -oE " $1=\"[0-9]+\"" | grep -oE '[0-9]+'; }   # surefire orders attributes freely
relocated="tests=$(attr tests) failures=$(attr failures) errors=$(attr errors) skipped=$(attr skipped)"
donor=$(grep '^donor-test ' "$SCRATCH/differential.log")
case "$relocated|$donor" in "tests=10 failures=0 errors=0 skipped=0|"*"successful=10 failed=0"*) gate 2_donor_test_both_sides PASS "relocated $relocated; $donor" ;;
    *) gate 2_donor_test_both_sides FAIL "relocated [$relocated] donor [$donor]" ;; esac
if [ "$differential" = PASS ]; then
    gate 3_differential PASS "$(grep -E '^(dump-(default|xint)|api) ' "$SCRATCH/differential.log" | awk '{print $1"="$2" "$3}' | tr '\n' ' ')$(grep '^TOTAL ' "$SCRATCH/differential/out/relocated-dump-default.txt")"
else
    gate 3_differential FAIL "log=$SCRATCH/differential.log"; grep -E 'DIFFERENT|FAIL' "$SCRATCH/differential.log" | head -5
fi

bash "$MODULE/recipe/intern-space-20261106/differential.sh" "$DONOR_GIT" "$INTERN_SPACE_REV" "$SCRATCH/intern-space" > "$SCRATCH/intern-space.log" 2>&1
digests=$(grep -E '^dump-(default|xint) IDENTICAL sha256='"$INTERN_SPACE_DIGEST" "$SCRATCH/intern-space.log" | wc -l)
if grep -q '^DIFFERENTIAL PASS' "$SCRATCH/intern-space.log" && [ "$digests" = 2 ]; then
    gate 4_intern_space_differential PASS "$(grep -c ' IDENTICAL ' "$SCRATCH/intern-space.log") outputs IDENTICAL, dump default+xint sha256=${INTERN_SPACE_DIGEST:0:12}..., $(grep '^javap classes=' "$SCRATCH/intern-space.log")"
else
    gate 4_intern_space_differential FAIL "log=$SCRATCH/intern-space.log"
fi

base_dir="$SCRATCH/base"
rm -rf "$base_dir" && mkdir -p "$base_dir"
(cd "$ROOT" && git archive "$BASE" make/m3/intern) | tar -x -C "$base_dir"
if (cd "$base_dir" && "${MVN[@]}" -q -f make/m3/intern/pom.xml -DskipTests clean package > "$SCRATCH/base-build.log" 2>&1); then
    same=0; differ=0
    while IFS= read -r f; do
        if cmp -s "$base_dir/make/m3/intern/target/classes/$f" "$MODULE/target/classes/$f"; then same=$((same + 1)); else differ=$((differ + 1)); echo "DIFFERENT $f"; fi
    done < <(cd "$base_dir/make/m3/intern/target/classes" && find . -name '*.class' | sort)
    added=$(comm -13 <(cd "$base_dir/make/m3/intern/target/classes" && find . -name '*.class' | sort) <(cd "$MODULE/target/classes" && find . -name '*.class' | sort) | sed 's#^\./##' | tr '\n' ' ')
    [ "$differ" = 0 ] && gate 5_untouched_classes PASS "base $(cd "$ROOT" && git rev-parse --short=12 "$BASE"): $same class files IDENTICAL, 0 different; added: $added" \
        || gate 5_untouched_classes FAIL "different=$differ"
else
    gate 5_untouched_classes FAIL "log=$SCRATCH/base-build.log"
fi

naming=$(cd "$ROOT" && python -I make/m3/tooling/jdk-naming/jdk_naming.py apply --root . --only make/m3/intern/ 2>&1 | grep -E "^(JDK_NAMING|REFUSED)" | tail -1)
after=$(recipe_check)
case "$naming|$after" in *"moves=0 rewrites=0 refusals=0"*"|"*"changed=0 provenance=UNCHANGED"*) gate 6_naming PASS "$naming; relocate.py --check after naming: changed=0" ;;
    *) gate 6_naming FAIL "$naming | $after (stage the make/m3/intern files first: jdk_naming.py reads git ls-files)" ;; esac

outside=$(cd "$ROOT" && { git diff --name-only "$BASE" -- . ; git ls-files --others --exclude-standard; } | sort -u | grep -vE '^make/m3/intern/target/' | grep -v '^make/m3/intern/' | tr '\n' ' ')
changed=$(cd "$ROOT" && { git diff --name-only "$BASE" -- . ; git ls-files --others --exclude-standard; } | sort -u | grep -vcE '^make/m3/intern/target/')
[ -z "$outside" ] && gate 7_scope PASS "$changed paths differ from the base, all under make/m3/intern/" || gate 7_scope FAIL "outside make/m3/intern: $outside"
echo "VERIFY $status $(( $(date +%s) - started ))s"
[ "$status" = PASS ]
