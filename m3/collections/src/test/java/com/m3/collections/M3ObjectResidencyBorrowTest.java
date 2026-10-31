// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** Exact cooperative DOOP lease counts, not HotSpot object reachability. */
final class M3ObjectResidencyBorrowTest {
    private static final int MODE = M3ObjectResidency.LAZY_CREATE
            | M3ObjectResidency.LRU | M3ObjectResidency.RECREATE
            | M3ObjectResidency.STRICT_NO_ESCAPE;

    @Test
    void independentBorrowersPreventEarlyReclamationAndStaleReuse() {
        int[] constructed = new int[2];
        var arena = new M3ObjectResidency<Integer>(2, MODE, 8, 0,
                slot -> ++constructed[slot], null, null);
        arena.writeParentBits(0, 0x55L);
        long a = arena.retainProjection(0, 8);
        long b = arena.retainProjection(0, 8);
        assertEquals(2, arena.projectionBorrowCount(0));
        assertEquals(1L, arena.useProjection(a, Integer::longValue).longValue());
        assertFalse(arena.retireUnborrowedProjection(0));
        assertThrows(M3ObjectResidency.BudgetExceededException.class,
                () -> arena.use(1, 8, value -> 1L));
        arena.releaseProjection(a);
        assertEquals(1, arena.projectionBorrowCount(0));
        assertFalse(arena.retireUnborrowedProjection(0));
        assertThrows(IllegalStateException.class, () -> arena.releaseProjection(a));
        arena.releaseProjection(b);
        assertEquals(0, arena.projectionBorrowCount(0));
        assertTrue(arena.retireUnborrowedProjection(0));
        assertEquals(M3ObjectResidency.State.ABSENT, arena.state(0));
        assertEquals(0x55L, arena.readParentBits(0));
        assertEquals(2L, arena.use(0, 8, Integer::longValue).longValue());
        assertEquals(2, constructed[0]);
        long next = arena.retainProjection(0, 8);
        assertNotEquals(a, next);
        assertThrows(IllegalStateException.class, () -> arena.useProjection(a, x -> 1L));
        arena.releaseProjection(next);
    }

    @Test
    void callbackExceptionAndReentrantReleasePreserveCount() {
        var arena = new M3ObjectResidency<StringBuilder>(1, MODE, 64, 0,
                slot -> new StringBuilder("a"), null, null);
        long token = arena.retainProjection(0, 8);
        assertThrows(IllegalArgumentException.class, () -> arena.useProjection(token,
                projection -> {
                    assertThrows(IllegalStateException.class,
                            () -> arena.releaseProjection(token));
                    throw new IllegalArgumentException("event failed");
                }));
        assertEquals(1, arena.projectionBorrowCount(0));
        assertFalse(arena.retireUnborrowedProjection(0));
        assertThrows(IllegalStateException.class, () -> arena.useProjection(token,
                projection -> projection));
        arena.releaseProjection(token);
        assertTrue(arena.retireUnborrowedProjection(0));
        assertThrows(IllegalStateException.class, () -> arena.releaseProjection(token));
    }

    @Test
    void ExternalAndUnprovenOwnersNeverFastRetire() {
        var installed = new M3ObjectResidency<Object>(1, MODE, 8, 0,
                slot -> new Object(), null, null);
        installed.install(0, new Object(), 8);
        assertFalse(installed.retireUnborrowedProjection(0));

        var unproven = new M3ObjectResidency<Object>(1,
                M3ObjectResidency.LAZY_CREATE | M3ObjectResidency.RECREATE,
                8, 0, slot -> new Object(), null, null);
        unproven.use(0, 8, value -> 1L);
        assertFalse(unproven.retireUnborrowedProjection(0));
        assertThrows(IndexOutOfBoundsException.class,
                () -> unproven.projectionBorrowCount(1));
        assertThrows(IllegalArgumentException.class,
                () -> unproven.retainProjection(0, -1));
    }

    @Test
    void asynchronousBorrowKeepsEventOwnerUntilAck() throws Exception {
        var arena = new M3ObjectResidency<StringBuilder>(1, MODE, 8, 0,
                slot -> new StringBuilder("event"), null, null);
        long token = arena.retainProjection(0, 8);
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var failure = new AtomicReference<Throwable>();
        Thread worker = new Thread(() -> {
            try {
                started.countDown();
                if (!release.await(5, TimeUnit.SECONDS)) {
                    throw new AssertionError("worker timed out");
                }
                assertEquals(5, arena.useProjection(token, StringBuilder::length).intValue());
                arena.releaseProjection(token);
            } catch (Throwable error) {
                failure.set(error);
            }
        });
        worker.start();
        assertTrue(started.await(5, TimeUnit.SECONDS));
        assertFalse(arena.retireUnborrowedProjection(0));
        release.countDown();
        worker.join(5000);
        assertFalse(worker.isAlive());
        if (failure.get() != null) throw new AssertionError(failure.get());
        assertEquals(0, arena.projectionBorrowCount(0));
        assertTrue(arena.retireUnborrowedProjection(0));
    }
}
