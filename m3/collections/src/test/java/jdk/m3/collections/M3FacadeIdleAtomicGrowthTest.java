// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Paired idle Object[]/long[] admission must be atomic across copy failure. */
//FLLDCIM: RELOCATE - donor blob ea6e96263034 at ff83fd2a685b; recipe m3/collections/qualification/doop-facade-fixes-20261107/apply.py v1
final class M3FacadeIdleAtomicGrowthTest {
    private static final class Cell {
        long id;
        Cell(long id) { this.id = id; }
    }

    private static M3TransientFacadeStore<Cell> store(AtomicInteger made,
            AtomicInteger rebound, AtomicInteger scrubbed, List<Long> disposed) {
        return new M3TransientFacadeStore<>(id -> {
            made.incrementAndGet();
            return new Cell(id);
        }, (id, facade) -> disposed.add(id), M3TransientFacadeStore.DEFAULT_MODE,
                new M3TransientFacadeStore.Recycler<>() {
                    @Override public void scrub(Cell facade) {
                        scrubbed.incrementAndGet();
                        facade.id = -1L;
                    }
                    @Override public void rebind(Cell facade, long id) {
                        assertEquals(-1L, facade.id, "idle must be scrubbed");
                        rebound.incrementAndGet();
                        facade.id = id;
                    }
                }, 4);
    }

    private static M3TransientFacadeStore.BackingCopier failLongs() {
        return new M3TransientFacadeStore.BackingCopier() {
            @Override public Object[] objects(Object[] array, int size) {
                return Arrays.copyOf(array, size);
            }
            @Override public long[] longs(long[] array, int size) {
                throw new IllegalStateException("injected second idle allocation failure");
            }
            @Override public short[] shorts(short[] array, int size) {
                throw new AssertionError("idle growth cannot allocate flag lane");
            }
        };
    }

    @Test
    void secondIdleLaneFailureCannotPoisonSubsequentNormalReturn() {
        AtomicInteger made = new AtomicInteger();
        AtomicInteger rebound = new AtomicInteger();
        AtomicInteger scrubbed = new AtomicInteger();
        List<Long> disposed = new ArrayList<>();
        try (var owner = store(made, rebound, scrubbed, disposed)) {
            IllegalStateException failure = assertThrows(IllegalStateException.class,
                    () -> owner.ensureReusableCapacity(1, failLongs()));
            assertEquals("injected second idle allocation failure", failure.getMessage());
            assertEquals(0, owner.reusableIdleCount());
            owner.withInternal(11, cell -> assertEquals(11L, cell.id));
            assertEquals(1, owner.reusableIdleCount(), "failed growth must not strand ID lane");
            owner.withInternal(12, cell -> assertEquals(12L, cell.id));
            assertEquals(1, owner.reusableIdleCount());
            assertEquals(1, made.get(), "reborrow must reuse the only idle object");
            assertEquals(1, rebound.get());
            assertEquals(2, scrubbed.get());
        }
        assertEquals(List.of(12L), disposed);
    }

    @Test
    void malformedFirstIdleCopyCannotPublishBeforeLengthCheck() {
        AtomicInteger made = new AtomicInteger();
        try (var owner = store(made, new AtomicInteger(), new AtomicInteger(), new ArrayList<>())) {
            var wrongSize = new M3TransientFacadeStore.BackingCopier() {
                @Override public Object[] objects(Object[] array, int capacity) {
                    return Arrays.copyOf(array, capacity - 1);
                }
                @Override public long[] longs(long[] array, int capacity) {
                    return Arrays.copyOf(array, capacity);
                }
                @Override public short[] shorts(short[] array, int capacity) {
                    throw new AssertionError("unexpected");
                }
            };
            assertThrows(IllegalStateException.class,
                    () -> owner.ensureReusableCapacity(1, wrongSize));
            owner.withInternal(5, cell -> assertEquals(5L, cell.id));
            assertEquals(1, owner.reusableIdleCount());
            assertEquals(1, made.get());
        }
    }

