// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/com/synexia/m3/search/KmpByteBufferAbsoluteParityTest.java (blob a11524248411) by M3_SEARCH_RELOCATE v1.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Exact KMP buffer-offset and caller-state parity without duplicate-buffer ownership. */
final class KmpByteBufferAbsoluteParityTest {
    @Test
    void randomizedByteArraysAndAllBufferViewsMatchIndependentReference() {
        Random random = new Random(0x4D334B4D50425546L);
        long comparisons = 0L;
        for (int trial = 0; trial < 5_000; trial++) {
            int length = random.nextInt(280);
            byte[] text = new byte[length];
            byte[] pattern = new byte[1 + random.nextInt(28)];
            int alphabet = switch (trial % 5) {
                case 0 -> 1;
                case 1 -> 2;
                case 2 -> 4;
                case 3 -> 16;
                default -> 256;
            };
            for (int i = 0; i < text.length; i++) text[i] = (byte) random.nextInt(alphabet);
            for (int i = 0; i < pattern.length; i++) pattern[i] = (byte) random.nextInt(alphabet);
            if (length >= pattern.length && trial % 3 == 0) {
                System.arraycopy(pattern, 0, text,
                        random.nextInt(length - pattern.length + 1), pattern.length);
            }
            try (M3CompiledSearch search = new M3KmpSearch(pattern)) {
                assertEquals("KMP", search.strategy());
                for (boolean direct : new boolean[] {false, true}) {
                    ByteBuffer original = direct
                            ? ByteBuffer.allocateDirect(length + 11)
                            : ByteBuffer.allocate(length + 11);
                    for (int i = 0; i < length; i++) original.put(i + 4, text[i]);
                    original.position(4);
                    original.limit(length + 4);
                    original.mark();
                    for (ByteBuffer view : new ByteBuffer[] {
                            original, original.asReadOnlyBuffer(), original.slice()}) {
                        int position = view.position(), limit = view.limit();
                        for (int from : new int[] {Integer.MIN_VALUE, -1, 0, 1,
                                length / 2, length - 1, length, Integer.MAX_VALUE}) {
                            assertEquals(firstOracle(text, pattern, from), search.indexOf(view, from),
                                    "first trial=" + trial + " from=" + from + " direct=" + direct);
                            assertEquals(lastOracle(text, pattern, from), search.lastIndexOf(view, from),
                                    "last trial=" + trial + " from=" + from + " direct=" + direct);
                            assertEquals(position, view.position(), "position must remain unchanged");
                            assertEquals(limit, view.limit(), "limit must remain unchanged");
                            comparisons += 2;
                        }
                    }
                    original.reset();
                    assertEquals(4, original.position(), "mark must remain unchanged");
                }
            }
        }
        assertEquals(480_000L, comparisons);
    }

    @Test
    void periodicOverlapsAndBoundsRetainOriginalKmpIndexPolicy() {
        byte[] text = {1, 2, 1, 2, 1, 2, 1, 2, 1};
        byte[] pattern = {1, 2, 1};
        try (M3CompiledSearch search = new M3KmpSearch(pattern)) {
            for (ByteBuffer view : new ByteBuffer[] {
                    ByteBuffer.wrap(text), ByteBuffer.wrap(text).asReadOnlyBuffer()}) {
                assertEquals(0, search.indexOf(view, Integer.MIN_VALUE));
                assertEquals(2, search.indexOf(view, 1));
                assertEquals(6, search.lastIndexOf(view, Integer.MAX_VALUE));
                assertEquals(4, search.lastIndexOf(view, 5));
                assertEquals(-1, search.lastIndexOf(view, -1));
                assertEquals(-1, search.indexOf(view, text.length));
            }
        }
    }

    @Test
    void immutableCompiledPatternKeepsOriginalSnapshotAndNullPolicy() {
        byte[] needle = {0, -1, 0};
        try (M3CompiledSearch compiled = new M3KmpSearch(needle)) {
            Arrays.fill(needle, (byte) 9);
            ByteBuffer source = ByteBuffer.allocateDirect(8);
            source.put(2, (byte) 0);
            source.put(3, (byte) -1);
            source.put(4, (byte) 0);
            source.position(2);
            source.limit(5);
            assertEquals(0, compiled.indexOf(source, 0));
            assertEquals(0, compiled.lastIndexOf(source, Integer.MAX_VALUE));
            assertEquals(2, source.position());
            assertEquals(5, source.limit());
            assertThrows(NullPointerException.class,
                    () -> compiled.indexOf((ByteBuffer) null, 0));
            assertThrows(NullPointerException.class,
                    () -> compiled.lastIndexOf((ByteBuffer) null, 0));
        }
        assertThrows(IllegalArgumentException.class, () -> new M3KmpSearch(new byte[0]));
    }

    private static int firstOracle(byte[] text, byte[] needle, int from) {
        for (int index = Math.max(0, from); index <= text.length - needle.length; index++) {
            int cursor = 0;
            while (cursor < needle.length && text[index + cursor] == needle[cursor]) cursor++;
            if (cursor == needle.length) return index;
        }
        return -1;
    }

    private static int lastOracle(byte[] text, byte[] needle, int from) {
        for (int index = Math.min(from, text.length - needle.length); index >= 0; index--) {
            int cursor = 0;
            while (cursor < needle.length && text[index + cursor] == needle[cursor]) cursor++;
            if (cursor == needle.length) return index;
        }
        return -1;
    }
}
