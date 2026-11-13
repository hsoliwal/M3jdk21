#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Runs every gate of recipe static-use-plan-20261107 in order and prints one line per gate (see GATES.tsv):
#   recipe   relocate.py --check is a fixed point (changed=0 provenance=UNCHANGED)
#   1        mvn clean package of m3/compiler/static-use on JDK 21 (-Xlint:all -Werror on main and test sources), 0 [WARNING]
#   2        the donor test and the dump ran in the test phase (surefire totals, dump figures)
#   3        differential.sh: donor vs relocated dumps byte-identical (default, -Xint), donor JUnit test, mutants refused, javap
#   reactor  mvn -f make/m3/pom.xml -DskipTests package -pl compiler/static-use -am (the module is a default reactor module)
#   naming   jdk_naming.py apply --only m3/compiler/static-use/ reports 0 moves, 0 rewrites, 0 refusals (new files staged first)
#   siblings every m3/compiler/<dir> module descriptor is 'module org.m3jdk.compiler.<x>' exporting jdk.m3.compiler.<x>, every
#            m3/compiler pom has groupId com.m3, and every main source package is jdk.m3.compiler.<x> (this module included)
# Every JVM gets -Djava.io.tmpdir=<scratch>/tmp (surefire through -DargLine, Maven through MAVEN_OPTS).
# usage: JAVA_HOME=<JDK 21> verify.sh <donor-git-dir> <donor-rev> <scratch-dir> <junit-console-jar> [maven-repo-local [maven-repo-local-tail]]
set -uo pipefail
RECIPE_DIR=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$RECIPE_DIR/../../../../.." && pwd)   # recipe dir = <root>/m3/compiler/static-use/recipe/<name>
DONOR_GIT=$1; REV=$2; SCRATCH=$3; CONSOLE=$4; REPO=${5:-}; TAIL=${6:-}
mkdir -p "$SCRATCH/tmp"
TMPDIR_JVM=$(cd "$SCRATCH/tmp" && (pwd -W 2>/dev/null || pwd))
MVN_COMMON=(-B -ntp "-DargLine=-Djava.io.tmpdir=$TMPDIR_JVM")
[ -n "$REPO" ] && MVN_COMMON+=("-Dmaven.repo.local=$REPO")
[ -n "$TAIL" ] && MVN_COMMON+=("-Dmaven.repo.local.tail=$TAIL")
export MAVEN_OPTS="${MAVEN_OPTS:-} -Djava.io.tmpdir=$TMPDIR_JVM"
MODULE="$ROOT/m3/compiler/static-use"
status=PASS
gate() { local name=$1 verdict=$2; shift 2; echo "GATE $name $verdict $*"; [ "$verdict" = PASS ] || [ "$verdict" = SKIP ] || status=FAIL; }

out=$(python -I "$RECIPE_DIR/relocate.py" --donor-git "$DONOR_GIT" --donor-rev "$REV" --module "$MODULE" --check 2>&1 | tail -1)
case "$out" in *"changed=0 provenance=UNCHANGED"*) gate recipe PASS "$out" ;; *) gate recipe FAIL "$out" ;; esac

(cd "$ROOT" && mvn "${MVN_COMMON[@]}" -f "$MODULE/pom.xml" clean package > "$SCRATCH/mvn-package.log" 2>&1)
warnings=$(grep -c '^\[WARNING\]' "$SCRATCH/mvn-package.log"); warnings=${warnings:-0}
if grep -q "BUILD SUCCESS" "$SCRATCH/mvn-package.log" && ! grep -qE "^\[ERROR\]" "$SCRATCH/mvn-package.log" && [ "$warnings" = 0 ]; then
    gate 1_package_lint_werror PASS "BUILD SUCCESS, [WARNING] lines=$warnings log=$SCRATCH/mvn-package.log"
else
    gate 1_package_lint_werror FAIL "[WARNING] lines=$warnings log=$SCRATCH/mvn-package.log"; grep -E "ERROR|FAIL" "$SCRATCH/mvn-package.log" | head -20
