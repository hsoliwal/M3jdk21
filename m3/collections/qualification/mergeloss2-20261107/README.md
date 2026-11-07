<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Merge-loss audit phase 2, m3/collections restores (2026-11-07)

Two leaves of the phase-2 merge-loss audit (`CLEAN_0E.tsv`, lane com-synexia-0e) whose lost lines are still absent on
master `baafdafdc817` and have positive evidence. Every other m3/collections leaf of the lane (28, 30, 32, 33, 39, 44,
47, 49, 50, 54) needs no edit; their decisions are in the record-only PR.

| leaf | owner | origin | dropped by | decision |
|---|---|---|---|---|
| 45 | `M3ObjectResidency` | `4e229782bf1` doop: avoid unused pressure snapshots in fixed-budget residency | `7c6ddb6b7aae` Superset: retain PR #2157 | RESTORE |
| 59 | `M3SingleValueMapView` (spring) | `554b4d715e0` restore published M3MultiValueMap constructor binary descriptor | `fc917d80ea3c` fold #2390 through API_FOLD v1 | RESTORE |

- **45**: master samples `HeapSnapshot.current()` at all four budget sites even for a fixed budget. The origin receipt
  `m3/qualification/m3-doop-pressure-sampling-20261030.json` records `snapshot_call_sites_before_after [4, 1]` and
  48 -> 0 bytes per fixed-budget `signalMemoryPressure()` under `-Xint`. `targetBudget()` now reads the snapshot only
  under `PRESSURE_AWARE`. The 3-way merge has one conflict, because census L066 and victim progress P1 restructured
  `admit` on master. It is resolved by keeping ours and applying only the origin's substitution.
- **59**: `m3/collections/qualification/spring-scalar-constructor-abi/README.md` records that the published
  `(M3MultiValueMap)V` constructor must stay, because precompiled clients throw `NoSuchMethodError` without it. Its
  verified preimage `ab4ffb724568` is the current master blob. The 3-way merge is clean.

## Files

| file | purpose |
|---|---|
| `apply.py` | recipe MERGELOSS2_RESTORE v1: pinned preimage blobs, refuses drift, idempotent, `--check`, `--leaf` |
| `EDITS.tsv` | per file: before/after sha256, leaf, origin commit, dropping merge, 3-way record, rule |
| `DECISION.tsv` | the two leaves' decisions with evidence |
| `GATES.tsv` | executed gates with numbers |
| `ResidencyPressureSamplingProbe.java` | leaf 45: thread-allocated bytes per `signalMemoryPressure()` |
| `SpringCtorAbiProbe.java` | leaf 59: a client linked to the published constructor descriptor |

```text
python -I m3/collections/qualification/mergeloss2-20261107/apply.py [--check] [--leaf 45|59]
```
