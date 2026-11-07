#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# fullrun.sh <log-name> <TAG>: record one full-reactor run (`mvn -B -ntp -T1 -fae -f m3/pom.xml verify
# -Dmaven.test.failure.ignore=true`, already executed through mvn.sh) as a GATES.tsv row and two ledgers,
# <TAG>_TESTS.tsv (testsum.py, one row per module) and <TAG>_FAILING.tsv (testsum.py --failing, sorted, for cmp with
# an earlier run's failing set). Runs no Maven; wall time from the log's START/END stamps.
# Env: W worktree, R scratch (logs). Default W=S:/m3-scratch/eb-conv/wt-reactor R=S:/m3-scratch/eb-conv/reactor
set -u
NAME=$1; TAG=$2
W=${W:-/s/m3-scratch/eb-conv/wt-reactor}; R=${R:-/s/m3-scratch/eb-conv/reactor}
Q=$W/m3/qualification/reactor-wiring-20261107
LOG=$R/logs/$NAME.log
cd "$W" || exit 9
test -f "$LOG" || { echo "FULLRUN_ABORT no log $LOG"; exit 8; }
CMD=$(sed -n '1s/^START [^ ]* //p' "$LOG" | sed -E 's/^mvn /mvn -B -ntp /')
RC=$(cat "$LOG.rc" 2>/dev/null || echo 99)
TS=$(sed -n '1s/^START \([^ ]*\) .*/\1/p' "$LOG"); TE=$(grep -o -E '^END [^ ]+' "$LOG" | tail -1 | cut -d' ' -f2)
WALL=$(( $(date -d "$TE" +%s) - $(date -d "$TS" +%s) ))
BUILD=$(grep -o -E 'BUILD (SUCCESS|FAILURE)' "$LOG" | tail -1); BUILD=${BUILD:-NO_BUILD_RESULT}
SUMMARY=$(sed -n '/Reactor Summary/,/BUILD/p' "$LOG")
NS=$(echo "$SUMMARY" | grep -c -E ' SUCCESS \['); NF=$(echo "$SUMMARY" | grep -c -E ' FAILURE \['); NK=$(echo "$SUMMARY" | grep -c -E ' SKIPPED *$')
TOTAL=$(grep -E '^\[(INFO|WARNING|ERROR)\] Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$' "$LOG" | sed -E 's/^\[[A-Z]+\] //' | awk -F'[:,]' '{r+=$2; f+=$4; e+=$6; s+=$8} END {printf "Tests run: %d, Failures: %d, Errors: %d, Skipped: %d", r, f, e, s}')
LOGSHA=$(sha256sum "$LOG" | cut -c1-64)
python -I "$Q/testsum.py" "$LOG" > "$Q/${TAG}_TESTS.tsv"
python -I "$Q/testsum.py" "$LOG" --failing > "$Q/${TAG}_FAILING.tsv"
printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$NAME" "-" "$CMD" "$(cygpath -m "$LOG")" "$RC" "$BUILD" "$WALL" "$NS SUCCESS;$NF FAILURE;$NK SKIPPED" "$TOTAL (sum of module summaries)" "$LOGSHA" >> "$Q/GATES.tsv"
echo "FULLRUN $NAME $BUILD rc=$RC wall=${WALL}s modules=$NS/$NF/$NK $TOTAL failing_rows=$(grep -c . "$Q/${TAG}_FAILING.tsv")"
