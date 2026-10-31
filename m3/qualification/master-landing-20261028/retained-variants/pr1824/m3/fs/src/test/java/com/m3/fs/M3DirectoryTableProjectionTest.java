/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

/** Contract oracles over the original native-directory column owner. */
final class M3DirectoryTableProjectionTest {
    private static final M3FS.Kind[] KINDS = M3FS.Kind.values();

    private static M3DirectoryTable table(M3FS.Kind[] kinds, long[] sizes) {
        assertEquals(kinds.length, sizes.length);
        List<M3FS.DirEntry> rows = new ArrayList<>();
        for (int i = 0; i < sizes.length; i++) {
            rows.add(new M3FS.DirEntry("n" + i,
                    new M3FS.Stat(i + 1L, sizes[i], 1L, 0L, kinds[i], 0644, 1, 4096)));
        }
        return M3DirectoryTable.fromEntries(rows);
    }

    private static M3DirectoryTable known() {
        return table(new M3FS.Kind[]{M3FS.Kind.FILE, M3FS.Kind.DIRECTORY,
                M3FS.Kind.FILE, M3FS.Kind.SYMLINK, M3FS.Kind.DIRECTORY,
                M3FS.Kind.FILE, M3FS.Kind.FILE},
                new long[]{9, 6, 1, 6, 3, Long.MAX_VALUE, 0});
    }

    @Test void maskFilteredRowsRetainAbsoluteSlotsAndNativeOwnership() {
        M3DirectoryTable t = known();
        assertArrayEquals(new int[]{0, 2, 5, 6}, t.selectedRows(M3DirectoryTable.KIND_FILE));
        assertArrayEquals(new int[]{2, 5}, t.selectedRows(1, 5, M3DirectoryTable.KIND_FILE));
        assertArrayEquals(new long[]{101L}, t.selectedMaskWords(M3DirectoryTable.KIND_FILE));
        assertArrayEquals(new long[]{36L}, t.selectedMaskWords(1, 5, M3DirectoryTable.KIND_FILE));
        assertArrayEquals(new int[]{1, 4},
                t.selectedRows(M3DirectoryTable.KIND_DIRECTORY));
        assertArrayEquals(new int[]{3}, t.selectedRows(M3DirectoryTable.KIND_SYMLINK));
        assertEquals("n0", t.name(0));
        assertEquals(Long.MAX_VALUE, t.size(5));
        assertEquals(M3FS.Kind.SYMLINK, t.kind(3));
        assertEquals(7, t.entries().size()); // Prior API remains in source physical order.
    }

    @Test void stableIndexSortNeverReordersParentPayloadOrEqualSizes() {
        M3DirectoryTable t = known();
        assertArrayEquals(new int[]{6, 2, 4, 1, 3, 0, 5},
                t.rowsBySize(M3DirectoryTable.KIND_ALL, false));
        assertArrayEquals(new int[]{5, 0, 1, 3, 4, 2, 6},
                t.rowsBySize(M3DirectoryTable.KIND_ALL, true));
        assertArrayEquals(new int[]{6, 2, 0, 5},
                t.rowsBySize(M3DirectoryTable.KIND_FILE, false));
        assertArrayEquals(new int[]{2, 5},
                t.rowsBySize(1, 5, M3DirectoryTable.KIND_FILE, false));
        assertEquals(List.of("n6", "n2", "n4", "n1", "n3", "n0", "n5"),
                t.entriesBySize(M3DirectoryTable.KIND_ALL, false).stream()
                        .map(M3FS.DirEntry::name).toList());
        assertEquals(List.of("n0", "n1", "n2", "n3", "n4", "n5", "n6"),
                t.entries().stream().map(M3FS.DirEntry::name).toList());
    }

    @Test void readOnlyIndexProjectionDetachesCallerArrayButNotElementPayloads() {
        M3DirectoryTable t = known();
        int[] indexes = {2, 0, 2};
        List<M3FS.DirEntry> view = t.entriesByRows(indexes);
        indexes[0] = 6;
        assertEquals(List.of("n2", "n0", "n2"),
                view.stream().map(M3FS.DirEntry::name).toList());
        assertNotSame(view.get(0), view.get(0)); // Public records materialize on demand.
        assertEquals("n2", view.reversed().get(0).name());
        assertThrows(UnsupportedOperationException.class, view::clear);
        assertThrows(UnsupportedOperationException.class, () -> view.add(view.get(0)));
        assertThrows(UnsupportedOperationException.class, () -> view.removeIf(x -> false));
        assertThrows(UnsupportedOperationException.class,
                () -> view.listIterator().remove());
        assertThrows(UnsupportedOperationException.class,
                () -> t.entriesOfKind(0).clear());
        assertThrows(IndexOutOfBoundsException.class, () -> t.entriesByRows(new int[]{0, 7}));
        assertThrows(IndexOutOfBoundsException.class, () -> t.entriesByRows(new int[]{-1}));
        assertThrows(NullPointerException.class, () -> t.entriesByRows(null));
    }

