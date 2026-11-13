#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0
# Atom (pattern CHERRY_PICK_REPLAY_BY_MERGE_TREE): replay the non-merge commits of <base>..<head>, oldest first, onto
# <onto> without a worktree: each commit c is a three-way merge-tree with merge base c^, ours = the replay tip,
# theirs = c. A clean result becomes a commit with c's author, author date and message plus a "Replayed-from: <c>"
# line; a commit whose change is already in the tip (result tree == tip tree) is recorded EMPTY and skipped; a
# conflicting one is recorded CONFLICT with its paths and skipped (queue for a port). Writes no ref; prints the final
# tip on the last line. Ledger rows: commit, result, detail.
# Usage: replay_onto.sh <repo> <onto> <base> <head> <ledger.tsv>
set -uo pipefail
R="$1"; tip=$(git -C "$1" rev-parse "$2^{commit}"); BASE="$3"; HEAD_="$4"; OUT="$5"
printf 'commit\tresult\tdetail\n' > "$OUT"
for c in $(git -C "$R" rev-list --reverse --no-merges "$BASE..$HEAD_"); do
	mt=$(git -C "$R" -c merge.renames=false merge-tree --write-tree --name-only --no-messages --merge-base "$c^" "$tip" "$c" 2>&1); rc=$?
	tree=$(printf '%s\n' "$mt" | head -1)
	if [ $rc -eq 1 ]; then printf '%s\tCONFLICT\t%s\n' "$c" "$(printf '%s\n' "$mt" | tail -n +2 | tr '\n' ' ')" >> "$OUT"; continue; fi
	[ $rc -eq 0 ] || { printf '%s\tERROR\t%s\n' "$c" "$(printf '%s' "$mt" | tr '\n\t' '  ' | cut -c1-160)" >> "$OUT"; continue; }
	if [ "$tree" = "$(git -C "$R" rev-parse "$tip^{tree}")" ]; then printf '%s\tEMPTY\talready in the tip\n' "$c" >> "$OUT"; continue; fi
	msg=$(git -C "$R" log -1 --format=%B "$c"); an=$(git -C "$R" log -1 --format=%an "$c"); ae=$(git -C "$R" log -1 --format=%ae "$c"); ad=$(git -C "$R" log -1 --format=%aD "$c")
	tip=$(printf '%s\n\nReplayed-from: %s\n' "$msg" "$c" | GIT_AUTHOR_NAME="$an" GIT_AUTHOR_EMAIL="$ae" GIT_AUTHOR_DATE="$ad" \
		git -C "$R" -c user.name="Hitesh Soliwal" -c user.email="hsoliwal@gmail.com" commit-tree "$tree" -p "$tip") || { printf '%s\tERROR\tcommit-tree\n' "$c" >> "$OUT"; continue; }
	printf '%s\tREPLAYED\t%s\n' "$c" "${tip:0:12}" >> "$OUT"
done
cut -f2 "$OUT" | tail -n +2 | sort | uniq -c | tr '\n' ' '; echo; echo "$tip"
