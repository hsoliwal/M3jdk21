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

    @Test
    void resetReentryCannotSpendTheDestinationBudgetTwice() {
        AtomicInteger creates = new AtomicInteger();
        var arena = projections(3, MODE, 16, 0, creates);
        seedBits(arena);
        int original = arena.use(0, 4, value -> value.instance);
        AtomicInteger resets = new AtomicInteger();
        assertThrows(M3ObjectResidency.BudgetExceededException.class,
                () -> arena.rebindUnborrowedProjection(0, 1, 12, (value, slot) -> {
                    resets.incrementAndGet();
                    assertEquals(original, value.instance);
                    value.text = "tainted";
                    assertEquals(4L, arena.residentBytes());
                    assertEquals(M3ObjectResidency.State.CREATING, arena.state(0));
                    assertEquals(M3ObjectResidency.State.CREATING, arena.state(1));
                    arena.use(2, 8, other -> other.instance);
                }));
        assertEquals(1, resets.get());
        assertEquals(8L, arena.residentBytes());
        assertEquals(M3ObjectResidency.State.ABSENT, arena.state(0));
        assertEquals(M3ObjectResidency.State.ABSENT, arena.state(1));
        assertEquals(M3ObjectResidency.State.RESIDENT, arena.state(2));
        assertEquals(2, arena.use(2, 8, value -> value.instance).intValue());
        assertEquals(2, creates.get());
        assertSeedBits(arena);
        int recreated = arena.use(0, 4, value -> value.instance);
        assertNotEquals(original, recreated);
        assertEquals("fresh", arena.use(0, 4, value -> value.text));
        assertEquals(3, creates.get());
    }

    @Test
    void checkedResetFailureWrapsOriginalAndDiscardsTaintedShell() {
        assertResetFailure(new java.io.IOException("checked reset failure"));
    }

    @Test
    void runtimeResetFailurePreservesExceptionIdentityAndDiscardsTaintedShell() {
        assertResetFailure(new IllegalArgumentException("runtime reset failure"));
    }

    @Test
    void resetErrorPreservesErrorIdentityAndDiscardsTaintedShell() {
        assertResetFailure(new AssertionError("reset error"));
    }

    @Test
    void sourceBorrowAndActiveCallbackRefuseBeforeReset() {
        var arena = projections(2, MODE, 32, 0, new AtomicInteger());
        seedBits(arena);
        AtomicInteger resets = new AtomicInteger();
        long token = arena.retainProjection(0, 8);
        assertRefused(arena, 0, 1, 8, resets);
        assertEquals(1, arena.projectionBorrowCount(0));
        arena.useProjection(token, value -> {
            assertRefused(arena, 0, 1, 8, resets);
            return value.instance;
        });
        arena.releaseProjection(token);
        arena.use(0, 8, value -> {
            assertRefused(arena, 0, 1, 8, resets);
            return value.instance;
        });
        assertEquals(0, resets.get());
        assertTrue(arena.rebindUnborrowedProjection(0, 1, 8,
                (value, slot) -> resets.incrementAndGet()));
        assertEquals(1, resets.get());
        assertThrows(IllegalStateException.class, () -> arena.useProjection(token, value -> value.instance));
        assertSeedBits(arena);
    }

    @Test
    void occupiedDestinationBorrowAndActiveCallbackRefuseBeforeReset() {
        var arena = projections(2, MODE, 32, 0, new AtomicInteger());
        seedBits(arena);
        arena.use(0, 8, value -> value.instance);
        long token = arena.retainProjection(1, 8);
        AtomicInteger resets = new AtomicInteger();
        assertRefused(arena, 0, 1, 8, resets);
        arena.useProjection(token, value -> {
            assertRefused(arena, 0, 1, 8, resets);
            return value.instance;
        });
        arena.releaseProjection(token);
        arena.use(1, 8, value -> {
            assertRefused(arena, 0, 1, 8, resets);
            return value.instance;
        });
        assertEquals(0, resets.get());
        assertEquals(16L, arena.residentBytes());
        assertEquals(M3ObjectResidency.State.RESIDENT, arena.state(0));
        assertEquals(M3ObjectResidency.State.RESIDENT, arena.state(1));
        assertSeedBits(arena);
    }

    @Test
    void sourceAndDestinationPinAndCountLanesFailClosed() throws Exception {
        // Exercise defensive guards on the real owner. In particular, nonzero
        // destination lanes with ABSENT state are corruption controls, not a
        // claim that normal public calls can construct those combinations.
        for (int guardedSlot = 0; guardedSlot < 2; guardedSlot++) {
            for (String lane : new String[]{"active", "borrowers", "states"}) {
                var arena = projections(2, MODE, 32, 0, new AtomicInteger());
                seedBits(arena);
                arena.use(0, 8, value -> value.instance);
                var field = M3ObjectResidency.class.getDeclaredField(lane);
                field.setAccessible(true);
                AtomicInteger resets = new AtomicInteger();
                if (lane.equals("states")) {
                    byte[] states = (byte[]) field.get(arena);
                    byte old = states[guardedSlot];
                    try {
                        states[guardedSlot] = (byte) (old | (1 << 6));
                        assertRefused(arena, 0, 1, 8, resets);
                    } finally {
                        states[guardedSlot] = old;
                    }
                } else {
                    int[] counts = (int[]) field.get(arena);
                    int old = counts[guardedSlot];
                    try {
                        counts[guardedSlot] = 1;
                        assertRefused(arena, 0, 1, 8, resets);
                    } finally {
                        counts[guardedSlot] = old;
                    }
                }
                assertEquals(0, resets.get(), lane + " slot " + guardedSlot);
                assertEquals(8L, arena.residentBytes());
                assertEquals(M3ObjectResidency.State.RESIDENT, arena.state(0));
                assertEquals(M3ObjectResidency.State.ABSENT, arena.state(1));
                assertSeedBits(arena);
            }
        }
    }

    @Test
    void callerInstalledProjectionsAndUnprovedModesNeverInvokeReset() {
        int[] modes = {MODE & ~M3ObjectResidency.STRICT_NO_ESCAPE,
                MODE & ~M3ObjectResidency.RECREATE, MODE | M3ObjectResidency.OBSERVE_ONLY};
        for (int mode : modes) {
            var arena = projections(2, mode, 32, 0, new AtomicInteger());
            arena.use(0, 8, value -> value.instance);
            AtomicInteger resets = new AtomicInteger();
            assertRefused(arena, 0, 1, 8, resets);
            assertEquals(0, resets.get());
        }
        for (int installedSlot = 0; installedSlot < 2; installedSlot++) {
            var arena = projections(2, MODE | M3ObjectResidency.PIN_EXPOSED,
                    32, 0, new AtomicInteger());
            if (installedSlot != 0) arena.use(0, 8, value -> value.instance);
            arena.install(installedSlot, new Projection(99), 8);
            AtomicInteger resets = new AtomicInteger();
            assertRefused(arena, 0, 1, 8, resets);
            assertEquals(0, resets.get());
        }
        var noLazy = projections(2, MODE & ~M3ObjectResidency.LAZY_CREATE,
                32, 0, new AtomicInteger());
        noLazy.install(0, new Projection(99), 8);
        assertRefused(noLazy, 0, 1, 8, new AtomicInteger());
    }

    @Test
    void absentSameOccupiedAndInvalidRequestsDoNotInvokeReset() {
        var arena = projections(3, MODE, 16, 0, new AtomicInteger());
        seedBits(arena);
        AtomicInteger resets = new AtomicInteger();
        assertRefused(arena, 0, 1, 8, resets);
        arena.use(0, 8, value -> value.instance);
        M3ObjectResidency.Decision prior = arena.lastDecision();
        assertRefused(arena, 0, 0, 8, resets);
        assertRefused(arena, 0, 1, 17, resets);
        assertThrows(IndexOutOfBoundsException.class,
                () -> arena.rebindUnborrowedProjection(-1, 1, 8,
                        (value, slot) -> resets.incrementAndGet()));
        assertThrows(IndexOutOfBoundsException.class,
                () -> arena.rebindUnborrowedProjection(0, 3, 8,
                        (value, slot) -> resets.incrementAndGet()));
        assertThrows(IllegalArgumentException.class,
                () -> arena.rebindUnborrowedProjection(0, 1, -1,
                        (value, slot) -> resets.incrementAndGet()));
        assertThrows(NullPointerException.class,
                () -> arena.rebindUnborrowedProjection(0, 1, 8, null));
        assertSame(prior, arena.lastDecision());
        assertEquals(8L, arena.residentBytes());
        arena.use(1, 8, value -> value.instance);
        assertRefused(arena, 0, 1, 8, resets);
        assertEquals(0, resets.get());
        assertSeedBits(arena);
    }

    @Test
    void resetGuardsBothParticipatingSlotsButAllowsUnrelatedCalls() {
        var arena = projections(3, MODE, 32, 0, new AtomicInteger());
        seedBits(arena);
        int identity = arena.use(0, 8, System::identityHashCode);
        assertTrue(arena.rebindUnborrowedProjection(0, 1, 8, (value, slot) -> {
            assertEquals(identity, System.identityHashCode(value));
            assertEquals(M3ObjectResidency.State.CREATING, arena.state(0));
            assertEquals(M3ObjectResidency.State.CREATING, arena.state(1));
            assertEquals(8L, arena.residentBytes());
            for (int guarded : new int[]{0, 1}) {
                assertThrows(IllegalStateException.class, () -> arena.use(guarded, 8, v -> v.instance));
                assertThrows(IllegalStateException.class, () -> arena.retainProjection(guarded, 8));
                assertThrows(IllegalStateException.class, () -> arena.install(guarded, new Projection(99), 8));
            }
            arena.use(2, 8, v -> v.instance);
            value.text = "target=" + arena.readParentBits(slot);
        }));
        assertEquals(identity, arena.use(1, 8, System::identityHashCode).intValue());
        assertEquals("target=101", arena.use(1, 8, value -> value.text));
        assertEquals(16L, arena.residentBytes());
        assertSeedBits(arena);
    }

    @Test
    void successfulRebindPreservesLogicalBitsAndMakesTargetMostRecentlyUsed() {
        var arena = projections(4, MODE, 16, 0, new AtomicInteger());
        seedBits(arena);
        arena.use(0, 8, value -> value.instance);
        arena.use(1, 8, value -> value.instance);
        arena.use(0, 8, value -> value.instance);
        assertTrue(arena.rebindUnborrowedProjection(0, 2, 8, (value, slot) -> value.text = "rebound"));
        arena.use(3, 8, value -> value.instance);
        assertEquals(M3ObjectResidency.State.ABSENT, arena.state(0));
        assertEquals(M3ObjectResidency.State.ABSENT, arena.state(1));
        assertEquals(M3ObjectResidency.State.RESIDENT, arena.state(2));
        assertEquals(M3ObjectResidency.State.RESIDENT, arena.state(3));
        assertEquals("rebound", arena.use(2, 8, value -> value.text));
        assertEquals(16L, arena.residentBytes());
        assertSeedBits(arena);
    }

    @Test
    void failedRebindPreservesUnrelatedLruVictimOrdering() {
        var arena = projections(5, MODE, 24, 0, new AtomicInteger());
        seedBits(arena);
        arena.use(0, 8, value -> value.instance);
        arena.use(1, 8, value -> value.instance);
        arena.use(2, 8, value -> value.instance);
        IllegalArgumentException marker = new IllegalArgumentException("reset");
        assertSame(marker, assertThrows(IllegalArgumentException.class,
                () -> arena.rebindUnborrowedProjection(0, 4, 8, (value, slot) -> { throw marker; })));
        arena.use(3, 16, value -> value.instance);
        assertEquals(M3ObjectResidency.State.ABSENT, arena.state(0));
        assertEquals(M3ObjectResidency.State.ABSENT, arena.state(1));
        assertEquals(M3ObjectResidency.State.RESIDENT, arena.state(2));
        assertEquals(M3ObjectResidency.State.RESIDENT, arena.state(3));
        assertEquals(M3ObjectResidency.State.ABSENT, arena.state(4));
        assertEquals(24L, arena.residentBytes());
        assertSeedBits(arena);
    }

    @Test
    void maximumBudgetArithmeticRejectsOverflowAndAcceptsExactBoundary() {
        var arena = projections(3, MODE, Long.MAX_VALUE, 0, new AtomicInteger());
        seedBits(arena);
        int identity = arena.use(0, Long.MAX_VALUE - 2, System::identityHashCode);
        arena.use(2, 2, value -> value.instance);
        AtomicInteger resets = new AtomicInteger();
        assertRefused(arena, 0, 1, Long.MAX_VALUE - 1, resets);
        assertEquals(Long.MAX_VALUE, arena.residentBytes());
        assertTrue(arena.rebindUnborrowedProjection(0, 1, Long.MAX_VALUE - 2,
                (value, slot) -> resets.incrementAndGet()));
        assertEquals(1, resets.get());
        assertEquals(identity, arena.use(1, Long.MAX_VALUE - 2, System::identityHashCode).intValue());
        assertEquals(Long.MAX_VALUE, arena.residentBytes());
        assertEquals(M3ObjectResidency.State.RESIDENT, arena.state(2));
        assertSeedBits(arena);
    }

    @Test
    void zeroWeightAndMaximumWeightCanRebindWithoutArithmeticOverflow() {
        var arena = projections(3, MODE, Long.MAX_VALUE, 0, new AtomicInteger());
        int identity = arena.use(0, 0, System::identityHashCode);
        assertTrue(arena.rebindUnborrowedProjection(0, 1, Long.MAX_VALUE, (value, slot) -> { }));
        assertEquals(Long.MAX_VALUE, arena.residentBytes());
        assertTrue(arena.rebindUnborrowedProjection(1, 2, 0, (value, slot) -> { }));
        assertEquals(0L, arena.residentBytes());
        assertEquals(identity, arena.use(2, 0, System::identityHashCode).intValue());
    }

    @Test
    void pressureAwareReserveStillControlsAdmissionAndRebind() {
        var pressure = projections(2, MODE | M3ObjectResidency.PRESSURE_AWARE,
                Long.MAX_VALUE, Long.MAX_VALUE, new AtomicInteger());
        pressure.use(0, 0, value -> value.instance);
        AtomicInteger resets = new AtomicInteger();
        assertRefused(pressure, 0, 1, 1, resets);
        long token = pressure.retainProjection(0, 0);
        try {
            assertThrows(M3ObjectResidency.BudgetExceededException.class,
                    () -> pressure.use(1, 1, value -> value.instance));
        } finally {
            pressure.releaseProjection(token);
        }
        assertEquals(0, resets.get());
        assertTrue(pressure.rebindUnborrowedProjection(0, 1, 0,
                (value, slot) -> resets.incrementAndGet()));
        pressure.signalMemoryPressure();
        assertEquals(1, resets.get());
        assertEquals(0L, pressure.residentBytes());
        assertEquals(M3ObjectResidency.State.RESIDENT, pressure.state(1));

        var fixed = projections(2, MODE, Long.MAX_VALUE, Long.MAX_VALUE, new AtomicInteger());
        fixed.use(0, 1, value -> value.instance);
        assertTrue(fixed.rebindUnborrowedProjection(0, 1, 1, (value, slot) -> { }));
        fixed.signalMemoryPressure();
        assertEquals(1L, fixed.residentBytes());
    }

    private static final class Projection {
        final int instance;
        String text = "fresh";
        Projection(int instance) { this.instance = instance; }
    }

    private static M3ObjectResidency<Projection> projections(int slots, int flags,
            long budget, long reserve, AtomicInteger creates) {
        return new M3ObjectResidency<>(slots, flags, budget, reserve,
                slot -> new Projection(creates.incrementAndGet()), null, null);
    }

    private static <T> void assertRefused(M3ObjectResidency<T> arena,
            int from, int to, long bytes, AtomicInteger resets) {
        int before = resets.get();
        assertFalse(arena.rebindUnborrowedProjection(from, to, bytes,
                (value, slot) -> resets.incrementAndGet()));
        assertEquals(before, resets.get());
    }

    private static void seedBits(M3ObjectResidency<?> arena) {
        for (int slot = 0; slot < arena.slots(); slot++) arena.writeParentBits(slot, 100L + slot);
    }

    private static void assertSeedBits(M3ObjectResidency<?> arena) {
        for (int slot = 0; slot < arena.slots(); slot++) assertEquals(100L + slot, arena.readParentBits(slot));
    }

    private static void assertResetFailure(Throwable marker) {
        AtomicInteger creates = new AtomicInteger();
        var arena = projections(3, MODE, 64, 0, creates);
        seedBits(arena);
        int original = arena.use(0, 8, value -> value.instance);
        int unrelated = arena.use(2, 4, value -> value.instance);
        Throwable observed = assertThrows(Throwable.class,
                () -> arena.rebindUnborrowedProjection(0, 1, 16, (value, slot) -> {
                    assertEquals(original, value.instance);
                    value.text = "tainted";
                    if (marker instanceof Exception exception) throw exception;
                    throw (Error) marker;
                }));
        if (marker instanceof RuntimeException || marker instanceof Error) assertSame(marker, observed);
        else {
            assertInstanceOf(M3ObjectResidency.ResidencyException.class, observed);
            assertSame(marker, observed.getCause());
        }
        assertEquals(M3ObjectResidency.State.ABSENT, arena.state(0));
        assertEquals(M3ObjectResidency.State.ABSENT, arena.state(1));
        assertEquals(4L, arena.residentBytes());
        assertEquals(0, arena.projectionBorrowCount(0));
        assertEquals(0, arena.projectionBorrowCount(1));
        assertEquals(unrelated, arena.use(2, 4, value -> value.instance).intValue());
        assertSeedBits(arena);
        assertNotEquals(original, arena.use(0, 8, value -> value.instance).intValue());
        assertEquals("fresh", arena.use(0, 8, value -> value.text));
        assertEquals(3, creates.get());
    }
}
