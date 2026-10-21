# M3JDK21 linked-index no-copy Spliterator - product receiving
Date: 2026-10-19; SPDX-License-Identifier: Apache-2.0.
Source owner: M3JDK21 M3LinkedList, existing Object[] values / int[] before-after / long[] liveMask and free-list.
Canonical Synexia source-sealed recipe: https://github.com/hsoliwal/com.synexia/pull/12424
Original M3LinkedList master blob: 744d1c488b8342c98e49d2cfd3465c0f558b199d

One behavioral atom replaces delegated JDK IteratorSpliterator's temporary element-batch splitting.
LinkedIndexSplit reads physical links directly, partitions by midpoint walks, bounds CPU shard count <=128, no per-element node/Entry/Object[] copy.
Original List/Deque methods, nulls, duplicates, reversed backed views, modCount and physical slot masks stay owner-authoritative.
Complexity note: O(n log P) total midpoint work, O(P) cursors, not O(1) split; a sparse rank directory would only be considered after CPU/memory measurements.
Spliterator is late-bound; unfiltered M3LinkedList retains ORDERED|SIZED|SUBSIZED. Filtered variants must remove SIZED when unknown.
No Java object refs, predicates or long-lived GC pointers cross JNI. Existing M3IndexPathMasks C17/JNI remains unchanged and separately owned.

LOCAL PROOF: Java21 independent algorithmic twin compiled with javac --release 21 -Xlint:all -Werror; 5155 assertions passed.
NOT EXECUTED HERE: actual M3LinkedList test source, Maven/OpenRewrite SDK, JUnit owning module, native integration, jtreg/JCK, cross-platform, JMH benchmarking.
Do not merge until actual file hashes, compile/test receipts and complete parent/source materialization are reviewed. No force push or direct master updates.
