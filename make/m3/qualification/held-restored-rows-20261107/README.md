<!-- SPDX-License-Identifier: Apache-2.0 -->
# held-restored-rows-20261107

#2649 (merge `664dd18c7eef`, merge-loss case 05, `mergeloss-s1-20261106` leaf L2) moved the two held tests
`M3IndexPathMasksLongSelfTest.java` and `M3IndexPathMasksLongTest.java` out of
`make/m3/qualification/loss-guard-restore-20261106/held/` back to their paths, byte for byte, after its L1 restored
`M3IndexPathMasks.selectLong`. This change records that in the held set's own ledger: one RESTORED status row per test is
appended to `make/m3/qualification/loss-guard-restore-20261106/HELD.tsv`; the original HELD rows are kept unchanged.

The rows follow that file's own five columns (`path, source_blob, reason, evidence, owner_action`, the schema #2517's
writer used; hold.py's `path, labels, gate, held_sha256, reason` belongs to #2547's ledger): status `RESTORED` leads
`reason`, `evidence` carries the held sha256 and #2649's gate figures, `source_blob` is the held (= live) blob.

| file | content |
|---|---|
| `apply.py` | recipe HELD_RESTORED_ROWS v1: hash-pinned append (anchor = the last original row), `--check`, refuses drift |
| `EDITS.tsv` | file, before/after sha256, decision, citation (written by apply.py) |
| `check_rows.py` | reader checks: field count, original rows kept as a byte prefix, path-keyed and path-set readings, live blob/sha256 vs the row, held copy absent, no other reader |
| `GATES.tsv` | gates |

Replay: `python -I apply.py --check` (exit 0, changed=0); `python -I check_rows.py`.
