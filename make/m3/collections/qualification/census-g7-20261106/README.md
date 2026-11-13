<!-- SPDX-License-Identifier: Apache-2.0 -->
# Census group G7: DOOP residency reuse tests (L067)

Census leaf L067: crates `m3jdk21-doop-direct-reuse-20261030` and `m3-doop-reuse-20261030` (com.synexia develop
`ca1aafc4d94f`), tests on the existing residency owner `jdk.m3.collections.M3ObjectResidency` only. Owner decisions:
`recipe/transient-facade-20261106/DECISION.tsv` (#2566); main-side residency steps are leaf L066 (#2565).

| row | verdict | evidence |
|---|---|---|
| `M3ObjectResidencyReuseTest` (5 crate postimages) | PORTED (tests, 23 of 44) as `M3ObjectResidencyReuseContractTest` | the publication-budget postimage holds all 44 tests; 44/44 pass on the donor residency postimage; on the owner 23 pass and are kept; the existing `M3ObjectResidencyReuseTest` (7 tests) is untouched |
| 21 dropped tests | HOLD_L066 | they need residency steps the owner lacks: callback `Error` state restoration (decode/delete/encode/factory/read errors), admission claims under re-entry (CREATING claims, in-progress swap exclusion), the donor rebind atom's refusal-without-side-effects and reset-failure semantics, the reentrant budget re-check; 2 of them time out (2000 ms) on the owner in nested victim selection; per-test failures in `OWNER_VERDICTS.tsv` |
| `ReentrantBudgetWitness` | HOLD_L066 | evidence that the owner's `reuseUnborrowedProjection` is weaker than the donor atom on a reentrant budget |
| `M3MemoryPressureSamplingTest`, `M3MemoryPressure` | HOLD_DEFER | synexia-m3-residency closure, DEFER in #2566 |
| `M3NativeLeaseCounter`; `M3CooperativeBorrowRetireTest`, `M3CooperativeRetireTest` | ALREADY (`jdk.m3.jni.M3ScopedLeaseTable`); HOLD_SCOPE | the owner lives in m3/native (#2566), outside this group's scope |
| `M3Jdk21DoopDirectReuseRecipeTest`, harness candidates, `M3DoopReuseRecipeTest` | DECLINED | OpenRewrite recipe tests and evidence; recipe custody stays in com.synexia |

NEC: `M3ObjectResidency.reuseUnborrowedProjection` (owner) vs the donor `rebindUnborrowedProjection`: same parameters,
different guards and failure side effects; the MEMBER row maps only the call sites of the kept tests.

Recipe `relocate.py` (RELOCATE_CENSUS v5: v4 + annotation-aware DROP_METHOD), `RELOCATION_MAP.tsv`, `PROVENANCE.tsv`;
proof `differential.sh` (donor / owner-probe / relocated verdicts), `OWNER_VERDICTS.tsv`, `GATES.tsv`,
`CENSUS_RERUN.tsv`, `RECIPE_MEMORY.tsv`.
