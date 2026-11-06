// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/MinimumCoverWindowSelfTest.java (blob 828a7bfd94e2) by M3_SEARCH_RELOCATE v1.
package jdk.m3.search;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.SplittableRandom;

/** Bounded independent enumeration oracle + opt-in real JNI differential. */
public final class MinimumCoverWindowSelfTest {
    private static final long SEED = 0x4d335f4d494e574cL;
    private static final char[] ALPHABET = {
        'a', 'b', 'c', 'A', 'B', '\0', '\r', '\n',
        '\u00e9', '\u4e2d', '\uD800', '\uDC00', '\uD83D', '\uDE00', '\uFFFF'
    };
    private static long checks;
    private static boolean withNative;

    private MinimumCoverWindowSelfTest() { }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message + " check=" + checks);
    }

    private static long oracle(String source, String required) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(required, "required");
        if (required.isEmpty()) return 0L;
        if (required.length() > source.length()) return -1L;
        int bestLength = Integer.MAX_VALUE;
        int bestStart = -1;
        int bestEnd = 0;
        // Enumerate ALL starts independently; no sliding-window state is reused.
        for (int start = 0; start < source.length(); start++) {
            Map<Character, Integer> frequencies = new HashMap<>();
            for (int i = 0; i < required.length(); i++) {
                frequencies.merge(required.charAt(i), 1, Integer::sum);
            }
            int missing = required.length();
            for (int end = start; end < source.length(); end++) {
                char unit = source.charAt(end);
                int outstanding = frequencies.getOrDefault(unit, 0);
                if (outstanding > 0) missing--;
                frequencies.put(unit, outstanding - 1);
                if (missing == 0) {
                    int size = end - start + 1;
                    if (size < bestLength) {
                        bestLength = size;
                        bestStart = start;
                        bestEnd = end + 1;
                    }
                    break;
                }
            }
        }
        return bestStart < 0 ? -1L : M3MinimumCoverWindow.pack(bestStart, bestEnd);
    }

    private static void caseOne(String source, String required) {
        long expected = oracle(source, required);
        long observed = M3MinimumCoverWindow.javaSpan(source, required);
        require(observed == expected, "java mismatch src=" + show(source)
                + " req=" + show(required) + " expected=" + expected + " got=" + observed);
        if (withNative) {
            long nativeResult = M3MinimumCoverWindow.nativeSpan(
                    source.toCharArray(), required.toCharArray());
            require(nativeResult == expected, "jni mismatch src=" + show(source)
                    + " req=" + show(required) + " expected=" + expected
                    + " got=" + nativeResult);
        }
    }

    private static String show(String text) {
        StringBuilder buffer = new StringBuilder("[");
        for (int i = 0; i < text.length(); i++) {
            if (i != 0) buffer.append(',');
            buffer.append(Integer.toHexString(text.charAt(i)));
        }
        return buffer.append(']').toString();
    }

    private static void throwsExpected(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try {
            action.run();
        } catch (Throwable caught) {
            if (type.isInstance(caught)) return;
            throw new AssertionError("expected " + type.getName() + ", got " + caught, caught);
        }
        throw new AssertionError("expected " + type.getName());
    }

    private static String enumerated(int ordinal, int width) {
        StringBuilder word = new StringBuilder(width);
        for (int i = 0; i < width; i++) {
            word.append(ALPHABET[ordinal % 3]);
            ordinal /= 3;
        }
        return word.toString();
    }

    public static void main(String[] args) {
        if (args.length != 1 || !(args[0].equals("java") || args[0].equals("native"))) {
            throw new IllegalArgumentException("expected java or native mode");
        }
        withNative = args[0].equals("native");
        if (withNative) {
            System.load(Objects.requireNonNull(System.getProperty("m3.window.native.library")));
        }
        caseOne("ADOBECODEBANC", "ABC");
        caseOne("a", "a");
        caseOne("a", "aa");
        caseOne("aba", "aab");
        caseOne("cabxab", "ab");
        caseOne("", "");
        caseOne("", "a");
        caseOne("\uD800\uDC00", "\uDC00\uD800");
        caseOne("a\0b\0a", "\0a");
        caseOne("aaaaabbbbbccccc", "bbcc");
        for (int sourceLength = 0; sourceLength <= 4; sourceLength++) {
            int sourceVariants = (int) Math.pow(3, sourceLength);
            for (int sourceVariant = 0; sourceVariant < sourceVariants; sourceVariant++) {
                String source = enumerated(sourceVariant, sourceLength);
                for (int requiredLength = 0; requiredLength <= 3; requiredLength++) {
                    int requiredVariants = (int) Math.pow(3, requiredLength);
                    for (int requiredVariant = 0; requiredVariant < requiredVariants;
                            requiredVariant++) {
                        caseOne(source, enumerated(requiredVariant, requiredLength));
                    }
                }
            }
        }
        SplittableRandom random = new SplittableRandom(SEED);
        for (int iteration = 0; iteration < 25_000; iteration++) {
            StringBuilder source = new StringBuilder();
            StringBuilder required = new StringBuilder();
            for (int n = random.nextInt(41); n > 0; n--) {
                source.append(ALPHABET[random.nextInt(ALPHABET.length)]);
            }
            for (int n = random.nextInt(14); n > 0; n--) {
                required.append(ALPHABET[random.nextInt(ALPHABET.length)]);
            }
            caseOne(source.toString(), required.toString());
        }
        throwsExpected(NullPointerException.class, () -> M3MinimumCoverWindow.javaSpan(null, "a"));
        throwsExpected(NullPointerException.class, () -> M3MinimumCoverWindow.javaSpan("a", null));
        throwsExpected(IllegalArgumentException.class,
                () -> M3MinimumCoverWindow.javaSpan("x".repeat(1_000_001), "x"));
        throwsExpected(IllegalArgumentException.class,
                () -> M3MinimumCoverWindow.javaSpan("x", "x".repeat(1_000_001)));
        if (withNative) {
            throwsExpected(NullPointerException.class,
                    () -> M3MinimumCoverWindow.nativeSpan(null, new char[] {'a'}));
            throwsExpected(NullPointerException.class,
                    () -> M3MinimumCoverWindow.nativeSpan(new char[] {'a'}, null));
            throwsExpected(IllegalArgumentException.class,
                    () -> M3MinimumCoverWindow.nativeSpan(new char[1_000_001], new char[] {'x'}));
            throwsExpected(IllegalArgumentException.class,
                    () -> M3MinimumCoverWindow.nativeSpan(new char[] {'x'}, new char[1_000_001]));
        }
        require(M3MinimumCoverWindow.javaSpan("a".repeat(1_000_000), "a".repeat(5))
                == M3MinimumCoverWindow.pack(0, 5), "large Java boundary");
        if (withNative) {
            require(M3MinimumCoverWindow.nativeSpan("a".repeat(1_000_000).toCharArray(),
                    "a".repeat(5).toCharArray()) == M3MinimumCoverWindow.pack(0, 5),
                    "large JNI boundary");
        }
        System.out.println("M3_MINIMUM_COVER_PASS mode=" + args[0] + " checks=" + checks
                + " seed=" + Long.toHexString(SEED));
    }
}
