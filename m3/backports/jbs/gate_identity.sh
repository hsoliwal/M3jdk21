#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0
# Atom PATCH_IDENTITY_GATE: every picked commit changes exactly the lines its upstream commit changes (sha256 of the
# +/- lines of both diffs, index lines ignored). Usage: gate_identity.sh <repo> <PICKS.tsv>
set -uo pipefail
R="$1"; export GIT_NO_LAZY_FETCH=1; same=0; diff=0
h() { git -C "$R" -c diff.renames=false diff-tree -p --binary --no-commit-id "$1^" "$1" | grep -E '^[-+]' | grep -vE '^(\+\+\+|---) ' | sha256sum | cut -c1-16; }
while IFS=$'\t' read -r u r n d; do
	[ "$r" = PICKED ] || continue
	a=$(h "$u"); b=$(h "$n"); if [ "$a" = "$b" ]; then same=$((same + 1)); else diff=$((diff + 1)); echo "DIFFERENT $u $n"; fi
done < <(tail -n +2 "$2")
echo "identity same=$same different=$diff"; [ "$diff" -eq 0 ]
