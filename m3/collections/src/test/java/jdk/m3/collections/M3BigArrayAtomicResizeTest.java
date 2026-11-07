/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class M3BigArrayAtomicResizeTest {
    @Test void failingNewSegmentDoesNotPublishAnyDirectoryChanges() {
        BigArray array = new BigArray(PrimitiveKind.LONG, 1_025, 10);
        array.setBits(0, 100);
        array.setBits(1_024, 200);
        Object first = array.segmentArray(0);
        Object last = array.segmentArray(1);
        BigArray.ResizeOps failure = new BigArray.ResizeOps() {
            public Object newSegment(PrimitiveKind kind, int length) {
                throw new IllegalStateException("synthetic segment allocation failure");
            }
            public Object copySegment(PrimitiveKind kind, Object source, int length) {
                return kind.copyOf(source, length);
            }
        };
        assertThrows(IllegalStateException.class, () -> array.resize(3_000, failure));
        assertEquals(1_025L, array.length());
        assertEquals(2, array.segmentCount());
        assertSame(first, array.segmentArray(0));
        assertSame(last, array.segmentArray(1));
        assertEquals(100L, array.getBits(0));
        assertEquals(200L, array.getBits(1_024));
        array.resize(3_000);
        assertEquals(3_000L, array.length());
        assertEquals(200L, array.getBits(1_024));
    }

    @Test void failingShortTailCopyDoesNotChangeExistingGeometry() {
        BigArray array = new BigArray(PrimitiveKind.LONG, 1_025, 10);
        array.setBits(1_024, 12345);
        Object old = array.segmentArray(1);
        BigArray.ResizeOps failure = new BigArray.ResizeOps() {
            public Object newSegment(PrimitiveKind kind, int length) {
                return kind.newArray(length);
            }
            public Object copySegment(PrimitiveKind kind, Object source, int length) {
                throw new IllegalStateException("synthetic tail resize failure");
            }
        };
        assertThrows(IllegalStateException.class, () -> array.resize(1_500, failure));
        assertEquals(1_025L, array.length());
        assertSame(old, array.segmentArray(1));
        assertEquals(12345L, array.getBits(1_024));
        array.resize(1_500);
        assertEquals(12345L, array.getBits(1_024));
    }

    @Test void ordinaryWindowResizePreservesIndexAndElementContracts() {
        BigArray array = new BigArray(PrimitiveKind.LONG, 3, 10);
        array.setBits(0, 99);
        array.setBits(2, 101);
        array.resize(2_100);
        assertEquals(99L, array.getBits(0));
        assertEquals(101L, array.getBits(2));
        array.setBits(2_050, -17);
        assertEquals(-17L, array.getBits(2_050));
        array.resize(1_030);
        assertEquals(101L, array.getBits(2));
        array.resize(0);
        assertEquals(0L, array.length());
    }
}
