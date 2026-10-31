<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0
-->
# Array-first collection alternatives: bounded qualification

Status: contract frozen before implementation; execution results belong in
`RESULTS.md` and the ordered event ledger. This document is not a passing gate.

## Scope and ownership

The requested direction is to prefer compact arrays and suitable challenge-style
algorithms in M3 collection implementations, retain specialized structures under
accurate strategy labels, and examine LeetCode/HackerRank alternatives. Public
class names, provider contracts and existing factory return types remain locked.
This packet qualifies one concrete sorted-batch optimization. The accompanying
algorithm catalogue records broader candidates without claiming every fork was
audited, converted, benchmarked or admitted to the JDK runtime.

Receiver baseline: `hsoliwal/M3jdk21` commit
`d814e68f98715ebf71743c00b82f5a9a61223af0`.
Canonical reusable recipe owner: `hsoliwal/com.synexia`,
`synexia-openrewrite-recipes`. Original LongLongTreeMap recipe PR 12328 remains
open; its receiver PR 1322 was merged. This successor must match the current
receiver preimage rather than replay an old absent-file admission. It reuses the
current hash-pinned Java/text OpenRewrite engines, with a distinct named crate.

No production source is to be hand-edited. Candidate templates precede recipe
lint, compilation and tests; only actual qualified recipe output is materialized.
The independent test oracle below is a qualification artifact, not a new recipe
engine. The runtime admission order in the root policies is unchanged.

## Existing owners and immutable preimages

| Path relative to `m3/collections` | Observed strategy | SHA-256 before |
| --- | --- | --- |
| `src/main/java/com/m3/collections/M3LongLongTreeMap.java` | sorted parallel primitive arrays; forward sorted-batch merge with four temporary/replacement arrays | `2673330335f4ea415b49a295d9cde4988c27280b990ab33984a195ca032d5791` |
| `JDK_FAMILY_MAP.tsv` | family/contract dispositions; TreeSet row still describes an unselected tree despite the existing paged-array owner | `88ed35987b8956046ac8644ee8200d2aadea7ad9dd7a3175989f1e2043b9b83b` |

Other inventoried owners: `M3TreeMap` uses indexed AVL metadata; `M3LongTreeSet`
uses paged sorted primitive arrays; `M3GenericCollections` provides sorted index
snapshots over captured payloads. These structures remain available. Array
storage alone does not mean that a tree has no balancing metadata.

## Frozen `putAllSorted` contract

1. Preserve public/protected signatures and existing class/interface names.
2. Validate source keys for null before source values; preserve exception types
   and messages for null, unequal lengths and descending input.
3. Empty input returns zero without a structural change or capacity change.
4. Nondecreasing keys are required; the final incoming value for each equal key
   wins over earlier incoming and existing values. Compare keys relationally,
   including `Long.MIN_VALUE` and `Long.MAX_VALUE`.
5. Validate the complete batch before changing map contents, size, capacity or
   iterator state. Do not modify either input array. Passing the same caller
   array as keys and values remains valid.
6. Return the number of newly introduced distinct keys. Keys remain sorted and
   unique; values remain aligned with their keys.
7. Preserve the observable batch capacity rule:
   `max(previousCapacity, previousSize + distinctIncomingKeys)`. It is not based
   on union size and does not use the single-insertion geometric growth rule.
   Even an overwrite-only batch may increase capacity.
8. Increment structural modification state once exactly when any key is added.
   Replacement-only batches, including capacity growth, leave existing key
   iterators valid. Batches introducing keys invalidate existing iterators.
9. Internal arrays are privately owned; public snapshots copy. The reuse proof
   does not grant support for reflective exposure/mutation of private lanes.
10. Preserve single-key operations, navigation, callbacks, cloning/serialization
    where applicable, and all other methods. This leaf has no JNI entry point.

The old implementation can overflow `previousSize + distinctIncomingKeys` in
int arithmetic. The candidate will explicitly reject a nonrepresentable capacity
before mutation using widened arithmetic. Representable lengths are still left
to the VM rather than imposing the separate single-insert `MAX_ARRAY_SIZE` limit.
This is an explicit extreme-capacity hardening decision, not a claim of identical
old overflow or `OutOfMemoryError` timing. Removing allocations necessarily changes
resource-exhaustion timing, including before sortedness validation.

## Candidate and proof obligations

Use one read-only forward pass to validate input and count distinct/new keys,
then merge from the right. Reverse duplicate runs supply their final value.
The output begins at the exact union size minus one. At every step the unread
existing prefix fits within the remaining output prefix, so the destination
write index is never below the unread existing index. Existing lanes can be
reused when the frozen capacity rule permits it. When growth is required, both
new lanes are allocated locally before either field is published.

The intended resource result is no allocated batch/output arrays when capacity
suffices, and two destination arrays on growth instead of two batch arrays plus
two destination arrays. Complexity remains linear in existing plus incoming
length for batch merging. This does not improve the worst-case linear movement
of a single insertion into a packed sorted array.

LeetCode 88 supplies a problem category and adversarial shape, not copied code:
<https://leetcode.com/problems/merge-sorted-array/>. The candidate is independently
implemented from the target's contracts. No challenge/editorial/submission source
is imported; no new production dependency or generic collection owner is added.

## Ordered qualification gates

1. Inventory and frozen contract (this document and hash records).
2. Canonical template/manifest patch, license/provenance and structural lint.
3. Compile the bounded canonical OpenRewrite host and its existing harness.
4. Execute actual SDK recipe tests: exact output, fixed point, source drift,
   missing/duplicate/non-Java refusal, unrelated-source preservation and input
   permutations. Capture actual SDK candidate output.
5. Lint then compile receiver preimage and generated candidate using Java 21
   with warnings as errors. Compare public/protected API descriptors.
6. Execute unchanged semantic oracles against both versions; execute candidate
   lane-reuse checks. The oracle uses sequential `java.util.TreeMap.put`, not a
   copy of the merge algorithm. Include capacity versus union size, last-wins
   duplicates, extrema, same-array source aliasing, rejection atomicity, iterator
   semantics, exhaustive finite cases and fixed-seed mixed operations.
7. Execute bounded allocation/resource probes separately from correctness tests;
   record toolchain, command, actual counts and limitations. No timing claims
   from a structural assertion or tiny probe.
8. Materialize exact SDK output only after successful applicable local gates;
   record pre/post hashes and provenance. Full reactor/vendor/runtime gates not
   executed remain explicitly unverified, and review PRs remain draft.

No gate is green because a command was written here. Near-two-billion-element
allocation behavior is reviewed arithmetically unless actually exercised. Finite
coverage is not a proof of unrestricted semantic equivalence.
