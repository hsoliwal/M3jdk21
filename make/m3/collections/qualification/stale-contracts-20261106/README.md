<!-- SPDX-License-Identifier: Apache-2.0 -->
# stale-contracts-20261106

Request from the lane owner (com-synexia-45): two test contracts of `m3/collections` on master were stale or unrun.
`M3LinkedClearContract` had no JUnit adapter and stopped at its bound-spliterator check; `M3WrapperConvergenceContract`
failed in three modes that its JUnit (`readonly-policy`, 25 checks) does not run. Base: master `2d8273c6ad55`.

| file | content |
|---|---|
| `apply.py` + `EDITS.tsv` | recipe STALE_CONTRACTS v1 (from `../census2-c2-20261106/apply.py`): 3 anchored EDIT rules with hash-pinned preimage blobs and 1 NEW rule, `--check` fixed point |
| `DECISION.tsv` | decision per check with the cited commits and the probe evidence |
| `tools/probe_owner.sh` | runs a checked-out contract against the owner sources of any commit (`-sourcepath` closure); the per-commit evidence of `DECISION.tsv` and gates 8-10 |
| `GATES.tsv` | gates and receipt figures |

## Decisions

| leaf | check | decision | cited |
|---|---|---|---|
| L1 | `M3LinkedClearContract` bound spliterator after a nonempty clear | STALE_CHECK | `4650910eff1` |
| W1 | `M3WrapperConvergenceContract` `comparator-failure` | STALE_CHECK | `79a8df95080`, `ef0beec8bfb` |
| W2 | `M3WrapperConvergenceContract` `readonly-linear` | PENDING_WITNESS | `5fde7416ab1` |
| W3 | `M3WrapperConvergenceContract` `readonly-failfast` | PENDING_WITNESS | `5fde7416ab1` |

**L1.** `4650910eff1` made `M3LinkedList.spliterator()` the fail-fast `LinkedIndexSplit` on purpose: its own contract
`M3LinkedIndexSplitContract.mutation` asserts the `ConcurrentModificationException`, and its RECEIVING.md keeps
`modCount` owner-authoritative. The clear contract (`4f33ce9281c`) was written on a parallel line against the older
iterator spliterator. `2aeaffe6424` and `3538a25a194` already moved this one check to the fail-fast form, but the
retain merges `11848337f18` (#1479) and `6d0e9f5461e` (#1522) kept the first-parent copy, so master never carried it.
Rule L1 restores that form byte for byte (only the licence header and the `//FLLDCIM: MRL` anchor differ from
`3538a25a194`). `M3LinkedClearTest` runs it in surefire: with `m3.arrays.native.path` (profile
`m3-collections-native`) the whole contract; without it the `M3LinkedList` half runs and must pass, the map/set half
must refuse in `mapAndSet` with the JNI `UnsatisfiedLinkError` (master's `M3LinkedHashMap` allocates its tables
through `M3ArrayNative`), and the test aborts (skipped) with that reason.

**W1.** The check threw only on `compare(5, 2)`, the first call of the old `headMap(5, true)` range construction.
`79a8df95080` (`M3FilteredViews`, recipe com.synexia #13198, `../filtered-nav-comparator-20261023`) replaced that
range plus blanket `IllegalArgumentException` catch with the owner's near-entry primitive on purpose, because the
catch swallowed comparator exceptions; `ef0beec8bfb` restored the same result for `M3MutableFilteredViews`. JDK
`subMap.floorEntry(5)` calls `5:8, 5:4, 5:6, 4:2`, never `5:2`, so the frozen trigger never fired. Rule W1 throws on the
first armed comparator call and pins a one-call tape; the same marker must propagate from both views. The edited
check still fails on the blanket-catch owners (`098c4de24fd`, `0985e1ceb8f`) and on each owner just before its fix.

**W2, W3.** Not STALE_CHECK and not OWNER_REGRESSION: no owner commit changed these behaviours. Both modes were written
as pending counterexamples (catalogue `WRAP-005`/`WRAP-006`, `PENDING_REPRODUCED;DIRECT_CURSOR_REFUSED`) and fail the
same way on all 37 executed owner trees from `5fde7416ab1` to master. No edit; they stay out of JUnit.

No OWNER_REGRESSION was found. Replay: `python -I apply.py --check` (exit 0, changed=0).
