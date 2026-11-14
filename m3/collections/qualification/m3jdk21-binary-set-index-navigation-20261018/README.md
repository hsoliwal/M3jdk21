<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 indexed paged-long binary set-algebra navigation

## Authority and contracts
Canonical named Synexia Maven/OpenRewrite recipe:
`com.synexia.m3.Jdk21BinarySetIndexNavigation20261018`.
Exact M3JDK21 baseline `master@2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49`.
One owner `M3LongCompositeViews` and two previously absent Java21 oracles.
The existing hash-sealed recipe replaces only this file's bounded navigation atom;
no manual source postimage patches or transformation framework.

A binary sorted-set algebra navigation call currently builds a full iterator from
the beginning even if searching for an element near the far end. When *both*
operands are the final first-party M3LongTreeSet with proven signed-long indexed
navigation, reuse `higher/lower` to begin near the pivot, then advance only
candidate rows. All other M3LongSortedSet suppliers retain the literal old path,
including invalid-order validation and callback behavior.

This is an O((1+skipped)log n) selective path for late/sparse queries, not a
claim of a universal throughput win over O(n) sequential merging. Preserve
Long.MIN_VALUE/MAX_VALUE, duplicate suppression, empty sets, caller fallback,
natural signed order and stable source lifetime. No Entry/Node payload owner,
array storage duplicate, JNI ABI, or API contract changes.

## Evaluated donors
First-party M3 paged TreeSet and primitive binary algebra are preferred. JDK21
TreeSet is an independent test oracle. LeetCode 349/350, HackerRank Sets and
GeeksforGeeks sorted array algorithms provide categories only; no code borrowed.
Existing licenses and source attribution retained (Apache-2.0 for new output).

Java21 JDK differential tests, original generic sorted-algebra oracle, actual
canonical Maven/OpenRewrite fixed point/drift refusal, JNI parity for unchanged
native selector, source-vendored JUnit, installed-JDK/JTreg/JCK and CPU/heap
benchmarks must gate promotion.
