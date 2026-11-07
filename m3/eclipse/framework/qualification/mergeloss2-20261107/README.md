<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Merge-loss audit phase 2, leaf 97: Eclipse resource-delta work-state nominations (2026-11-07)

Leaf 97 of the phase-2 merge-loss audit (`CLEAN_0E.tsv`, lane com-synexia-0e). Origin `dce32109366` ("Refine M3
arrays, monitored scheduling, and Eclipse resource state") gave `M3EclipseResourceDeltaSnapshot` opt-in build-work
nominations: `captureChanges`, eight work-state bits, `capturedWorkState` and `selectWorkOrdinals` through
`M3IndexMaskCompactor`. The retain merge `b11532eb12e1` (Superset: retain PR #1637) kept master's file and parked the
PR's version as `retained-variants/pr1637`. #2547 left its single conflict open for owner review.

Decision: RESTORE by ADD rows. Master already freezes full-width flags in every `capture()`
(`DELTA_FULL_WIDTH_FLAGS_20261020.md`). That flag lane, `capturedFlags` and `selectByFlags` stay exactly as they are.
The work-state lane is added beside them, and `captureChanges` reads flags from the captured lane instead of calling
`getFlags()` a second time. `hasCapturedChanges()` now reports the work-state lane.

| file | purpose |
|---|---|
| `apply.py` | recipe MERGELOSS2_RESTORE v1: pinned preimage blob, refuses drift, idempotent, `--check` |
| `EDITS.tsv` | before/after sha256, leaf, origin commit, dropping merge, 3-way record, rule |
| `DECISION.tsv` | the decision with evidence |
| `GATES.tsv` | executed gates with numbers, including the twin's own test run against the result |

```text
python -I m3/eclipse/framework/qualification/mergeloss2-20261107/apply.py [--check]
```
