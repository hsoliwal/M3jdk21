#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0
# Atom APPLY_CHECK_HEADER_RELAXED: re-check CONFLICT commits with copyright-year hunks dropped (drop_copyright_hunks.py).
# CLEAN_EXCEPT_COPYRIGHT when the rest applies. Usage: recheck_copyright.sh <repo> <target rev> <apply tsv...> > out.tsv
set -uo pipefail
R="$1"; TGT="$2"; shift 2; T=$(mktemp -d); export GIT_NO_LAZY_FETCH=1; PY=$(cygpath -m "$(dirname "$0")/drop_copyright_hunks.py")
GIT_INDEX_FILE="$T/idx" git -C "$R" read-tree "$TGT" || exit 2
awk -F'\t' '$2=="CONFLICT" && $3 !~ /does not exist/ {print $1}' "$@" | while read -r c; do
	git -C "$R" -c diff.renames=false diff-tree -p --binary --no-commit-id "$c^" "$c" | python -I "$PY" > "$T/p"; d=$?
	if [ $d -eq 3 ]; then r=CONFLICT
	elif GIT_INDEX_FILE="$T/idx" git -C "$R" apply --cached --check "$T/p" 2>/dev/null; then r=CLEAN_EXCEPT_COPYRIGHT
	else r=CONFLICT; fi
	printf '%s\t%s\n' "$c" "$r"
done; rm -rf "${T:?}"
