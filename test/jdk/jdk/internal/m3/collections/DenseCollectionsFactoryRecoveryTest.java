// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.internal.m3.collections;

import jdk.m3.collections.LongLongPriorityQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

/** Frozen caller and ordering contracts for the existing #13922 factory repair. */
final class DenseCollectionsFactoryRecoveryTest {
    @Test
    void sixCallerDescriptorsArePreserved() throws ReflectiveOperationException {
        descriptor("longByteMap", "()Ljdk/m3/collections/LongByteHashMap;");
        descriptor("longSortedSet", "()Ljdk/m3/collections/LongSortedSet;");
        descriptor("longByteSortedMap", "()Ljdk/internal/m3/collections/LongByteSortedMap;");
        descriptor("longLongSortedMap", "()Ljdk/m3/collections/LongLongSortedMap;");
        descriptor("longPriorityQueue", "()Ljdk/m3/collections/LongLongPriorityQueue;");
        descriptor("longPriorityQueue", "(Z)Ljdk/m3/collections/LongLongPriorityQueue;", boolean.class);
    }

    @Test
    void factoriesReturnIndependentPrimitiveOwners() {
        var bytes = DenseCollections.longByteMap();
        var otherBytes = DenseCollections.longByteMap();
        assertNotSame(bytes, otherBytes);
        bytes.put(Long.MIN_VALUE, Byte.MIN_VALUE);
        bytes.put(Long.MAX_VALUE, Byte.MAX_VALUE);
        assertEquals(Byte.MIN_VALUE, bytes.get(Long.MIN_VALUE));
        assertEquals(Byte.MAX_VALUE, bytes.get(Long.MAX_VALUE));
        assertTrue(otherBytes.isEmpty());

        var values = DenseCollections.longSortedSet();
        var otherValues = DenseCollections.longSortedSet();
        assertNotSame(values, otherValues);
        values.add(Long.MAX_VALUE);
        values.add(Long.MIN_VALUE);
        assertEquals(Long.MIN_VALUE, values.first());
        assertEquals(Long.MAX_VALUE, values.last());
        assertTrue(otherValues.isEmpty());

        var sortedBytes = DenseCollections.longByteSortedMap();
        var otherSortedBytes = DenseCollections.longByteSortedMap();
        assertNotSame(sortedBytes, otherSortedBytes);
        sortedBytes.put(Long.MAX_VALUE, Byte.MAX_VALUE);
        sortedBytes.put(Long.MIN_VALUE, Byte.MIN_VALUE);
        assertEquals(Long.MIN_VALUE, sortedBytes.firstKey());
        assertEquals(Byte.MAX_VALUE, sortedBytes.get(Long.MAX_VALUE));
        assertTrue(otherSortedBytes.isEmpty());

        var sortedLongs = DenseCollections.longLongSortedMap();
        var otherSortedLongs = DenseCollections.longLongSortedMap();
        assertNotSame(sortedLongs, otherSortedLongs);
        sortedLongs.put(Long.MAX_VALUE, Long.MIN_VALUE);
        sortedLongs.put(Long.MIN_VALUE, Long.MAX_VALUE);
        assertEquals(Long.MIN_VALUE, sortedLongs.firstKey());
        assertEquals(Long.MIN_VALUE, sortedLongs.get(Long.MAX_VALUE));
        assertTrue(otherSortedLongs.isEmpty());
    }

    @Test
    void priorityQueueFactoriesHonorDefaultAndBothExplicitOrders() {
        var defaultQueue = DenseCollections.longPriorityQueue();
        var minQueue = DenseCollections.longPriorityQueue(true);
        var maxQueue = DenseCollections.longPriorityQueue(false);
        assertNotSame(defaultQueue, DenseCollections.longPriorityQueue());
        assertNotSame(minQueue, DenseCollections.longPriorityQueue(true));
        assertNotSame(maxQueue, DenseCollections.longPriorityQueue(false));
        assertTrue(defaultQueue.minFirst());
        assertTrue(minQueue.minFirst());
        assertFalse(maxQueue.minFirst());
        assertEquals(8, defaultQueue.capacity());
        assertEquals(8, minQueue.capacity());
        assertEquals(8, maxQueue.capacity());
        for (var queue : new LongLongPriorityQueue[] {defaultQueue, minQueue, maxQueue}) {
            queue.offer(7, Long.MAX_VALUE);
            queue.offer(9, Long.MIN_VALUE);
        }
        assertEquals(9, defaultQueue.peekValue());
        assertEquals(9, minQueue.peekValue());
        assertEquals(7, maxQueue.peekValue());
    }

    private static void descriptor(String name, String expected, Class<?>... parameters)
            throws ReflectiveOperationException {
        Method method = DenseCollections.class.getMethod(name, parameters);
        assertEquals(expected, MethodType.methodType(method.getReturnType(),
                method.getParameterTypes()).toMethodDescriptorString());
    }
}