fi
surefire=$(grep -E "^\[INFO\] Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$" "$SCRATCH/mvn-package.log" | tail -1 | tr -d '\r')
figures=$(grep -E "^(facts|loops|growth|CHECKS|RESULT|CompilerOwnershipProof)" "$SCRATCH/mvn-package.log" | cut -d' ' -f1-4 | tr -d '\r' | tr '\n' ' ')
case "$surefire $figures" in *"Failures: 0, Errors: 0, Skipped: 0"*"RESULT PASS"*) gate 2_test_phase PASS "$surefire; $figures" ;; *) gate 2_test_phase FAIL "$surefire; $figures" ;; esac

if bash "$RECIPE_DIR/differential.sh" "$DONOR_GIT" "$REV" "$SCRATCH/differential" "$CONSOLE" > "$SCRATCH/differential.log" 2>&1; then
    gate 3_differential PASS "$(grep -E ' (IDENTICAL|DIFFERENT) |^mutant |^donor-junit |^javap ' "$SCRATCH/differential.log" | awk '{ if ($1 == "mutant") print $2"="$3; else if ($1 == "javap" || $1 == "donor-junit") print $0; else print $1"="$2 }' | tr -s ' ' | tr '\n' ' ')"
else
    gate 3_differential FAIL "log=$SCRATCH/differential.log"; tail -8 "$SCRATCH/differential.log"
fi

(cd "$ROOT" && mvn "${MVN_COMMON[@]}" -f make/m3/pom.xml -DskipTests package -pl compiler/static-use -am > "$SCRATCH/reactor-package.log" 2>&1)
if grep -q "BUILD SUCCESS" "$SCRATCH/reactor-package.log" && ! grep -qE "^\[ERROR\]" "$SCRATCH/reactor-package.log"; then
    gate reactor PASS "mvn -f make/m3/pom.xml -DskipTests package -pl compiler/static-use -am: BUILD SUCCESS ($(grep -cE '^\[INFO\] Building ' "$SCRATCH/reactor-package.log") project) log=$SCRATCH/reactor-package.log"
else
    gate reactor FAIL "log=$SCRATCH/reactor-package.log"
fi

siblings=$(python -I - "$ROOT/m3/compiler" <<'EOF'
import re, sys
from pathlib import Path
root = Path(sys.argv[1]); rows = []; bad = 0
for module in sorted(p for p in root.iterdir() if (p / "pom.xml").is_file()):
    pom = (module / "pom.xml").read_text(encoding="utf-8")
    group = re.search(r"<modelVersion>4.0.0</modelVersion>\s*<groupId>([^<]+)</groupId>\s*<artifactId>([^<]+)</artifactId>", pom)
    packages = sorted({m.group(1) for f in (module / "src/main/java").rglob("*.java") if f.name != "module-info.java"
                       for m in [re.search(r"(?m)^package\s+([\w.]+);", f.read_text(encoding="utf-8"))] if m})
    info = module / "src/main/java/module-info.java"
    name = exports = "-"
    if info.is_file():
        text = info.read_text(encoding="utf-8")
        name = re.search(r"(?m)^\s*module\s+([\w.]+)", text).group(1)
        exports = ",".join(re.findall(r"(?m)^\s*exports\s+([\w.]+);", text))
        ok = bool(re.fullmatch(r"org\.m3jdk\.compiler\.[a-z]+", name)) and exports == "jdk.m3.compiler." + name.rsplit(".", 1)[1]
    else:
        ok = True
    ok = ok and group is not None and group.group(1) == "com.m3" and all(p.startswith("jdk.m3.compiler.") for p in packages)
    bad += not ok
    rows.append("%s:%s/%s:%s:%s:%s" % (module.name, group.group(1) if group else "?", group.group(2) if group else "?", name, exports, "|".join(packages)))
print(("OK " if bad == 0 else "MISMATCH ") + " ".join(rows))
EOF
)
case "$siblings" in OK*) gate siblings PASS "$siblings" ;; *) gate siblings FAIL "$siblings" ;; esac

naming=$(cd "$ROOT" && python -I make/m3/tooling/jdk-naming/jdk_naming.py apply --root . --only m3/compiler/static-use/ 2>&1 | grep -E "^(JDK_NAMING|REFUSED)" | tail -1)
case "$naming" in *"moves=0 rewrites=0 refusals=0"*) gate naming PASS "$naming" ;; *) gate naming FAIL "$naming" ;; esac
echo "VERIFY $status"
[ "$status" = PASS ]
