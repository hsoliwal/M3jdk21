#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Runs every gate of recipe intern-space-20261106 in order and prints one line per gate (see GATES.tsv):
#   recipe   relocate.py --check is a fixed point (files=18 held=9 pins=7 changed=0 provenance=UNCHANGED)
#   1        mvn package of make/m3/intern on JDK 21 (-Xlint:all -Werror, no lint exclusion), 0 compiler warnings, the 7
#            donor JUnit wrappers green
#   2        the seven proof mains print the crate README figures (default JIT and -Xint -Xmx128m)
#   3        differential.sh: donor tree vs relocated classes, proofs + the seeded dump byte-identical (default, -Xint)
#   4        javap -p parity of every main class modulo the PACKAGE map (printed by differential.sh)
#   5        scope: nothing outside make/m3/intern changes except one <module> line of make/m3/pom.xml; the reactor build of
#            make/m3/pom.xml -pl intern produces class files byte-identical to the standalone build
#   6        naming: jdk_naming.py apply --only make/m3/intern/ reports 0 moves, 0 rewrites, 0 refusals (files staged first)
#   7        make/m3/pom.xml parses and lists intern after collections (help:evaluate -N)
# usage: JAVA_HOME=<JDK 21> verify.sh <donor-git-dir> <donor-rev> <scratch-dir> [maven-repo-local] [base-rev]
set -uo pipefail
RECIPE_DIR=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$RECIPE_DIR/../../../.." && pwd)   # recipe dir = <root>/m3/intern/recipe/<name>
DONOR_GIT=$1; REV=$2; SCRATCH=$3; REPO=${4:-}; BASE=${5:-origin/master}
MVN=(mvn -B -ntp)
[ -n "$REPO" ] && MVN+=("-Dmaven.repo.local=$REPO")
MODULE="$ROOT/make/m3/intern"
mkdir -p "$SCRATCH"
status=PASS
gate() { local name=$1 verdict=$2; shift 2; echo "GATE $name $verdict $*"; [ "$verdict" = PASS ] || status=FAIL; }
started=$(date +%s)

out=$(python -I "$RECIPE_DIR/relocate.py" --donor-git "$DONOR_GIT" --donor-rev "$REV" --module "$MODULE" --check 2>&1 | tail -1)
case "$out" in *"changed=0 provenance=UNCHANGED"*) gate recipe PASS "$out" ;; *) gate recipe FAIL "$out" ;; esac

if (cd "$ROOT" && "${MVN[@]}" -f make/m3/intern/pom.xml clean package > "$SCRATCH/mvn-package.log" 2>&1) && grep -q "BUILD SUCCESS" "$SCRATCH/mvn-package.log"; then
    warnings=$(grep -c "WARNING.*\.java" "$SCRATCH/mvn-package.log")
    tests=$(grep -E "Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$" "$SCRATCH/mvn-package.log" | tail -1 | sed 's/.*Tests run/Tests run/')
    [ "$warnings" = 0 ] && gate 1_package_lint_werror PASS "BUILD SUCCESS warnings=0 $tests" || gate 1_package_lint_werror FAIL "warnings=$warnings"
else
    gate 1_package_lint_werror FAIL "log=$SCRATCH/mvn-package.log"; grep -E "ERROR|BUILD" "$SCRATCH/mvn-package.log" | head -10
fi

JAVA="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin/java"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; JAVA_MODULE=$(cd "$MODULE" && pwd -W) ;; *) SEP=':'; JAVA_MODULE=$MODULE ;; esac
CP="$JAVA_MODULE/target/classes$SEP$JAVA_MODULE/target/test-classes"   # a Windows JVM needs drive paths, not /s/...
figures=""; proofs_ok=PASS
while IFS=$'\t' read -r proof expected; do
    for flags in "" "-Xint -Xmx128m"; do
        line=$("$JAVA" $flags -cp "$CP" "jdk.m3.intern.$proof" 2>&1 | tail -1 | tr -d '\r')
        [ "$line" = "$proof PASS checks=$expected" ] || { proofs_ok=FAIL; echo "MISMATCH $proof flags=[$flags]: $line"; }
    done
    figures="$figures $proof=$expected"
