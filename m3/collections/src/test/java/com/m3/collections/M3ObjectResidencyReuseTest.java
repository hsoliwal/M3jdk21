// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Physical shell handoff before GC eligibility, not JVM object resurrection. */
final class M3ObjectResidencyReuseTest {
    private static final int MODE = M3ObjectResidency.LAZY_CREATE
            | M3ObjectResidency.LRU | M3ObjectResidency.RECREATE
            | M3ObjectResidency.STRICT_NO_ESCAPE;

    @Test
    void handoffPreservesIdentityAndLogicalBitsWithoutNewFactoryObject() {
        AtomicInteger creates = new AtomicInteger();
        var arena = new M3ObjectResidency<StringBuilder>(2, MODE, 64, 0,
                slot -> { creates.incrementAndGet(); return new StringBuilder("old"); },
                null, null);
        arena.writeParentBits(0, 0x55L);
        arena.writeParentBits(1, 0x66L);
        int originalId = arena.use(0, 8, System::identityHashCode);
        assertTrue(arena.rebindUnborrowedProjection(0, 1, 16, (value, slot) -> {
            value.setLength(0);
            value.append("slot-").append(slot).append(':').append(arena.readParentBits(slot));
            assertThrows(IllegalStateException.class, () -> arena.use(0, 8, v -> 1));
            assertThrows(IllegalStateException.class, () -> arena.use(1, 16, v -> 1));
        }));
        assertEquals(originalId, arena.use(1, 16, System::identityHashCode).intValue());
        assertEquals("slot-1:102", arena.use(1, 16, StringBuilder::toString));
        assertEquals(1, creates.get());
        assertEquals(M3ObjectResidency.State.ABSENT, arena.state(0));
        assertEquals(M3ObjectResidency.State.RESIDENT, arena.state(1));
        assertEquals(16L, arena.residentBytes());
        assertEquals(0x55L, arena.readParentBits(0));
        assertEquals(0x66L, arena.readParentBits(1));
    }

    @Test
    void activeBorrowAndInstalledAliasRefuseHandoff() {
        var arena = new M3ObjectResidency<StringBuilder>(2, MODE, 32, 0,
                slot -> new StringBuilder("original"), null, null);
        long token = arena.retainProjection(0, 8);
        assertFalse(arena.rebindUnborrowedProjection(0, 1, 8, (v, s) -> v.setLength(0)));
        arena.releaseProjection(token);
        assertEquals(8, arena.use(0, 8, value -> {
            assertFalse(arena.rebindUnborrowedProjection(0, 1, 8, (v, s) -> v.setLength(0)));
            return value.length();
        }).intValue());
        var installed = new M3ObjectResidency<StringBuilder>(2, MODE, 32, 0,
                slot -> new StringBuilder(), null, null);
        installed.install(0, new StringBuilder("external"), 8);
        assertFalse(installed.rebindUnborrowedProjection(0, 1, 8, (v, s) -> v.setLength(0)));
        assertFalse(arena.rebindUnborrowedProjection(0, 0, 8, (v, s) -> v.setLength(0)));
    }

    @Test
    void refusalDoesNotMutateAndFailedResetDiscardsShell() {
        AtomicInteger creates = new AtomicInteger();
        var arena = new M3ObjectResidency<StringBuilder>(2, MODE, 8, 0,
                slot -> { creates.incrementAndGet(); return new StringBuilder("before"); },
                null, null);
        assertEquals("before", arena.use(0, 8, StringBuilder::toString));
        assertFalse(arena.rebindUnborrowedProjection(0, 1, 9, (v, s) -> v.setLength(0)));
        assertEquals(M3ObjectResidency.State.RESIDENT, arena.state(0));
        assertEquals(8L, arena.residentBytes());
        assertThrows(IllegalArgumentException.class,
                () -> arena.rebindUnborrowedProjection(0, 1, 8, (v, s) -> {
                    v.setLength(0);
                    throw new IllegalArgumentException("reset failed");
                }));
        assertEquals(M3ObjectResidency.State.ABSENT, arena.state(0));
        assertEquals(M3ObjectResidency.State.ABSENT, arena.state(1));
        assertEquals(0L, arena.residentBytes());
        assertEquals("before", arena.use(0, 8, StringBuilder::toString));
        assertEquals(2, creates.get());
    }

    @Test
    void zeroBorrowCountDoesNotProveNoEscape() {
        var arena = new M3ObjectResidency<StringBuilder>(2,
                M3ObjectResidency.LAZY_CREATE | M3ObjectResidency.LRU
                        | M3ObjectResidency.RECREATE,
                32, 0, slot -> new StringBuilder("still here"), null, null);
        assertEquals(10, arena.use(0, 8, StringBuilder::length).intValue());
        assertFalse(arena.rebindUnborrowedProjection(0, 1, 8, (v, s) -> v.setLength(0)));
        assertEquals(M3ObjectResidency.State.RESIDENT, arena.state(0));
    }
}
