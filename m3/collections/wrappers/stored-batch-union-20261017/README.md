<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Filtered / sorted wrapper combined receiver — October 17, 2026

**Draft; stacked on M3JDK21 #1051** (`m3/filtered-batch-admission-20261017` at `c5e5e90e2dcc2298a372ab39640120dd60a2edb7`).
Canonical recipe in Synexia **PR #12040** (`m3/collection-stored-batch-union-20261017`, source commit `4603897b59df5d59870f62f6d1fb4ffe58588e79`).
Do not rebase/reset/force-push default branches. Do not claim a merge means the original files landed: inspect target source-tree blobs.

## Exact source-sealed payload

Manifest has one existing Java receiver and one previously absent standalone test.

| Target | Preimage SHA-256 | Reviewed postimage SHA-256 |
|---|---|---|
| `m3/collections/src/main/java/com/m3/collections/M3CollectionViews.java` | `115f16592a41ce78a63b4892904cb4fe934f5ee45974ffb3305ec39a7c0b9f6e` | `e71a3ee67e69fa4f24d20e4a44e929333334ca82cf05793c241d8119cd2937f1` |
| `m3/collections/wrappers/src/test/java/com/m3/collections/M3StoredRepresentativeProof.java` | ABSENT | `2c19ec2913ab6f9e5874f0209565f90ea858b79521f55a0916ed9892548da136` |

Original source and regression donor:
`M3CollectionViews.java` #1051 Git blob `55dd91e8f413b48a59a60637aa9f9d1ee15e678a`;
stored-representative methods #1050 Git blob `a8484e14fedc280a9c9f02169d2a749082b20932`;
original test #1050 blob `859da39c420ccf05adfe1a60c8da86c34e2e9571`.

## Behavior / invariants

1. Preserves the complete existing `FilteredCollection.addAll` staged/validated batch atom, preventing an intervening second source read; it is not a transactional guarantee.
2. `contains` observes the **stored** equal occurrence's predicate status, not a caller's equal probe with different state.
3. `remove` targets the actual visible List index (including CopyOnWriteArrayList), avoiding deletion of a hidden equal duplicate. Other owner collections use their supported removal boundary.
4. Existing SortedSet/NavigableSet comparator representatives, bounded/descending subset rules, predicate order, deep filter tree, spliterator metadata and sorted snapshots are unchanged.
5. No public API descriptors, existing `java.util` or `java.lang.String` files, JNI symbols, external provider dependencies or Synexia runtime services are altered.

## Reproduction

The target retains only exact checksum/provenance receipts, compiled Java test sources and a **thin** receiver verifier.
The **Maven/OpenRewrite class-backed recipe, JUnit fixed-point/refusal fixtures, source images and catalogue** remain in Synexia PR #12040.

To verify the compiled wrapper against actual Java21 JDK backing types:

```sh
bash m3/collections/wrappers/stored-batch-union-20261017/verify.sh
```

This checks current installed SHA-256 postimage, compiles with `javac --release 21 -Xlint:all -Werror`, runs inherited `FilteredBatchContracts` and the original stored-representative proof in tiered and interpreted modes. A separate canonical Maven SDK execution must prove recipe emission, fixed point and refusal at the exact source identity; a checksum check does **not** replace Maven.

### JNI/native and full-product holds

Reuses unchanged native collection iteration/spliterator consumer from predecessor #1051 / Synexia #12028. No native callback is pushed into the predicate or comparator; adding a JNI export without a measured safe benefit would change semantics and cost. Existing native JNI coverage is an independent gate. **Not qualified here:** Maven SDK run, complete M3 collections reactor, source-built JDK, jtreg/JCK, JNI native memory/lifetime, Win/macOS and concurrency/serialization/stress and CPU/retained-heap benchmarks. String→arrays→collections receiving phase is **not** automatically advanced.

Rights: Source and recipe expressions remain first-party Apache-2.0; the original OpenJDK `java.util` license and source stay untouched. Catalogue donors Guava/Eclipse, JDK, LeetCode/HackerRank/GfG were compared without importing external source/solutions.
