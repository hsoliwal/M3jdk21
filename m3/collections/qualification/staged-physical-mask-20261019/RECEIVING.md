<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 multi-stage indexed-mask collection receiver

This is M3JDK21 itself, **not** an M3 library wrapped around an unchanged JDK. It extends the *existing* M3JDK21 m3/collections M3IndexedListSnapshot and M3CollectionSdk owners, without adding a second retained object store or loading Synexia at runtime.

## Frozen mixed-epoch input

- M3JDK21 canonical runtime preimage: `2786e22ee2b85af4c1853041c7d80415a9c2a6d1`, after the master's original single-stage physical mask method and private int[] stable sort were both present.
- Existing source-aligned proof workflow preimage: original unmerged M3JDK21 #1364 head `5dd41493b8521c97d692fe9c6c4ae27092a2bfe3`.
- Canonical Synexia first-pass source-sealed recipe: PR #12369; the new v2 receipt is a distinct hash-pinned successor, not an overwritten historical template.
- No existing code or PR branch is discarded: the target tree is a literal additive superset and its merge commit retains the earlier #1364 head as a second parent.

## Program invariant

```
INVENTORY (exact current source; check chronological ancestry)
-> INLINE -> ATOMIZE -> PATTERNIZE -> COMMENT
-> MECHANICAL FORWARD/BACKWARD (per-file)
-> ALGORITHMIC FORWARD/BACKWARD (problem shape/donor evidence)
-> MATHEMATICAL FORWARD/BACKWARD (O(stage*n), stable sort)
-> LLM LAST (typed unresolved residue only)
-> FIXED POINT / REFUSAL
-> DIFF -> LINTER -> COMPILER -> TESTS -> RUNTIME
-> SERIAL PROMOTION (only for admitted output)
```

Canonical runtime representation:

```
Object[]      single GC-visible authoritative payload snapshot
int[]         immutable logical-to-physical position directory
int[][]       temporary caller-borrowed full-width physical state planes
int[]         selected private output directory, reused by stable sort
offset/count  logical view coordinates
```

- `maskedByPhysicalStates` accepts one or more precomputed state planes with exact physical-capacity alignment. Each plane is tested left-to-right against 32-bit required/forbidden masks, including the sign bit.
- `sortedMaskedByPhysicalStates` applies only the *existing* M3IndexPathMasks Java/C17 JNI mask selector and the *existing* stable physical-index sorter. Only selected values reach Java comparators, no Java object enters native code, and JNI does not own payload storage.
- `M3CollectionSdk.indexedObjectSnapshot` and `maskedSortedIndexedSnapshot` expose the same owner, preserving separate live filtered wrapper contracts (M3FilteredLists and M3FilteredViews).
- Validation before native dispatch protects wrong/null/short/oversized stages. Source and snapshots remain unaffected; no mutation is admitted through these immutable operations.

## Proof/failure boundaries

- Distinct source manifests and exact SHA-256 seals must match all non-absent preimages and postimages; second run must be unchanged; drift/missing/occupied targets refuse.
- `M3StagedPhysicalStateSnapshotProof` is an independently coded List oracle: full-width, sign bit, null/duplicate values, stable equal-comparison ties, all/no filter, arbitrary slices/permutations, defensive ownership and native-required refusal.
- Execute using the existing Java21 C17/JNI workflow amended to run both first-pass and staged tests; the Maven JUnit entrypoint also exercises the SDK facade.
- Runtime proof does not establish faster JNI dispatch, installed java.base replacement, full five-provider equivalence, compiler/HotSpot acceptance or platform breadth.
- String → arrays → collections → AST/compiler remains the target receiving order. This candidate does not override the active String phase.
