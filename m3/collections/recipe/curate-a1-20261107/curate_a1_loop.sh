#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
# curate_a1_loop.sh <base git tree> <a1.txt> <work-dir> [max rounds]: phase A1 compiler fixed point (v2). Each round
# clones the base tree, applies curate_apply_a1.py with the current moving list and the compiler-placed test lists
# (TEST_MOVE / TEST_STAY), runs mvn -o test-compile (-Werror) and feeds the log to curate_a1_unmove.py; stops when the
# build passes (CLEAN) or nothing changes (STUCK). The lists persist in <work-dir> (a1.final.txt, TEST_MOVE.txt,
# TEST_STAY.txt, UNMOVED.tsv) and form the recipe input of the landing.
set -uo pipefail
H=$(cd "$(dirname "$0")" && pwd); BASE="$1"; LIST="$2"; W="$3"; MAX=${4:-15}; mkdir -p "$W"
export JAVA_HOME="C:/Program Files/Java/jdk-21" PATH="/c/Program Files/Java/jdk-21/bin:$PATH"
cp "$LIST" "$W/a1.r0.txt"; touch "$W/UNMOVED.tsv" "$W/TEST_MOVE.txt" "$W/TEST_STAY.txt"
export TEST_MOVE="$W/TEST_MOVE.txt" TEST_STAY="$W/TEST_STAY.txt"
for r in $(seq 0 "$MAX"); do
    rm -rf "$W/tree"; git -c core.autocrlf=false clone -q "$BASE" "$W/tree"
    python -I "$H/curate_apply_a1.py" "$W/tree" "$W/a1.r$r.txt" > "$W/apply.r$r.log"
    (cd "$W/tree" && timeout 2400 mvn -o -B test-compile > "../build.r$r.log" 2>&1)
    if grep -q 'BUILD SUCCESS' "$W/build.r$r.log"; then
        echo "round $r: CLEAN, MOVING $(wc -l < "$W/a1.r$r.txt")"; cp "$W/a1.r$r.txt" "$W/a1.final.txt"; exit 0
    fi
    echo "round $r: $(grep -cE '\[(ERROR|WARNING)\] /.*\.java' "$W/build.r$r.log") diagnostics"
    python -I "$H/curate_a1_unmove.py" "$W/build.r$r.log" "$W/a1.r$r.txt" "$W/a1.r$((r + 1)).txt" "$W/UNMOVED.tsv" \
        "$W/TEST_MOVE.txt" "$W/TEST_STAY.txt" || { echo "STUCK at round $r"; exit 2; }
done
echo "STUCK: round limit"; exit 2
