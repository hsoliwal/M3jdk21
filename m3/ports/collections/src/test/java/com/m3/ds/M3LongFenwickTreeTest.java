// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.ds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class M3LongFenwickTreeTest {
    @Test
    void exactQueriesAndMutationsMatchTheDocumentedFenwickContract() {
        M3LongFenwickTree tree = new M3LongFenwickTree(new long[] {1, 2, 3, 4, 5});
        assertEquals(5, tree.size());
        assertEquals(3, tree.point(2));
        assertEquals(6, tree.prefixSum(3));
        assertEquals(9, tree.rangeSum(1, 4));
        assertEquals(15, tree.total());

        assertEquals(0, tree.lowerBound(1));
        assertEquals(1, tree.lowerBound(2));
        assertEquals(1, tree.lowerBound(3));
        assertEquals(2, tree.lowerBound(4));
        assertEquals(4, tree.lowerBound(15));
        assertEquals(5, tree.lowerBound(16));

        tree.add(2, 7);
        assertEquals(10, tree.point(2));
        assertEquals(22, tree.total());
        tree.set(4, 1);
        assertEquals(18, tree.total());

        tree.clear();
        assertEquals(0, tree.total());
        assertEquals(0, tree.point(0));
        assertEquals(0, tree.point(4));
    }

    @Test
    void lowerBoundRejectsNegativePoints() {
        M3LongFenwickTree tree = new M3LongFenwickTree(new long[] {1, 2});
        tree.set(0, -1);
        assertThrows(IllegalStateException.class, () -> tree.lowerBound(1));
    }

    @Test
    void mutationOverflowIsFailureAtomic() {
        M3LongFenwickTree tree = new M3LongFenwickTree(new long[] {Long.MAX_VALUE, 0});
        assertThrows(ArithmeticException.class, () -> tree.add(1, 1));
        assertEquals(Long.MAX_VALUE, tree.point(0));
        assertEquals(0, tree.point(1));
        assertEquals(Long.MAX_VALUE, tree.total());
    }
}
