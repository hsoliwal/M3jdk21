<!-- SPDX-License-Identifier: Apache-2.0 -->
# Merge-loss phase 2: the HANDOFF lines of the filtered-views family

#2695 (`../mergeloss2-filtered-views-20261107`) decided the 22 filtered-views rows and handed off 82 exact lost lines
whose owners other agents then held. Those owners are free now (#2649, #2680 landed). `DECISION.tsv` decides every
line; `LOST_LINES.tsv` re-tests them on the base (master + #2695) and on the head.

| rows | file | lines | decision | citation |
|---|---|---|---|---|
| 35 | m3_index_mask_compactor.c | 17 | RESTORE | origin harness m3_sparse_mask_reads.c fails on master, passes here |
| 27 | m3_index_mask_compactor.c | 25 | RESTORE (MAPPED) | the same enclosing-span transfer through row 35's helper; origin harness m3_mask_compactor_spans.c fails on master, passes here; m3_admit_chunk bytes NEC |
| 17 | m3_index_mask_compactor.c | 9 | PRESENT_EVOLVED | 05a37a87a10: rejected pages transfer no rows, drift keeps the CME; the m3_verify_empty shortcut NEC |
| 19 | M3IndexPathMasks | 3 / 10 / 11 | RESTORED_SINCE / PRESENT_EVOLVED / NEC | #2649 664dd18c7ee; c792eff1c21; fast paths only (origin tests pass on master) |
| 19 | M3LongIndexPathMasks | 2 | RESTORED_SINCE | #2680 a9c8740a5d8 |
| 17 | M3IndexedListSnapshot | 5 | SUPERSEDED | 336b0f44141 + M3IndexedSnapshotDirectoryOwnershipSelfTest:184 (phase-1 leaf 21, `../mergeloss-21-20261107`) |

The restore (`apply.py`, MERGELOSS2_FV_HANDOFF v1, `EDITS.tsv`): the single-mask compactor exports
(`m3_select_range` behind selectNative, selectRangeNative and selectWindowNative, and selectLongWindowNative) fetch only
the enclosing admitted span `[first, last]` of each 256-state page. Master already skipped fully rejected pages
(05a37a87a10) and trims both ends in the two-mask exports (adab56cdc85, "only admitted endpoint spans cross JNI");
row 35's `m3_selected_span` replaces `m3_first_selected`, whose front scan it contains. Exports, validation, the second
state scan and the drift exception are unchanged; results are byte-identical to the Java backend.

Tests relocated verbatim from the retained-variant blobs: `src/test/native/m3_sparse_mask_reads.c` (row 35) and
`src/test/native/m3_mask_compactor_spans.c` (row 27), JNI function-table harnesses that fail on master;
`M3MaskCompactorSpanContract` + `M3MaskCompactorSpanTest` (scalar oracle, Java backend in the module suite, native under
`-Xcheck:jni` in the native gate) and `M3SparseMaskReadContract` (real-JNI parity).

Files: `DECISION.tsv`, `LOST_LINES.tsv`, `apply.py` + `EDITS.tsv`, `native-gate.sh` (from `../mergeloss2-50-20261107`),
`mutants.sh`, `GATES.tsv`.

Re-run: `python -I apply.py --check`; in WSL `native-gate.sh <base root> <cand root> <out>
<classes:test-classes:m3-arrays.jar> <javac -h dir>` (the row-17 origin harness via `ORIGIN=<dir>`) and
`mutants.sh <cand root> <out> <classes:test-classes:m3-arrays.jar>`.
