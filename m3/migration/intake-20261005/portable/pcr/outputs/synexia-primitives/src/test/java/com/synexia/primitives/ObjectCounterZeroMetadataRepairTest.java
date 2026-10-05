// SPDX-License-Identifier: Apache-2.0
package com.synexia.primitives;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Regressions for the counter wrapper over the existing zero-metadata map owner. */
final class ObjectCounterZeroMetadataRepairTest {
    @Test
    void emptyExtremaAndTraversalKeepTheConfiguredMissingValue() {
        ObjectCounterMap<String> counts = new ObjectCounterMap<>(17L);

        assertEquals(17L, counts.minValue());
        assertEquals(17L, counts.maxValue());
        List<String> visited = new ArrayList<>();
        counts.forEach((key, value) -> visited.add(key));
        assertTrue(visited.isEmpty());
        assertThrows(NullPointerException.class, () -> counts.forEach(null));

        counts.putBits("item", -8L);
        counts.clear();
        assertTrue(counts.isEmpty());
        assertEquals(17L, counts.minValue());
        assertEquals(17L, counts.maxValue());
    }

    @Test
    void nullSidecarParticipatesInExtremaWithZeroAndBothLongLimits() {
        ObjectCounterMap<String> counts = new ObjectCounterMap<>(17L);

        counts.putBits(null, Long.MIN_VALUE);
        assertEquals(Long.MIN_VALUE, counts.minValue());
        assertEquals(Long.MIN_VALUE, counts.maxValue());
        counts.putBits("high", Long.MAX_VALUE);
        assertEquals(Long.MIN_VALUE, counts.minValue());
        assertEquals(Long.MAX_VALUE, counts.maxValue());

        assertEquals(Long.MIN_VALUE, counts.remove(null));
        assertFalse(counts.containsKey(null));
        assertEquals(Long.MAX_VALUE, counts.minValue());
        assertEquals(Long.MAX_VALUE, counts.maxValue());

        counts.putBits(null, 0L);
        assertEquals(0L, counts.minValue());
        assertEquals(Long.MAX_VALUE, counts.maxValue());
        counts.remove("high");
        assertEquals(0L, counts.minValue());
        assertEquals(0L, counts.maxValue());

        counts.putBits(null, Long.MAX_VALUE);
        counts.putBits("low", Long.MIN_VALUE);
        assertEquals(Long.MIN_VALUE, counts.minValue());
        assertEquals(Long.MAX_VALUE, counts.maxValue());
        counts.clear();
        assertFalse(counts.containsKey(null));
        assertEquals(17L, counts.minValue());
        assertEquals(17L, counts.maxValue());
    }

    @Test
    void removingNullByReachingTheMissingValueLeavesNoExtremaOrTraversalGhost() {
        ObjectCounterMap<String> counts = new ObjectCounterMap<>(5L);
        counts.putBits(null, 12L);
        counts.putBits("remaining", -7L);

        assertEquals(12L, counts.getAndAdd(null, -7L));
        assertFalse(counts.containsKey(null));
        assertEquals(1, counts.size());
        assertEquals(-7L, counts.minValue());
        assertEquals(-7L, counts.maxValue());
        List<String> visited = new ArrayList<>();
        counts.forEach((key, value) -> {
            visited.add(key);
            assertEquals(-7L, value);
        });
        assertEquals(List.of("remaining"), visited);

        counts.remove("remaining");
        assertEquals(5L, counts.minValue());
        assertEquals(5L, counts.maxValue());
    }

    @Test
    void traversalKeepsOwnerOrderIdentityAndRawValuesAfterGrowthAndDeletion() {
        ObjectCounterMap<CollisionKey> counts = new ObjectCounterMap<>();
        ObjectPrimitiveHashMap<CollisionKey> owner =
                new ObjectPrimitiveHashMap<>(PrimitiveKind.LONG);
        List<CollisionKey> inserted = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            CollisionKey key = new CollisionKey(i);
            inserted.add(key);
            long value = (i & 1) == 0 ? Long.MIN_VALUE + i : Long.MAX_VALUE - i;
            counts.putBits(key, value);
            owner.putBits(key, value, 0L);
        }
        counts.putBits(null, -13L);
        owner.putBits(null, -13L, 0L);
        for (int i = 0; i < inserted.size(); i += 3) {
            counts.remove(inserted.get(i));
            owner.removeOrDefaultBits(inserted.get(i), 0L);
        }
        CollisionKey equalReplacement = new CollisionKey(1);
        counts.putBits(equalReplacement, Long.MAX_VALUE);
        owner.putBits(equalReplacement, Long.MAX_VALUE, 0L);

        List<CollisionKey> expectedKeys = new ArrayList<>();
        List<Long> expectedValues = new ArrayList<>();
        owner.forEach((key, value) -> {
            expectedKeys.add(key);
            expectedValues.add(value);
        });
        List<CollisionKey> actualKeys = new ArrayList<>();
        List<Long> actualValues = new ArrayList<>();
        counts.forEach((key, value) -> {
            actualKeys.add(key);
            actualValues.add(value);
        });

        assertEquals(owner.size(), actualKeys.size());
        assertEquals(expectedValues, actualValues);
        for (int i = 0; i < expectedKeys.size(); i++) {
            assertSame(expectedKeys.get(i), actualKeys.get(i));
        }
        assertSame(null, actualKeys.get(0));
        assertSame(inserted.get(1), actualKeys.get(actualKeys.indexOf(equalReplacement)));
    }

    @Test
    void consumerFailureKeepsItsIdentityAndLeavesTheCounterUsable() {
        ObjectCounterMap<String> counts = new ObjectCounterMap<>();
        counts.putBits("item", 4L);
        counts.putBits(null, -2L);
        assertThrows(NullPointerException.class, () -> counts.forEach(null));
        AssertionError failure = new AssertionError("consumer failure");

        assertSame(failure, assertThrows(AssertionError.class, () -> counts.forEach((key, value) -> {
            assertSame(null, key);
            assertEquals(-2L, value);
            throw failure;
        })));

        assertEquals(2, counts.size());
        assertEquals(-2L, counts.minValue());
        assertEquals(4L, counts.maxValue());
        List<String> visited = new ArrayList<>();
        counts.forEach((key, value) -> visited.add(key));
        assertEquals(2, visited.size());
    }

    @Test
    void counterRetainsTheExistingValueLaneFootprintWithoutOccupancyStorage() {
        ObjectCounterMap<Integer> counts = new ObjectCounterMap<>();
        long emptyPayloadBytes = counts.primitivePayloadBytes();
        assertEquals((long) counts.referenceSlots() * Long.BYTES, emptyPayloadBytes);
        counts.putBits(null, 1L);
        assertEquals(emptyPayloadBytes, counts.primitivePayloadBytes());
        for (int i = 0; i < 40; i++) counts.putBits(i, i + 1L);
        assertEquals((long) counts.referenceSlots() * Long.BYTES, counts.primitivePayloadBytes());

        for (var field : ObjectPrimitiveHashMap.class.getDeclaredFields()) {
            assertNotEquals("occupied", field.getName());
            assertNotEquals(byte[].class, field.getType());
        }
        for (var field : ObjectCounterMap.class.getDeclaredFields()) {
            assertFalse(field.getType().isArray());
        }
    }

    private record CollisionKey(int id) {
        @Override
        public int hashCode() {
            return 1;
        }
    }
}
