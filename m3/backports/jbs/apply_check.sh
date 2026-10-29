#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0
# Atom APPLY_CHECK: standalone dry run of each upstream commit against a target tree, no worktree: the commit's own
# patch (diff-tree c^ c, binary, no rename detection) through `git apply --cached --check` on a scratch index of the
# target. CLEAN | CONFLICT | MISSING_BLOB, with the first apply error. Lazy fetch is off, so nothing is downloaded.
# Usage: apply_check.sh <repo> <target rev> <commit list> <out.tsv>
set -uo pipefail
R="$1"; TGT="$2"; LIST="$3"; OUT="$4"; T=$(mktemp -d); export GIT_NO_LAZY_FETCH=1
GIT_INDEX_FILE="$T/idx" git -C "$R" read-tree "$TGT" || exit 2
n=0; : > "$OUT"
while read -r c; do
	p=$(git -C "$R" -c diff.renames=false diff-tree -p --binary --no-commit-id "$c^" "$c" 2> "$T/e1")
	if [ -s "$T/e1" ]; then r=MISSING_BLOB; d=$(head -1 "$T/e1")
	elif printf '%s\n' "$p" | GIT_INDEX_FILE="$T/idx" git -C "$R" apply --cached --check - 2> "$T/e2"; then r=CLEAN; d=-
	else r=CONFLICT; d=$(grep -m1 'error:' "$T/e2" || head -1 "$T/e2"); fi
	printf '%s\t%s\t%s\n' "$c" "$r" "${d//$'\t'/ }" >> "$OUT"
	n=$((n + 1)); [ $((n % 250)) -eq 0 ] && echo "HEARTBEAT $(basename "$OUT") $n $(date -u +%T)"
done < "$LIST"
echo "done $(basename "$OUT") $n $(date -u +%T)"; rm -rf "${T:?}"
