// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

//FLLDCIM: RELOCATE - donor blob 6afad3998f79 at fd9a233680b4; recipe m3/collections/recipe/transient-facade-20261106/relocate.py v4
class FacadeAtomicGrowthTest {
    private static final int MODE = TransientFacadeStore.RETAIN_UNEXPOSED
            | TransientFacadeStore.PIN_EXPOSED
            | TransientFacadeStore.PRESSURE_TRIMMABLE;

    private static final TransientFacadeStore.BackingCopier NORMAL =
            new TransientFacadeStore.BackingCopier() {
                public long[] longs(long[] values, int length) {
                    return Arrays.copyOf(values, length);
                }
                public Object[] objects(Object[] values, int length) {
                    return Arrays.copyOf(values, length);
                }
                public short[] shorts(short[] values, int length) {
                    return Arrays.copyOf(values, length);
                }
            };

    @Test void failedSecondAllocationRetainsAllFourOriginalLanes() {
        try (TransientFacadeStore<Object> store = new TransientFacadeStore<>(
                id -> new Object(), MODE)) {
            Object first = store.expose(7);
            for (int i = 0; i < 3; i++) store.withInternal(20 + i, ignored -> { });
            TransientFacadeStore.BackingCopier failSecond =
                    new TransientFacadeStore.BackingCopier() {
                        public long[] longs(long[] values, int length) {
                            return Arrays.copyOf(values, length);
                        }
                        public Object[] objects(Object[] values, int length) {
                            throw new IllegalStateException("synthetic allocation failure");
                        }
                        public short[] shorts(short[] values, int length) {
                            return Arrays.copyOf(values, length);
                        }
                    };
            assertThrows(IllegalStateException.class, () -> store.ensureCapacity(5, failSecond));
            assertEquals(4, store.residentCount());
            assertSame(first, store.expose(7));
            for (int i = 0; i < 3; i++) assertTrue(store.isResident(20 + i));
            store.withInternal(50, ignored -> { });
            assertEquals(5, store.residentCount());
            assertSame(first, store.expose(7));
        }
    }

    @Test void malformedCopyDoesNotPublishPartialArrays() {
        try (TransientFacadeStore<String> store = new TransientFacadeStore<>(
                id -> "f" + id, MODE)) {
            store.withInternal(0, ignored -> { });
            TransientFacadeStore.BackingCopier shortArray =
                    new TransientFacadeStore.BackingCopier() {
                        public long[] longs(long[] values, int length) {
                            return Arrays.copyOf(values, length);
                        }
                        public Object[] objects(Object[] values, int length) {
                            return Arrays.copyOf(values, length - 1);
                        }
                        public short[] shorts(short[] values, int length) {
                            return Arrays.copyOf(values, length);
                        }
                    };
            assertThrows(IllegalStateException.class, () -> store.ensureCapacity(10, shortArray));
            assertEquals(1, store.residentCount());
            assertTrue(store.isResident(0));
            store.ensureCapacity(10, NORMAL);
            for (int i = 1; i < 10; i++) store.withInternal(i, ignored -> { });
            assertEquals(10, store.residentCount());
        }
    }
}
