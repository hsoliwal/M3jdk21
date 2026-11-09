#!/bin/bash
# harvest_group.sh <idx> <group>: both deletion harvests for one pathspec (multi-pathspec runs drop merges)
cd /c/m3-superset/conv/m3jdk21 || exit 1
R=/s/m3-scratch/eb-conv/restore
git log --full-history -m --no-renames --diff-filter=D --name-only --since=2026-10-01 --format='%x01%H %cs %P|%s' origin/master -- "$2" > $R/DROP_M/$1.txt 2>/dev/null
git log --full-history --diff-merges=first-parent --no-renames --diff-filter=D --name-only --since=2026-10-01 --format='%x01%H %cs %P|%s' origin/master -- "$2" > $R/DROP_FP/$1.txt 2>/dev/null
echo "done $1 $2 m=$(wc -l < $R/DROP_M/$1.txt) fp=$(wc -l < $R/DROP_FP/$1.txt) $(date +%H:%M:%S)" >> $R/logs/harvest_groups.log
