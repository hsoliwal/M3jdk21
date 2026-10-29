#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0
# Atom CHERRY_PICK_PLUMBING: `git cherry-pick -x` without a worktree, oldest first. Per commit c: merge-tree with
# merge base c^ (the cherry-pick merge), commit-tree with c's author/date and message plus the -x line, committer
# Hitesh Soliwal. A commit that conflicts on the moving tip is skipped and logged, never resolved by hand here.
# Usage: pick_batch.sh <repo> <base rev> <commit list> <ref to write> <log.tsv>
set -uo pipefail
R="$1"; tip=$(git -C "$1" rev-parse "$2^{commit}"); LIST="$3"; REF="$4"; LOG="$5"; T=$(mktemp -d); export GIT_NO_LAZY_FETCH=1
export GIT_COMMITTER_NAME="Hitesh Soliwal" GIT_COMMITTER_EMAIL="hsoliwal@gmail.com"
G() { git -C "$R" -c merge.renames=false -c diff.renames=false "$@"; }
printf 'upstream_commit\tresult\tnew_commit\tdetail\n' > "$LOG"; n=0
while read -r c; do
	if out=$(G merge-tree --write-tree --merge-base="$c^" "$tip" "$c" 2> "$T/err"); then
		tree=${out%%$'\n'*}
		G log -1 --format=%B "$c" | sed -e :a -e '/^\n*$/{$d;N;ba' -e '}' > "$T/msg"
		last=$(awk 'BEGIN{RS="";} {p=$0} END{print p}' "$T/msg")
		if printf '%s\n' "$last" | grep -qvE '^[A-Za-z][A-Za-z0-9-]*: '; then echo >> "$T/msg"; fi
		echo "(cherry picked from commit $c)" >> "$T/msg"
		new=$(GIT_AUTHOR_NAME=$(G log -1 --format=%an "$c") GIT_AUTHOR_EMAIL=$(G log -1 --format=%ae "$c") \
			GIT_AUTHOR_DATE=$(G log -1 --format=%aI "$c") G commit-tree "$tree" -p "$tip" -F "$T/msg") || { echo "commit-tree failed $c"; exit 2; }
		tip=$new; printf '%s\tPICKED\t%s\t-\n' "$c" "$new" >> "$LOG"
	else
		d=$(printf '%s\n%s' "$out" "$(cat "$T/err")" | grep -m1 -E 'CONFLICT|fatal|error' | tr '\t' ' ')
		printf '%s\tCONFLICT_ON_TIP\t-\t%s\n' "$c" "${d:-conflict}" >> "$LOG"
	fi
	n=$((n + 1)); [ $((n % 10)) -eq 0 ] && echo "HEARTBEAT pick $n $(date -u +%T)"
done < "$LIST"
G update-ref "$REF" "$tip"; echo "tip $tip picked $(grep -c PICKED "$LOG")"; rm -rf "${T:?}"
