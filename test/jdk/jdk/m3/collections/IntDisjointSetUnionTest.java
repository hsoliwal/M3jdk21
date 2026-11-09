/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** API_UNION U4: IntDisjointSet is one interface; the Synexia contract and the M3 members meet on it. */
class IntDisjointSetUnionTest {

    @Test
    void synexiaContractMembers() {
        IntDisjointSet sets = IntDisjointSet.of(6);
        assertEquals(6, sets.elements());
        assertEquals(sets.elements(), sets.size());
        assertEquals(6, sets.components());
        assertTrue(sets.union(0, 1));
        assertTrue(sets.union(2, 1));
        assertFalse(sets.union(0, 2));
        assertTrue(sets.connected(0, 2));
        assertFalse(sets.connected(0, 3));
        assertEquals(3, sets.componentSize(2));
        assertEquals(sets.find(0), sets.find(2));
        assertEquals(4, sets.components());
        assertEquals(6L * Integer.BYTES * 2L, sets.payloadBytes());
        sets.reset();
        assertEquals(6, sets.components());
        assertEquals(1, sets.componentSize(2));
        assertThrows(IndexOutOfBoundsException.class, () -> sets.find(6));
        assertThrows(IllegalArgumentException.class, () -> IntDisjointSet.of(-1));
    }

    @Test
    void sizeDefaultOnAnotherImplementation() {
        IntDisjointSet inner = IntDisjointSet.of(3);
        IntDisjointSet foreign = new IntDisjointSet() {
            @Override public int elements() { return inner.elements(); }
            @Override public int components() { return inner.components(); }
            @Override public int find(int v) { return inner.find(v); }
            @Override public int componentSize(int v) { return inner.componentSize(v); }
            @Override public boolean connected(int l, int r) { return inner.connected(l, r); }
            @Override public boolean union(int l, int r) { return inner.union(l, r); }
            @Override public void reset() { inner.reset(); }
            @Override public long payloadBytes() { return inner.payloadBytes(); }
        };
        assertEquals(3, foreign.size());
        assertTrue(foreign.union(0, 2));
        assertEquals(2, foreign.components());
    }
}
