<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Binary-heap construction: qualified technique candidate

LC215 is used only to classify the heap/priority-queue problem. No problem solution
source or editorial code was copied. The candidate follows bottom-up Floyd heap
construction on existing primitive array owners. Building from a copied primitive
array takes O(n) sifting work; repeated `offer` builds in O(n log n) worst case.

Pre-existing operations, public class names, iterator invalidation, signed primitive
order, duplicate treatment, array detach, and growth policy are retained. Physical
heap traversal order from construction is not a public sorted-order promise.

Canonical private source recipe: [com.synexia #14086](https://github.com/hsoliwal/com.synexia/pull/14086).
Do not treat the new constructor as a LeetCode donor implementation or as a JMH
performance result. Receipt in qualification/priority-heapify-20261029.
