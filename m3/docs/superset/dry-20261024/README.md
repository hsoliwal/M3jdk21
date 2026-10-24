# Dry verdicts of the v2 consolidation tools (2026-10-24)

Nothing landed: the auto-mode classifier refuses the landing loops. The owner lands these by running, in S:/m3-scratch/consolidate:

    WORKFLOW_POLICY=ours WT_BASE=S:/m3-scratch/worktrees/M3jdk21v8 C_OURS_PRS=S:/m3-scratch/consolidate/C_OURS_PRS.txt ./batch_resolve6.sh K:/m3-work/M3jdk21 hsoliwal/M3jdk21 m3/superset-20261023 CONFLICTS.M3jdk21.tsv resolved.M3jdk21.tsv
    ./repair_theirs2.sh K:/m3-work/M3jdk21 hsoliwal/M3jdk21 m3/superset-20261023 AUDIT_THEIRS.M3jdk21.tsv REPAIRS_20261023.tsv

- `DRY_VERDICTS_20261024_batch6.tsv`: the 9 pending source-conflict PRs under batch_resolve6.sh (baseline-aware gate, v2 tools): 1614, 1766, 1846, 1847 resolve (candidate named in the note); 1406, 1608, 1733, 1330, 1586 have no candidate that compiles over the head (reading pass).
- `DRY_VERDICTS_20261024_repair2.tsv`: the 23 whole-file resolutions under repair_theirs2.sh (hunk-level re-merge, replay head-first, no head line dropped): 11 repairs resolve and restore 648 superset lines in total (1441: 269, 1631: 139, 1530: 108, 1482: 37, 1849: 31, 1784: 17, 1764: 14, 1776: 12, 1770: 10, 1855: 7, 1788: 4); 12 refuse (every candidate either fails the compile or would drop head lines: 1367, 1397, 1777, 1581, 1590 (restores nothing), 1602, 1611, 1559, 1573, 1637, 1332, 1597), m3/file and the parser pack among them: reading pass.

Tools: synexia-openrewrite-recipes/recipe-crates/m3-superset-merge-20261023 (batch_resolve6.sh, gatelib2.sh, repair_theirs2.sh, start/finish_pr_merge2.sh, resolve_hunks2.py; RECIPE_MEMORY.tsv rows 41-50).
