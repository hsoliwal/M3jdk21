// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class M3PackedLongLongLruPressureTest {
    @Test
    void shrinkEvictsLeastRecentAndPreservesConfiguredCapacity() {
        M3PackedLongLongLruCache cache = new M3PackedLongLongLruCache(8);
        for (long key = 0; key < 8; key++) {
            assertTrue(cache.put(key, key * 10));
        }

        assertEquals(0, cache.getOrDefault(0, -1));
        assertEquals(10, cache.getOrDefault(1, -1));
        long fullPayload = cache.arrayPayloadBytes();

        assertEquals(4, cache.readjust(0.5));
        assertEquals(8, cache.capacity());
        assertEquals(4, cache.residentCapacity());
        assertEquals(4, cache.size());
        assertEquals(4, cache.evictions());
        assertTrue(cache.arrayPayloadBytes() > 0);
        assertTrue(cache.arrayPayloadBytes() < fullPayload);
        assertArrayEquals(new long[] {6, 7, 0, 1}, cache.keysOldestFirst());

        long reducedPayload = cache.arrayPayloadBytes();
        assertEquals(0, cache.readjust(0.5));
        assertEquals(reducedPayload, cache.arrayPayloadBytes());
        assertEquals(4, cache.evictions());
    }

    @Test
    void zeroResidencyDropsEverythingAndRefusesFutureAdmission() {
        AtomicInteger callbacks = new AtomicInteger();
        M3PackedLongLongLruCache cache =
                new M3PackedLongLongLruCache(4, (key, value) -> {
                    callbacks.incrementAndGet();
                    return true;
                });

        assertTrue(cache.put(1, 10));
        assertTrue(cache.put(2, 20));
        assertEquals(2, callbacks.get());

        assertEquals(2, cache.readjust(0.0));
        assertEquals(0, cache.residentCapacity());
        assertEquals(0, cache.size());
        assertEquals(0, cache.arrayPayloadBytes());
        assertEquals(2, cache.evictions());

        assertEquals(0, cache.readjust(0.0));
        assertEquals(0, cache.arrayPayloadBytes());
        assertFalse(cache.put(3, 30));
        assertEquals(2, callbacks.get(), "zero-residency cache must not run admission");
        assertEquals(0, cache.size());
    }

    @Test
    void recoveryRaisesBudgetWithoutResurrectingEvictedMappings() {
        M3PackedLongLongLruCache cache = new M3PackedLongLongLruCache(4);
        for (long key = 0; key < 4; key++) {
            cache.put(key, key);
        }

        assertEquals(4, cache.readjust(0.0));
        assertEquals(0, cache.size());

        assertEquals(0, cache.readjust(1.0));
        assertEquals(4, cache.residentCapacity());
        assertEquals(0, cache.size());
        assertEquals(0, cache.arrayPayloadBytes());
        assertEquals(-1, cache.getOrDefault(1, -1));

        assertTrue(cache.put(9, 90));
        assertEquals(1, cache.size());
        assertTrue(cache.arrayPayloadBytes() > 0);
        assertEquals(90, cache.getOrDefault(9, -1));
    }


    @Test
    void recoveryDoesNotExpandExistingCompactBackingUntilDemand() {
        M3PackedLongLongLruCache cache = new M3PackedLongLongLruCache(8);
        for (long key = 0; key < 8; key++) {
            cache.put(key, key);
        }

        assertEquals(4, cache.readjust(0.5));
        long compactPayload = cache.arrayPayloadBytes();
        assertEquals(4, cache.residentCapacity());

        assertEquals(0, cache.readjust(1.0));
        assertEquals(8, cache.residentCapacity());
        assertEquals(4, cache.size());
        assertEquals(compactPayload, cache.arrayPayloadBytes());

        cache.put(8, 8);
        assertEquals(5, cache.size());
        assertTrue(cache.arrayPayloadBytes() >= compactPayload);
    }

    @Test
    void emptySameRatioSampleReleasesStaleBacking() {
        M3PackedLongLongLruCache cache = new M3PackedLongLongLruCache(8);
        cache.put(1, 1);
        cache.clear();
        assertEquals(0, cache.size());
        assertTrue(cache.arrayPayloadBytes() > 0);

        assertEquals(0, cache.readjust(1.0));
        assertEquals(8, cache.residentCapacity());
        assertEquals(0, cache.arrayPayloadBytes());
    }

    @Test
    void pressureCapacityUsesFloorAndReplacementDoesNotEvict() {
        M3PackedLongLongLruCache cache = new M3PackedLongLongLruCache(31);
        assertEquals(0, cache.readjust(0.5));
        assertEquals(15, cache.residentCapacity());
        assertEquals(31, cache.capacity());

        for (int key = 0; key < 15; key++) {
            assertTrue(cache.put(key, key));
        }
        long before = cache.evictions();

        assertTrue(cache.put(14, 1400));
        assertEquals(before, cache.evictions());
        assertEquals(15, cache.size());

        assertTrue(cache.put(99, 99));
        assertEquals(before + 1, cache.evictions());
        assertEquals(15, cache.size());
        assertFalse(cache.containsKey(0));
        assertEquals(1400, cache.peekOrDefault(14, -1));
    }


    @Test
    void configuredZeroCapacityRetainsNoPrimitiveBackingPayload() {
        M3PackedLongLongLruCache cache = new M3PackedLongLongLruCache(0);
        assertEquals(0, cache.capacity());
        assertEquals(0, cache.residentCapacity());
        assertEquals(0, cache.size());
        assertEquals(0, cache.arrayPayloadBytes());
        assertFalse(cache.put(1, 1));
    }

    @Test
    void invalidRatioFailsBeforeMutation() {
        M3PackedLongLongLruCache cache = new M3PackedLongLongLruCache(4);
        cache.put(1, 10);
        cache.put(2, 20);

        double[] invalid = {
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            -Double.MIN_VALUE,
            1.0000000000000002
        };
        for (double ratio : invalid) {
            assertThrows(IllegalArgumentException.class, () -> cache.readjust(ratio));
            assertEquals(4, cache.residentCapacity());
            assertArrayEquals(new long[] {1, 2}, cache.keysOldestFirst());
            assertEquals(0, cache.evictions());
        }
    }

    @Test
    void pressureAdjustmentIsRejectedInsideCallbacks() {
        M3PackedLongLongLruCache cache = new M3PackedLongLongLruCache(4);
        cache.put(1, 10);
        cache.put(2, 20);

        assertThrows(
                IllegalStateException.class,
                () -> cache.forEachOldestFirst((key, value) -> cache.readjust(0.0)));

        assertEquals(4, cache.residentCapacity());
        assertArrayEquals(new long[] {1, 2}, cache.keysOldestFirst());
        assertEquals(0, cache.evictions());
    }
}
