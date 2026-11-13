<!-- SPDX-License-Identifier: Apache-2.0 -->
# Residency fold (synexia-m3-residency into m3/collections)

This leaf takes the residency rows that #2566 (transient facade) deferred, the census2 rows P2-116 and P2-079, and
the pressure-sampling atom that #2610/#2614 left unported. It decides every donor class first (`DECISION.tsv`), then
ports by recipe. One production owner per capability.

* Donor: hsoliwal/com.synexia develop `165ceb5a020291e99ebeb89783b88e8a9686ae5c` (2026-11-06).
* Host: M3JDK master `a3ee7b9f5848a0df33f46fcd9335ebebf1f63b8d` (decisions), gates re-run after the rebase onto
  `e03bb28d54b`; module `m3/collections`.

## Decisions

| Donor | Verdict | Where |
| --- | --- | --- |
| pressure-sampling atom (`m3-doop-pressure-sampling-20261030`) | EXTEND `M3ObjectResidency` (ADOPT) | this PR: recipe RESIDENCY_DONOR_ATOMS v3 leaf PS |
| M3PagedArray, M3MemoryPressure + 4 self tests (P2-116) | ABSORB | the family PR, `../../recipe/residency-paged-array-20261107/` |
| libm3_residency (`m3_residency.c`, probe header, CMake, native test) | DEFER | same family PR |
| M3ResidentPressureCadenceRecipeTest | OUT (com.synexia OpenRewrite SDK test) | - |
| M3VirtualNodeArena, M3AutoPagedLongList, M3LazyHandlePool, M3VirtualObject + tests (P2-079) | DEFER | the MIndex swap closure (`MIndexCollectionPageArchive`, `SwapPressureController`) is not in M3JDK's reactor |
| M3BehaviorOnly | ALREADY | `jdk.m3.residency.M3BehaviourOnly` (m3/ports/compiletime) |
| M3VirtualDescriptorProcessor | OUT (DEFER) | compiler tooling (m3/compiler or m3/ports/compiletime), with the arena |

## Leaf PS: lazy pressure sampling

Without `PRESSURE_AWARE`, `targetBudget` ignores the heap, yet master built a `HeapSnapshot` at every admission,
`signalMemoryPressure()` and both reuse budget checks. The atom moves `HeapSnapshot.current()` inside the flag test.
M3JDK received it once, as `4e229782bf1` (PR #2157). The retain merge `7c6ddb6b7aa` then lost it, together with the
atoms #2610/#2614 restored. `../residency-donor-semantics-20261106/apply.py` v3 adds it as serial leaf PS on the leaf CD
postimage (master's blob). The head hunk equals `4e229782bf1`'s hunk, except one line: the admit call site is now
inside P1's claim `try` block, so it has 4 more spaces of indentation.

* `M3ObjectResidencyPressureSamplingTest` counts thread-allocated bytes.
  * Fixed budget: `signalMemoryPressure()`, an admission net of its `Decision` and a budget-refused reuse must each
    stay below one byte per call over 20,000 calls.
  * With `PRESSURE_AWARE`, the boundaries must still read the live heap: a `Long.MAX_VALUE` reserve refuses the
    admission, and reserve 0 keeps the maximum.
  * On master classes the test fails 3 of 4: 48 bytes per call, one `HeapSnapshot` per boundary.
* `oracle/psdiff.sh` runs `PressureSamplingDifferential`, a seeded behaviour trace over 7 flag/reserve kinds with
  re-entering callbacks and reuse. It also runs `PressureSamplingAllocationProbe` under `-Xint` and classifies every
  differing figure.

Gate results: `GATES.tsv`. JIT-mode allocation figures depend on C2 scalar replacement and are evidence only (open item
in `GATES.tsv`).
