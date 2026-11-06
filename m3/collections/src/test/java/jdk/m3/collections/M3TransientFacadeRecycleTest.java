// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** A deterministic DOOP no-escape lease oracle; no JVMTI snapshot is a reuse permit. */
//FLLDCIM: RELOCATE - donor blob 94857594c8b0 at fd9a233680b4; recipe m3/collections/recipe/transient-facade-20261106/relocate.py v4
class M3TransientFacadeRecycleTest {
    private static final class Mutable {
        long id;
        Object attached;
        Mutable(long id) { this.id = id; attached = new Object(); }
    }

    private static final M3TransientFacadeStore.Recycler<Mutable> RECYCLER =
            new M3TransientFacadeStore.Recycler<>() {
                @Override public void scrub(Mutable value) {
                    value.id = -1L;
                    value.attached = null;
                }
                @Override public void rebind(Mutable value, long logicalId) {
                    assertNull(value.attached, "previous owner must be scrubbed");
                    assertEquals(-1L, value.id, "previous logical ID must be detached");
                    value.id = logicalId;
                    value.attached = new Object();
                }
            };

    @Test
    void thousandSerialInternalLeasesReuseOneFacadeAndScrubEachRelease() {
        AtomicInteger creations = new AtomicInteger();
        AtomicInteger disposals = new AtomicInteger();
        int[] firstIdentity = {0};
        boolean[] seenIdentity = {false};
        try (var owner = new M3TransientFacadeStore<Mutable>(
                id -> { creations.incrementAndGet(); return new Mutable(id); },
                (id, value) -> { disposals.incrementAndGet(); },
                M3TransientFacadeStore.DEFAULT_MODE, RECYCLER, 4)) {
            for (int i = 0; i < 1_000; i++) {
                final long logical = i;
                owner.withInternal(logical, facade -> {
                    assertEquals(logical, facade.id);
                    assertNotNull(facade.attached);
                    int identity = System.identityHashCode(facade);
                    if (!seenIdentity[0]) {
                        firstIdentity[0] = identity;
                        seenIdentity[0] = true;
                    } else {
                        assertEquals(firstIdentity[0], identity);
                    }
                });
            }
            assertEquals(1, creations.get());
            assertEquals(999L, owner.reuseCount());
            assertEquals(1, owner.reusableIdleCount());
            assertEquals(0, owner.residentCount());
            assertEquals(0, disposals.get());
        }
        assertEquals(1, disposals.get(), "close must dispose the pooled object once");
    }

    @Test
    void publicFacadesStayPinnedAndNeverEnterRecycledInternalPool() {
        AtomicInteger creations = new AtomicInteger();
        try (var owner = new M3TransientFacadeStore<Mutable>(
                id -> { creations.incrementAndGet(); return new Mutable(id); },
                (id, value) -> { },
                M3TransientFacadeStore.DEFAULT_MODE, RECYCLER, 1)) {
            owner.withInternal(2, value -> { });
            Mutable pinned = owner.expose(2);
            assertSame(pinned, owner.expose(2));
            assertEquals(1, owner.reusableIdleCount());
            owner.withInternal(3, value -> assertNotSame(pinned, value));
            assertEquals(2, creations.get());
            assertEquals(1, owner.pinnedCount());
            assertEquals(1, owner.releaseReusable(0));
            assertSame(pinned, owner.peekResident(2));
            assertEquals(0, owner.releaseOptionalResidency(0));
        }
    }

    @Test
    void reentrantLeaseCannotBorrowAlreadyInUseObject() {
        AtomicInteger creations = new AtomicInteger();
        AtomicInteger disposed = new AtomicInteger();
        try (var owner = new M3TransientFacadeStore<Mutable>(
                id -> { creations.incrementAndGet(); return new Mutable(id); },
                (id, value) -> disposed.incrementAndGet(),
                M3TransientFacadeStore.DEFAULT_MODE, RECYCLER, 1)) {
            owner.withInternal(1, outer ->
                    owner.withInternal(2, inner -> {
                        assertNotSame(outer, inner);
                        assertEquals(1, outer.id);
                        assertEquals(2, inner.id);
                    }));
            assertEquals(2, creations.get());
            assertEquals(1, owner.reusableIdleCount());
            assertEquals(1, disposed.get(), "capacity overflow disposes one instance");
        }
        assertEquals(2, disposed.get());
    }

    @Test
    void actionFailureStillReturnsScrubbedLeaseAndPreservesOriginalException() {
        IllegalStateException original = new IllegalStateException("caller failure");
        AtomicInteger creations = new AtomicInteger();
        try (var owner = new M3TransientFacadeStore<Mutable>(
                id -> { creations.incrementAndGet(); return new Mutable(id); },
                (id, value) -> { },
                M3TransientFacadeStore.DEFAULT_MODE, RECYCLER, 1)) {
            assertSame(original, assertThrows(IllegalStateException.class,
                    () -> owner.withInternal(1, value -> { throw original; })));
            assertEquals(1, owner.reusableIdleCount());
            owner.withInternal(2, value -> assertEquals(2, value.id));
            assertEquals(1, creations.get());
        }
    }

