// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

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

    // SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
    // SPDX-License-Identifier: Apache-2.0
    // Admission callbacks must not select a transitioning victim or publish the pending request.
    @Test
    void linkedVictimsExcludeAnInProgressSwap() throws Exception {
        assertNestedSwapAdmissionRefused(false);
    }

    @Test
    void rolloverScanVictimsExcludeAnInProgressSwap() throws Exception {
        assertNestedSwapAdmissionRefused(true);
    }

    @Test
    void admissionClaimsRequestedSlotBeforeCodecOrStoreCanPublishItAgain() throws Exception {
        for (boolean duringWrite : new boolean[] {false, true}) {
            SwapFixture fixture = new SwapFixture(3, 16, 0, false);
            fixture.use(0);
            fixture.use(1);
            AtomicInteger refused = new AtomicInteger();
            fixture.hook(duringWrite, slot -> {
                assertEquals(0, slot);
                assertEquals(M3ObjectResidency.State.CREATING, fixture.arena.state(2));
                assertThrows(IllegalStateException.class, () -> fixture.use(2));
                refused.incrementAndGet();
            });
            assertTimeoutPreemptively(java.time.Duration.ofSeconds(2), () -> fixture.use(2));
            assertEquals(1, refused.get());
            assertEquals(1, fixture.creates[2]);
            assertEquals(16L, fixture.arena.residentBytes());
            assertEquals(M3ObjectResidency.State.SWAPPED, fixture.arena.state(0));
            assertEquals(M3ObjectResidency.State.RESIDENT, fixture.arena.state(1));
            assertEquals(M3ObjectResidency.State.RESIDENT, fixture.arena.state(2));
            fixture.assertBits();
        }
    }

    @Test
    void admissionClaimsInstallTargetAgainstInstallAndBorrowReentry() throws Exception {
        SwapFixture fixture = new SwapFixture(3, 16, 0, false);
        fixture.use(0);
        fixture.use(1);
        String installed = new String("c-external");
        AtomicInteger refused = new AtomicInteger();
        fixture.onEncode = slot -> {
            assertEquals(M3ObjectResidency.State.CREATING, fixture.arena.state(2));
            assertThrows(IllegalStateException.class,
                    () -> fixture.arena.install(2, "c-unreviewed", 8));
            assertThrows(IllegalStateException.class,
                    () -> fixture.arena.retainProjection(2, 8));
            assertThrows(IllegalStateException.class, () -> fixture.use(2));
            refused.incrementAndGet();
        };
        assertTimeoutPreemptively(java.time.Duration.ofSeconds(2),
                () -> fixture.arena.install(2, installed, 8));
        assertEquals(1, refused.get());
        assertSame(installed, fixture.arena.use(2, 8, value -> value));
        assertEquals(0, fixture.creates[2]);
        assertEquals(0, fixture.arena.projectionBorrowCount(2));
        assertEquals(16L, fixture.arena.residentBytes());
        fixture.assertBits();
    }

    @Test
    void restoreAdmissionClaimsSwappedRequestAndPreservesStoreCallbackOrder() throws Exception {
        SwapFixture fixture = new SwapFixture(3, 8, 0, false);
        fixture.use(0);
        fixture.use(1);
        fixture.events.clear();
        fixture.onEncode = slot -> {
            assertEquals(1, slot);
            assertEquals(M3ObjectResidency.State.RESTORING, fixture.arena.state(0));
            assertThrows(IllegalStateException.class, () -> fixture.use(0));
            assertThrows(M3ObjectResidency.BudgetExceededException.class, () -> fixture.use(2));
            assertEquals(M3ObjectResidency.State.ABSENT, fixture.arena.state(2));
        };
        assertTimeoutPreemptively(java.time.Duration.ofSeconds(2), () -> fixture.use(0));
        assertEquals(java.util.List.of("encode 1", "write 1", "read 0", "decode 0", "delete 0"),
                fixture.events);
        assertEquals(1, fixture.creates[0]);
        assertEquals(8L, fixture.arena.residentBytes());
        assertEquals(M3ObjectResidency.State.RESIDENT, fixture.arena.state(0));
        assertEquals(M3ObjectResidency.State.SWAPPED, fixture.arena.state(1));
        assertEquals(M3ObjectResidency.State.ABSENT, fixture.arena.state(2));
        assertFalse(fixture.secondary.containsKey(0));
        assertTrue(fixture.secondary.containsKey(1));
        fixture.assertBits();
    }

    @Test
    void unrelatedNestedRequestCanUseTheNextEligibleVictimAndKeepsLruOrder() throws Exception {
        for (boolean rollover : new boolean[] {false, true}) {
            SwapFixture fixture = new SwapFixture(4, 16, 0, rollover);
            fixture.use(0);
            fixture.use(1);
            fixture.events.clear();
            fixture.onEncode = slot -> {
                fixture.onEncode = null;
                assertEquals(0, slot);
                fixture.use(3);
            };
            assertTimeoutPreemptively(java.time.Duration.ofSeconds(2), () -> fixture.use(2));
            assertEquals(java.util.List.of("encode 0", "encode 1", "write 1", "create 3",
                    "write 0", "create 2"), fixture.events);
            assertEquals(16L, fixture.arena.residentBytes());
            assertEquals(M3ObjectResidency.State.SWAPPED, fixture.arena.state(0));
            assertEquals(M3ObjectResidency.State.SWAPPED, fixture.arena.state(1));
            assertEquals(M3ObjectResidency.State.RESIDENT, fixture.arena.state(2));
            assertEquals(M3ObjectResidency.State.RESIDENT, fixture.arena.state(3));
            fixture.use(0);
            assertEquals(M3ObjectResidency.State.SWAPPED, fixture.arena.state(3));
            assertEquals(M3ObjectResidency.State.RESIDENT, fixture.arena.state(2));
            fixture.assertBits();
        }
    }

    @Test
    void admissionRestoresAbsentRequestAfterIoOrRuntimeFailureAndAllowsRetry() throws Exception {
        for (boolean duringWrite : new boolean[] {false, true}) {
            for (boolean checked : new boolean[] {false, true}) {
                SwapFixture fixture = new SwapFixture(2, 8, 0, false);
                fixture.use(0);
                Throwable marker = checked ? new java.io.IOException("swap I/O marker")
                        : new IllegalArgumentException("swap runtime marker");
                fixture.hook(duringWrite, slot -> {
                    if (marker instanceof java.io.IOException io) throw io;
                    throw (RuntimeException) marker;
                });
                M3ObjectResidency.Decision previous = fixture.arena.lastDecision();
                M3ObjectResidency.ResidencyException observed = assertThrows(
                        M3ObjectResidency.ResidencyException.class, () -> fixture.use(1));
                assertSame(marker, observed.getCause());
                assertSame(previous, fixture.arena.lastDecision());
                assertEquals(M3ObjectResidency.State.ABSENT, fixture.arena.state(1));
                assertEquals(M3ObjectResidency.State.RESIDENT, fixture.arena.state(0));
                assertEquals(8L, fixture.arena.residentBytes());
                assertEquals(0, fixture.creates[1]);
                fixture.onEncode = null;
                fixture.onWrite = null;
                fixture.use(1);
                assertEquals(1, fixture.creates[1]);
                assertEquals(8L, fixture.arena.residentBytes());
                fixture.assertBits();
            }
        }
    }

    @Test
    void admissionRestoresCreateInstallAndRestoreRequestOnErrorWithoutReplacingIt() throws Exception {
        for (boolean duringWrite : new boolean[] {false, true}) {
            for (int operation = 0; operation < 3; operation++) {
                SwapFixture fixture = new SwapFixture(2, 8, 0, false);
                fixture.use(0);
                if (operation == 2) fixture.use(1);
                int requested = operation == 2 ? 0 : 1;
                M3ObjectResidency.State previous = fixture.arena.state(requested);
                int creates = fixture.creates[requested];
                AssertionError marker = new AssertionError("admission callback marker");
                fixture.hook(duringWrite, slot -> { throw marker; });
                int selectedOperation = operation;
                AssertionError observed = assertThrows(AssertionError.class, () -> {
                    if (selectedOperation == 1) fixture.arena.install(requested, "b-external", 8);
                    else fixture.use(requested);
                });
                assertSame(marker, observed);
                assertEquals(previous, fixture.arena.state(requested));
                assertEquals(creates, fixture.creates[requested]);
                assertEquals(8L, fixture.arena.residentBytes());
                assertEquals(0, fixture.arena.projectionBorrowCount(requested));
                if (operation == 2) assertTrue(fixture.secondary.containsKey(0));
                fixture.assertBits();
                // Failed victim cleanup is a separate lifecycle contract. This test
                // constrains only rollback of the admission claim owned by this atom.
            }
        }
    }

    @Test
    void swappingVictimExclusionPreservesBorrowActiveAndPermanentPinGuards() throws Exception {
        for (int protection = 0; protection < 3; protection++) {
            SwapFixture fixture = new SwapFixture(4, 16,
                    protection == 2 ? M3ObjectResidency.PIN_EXPOSED : 0, false);
            fixture.use(0);
            fixture.use(1);
            long token = protection == 0 ? fixture.arena.retainProjection(1, 8) : 0L;
            if (protection == 2) fixture.arena.use(1, 8, value -> value);
            AtomicInteger refusals = new AtomicInteger();
            fixture.onEncode = slot -> {
                assertEquals(0, slot);
                assertThrows(M3ObjectResidency.BudgetExceededException.class, () -> fixture.use(3));
                assertEquals(M3ObjectResidency.State.ABSENT, fixture.arena.state(3));
                refusals.incrementAndGet();
            };
            int selectedProtection = protection;
            assertTimeoutPreemptively(java.time.Duration.ofSeconds(2), () -> {
                if (selectedProtection == 1)
                    fixture.arena.use(1, 8, value -> { fixture.use(2); return value.length(); });
                else fixture.use(2);
            });
            assertEquals(1, refusals.get());
            assertEquals(16L, fixture.arena.residentBytes());
            assertEquals(M3ObjectResidency.State.RESIDENT, fixture.arena.state(1));
            assertEquals(M3ObjectResidency.State.RESIDENT, fixture.arena.state(2));
            assertEquals(0, fixture.creates[3]);
            if (protection == 0) {
                assertEquals(1, fixture.arena.projectionBorrowCount(1));
                assertEquals(3, fixture.arena.useProjection(token, String::length).intValue());
                fixture.arena.releaseProjection(token);
            }
            fixture.assertBits();
        }
    }

    @Test
    void noVictimAdmissionRestoresTheClaimWithoutFactoryOrDecisionEffects() throws Exception {
        SwapFixture fixture = new SwapFixture(2, Long.MAX_VALUE, 0, false);
        fixture.arena.use(0, Long.MAX_VALUE, String::length);
        long token = fixture.arena.retainProjection(0, Long.MAX_VALUE);
        M3ObjectResidency.Decision decision = fixture.arena.lastDecision();
        assertThrows(M3ObjectResidency.BudgetExceededException.class,
                () -> fixture.arena.use(1, 1, String::length));
        assertEquals(M3ObjectResidency.State.ABSENT, fixture.arena.state(1));
        assertEquals(0, fixture.creates[1]);
        assertSame(decision, fixture.arena.lastDecision());
        assertEquals(Long.MAX_VALUE, fixture.arena.residentBytes());
        fixture.arena.releaseProjection(token);
        fixture.assertBits();
    }

    private static void assertNestedSwapAdmissionRefused(boolean rollover) throws Exception {
        for (boolean duringWrite : new boolean[] {false, true}) {
            SwapFixture fixture = new SwapFixture(3, 8, 0, rollover);
            fixture.use(0);
            AtomicInteger refused = new AtomicInteger();
            fixture.hook(duringWrite, slot -> {
                assertEquals(M3ObjectResidency.State.SWAPPING, fixture.arena.state(0));
                assertEquals(M3ObjectResidency.State.CREATING, fixture.arena.state(1));
                assertThrows(M3ObjectResidency.BudgetExceededException.class, () -> fixture.use(2));
                assertEquals(M3ObjectResidency.State.ABSENT, fixture.arena.state(2));
                refused.incrementAndGet();
            });
            assertTimeoutPreemptively(java.time.Duration.ofSeconds(2), () -> fixture.use(1));
            assertEquals(1, refused.get());
            assertEquals(8L, fixture.arena.residentBytes());
            assertEquals(M3ObjectResidency.State.SWAPPED, fixture.arena.state(0));
            assertEquals(M3ObjectResidency.State.RESIDENT, fixture.arena.state(1));
            assertEquals(M3ObjectResidency.State.ABSENT, fixture.arena.state(2));
            assertEquals(0, fixture.creates[2]);
            fixture.assertBits();
        }
    }

    @FunctionalInterface
    private interface SwapHook {
        void run(int slot) throws java.io.IOException;
    }

    private static final class SwapFixture {
        final int[] creates;
        final java.util.Map<Integer, byte[]> secondary = new java.util.HashMap<>();
        final java.util.List<String> events = new java.util.ArrayList<>();
        final M3ObjectResidency<String> arena;
        SwapHook onEncode;
        SwapHook onWrite;

        SwapFixture(int slots, long budget, int extraFlags, boolean rollover) throws Exception {
            creates = new int[slots];
            var codec = new M3ObjectResidency.Codec<String>() {
                @Override public byte[] encode(String value) throws java.io.IOException {
                    int slot = value.charAt(0) - 'a';
                    events.add("encode " + slot);
                    if (onEncode != null) onEncode.run(slot);
                    return value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                }
                @Override public String decode(int slot, byte[] bytes) {
                    events.add("decode " + slot);
                    return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                }
            };
            var store = new M3ObjectResidency.SpillStore() {
                @Override public void write(int slot, byte[] bytes) throws java.io.IOException {
                    events.add("write " + slot);
                    if (onWrite != null) onWrite.run(slot);
                    secondary.put(slot, bytes.clone());
                }
                @Override public byte[] read(int slot) throws java.io.IOException {
                    events.add("read " + slot);
                    byte[] bytes = secondary.get(slot);
                    if (bytes == null) throw new java.io.IOException("missing slot " + slot);
                    return bytes.clone();
                }
                @Override public void delete(int slot) {
                    events.add("delete " + slot);
                    secondary.remove(slot);
                }
            };
            arena = new M3ObjectResidency<>(slots, M3ObjectResidency.LAZY_CREATE
                    | M3ObjectResidency.LRU | M3ObjectResidency.SWAP_SECONDARY | extraFlags,
                    budget, 0, slot -> {
                        events.add("create " + slot);
                        return String.valueOf((char) ('a' + slot)) + "-" + ++creates[slot];
                    }, codec, store);
            for (int slot = 0; slot < slots; slot++) arena.writeParentBits(slot, 0x100L + slot);
            if (rollover) {
                var tick = M3ObjectResidency.class.getDeclaredField("tick");
                tick.setAccessible(true);
                tick.setLong(arena, Long.MAX_VALUE);
            }
        }

        void use(int slot) { arena.use(slot, 8, String::length); }

        void hook(boolean duringWrite, SwapHook hook) {
            if (duringWrite) onWrite = hook;
            else onEncode = hook;
        }

        void assertBits() {
            for (int slot = 0; slot < creates.length; slot++)
                assertEquals(0x100L + slot, arena.readParentBits(slot));
        }
    }

    @Test
    void factoryErrorRestoresAbsentStateAndOriginalThrowableBeforeRetry() {
        for (Error marker : new Error[] {new AssertionError("factory assertion"),
                new LinkageError("factory linkage"), new OutOfMemoryError("synthetic factory marker")}) {
            AtomicInteger calls = new AtomicInteger();
            var arena = new M3ObjectResidency<StringBuilder>(2, MODE, 16, 0, slot -> {
                if (calls.getAndIncrement() == 0) throw marker;
                return new StringBuilder("projection");
            }, null, null);
            arena.writeParentBits(0, 17L);
            assertSame(marker, assertThrows(Error.class, () -> arena.use(0, 8, StringBuilder::length)));
            assertEquals(M3ObjectResidency.State.ABSENT, arena.state(0));
            assertEquals(0L, arena.residentBytes());
            assertNull(arena.lastDecision());
            assertEquals(17L, arena.readParentBits(0));
            int identity = arena.use(0, 8, System::identityHashCode);
            assertEquals(2, calls.get());
            assertTrue(arena.rebindUnborrowedProjection(0, 1, 8, (value, slot) -> {
                value.setLength(0);
                value.append("projection");
            }));
            assertEquals(identity, arena.use(1, 8, System::identityHashCode).intValue());
            assertEquals(2, calls.get());
            assertEquals(17L, arena.readParentBits(0));
        }
    }

    @Test
    void readErrorRestoresSwappedStateAndRetainsSecondaryBytesForRetry() {
        assertRestoreErrorRetainsBytes("read");
    }

    @Test
    void decodeErrorRestoresSwappedStateAndRetainsSecondaryBytesForRetry() {
        assertRestoreErrorRetainsBytes("decode");
    }

    @Test
    void deleteErrorBeforeMutationRestoresSwappedStateAndAllowsRetry() {
        assertRestoreErrorRetainsBytes("delete");
    }

    @Test
    void encodeAndWriteErrorsRetainResidentIdentityAndOriginalThrowable() throws Exception {
        for (boolean duringWrite : new boolean[] {false, true}) {
            SwapFixture fixture = new SwapFixture(2, 8, 0, false);
            fixture.use(0);
            String original = fixture.arena.use(0, 8, value -> value);
            AssertionError marker = new AssertionError("swap error marker");
            fixture.hook(duringWrite, slot -> { throw marker; });
            M3ObjectResidency.Decision decision = fixture.arena.lastDecision();
            assertSame(marker, assertThrows(AssertionError.class, () -> fixture.use(1)));
            assertEquals(M3ObjectResidency.State.RESIDENT, fixture.arena.state(0));
            assertEquals(M3ObjectResidency.State.ABSENT, fixture.arena.state(1));
            assertEquals(8L, fixture.arena.residentBytes());
            assertSame(decision, fixture.arena.lastDecision());
            assertSame(original, fixture.arena.use(0, 8, value -> value));
            assertEquals(0, fixture.creates[1]);
            fixture.onEncode = null;
            fixture.onWrite = null;
            fixture.use(1);
            assertEquals(M3ObjectResidency.State.SWAPPED, fixture.arena.state(0));
            fixture.use(0);
            assertEquals(original, fixture.arena.use(0, 8, value -> value));
            assertEquals(1, fixture.creates[0]);
            fixture.assertBits();
        }
    }

    @Test
    void swapWriteErrorAfterStoreMutationStillRetainsResidentValue() {
        String original = new String("owned");
        byte[][] secondary = new byte[2][];
        AtomicInteger writes = new AtomicInteger();
        AssertionError marker = new AssertionError("write completed then failed");
        var codec = new M3ObjectResidency.Codec<String>() {
            @Override public byte[] encode(String value) {
                return value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            }
            @Override public String decode(int slot, byte[] bytes) {
                return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            }
        };
        var store = new M3ObjectResidency.SpillStore() {
            @Override public void write(int slot, byte[] bytes) {
                secondary[slot] = bytes.clone();
                if (writes.getAndIncrement() == 0) throw marker;
            }
            @Override public byte[] read(int slot) { return secondary[slot].clone(); }
            @Override public void delete(int slot) { secondary[slot] = null; }
        };
        var arena = new M3ObjectResidency<String>(2, M3ObjectResidency.LAZY_CREATE
                | M3ObjectResidency.LRU | M3ObjectResidency.SWAP_SECONDARY,
                8, 0, slot -> slot == 0 ? original : "other", codec, store);
        arena.use(0, 8, String::length);
        assertSame(marker, assertThrows(AssertionError.class, () -> arena.use(1, 8, String::length)));
        assertEquals(M3ObjectResidency.State.RESIDENT, arena.state(0));
        assertSame(original, arena.use(0, 8, value -> value));
        assertEquals(8L, arena.residentBytes());
        assertNotNull(secondary[0]);
        arena.use(1, 8, String::length);
        assertEquals(2, writes.get());
        assertEquals(M3ObjectResidency.State.SWAPPED, arena.state(0));
        assertEquals(original, arena.use(0, 8, value -> value));
    }

    private static void assertRestoreErrorRetainsBytes(String phase) {
        LifecycleFixture fixture = new LifecycleFixture(phase);
        fixture.arena.use(0, 8, String::length);
        fixture.arena.use(1, 8, String::length);
        byte[] retained = fixture.secondary[0].clone();
        fixture.armed = true;
        assertSame(fixture.marker, assertThrows(AssertionError.class,
                () -> fixture.arena.use(0, 8, String::length)));
        assertEquals(M3ObjectResidency.State.SWAPPED, fixture.arena.state(0));
        assertEquals(M3ObjectResidency.State.SWAPPED, fixture.arena.state(1));
        assertEquals(0L, fixture.arena.residentBytes());
        assertArrayEquals(retained, fixture.secondary[0]);
        assertNotNull(fixture.secondary[1]);
        assertEquals(1, fixture.creates[0]);
        assertEquals(1, fixture.creates[1]);
        assertEquals("a-original", fixture.arena.use(0, 8, value -> value));
        assertNull(fixture.secondary[0]);
        assertEquals(8L, fixture.arena.residentBytes());
        assertEquals(1, fixture.creates[0]);
        assertEquals(41L, fixture.arena.readParentBits(0));
        assertEquals(42L, fixture.arena.readParentBits(1));
    }

    private static final class LifecycleFixture {
        final String phase;
        final AssertionError marker = new AssertionError("callback failure");
        final byte[][] secondary = new byte[2][];
        final int[] creates = new int[2];
        final M3ObjectResidency<String> arena;
        boolean armed;

        LifecycleFixture(String phase) {
            this.phase = phase;
            var codec = new M3ObjectResidency.Codec<String>() {
                @Override public byte[] encode(String value) {
                    checkpoint("encode");
                    return value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                }
                @Override public String decode(int slot, byte[] bytes) {
                    checkpoint("decode");
                    return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                }
            };
            var store = new M3ObjectResidency.SpillStore() {
                @Override public void write(int slot, byte[] bytes) {
                    checkpoint("write");
                    secondary[slot] = bytes.clone();
                }
                @Override public byte[] read(int slot) {
                    checkpoint("read");
                    return secondary[slot].clone();
                }
                @Override public void delete(int slot) {
                    checkpoint("delete");
                    secondary[slot] = null;
                }
            };
            arena = new M3ObjectResidency<>(2, M3ObjectResidency.LAZY_CREATE
                    | M3ObjectResidency.LRU | M3ObjectResidency.SWAP_SECONDARY,
                    8, 0, slot -> {
                        creates[slot]++;
                        checkpoint("factory");
                        return slot == 0 ? "a-original" : "b-original";
                    }, codec, store);
            arena.writeParentBits(0, 41L);
            arena.writeParentBits(1, 42L);
        }

        private void checkpoint(String observedPhase) {
            if (armed && phase.equals(observedPhase)) {
                armed = false;
                throw marker;
            }
        }
    }

    @Test
    void factoryReentryRefusesPublicationWithoutEvictingTheNestedProjection() {
        for (int extraFlags : new int[] {0, M3ObjectResidency.PRESSURE_AWARE}) {
            PublicationFixture fixture = new PublicationFixture("factory", 8, extraFlags);
            fixture.armed = true;
            AtomicInteger operations = new AtomicInteger();
            var failure = assertThrows(M3ObjectResidency.BudgetExceededException.class,
                    () -> fixture.arena.use(0, 8, value -> operations.incrementAndGet()));
            assertEquals(8L, failure.requestedBytes);
            assertEquals(8L, failure.residentBytes);
            assertEquals(8L, failure.targetBytes);
            assertEquals(0, operations.get());
            assertEquals(M3ObjectResidency.State.ABSENT, fixture.arena.state(0));
            assertEquals(M3ObjectResidency.State.RESIDENT, fixture.arena.state(2));
            assertEquals(8L, fixture.arena.residentBytes());
            assertEquals(java.util.List.of("create 0", "create 2"), fixture.events);
            assertEquals(1, fixture.creates[0]);
            assertEquals(1, fixture.creates[2]);
            assertSame(fixture.nestedDecision, fixture.arena.lastDecision());
            assertEquals("slot-2-1", fixture.arena.use(2, 8, value -> value));
            fixture.assertBits();
            assertEquals("slot-0-2", fixture.arena.use(0, 8, value -> value));
            assertEquals(8L, fixture.arena.residentBytes());
            assertEquals(M3ObjectResidency.State.SWAPPED, fixture.arena.state(2));
            fixture.assertBits();
        }
    }

    @Test
    void decodeReentryRefusesBeforeDeletingTheSnapshotAndAllowsRetry() {
        assertRestorePublicationRefused("decode");
    }

    @Test
    void readReentryStillDecodesBeforeThePublicationCheck() {
        assertRestorePublicationRefused("read");
    }

    @Test
    void withinBudgetFactoryReentryPreservesOrderAndTheNextLruVictim() {
        PublicationFixture fixture = new PublicationFixture("factory", 16, 0);
        fixture.armed = true;
        assertEquals("slot-0-1", fixture.arena.use(0, 8, value -> value));
        assertEquals(java.util.List.of("create 0", "create 2"), fixture.events);
        assertEquals(16L, fixture.arena.residentBytes());
        assertEquals(M3ObjectResidency.State.RESIDENT, fixture.arena.state(0));
        assertEquals(M3ObjectResidency.State.RESIDENT, fixture.arena.state(2));
        fixture.events.clear();
        fixture.arena.use(1, 8, String::length);
        assertEquals(java.util.List.of("encode slot-2-1", "write 2", "create 1"), fixture.events);
        assertEquals(M3ObjectResidency.State.RESIDENT, fixture.arena.state(0));
        assertEquals(M3ObjectResidency.State.RESIDENT, fixture.arena.state(1));
        assertEquals(M3ObjectResidency.State.SWAPPED, fixture.arena.state(2));
        assertEquals(16L, fixture.arena.residentBytes());
        fixture.assertBits();
    }

    @Test
    void publicationAcceptsZeroAndExactLongMaxBudgetBoundaries() {
        long[][] cases = {{0L, 0L, 0L}, {Long.MAX_VALUE, Long.MAX_VALUE - 8L, 8L}};
        for (long[] values : cases) {
            PublicationFixture fixture = new PublicationFixture("factory", values[0], 0);
            fixture.nestedBytes = values[1];
            fixture.armed = true;
            assertEquals("slot-0-1", fixture.arena.use(0, values[2], value -> value));
            assertEquals(values[0], fixture.arena.residentBytes());
            assertEquals(M3ObjectResidency.State.RESIDENT, fixture.arena.state(0));
            assertEquals(M3ObjectResidency.State.RESIDENT, fixture.arena.state(2));
            assertEquals(java.util.List.of("create 0", "create 2"), fixture.events);
            assertEquals(values[0], fixture.arena.lastDecision().residentBytes());
            fixture.assertBits();
        }
    }

    @Test
    void observeOnlyStillAllowsFactoryReentryToExceedTheBudget() {
        PublicationFixture fixture = new PublicationFixture("factory", 8, M3ObjectResidency.OBSERVE_ONLY);
        fixture.armed = true;
        assertEquals("slot-0-1", fixture.arena.use(0, 8, value -> value));
        assertEquals(16L, fixture.arena.residentBytes());
        assertEquals(M3ObjectResidency.State.RESIDENT, fixture.arena.state(0));
        assertEquals(M3ObjectResidency.State.RESIDENT, fixture.arena.state(2));
        assertEquals(java.util.List.of("create 0", "create 2"), fixture.events);
        fixture.assertBits();
    }

    @Test
    void observeOnlyOverflowKeepsCallbackDemandAndArithmeticFailureOrdering() {
        PublicationFixture fixture = new PublicationFixture("factory", Long.MAX_VALUE,
                M3ObjectResidency.OBSERVE_ONLY);
        fixture.nestedBytes = Long.MAX_VALUE;
        fixture.armed = true;
        AtomicInteger operations = new AtomicInteger();
        assertThrows(ArithmeticException.class,
                () -> fixture.arena.use(0, 1, value -> operations.incrementAndGet()));
        assertEquals(java.util.List.of("create 0", "create 2"), fixture.events);
        assertEquals(0, operations.get());
        assertEquals(Long.MAX_VALUE, fixture.arena.residentBytes());
        assertEquals(M3ObjectResidency.State.ABSENT, fixture.arena.state(0));
        assertEquals(M3ObjectResidency.State.RESIDENT, fixture.arena.state(2));
        assertSame(fixture.nestedDecision, fixture.arena.lastDecision());
        fixture.assertBits();
    }

    @Test
    void factoryAndDecodeNullResultsStillFailBeforeTheBudgetCheck() {
        for (String phase : new String[] {"factory", "decode"}) {
            PublicationFixture fixture = new PublicationFixture(phase, 8, 0);
            if (phase.equals("decode")) fixture.prepareSwapped();
            fixture.nullResult = true;
            fixture.armed = true;
            var failure = assertThrows(NullPointerException.class,
                    () -> fixture.arena.use(0, 8, String::length));
            assertEquals(phase.equals("factory") ? "created projection" : "decoded projection",
                    failure.getMessage());
            assertEquals(phase.equals("factory") ? M3ObjectResidency.State.ABSENT
                    : M3ObjectResidency.State.SWAPPED, fixture.arena.state(0));
            assertEquals(M3ObjectResidency.State.RESIDENT, fixture.arena.state(2));
            assertEquals(8L, fixture.arena.residentBytes());
            assertFalse(fixture.events.contains("delete 0"));
            if (phase.equals("decode")) assertNotNull(fixture.secondary[0]);
            fixture.assertBits();
        }
    }

    @Test
    void factoryFailureAfterNestedAdmissionKeepsTheOriginalThrowable() {
        for (Throwable marker : new Throwable[] {new java.io.IOException("factory checked"),
                new IllegalArgumentException("factory runtime"), new AssertionError("factory error")}) {
            PublicationFixture fixture = new PublicationFixture("factory", 8, 0);
            fixture.factoryFailure = marker;
            fixture.armed = true;
            if (marker instanceof java.io.IOException) {
                var failure = assertThrows(M3ObjectResidency.ResidencyException.class,
                        () -> fixture.arena.use(0, 8, String::length));
                assertSame(marker, failure.getCause());
            } else if (marker instanceof Error) {
                assertSame(marker, assertThrows(Error.class,
                        () -> fixture.arena.use(0, 8, String::length)));
            } else {
                assertSame(marker, assertThrows(RuntimeException.class,
                        () -> fixture.arena.use(0, 8, String::length)));
            }
            assertEquals(java.util.List.of("create 0", "create 2"), fixture.events);
            assertEquals(M3ObjectResidency.State.ABSENT, fixture.arena.state(0));
            assertEquals(M3ObjectResidency.State.RESIDENT, fixture.arena.state(2));
            assertEquals(8L, fixture.arena.residentBytes());
            assertSame(fixture.nestedDecision, fixture.arena.lastDecision());
            fixture.assertBits();
        }
    }

    private static void assertRestorePublicationRefused(String phase) {
        for (int extraFlags : new int[] {0, M3ObjectResidency.PRESSURE_AWARE}) {
            PublicationFixture fixture = new PublicationFixture(phase, 8, extraFlags);
            fixture.prepareSwapped();
            byte[] retained = fixture.secondary[0].clone();
            fixture.events.clear();
            fixture.armed = true;
            AtomicInteger operations = new AtomicInteger();
            var failure = assertThrows(M3ObjectResidency.BudgetExceededException.class,
                    () -> fixture.arena.use(0, 8, value -> operations.incrementAndGet()));
            assertEquals(8L, failure.requestedBytes);
            assertEquals(8L, failure.residentBytes);
            assertEquals(8L, failure.targetBytes);
            assertEquals(0, operations.get());
            assertEquals(M3ObjectResidency.State.SWAPPED, fixture.arena.state(0));
            assertEquals(M3ObjectResidency.State.SWAPPED, fixture.arena.state(1));
            assertEquals(M3ObjectResidency.State.RESIDENT, fixture.arena.state(2));
            assertEquals(8L, fixture.arena.residentBytes());
            assertArrayEquals(retained, fixture.secondary[0]);
            assertNotNull(fixture.secondary[1]);
            assertEquals(1, fixture.creates[0]);
            assertEquals(1, fixture.creates[2]);
            assertSame(fixture.nestedDecision, fixture.arena.lastDecision());
            java.util.List<String> expected = phase.equals("read")
                    ? java.util.List.of("encode slot-1-1", "write 1", "read 0", "create 2", "decode 0")
                    : java.util.List.of("encode slot-1-1", "write 1", "read 0", "decode 0", "create 2");
            assertEquals(expected, fixture.events);
            fixture.events.clear();
            assertEquals("slot-0-1", fixture.arena.use(0, 8, value -> value));
            assertEquals(java.util.List.of("encode slot-2-1", "write 2", "read 0", "decode 0", "delete 0"),
                    fixture.events);
            assertEquals(8L, fixture.arena.residentBytes());
            assertNull(fixture.secondary[0]);
            assertNotNull(fixture.secondary[2]);
            assertEquals(1, fixture.creates[0]);
            fixture.assertBits();
        }
    }

    private static final class PublicationFixture {
        final String phase;
        final byte[][] secondary = new byte[4][];
        final int[] creates = new int[4];
        final java.util.List<String> events = new java.util.ArrayList<>();
        final M3ObjectResidency<String> arena;
        M3ObjectResidency.Decision nestedDecision;
        long nestedBytes = 8L;
        boolean armed;
        boolean nullResult;
        Throwable factoryFailure;

        PublicationFixture(String phase, long budget, int extraFlags) {
            this.phase = phase;
            var codec = new M3ObjectResidency.Codec<String>() {
                @Override public byte[] encode(String value) {
                    events.add("encode " + value);
                    return value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                }
                @Override public String decode(int slot, byte[] bytes) {
                    events.add("decode " + slot);
                    checkpoint("decode", slot);
                    return nullResult && slot == 0 ? null
                            : new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                }
            };
            var store = new M3ObjectResidency.SpillStore() {
                @Override public void write(int slot, byte[] bytes) {
                    events.add("write " + slot);
                    secondary[slot] = bytes.clone();
                }
                @Override public byte[] read(int slot) {
                    events.add("read " + slot);
                    checkpoint("read", slot);
                    return secondary[slot].clone();
                }
                @Override public void delete(int slot) {
                    events.add("delete " + slot);
                    secondary[slot] = null;
                }
            };
            arena = new M3ObjectResidency<>(4, M3ObjectResidency.LAZY_CREATE
                    | M3ObjectResidency.LRU | M3ObjectResidency.SWAP_SECONDARY | extraFlags,
                    budget, 0, slot -> {
                        events.add("create " + slot);
                        creates[slot]++;
                        checkpoint("factory", slot);
                        if (slot == 0 && factoryFailure instanceof Error error) throw error;
                        if (slot == 0 && factoryFailure instanceof Exception failure) throw failure;
                        return nullResult && slot == 0 ? null : "slot-" + slot + "-" + creates[slot];
                    }, codec, store);
            for (int slot = 0; slot < 4; slot++) arena.writeParentBits(slot, 0x200L + slot);
        }

        private void checkpoint(String observed, int slot) {
            if (armed && slot == 0 && phase.equals(observed)) {
                armed = false;
                assertEquals(phase.equals("factory") ? M3ObjectResidency.State.CREATING
                        : M3ObjectResidency.State.RESTORING, arena.state(0));
                arena.use(2, nestedBytes, String::length);
                nestedDecision = arena.lastDecision();
            }
        }

        void prepareSwapped() {
            arena.use(0, 8, String::length);
            arena.use(1, 8, String::length);
            assertEquals(M3ObjectResidency.State.SWAPPED, arena.state(0));
        }

        void assertBits() {
            for (int slot = 0; slot < 4; slot++) assertEquals(0x200L + slot, arena.readParentBits(slot));
        }
    }
}
