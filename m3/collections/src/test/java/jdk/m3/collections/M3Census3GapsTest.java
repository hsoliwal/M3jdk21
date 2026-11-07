// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

/**
 * Census pass 3 restored members of leaves P3-006 (M3MonotoneLongDeque) and P3-009 (M3PersistentLongSumTree),
 * donor com.synexia develop 78cf2ad4c7e5. Every test names members absent on master (test compilation fails there).
 */
//FLLDCIM: MRL - census3 P3-006/P3-009 restored-member tests (qualification/census3-gaps-20261107)
class M3Census3GapsTest {

    @Test
    void monotoneTailIsTheLatestOffer() {
        M3MonotoneLongDeque max = new M3MonotoneLongDeque(4, M3MonotoneLongDeque.Mode.MAX);
        max.offer(0, 5L);
        max.offer(1, 3L);
        assertEquals(1L, max.lastIndex());
        assertEquals(3L, max.lastValue());
        max.offer(2, 3L);
        assertEquals(2L, max.lastIndex());
        assertEquals(3L, max.lastValue());
        max.offer(3, 9L);
        assertEquals(3L, max.firstIndex());
        assertEquals(3L, max.lastIndex());
        assertEquals(9L, max.lastValue());

        M3MonotoneLongDeque min = new M3MonotoneLongDeque(3, M3MonotoneLongDeque.Mode.MIN);
        min.offer(10, 4L);
        min.offer(11, 7L);
        assertEquals(11L, min.lastIndex());
        assertEquals(7L, min.lastValue());
        assertEquals(4L, min.firstValue());
    }

    @Test
    void monotoneTailOfAnEmptyDequeThrows() {
        M3MonotoneLongDeque deque = new M3MonotoneLongDeque(2, M3MonotoneLongDeque.Mode.MIN);
        assertEquals("empty monotone deque",
                assertThrows(NoSuchElementException.class, deque::lastIndex).getMessage());
        deque.offer(4, 1L);
        deque.evictBefore(5);
        assertEquals("empty monotone deque",
                assertThrows(NoSuchElementException.class, deque::lastValue).getMessage());
    }

    @Test
    void forkAddCreatesVersionsAndKeepsTheBase() {
        M3PersistentLongSumTree tree = new M3PersistentLongSumTree(8, 4);
        assertEquals(8, tree.length());
        assertEquals(0, tree.rootNode(0));
        int v1 = tree.forkAdd(0, 3, 5L);
        int v2 = tree.forkAdd(v1, 6, -2L);
        int v3 = tree.forkAdd(0, 3, 7L);
        assertEquals(1, v1);
        assertEquals(2, v2);
        assertEquals(3, v3);
        assertEquals(0L, tree.total(0));
        assertEquals(5L, tree.total(v1));
        assertEquals(3L, tree.total(v2));
        assertEquals(-2L, tree.rangeSum(v2, 4, 8));
        assertEquals(7L, tree.get(v3, 3));
        assertEquals(0L, tree.get(v3, 6));
        // a path of 4 nodes per update (length 8): node ids follow the donor numbering 1..12
        assertEquals(4, tree.rootNode(v1));
        assertEquals(8, tree.rootNode(v2));
        assertEquals(12, tree.rootNode(v3));
    }

    @Test
    void zeroDeltaSharesTheBaseRoot() {
        M3PersistentLongSumTree tree = new M3PersistentLongSumTree(5);
        int v1 = tree.forkAdd(0, 2, 9L);
        int nodes = tree.nodeCount();
        int v2 = tree.forkAdd(v1, 4, 0L);
        assertEquals(tree.rootNode(v1), tree.rootNode(v2));
        assertEquals(nodes, tree.nodeCount());
        assertEquals(3, tree.versionCount());
    }

    @Test
    void overflowRefusesBeforeAnyStateChange() {
        M3PersistentLongSumTree tree = new M3PersistentLongSumTree(4, 1);
        int v1 = tree.forkAdd(0, 1, Long.MAX_VALUE);
        int nodes = tree.nodeCount();
        int versions = tree.versionCount();
        assertThrows(ArithmeticException.class, () -> tree.forkAdd(v1, 2, 1L));
        assertEquals(nodes, tree.nodeCount());
        assertEquals(versions, tree.versionCount());
        assertEquals(Long.MAX_VALUE, tree.total(v1));
    }

    @Test
    void presizingConstructorAndRootNodeValidate() {
        assertThrows(IllegalArgumentException.class, () -> new M3PersistentLongSumTree(0, 1));
        assertThrows(IllegalArgumentException.class, () -> new M3PersistentLongSumTree(3, -1));
        M3PersistentLongSumTree tree = new M3PersistentLongSumTree(1, 0);
        assertEquals(1, tree.length());
        assertThrows(IndexOutOfBoundsException.class, () -> tree.rootNode(1));
        assertThrows(IndexOutOfBoundsException.class, () -> tree.forkAdd(0, 1, 1L));
        assertThrows(IndexOutOfBoundsException.class, () -> tree.forkAdd(2, 0, 1L));
        assertEquals(1, tree.forkAdd(0, 0, 1L));
        assertEquals(1L, tree.total(1));
    }
}
