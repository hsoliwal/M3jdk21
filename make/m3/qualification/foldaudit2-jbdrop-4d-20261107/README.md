# Fold-drop triage, lane 4d java.base, batch 1

Input: `S:/m3-scratch/eb-conv/foldaudit2/MERGED_FOLD_DROPS.tsv` (repo-wide scan of master's first-parent merges
since 2026-10-15). It lists every path whose parent-side content never reached the merge tree. Lane 4d holds 158
rows under `src/java.base`, which collapse to 114 unique (path, parent content, base content) cases over 61 paths.

`three_way_rows.py` classifies each row by a 3-way merge onto current master: ours = master file, base =
merge-base file, theirs = parent file. The verdicts are ALREADY, CLEAN, CONFLICT=n, ABSENT_OURS and NO_BASE.
`lost_now.py` measures, for each row, how many parent-side lines master's file still lacks.

| verdict | cases |
|---------|-------|
| ALREADY | 2 rows (dropped as duplicates) |
| CLEAN | 10 |
| ABSENT_OURS | 2 |
| CONFLICT | 54 |
| NO_BASE | 48 |

Batch 1 decides the 10 CLEAN cases (`DECISION.tsv`):

- **RESTORE (in this commit):**
  - `M3ObjectMetrics.c` (#1456): one JVMTI environment per VM instead of one per call.
  - `M3StringAtom.java` (#1619): native allocations freed when construction fails; code-point writes
    bounds-checked.
  - `GathererOp.java` (#1936): a gatherer does not know its output size.
- **NEC:** `Object.java` (#2410).
- **PUBLIC_API_HOLD:** `ScheduledExecutorService` (#1637) and the #1608 progress-stream family. These need a
  user decision.
- **WHOLE_PR:** #1381, #1586 and #1619 lost whole features (vocabulary filter, M3 token plans, identity collection
  views). Each will be restored as one PR, as #1080 was.

The ABSENT_OURS, NO_BASE and CONFLICT cases follow in later batches.

Gates (`GATES.tsv`): patch-module javac of the two Java files against current master java.base; MinGW and WSL
`gcc -Werror` of the C file; `OmProbe` with both native libraries; `GatherSize` with both GathererOp versions.
