#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# gate.sh <module> <lane> [section]: one reactor-wiring gate (recipe REACTOR_WIRING_GATE).
# 1 temporary <module> line in m3/pom.xml (unrecorded)  2 ONE Maven run: -pl <module> -am -DskipTests=false package
# 3 revert the temporary line  4 BUILD SUCCESS -> record the WIRED edit; else -> record the HELD edit (profile
# m3-offreactor-held) with the first error  5 DECISION.tsv + GATES.tsv rows  6 one commit. No module source is touched.
# Env: W worktree, R scratch (mvn.sh, logs). Default W=S:/m3-scratch/eb-conv/wt-reactor R=S:/m3-scratch/eb-conv/reactor
set -u
M=$1; LANE=$2; SECTION=${3:-"lane 37"}
W=${W:-/s/m3-scratch/eb-conv/wt-reactor}; R=${R:-/s/m3-scratch/eb-conv/reactor}
Q=$W/m3/qualification/reactor-wiring-20261107
AP="python -I $Q/apply.py"
SLUG=$(echo "$M" | tr '/' '-')
LOG=$R/logs/gate-$SLUG.log
cd "$W" || exit 9
git diff --quiet -- m3/pom.xml || { echo "GATE_ABORT $M dirty pom"; exit 8; }
$AP verify --root . >/dev/null || { echo "GATE_ABORT $M pom not at EDITS chain final"; exit 8; }
test -f "m3/$M/pom.xml" || { echo "GATE_ABORT $M no pom"; exit 8; }
skel() { tr -d '\n' < "m3/$M/pom.xml" | sed -E 's/<parent>.*<\/parent>//; s/<dependencies>.*//; s/<build>.*//; s/<profiles>.*//'; }
GA=$(skel | grep -o -E '<(groupId|artifactId)>[^<]+' | sed -E 's/<[a-zA-Z]+>//' | head -2 | tr '\n' ':' | sed 's/:$//')
GAV_FULL=$(skel | grep -o -E '<(groupId|artifactId|version|packaging)>[^<]+' | sed -E 's/<[a-zA-Z]+>//' | head -4 | tr '\n' ':' | sed 's/:$//')
RDEPS=$(tr -d '\n' < "m3/$M/pom.xml" | sed -E 's/<dependencyManagement>.*<\/dependencyManagement>//; s/<build>.*//; s/<profiles>.*//' | grep -o -E '<dependency>.*</dependency>' | sed -E 's/<\/dependency>/\n/g' | grep -o -E '<groupId>com\.(m3|synexia)[^<]*</groupId>[[:space:]]*<artifactId>[^<]+</artifactId>([[:space:]]*<version>[^<]+</version>)?' | sed -E 's/<\/?(groupId|artifactId|version)>/:/g; s/::/:/g; s/^://; s/:$//; s/[[:space:]]+//g' | sort -u | tr '\n' ' ')
# 1 temporary wiring
$AP edit --root . --file m3/pom.xml --anchor '  </modules>' --text "    <module>$M</module>" >/dev/null || { echo "GATE_ABORT $M temp edit"; exit 8; }
# 2 one Maven run
CMD="mvn -B -ntp -T1 -f m3/pom.xml -pl $M -am -DskipTests=false package"
T0=$(date +%s)
bash "$R/mvn.sh" "gate-$SLUG" -T1 -f m3/pom.xml -pl "$M" -am -DskipTests=false package
RC=$(cat "$LOG.rc" 2>/dev/null || echo 99); T1=$(date +%s)
# 3 revert
git checkout -q -- m3/pom.xml
# 4 parse
BUILD=$(grep -o -E 'BUILD (SUCCESS|FAILURE)' "$LOG" | tail -1); BUILD=${BUILD:-NO_BUILD_RESULT}
BUILT=$(sed -n '/Reactor Summary/,/BUILD/p' "$LOG" | grep -E 'SUCCESS|FAILURE|SKIPPED' | sed -E 's/\[INFO\] //; s/ +\.+ *(SUCCESS|FAILURE|SKIPPED).*/=\1/' | tr '\n' ';' | tr '\t' ' ')
HDR=$(grep -n -F "< $GA >" "$LOG" | head -1 | cut -d: -f1); HDR=${HDR:-1}
OWN=$(tail -n +"$HDR" "$LOG" | grep -E '^\[(INFO|WARNING|ERROR)\] Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$' | tail -1 | sed -E 's/^\[[A-Z]+\] //'); OWN=${OWN:-n/a}
FAILED_PROJECT=$(grep -o -E 'on project [^ :]+' "$LOG" | head -1 | sed 's/on project //'); FAILED_PROJECT=${FAILED_PROJECT:--}
E_COMPILE=$(grep -E '^\[ERROR\] (/|[A-Za-z]:)[^ ]+\.java:\[[0-9]+,[0-9]+\] ' "$LOG" | head -1)
E_TEST=$(grep -E '^\[ERROR\]   [A-Za-z0-9_.$]+:[0-9]+ ' "$LOG" | head -1)
E_GOAL=$(grep -E '^\[ERROR\] Failed to execute goal' "$LOG" | head -1)
E_ANY=$(grep -E '^\[ERROR\] ' "$LOG" | grep -v -E 'Tests run:|^\[ERROR\] *$|Failures: *$|Errors: *$' | head -1)
if [ -n "$E_COMPILE" ]; then
  EF=$(echo "$E_COMPILE" | sed -E 's/^\[ERROR\] ([^ ]+\.java):\[[0-9]+,[0-9]+\] .*$/\1/')
  EL=$(echo "$E_COMPILE" | sed -E 's/^\[ERROR\] [^ ]+\.java:\[([0-9]+),[0-9]+\] .*$/\1/')
  EM=$(echo "$E_COMPILE" | sed -E 's/^\[ERROR\] [^ ]+\.java:\[[0-9]+,[0-9]+\] //')