done <<'EOF'
M3InternSpaceProof	21059
M3InternArchiveGuardProof	414
M3InternSnapshotProof	102
M3InternShapesProof	20028
M3InternChunkMatchProof	1147
M3InternPathTrieProof	38909
M3InternTrieScalarProof	46617
EOF
gate 2_proof_figures "$proofs_ok" "default+Xint:$figures"

if bash "$RECIPE_DIR/differential.sh" "$DONOR_GIT" "$REV" "$SCRATCH/differential" > "$SCRATCH/differential.log" 2>&1; then
    gate 3_differential PASS "$(grep -E '^dump-(default|xint) ' "$SCRATCH/differential.log" | awk '{print $1"="$2" "$3}' | tr '\n' ' ')$(grep -c ' IDENTICAL ' "$SCRATCH/differential.log") outputs IDENTICAL"
    gate 4_javap PASS "$(grep '^javap classes=' "$SCRATCH/differential.log")"
else
    gate 3_differential FAIL "log=$SCRATCH/differential.log"; grep -E 'DIFFERENT|DIFFERENTIAL' "$SCRATCH/differential.log" | head -5
    gate 4_javap FAIL "see $SCRATCH/differential.log"
fi

outside=$(cd "$ROOT" && { git diff --name-only "$BASE" -- . ; git ls-files --others --exclude-standard; } | sort -u | grep -v '^make/m3/intern/' | tr '\n' ' ')
pom_delta=$(cd "$ROOT" && git diff "$BASE" -- make/m3/pom.xml | grep -E '^[+-] ' | tr -d '\n')
mkdir -p "$SCRATCH/standalone-classes" && rm -rf "$SCRATCH/standalone-classes" && cp -r "$MODULE/target/classes" "$SCRATCH/standalone-classes"
if [ "$outside" = "make/m3/pom.xml " ] && [ "$pom_delta" = "+    <module>intern</module>" ] \
    && (cd "$ROOT" && "${MVN[@]}" -q -f make/m3/pom.xml -pl intern -DskipTests clean package > "$SCRATCH/reactor.log" 2>&1) \
    && diff -r "$SCRATCH/standalone-classes" "$MODULE/target/classes" > /dev/null; then
    gate 5_scope_untouched PASS "outside make/m3/intern: make/m3/pom.xml (+1 line '<module>intern</module>'); reactor -pl intern class files IDENTICAL to standalone ($(find "$MODULE/target/classes" -name '*.class' | wc -l) files)"
else
    gate 5_scope_untouched FAIL "outside=[$outside] pom=[$pom_delta] log=$SCRATCH/reactor.log"
fi

naming=$(cd "$ROOT" && python -I make/m3/tooling/jdk-naming/jdk_naming.py apply --root . --only make/m3/intern/ 2>&1 | grep -E "^(JDK_NAMING|REFUSED)" | tail -1)
case "$naming" in *"moves=0 rewrites=0 refusals=0"*) gate 6_naming PASS "$naming" ;; *) gate 6_naming FAIL "$naming (stage the make/m3/intern files first: jdk_naming.py reads git ls-files)" ;; esac

modules=$(cd "$ROOT" && "${MVN[@]}" -q -N -f make/m3/pom.xml help:evaluate -Dexpression=project.modules -DforceStdout 2>&1 | tr -d '\r\n ' )
case "$modules" in *"<string>collections</string><string>intern</string>"*) gate 7_pom_parses PASS "$(echo "$modules" | grep -o '<string>' | wc -l) default modules, intern after collections" ;; *) gate 7_pom_parses FAIL "$modules" ;; esac
echo "VERIFY $status $(( $(date +%s) - started ))s"
[ "$status" = PASS ]
