#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0
# Atom PLUMBING_ADD_COMMIT: commit a directory of files under a path prefix on top of a parent commit, without a
# worktree (scratch index: read-tree parent, hash-object -w, update-index --cacheinfo, write-tree, commit-tree).
# Author and committer Hitesh Soliwal. Usage: commit_files.sh <repo> <parent rev> <ref> <msg file> <src dir> <prefix>
set -euo pipefail
R="$1"; P=$(git -C "$1" rev-parse "$2^{commit}"); REF="$3"; MSG="$4"; SRC=$(cd "$5" && pwd); PFX="${6%/}"; T=$(mktemp -d)
export GIT_INDEX_FILE="$T/idx" GIT_NO_LAZY_FETCH=1 GIT_AUTHOR_NAME="Hitesh Soliwal" GIT_AUTHOR_EMAIL="hsoliwal@gmail.com" \
	GIT_COMMITTER_NAME="Hitesh Soliwal" GIT_COMMITTER_EMAIL="hsoliwal@gmail.com"
git -C "$R" read-tree "$P"
(cd "$SRC" && find . -type f | sed 's#^\./##' | sort) | while read -r f; do
	mode=100644; [ -x "$SRC/$f" ] && case "$f" in *.sh|*.py) mode=100755;; esac
	oid=$(git -C "$R" hash-object -w --no-filters "$SRC/$f"); git -C "$R" update-index --add --cacheinfo "$mode,$oid,$PFX/$f"
done
tree=$(git -C "$R" write-tree); c=$(git -C "$R" commit-tree "$tree" -p "$P" -F "$MSG"); git -C "$R" update-ref "$REF" "$c"
echo "$c"; git -C "$R" diff --stat "$P" "$c" | tail -1; rm -rf "${T:?}"
