<!-- SPDX-License-Identifier: Apache-2.0 -->
# Recipe transient-facade-20261106: DOOP transient-facade residency into jdk.m3.collections

Leaf `doop-transient-facade-residency` of the M3JDK convergence programme (plan
`C:/m3-superset/conv/plans/native-compiler-openjdk.tsv` rank 6). It is a decide-then-port leaf: the family overlaps
existing M3JDK owners, so every donor class received an owner decision (`DECISION.tsv`) before any byte was relocated.
One production owner per capability.

* Donor: hsoliwal/com.synexia develop `fd9a233680b43197eb8c5745a729c342ca56c4e2` (2026-11-06). Newest blob commits:
  `2ab200c1755` (2026-11-05, store: refuse post-close facade callbacks, relinquish zero-idle roots),
  `413673af79a` (2026-11-05, recycler: refuse the borrower action after a reentrant owner close), `4a2e6452a7a`
  (2026-10-31, merge with atom proofs).
* Host: M3JDK master `72ec7c44da12a67e78b09f1eec14c05ccc21413a` (decisions), rebased onto `feae0743e6c25f826d7f289c0b0acca19ff65e62`
  (gates), module `m3/collections` (package `jdk.m3.collections`).

## Phase 1: owner decisions

| Donor class | LOC | Verdict | Owner / target |
| --- | ---: | --- | --- |
| M3TransientFacadeStore | 587 | ABSORB | new `jdk.m3.collections.M3TransientFacadeStore` |
| M3FacadePressureController | 45 | ABSORB | package-private helper beside the store; consumes `M3RuntimeHeap` |
| M3ResidentTrimPlanner | 166 | ABSORB | package-private helper beside the store |
| LazyElementFlags (closure) | 48 | ALREADY (at rebase) | `jdk.m3.collections.M3LazyElementFlags` (absorb-primitives wave 8, #2552) |
| M3InternalLeaseRecycler | 191 | ALREADY (at rebase) | `jdk.m3.collections.M3InternalLeaseRecycler` (absorb-primitives waves 7/8, #2546/#2552) |
| ProviderResidencyClock | 150 | DEFER | moves with its consumers (BoundedProviderBacked*Tree), absent from master |
| M3NativeLeaseCounter | 173 | ALREADY | `jdk.m3.jni.M3ScopedLeaseTable` + `M3LeaseScopes` (#2515) |
| M3PagedArray | 656 | DEFER | synexia-m3-residency fold leaf (needs an M3MemoryPressure decision) |
| M3ProxyArena | 973 | DEFER | synexia-m3-residency fold leaf (14-class closure) |

Dependencies resolve to existing owners: the donor `M3RuntimeHeap` is `jdk.m3.collections.M3RuntimeHeap` (javap
-public identical modulo package; the donor copy adds unused package-private segmented-capacity methods).

### Re-decision at rebase

The decisions were taken on master `72ec7c44da1`, where neither `M3InternalLeaseRecycler` nor `M3LazyElementFlags` had an
owner. Before the push, master `feae0743e6c` carried both: the absorb-primitives waves 7/8 (RELOCATE_SYNEXIA_OWNER v3)
relocated the same donor blobs (132707c53896, 59205c314494 at the same develop commit) with bodies identical to this
recipe's output modulo header and Javadoc lineage, plus four of the recycler's six donor tests. One owner per
capability: both rows became ALREADY, their FILE rows became `SKIP ... ALREADY <path>` rows, and the recipe now
relocates only what master lacks: the store, its two package-private helpers, the store/planner tests, the two missing
recycler JUnit endpoints (the close-boundary self test of 413673af79a had no surefire entry point on master) and the
idle differential main. The wave-9 record left `M3ResidentTrimPlanner` to this leaf explicitly.

### Why not ALREADY/EXTEND for the store

`M3ObjectResidency` is the canonical owner of dense, byte-budgeted projections (fixed `int` slots, swap/recreate,
cooperative borrow handles). The store owns a different capability: identity of transient facades over sparse `long`
logical ids with a disposer lifecycle. javap -public of the donor store has 26 members; none has an owner member with
the same contract:

| Donor member | Nearest M3ObjectResidency member | Gap |
| --- | --- | --- |
| `expose(long)` (pin identity) | `use(int, long, f -> f)` with PIN_EXPOSED | int slot + byte weight; no sparse id |
| `withInternal(long, Consumer)` | `use(...)` with STRICT_NO_ESCAPE | no dispose, no idle scrub/rebind lane |
| `peekResident`, `isResident`, `isPinned`, `residencyFlags` | `state(int)` | no value peek, no pin query, enum not flag word |
| `releaseOptionalResidency(int)` (count target, top-k LRU) | `signalMemoryPressure()` | byte budget only |
| `remove(long)` (releases a pinned facade) | `retireUnborrowedProjection(int)` | refuses pinned slots |
| `reusableIdleCount`, `reuseCount`, `releaseReusable` | `reuseUnborrowedProjection` | slot-to-slot transfer, no idle pool |
| `residentCount`, `pinnedCount`, `stats()` | `residentBytes()`, `lastDecision()` | no counts/hit-miss stats |
| `close()` (AutoCloseable, disposes, post-close refusal) | none | no close boundary at all |
| `Factory`, `Disposer`, `Recycler` | `Factory(int)`, `Reinitializer` | no disposer, no scrub |

The donor tests pin post-close refusal and reentrant close from factory/rebind/scrub; an owner without a close
boundary cannot express them through any adapter. EXTEND would merge a second storage model (sorted long ids, count
trim, disposer lifecycle) into `M3ObjectResidency`: two owners in one file. `LazyObjectParent` (m3/lazyobjects) is a
String-keyed disk-spill proof of concept outside the Maven reactor.

### ALREADY: M3NativeLeaseCounter -> M3ScopedLeaseTable

`retain()`/`Lease.close()` -> `acquire(id)`/`release(id)`; `withLease(Runnable)` -> `M3LeaseScopes.withLease(table, id,
body)`; `activeCount()` -> `leaseCount(id)`; `tryRetire()` -> `tryRetire(id)`; `close()` refusing active leases ->
`close()` refusing outstanding leases/pins. The donor M3CooperativeRetireTest single-thread sequence replayed on the
owner (scratch check, 10 checks PASS): idle retire wins, retire refused with two and with one lease, count preserved,
`withLease` balanced, last release admits one retirement, close refused while leased. Recorded deltas (NEC, not
ported): no per-counter cap; a second `tryRetire` throws `retired logical ID` instead of returning false; counts live
in Java lanes (the owner's JNI lane is the scan), not in a JNI-adjusted direct-buffer header.

### DEFER details

* ProviderResidencyClock: its closure `LazyPrimitiveAddressSpace` (blob e71a64732593) does not compile at the donor
  head (`variable last is already defined`, line 273); `PrimitiveKind` needs `DenseHash`; no consumer on master.
* M3PagedArray: depends on `M3MemoryPressure` (heap + JNI cgroup physical-memory sampler, libm3_residency), a second
  pressure owner beside `M3RuntimeHeap` and a third spill owner beside `M3ObjectResidency`/`LazyObjectParent`. The
  lifetime-reuse identity count was reproduced on the donor for the fold leaf: `M3PagedArrayBackingReuseTest resources`
  (16 pages x 256 slots, 4,096 appends, 15 spills) gives distinctBackings=1 at the donor head (cold and second) and 16
  for the preimage (crate `M3PagedArray.patch` reversed): 15 page-container allocations removed.

### Phase 2A not needed

The target module is m3/collections (beside `M3ObjectResidency` and `M3RuntimeHeap`, which the store's package-private
seams need). m3/lazyobjects has no pom.xml and is not in m3/pom.xml; `m3/tooling/jdk-naming/RENAME_MAP.tsv` plans no
rename of `org.m3.runtime.lazy`. Its package naming stays an open item for its own leaf.

## Phase 2B: the port

`relocate.py` v4 (derived from `entryless-lane-kernel-20261106/relocate.py` v3; new: block-comment donor headers)
relocates 14 FILE rows of `RELOCATION_MAP.tsv`: 3 main sources (store, pressure controller, trim planner) and 11 tests
(9 JUnit classes, the top-k self-test main, the idle differential main), package `com.synexia.primitives` ->
`jdk.m3.collections`, one TYPE row (`LazyElementFlags` -> `M3LazyElementFlags`, the owner already on master), no
DROP/REWRITE rows: every donor body line is kept (body_match 14/14). Six ALREADY rows name the master owners and tests.
Lineage: the retained-variants snapshots of master-landing-20261028 (11 store, 5 planner, 2 recycler variants) are
older conflict losers; the main path continues pr14292's lineage to the 2026-11-05 commits (SKIP rows).

`M3TransientFacadeDump` (M3JDK-authored test source) digests 8 seeded scenario families x 100,000 operations:
lifecycle-recreate, lifecycle-retain, recycle, reentrant-close (factory/rebind/scrub/disposer/action close the owner),
pressure (injected NORMAL/WARM/HIGH heap snapshots, AUTO_HEAP_PRESSURE), topk (planner geometry incl. invalid input),
recycler (faults in factory/rebind/reset/action, reentrant close) and growth (fault-injected four-lane backing growth).
Messages of implicit VM exceptions are recorded as `<implicit>` (C2 fast-throw drops them; -Xint keeps them).

## Donor finding preserved by the relocation

A `Disposer` that closes the owner reentrantly during `remove`/`close` makes the store dispose the same facade twice
and then throw `ArrayIndexOutOfBoundsException` (index -1): `close()` iterates slots that the nested close already
removed. 2ab200c1755 guarded factory/rebind/scrub, not the disposer. The relocation keeps donor behaviour
byte-identical (reentrant-close family digest); the fix belongs to a follow-up in the owner (open item).

Closed by one hunk applied to the owner and the donor (M3JDK branch `m3/collections-facade-reentrant-dispose-20261106a`,
com.synexia branch `m3/primitives-facade-reentrant-dispose-20261106`, donor blob `0b707f7754fe`): `removeSlot` detaches the
slot before the Disposer runs and restores it (identity, flags, recency) only when the Disposer throws while the owner
is still open, so the failed-disposer contract is unchanged; `close()` re-reads `size` after every callback and returns
when a nested close finished. `M3FacadeDisposerReentryTest` (6 tests) pins it. Against donor rev `fd9a233680b`,
`relocate.py --check` now reports the store row CHANGED (exactly that hunk) until the recipe is re-pinned to a develop
commit that carries the donor fix; relocating from the fixed donor commit reproduces every target byte except the RELOCATE
anchor lines.

The trim path had the same defect: in `releaseOptionalResidency` a Disposer that closed the owner disposed the
in-flight victim twice and handed `(0, null)` to the Disposer for the remaining victims; a reentrant remove, expose or
lease shifted the planned slots (wrong victim, pinned facade disposed, leased after disposal). Closed by a second
identical hunk (M3JDK branch `m3/collections-facade-release-residency-20261106a`, com.synexia branch
`m3/primitives-facade-release-residency-20261106`): `detachVictims` snapshots the planned victims into the private
`Backing` record and removes them in the single compacting pass before the first callback; on a failed disposal
`returnVictims` restores the failed and the not-yet-offered victims (`restoreSlot`), or disposes a never-offered victim
once when it cannot return (closed owner, id re-admitted). `M3FacadeDisposerReentryTest` gains 5 trim tests;
`M3TransientFacadeDump` gains a ninth family, reentrant-trim (Disposer reentry kinds close, remove, expose, lease, nested
trim and fault over RETAIN_UNEXPOSED owners); the eight earlier family digests are unchanged.

## Files

* `DECISION.tsv` owner decisions; `RELOCATION_MAP.tsv` rows; `PROVENANCE.tsv` per-row donor blob/sha256/URL pins and
  body_match (written by the recipe); `GATES.tsv` gate results; `TEST_BASELINE_DIFF.txt` base/head failing sets;
  `RECIPE_MEMORY.tsv` lessons.
* `relocate.py` (`--check` for the fixed point), `differential.sh`, `verify.sh`.

```
python -I m3/collections/recipe/transient-facade-20261106/relocate.py --donor-git <com.synexia clone> --donor-rev fd9a233680b43197eb8c5745a729c342ca56c4e2 --check
JAVA_HOME=<JDK 21> bash m3/collections/recipe/transient-facade-20261106/verify.sh <com.synexia clone> fd9a233680b43197eb8c5745a729c342ca56c4e2 <scratch>
```
