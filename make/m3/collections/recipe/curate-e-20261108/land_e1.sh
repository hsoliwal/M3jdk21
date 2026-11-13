#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
# land_e1.sh <tag> <message-file>: land CURATE_E pass E1 from e1_rebase.sh <tag> outputs.
# The curated tree mvn_<tag>/tree is a clone of base_<tag> with the recipe applied: its git status (minus build output)
# is the change set. The recipe crate goes to make/m3/collections/recipe/curate-e-20261108/. One API commit on the base master,
# PR, merge with the head pinned. Refuses unless E1_<tag>.txt shows CLEAN maven, a bytecode verdict and equal test lines.
set -euo pipefail
H=$(cd "$(dirname "$0")" && pwd); cd "$H"; H=$(cygpath -m "$H"); T="$1"; MSG="$2"; R="E1_$T.txt"
grep -q "maven -Werror test-compile CLEAN" "$R" || { echo "maven not clean" >&2; exit 2; }
b=$(grep '^tests base:' "$R" | sed 's/^tests base: //; s/ BUILD.*//'); c=$(grep '^tests cur:' "$R" | sed 's/^tests cur: //; s/ BUILD.*//')
[ -n "$b" ] && [ "$b" = "$c" ] || { echo "test lines differ: base [$b] cur [$c]" >&2; exit 2; }
M=$(sed -n 's/^master //p' "$R"); MF=$(git -C bcw rev-parse "$M")
TR="mvn_$T/tree"; SP="land_e1_$T.specs"; : > "$SP"
git -C "$TR" status --porcelain -uall -- m3 | grep -v '/target/' | while IFS= read -r l; do
    st=${l:0:2}; p=${l:3}
    case "$st" in
        " D"|"D ") echo "$p=-" >> "$SP";;
        *) echo "$p=$H/$TR/$p" >> "$SP";;
    esac
done
C=make/m3/collections/recipe/curate-e-20261108
for f in curate-e/README.md curate_rename_e.py make_curate_e.py curate_e_classify.py gen_map_e1.py carry_map_e1.py \
         curate_e_place.py curate_e_fastloop.sh curate_e_loop.sh curate_e_unmove.py curate_e_bytecode.py \
         make_curate_e_bytecode.py sprawl_census.py reach_census.py e1_rebase.sh land_e1.sh DEQUALIFY.tsv curate-e/E1_TEST_EDITS.json; do
    echo "$C/$(basename "$f")=$H/$f" >> "$SP"; done
echo "$C/RENAME_MAP_E1.tsv=$H/RENAME_MAP_E1_$T.tsv" >> "$SP"
echo "$C/BYTECODE.tsv=$H/proof_$T/BYTECODE.tsv" >> "$SP"
echo "$C/E1_REPORT.txt=$H/$R" >> "$SP"
echo "$C/EDGES.tsv=$H/fast_$T/edges.tsv" >> "$SP"
echo "specs: $(wc -l < "$SP") ($(grep -c '=-$' "$SP") deletes)"
hm=$(python -I api_commit_big.py hsoliwal/M3jdk21 "m3/curate-e1-$T" "$MF" "$MSG" "$SP" | tail -1)
echo "commit $hm"
url=$(gh pr create -R hsoliwal/M3jdk21 --base master --head "m3/curate-e1-$T" --title "$(head -1 "$MSG")" --body-file "$MSG" | tail -1); n=${url##*/}
if [ -n "${NO_MERGE:-}" ]; then echo "PR $url opened, head $hm; merge left to the user"; exit 0; fi
gh pr merge "$n" -R hsoliwal/M3jdk21 --merge --match-head-commit "$hm" > /dev/null 2>&1 || gh pr merge "$n" -R hsoliwal/M3jdk21 --merge --admin --match-head-commit "$hm" > /dev/null
gh pr view "$n" -R hsoliwal/M3jdk21 --json number,state,mergeCommit --jq '"M3jdk21 #\(.number) \(.state) \(.mergeCommit.oid[0:12] // "-")"'
