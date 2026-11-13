// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Runnable differential oracle; assertions are always enabled and need no donor JAR. */
public final class NativeLineBatchSelfTest {
    private static int checks;
    private static final String[] ATOMS = {"", "a", "aa", "ab", "b", "c", "é", "👩🏽‍💻",
            "中", "e\u0301", "a\u0000b", "\r", "\n", "💠", "aabab"};

    private NativeLineBatchSelfTest() { }

    private static void equal(Object observed, Object expected) {
        checks++;
        if (!observed.equals(expected)) {
            throw new AssertionError("Mismatch " + checks + " actual=" + observed + " expected=" + expected);
        }
    }

    private static void rejects(Runnable operation) {
        checks++;
        try { operation.run(); }
        catch (IllegalArgumentException | IndexOutOfBoundsException expected) { return; }
        throw new AssertionError("Expected invalid-argument refusal");
    }

    private static List<Integer> indices(NativeLineBatch batch) { return batch.selectedIndices(); }

    private static List<Integer> grep(List<String> text, List<Integer> active, String needle) {
        List<Integer> out = new ArrayList<>();
        for (int i : active) if (text.get(i).contains(needle)) out.add(i);
        return out;
    }

    private static List<Integer> uniq(List<String> text, List<Integer> active) {
        List<Integer> out = new ArrayList<>();
        String previous = null;
        boolean first = true;
        for (int i : active) {
            if (first || !previous.equals(text.get(i))) out.add(i);
            previous = text.get(i);
            first = false;
        }
        return out;
    }

    private static List<Integer> head(List<Integer> active, int amount) {
        return active.subList(0, Math.min(amount, active.size()));
    }

    private static List<Integer> tail(List<Integer> active, int amount) {
        return active.subList(Math.max(0, active.size() - amount), active.size());
    }

    private static void checkOne(List<String> lines, String pattern, int amount) {
        NativeLineBatch base = NativeLineBatch.fromUtf8Lines(lines);
        byte[][] encoded = new byte[lines.size()][];
        int[] offsets = new int[lines.size() + 1];
        for (int i = 0; i < lines.size(); i++) {
            encoded[i] = lines.get(i).getBytes(StandardCharsets.UTF_8);
            offsets[i + 1] = Math.addExact(offsets[i], encoded[i].length);
        }
        ByteBuffer writable = ByteBuffer.allocateDirect(offsets[offsets.length - 1] + 7);
        writable.position(3);
        for (byte[] bytes : encoded) writable.put(bytes);
        writable.limit(3 + offsets[offsets.length - 1]);
        writable.position(3);
        NativeLineBatch direct = NativeLineBatch.fromReadOnlyM3Bytes(
                writable.slice().asReadOnlyBuffer(), offsets, writable);
        equal(direct.lineCount(), base.lineCount());
        equal(indices(direct), indices(base));
        equal(indices(direct.grepLiteral(pattern)), indices(base.grepLiteral(pattern)));
        equal(indices(direct.uniqAdjacent()), indices(base.uniqAdjacent()));
        equal(indices(direct.head(amount)), indices(base.head(amount)));
        equal(indices(direct.tail(amount)), indices(base.tail(amount)));
        equal(indices(direct.grepLiteral(pattern).uniqAdjacent().tail(amount)),
                indices(base.grepLiteral(pattern).uniqAdjacent().tail(amount)));
        for (int i = 0; i < lines.size(); i++) equal(direct.utf8Content(i), lines.get(i));
        equal(base.lineCount(), lines.size());
        List<Integer> all = indices(base);
        equal(all.size(), lines.size());
        for (int i = 0; i < lines.size(); i++) equal(base.utf8Content(i), lines.get(i));

        NativeLineBatch g = base.grepLiteral(pattern);
        equal(indices(g), grep(lines, all, pattern));
        equal(indices(g.uniqAdjacent()), uniq(lines, indices(g)));
        equal(indices(base.uniqAdjacent()), uniq(lines, all));
        equal(indices(base.head(amount)), head(all, amount));
        equal(indices(base.tail(amount)), tail(all, amount));
        equal(indices(g.head(amount)), head(indices(g), amount));
        equal(indices(g.tail(amount)), tail(indices(g), amount));
        equal(indices(g.head(amount).uniqAdjacent()), uniq(lines, head(indices(g), amount)));
        equal(indices(g.tail(amount).uniqAdjacent()), uniq(lines, tail(indices(g), amount)));
        // Earlier snapshots must remain unchanged after later native operations.
        equal(indices(base), all);
        equal(indices(g), grep(lines, all, pattern));
        equal(g.selectedCount(), grep(lines, all, pattern).size());
    }

    public static void main(String[] args) {
        equal(NativeLineBatch.nativeAbi(), 0x00010002);
        checkOne(List.of(), "", 0);
        checkOne(List.of("", "", "a", "a", "a\u0000b", "a", "a"), "a", 2);
        checkOne(List.of("a", "abc", "a", "é", "👩🏽‍💻", "é", ""), "", Integer.MAX_VALUE);
        checkOne(List.of("aa", "a", "", "a", "a", "a", ""), "a", 0);
        // Hostile repeated-prefix patterns: selected JNI rows must match String.contains.
        String repeated = "a".repeat(8192);
        checkOne(List.of(repeated + "b", repeated + "c", repeated + "b"),
                "a".repeat(4096) + "b", 2);
        checkOne(List.of(repeated + "b", repeated + "c", repeated + "b"),
                "a".repeat(4096) + "z", 2);
        checkOne(List.of("a\u0000b", "👩🏽‍💻", "é", ""), "a\u0000b", 1);
        Random random = new Random(0x4d33554e49584a4cL);
        for (int iteration = 0; iteration < 4000; iteration++) {
            int size = random.nextInt(100);
            List<String> lines = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                StringBuilder row = new StringBuilder();
                for (int j = random.nextInt(5); j > 0; j--) row.append(ATOMS[random.nextInt(ATOMS.length)]);
                lines.add(row.toString());
            }
            checkOne(lines, ATOMS[random.nextInt(ATOMS.length)], random.nextInt(110));
        }
        rejects(() -> NativeLineBatch.fromPacked(new byte[2], new int[] {0, 1}));
        rejects(() -> NativeLineBatch.fromPacked(new byte[2], new int[] {0, 2, 1}));
        rejects(() -> NativeLineBatch.fromPacked(new byte[2], new int[] {-1, 2}));
        rejects(() -> NativeLineBatch.fromUtf8Lines(List.of("\uD800")));
        rejects(() -> NativeLineBatch.fromUtf8Lines(List.of("\uDC00")));
        rejects(() -> NativeLineBatch.fromUtf8Lines(List.of("a")).grepLiteral("\uD800"));
        rejects(() -> NativeLineBatch.fromUtf8Lines(List.of("a")).head(-1));
        rejects(() -> NativeLineBatch.fromUtf8Lines(List.of("a")).tail(-1));
        rejects(() -> NativeLineBatch.fromReadOnlyM3Bytes(
                ByteBuffer.allocateDirect(2), new int[] {0, 2}, new Object()));
        rejects(() -> NativeLineBatch.fromReadOnlyM3Bytes(
                ByteBuffer.allocate(2).asReadOnlyBuffer(), new int[] {0, 2}, new Object()));
        equal(indices(NativeLineBatch.fromPacked(new byte[0], new int[] {0, 0}).grepLiteral("")), List.of(0));
        System.out.println("M3_NATIVE_UNIX_TEXT_PASS checks=" + checks + " seed=4d33554e49584a4c");
    }
}
