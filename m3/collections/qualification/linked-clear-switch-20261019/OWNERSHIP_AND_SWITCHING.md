<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Reuse, cooperative borrows, and switching

Source observations below are pinned to M3JDK21
`a3e88976ee8664d1ef5d0cade96aec1206b2133d`. They describe existing code;
they do not assert that the JVM reference-counts every Java object.

| Existing owner | What it owns | Safe recovery boundary |
| --- | --- | --- |
| `M3LinkedList.Storage` | Object payload lane, integer links, occupancy bits, free head | erase nulls the removed reference; a later insertion reuses that slot |
| `M3ObjectResidency` | Parent words and disposable child projections | no active callback or explicit borrower, with the applicable recreate/swap/pin policy |
| `M3SlotAllocator28` | Primitive slot generations and free-list state | release invalidates the old generation; a reused slot gets a different handle |
| `M3ResidencyStateMaskIndex` | No child payload owner; selects primitive coordinates | caller-owned output can be reused after its consumer finishes |
| `M3ObjectArrayPool` | Bounded thread-confined temporary arrays | same-thread Lease.close clears references before optional caching |

## What M3's count means

The existing indexed list removes per-node wrapper objects, not all Java
allocation or GC work. Its Object[] payload lane stores references; it does
not place arbitrary payload objects inline. Growth allocates replacement
arrays, and free-slot reuse can scatter logical neighbors across physical
slots. Dense sequential traversal and linked traversal therefore need distinct
measurements. A directory of fixed-size chunks can support indexed lookup;
a chain of chunks alone does not make arbitrary indexing constant-time.

Java references are not invariably eight bytes: compressed references can be
four bytes, as the [OpenJDK JOL examples](https://github.com/openjdk/jol) show.
Compare headers, alignment, spare capacity, payload references and every index
lane on the target JVM. A 16-bit link also imposes a slot/sentinel bound and
requires a checked migration policy on growth. Neither narrower links nor
object-array serialization is a free general replacement.

`M3ObjectResidency.retainProjection(slot, estimatedBytes)` returns a primitive
long handle. `useProjection(handle, operation)` resolves it through the parent
under the existing monitor. `releaseProjection(handle)` decrements the
explicit borrower count and invalidates the handle. Each retain needs exactly
one release in a finally block. Independent consumers retain independently;
copying a long does not create a second borrow. A stale or double-released
handle is rejected, and release during its callback is rejected.

```java
long borrow = arena.retainProjection(slot, estimatedBytes);
try {
    return arena.useProjection(borrow, projection -> projection.size());
} finally {
    arena.releaseProjection(borrow);
}
```

The count covers this cooperative API, not arbitrary heap aliases, iterator
references, JNI global references or hidden callback escapes. The current
implementation is synchronized, not lock-free. `STRICT_NO_ESCAPE` rejects a
direct return of the projection but cannot detect capture in a field, lambda,
container or thread-local; caller/compile-time non-escape proof is required.
An asynchronous event must retain until acknowledgement and release even on
failure or cancellation. Enqueue failure must also release the acquired borrow.

`retireUnborrowedProjection` additionally requires RECREATE and
STRICT_NO_ESCAPE, no swap mode, no caller-installed value, no permanent pin,
and a resident slot. It drops the arena's strong reference. GC determines when
memory is reclaimed; the method neither frees a Java address nor pools the
arbitrary child object. Canonical parent words survive. A factory must rebuild
from that canonical state, not from state held only in the retired projection.
Reclamation at every operation can create churn; retain a useful projection
while hot and retire at an explicit safe boundary or pressure decision.

## Two distinct kinds of switching

The implemented candidate switches the *clear algorithm* using current live
size divided by retained capacity. It retains one Storage and the same arrays.
Lists at no more than 1/64 occupancy visit live links and zero touched mask
words. Denser lists keep the original bulk clearing. Hash maps/sets use the
same payload rule but still reset the full bucket index. There is no representation
migration and therefore no converted copy or handoff allocation. The threshold
is a measured heuristic, not a promise that every machine has the same crossover.

A future switch between flat, segmented, indexed-linked or native storage is
a different operation. It must retain public order, equality, null policy,
backed views, iterator/spliterator rules and concurrency semantics. Keep one
authoritative payload, validate the prospective representation before publishing
it, and preserve the old state on failure. Existing borrowed projections stay
valid until their borrowers finish. Use generation-checked handles for opted-in
projections; ordinary List iterators are not automatically such handles.
Do not recycle the old storage while an allowed reader can still access it.

Such migration needs measured entry/exit thresholds and hysteresis to avoid
oscillation. Its temporary double-storage peak must fit the memory budget.
Under pressure, dropping optional indexes or staying in the current form can be
safer than starting a large conversion. No universal auto-switching storage
backend is introduced by this change.

## Pressure and JNI

Free-slot reuse lowers subsequent allocation, but retained capacity still
occupies heap. List clear releases payload references and retains its arrays.
Reference counts alone do not reduce those arrays or make allocation free.
The residency owner has explicit budget/pressure admission and excludes active,
borrowed or pinned victims; its LRU victim search currently scans slots. Those
costs must be measured before using it on every collection operation.

`M3ObjectArrayPool` is for its documented temporary arrays, not live collection
backing storage. Its leases have their own allocations and thread ownership.
DOOP event observation is also separate from lifetime counting; an observer
notification does not prove a storage object has no readers.

JNI is a batch/materialization boundary here, not a way to resize Java arrays,
cast a jobject handle into a stable heap address, or return one char[] view over
disjoint arrays. Array-element acquisition may copy; acquired pointers must be
released on every path. Direct buffers have fixed capacity and explicit owner
lifetimes. Filtering into a vector and returning a new Java array allocates;
it is not a zero-allocation view. Primitive selection into caller-owned output
already exists in `M3ResidencyStateMaskIndex.selectInto` where that contract fits.

## Delivery limits

This work uses first-party source under its existing Apache-2.0 attribution.
Challenge categories and external collection layouts are evidence, not copied
implementations or proof of a performance gain. This atom changes list and
linked-map payload clearing. It neither adds a JVM reference-counting collector nor claims every
linked collection, priority queue, map, stream or fork is optimized.
