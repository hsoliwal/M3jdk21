#!/bin/bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
#
# STRING_OP_CENSUS v1 (2026-11-16). usage: census.sh <git clone> <commit> <out-dir>
# Lexical count of String, StringBuilder and regex call sites in the main Java sources of a repository, read from the
# git tree (no checkout needed; the blobs must be present, GIT_NO_LAZY_FETCH stops a lazy fetch). Tests and its/ are
# left out. A call-site count ranks operations by how often the code names them, not by how often they run.
set -u
clone=$1; commit=$2; out=$3; mkdir -p "$out"
export GIT_OPTIONAL_LOCKS=0 GIT_TERMINAL_PROMPT=0 GIT_NO_LAZY_FETCH=1
RX='\.(startsWith|endsWith|indexOf|lastIndexOf|substring|replace|replaceAll|replaceFirst|split|trim|strip|isBlank|intern|formatted|matches|toLowerCase|toUpperCase|equalsIgnoreCase|charAt|toCharArray|getBytes|lines|concat|repeat|chars|codePointAt|regionMatches|compareToIgnoreCase|append)\(|String\.(format|join|valueOf)\(|Pattern\.compile\(|new (StringBuilder|StringBuffer|StringTokenizer)[ (<]|StringUtils\.[a-zA-Z]+\('
git -C "$clone" grep -o -h -E "$RX" "$commit" -- '*.java' ':!*/src/test/*' ':!its/*' > "$out/ops_only.txt" || exit 1
git -C "$clone" grep -c -E "$RX" "$commit" -- '*.java' ':!*/src/test/*' ':!its/*' | sed "s/^$commit://" > "$out/per_file.txt"
{ printf 'call_sites\toperation\n'; sed -E 's/^\.//; s/[ (<]$//' "$out/ops_only.txt" | sort | uniq -c | sort -nr \
    | awk '{printf "%s\t%s%s\n", $1, $2, ($3?" "$3:"")}'; } > "$out/STRING_OPS.tsv"
echo "sites $(wc -l < "$out/ops_only.txt") files $(wc -l < "$out/per_file.txt")"
