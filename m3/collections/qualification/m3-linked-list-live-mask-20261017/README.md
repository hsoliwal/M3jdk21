<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Source-first linked-list physical-slot occupancy masks
Canonical Synexia source-generation proof; M3JDK21 separately built Apache-2.0 receiver.
Target baseline `master@e913ab89056657eca4009678cfe9a657c020e79b`.

## Per-file M3Scale passes
1. **Inventory:** `M3LinkedList` already owns `Object[] values` and `int[] before/after` with a linked reusable free list. Public `List/Deque/SequencedCollection`, JDK21 reverse view and iterator semantics remain the authority.
2. **Inline:** retain existing indexed payload and free-list operations; no new owner.
3. **Atomize:** add one `long[] liveMask` (one bit per *physical* slot, with a shared zero-length mask for empty stores) to the same `Storage`; changed on insert/erase/clear. Grow in the same staged array promotion as the original three arrays.
4. **Patternize:** use bitwise occupancy for free/link invariant checks instead of temporary `boolean[used]`. Explicit checks bound list walks to prevent cycles.
5. **Comment/test:** 25,000 seeded real JDK21 LinkedList operations, reverse mutations, fail-fast iteration, 4,097-slot reuse, reset, nullable elements; source hashes and fixed-point/refusal.

This optimizes repeated invariant verification's temporary memory from O(used)
to O(1), with retained state overhead `ceil(capacity/64)*8` bytes.
It is **not** a universal CPU/heap win; liveMask adds permanent storage.
Existing JNI mask selectors remain the *primitive-index* owner and are not
changed. Java `Object[]` payload references never cross JNI.

Run `mvn -B -ntp -f synexia-openrewrite-recipes/recipes/m3-linked-list-live-mask-20261017/pom.xml verify` for exact source recipe proof.
Subsequent M3JDK21 CI must compile real Java21 sources, run the JDK differential
oracle, current JNI compile parity, and owning Maven. These remain HOLD until executed;
no JDK image, jtreg/JCK, cross-platform or String→arrays→collections promotion claim.
