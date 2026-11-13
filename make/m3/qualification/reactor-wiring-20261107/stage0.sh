#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# stage0.sh <modules-csv | f:<pom-path>> <slug>: stage 0 of the reactor gate for a module whose -m3-source-1 dependency
# is only carried as sources under m3/vendor/third_party (standalone law: no Central artifact). ONE Maven run installs
# the named vendor modules into the head local repo (`-f m3/vendor/third_party/pom.xml -pl <csv> install -DskipTests`;
# a vendor pom the aggregator does not list is built directly: `f:<pom-path>` -> `-f <pom-path> install -DskipTests`),
# then gate.sh can resolve them. Records one GATES.tsv row (gate "stage0-<slug>"; wall time from the log's START/END
# stamps, so re-parsing an existing log keeps the real figure); edits no pom.
# Env: W worktree, R scratch (mvn.sh, logs). Default W=S:/m3-scratch/eb-conv/wt-reactor R=S:/m3-scratch/eb-conv/reactor
set -u
MODS=$1; SLUG=$2
W=${W:-/s/m3-scratch/eb-conv/wt-reactor}; R=${R:-/s/m3-scratch/eb-conv/reactor}
Q=$W/m3/qualification/reactor-wiring-20261107
LOG=$R/logs/stage0-$SLUG.log
cd "$W" || exit 9
case "$MODS" in
  f:*) POM=${MODS#f:}; SEL=(-f "$POM"); CMD="mvn -B -ntp -T1 -f $POM install -DskipTests" ;;
  *)   SEL=(-f m3/vendor/third_party/pom.xml -pl "$MODS"); CMD="mvn -B -ntp -T1 -f m3/vendor/third_party/pom.xml -pl $MODS install -DskipTests" ;;
esac
if [ ! -f "$LOG.rc" ] || [ "${STAGE0_RERUN:-0}" = 1 ]; then
  bash "$R/mvn.sh" "stage0-$SLUG" -T1 "${SEL[@]}" install -DskipTests
fi
RC=$(cat "$LOG.rc" 2>/dev/null || echo 99)
TS=$(sed -n '1s/^START \([^ ]*\) .*/\1/p' "$LOG"); TE=$(grep -o -E '^END [^ ]+' "$LOG" | tail -1 | cut -d' ' -f2)
WALL=$(( $(date -d "$TE" +%s) - $(date -d "$TS" +%s) ))
BUILD=$(grep -o -E 'BUILD (SUCCESS|FAILURE)' "$LOG" | tail -1); BUILD=${BUILD:-NO_BUILD_RESULT}
BUILT=$(sed -n '/Reactor Summary/,/BUILD/p' "$LOG" | grep -E 'SUCCESS|FAILURE|SKIPPED' | sed -E 's/\[INFO\] //; s/ +\.+ *(SUCCESS|FAILURE|SKIPPED).*/=\1/' | tr '\n' ';' | tr '\t' ' ')
INSTALLED=$(grep -o -E 'Installing .*\.jar to .*' "$LOG" | sed -E 's/.* to //' | tr '\134' '/' | sed -E 's#.*/m2/##' | tr '\n' ';' | cut -c1-400)
LOGSHA=$(sha256sum "$LOG" | cut -c1-64)
test -f "$Q/GATES.tsv" || printf 'gate\tmodule\tcommand\tlog\trc\tbuild\twall_s\treactor_built\town_tests\tlog_sha256\n' > "$Q/GATES.tsv"
printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "stage0-$SLUG" "vendor/third_party:$MODS" "$CMD" "$(cygpath -m "$LOG")" "$RC" "$BUILD" "$WALL" "${BUILT:--}" "installed=${INSTALLED:--}" "$LOGSHA" >> "$Q/GATES.tsv"
echo "STAGE0 $SLUG $BUILD rc=$RC wall=${WALL}s"