    @Test void emptyWindowsInvalidMasksAndOutOfBoundsFailBeforePublication() {
        M3DirectoryTable t = table(new M3FS.Kind[0], new long[0]);
        assertArrayEquals(new int[0], t.selectedRows(M3DirectoryTable.KIND_ALL));
        assertArrayEquals(new long[0], t.selectedMaskWords(M3DirectoryTable.KIND_ALL));
        assertEquals(List.of(), t.entriesBySize(M3DirectoryTable.KIND_ALL, true));
        M3DirectoryTable actual = known();
        assertArrayEquals(new int[0], actual.selectedRows(7, 0, M3DirectoryTable.KIND_ALL));
        assertArrayEquals(new long[]{0}, actual.selectedMaskWords(7, 0, M3DirectoryTable.KIND_ALL));
        assertArrayEquals(new int[0], actual.rowsBySize(0, false));
        assertThrows(IllegalArgumentException.class, () -> actual.selectedRows(8));
        assertThrows(IllegalArgumentException.class, () -> actual.selectedMaskWords(-1));
        assertThrows(IllegalArgumentException.class, () -> actual.rowsBySize(9, false));
        assertThrows(IndexOutOfBoundsException.class, () -> actual.selectedRows(8, 0, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> actual.selectedMaskWords(-1, 1, 1));
        assertThrows(IndexOutOfBoundsException.class,
                () -> actual.rowsBySize(2, Integer.MAX_VALUE, M3DirectoryTable.KIND_ALL, true));
    }

    @Test void nativeUtf8DirectoryCodecFeedsTheSameIndexAndMaskSelectors() {
        byte[] firstName = "é".getBytes(StandardCharsets.UTF_8);
        byte[] secondName = "𐐷".getBytes(StandardCharsets.UTF_8);
        ByteBuffer data = ByteBuffer.allocate(2 * (4 + 48) + firstName.length + secondName.length)
                .order(ByteOrder.LITTLE_ENDIAN);
        data.putInt(firstName.length).put(firstName);
        data.putInt(1).putInt(0644).putInt(1).putInt(4096);
        data.putLong(7).putLong(10).putLong(1).putLong(3);
        data.putInt(secondName.length).put(secondName);
        data.putInt(2).putInt(0755).putInt(1).putInt(4096);
        data.putLong(8).putLong(1).putLong(1).putLong(4);
        data.flip();
        M3DirectoryTable t = M3DirectoryTable.decode(data);
        assertEquals("é", t.name(0));
        assertEquals("𐐷", t.name(1));
        assertArrayEquals(new int[]{0}, t.selectedRows(M3DirectoryTable.KIND_FILE));
        assertArrayEquals(new long[]{2}, t.selectedMaskWords(M3DirectoryTable.KIND_DIRECTORY));
        assertArrayEquals(new int[]{1, 0}, t.rowsBySize(M3DirectoryTable.KIND_ALL, false));
        assertEquals(List.of("𐐷", "é"), t.entriesBySize(M3DirectoryTable.KIND_ALL, false)
                .stream().map(M3FS.DirEntry::name).toList());
    }

    @Test void randomizedReferenceComparisonForKindMasksWindowsAndStableSorting() {
        Random random = new Random(0x4D334453);
        for (int trial = 0; trial < 520; trial++) {
            int count = trial < 9 ? new int[]{0, 1, 2, 63, 64, 65, 127, 128, 129}[trial]
                    : random.nextInt(210);
            M3FS.Kind[] kinds = new M3FS.Kind[count];
            long[] sizes = new long[count];
            for (int i = 0; i < count; i++) {
                kinds[i] = KINDS[random.nextInt(KINDS.length)];
                sizes[i] = random.nextInt(29) == 0 ? Long.MAX_VALUE : random.nextInt(25);
            }
            M3DirectoryTable t = table(kinds, sizes);
            int first = random.nextInt(count + 1);
            int length = random.nextInt(count - first + 1);
            int mask = random.nextInt(8);
            List<Integer> reference = new ArrayList<>();
            long[] expectedMask = new long[(count + 63) >>> 6];
            for (int i = first; i < first + length; i++) {
                if ((mask & (1 << kinds[i].ordinal())) != 0) {
                    reference.add(i);
                    expectedMask[i >>> 6] |= 1L << (i & 63);
                }
            }
            assertArrayEquals(reference.stream().mapToInt(Integer::intValue).toArray(),
                    t.selectedRows(first, length, mask));
            assertArrayEquals(expectedMask, t.selectedMaskWords(first, length, mask));
            for (boolean descending : new boolean[]{false, true}) {
                List<Integer> sorted = new ArrayList<>(reference);
                sorted.sort((a, b) -> {
                    int value = Long.compare(sizes[a], sizes[b]);
                    if (descending) value = -value;
                    return value != 0 ? value : Integer.compare(a, b);
                });
                int[] selected = t.rowsBySize(first, length, mask, descending);
                assertArrayEquals(sorted.stream().mapToInt(Integer::intValue).toArray(), selected);
                assertEquals(sorted.stream().map(i -> "n" + i).toList(),
                        t.entriesByRows(selected).stream().map(M3FS.DirEntry::name).toList());
            }
            assertEquals(count, t.entries().size());
            assertEquals(count, t.size());
        }
    }
}
