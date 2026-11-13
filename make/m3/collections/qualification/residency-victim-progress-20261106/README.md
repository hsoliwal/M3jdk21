<!-- SPDX-License-Identifier: Apache-2.0 -->
# Residency victim progress (leaf P1)

Follow-up of census group G7 (#2598): 2 of the 21 HOLD_L066 residency contract tests timed out (2000 ms) in nested
victim selection on the owner `jdk.m3.collections.M3ObjectResidency`. Verdict: **a true livelock in master**, reachable
through the public API alone (`HANG.txt`).

## The bug

`admit()` loops `while (wouldExceed(...)) { victim = leastRecentlyUsed(requested); evict(victim); }`. During
`evict(v)` with SWAP_SECONDARY the victim is SWAPPING while `Codec.encode` / `SpillStore.write` run, and it still holds
its resident value. A nested `use()` from that callback admits again; both selectors accept any slot with a resident
value, so they return `v` again; `evict(v)` returns at its first line (state is not RESIDENT) and nothing changes.
The inner loop spins forever under the arena monitor (jstack: `admit:451 <- ensureResident:404 <- use:168 <-
Codec.encode <- evict:552 <- admit:450 <- use:168`, RUNNABLE, cpu time grows with wall time, every lane constant).
Every other thread that touches the arena then blocks.

## The fix

Recipe `apply.py` (RESIDENCY_DONOR_ATOMS v1, derived from the census-small-batch recipe) ports the crate step
`m3-doop-reentrant-eviction-20261030` of `m3-doop-reuse-20261030` (com.synexia develop `ca1aafc4d94f`, donor blob
`0f7bf56e5aa8`) as one atom, anchored `//FLLDCIM: MRL`:

- both victim selectors (linked LRU and the rollover scan) require RESIDENT, so every selected victim is detached or
  throws: admission ends after at most `slots()` evictions or with `BudgetExceededException`;
- `admit()` claims the requested slot (CREATING, or RESTORING for a SWAPPED slot) before the eviction callbacks run and
  restores the prior state in a `finally`, also on `Error`; publication stays with the caller.

The selector-only change is rejected: it ends the livelock but lets a nested `use()` of the slot being admitted
publish it before the outer admission publishes it again (two creations, 8 bytes leaked in `residentBytes`,
`VARIANTS.tsv`); the crate keeps that form as its own rejected counterexample. M3JDK had received this atom before
(452b77db672) and lost it at the superset landing 7c6ddb6b7aa (retain merge of PR #2157, parked under
`m3/qualification/master-landing-20261028/retained-variants/pr2157/`).

## Tests

No hand-written test: the census G7 recipe (`../census-g7-20261106/relocate.py`, RELOCATE_CENSUS v5) relocates 7 more
of the crate's tests after their DROP_METHOD rows were removed from its `RELOCATION_MAP.tsv` (the 2 timeouts and the 5
admission-claim tests). `M3ObjectResidencyReuseContractTest` 23 -> 30 tests: on master the 7 fail (2 time out), on
this head 30/30 pass. 14 tests stay HOLD_L066 (callback `Error` restoration, rebind refusal/reset semantics, reentrant
rebind budget): `OWNER_VERDICTS.tsv`.

## Proof

`GATES.tsv`. Differentials: the census L066 oracle (byte-identical: it never re-enters from an eviction callback), the
transient-facade dump of #2566 (byte-identical), and `oracle/ResidencyEvictionReentryDifferential.java`, which
re-enters from `Codec.encode` and `SpillStore.write` under a watchdog (`oracle/vpdiff.sh`); every differing line is
classified by `oracle/classify_victim_progress.py` (`VP_CLASSES.tsv`): MASTER_LIVELOCK or CLAIM_REFUSAL, then CASCADE.
`DECISION.tsv`, `RECIPE_MEMORY.tsv`.
