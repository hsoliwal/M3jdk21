<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Existing-owner refinement proposals

Status: source-review proposals only. No target patch, interface change, recipe application, compile, test, runtime or benchmark has been performed by this research unit. Exact whole-file observations are in `LOCAL_OWNER_OBSERVATIONS.json`; the parent must recheck current preimages before any change.

## 1. Linear stable conversion between already-known opposite sort orders

Existing owner: `m3/collections/src/main/java/com/m3/collections/M3LongIndexProjection.java`, private `sorted(boolean)`.

The current source already returns a shared immutable view when `order` equals the requested direction. When `order` proves the opposite direction, it still copies the physical-index window and allocates a second `int[size]` scratch array for a full bottom-up stable merge sort. The immutable order metadata is retained by slices and stable filtering.

Candidate leaf: when and only when the recorded order is the opposite sorted direction, copy the index window once; reverse the entire index vector; reverse each maximal equal-value run. Leave the payload array untouched and leave the `ENCOUNTER` path on its existing general sorting algorithm. No public signature or owner needs to be added.

Correctness argument: an already-sorted sequence comprises equal-key encounter-order runs G1, G2, ..., Gk. Whole reversal produces reversed(Gk), ..., reversed(G1). Reversing each equal-key run produces Gk, ..., G1 and preserves the original physical-index encounter order within ties. This applies symmetrically to ascending and descending conversion and to bounded immutable slices.

Expected structural cost from the loop design, not a measurement: O(n) index work and one new result index array, eliminating the separate merge scratch array on this eligible path. Empty/singleton behavior, original physical identities, signed long extrema, duplicate tie order, detached payload ownership and repeated-direction no-copy behavior remain locked.

Candidate donor atom: the MIT pinned LeetCode #31 source contains a bounded in-place two-pointer reverse operation at `https://github.com/javadev/LeetCode-in-Java/blob/bed5d330e2b145e24e6fc1048ca28bf156b3d639/src/main/java/g0001_0100/s0031_next_permutation/Solution.java`, Git blob `302912114796dc5446be995f41eaae820efc743f`. Its lexicographic successor operation is not being proposed as an interchangeable sort. Only a separately attributed and checked reversal atom, composed under the stated M3 invariant, is relevant. The stable run-conversion proof is an explicit M3 adaptation proposal.

Required evidence: current preimage/API seal; recipe fixed point/refusal; independent stable-sort differential over repeated keys and signed extrema; sliced/filtered/opposite-order/round-trip sequences; physical-index tie-order comparison; actual Java allocation/CPU measurements if a performance claim is made.

## 2. Binary boundary discovery in the existing packed interval owner

Existing owner: `m3/collections/src/main/java/com/m3/collections/M3LongRangeSet.java`.

The full-read source keeps sorted nonoverlapping nonadjacent parallel `starts[]` and `ends[]` lanes. Its `add(from,to)` currently linearly scans every preceding end before coalescing overlapping/touching intervals. Its `remove` uses a binary search for its first affected interval but linearly searches the last start.

Candidate leaf: use bounded binary lower/upper searches for the appropriate endpoint inequalities, retaining the existing shift/copy, capacity, no-op and revision mechanics. `add` must include touching intervals, whereas `remove` must use half-open intersection rules. Reuse this owner's existing binary-search structure instead of creating another range/index library.

Reference: MIT pinned LeetCode #57, `https://github.com/javadev/LeetCode-in-Java/blob/bed5d330e2b145e24e6fc1048ca28bf156b3d639/src/main/java/g0001_0100/s0057_insert_interval/Solution.java`, Git blob `be5b6e722b2282cf1d1d2dbd1abf78f5ec45d813`. It supplies an interval-prefix/suffix comparison reference but its closed `int[][]` storage, row allocation and suffix aliasing are not suitable direct substitutes for the existing half-open signed-long packed owner. Binary boundary mechanics are already established by M3LongRangeSet/M3LongSortedSnapshot and should remain within those owners.

Required evidence: adjacency, empty/reversed intervals, both signed extrema, one-range containment/idempotent insertion, splits, removal at boundaries, capacity equality, callback revision detection, invariant checks and exact comparison against the old implementation. Overall shifting operations remain O(r); only boundary discovery is proposed for reduction. No speedup is claimed.
