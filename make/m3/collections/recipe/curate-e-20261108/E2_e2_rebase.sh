#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
# e2_rebase.sh <tag>: CURATE_E pass E2 (public jdk.m3.collections types and tests drop the M3 prefix) on the current master.
#   1. bcw -> origin/master; refuses unless E1 (#2826, head 0a4bd1dea3ee) is an ancestor of master;
#   2. RENAME_MAP_E2 regenerated from the tree (M3-prefixed core types/tests; java.* names and their tests keep M3);
#   3. curate_rename_e.py in place (CURATE_CORE=jdk.m3.collections, TESTS_AS_OWNERS=1, records skipped);
#   4. pom_dependents.py --require-checkout guard (#2830) when present; 5. e2_verify.sh <tag>.
set -uo pipefail
H=$(cd "$(dirname "$0")" && pwd); cd "$H"; T="$1"
git -C bcw fetch -q origin master && git -C bcw checkout -q --detach origin/master && git -C bcw reset -q --hard origin/master && git -C bcw clean -fdq -- m3
git -C bcw merge-base --is-ancestor 0a4bd1dea3ee HEAD || { echo "E1 (#2826) is not on master yet: merge it first"; exit 2; }
S=bcw/m3/collections/src
python -I gen_map_e2.py "$S" curation/java_names.txt "RENAME_MAP_E2_$T.tsv" || exit 3
echo "E2 map: $(awk -F'\t' 'NR>1{print $3}' "RENAME_MAP_E2_$T.tsv" | sort | uniq -c | tr '\n' ' ')"
cp "RENAME_MAP_E2_$T.tsv" RENAME_MAP_E2.tsv
CURATE_CORE=jdk.m3.collections TESTS_AS_OWNERS=1 EXTRA_SKIP_DIRS=recipes,recipe,verification python -I curate_rename_e.py bcw RENAME_MAP_E2.tsv | tail -1
G=bcw/m3/tooling/jdk-naming/pom_dependents.py
[ -f "$G" ] && (cd bcw && python -I make/m3/tooling/jdk-naming/pom_dependents.py --help 2>&1 | head -3)
bash e2_verify.sh "$T"
