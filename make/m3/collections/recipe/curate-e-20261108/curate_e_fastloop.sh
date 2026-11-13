#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
# curate_e_fastloop.sh <base git tree> <intended RENAME_MAP_E1.tsv> <work-dir> [max]
# CURATE_E placement fixed point with plain javac (no 100-error cap): each round clones the base, applies
# curate_rename_e.py with the current map, compiles m3/arrays + m3/collections main and test sources with
# -Xmaxerrs 100000, and feeds every coupling error to curate_e_place.py (component placement; edges accumulate).
# Stops STABLE when a round adds no edge and javac is clean, then prints the final map path.
set -uo pipefail
H=$(cd "$(dirname "$0")" && pwd); BASE="$1"; INT="$2"; W="$3"; MAX=${4:-12}; mkdir -p "$W"
export PATH="/c/Program Files/Java/jdk-21/bin:$PATH" CURATE_CORE=jdk.internal.m3.collections TESTS_AS_OWNERS=1
JUNIT=$(ls ~/.m2/repository/org/junit/platform/junit-platform-console-standalone/1.10.1/*.jar | head -1)
cp "$INT" "$W/map.r0.tsv"; : > "$W/edges.tsv"
for r in $(seq 0 "$MAX"); do
    rm -rf "$W/cls"
    if [ -d "$W/tree/.git" ]; then git -C "$W/tree" checkout -q -- . && git -C "$W/tree" clean -fdq; else
        git -c core.autocrlf=false -c core.longpaths=true clone -q "$BASE" "$W/tree"; git -C "$W/tree" config core.autocrlf false; git -C "$W/tree" config core.longpaths true; fi
    python -I "$H/curate_rename_e.py" "$W/tree" "$W/map.r$r.tsv" > "$W/apply.r$r.log" || { echo "round $r: recipe refused"; cat "$W/apply.r$r.log"; exit 3; }
    find "$W/tree/m3/arrays/src/main/java" "$W/tree/m3/collections/src/main/java" "$W/tree/m3/collections/src/test/java" \
        -name "*.java" ! -name module-info.java > "$W/files.txt"
    mkdir -p "$W/cls"; javac --release 21 -nowarn -encoding UTF-8 -Xmaxerrs 100000 -proc:none -d "$W/cls" \
        -cp "$(cygpath -w "$JUNIT")" @"$W/files.txt" > "$W/javac.r$r.log" 2>&1; rc=$?
    ne=$(grep -c ': error:' "$W/javac.r$r.log"); ne=${ne:-0}
    python -I "$H/curate_e_place.py" "$INT" "$W/javac.r$r.log" "$W/tree" "$W/edges.tsv" "$W/map.r$((r+1)).tsv"; grew=$?
    echo "round $r: javac rc=$rc errors=$ne"
    if [ $rc = 0 ] && [ $grew != 0 ]; then echo "STABLE at round $r: $W/map.r$r.tsv"; exit 0; fi
    if [ $rc != 0 ] && [ $grew != 0 ]; then echo "STUCK at round $r (errors without new coupling edges)"; grep ': error:' "$W/javac.r$r.log" | head -8; exit 2; fi
done
echo "STUCK: round limit"; exit 2
