// SPDX-License-Identifier: Apache-2.0
package com.m3.arrays;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ReadOnlyBufferException;
import java.util.List;
import org.junit.jupiter.api.Test;

class M3ArraysTest {
    @Test
    void snapshotsDetachFromMutableInputsAndExportsAreIndependent() {
        byte[] bytes = {1, 2, 3};
        char[] chars = {'a', 'b', 'c'};
        int[] ints = {4, 5, 6};
        long[] longs = {7, 8, 9};

        M3ByteArrayView byteView = M3Arrays.bytes(bytes);
        M3Utf16ArrayView charView = M3Arrays.chars(chars);
        M3IntArrayView intView = M3Arrays.ints(ints);
        M3LongArrayView longView = M3Arrays.longs(longs);

        bytes[0] = 99;
        chars[0] = 'z';
        ints[0] = 99;
        longs[0] = 99;

        assertArrayEquals(new byte[] {1, 2, 3}, byteView.copy());
        assertArrayEquals(new char[] {'a', 'b', 'c'}, charView.copy());
        assertArrayEquals(new int[] {4, 5, 6}, intView.copy());
        assertArrayEquals(new long[] {7, 8, 9}, longView.copy());

        byte[] exported = byteView.copy();
        char[] exportedChars = charView.copy();
        exported[1] = 42;
        exportedChars[1] = 'x';
        assertEquals(2, byteView.byteAt(1));
        assertEquals('b', charView.charAt(1));

        assertThrows(
                ReadOnlyBufferException.class,
                () -> byteView.asReadOnlyBuffers()[0].put(0, (byte) 9));
        assertThrows(
                ReadOnlyBufferException.class,
                () -> charView.asReadOnlyCharBuffers()[0].put(0, 'q'));
    }

    @Test
    void utf16JoinKeepsExactHashAndCodePointFactsAcrossSurrogateSeam() {
        M3Utf16ArrayView left = M3Arrays.chars(new char[] {'A', '\ud83d'});
        M3Utf16ArrayView right = M3Arrays.chars(new char[] {'\ude00', 'B'});
        M3Utf16ArrayView joined = M3Arrays.join(left, right);
        String expected = "A\ud83d\ude00B";

        assertEquals(expected.length(), joined.length());
        assertEquals(expected.hashCode(), joined.javaHashCode());
        assertEquals(expected.codePointCount(0, expected.length()), joined.codePointCount());
        assertEquals(expected.codePointAt(1), joined.codePointAt(1));
        assertEquals(List.of((int) 'A', 0x1f600, (int) 'B'), joined.codePoints().boxed().toList());
        assertEquals(2, joined.segmentCount());
        assertEquals(expected, joined.toString());
    }

    @Test
    void slicesAndJoinsRemainCoordinateExactWithoutFlattening() {
        M3Utf16ArrayView joined =
                M3Arrays.join(
                        M3Arrays.chars("hello"),
                        M3Arrays.chars(" "),
                        M3Arrays.chars("world"));

        assertEquals(3, joined.segmentCount());
        assertEquals("hello world", joined.toString());
        assertEquals("world", joined.subSequence(6, 11).toString());
        assertEquals(6, joined.indexOf("world", 0));
        assertEquals(3, joined.indexOf("lo wo", 0));
        assertEquals(3, joined.lastIndexOf("lo", joined.length()));
        assertEquals(joined.length(), joined.indexOf("", Integer.MAX_VALUE));
        assertEquals(-1, joined.lastIndexOf("", -1));

        char[] range = new char[5];
        joined.copyTo(6, range, 0, 5);
        assertArrayEquals("world".toCharArray(), range);
    }

    @Test
    void primitiveJoinsPreserveOrderAndCheckedSlices() {
        M3IntArrayView ints = M3Arrays.join(M3Arrays.ints(new int[] {1, 2}), M3Arrays.ints(new int[] {3, 4}));
        M3LongArrayView longs = M3Arrays.join(M3Arrays.longs(new long[] {5, 6}), M3Arrays.longs(new long[] {7}));

        assertArrayEquals(new int[] {1, 2, 3, 4}, ints.copy());
        assertArrayEquals(new int[] {2, 3}, ints.slice(1, 3).copy());
        assertArrayEquals(new long[] {5, 6, 7}, longs.copy());
        assertArrayEquals(new long[] {6, 7}, longs.slice(1, 3).copy());
        assertThrows(IndexOutOfBoundsException.class, () -> ints.intAt(4));
        assertThrows(IndexOutOfBoundsException.class, () -> longs.slice(-1, 1));
    }

    @Test
    void byteJoinUsesScatterGatherReadOnlyDescriptors() {
        M3ByteArrayView view =
                M3Arrays.join(
                        M3Arrays.bytes(new byte[] {1, 2}),
                        M3Arrays.bytes(new byte[0]),
                        M3Arrays.bytes(new byte[] {3, 4, 5}));

        assertEquals(5, view.length());
        assertEquals(2, view.segmentCount());
        assertArrayEquals(new byte[] {1, 2, 3, 4, 5}, view.copy());
        assertArrayEquals(new byte[] {2, 3, 4}, view.slice(1, 4).copy());
        assertEquals(2, view.asReadOnlyBuffers().length);
    }

    @Test
    void precomputedFactCompositionMatchesDirectComputation() {
        for (String left : List.of("", "abc", "\ud83d", "é", "A\ud83d")) {
            for (String right : List.of("", "xyz", "\ude00", "ß", "\ude00B")) {
                M3Utf16Facts composed =
                        M3Utf16Facts.combine(
                                M3Arrays.chars(left).facts(),
                                M3Arrays.chars(right).facts());
                M3Utf16Facts direct = M3Utf16Facts.of(left + right);
                assertEquals(direct, composed, left + " + " + right);
            }
        }
    }
}
