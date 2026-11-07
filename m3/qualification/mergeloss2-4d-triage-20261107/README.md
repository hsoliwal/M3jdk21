<!-- SPDX-License-Identifier: Apache-2.0 -->
# Merge-loss phase 2, lane 4d: triage of the 42 clean leaves (CLEAN_4D)

The input is 0e's `mergeloss2/CLEAN_4D.tsv`, the leaves in the java.base and tooling lanes that carry no
ancestry risk. The tools below were run against master 33541216b2cd.

| File | What |
| --- | --- |
| `BATCH_3WAY.txt` | `mergeloss_batch.py` (#2626): 3-way of each origin commit onto master. 10 CLEAN, 30 CONFLICT, 2 NO_ORIGIN. |
| `refcount.py` / `REFCOUNT_TRIAGE.tsv` | MERGE_LOSS_REFCOUNT v1. Each member missing on master is counted as a token in master's java.base and parser trees. Classes: BODY_ONLY (loss inside existing members; gate by differential), REFERENCED (name occurs elsewhere; check for a homonym), UNREFERENCED (capability or dead code). |
| `hunk_kinds.py` | MERGE_HUNK_KINDS v1: classifies diff3 conflict hunks as ADD_ADD, THEIRS_DUP, OURS_DUP or MODIFY. |
| `closure.py` | INDEXSTRING_CLOSURE v1: same-package type closure, used to size each port leaf before it starts. |
| `DECISION.tsv` | State per leaf: RESTORED (70 #2683, 78 #2678, 79 #2677, 88 #2684), HELD_USER (76: public ScheduledExecutorService API), NO_ORIGIN, OPEN. |
