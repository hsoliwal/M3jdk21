#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
# curate_bc_loop.sh <base git tree> <RENAME_MAP.tsv> <work-dir> [max]: compiler fixed point of curation B+C. Each round
# clones the base tree, applies curate_rename_bc.py with STAY_CORE, runs mvn -o test-compile (-Werror) and feeds the log to
# curate_bc_unmove.py. Stops CLEAN (prints the stay-core count) or STUCK.
set -uo pipefail
H=$(cd "$(dirname "$0")" && pwd); BASE="$1"; MAP="$2"; W="$3"; MAX=${4:-15}; mkdir -p "$W"
export JAVA_HOME="C:/Program Files/Java/jdk-21" PATH="/c/Program Files/Java/jdk-21/bin:$PATH"
touch "$W/STAY_CORE.txt" "$W/WHY.tsv"; export STAY_CORE="$W/STAY_CORE.txt"
for r in $(seq 0 "$MAX"); do
    rm -rf "$W/tree"; git -c core.autocrlf=false clone -q "$BASE" "$W/tree"; git -C "$W/tree" config core.autocrlf false
    python -I "$H/curate_rename_bc.py" "$W/tree" "$MAP" > "$W/apply.r$r.log"
    (cd "$W/tree" && timeout 2400 mvn -o -B -q test-compile > "../build.r$r.log" 2>&1); rc=$?
    if [ $rc = 0 ] && ! grep -qE '\[ERROR\]' "$W/build.r$r.log"; then
        echo "round $r: CLEAN, stay-core $(grep -c . "$W/STAY_CORE.txt")"; exit 0
    fi
    echo "round $r: $(grep -cE '\[(ERROR|WARNING)\] /.*\.java' "$W/build.r$r.log") diagnostics"
    python -I "$H/curate_bc_unmove.py" "$W/build.r$r.log" "$MAP" "$W/STAY_CORE.txt" "$W/WHY.tsv" || { echo "STUCK at round $r"; exit 2; }
done
echo "STUCK: round limit"; exit 2
