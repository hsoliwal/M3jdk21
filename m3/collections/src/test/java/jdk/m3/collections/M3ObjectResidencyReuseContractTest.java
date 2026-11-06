// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Physical shell handoff before GC eligibility, not JVM object resurrection. */
//FLLDCIM: RELOCATE - donor blob 0d3aee2044b8 at ca1aafc4d94f; recipe m3/collections/qualification/census-g7-20261106/relocate.py v5
final class M3ObjectResidencyReuseContractTest {
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
        assertTrue(arena.reuseUnborrowedProjection(0, 1, 16, (value, slot) -> {
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
        assertFalse(arena.reuseUnborrowedProjection(0, 1, 8, (v, s) -> v.setLength(0)));
        arena.releaseProjection(token);
        assertEquals(8, arena.use(0, 8, value -> {
            assertFalse(arena.reuseUnborrowedProjection(0, 1, 8, (v, s) -> v.setLength(0)));
            return value.length();
        }).intValue());
        var installed = new M3ObjectResidency<StringBuilder>(2, MODE, 32, 0,
                slot -> new StringBuilder(), null, null);
        installed.install(0, new StringBuilder("external"), 8);
        assertFalse(installed.reuseUnborrowedProjection(0, 1, 8, (v, s) -> v.setLength(0)));
        assertFalse(arena.reuseUnborrowedProjection(0, 0, 8, (v, s) -> v.setLength(0)));
    }

    @Test
    void zeroBorrowCountDoesNotProveNoEscape() {
        var arena = new M3ObjectResidency<StringBuilder>(2,
                M3ObjectResidency.LAZY_CREATE | M3ObjectResidency.LRU
                        | M3ObjectResidency.RECREATE,
                32, 0, slot -> new StringBuilder("still here"), null, null);
        assertEquals(10, arena.use(0, 8, StringBuilder::length).intValue());
        assertFalse(arena.reuseUnborrowedProjection(0, 1, 8, (v, s) -> v.setLength(0)));
        assertEquals(M3ObjectResidency.State.RESIDENT, arena.state(0));
    }

    @Test
    void checkedResetFailureWrapsOriginalAndDiscardsTaintedShell() {
        assertResetFailure(new java.io.IOException("checked reset failure"));
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
        assertTrue(arena.reuseUnborrowedProjection(0, 1, 8,
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
                () -> arena.reuseUnborrowedProjection(-1, 1, 8,
                        (value, slot) -> resets.incrementAndGet()));
        assertThrows(IndexOutOfBoundsException.class,
                () -> arena.reuseUnborrowedProjection(0, 3, 8,
                        (value, slot) -> resets.incrementAndGet()));
        assertThrows(IllegalArgumentException.class,
                () -> arena.reuseUnborrowedProjection(0, 1, -1,
                        (value, slot) -> resets.incrementAndGet()));
        assertThrows(NullPointerException.class,
                () -> arena.reuseUnborrowedProjection(0, 1, 8, null));
        assertSame(prior, arena.lastDecision());
        assertEquals(8L, arena.residentBytes());
        arena.use(1, 8, value -> value.instance);
        assertRefused(arena, 0, 1, 8, resets);
        assertEquals(0, resets.get());
        assertSeedBits(arena);
    }

    @Test
    void successfulRebindPreservesLogicalBitsAndMakesTargetMostRecentlyUsed() {
        var arena = projections(4, MODE, 16, 0, new AtomicInteger());
        seedBits(arena);
        arena.use(0, 8, value -> value.instance);
        arena.use(1, 8, value -> value.instance);
        arena.use(0, 8, value -> value.instance);
        assertTrue(arena.reuseUnborrowedProjection(0, 2, 8, (value, slot) -> value.text = "rebound"));
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
    void maximumBudgetArithmeticRejectsOverflowAndAcceptsExactBoundary() {
        var arena = projections(3, MODE, Long.MAX_VALUE, 0, new AtomicInteger());
        seedBits(arena);
        int identity = arena.use(0, Long.MAX_VALUE - 2, System::identityHashCode);
        arena.use(2, 2, value -> value.instance);
        AtomicInteger resets = new AtomicInteger();
        assertRefused(arena, 0, 1, Long.MAX_VALUE - 1, resets);
        assertEquals(Long.MAX_VALUE, arena.residentBytes());
        assertTrue(arena.reuseUnborrowedProjection(0, 1, Long.MAX_VALUE - 2,
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
        assertTrue(arena.reuseUnborrowedProjection(0, 1, Long.MAX_VALUE, (value, slot) -> { }));
        assertEquals(Long.MAX_VALUE, arena.residentBytes());
        assertTrue(arena.reuseUnborrowedProjection(1, 2, 0, (value, slot) -> { }));
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
        assertTrue(pressure.reuseUnborrowedProjection(0, 1, 0,
                (value, slot) -> resets.incrementAndGet()));
        pressure.signalMemoryPressure();
        assertEquals(1, resets.get());
        assertEquals(0L, pressure.residentBytes());
        assertEquals(M3ObjectResidency.State.RESIDENT, pressure.state(1));

        var fixed = projections(2, MODE, Long.MAX_VALUE, Long.MAX_VALUE, new AtomicInteger());
        fixed.use(0, 1, value -> value.instance);
        assertTrue(fixed.reuseUnborrowedProjection(0, 1, 1, (value, slot) -> { }));
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
        assertFalse(arena.reuseUnborrowedProjection(from, to, bytes,
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
                () -> arena.reuseUnborrowedProjection(0, 1, 16, (value, slot) -> {
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
