/*
 * Copyright (c) 2026, Hitesh Soliwal (Synexia) and contributors. All rights reserved.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
package m3.swing.api;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Random;
import java.util.TreeSet;

import javax.swing.M3ItemRange;

import org.junit.jupiter.api.Test;

/** Contract of javax.swing.M3ItemRange (leaf S1): the value type and the normal form of index sets. */
public class M3ItemRangeTest {

    @Test
    public void valueAccessors() {
        M3ItemRange range = new M3ItemRange(3, 7);
        assertEquals(3, range.start());
        assertEquals(7, range.end());
        assertEquals(5, range.size());
        assertFalse(range.isEmpty());
        assertTrue(range.contains(3) && range.contains(7));
        assertFalse(range.contains(2) || range.contains(8));
        assertEquals("M3ItemRange [3..7]", range.toString());
        M3ItemRange empty = new M3ItemRange(4, 1);
        assertTrue(empty.isEmpty());
        assertEquals(0, empty.size());
        assertEquals(M3ItemRange.EMPTY, empty);
        assertEquals(M3ItemRange.EMPTY.hashCode(), empty.hashCode());
        assertEquals("M3ItemRange []", empty.toString());
        assertSame(M3ItemRange.EMPTY, M3ItemRange.of(5, 4));
        assertEquals(new M3ItemRange(2, 2), M3ItemRange.of(2, 2));
        assertEquals(range, range);
        assertNotEquals(range, M3ItemRange.of(3, 8));
        assertNotEquals(range, M3ItemRange.of(2, 7));
        assertNotEquals(range, "M3ItemRange [3..7]");
        assertNotEquals(range, M3ItemRange.EMPTY);
        assertEquals(range.hashCode(), M3ItemRange.of(3, 7).hashCode());
    }

    @Test
    public void intersectionAndCount() {
        M3ItemRange a = M3ItemRange.of(10, 20);
        assertEquals(M3ItemRange.of(15, 20), a.intersection(M3ItemRange.of(15, 30)));
        assertEquals(M3ItemRange.of(10, 12), a.intersection(M3ItemRange.of(0, 12)));
        assertTrue(a.intersection(M3ItemRange.of(21, 30)).isEmpty());
        assertTrue(a.intersection(M3ItemRange.EMPTY).isEmpty());
        assertEquals(11 + 1 + 0, M3ItemRange.countOf(new M3ItemRange[] {a, M3ItemRange.of(0, 0), M3ItemRange.EMPTY}));
        assertEquals(0, M3ItemRange.countOf(new M3ItemRange[0]));
        assertThrows(NullPointerException.class, () -> a.intersection(null));
        assertThrows(NullPointerException.class, () -> M3ItemRange.countOf(null));
        assertThrows(NullPointerException.class, () -> M3ItemRange.countOf(new M3ItemRange[] {null}));
    }

    @Test
    public void coalesceAgreesWithASetOracle() {
        Random random = new Random(1);
        for (int round = 0; round < 300; round++) {
            int[] indices = random.ints(random.nextInt(60), 0, 80).toArray();
            M3ItemRange[] ranges = M3ItemRange.coalesce(indices);
            TreeSet<Integer> expected = new TreeSet<>();
            for (int index : indices) {
                expected.add(index);
            }
            TreeSet<Integer> actual = new TreeSet<>();
            for (int i = 0; i < ranges.length; i++) {
                for (int index = ranges[i].start(); index <= ranges[i].end(); index++) {
                    actual.add(index);
                }
                if (i > 0) {
                    assertTrue(ranges[i].start() > ranges[i - 1].end() + 1, "not normal: " + Arrays.toString(ranges));
                }
            }
            assertEquals(expected, actual);
            assertEquals(expected.size(), M3ItemRange.countOf(ranges));
        }
        assertEquals(0, M3ItemRange.coalesce(new int[0]).length);
        int[] input = {5, 1, 2};
        M3ItemRange.coalesce(input);
        assertArrayEquals(new int[] {5, 1, 2}, input, "the argument is not reordered");
    }

    @Test
    public void argumentsAreChecked() {
        assertThrows(IllegalArgumentException.class, () -> new M3ItemRange(-1, 3));
        assertThrows(IllegalArgumentException.class, () -> M3ItemRange.of(-1, 3));
        assertThrows(IllegalArgumentException.class, () -> M3ItemRange.coalesce(new int[] {3, -2}));
        assertThrows(NullPointerException.class, () -> M3ItemRange.coalesce(null));
    }
}
