// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** Direct actual-JNI range checks and regression checks for the retained old exports. */
public final class CsvNativeContract {
    private static long checks;
    private CsvNativeContract() { }

    public static void main(String[] args) {
        rangesAndBoundaries();
        argumentValidation();
        retainedNativeExports();
        System.out.println("CSV_NATIVE_CONTRACT_PASS checks=" + checks);
    }

    static void rangesAndBoundaries() {
        require(NativeLineOps.structuralAvailable(), "actual native structural symbol required");
        Random random = new Random(2026110403L);
        char[] delimiters = {',', '\t', ';', '\u0000', '\u00e9', '\uffff'};
        for (int trial = 0; trial < 4096; trial++) {
            int length = random.nextInt(800);
            char[] text = new char[length];
            for (int i = 0; i < length; i++) {
                text[i] = switch (random.nextInt(9)) {
                    case 0 -> '"'; case 1 -> '\r'; case 2 -> '\n';
                    case 3 -> ','; case 4 -> '\u0000'; case 5 -> '\t';
                    default -> (char) random.nextInt(65536);
                };
            }
            String input = new String(text);
            int from = random.nextInt(length + 1);
            int units = random.nextInt(Math.min(256, length - from) + 1);
            char delimiter = delimiters[random.nextInt(delimiters.length)];
            compareScan(input, from, units, delimiter);
        }
        for (int units = 0; units <= 256; units++) {
            compareScan(",".repeat(256), 0, units, ',');
            compareScan("a".repeat(256), 0, units, ',');
            compareScan("\uD83D\uDE00".repeat(256), 1, units, ',');
            compareScan("\u0000".repeat(512), 256, units, '\u0000');
        }
    }

    static void argumentValidation() {
        require(NativeLineOps.structuralAvailable(), "actual native structural symbol required");
        expect(NullPointerException.class, "structural scan arguments",
                () -> NativeLineOps.structuralOffsets(null, 0, 0, ',', new int[256]));
        expect(NullPointerException.class, "structural scan arguments",
                () -> NativeLineOps.structuralOffsets("", 0, 0, ',', null));
        for (int[] pair : new int[][] {{-1, 0}, {2, 0}, {0, -1}, {0, 2}, {1, 1},
                {Integer.MIN_VALUE, 0}, {Integer.MAX_VALUE, 0}, {0, Integer.MAX_VALUE},
                {0, Integer.MIN_VALUE}}) {
            expect(IllegalArgumentException.class, "structural scan bounds",
                    () -> NativeLineOps.structuralOffsets("a", pair[0], pair[1], ',', new int[256]));
        }
        expect(IllegalArgumentException.class, "structural scan bounds",
                () -> NativeLineOps.structuralOffsets("a".repeat(257), 0, 257, ',', new int[257]));
        expect(IllegalArgumentException.class, "structural scan bounds",
                () -> NativeLineOps.structuralOffsets("a", 0, 1, ',', new int[0]));
        int[] untouched = {42};
        require(NativeLineOps.structuralOffsets("a", 1, 0, ',', untouched) == 0,
                "empty end range");
        require(untouched[0] == 42, "empty range may not write output");
    }

    static void retainedNativeExports() {
        require(NativeLineOps.available(), "actual native library required");
        Random random = new Random(2026110404L);
        String[] vocabulary = {"", "a", "a", "b", "\u0000", "\u0000".repeat(9),
                "abcdefghx", "abcdefghy", "abcdefgh", "\uD83D\uDE00", "\uffff"};
        for (int trial = 0; trial < 256; trial++) {
            List<TextLine> lines = new ArrayList<>();
            for (int i = random.nextInt(100); i > 0; i--)
                lines.add(new TextLine(vocabulary[random.nextInt(vocabulary.length)], "\n"));
            Integer[] order = new Integer[lines.size()];
            for (int i = 0; i < order.length; i++) order[i] = i;
            for (boolean reverse : new boolean[] {false, true}) {
                Comparator<Integer> comparator = Comparator.comparing(i -> lines.get(i).content());
                if (reverse) comparator = comparator.reversed();
                Integer[] expected = order.clone();
                Arrays.sort(expected, comparator);
                int[] actual = NativeLineOps.sortOrder(lines, reverse);
                require(actual.length == expected.length, "old sort length");
                for (int i = 0; i < actual.length; i++)
                    require(actual[i] == expected[i], "old stable sort export changed");
            }
            long[] kept = NativeLineOps.adjacentKeep(lines);
            for (int i = 0; i < lines.size(); i++) {
                boolean expected = i == 0 || !lines.get(i - 1).content().equals(lines.get(i).content());
                require(((kept[i >>> 6] >>> (i & 63)) & 1L) != 0 == expected,
                        "old adjacent unique export changed");
            }
        }
    }

    private static void compareScan(String text, int from, int units, char delimiter) {
        int[] output = new int[260];
        Arrays.fill(output, -77);
        int actualCount = NativeLineOps.structuralOffsets(text, from, units, delimiter, output);
        int count = 0;
        for (int i = from; i < from + units; i++) {
            char unit = text.charAt(i);
            if (unit == delimiter || unit == '"' || unit == '\r' || unit == '\n') {
                require(output[count] == i, "structural offset mismatch");
                count++;
            }
        }
        require(actualCount == count, "structural count mismatch");
        for (int i = count; i < output.length; i++)
            require(output[i] == -77, "native scan wrote beyond its output prefix");
    }

    private static void expect(Class<? extends RuntimeException> kind, String message, Runnable action) {
        try { action.run(); }
        catch (RuntimeException failure) {
            require(failure.getClass() == kind && message.equals(failure.getMessage()),
                    "wrong native argument failure: " + failure);
            return;
        }
        throw new AssertionError("missing native argument refusal " + kind.getName());
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
