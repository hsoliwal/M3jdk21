#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0
# Atom (pattern MERGE_TREE_PLUS_RESOLUTIONS): build a real two-parent merge commit without a worktree. Start from the
# tree `git merge-tree --write-tree <ours> <theirs>` wrote (conflicted paths carry markers there) and replace every
# path of <resolutions.tsv> (path TAB source; source = ours | theirs | file:<resolved file>) in a private index; the
# path keeps its mode from the ours tree (else theirs). Refuses when a conflicted path of merge-tree has no row, or a
# resolved file still has conflict markers. Prints the merge commit id; writes no ref (push it yourself).
# Usage: merge_from_resolutions.sh <clone> <ours> <theirs> <resolutions.tsv> <message file>
set -uo pipefail
C="$1"; OURS="$2"; THEIRS="$3"; RES="$4"; MSG="$5"
T=$(cygpath -m "$(mktemp -d)"); trap 'rm -rf "$T"' EXIT; export GIT_INDEX_FILE="$T/index"
git -C "$C" -c merge.renames=false merge-tree --write-tree --name-only --no-messages "$OURS" "$THEIRS" > "$T/mt.out"
tree=$(head -1 "$T/mt.out"); tail -n +2 "$T/mt.out" | sort > "$T/conflicts"
cut -f1 "$RES" | sort > "$T/rows"; missing=$(comm -23 "$T/conflicts" "$T/rows")
[ -z "$missing" ] || { printf 'REFUSED: conflicted paths without a resolution row:\n%s\n' "$missing" >&2; exit 3; }
git -C "$C" read-tree "$tree" || exit 2
while IFS=$'\t' read -r path src; do
	[ -n "$path" ] || continue
	mode=$(git -C "$C" ls-tree "$OURS" -- "$path" | cut -d' ' -f1); [ -n "$mode" ] || mode=$(git -C "$C" ls-tree "$THEIRS" -- "$path" | cut -d' ' -f1)
	case "$src" in
		ours) blob=$(git -C "$C" rev-parse "$OURS:$path") ;;
		theirs) blob=$(git -C "$C" rev-parse "$THEIRS:$path") ;;
		file:*) f="${src#file:}"; grep -q -E '^(<<<<<<< |>>>>>>> )' "$f" && { echo "REFUSED: markers left in $f" >&2; exit 3; }
			blob=$(git -C "$C" hash-object -w --no-filters "$f") ;;
		*) echo "REFUSED: bad source $src for $path" >&2; exit 3 ;;
	esac
	git -C "$C" update-index --add --cacheinfo "${mode:-100644},$blob,$path" || exit 2
done < "$RES"
nt=$(git -C "$C" write-tree) || exit 2
git -C "$C" -c user.name="Hitesh Soliwal" -c user.email="hsoliwal@gmail.com" commit-tree "$nt" -p "$OURS" -p "$THEIRS" -F "$MSG"
