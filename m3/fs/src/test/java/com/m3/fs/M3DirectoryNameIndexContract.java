// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/** Original-owner Java21 oracle for primitive UTF-16 directory indexes. */
public final class M3DirectoryNameIndexContract {
    private static int checks;
    private M3DirectoryNameIndexContract() { }

    private static void check(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError(reason);
    }

    private static void compare(M3DirectoryTable table, List<M3FS.DirEntry> entries,
            int mask, String prefix) {
        var index = table.nameIndex(mask);
        Integer[] expected = new Integer[entries.size()];
        int used = 0;
        for (int row = 0; row < entries.size(); row++) {
            if ((mask & (1 << entries.get(row).stat().kind().ordinal())) != 0) {
                expected[used++] = row;
            }
        }
        expected = Arrays.copyOf(expected, used);
        // JDK test oracle only: stable Integer[] comparator sort may box.
        Arrays.sort(expected, (a, b) ->
                entries.get(a).name().compareTo(entries.get(b).name()));
        check(index.size() == expected.length, "sorted index row count");
        for (int rank = 0; rank < expected.length; rank++) {
            check(index.row(rank) == expected[rank], "UTF-16 order and stable ties");
        }
        int selected = 0;
        for (Integer row : expected) {
            if (entries.get(row).name().startsWith(prefix)) selected++;
        }
        int[] expectedPrefix = new int[selected];
        int at = 0;
        for (Integer row : expected) {
            if (entries.get(row).name().startsWith(prefix)) expectedPrefix[at++] = row;
        }
        check(Arrays.equals(expectedPrefix, index.rowsWithPrefix(prefix)),
                "binary prefix matches String.startsWith");
        var projected = index.entriesWithPrefix(prefix);
        check(projected.size() == expectedPrefix.length, "lazy prefix view size");
        for (int rank = 0; rank < projected.size(); rank++) {
            check(projected.get(rank).equals(entries.get(expectedPrefix[rank])),
                    "lazy original DirEntry value");
        }
        int[] detached = index.rows();
        if (detached.length > 0) {
            detached[0] = -1;
            check(index.row(0) >= 0, "detached sorted row array");
        }
        detached = index.rowsWithPrefix(prefix);
        if (detached.length > 0) {
            detached[0] = -1;
            check(Arrays.equals(expectedPrefix, index.rowsWithPrefix(prefix)),
                    "detached selected prefix rows");
        }
        boolean immutable = false;
        try {
            projected.clear();
        } catch (UnsupportedOperationException expectedException) {
            immutable = true;
        }
        check(immutable, "unmodifiable prefix view, including empty");
    }

    private static String randomName(Random random) {
        String alphabet = "abcXY01_-.é🔎";
        int length = 1 + random.nextInt(10);
        StringBuilder name = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            name.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return name.toString();
    }

    private static M3FS.DirEntry entry(String name, M3FS.Kind kind, long inode) {
        return new M3FS.DirEntry(name,
                new M3FS.Stat(inode, inode & 63, 1L, 0L, kind, 0644, 1, 4096));
    }

    private static void seededOracle() {
        Random random = new Random(20261022L);
        M3FS.Kind[] kinds = M3FS.Kind.values();
        for (int trial = 0; trial < 75; trial++) {
            int count = random.nextInt(320);
            List<M3FS.DirEntry> rows = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                String name = i > 0 && random.nextInt(5) == 0
                        ? rows.get(random.nextInt(i)).name() : randomName(random);
                rows.add(entry(name, kinds[random.nextInt(kinds.length)], i + 1));
            }
            M3DirectoryTable table = M3DirectoryTable.fromEntries(rows);
            for (int mask = 0; mask < 8; mask++) {
                compare(table, rows, mask, "");
                compare(table, rows, mask, "zzzz");
                compare(table, rows, mask, "\uD800");
                for (int query = 0; query < 12; query++) {
                    String prefix = count == 0 ? "" : rows.get(random.nextInt(count)).name();
                    prefix = prefix.substring(0, random.nextInt(prefix.length() + 1));
                    compare(table, rows, mask, prefix);
                }
            }
            check(table.entries().equals(rows), "parent physical encounter order unchanged");
        }
    }

    private static void edges() {
        List<M3FS.DirEntry> rows = List.of(
                entry("a", M3FS.Kind.FILE, 1), entry("ab", M3FS.Kind.DIRECTORY, 2),
                entry("abc", M3FS.Kind.FILE, 3), entry("a🔎", M3FS.Kind.SYMLINK, 4),
                entry("b", M3FS.Kind.DIRECTORY, 5), entry("ab", M3FS.Kind.FILE, 6),
                entry("\uD800", M3FS.Kind.SYMLINK, 7));
        var table = M3DirectoryTable.fromEntries(rows);
        for (String prefix : List.of("", "a", "ab", "abc", "a🔎", "b", "z", "\uD800")) {
            compare(table, rows, M3DirectoryTable.KIND_ALL, prefix);
        }
        check(table.nameIndex(0).size() == 0, "empty kind mask");
        try {
            table.nameIndex(8);
            throw new AssertionError("unknown kind mask allowed");
        } catch (IllegalArgumentException expected) { checks++; }
        try {
            table.nameIndex(7).rowsWithPrefix(null);
            throw new AssertionError("null prefix allowed");
        } catch (NullPointerException expected) { checks++; }
        try {
            table.nameIndex(7).row(-1);
            throw new AssertionError("negative sorted rank allowed");
        } catch (IndexOutOfBoundsException expected) { checks++; }
        try {
            table.nameIndex(7).row(7);
            throw new AssertionError("out-of-bounds sorted rank allowed");
        } catch (IndexOutOfBoundsException expected) { checks++; }

        byte[] utf8a = "é".getBytes(StandardCharsets.UTF_8);
        byte[] utf8b = "𐐷".getBytes(StandardCharsets.UTF_8);
        ByteBuffer nativeRows = ByteBuffer.allocate(104 + utf8a.length + utf8b.length)
                .order(ByteOrder.LITTLE_ENDIAN);
        nativeRows.putInt(utf8a.length).put(utf8a);
        nativeRows.putInt(1).putInt(0644).putInt(1).putInt(4096);
        nativeRows.putLong(1).putLong(10).putLong(1).putLong(0);
        nativeRows.putInt(utf8b.length).put(utf8b);
        nativeRows.putInt(2).putInt(0755).putInt(1).putInt(4096);
        nativeRows.putLong(2).putLong(20).putLong(1).putLong(0);
        nativeRows.flip();
        var decoded = M3DirectoryTable.decode(nativeRows);
        check(decoded.nameIndex(7).entriesWithPrefix("𐐷").get(0).name().equals("𐐷"),
                "native UTF-8 row, supplementary prefix UTF-16");
        check(Arrays.equals(new int[]{0}, decoded.nameIndex(7).rowsWithPrefix("é")),
                "native UTF-8 row, BMP prefix");
    }

    public static void main(String[] args) {
        seededOracle();
        edges();
        System.out.println("M3_FS_NAME_INDEX_PASS checks=" + checks);
    }
}
