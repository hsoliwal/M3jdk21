// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Independent JDK StandardCharsets oracle, no native fallback or external JAR. */
public final class M3Utf8PackedRowsSelfTest {
    private static int checks;
    private M3Utf8PackedRowsSelfTest() { }

    private static void check(List<String> rows) {
        M3Utf8PackedRows.Packed packed = M3Utf8PackedRows.pack(rows);
        int[] starts = packed.offsets();
        if (starts.length != rows.size() + 1 || starts[0] != 0
                || starts[rows.size()] != packed.bytes().length) throw new AssertionError("geometry");
        checks++;
        for (int i = 0; i < rows.size(); i++) {
            byte[] expected = rows.get(i).getBytes(StandardCharsets.UTF_8);
            byte[] actual = Arrays.copyOfRange(packed.bytes(), starts[i], starts[i + 1]);
            if (!Arrays.equals(actual, expected))
                throw new AssertionError("UTF-8 mismatch at row " + i + " case " + checks);
            if (starts[i] > starts[i + 1]) throw new AssertionError("nonmonotone offsets");
            checks++;
        }
    }

    private static void refuses(List<String> input) {
        try { M3Utf8PackedRows.pack(input); }
        catch (IllegalArgumentException | NullPointerException expected) { checks++; return; }
        throw new AssertionError("missing refusal");
    }

    public static void main(String[] args) {
        check(List.of());
        check(List.of(""));
        check(List.of("", "", "a", "é", "中", "💠", "e\u0301", "a\0b", "\r\n", "👩🏽‍💻"));
        check(List.of("a".repeat(8191), "x".repeat(8192), "💠".repeat(8193)));
        check(List.of("A".repeat(65535), "B".repeat(65536), "中".repeat(32769)));
        refuses(List.of("\uD800"));
        refuses(List.of("\uDC00"));
        refuses(List.of("a\uD800b"));
        refuses(List.of("a\uDC00b"));
        refuses(List.of("\uD83D\uD83D\uDE80"));
        refuses(Arrays.asList("a", null));
        refuses(null);
        List<String> mutable = new ArrayList<>(List.of("a", "💠"));
        M3Utf8PackedRows.Packed frozen = M3Utf8PackedRows.pack(mutable);
        mutable.set(0, "changed");
        if (frozen.bytes()[0] != 'a' || frozen.offsets()[1] != 1) throw new AssertionError("not frozen");
        checks++;
        Random random = new Random(0x4d3355544638504bL);
        String[] atoms = {"", "a", "b", "a\0b", "é", "中", "🌍", "👩🏽‍💻", "e\u0301", "\r", "\n", "x".repeat(2048)};
        for (int trial = 0; trial < 2100; trial++) {
            int size = random.nextInt(125);
            List<String> rows = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                StringBuilder line = new StringBuilder();
                for (int j = random.nextInt(5); j > 0; j--) line.append(atoms[random.nextInt(atoms.length)]);
                rows.add(line.toString());
            }
            check(rows);
        }
        System.out.println("M3_UTF8_PACKED_ROWS_PASS checks=" + checks);
    }
}
