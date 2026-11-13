// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Independent sparse/dense selected-row parity across 64-bit JNI word boundaries. */
public final class M3NativeSparseSelectionSelfTest {
    private static int checks;
    private static final String[] TEXT = {"", "hit", "not", "é", "中hit", "💠",
            "a\u0000hit", "e\u0301", "hit", "hit", "👩🏽‍💻", "other"};
    private M3NativeSparseSelectionSelfTest() { }

    private static void eq(Object actual, Object expected) {
        checks++;
        if (!actual.equals(expected)) throw new AssertionError("at=" + checks
                + " expected=" + expected + " observed=" + actual);
    }

    private static List<Integer> grep(List<String> lines, List<Integer> input, String pattern) {
        List<Integer> result = new ArrayList<>();
        for (int i : input) if (lines.get(i).contains(pattern)) result.add(i);
        return result;
    }

    private static List<Integer> uniq(List<String> lines, List<Integer> input) {
        List<Integer> result = new ArrayList<>();
        String previous = null;
        for (int i : input) {
            if (previous == null || !previous.equals(lines.get(i))) result.add(i);
            previous = lines.get(i);
        }
        return result;
    }

    private static void caseOne(List<String> lines, Random random) {
        NativeLineBatch heap = NativeLineBatch.fromUtf8Lines(lines);
        M3Utf8PackedRows.Packed bytes = M3Utf8PackedRows.pack(lines);
        ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.bytes().length + 6);
        buffer.position(3);buffer.put(bytes.bytes());buffer.limit(3 + bytes.bytes().length);buffer.position(3);
        NativeLineBatch direct = NativeLineBatch.fromReadOnlyM3Bytes(buffer.slice().asReadOnlyBuffer(), bytes.offsets(), buffer);
        List<Integer> all = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) all.add(i);
        eq(heap.selectedIndices(), all);
        eq(direct.selectedIndices(), all);
        int max = lines.size();
        for (int q = 0; q < 3; q++) {
            String pattern = q == 0 ? "hit" : q == 1 ? "\u0000" : "";
            List<Integer> matched = grep(lines, all, pattern);
            int take = random.nextInt(max + 3);
            List<Integer> head = matched.subList(0, Math.min(take, matched.size()));
            List<Integer> tail = matched.subList(Math.max(0, matched.size()-take), matched.size());
            List<Integer> unique = uniq(lines, matched);
            for (NativeLineBatch starting : List.of(heap, direct)) {
                NativeLineBatch selected = starting.grepLiteral(pattern);
                eq(selected.selectedIndices(), matched);
                eq(selected.head(take).selectedIndices(), head);
                eq(selected.tail(take).selectedIndices(), tail);
                eq(selected.uniqAdjacent().selectedIndices(), unique);
                eq(selected.uniqAdjacent().tail(take).selectedIndices(),
                        unique.subList(Math.max(0, unique.size()-take), unique.size()));
                eq(selected.head(take).uniqAdjacent().selectedIndices(), uniq(lines, head));
                eq(starting.selectedIndices(), all); // persistent selection, never mutated
            }
        }
    }

    public static void main(String[] args) {
        if (NativeLineBatch.nativeAbi() != 0x00010002) throw new AssertionError("JNI ABI drift");
        Random random = new Random(0x4d334e4154495645L);
        for (int n : new int[]{0,1,2,63,64,65,127,128,129,1024,4095,4096,4097,65535,65536,65537}) {
            List<String> lines = new ArrayList<>(n);
            for (int i = 0; i < n; i++) lines.add(i % 163 == 0 ? "hit" : TEXT[random.nextInt(TEXT.length)]);
            caseOne(lines, random);
        }
        for (int i = 0; i < 500; i++) {
            int n = random.nextInt(1500);
            List<String> lines = new ArrayList<>(n);
            for (int j = 0; j < n; j++) lines.add(TEXT[random.nextInt(TEXT.length)]);
            caseOne(lines, random);
        }
        System.out.println("M3_NATIVE_SPARSE_ROW_PASS checks=" + checks);
    }
}
