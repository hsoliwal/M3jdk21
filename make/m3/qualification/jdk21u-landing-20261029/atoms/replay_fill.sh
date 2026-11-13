#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0
# Second pass of replay_onto.sh (pattern CHERRY_PICK_REPLAY_BY_MERGE_TREE + fill atoms): every CONFLICT row of
# <ledger> is retried on <tip>; each conflicted path gets a diff3 file (ours = tip, base = c^, theirs = c) and the
# fill atoms run in order (copyright_year_hunks.py, then hunk_contained_upstream.py with the pick as "upstream",
# so a tip hunk the pick already carries resolves to the pick). When no marker is left the commit is built from the
# merge-tree tree plus the filled paths (private index) and recorded FILLED; otherwise it stays CONFLICT with the
# paths still marked. Prints the final tip on the last line.
# Usage: replay_fill.sh <repo> <tip> <ledger.tsv> <work dir>
set -uo pipefail
R="$1"; tip=$(git -C "$1" rev-parse "$2^{commit}"); L="$3"; D="$4"; H=$(cygpath -m "$(cd "$(dirname "$0")" && pwd)"); mkdir -p "$D"   # Windows path: python gets it under MSYS_NO_PATHCONV
export GIT_INDEX_FILE="$D/index"
awk -F'\t' '$2 == "CONFLICT" { print $1 }' "$L" > "$D/todo"
while read -r c; do
	mt=$(git -C "$R" -c merge.renames=false merge-tree --write-tree --name-only --no-messages --merge-base "$c^" "$tip" "$c"); tree=$(printf '%s\n' "$mt" | head -1)
	rm -f "$GIT_INDEX_FILE"; git -C "$R" read-tree "$tree"; left=""
	for p in $(printf '%s\n' "$mt" | tail -n +2); do
		f="$D/$(printf '%s' "${c:0:9}_$p" | tr '/' '_')"
		git -C "$R" show "$c^:$p" > "$f.base"; git -C "$R" show "$tip:$p" > "$f.ours"; git -C "$R" show "$c:$p" > "$f.theirs"
		cp "$f.ours" "$f.m"; git merge-file --diff3 -L tip -L pick-base -L pick "$f.m" "$f.base" "$f.theirs"
		python -I "$H/copyright_year_hunks.py" "$(cygpath -m "$f.m")" > /dev/null || python -I "$H/hunk_contained_upstream.py" "$(cygpath -m "$f.m")" "$(cygpath -m "$f.theirs")" > /dev/null
		if grep -q -E '^(<<<<<<< |>>>>>>> )' "$f.m"; then left="$left $p"; continue; fi
		b=$(git -C "$R" hash-object -w --no-filters "$f.m"); m=$(git -C "$R" ls-tree "$tip" -- "$p" | cut -d' ' -f1)
		git -C "$R" update-index --cacheinfo "${m:-100644},$b,$p"
	done
	if [ -n "$left" ]; then printf '%s\tCONFLICT\t%s\n' "$c" "$left" >> "$L.fill"; continue; fi
	nt=$(git -C "$R" write-tree); msg=$(git -C "$R" log -1 --format=%B "$c")
	tip=$(printf '%s\n\nReplayed-from: %s\nFilled-by: copyright_year_hunks.py, hunk_contained_upstream.py\n' "$msg" "$c" |
		GIT_AUTHOR_NAME="$(git -C "$R" log -1 --format=%an "$c")" GIT_AUTHOR_EMAIL="$(git -C "$R" log -1 --format=%ae "$c")" GIT_AUTHOR_DATE="$(git -C "$R" log -1 --format=%aD "$c")" \
		git -C "$R" -c user.name="Hitesh Soliwal" -c user.email="hsoliwal@gmail.com" commit-tree "$nt" -p "$tip")
	printf '%s\tFILLED\t%s\n' "$c" "${tip:0:12}" >> "$L.fill"
done < "$D/todo"
rm -f "$GIT_INDEX_FILE"; cut -f2 "$L.fill" | sort | uniq -c | tr '\n' ' '; echo; echo "$tip"
