// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Executed with the actual C17 JNI owner by verify-jni-text.sh, never a shell fallback. */
public final class M3NativeMaskCursorIntegrationSelfTest {
    private static int checks;
    private M3NativeMaskCursorIntegrationSelfTest() { }

    private static void same(List<Integer> actual, List<Integer> expected) {
        checks++;
        if (!actual.equals(expected)) throw new AssertionError("mask parity " + checks);
    }

    private static void verify(NativeLineBatch selected) {
        List<Integer> expected = selected.selectedIndices();
        List<Integer> visited = new ArrayList<>();
        selected.forEachSelectedIndex(visited::add);
        same(visited, expected);
        checks++;
        if (visited.size() != selected.selectedCount()) throw new AssertionError("selection count changed");
        int before = visited.size();
        selected.forEachSelectedIndex(index -> { if (index < 0 || index >= selected.lineCount())
            throw new AssertionError("out of range"); });
        checks++;
        if (visited.size() != before) throw new AssertionError("mutable traversal");
    }

    public static void main(String[] args) {
        if (NativeLineBatch.nativeAbi() != 0x00010002) throw new AssertionError("JNI ABI");
        Random random = new Random(0x4d33554e49584c4cL);
        String[] atoms = {"", "a", "ba", "a\u0000b", "é", "中", "💠", "e\u0301", "not matching"};
        for (int n : new int[]{0, 1, 2, 63, 64, 65, 127, 128, 129, 257, 513}) {
            List<String> strings = new ArrayList<>(n);
            for (int i = 0; i < n; i++) strings.add(atoms[random.nextInt(atoms.length)]);
            exercise(strings, random);
        }
        for (int trial = 0; trial < 650; trial++) {
            int n = random.nextInt(320);
            List<String> strings = new ArrayList<>(n);
            for (int i = 0; i < n; i++) strings.add(atoms[random.nextInt(atoms.length)]);
            exercise(strings, random);
        }
        System.out.println("M3_NATIVE_MASK_CURSOR_INTEGRATION_PASS checks=" + checks);
    }

    private static void exercise(List<String> rows, Random random) {
        NativeLineBatch packed = NativeLineBatch.fromUtf8Lines(rows);
        int[] offsets = new int[rows.size() + 1];
        for (int i = 0; i < rows.size(); i++) offsets[i+1] = Math.addExact(offsets[i],
                rows.get(i).getBytes(StandardCharsets.UTF_8).length);
        ByteBuffer direct = ByteBuffer.allocateDirect(offsets[offsets.length-1]);
        for (String row : rows) direct.put(row.getBytes(StandardCharsets.UTF_8));
        direct.flip();
        NativeLineBatch borrowed = NativeLineBatch.fromReadOnlyM3Bytes(direct.asReadOnlyBuffer(), offsets, direct);
        verify(packed); verify(borrowed);
        for (NativeLineBatch window : new NativeLineBatch[]{packed, borrowed}) {
            int amount = random.nextInt(rows.size() + 6);
            verify(window.head(amount));
            verify(window.tail(amount));
            verify(window.uniqAdjacent());
            verify(window.grepLiteral("a"));
            verify(window.grepLiteral("").uniqAdjacent().tail(amount));
            verify(window.head(amount).grepLiteral("é").uniqAdjacent());
            verify(window.tail(amount).grepLiteral("☃"));
        }
        same(packed.grepLiteral("é").selectedIndices(), borrowed.grepLiteral("é").selectedIndices());
        same(packed.uniqAdjacent().head(4).selectedIndices(), borrowed.uniqAdjacent().head(4).selectedIndices());
    }
}
