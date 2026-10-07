// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
// Materialized from hsoliwal/com.synexia@8513abf8eb0d911873c9127a138007a42dd4ab60
// Source: synexia-primitives/src/test/java/com/synexia/m3/util/M3PrimitiveCollectionFacadesTest.java
// Source-Git-blob: 4f0705ba4f58a7bbb8e2947c119b2483b5273c06
package com.m3.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

final class M3PrimitiveCollectionFacadesTest {
    @Test
    void primitiveKindsPreserveLaneWidths() {
        assertEquals(1, M3PrimitiveKind.BOOLEAN.widthBytes());
        assertEquals(1, M3PrimitiveKind.BYTE.widthBytes());
        assertEquals(2, M3PrimitiveKind.SHORT.widthBytes());
        assertEquals(2, M3PrimitiveKind.CHAR.widthBytes());
        assertEquals(4, M3PrimitiveKind.INT.widthBytes());
        assertEquals(8, M3PrimitiveKind.LONG.widthBytes());
        assertEquals(4, M3PrimitiveKind.FLOAT.widthBytes());
        assertEquals(8, M3PrimitiveKind.DOUBLE.widthBytes());
        assertEquals(28, M3PrimitiveKind.INT.payloadBytes(7));
    }

    @Test
    void primitiveArrayListPreservesExactWidthOwnerContract() {
        M3PrimitiveArrayList list = new M3PrimitiveArrayList(M3PrimitiveKind.INT, 1);
        list.addBits(3);
        list.addBits(1);
        list.addBits(1, 2);
        assertArrayEquals(new int[] {3, 2, 1}, (int[]) list.toPrimitiveArray());
        assertEquals(2, list.getBits(1));
        assertEquals(2, list.setBits(1, 4));
        assertEquals(4, list.getBits(1));
        assertTrue(list.removeBits(3));
        list.addAllBits(new int[] {8, 6, 7}, 1, 2);
        assertArrayEquals(new int[] {4, 1, 6, 7}, (int[]) list.toPrimitiveArray());
        list.sort();
        assertArrayEquals(new int[] {1, 4, 6, 7}, (int[]) list.toPrimitiveArray());
        assertEquals(1, list.binarySearchBits(4));
        assertTrue(list.removeIfBits(value -> (value & 1L) != 0));
        assertArrayEquals(new int[] {4, 6}, (int[]) list.toPrimitiveArray());
        list.removeRange(0, 1);
        assertArrayEquals(new int[] {6}, (int[]) list.toPrimitiveArray());
        list.trimToSize();
        assertEquals(1, list.size());
        assertFalse(list.isEmpty());
    }

    @Test
    void primitiveArrayDequePreservesLogicalOrderAndThrowingEmptyContract() {
        M3PrimitiveArrayDeque deque = new M3PrimitiveArrayDeque(M3PrimitiveKind.INT, 1);
        deque.addLastBits(2);
        deque.addFirstBits(1);
        deque.addLastBits(3);
        assertEquals(1, deque.getFirstBits());
        assertEquals(3, deque.getLastBits());
        assertEquals(2, deque.getBits(1));
        assertTrue(deque.containsBits(2));
        assertTrue(deque.removeFirstOccurrenceBits(2));
        deque.pushBits(0);
        List<Long> observed = new ArrayList<>();
        deque.forEachBits(observed::add);
        assertEquals(List.of(0L, 1L, 3L), observed);
        assertEquals(0, deque.popBits());
        assertEquals(3, deque.removeLastBits());
        assertEquals(1, deque.removeFirstBits());
        assertTrue(deque.isEmpty());
        assertThrows(NoSuchElementException.class, deque::peekFirstBits);
    }

    @Test
    void intArrayDequePreservesPrimitiveOwnerContract() {
        M3IntArrayDeque deque = new M3IntArrayDeque(2);
        deque.addLast(2);
        deque.addFirst(1);
        deque.addAll(new int[] {3, 4, 5}, 0, 3);
        assertArrayEquals(new int[] {1, 2, 3, 4, 5}, deque.toArray());
        deque.push(0);
        assertEquals(0, deque.pop());
        assertEquals(1, deque.remove());
        assertEquals(5, deque.removeLast());
        assertEquals(2, deque.getFirst());
        assertEquals(4, deque.getLast());
        deque.clear();
        assertTrue(deque.isEmpty());
        assertThrows(NoSuchElementException.class, deque::getFirst);
    }

    @Test
    void primitiveMinMaxQueuePreservesSortedExtremaContract() {
        M3PrimitiveMinMaxQueue queue = new M3PrimitiveMinMaxQueue(M3PrimitiveKind.INT, 1);
        queue.offerBits(5);
        queue.offerBits(1);
        queue.offerBits(3);
        queue.offerBits(3);
        assertEquals(1, queue.peekMinBits());
        assertEquals(5, queue.peekMaxBits());
        assertEquals(2, queue.countBits(3));
        assertTrue(queue.removeOneBits(3));
        assertEquals(1, queue.countBits(3));
        assertEquals(1, queue.pollMinBits());
        assertEquals(5, queue.pollMaxBits());
        assertArrayEquals(new int[] {3}, (int[]) queue.toPrimitiveArray());
        queue.clear();
        assertTrue(queue.isEmpty());
        assertThrows(NoSuchElementException.class, queue::getMinBits);
    }
}
