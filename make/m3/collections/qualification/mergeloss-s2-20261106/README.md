<!-- SPDX-License-Identifier: Apache-2.0 -->
# mergeloss-s2-20261106

Case 08 of the M3jdk21 merge-loss audit: the ancestry octopus merge `f4fbe3333946` (2026-10-17, "preserve remaining
M3JDK fleet conflict ancestry", parent 3 lost) dropped the bounded int/long window JNI block that origin `aeece7a984e8`
("materialize sealed bounded int and long array JNI windows") appended to
`m3/collections/src/main/native/collections/m3_index_mask_compactor.c` (`m3_window_inputs`, `m3_window_invalid`,
`m3_window_pack`, and the `M3_DEFINE_WINDOW_JNI` instantiations `scanNative` and `scanLongNative` of
`M3IndexMaskWindow`).

Decision: **NEC, no source change** (`DECISION.tsv`). Checked against #2529 (entryless lane kernel appended to the same
file, `838e92129421`) and #2597 (java.base compactor fix, `33ada7681786`): neither uses the lost block.

| file | content |
|---|---|
| `DECISION.tsv` | decision, cited commits, evidence, action |
| `stage.sh` | stages the pinned master/origin blobs (any host with the repository) |
| `proof.sh` | Linux gcc link proof over the staged files |
| `GATES.tsv` | gate results |

Why not restored: libm3_collections_mask is linked from `m3_index_mask_compactor.c` and `m3_index_mask_window.c`
(three workflows); the origin block defines `Java_jdk_m3_collections_M3IndexMaskWindow_scanNative` a second time, so
master plus the block does not link, and neither did the origin's own tree (its parent already had `scanNative` in
`m3_index_mask_window.c`). The block's only new export, `scanLongNative`, has no Java declaration on master: the Java
side (`M3IndexMaskWindow.scanLong`, `scanLongJava`, `scanLongNative`) was dropped by the same merge and nothing on master
(tests, held files, retained twins, crates, receipts, workflows) references it, so there is no positive evidence that
the conflicting head was meant to land. Re-adding it would be new API, not a restore.

Replay: `bash stage.sh <repo> <master> <dir> && TMPDIR=<tmp> JAVA_HOME=<jdk21> bash proof.sh <dir>`.