    @Test
    void failedScrubNeverPublishesStaleObject() {
        AtomicInteger disposed = new AtomicInteger();
        var broken = new M3TransientFacadeStore.Recycler<Mutable>() {
            @Override public void scrub(Mutable value) {
                throw new IllegalStateException("scrub failed");
            }
            @Override public void rebind(Mutable value, long id) { throw new AssertionError(); }
        };
        try (var owner = new M3TransientFacadeStore<Mutable>(
                Mutable::new, (id, value) -> disposed.incrementAndGet(),
                M3TransientFacadeStore.DEFAULT_MODE, broken, 1)) {
            IllegalStateException e = assertThrows(IllegalStateException.class,
                    () -> owner.withInternal(1, value -> { }));
            assertEquals("scrub failed", e.getMessage());
            assertEquals(0, owner.reusableIdleCount());
            assertEquals(1, disposed.get());
        }
    }

    @Test
    void failedRebindDisposesDetachedOwnerAndDoesNotPoisonIdlePool() {
        AtomicInteger disposed = new AtomicInteger();
        var broken = new M3TransientFacadeStore.Recycler<Mutable>() {
            @Override public void scrub(Mutable value) { value.attached = null; }
            @Override public void rebind(Mutable value, long id) {
                throw new IllegalStateException("rebind failed");
            }
        };
        try (var owner = new M3TransientFacadeStore<Mutable>(
                Mutable::new, (id, value) -> disposed.incrementAndGet(),
                M3TransientFacadeStore.DEFAULT_MODE, broken, 1)) {
            owner.withInternal(1, value -> { });
            assertThrows(IllegalStateException.class, () -> owner.withInternal(2, value -> { }));
            assertEquals(0, owner.reusableIdleCount());
            assertEquals(1, disposed.get());
        }
    }

    @Test
    void highHeapPressureTrimsIdlePoolWithoutTouchingPinnedIdentity() {
        AtomicReference<M3RuntimeHeap.Snapshot> heap = new AtomicReference<>(
                new M3RuntimeHeap.Snapshot(1L << 30, 500L << 20, 120L << 20));
        AtomicInteger disposals = new AtomicInteger();
        int mode = M3TransientFacadeStore.DEFAULT_MODE
                | M3TransientFacadeStore.AUTO_HEAP_PRESSURE;
        try (var owner = new M3TransientFacadeStore<Mutable>(
                Mutable::new, (id, value) -> disposals.incrementAndGet(),
                mode, heap::get, RECYCLER, 4)) {
            Mutable pinned = owner.expose(3_000);
            owner.withInternal(1, value -> { });
            assertEquals(1, owner.reusableIdleCount());
            heap.set(new M3RuntimeHeap.Snapshot(1L << 30, 980L << 20, 940L << 20));
            for (int i = 0; i < 64; i++) owner.withInternal(100 + i, value -> { });
            assertEquals(0, owner.reusableIdleCount());
            assertTrue(disposals.get() >= 2);
            assertSame(pinned, owner.expose(3_000));
        }
    }

    @Test
    void legacyConstructorPreservesOriginalExceptionPrecedence() {
        IllegalStateException caller = new IllegalStateException("caller");
        IllegalArgumentException disposer = new IllegalArgumentException("disposer");
        try (var owner = new M3TransientFacadeStore<Mutable>(
                Mutable::new, (id, value) -> { throw disposer; },
                M3TransientFacadeStore.DEFAULT_MODE)) {
            IllegalArgumentException actual = assertThrows(IllegalArgumentException.class,
                    () -> owner.withInternal(19, value -> { throw caller; }));
            assertSame(disposer, actual,
                    "the opt-in recycler must not alter existing legacy callbacks");
            assertEquals(0, owner.residentCount());
        }
    }

    @Test
    void failedIdleDisposalCannotReturnPartiallyDisposedFacadeToPool() {
        AtomicInteger creations = new AtomicInteger();
        AtomicInteger attempts = new AtomicInteger();
        var owner = new M3TransientFacadeStore<Mutable>(
                id -> { creations.incrementAndGet(); return new Mutable(id); },
                (id, value) -> {
                    value.attached = null;
                    if (attempts.getAndIncrement() == 0) {
                        throw new IllegalStateException("disposal failed");
                    }
                },
                M3TransientFacadeStore.DEFAULT_MODE, RECYCLER, 1);
        owner.withInternal(1, value -> { });
        assertEquals(1, owner.reusableIdleCount());
        assertEquals("disposal failed",
                assertThrows(IllegalStateException.class,
                        () -> owner.releaseReusable(0)).getMessage());
        assertEquals(0, owner.reusableIdleCount(),
                "failed disposal must detach the idle slot");
        owner.withInternal(2, value -> {
            assertEquals(2, value.id);
            assertNotNull(value.attached);
        });
        assertEquals(2, creations.get(), "the disposed instance cannot be borrowed");
        owner.close();
        assertEquals(2, attempts.get());
    }

    @Test
    void rejectsUnknownLifetimeAndUnboundedRecyclerRequests() {
        assertThrows(IllegalArgumentException.class, () ->
                new M3TransientFacadeStore<Mutable>(Mutable::new, (id, value) -> { },
                        M3TransientFacadeStore.RETAIN_UNEXPOSED
                                | M3TransientFacadeStore.PIN_EXPOSED,
                        RECYCLER, 2));
        assertThrows(IllegalArgumentException.class, () ->
                new M3TransientFacadeStore<Mutable>(Mutable::new, (id, value) -> { },
                        M3TransientFacadeStore.DEFAULT_MODE, RECYCLER, 0));
        assertThrows(IllegalArgumentException.class, () ->
                new M3TransientFacadeStore<Mutable>(Mutable::new, (id, value) -> { },
                        M3TransientFacadeStore.DEFAULT_MODE, RECYCLER, 257));
    }
}