elif [ -n "$E_TEST" ]; then
  EF=$(echo "$E_TEST" | sed -E 's/^\[ERROR\]   ([A-Za-z0-9_.$]+):[0-9]+ .*$/\1/')
  EL=$(echo "$E_TEST" | sed -E 's/^\[ERROR\]   [A-Za-z0-9_.$]+:([0-9]+) .*$/\1/')
  EM=$(echo "$E_TEST" | sed -E 's/^\[ERROR\]   [A-Za-z0-9_.$]+:[0-9]+ //')
elif [ -n "$E_GOAL" ]; then EF="$FAILED_PROJECT"; EL="-"; EM=$(echo "$E_GOAL" | sed -E 's/^\[ERROR\] //')
elif [ -n "$E_ANY" ]; then EF="-"; EL="-"; EM=$(echo "$E_ANY" | sed -E 's/^\[ERROR\] //')
else EF="-"; EL="-"; EM="-"; fi
EF=$(echo "$EF" | sed -E 's#^/?[A-Za-z]:[/\\]m3-scratch[/\\]eb-conv[/\\]wt-[a-z-]+[/\\]##' | tr '\\' '/' | tr '\t' ' ')
EM=$(echo "$EM" | tr '\t' ' ' | cut -c1-400)
LOGSHA=$(sha256sum "$LOG" | cut -c1-64)
# 5 decision + recorded edit
if [ "$BUILD" = "BUILD SUCCESS" ]; then
  DEC=WIRED
  TEXT=$(printf '    <!-- //FLLDCIM: MRL - reactor-wiring-20261107 %s: %s WIRED (gate gate-%s, GATES.tsv) -->\n    <module>%s</module>' "$LANE" "$M" "$SLUG" "$M")
  $AP record --root . --file m3/pom.xml --anchor '  </modules>' --text "$TEXT" --note "$M WIRED ($LANE)" || exit 7
else
  DEC=HELD
  if ! grep -q 'm3-offreactor-held:end' m3/pom.xml; then
    PTEXT=$(printf '    <profile>\n      <!-- //FLLDCIM: MRL - reactor-wiring-20261107: poms outside the default reactor whose wiring gate was RED (first error in DECISION.tsv); one flag away: -Pm3-offreactor-held -->\n      <id>m3-offreactor-held</id>\n      <modules>\n        <!-- m3-offreactor-held:end -->\n      </modules>\n    </profile>')
    $AP record --root . --file m3/pom.xml --anchor '  </profiles>' --text "$PTEXT" --note "profile m3-offreactor-held created" || exit 7
  fi
  TEXT=$(printf '        <!-- //FLLDCIM: MRL - reactor-wiring-20261107 %s: %s HELD (gate gate-%s: %s; first error in DECISION.tsv) -->\n        <module>%s</module>' "$LANE" "$M" "$SLUG" "$BUILD" "$M")
  $AP record --root . --file m3/pom.xml --anchor '        <!-- m3-offreactor-held:end -->' --text "$TEXT" --note "$M HELD ($LANE)" || exit 7
fi
# 6 ledgers + commit
test -f "$Q/DECISION.tsv" || printf 'section\tmodule\tlane\tdecision\tgav\treactor_deps\tgate\tbuild\tfailed_project\tfirst_error_file\tfirst_error_line\tfirst_error_message\town_tests\n' > "$Q/DECISION.tsv"
test -f "$Q/GATES.tsv" || printf 'gate\tmodule\tcommand\tlog\trc\tbuild\twall_s\treactor_built\town_tests\tlog_sha256\n' > "$Q/GATES.tsv"
printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$SECTION" "$M" "$LANE" "$DEC" "$GAV_FULL" "${RDEPS:--}" "gate-$SLUG" "$BUILD" "$FAILED_PROJECT" "$EF" "$EL" "$EM" "$OWN" >> "$Q/DECISION.tsv"
printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "gate-$SLUG" "$M" "$CMD" "$(cygpath -m "$LOG")" "$RC" "$BUILD" "$((T1-T0))" "${BUILT:--}" "$OWN" "$LOGSHA" >> "$Q/GATES.tsv"
git add m3/pom.xml m3/qualification/reactor-wiring-20261107 && git commit -q -m "reactor-wiring: $M $DEC ($LANE)" || { echo "GATE_ABORT $M commit"; exit 7; }
echo "GATE_DONE $M $DEC build=$BUILD rc=$RC wall=$((T1-T0))s own_tests=[$OWN] failed_project=$FAILED_PROJECT first_error=$EF:$EL $(echo "$EM" | cut -c1-160)"
