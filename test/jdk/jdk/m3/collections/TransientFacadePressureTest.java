// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

//FLLDCIM: RELOCATE - donor blob ad0e9c56e43d at fd9a233680b4; recipe m3/collections/recipe/transient-facade-20261106/relocate.py v4
class TransientFacadePressureTest {
    private static final int MODE = TransientFacadeStore.RETAIN_UNEXPOSED
            | TransientFacadeStore.PIN_EXPOSED
            | TransientFacadeStore.PRESSURE_TRIMMABLE
            | TransientFacadeStore.AUTO_HEAP_PRESSURE;

    private static RuntimeHeap.Snapshot normal() {
        return new RuntimeHeap.Snapshot(1L << 30, 500L << 20, 120L << 20);
    }
    private static RuntimeHeap.Snapshot high() {
        return new RuntimeHeap.Snapshot(1L << 30, 980L << 20, 940L << 20);
    }

    @Test void normalOptionalCacheRetainsAndExposedFacadeNeverEvicts() {
        AtomicReference<RuntimeHeap.Snapshot> heap = new AtomicReference<>(normal());
        int[] disposal = {0};
        try (TransientFacadeStore<String> store = new TransientFacadeStore<>(
                id -> "facade-" + id, (id, value) -> disposal[0]++, MODE, heap::get)) {
            String pinned = store.expose(5);
            for (int i = 0; i < 64; i++) store.withInternal(i + 100L, ignored -> { });
            assertEquals(65, store.residentCount());
            heap.set(high());
            store.withInternal(1_000L, ignored -> { }); // 65th optional admission samples
            assertEquals(1, store.residentCount());
            assertSame(pinned, store.expose(5));
            assertTrue(store.isPinned(5));
            assertEquals(65, disposal[0]); // 64 unpinned + current transient
        }
    }

    @Test void highPressureSkipsOptionalRetentionAndDisposesTransient() {
        int[] disposed = {0};
        try (TransientFacadeStore<Object> store = new TransientFacadeStore<>(
                id -> new Object(), (id, value) -> disposed[0]++, MODE,
                TransientFacadePressureTest::high)) {
            for (int i = 0; i < 20; i++) store.withInternal(i, ignored -> { });
            assertEquals(0, store.residentCount());
            assertEquals(20, disposed[0]);
            Object pinned = store.expose(42);
            assertSame(pinned, store.expose(42));
            assertEquals(1, store.residentCount());
        }
    }

    @Test void existingManualFlagsRemainSourceCompatibleAndAutomaticFlagsNeedTrim() {
        int legacy = TransientFacadeStore.RETAIN_UNEXPOSED
                | TransientFacadeStore.PIN_EXPOSED
                | TransientFacadeStore.PRESSURE_TRIMMABLE;
        try (TransientFacadeStore<Object> store = new TransientFacadeStore<>(
                id -> new Object(), legacy)) {
            store.withInternal(10, ignored -> { });
            assertTrue(store.isResident(10));
        }
        assertThrows(IllegalArgumentException.class, () -> new TransientFacadeStore<>(
                id -> new Object(), TransientFacadeStore.RETAIN_UNEXPOSED
                        | TransientFacadeStore.PIN_EXPOSED
                        | TransientFacadeStore.AUTO_HEAP_PRESSURE));
    }

    @Test void failedDisposerDoesNotLoseOtherLogicalFacades() {
        int mode = TransientFacadeStore.RETAIN_UNEXPOSED
                | TransientFacadeStore.PIN_EXPOSED
                | TransientFacadeStore.PRESSURE_TRIMMABLE;
        try (TransientFacadeStore<String> store = new TransientFacadeStore<>(
                id -> "f" + id,
                (id, value) -> { if (id == 2) throw new IllegalStateException("fail"); }, mode)) {
            store.withInternal(1, ignored -> { });
            store.withInternal(2, ignored -> { });
            store.withInternal(3, ignored -> { });
            assertThrows(IllegalStateException.class, () -> store.releaseOptionalResidency(0));
            assertFalse(store.isResident(1)); // first LRU successfully disposed
            assertTrue(store.isResident(2)); // throwing disposer keeps the logical facade
            assertTrue(store.isResident(3));
            assertEquals(2, store.residentCount());
            store.remove(2); // throws; still resident
        } catch (IllegalStateException expected) {
            assertEquals("fail", expected.getMessage());
        }
    }
}