    @Test
    void nullSecondIdleCopyCannotPublishAndNormalCopyStillSucceeds() {
        AtomicInteger made = new AtomicInteger();
        try (var owner = store(made, new AtomicInteger(), new AtomicInteger(), new ArrayList<>())) {
            var nullSecond = new M3TransientFacadeStore.BackingCopier() {
                @Override public Object[] objects(Object[] array, int capacity) {
                    return Arrays.copyOf(array, capacity);
                }
                @Override public long[] longs(long[] array, int capacity) { return null; }
                @Override public short[] shorts(short[] array, int capacity) {
                    throw new AssertionError("unexpected");
                }
            };
            assertThrows(NullPointerException.class,
                    () -> owner.ensureReusableCapacity(1, nullSecond));
            owner.withInternal(6, cell -> assertEquals(6L, cell.id));
            assertEquals(1, owner.reusableIdleCount());
        }
        assertEquals(1, made.get());
    }

    @Test
    void growthFailureWithOneIdlePreservesExistingObjectAndAllowsNestedTwoIdle() {
        AtomicInteger made = new AtomicInteger();
        AtomicInteger rebound = new AtomicInteger();
        List<Long> disposed = new ArrayList<>();
        try (var owner = store(made, rebound, new AtomicInteger(), disposed)) {
            owner.withInternal(20, cell -> assertEquals(20L, cell.id));
            assertEquals(1, owner.reusableIdleCount());
            assertThrows(IllegalStateException.class,
                    () -> owner.ensureReusableCapacity(2, failLongs()));
            assertEquals(1, owner.reusableIdleCount());
            owner.withInternal(21, outer -> {
                assertEquals(21L, outer.id);
                owner.withInternal(22, inner -> {
                    assertEquals(22L, inner.id);
                    assertTrue(inner != outer, "nested lease must have exclusive object");
                });
            });
            assertEquals(2, owner.reusableIdleCount(), "both idle lanes grow together");
            assertEquals(2, made.get());
            assertEquals(1, rebound.get());
            assertEquals(2, owner.releaseReusable(0));
            assertEquals(0, owner.reusableIdleCount());
        }
        assertEquals(2, disposed.size());
    }

    @Test
    void alreadySufficientIdleCapacityDoesNotInvokeCopier() {
        AtomicInteger made = new AtomicInteger();
        try (var owner = store(made, new AtomicInteger(), new AtomicInteger(), new ArrayList<>())) {
            owner.withInternal(1, cell -> assertEquals(1L, cell.id));
            var forbidden = new M3TransientFacadeStore.BackingCopier() {
                @Override public Object[] objects(Object[] array, int capacity) {
                    throw new AssertionError("unnecessary idle copy");
                }
                @Override public long[] longs(long[] array, int capacity) {
                    throw new AssertionError("unnecessary ID copy");
                }
                @Override public short[] shorts(short[] array, int capacity) {
                    throw new AssertionError("unnecessary flags copy");
                }
            };
            owner.ensureReusableCapacity(1, forbidden);
            assertEquals(1, owner.reusableIdleCount());
        }
        assertEquals(1, made.get());
    }

    @Test
    void exposedPinnedFacadeNeverEntersRecyclableIdleLanes() {
        AtomicInteger made = new AtomicInteger();
        List<Long> disposed = new ArrayList<>();
        try (var owner = store(made, new AtomicInteger(), new AtomicInteger(), disposed)) {
            Cell pinned = owner.expose(100);
            owner.withInternal(101, internal -> assertEquals(101L, internal.id));
            assertSame(pinned, owner.expose(100));
            assertTrue(owner.isPinned(100));
            assertEquals(1, owner.reusableIdleCount());
            assertEquals(1, owner.releaseReusable(0));
            assertSame(pinned, owner.expose(100));
            assertEquals(2, made.get());
        }
        assertEquals(2, disposed.size());
        assertTrue(disposed.contains(100L));
        assertTrue(disposed.contains(101L));
    }
}
