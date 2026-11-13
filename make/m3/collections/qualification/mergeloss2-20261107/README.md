<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Merge-loss audit phase 2, m3/collections restore (2026-11-07)

The one m3/collections leaf of the phase-2 merge-loss audit (`CLEAN_0E.tsv`, lane com-synexia-0e) whose lost lines are
still absent and have positive evidence. Leaf 45 (M3ObjectResidency pressure sampling) was first restored here too.
Draft #2653 (`322af721aa7`, residency fold leaf PS) carries the same lines, so leaf 45 is RESTORED_SINCE #2653, and
this recipe does not touch M3ObjectResidency. The other m3/collections leaves of the lane (28, 30, 32, 33, 39, 44, 45,
47, 49, 50, 54) need no edit here; their decisions are in the record-only PR.

| leaf | owner | origin | dropped by | decision |
|---|---|---|---|---|
| 59 | `M3SingleValueMapView` (spring) | `554b4d715e0` restore published M3MultiValueMap constructor binary descriptor | `fc917d80ea3c` fold #2390 through API_FOLD v1 | RESTORE |

- **59**: `m3/collections/qualification/spring-scalar-constructor-abi/README.md` records that the published
  `(M3MultiValueMap)V` constructor must stay, because precompiled clients throw `NoSuchMethodError` without it. Its
  verified preimage `ab4ffb724568` is the current master blob. The 3-way merge is clean.

## Files

| file | purpose |
|---|---|
| `apply.py` | recipe MERGELOSS2_RESTORE v1: pinned preimage blobs, refuses drift, idempotent, `--check`, `--leaf` |
| `EDITS.tsv` | per file: before/after sha256, leaf, origin commit, dropping merge, 3-way record, rule |
| `DECISION.tsv` | the leaf's decision with evidence |
| `GATES.tsv` | executed gates with numbers |
| `SpringCtorAbiProbe.java` | leaf 59: a client linked to the published constructor descriptor |

```text
python -I m3/collections/qualification/mergeloss2-20261107/apply.py [--check] [--leaf 59]
```
