<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 primitive PriorityQueue in-place masked selection

Stack lineage: [M3JDK21 #971](https://github.com/hsoliwal/M3jdk21/pull/971)
(List/Deque), [#974](https://github.com/hsoliwal/M3jdk21/pull/974)
(Set/Map), this receiving continuation (PriorityQueue).
Canonical recipe: [Synexia #11874](https://github.com/hsoliwal/com.synexia/pull/11874),
commit `6c743495b135716a3f225fc45167d64c13802328`. Target parent `127a989e53772b7b27aeee05f39ced22386a6402`.

Exactly one existing Java class adds `retainByStateMask(byte[],int)`.
Its single `long[] heap` is the authoritative payload, and caller-owned
position-aligned flags describe the heap's **unsorted iterator order**.
The method compacts accepted values directly to the front of the original
array, zeros discarded positions, adjusts size, then invokes incumbent
`siftDown` bottom-up to restore min-heap invariant. Repeated removeFirstLong
remains signed-long sorted, including duplicate values and extrema. O(n)
work, O(1) additional scratch, **no intermediate Object/Long/Map.Entry,
array, heap node or collection**.

Invalid masks and wrong length refuse before any mutation. No-op masks
preserve existing fail-fast iterator validity; actual removal increments
modCount once. No concurrent or transactional snapshot promise.

Three target Java postimages (production/JUnit/standalone) are source-sealed
by `mask-priority-postimages-20261016.tsv`, and verified without mutation.
The focused Java21 workflow compiles the existing core, then runs 128-seed
binary-heap contract oracles against JDK PriorityQueue for priority order
and after-heapification insert/drain. JDK test boxed longs are test-only.

This is only the independent M3 collections proving module, **not java.base
JDK public PriorityQueue replacement**, JNI installation, all-provider
superset completion, phase advancement, M3JDK installed-image runtime proof,
or benchmark result. Actual CI/Maven/OpenRewrite/JUnit/ABI/installed-JDK
gates and independent review remain required. Keep DRAFT/HOLD.
