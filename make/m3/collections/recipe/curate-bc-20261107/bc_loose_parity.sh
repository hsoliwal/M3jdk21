#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
# bc_loose_parity.sh <worktree> <base classes> <curated classes> <out-dir> <file>...: compile parity for loose Java sources
# (recipe oracles, probes, script-compiled tests) touched by curation B+C. Each file is compiled twice with javac --release
# 21 and the JUnit console jar: its HEAD version against the base jdk.m3.collections classes, its curated version against
# the curated classes. PASS when both compile, or when both fail (a pre-existing dependency outside this change);
# REGRESSION when only the curated version fails. Prints one row per file.
set -uo pipefail
W="$1"; BASE="$2"; CUR="$3"; OUT="$4"; shift 4; mkdir -p "$OUT"
export PATH="/c/Program Files/Java/jdk-21/bin:$PATH"
J=$(ls ~/.m2/repository/org/junit/platform/junit-platform-console-standalone/1.10.1/*.jar | head -1)
for f in "$@"; do
    k=$(echo "$f" | tr '/' '_'); rm -rf "$OUT/$k"; mkdir -p "$OUT/$k/base" "$OUT/$k/cur" "$OUT/$k/src"
    git -C "$W" show "HEAD:$f" > "$OUT/$k/src/$(basename "$f")" 2>/dev/null
    javac --release 21 -nowarn -proc:none -d "$OUT/$k/base" -cp "$(cygpath -w "$BASE");$(cygpath -w "$J")" "$OUT/$k/src/$(basename "$f")" > "$OUT/$k/base.log" 2>&1; b=$?
    javac --release 21 -nowarn -proc:none -d "$OUT/$k/cur" -cp "$(cygpath -w "$CUR");$(cygpath -w "$J")" "$W/$f" > "$OUT/$k/cur.log" 2>&1; c=$?
    if [ $c = 0 ]; then v=PASS; elif [ $b != 0 ]; then v="PASS(both-fail)"; else v=REGRESSION; fi
    printf '%s\tbase=%s\tcurated=%s\t%s\n' "$f" "$b" "$c" "$v"
done
