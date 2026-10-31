<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Sorted-set algebra — actual current-master M3JDK21 receiving receipt

**Source baseline:** `hsoliwal/M3jdk21 master@4e29dc9d4141261e8763fbe9ffd6a20e017b1aff`.
**Canonical recipe:** Synexia [PR #12126](https://github.com/hsoliwal/com.synexia/pull/12126), restoring the original class-backed Maven/OpenRewrite recipe
from Synexia #11937; the canonical source snapshots are not product runtime dependencies.

The master `M3LongCompositeViews.java` Git blob before admission is
`30a9d1b1b3ae781c4cefa543a1b24fd9c6c62f58`, exactly the admitted preimage in the
source-sealed Java recipe. Its generated postimage Git blob is
`e3e6594c62a233aedfa4d4a9bd2f36557cb4a453`; the two JUnit/standalone
test blobs are `e7e9c6e3970adb0c5d7d482b78d22b3e49ab1621` and
`8b648ac7da24d144ab82e335c2ce8589ef6c956e`.
Exact SHA-256 custody of the three generated Java files is enforced by
`verify_sorted_set_algebra.py` and `sorted-set-algebra-postimages-20261017.tsv`.

**Three additive tools:** `sortedIntersection`, `sortedDifference`, and
`sortedSymmetricDifference`. Each creates a read-only live view of existing
`M3LongSortedSet` owners. Sorted signed-long traversal has O(n+m) total
merge work with two primitive cursor controls and O(1) scalar scratch.
There are no retained per-element `Long`, `Entry`, node or duplicate
payload objects. Navigation scans are O(n+m), not constant/logarithmic.
Java predicates are not executed through JNI. Existing source masks and
bit-index kernels remain unchanged and can compose at their documented
boundaries.

This is a set (unique-key) contract, not a bag or multiset count contract.
It deliberately does not implement `java.util.NavigableSet`; publicly
named JDK collection contracts remain unchanged. No source under
`src/java.base` or HotSpot is changed. The active M3 receiving stage is
still STRING: this separately built collection proving-ground module is
not proof of an installed-JDK backend replacement.

**Donor comparison:** first-party Apache-2.0 Synexia/M3 existing sorted-union
owner and primitive set shapes were reused. JDK TreeSet is an independent
test oracle. LeetCode 349/350 set versus multiset, HackerRank set operations,
and GeeksforGeeks two-pointer sorted-array intersection provide distinct
problem-category comparison ideas; none of their implementations or
editorial text is copied. Source rights are Apache-2.0 and OpenJDK stays
under its own file-level license.

**Verification status:** source/template SHA and legacy code review
are established. Current-head GitHub Actions Maven/JUnit, Java21
Linux/macOS/Windows, prior primitive/JNI regressions, installed M3JDK
jtreg/HotSpot/ABI, and retained heap/JMH are **pending** until those jobs
actually run successfully. A `merged` flag or donor-side proof does not
substitute for current-master tree readback and exact-head execution.
