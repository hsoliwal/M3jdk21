#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Runs the gates of GATES.tsv in order: recipe fixed point (relocate.py --check), Maven package of the sibling's module
# (m3/ports/indexstring, #2523: its test-phase mains and the dump driver), Maven package of m3/file without its tests
# (-Xlint:all,-module -Werror on main and test sources; -DskipTests -Dexec.skip=true: the module's surefire tests and its
# python3 verify.py execution are compared with the machine's baseline by the caller, GATES.tsv), the naming plan over
# the files of this leaf (0 moves / 0 rewrites / 0 refusals expected) and differential.sh. Maven logs go to
# <scratch-dir>/*-package.log and are grepped for BUILD SUCCESS.
# usage: verify.sh <donor-git-dir> <donor-rev> <scratch-dir>
# env:   JAVA_HOME (a JDK 21), MAVEN_REPO_LOCAL (optional -Dmaven.repo.local), PYTHON (default python)
set -euo pipefail
RECIPE=$(cd "$(dirname "$0")" && pwd)
MODULE=$(cd "$RECIPE/../.." && pwd)
ROOT=$(git -C "$MODULE" rev-parse --show-toplevel)
PY=${PYTHON:-python}
REPO=${MAVEN_REPO_LOCAL:+-Dmaven.repo.local=$MAVEN_REPO_LOCAL}
SIBLING=jdk.internal.m3.indexstring.M3IndexCharacterFlags
sibling_source=$(git -C "$ROOT" ls-files -- "*/src/main/java/${SIBLING//.//}.java")
SIBLING_MODULE="$ROOT/${sibling_source%%/src/main/java/*}"
mkdir -p "$3"
"$PY" -I "$RECIPE/relocate.py" --donor-git "$1" --donor-rev "$2" --check
mvn -B -ntp $REPO -f "$SIBLING_MODULE/pom.xml" package > "$3/sibling-package.log" 2>&1 || true
grep -E "_PASS |PASS actual|BUILD (SUCCESS|FAILURE)" "$3/sibling-package.log" || true
grep -q "BUILD SUCCESS" "$3/sibling-package.log"
mvn -B -ntp $REPO -DskipTests -Dexec.skip=true -f "$MODULE/pom.xml" package > "$3/file-package.log" 2>&1 || true
grep -E "BUILD (SUCCESS|FAILURE)|WARNING\].*\.java|ERROR\].*\.java" "$3/file-package.log" || true
grep -q "BUILD SUCCESS" "$3/file-package.log"
MODULE_REL=$(git -C "$MODULE" rev-parse --show-prefix)
TARGET_REL=$(awk -F'\t' '$1 == "FILE" {print $3; exit}' "$RECIPE/RELOCATION_MAP.tsv")
(cd "$ROOT" && "$PY" -I make/m3/tooling/jdk-naming/jdk_naming.py plan --only "$MODULE_REL$TARGET_REL" \
    --only "${MODULE_REL}recipe/${RECIPE##*/}/" | tail -n 1)
bash "$RECIPE/differential.sh" "$3/differential"
