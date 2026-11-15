/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. This file is subject to the
 * "Classpath" exception as provided in the LICENSE file that accompanied
 * this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

/*
 * @test
 * @summary Preserve selected-reference and public snapshot contracts across full-word runs
 * @modules java.base/java.util
 * @compile --patch-module java.base=${test.src} CollectionMaskRunCopyTest.java
 * @run main/othervm --patch-module java.base=${test.classes} java.util.CollectionMaskRunCopyTest
 */

package java.util;

public final class CollectionMaskRunCopyTest {
    private static long checks;
    private static int cases;

    private CollectionMaskRunCopyTest() { }

    private record Token(int index) { }

    public static void main(String[] arguments) {
        if (arguments.length != 0) System.load(arguments[0]);
        int[] lengths = {0, 1, 2, 63, 64, 65, 127, 128, 129, 191, 192, 193,
                255, 256, 257, 4095, 4096, 4097, 8191, 8192, 8193, 16384, 65537};
        for (int length : lengths) {
            Object[] values = new Object[length];
            for (int i = 0; i < length; i++) values[i] = i % 11 == 0 ? null : new Token(i);
            for (int pattern = 0; pattern < 14; pattern++) {
                long[] words = new long[(length + 63) >>> 6];
                SplittableRandom random = new SplittableRandom(0x5e1ec7L + pattern);
                for (int i = 0; i < length; i++) {
                    int word = i >>> 6;
                    boolean keep = switch (pattern) {
                        case 0 -> false;
                        case 1 -> true;
                        case 2 -> i != length / 2;
                        case 3 -> i != 0 && i != length - 1;
                        case 4 -> (word & 1) == 0;
                        case 5 -> word % 3 != 2;
                        case 6 -> word % 17 != 16;
                        case 7 -> word % 129 != 128;
                        case 8 -> (i & 1) == 0;
                        case 9 -> random.nextInt(100) < 1;
                        case 10 -> random.nextInt(100) < 50;
                        case 11 -> random.nextInt(100) < 99;
                        case 12 -> i < (3L * length) / 4;
                        default -> i < (3L * length) / 4 + 1;
                    };
                    if (keep) words[word] |= 1L << (i & 63);
                }
                verify(words, values);
                if ((length & 63) != 0) {
                    words[words.length - 1] |= -1L << (length & 63);
                    verify(words, values);
                }
            }
        }
        System.out.println("PASS full-word runs cases=" + cases + " checks=" + checks);
    }

    private static void verify(long[] words, Object[] values) {
        long[] frozenWords = words.clone();
        Object[] frozenValues = values.clone();
        // Independent per-position oracle: no word-run or set-bit decoding.
        List<Object> expected = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            if ((words[i >>> 6] & (1L << (i & 63))) != 0L) expected.add(values[i]);
        }
        Object[] actual = M3CollectionMasks.selectedValues(words, values, expected.size());
        require(actual.length == expected.size(), "selected count");
        for (int i = 0; i < actual.length; i++) require(actual[i] == expected.get(i), "reference/order");
        require(actual != values, "detached output array");

        int[] callbackIndex = {0};
        List<Object> snapshot = Collections.filteredSnapshot(Arrays.asList(values), value -> {
            int i = callbackIndex[0]++;
            require(value == values[i], "predicate identity/order");
            return (words[i >>> 6] & (1L << (i & 63))) != 0L;
        });
        require(callbackIndex[0] == values.length, "predicate call count");
        require(snapshot.size() == expected.size(), "public snapshot size");
        for (int i = 0; i < snapshot.size(); i++) require(snapshot.get(i) == expected.get(i), "public reference/order");

        if (expected.size() > 0) wrongCount(words, values, expected.size() - 1);
        if (expected.size() < values.length) wrongCount(words, values, expected.size() + 1);
        if (actual.length != 0) actual[0] = new Token(-1);
        for (int i = 0; i < values.length; i++) require(values[i] == frozenValues[i], "unchanged source values");
        require(Arrays.equals(words, frozenWords), "unchanged source words");
        cases++;
    }

    private static void wrongCount(long[] words, Object[] values, int count) {
        try {
            M3CollectionMasks.selectedValues(words, values, count);
            throw new AssertionError("accepted incorrect count " + count);
        } catch (ConcurrentModificationException expected) {
            require("selection mask changed during extraction".equals(expected.getMessage()), "count drift message");
        }
    }

    private static void require(boolean condition, String description) {
        checks++;
        if (!condition) throw new AssertionError(description);
    }
}
