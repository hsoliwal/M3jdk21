// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Actual JNI parity of the exact-sized UTF-8 pack against an independent JDK encoder. */
public final class M3NativeIndexedPackIntegrationSelfTest {
    private static int checks;
    private M3NativeIndexedPackIntegrationSelfTest() { }

    private static void eq(Object actual, Object expected) {
        checks++;
        if (!actual.equals(expected)) throw new AssertionError("JNI packed parity check=" + checks
                + " observed=" + actual + " expected=" + expected);
    }

    private static void verify(List<String> rows, Random random) {
        NativeLineBatch packed = NativeLineBatch.fromUtf8Lines(rows);
        M3Utf8PackedRows.Packed bytes = M3Utf8PackedRows.pack(rows);
        ByteBuffer direct = ByteBuffer.allocateDirect(bytes.bytes().length);
        direct.put(bytes.bytes()).flip();
        NativeLineBatch borrowed = NativeLineBatch.fromReadOnlyM3Bytes(
                direct.asReadOnlyBuffer(), bytes.offsets(), direct);
        eq(packed.lineCount(), rows.size());
        eq(borrowed.selectedIndices(), packed.selectedIndices());
        for (int i = 0; i < rows.size(); i++) {
            eq(packed.utf8Content(i), rows.get(i));
            eq(borrowed.utf8Content(i), rows.get(i));
        }
        int amount = random.nextInt(rows.size() + 5);
        for (String needle : List.of("", "a", "é", "中", "💠", "\u0000", "☃")) {
            List<Integer> selected = new ArrayList<>();
            for (int i = 0; i < rows.size(); i++) if (rows.get(i).contains(needle)) selected.add(i);
            eq(packed.grepLiteral(needle).selectedIndices(), selected);
            eq(borrowed.grepLiteral(needle).selectedIndices(), selected);
        }
        eq(packed.head(amount).selectedIndices(), borrowed.head(amount).selectedIndices());
        eq(packed.tail(amount).selectedIndices(), borrowed.tail(amount).selectedIndices());
        eq(packed.uniqAdjacent().selectedIndices(), borrowed.uniqAdjacent().selectedIndices());
        eq(packed.grepLiteral("a").uniqAdjacent().tail(amount).selectedIndices(),
                borrowed.grepLiteral("a").uniqAdjacent().tail(amount).selectedIndices());
        checks++;
        if (!Arrays.equals(bytes.bytes(), independent(rows))) throw new AssertionError("bad complete pack");
    }

    private static byte[] independent(List<String> rows) {
        int n = 0;
        for (String row : rows) n = Math.addExact(n, row.getBytes(StandardCharsets.UTF_8).length);
        byte[] flat = new byte[n];
        int used = 0;
        for (String row : rows) {
            byte[] bytes = row.getBytes(StandardCharsets.UTF_8);
            System.arraycopy(bytes, 0, flat, used, bytes.length);
            used += bytes.length;
        }
        return flat;
    }

    public static void main(String[] args) {
        if (NativeLineBatch.nativeAbi() != 0x00010002) throw new AssertionError("ABI drift");
        Random random = new Random(0x4d335041434b4e49L);
        String[] words = {"", "a", "aa", "a\u0000b", "é", "中", "💠", "👩🏽‍💻", "e\u0301", "not matching"};
        for (int length : new int[]{0, 1, 2, 63, 64, 65, 127, 128, 129, 256, 513}) {
            List<String> rows = new ArrayList<>(length);
            for (int i = 0; i < length; i++) rows.add(words[random.nextInt(words.length)]);
            verify(rows, random);
        }
        for (int t = 0; t < 500; t++) {
            int n = random.nextInt(225);
            List<String> rows = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                StringBuilder text = new StringBuilder();
                for (int j = random.nextInt(5); j > 0; j--) text.append(words[random.nextInt(words.length)]);
                rows.add(text.toString());
            }
            verify(rows, random);
        }
        System.out.println("M3_NATIVE_INDEXED_UTF8_PACK_PASS checks=" + checks);
    }
}
