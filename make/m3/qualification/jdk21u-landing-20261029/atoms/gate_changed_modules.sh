#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0
# Driver of jdk_module_gate.sh (pattern JDK_MODULE_PATCH_GATE) over every JDK module a commit range changes: for each
# module with changed src/<module>/share/classes/**/*.java files, gate those files at <candidate> against <baseline>,
# then remove the two worktrees (disk-full hosts). Platform directories (unix/windows/macosx classes) and native code
# are listed as NOT_GATED. One TSV row per module: module, files, verdict line.
# Usage: gate_changed_modules.sh <clone> <candidate> <baseline> <out dir>
set -uo pipefail
C="$1"; CAND="$2"; BASE="$3"; OUT="$4"; H=$(cd "$(dirname "$0")" && pwd); export MSYS_NO_PATHCONV=1; mkdir -p "$OUT"
L="$OUT/GATE.tsv"; printf 'module\tfiles\tverdict\n' > "$L"
git -C "$C" diff --name-only --diff-filter=AM "$BASE" "$CAND" -- 'src/*' > "$OUT/changed.txt"
grep -v -E '^src/[^/]+/share/classes/.*\.java$' "$OUT/changed.txt" | awk '{ print "NOT_GATED\t" $0 }' > "$OUT/not_gated.tsv"
for mod in $(grep -E '^src/[^/]+/share/classes/.*\.java$' "$OUT/changed.txt" | cut -d/ -f2 | sort -u); do
	files=$(grep -E "^src/$mod/share/classes/.*\.java$" "$OUT/changed.txt" | grep -v '/module-info\.java$' | tr '\n' ' ')
	[ -n "$files" ] || continue
	echo "gate $mod ($(echo $files | wc -w) files)"
	v=$(bash "$H/jdk_module_gate.sh" "$C" "$CAND" "$BASE" "$mod" "$OUT/$mod" $files 2>&1 | grep -v '^waiting' | tail -1)
	printf '%s\t%s\t%s\n' "$mod" "$(echo $files | wc -w)" "$v" | tee -a "$L"
	for s in cand base; do git -C "$C" worktree remove --force "$OUT/$mod/wt-$s" > /dev/null 2>&1; done
done
git -C "$C" worktree prune
awk -F'\t' 'NR > 1 { split($3, a, " "); c[a[1]]++ } END { for (k in c) printf "%s %d  ", k, c[k]; print "" }' "$L"
