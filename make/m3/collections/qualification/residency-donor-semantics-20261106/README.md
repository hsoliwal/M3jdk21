<!-- SPDX-License-Identifier: Apache-2.0 -->
# Residency donor semantics (Phase 2, leaves A and CD)

Follow-up of census group G7 (#2598) and leaf P1 (#2610, `../residency-victim-progress-20261106/`). After P1, 14 of the
crate's 44 residency contract tests (crate `m3-doop-reuse-20261030`, com.synexia develop `ca1aafc4d94f`) were still
HOLD_L066 on the owner `jdk.m3.collections.M3ObjectResidency`. They group by missing semantic. `DECISION.tsv` records
one decision per group, with evidence:

| group | semantic | tests | decision |
|---|---|---|---|
| a | callback `Error` restoration | 7 | ADOPT, leaf A (crate step `m3-doop-callback-error-20261030`) |
| b | re-entry admission claims | 7 | ADOPT, landed by P1 #2610 |
| c | rebind refusal/reset semantics | 6 | ADOPT, leaf CD (the crate's `rebindUnborrowedProjection` under the owner's name `reuseUnborrowedProjection`) |
| d | reentrant budget re-check of the rebind | 1 | ADOPT, leaf CD (same atom) |

No group is KEEP_OWNER. None of the owner's differences is a deliberate choice:
- The owner's history (`git log --follow`) shows the crate's atoms were received by M3JDK (452b77db672, PR #2157).
- They were then lost when the superset landing kept the conflicting tree: retain merge 7c6ddb6b7aa, "Promotion to live code is a reviewed follow-up". `retained-twins-core-lanes-20261106/AUDIT.tsv` lists the twin as DIVERGENT with no decision.
- The owner's 23 own residency contract tests pass unchanged on this head.
- The owner's `reuseUnborrowedProjection` came from the parallel PR #2156. #2157's 4e229782bf1 calls the crate's ordering "the stronger rebind budget/reset ordering".
- On master the weaker ordering publishes over the budget: the crate's `ReentrantBudgetWitness` ends with residentBytes=20 at budget 16.

**NEC**: the rule replaces the owner body under the owner's API name. It does not declare the two members equivalent. The census G7 MEMBER row (rebind -> reuse) still only maps call sites.

## Recipe

`apply.py` is RESIDENCY_DONOR_ATOMS v2. It is P1's v1 plus serial leaves, `--leaf`, and drift refusal. Its preimage is the P1 postimage, blob `b7a83aa92326`. Each leaf is anchored `//FLLDCIM: MRL`.
- **Leaf A**: `catch (Error)` restores the prior state around the `ensureResident()` callbacks and around `evict()`'s encode/write.
- **Leaf CD**: the crate rebind body, with the owner's strings kept:
  - guards for LAZY_CREATE, OBSERVE_ONLY and a pinned, active or borrowed destination;
  - while the resetter runs, both slots are CREATING and the source stays resident, counted and active-pinned;
  - the budget is checked again after the resetter;
  - runtime exceptions and errors keep their identity, and checked exceptions are wrapped;
  - on failure, `discardFailedRebind` discards the tainted shell and records a decision.

The census G7 recipe relocated the 14 tests (all DROP_METHOD rows removed). `M3ObjectResidencyReuseContractTest` is now the crate class in full: 44 tests. Pass counts: master 23, P1 30, this head 44 (`OWNER_VERDICTS.tsv`).

## Second master livelock

`STUCK_STATE.txt`: on master, an `Error` from `Codec.encode` leaves the victim SWAPPING with its value. The next over-budget admission then spins in `admit()`. No re-entry is needed. The donor-semantics oracle hit this on master: jstack shows `main` RUNNABLE at `admit:451`.
- P1's selector guard ends the spin, but the slot stays SWAPPING for good.
- Leaf A restores it.

## Proof

All gates are in `GATES.tsv`.
- `oracle/ResidencyDonorSemanticsDifferential.java` reaches the adopted paths: Error/runtime/checked faults in every callback, and reuse with re-entering or throwing resetters, over 6 flag sets.
- `oracle/classify_donor_semantics.py` classifies every differing first line against P1 (`DS_CLASSES.tsv`): ERROR_RESTORE, REUSE_GUARD, REUSE_SOURCE_HELD or REUSE_BUDGET_RECHECK. Nothing is UNCLASSIFIED.
- The census L066 oracle, the transient-facade dump and P1's victim-progress oracle are byte-identical between P1 and this head.

`RECIPE_MEMORY.tsv` records the corrections.

## Leaf PS (recipe v3, residency fold 20261107)

`apply.py` v3 adds a third serial leaf on the leaf CD postimage: the crate step `m3-doop-pressure-sampling-20261030`
(M3JDK `4e229782bf1`, PR #2157, lost by the same retain merge `7c6ddb6b7aa`). `targetBudget()` takes
`HeapSnapshot.current()` only with `PRESSURE_AWARE`; admission, `signalMemoryPressure()` and both reuse budget checks
call it. Decision, behavioural test and gates: `../residency-fold-20261107/`.
