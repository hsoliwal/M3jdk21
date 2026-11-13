#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# regate.sh <module> <lane> <phase> [section]: ONE Maven run (-pl <module> -am -DskipTests=false <phase>) for a module
# already in the default reactor, after a recorded build-configuration fix; appends GATES.tsv + DECISION.tsv rows
# (decision REGATED_GREEN / REGATED_RED, gate "regate-<slug>-<phase>"). No pom edit, no module source touched.
# Env: W worktree, R scratch (mvn.sh, logs). Default W=S:/m3-scratch/eb-conv/wt-reactor R=S:/m3-scratch/eb-conv/reactor
set -u
M=$1; LANE=$2; PHASE=$3; SECTION=${4:-"lane 37"}
W=${W:-/s/m3-scratch/eb-conv/wt-reactor}; R=${R:-/s/m3-scratch/eb-conv/reactor}
Q=$W/make/m3/qualification/reactor-wiring-20261107
SLUG=$(echo "$M" | tr '/' '-'); NAME=regate-$SLUG-$PHASE; LOG=$R/logs/$NAME.log
cd "$W" || exit 9
grep -q "<module>$M</module>" make/m3/pom.xml || { echo "REGATE_ABORT $M not a reactor module"; exit 8; }
GAV=$(tr -d '\n' < "m3/$M/pom.xml" | sed -E 's/<parent>.*<\/parent>//; s/<dependencies>.*//; s/<build>.*//' | grep -o -E '<(groupId|artifactId|version|packaging)>[^<]+' | sed -E 's/<[a-zA-Z]+>//' | head -4 | tr '\n' ':' | sed 's/:$//')
CMD="mvn -B -ntp -T1 -f make/m3/pom.xml -pl $M -am -DskipTests=false $PHASE"
bash "$R/mvn.sh" "$NAME" -T1 -f make/m3/pom.xml -pl "$M" -am -DskipTests=false "$PHASE"
RC=$(cat "$LOG.rc" 2>/dev/null || echo 99)
TS=$(sed -n '1s/^START \([^ ]*\) .*/\1/p' "$LOG"); TE=$(grep -o -E '^END [^ ]+' "$LOG" | tail -1 | cut -d' ' -f2)
WALL=$(( $(date -d "$TE" +%s) - $(date -d "$TS" +%s) ))
BUILD=$(grep -o -E 'BUILD (SUCCESS|FAILURE)' "$LOG" | tail -1); BUILD=${BUILD:-NO_BUILD_RESULT}
BUILT=$(sed -n '/Reactor Summary/,/BUILD/p' "$LOG" | grep -E ' (SUCCESS|FAILURE|SKIPPED)( \[|$)' | sed -E 's/\[INFO\] //; s/ +\.* *(SUCCESS|FAILURE|SKIPPED).*/=\1/' | tr '\n' ';' | tr '\t' ' ')
OWN=$(grep -E '^\[(INFO|WARNING|ERROR)\] Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$' "$LOG" | tail -1 | sed -E 's/^\[[A-Z]+\] //'); OWN=${OWN:-n/a}
E_GOAL=$(grep -E '^\[ERROR\] Failed to execute goal' "$LOG" | head -1 | sed -E 's/^\[ERROR\] //' | tr '\t' ' ' | cut -c1-400)
FAILED_PROJECT=$(grep -o -E 'on project [^ :]+' "$LOG" | head -1 | sed 's/on project //'); FAILED_PROJECT=${FAILED_PROJECT:--}
LOGSHA=$(sha256sum "$LOG" | cut -c1-64)
if [ "$BUILD" = "BUILD SUCCESS" ]; then DEC=REGATED_GREEN; else DEC=REGATED_RED; fi
printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$SECTION" "$M" "$LANE" "$DEC" "$GAV" "-" "$NAME" "$BUILD" "$FAILED_PROJECT" "${FAILED_PROJECT}" "-" "${E_GOAL:--}" "$OWN" >> "$Q/DECISION.tsv"
printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$NAME" "$M" "$CMD" "$(cygpath -m "$LOG")" "$RC" "$BUILD" "$WALL" "${BUILT:--}" "$OWN" "$LOGSHA" >> "$Q/GATES.tsv"
echo "REGATE $M $PHASE $DEC build=$BUILD rc=$RC wall=${WALL}s own_tests=[$OWN] goal=${E_GOAL:--}"
