// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Opt-in DOOP handoff: recycle while still reachable, never resurrect GC garbage. */
final class ObjectResidencyReuseTest {
    private static final int MODE = ObjectResidency.LAZY_CREATE
            | ObjectResidency.LRU | ObjectResidency.RECREATE
            | ObjectResidency.STRICT_NO_ESCAPE;

    private static void reset(StringBuilder value, int slot) {
        value.setLength(0);
        value.append("slot-").append(slot);
    }

    @Test void transfersExclusiveProjectionWithoutFactoryAllocation() {
        AtomicInteger factories = new AtomicInteger();
        var arena = new ObjectResidency<StringBuilder>(2, MODE, 8, 0,
                slot -> {
                    factories.incrementAndGet();
                    return new StringBuilder("slot-" + slot);
                }, null, null);
        arena.writeParentBits(0, 17L);
        arena.writeParentBits(1, 23L);
        int identityBefore = arena.use(0, 8, System::identityHashCode);
        assertEquals(1, factories.get());
        assertTrue(arena.reuseUnborrowedProjection(0, 1, 8,
                ObjectResidencyReuseTest::reset));
        assertEquals(ObjectResidency.State.ABSENT, arena.state(0));
        assertEquals(ObjectResidency.State.RESIDENT, arena.state(1));
        assertEquals(8, arena.residentBytes());
        assertEquals(1, factories.get());
        assertEquals(identityBefore, arena.use(1, 8, System::identityHashCode));
        assertEquals("slot-1", arena.use(1, 8, StringBuilder::toString));
        assertEquals(17L, arena.readParentBits(0));
        assertEquals(23L, arena.readParentBits(1));
    }

    @Test void activeAndBorrowedProjectionsCannotBeReused() {
        var arena = new ObjectResidency<StringBuilder>(2, MODE, 8, 0,
                slot -> new StringBuilder("slot-" + slot), null, null);
        long lease = arena.retainProjection(0, 8);
        assertFalse(arena.reuseUnborrowedProjection(0, 1, 8,
                ObjectResidencyReuseTest::reset));
        arena.useProjection(lease, projection -> {
            assertFalse(arena.reuseUnborrowedProjection(0, 1, 8,
                    ObjectResidencyReuseTest::reset));
            return null;
        });
        arena.releaseProjection(lease);
        assertTrue(arena.reuseUnborrowedProjection(0, 1, 8,
                ObjectResidencyReuseTest::reset));
        assertThrows(IllegalStateException.class,
                () -> arena.useProjection(lease, StringBuilder::length));
    }

    @Test void refusesInstalledUnprovenAndNonAbsentTargets() {
        var installed = new ObjectResidency<StringBuilder>(2, MODE, 16, 0,
                slot -> new StringBuilder("created"), null, null);
        StringBuilder external = new StringBuilder("external");
        installed.install(0, external, 8);
        assertFalse(installed.reuseUnborrowedProjection(0, 1, 8,
                ObjectResidencyReuseTest::reset));
        assertEquals("external", external.toString());

        var unproven = new ObjectResidency<StringBuilder>(2,
                ObjectResidency.LAZY_CREATE | ObjectResidency.RECREATE, 16, 0,
                slot -> new StringBuilder("slot-" + slot), null, null);
        unproven.use(0, 8, StringBuilder::length);
        assertFalse(unproven.reuseUnborrowedProjection(0, 1, 8,
                ObjectResidencyReuseTest::reset));

        var occupied = new ObjectResidency<StringBuilder>(2, MODE, 16, 0,
                slot -> new StringBuilder("slot-" + slot), null, null);
        occupied.use(0, 8, StringBuilder::length);
        occupied.use(1, 8, StringBuilder::length);
        assertFalse(occupied.reuseUnborrowedProjection(0, 1, 8,
                ObjectResidencyReuseTest::reset));
    }

    @Test void refusesInsufficientBudgetAndInvalidInputsWithoutChangingSource() {
        var arena = new ObjectResidency<StringBuilder>(2, MODE, 8, 0,
                slot -> new StringBuilder("slot-" + slot), null, null);
        assertEquals(6, arena.use(0, 8, StringBuilder::length));
        assertFalse(arena.reuseUnborrowedProjection(0, 1, 9,
                ObjectResidencyReuseTest::reset));
        assertFalse(arena.reuseUnborrowedProjection(0, 0, 8,
                ObjectResidencyReuseTest::reset));
        assertThrows(IllegalArgumentException.class, () ->
                arena.reuseUnborrowedProjection(0, 1, -1, ObjectResidencyReuseTest::reset));
        assertThrows(NullPointerException.class, () ->
                arena.reuseUnborrowedProjection(0, 1, 8, null));
        assertEquals(ObjectResidency.State.RESIDENT, arena.state(0));
        assertEquals(ObjectResidency.State.ABSENT, arena.state(1));
        assertEquals(8L, arena.residentBytes());
    }

    @Test void failedResetDropsMutatedProjectionAndPreservesParentState() {
        AtomicInteger created = new AtomicInteger();
        var arena = new ObjectResidency<StringBuilder>(2, MODE, 8, 0,
                slot -> {
                    created.incrementAndGet();
                    return new StringBuilder("slot-" + slot);
                }, null, null);
        arena.writeParentBits(0, 99L);
        arena.use(0, 8, StringBuilder::length);
        var thrown = assertThrows(ObjectResidency.ResidencyException.class,
                () -> arena.reuseUnborrowedProjection(0, 1, 8, (value, target) -> {
                    value.append("-partially-reset");
                    throw new IOException("failed reset");
                }));
        assertInstanceOf(IOException.class, thrown.getCause());
        assertEquals(ObjectResidency.State.ABSENT, arena.state(0));
        assertEquals(ObjectResidency.State.ABSENT, arena.state(1));
        assertEquals(0L, arena.residentBytes());
        assertEquals(99L, arena.readParentBits(0));
        assertEquals("slot-0", arena.use(0, 8, StringBuilder::toString));
        assertEquals(2, created.get());
    }

    @Test void reinitializerCannotReenterTransitioningSlots() {
        var arena = new ObjectResidency<StringBuilder>(2, MODE, 8, 0,
                slot -> new StringBuilder("slot-" + slot), null, null);
        arena.use(0, 8, StringBuilder::length);
        assertTrue(arena.reuseUnborrowedProjection(0, 1, 8, (value, target) -> {
            assertThrows(IllegalStateException.class,
                    () -> arena.use(0, 8, StringBuilder::length));
            assertThrows(IllegalStateException.class,
                    () -> arena.use(1, 8, StringBuilder::length));
            reset(value, target);
        }));
        assertEquals("slot-1", arena.use(1, 8, StringBuilder::toString));
    }
}
