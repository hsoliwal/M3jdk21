<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Swift Collections donor intake and deque span copying

The requested `apple/swift-collection` URL returns 404. The inspected repository is
[`apple/swift-collections`](https://github.com/apple/swift-collections/tree/b905c889ff75059b8ef0bde480fe55948147b1e6).
Its exact file identities and license are in SOURCES.tsv and SWIFT-LICENSE.txt.

The searchable catalogue is [swift-collections-20261018.tsv](../../algorithm-catalog/swift-collections-20261018.tsv).
Eight categories map to existing M3 owners. Only the ring-copy atom is selected here.
Other rows document compatibility concerns and pending proof, not completed ports.
Existing collection-candidate-comparison-20261017.tsv and collection-file-review-20261017.tsv
were consulted first, including their LeetCode, HackerRank and GeeksforGeeks taxonomy references.
Those references nominate test shapes and convey no editorial/submission copying permission.

`M3LongArrayDeque.toArray()` and full-ring growth now share one private helper:
copy `min(size, capacity-head)` elements, then any wrapped suffix. The destination remains
a fresh primitive array. The growth threshold, capacity doubling, publication after allocation,
head reset, zeroing, exceptions, full long domain, iterator epochs and public API stay unchanged.
The algorithm remains O(n); it replaces per-element masked addressing with at most two arraycopy calls.
No new JNI method or native memory ownership is needed: it uses the existing JDK copy primitive.

Canonical recipe: `com.synexia.m3.SwiftDequeSpans20261018`, in Synexia's existing hash-pinned
Java/text recipe owners. Target predecessor: `7c6b3b51094c50c84746b819d79665e5f003bbdf`.
RECEIVER.json binds the published canonical recipe revision and exact materialization hashes.
LOCAL_VERIFICATION.md records baseline/candidate contracts, resource diagnostics and full Java/JNI checks.
No timing or memory result here establishes a universal speedup. Swift tests/toolchain are not run.
This is collection proving-ground evidence; it does not promote the ordered JDK receiving phase
or prove the unrelated 19,396-source generated-template/JAR assertion.
