// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/MinimumSubsequenceWindowSelfTest.java (blob 69b69859e522) by M3_SEARCH_FORWARD v1.
package jdk.m3.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/** Executable differential oracle; no junit or donor runtime dependencies. */
public final class MinimumSubsequenceWindowSelfTest {
    private static final char[] ALPHABET = { 'a', 'b', 'c', '\u0000', '\uD800',
            '\uDC00', '\u0301', '\u4E2D', '\uD83D', '\uDE00' };
    private static final long SEED = 0x4D33535542534551L;
    private static final AtomicLong ASSERTIONS = new AtomicLong();
    private static long examples;
    private static long digest = 0x3c6ef372fe94f82bL;
    private static boolean nativeMode;

    private MinimumSubsequenceWindowSelfTest() { }

    private static void check(boolean truth, String detail) {
        long ordinal = ASSERTIONS.incrementAndGet();
        if (!truth) throw new AssertionError("assertion=" + ordinal + " " + detail);
    }

    private static long oracle(String source, String sequence) {
        if (sequence.isEmpty()) return 0L;
        int n = source.length(), m = sequence.length();
        int bestStart = -1, bestEnd = 0, shortest = Integer.MAX_VALUE;
        // Independently start every candidate and take its earliest possible finish.
        for (int start = 0; start < n; start++) {
            int matched = 0;
            for (int end = start; end < n; end++) {
                if (source.charAt(end) == sequence.charAt(matched)) matched++;
                if (matched == m) {
                    int length = end + 1 - start;
                    if (length < shortest || (length == shortest && start < bestStart)) {
                        bestStart = start;
                        bestEnd = end + 1;
                        shortest = length;
                    }
                    break;
                }
            }
        }
        return bestStart < 0 ? -1L : M3MinimumSubsequenceWindow.pack(bestStart, bestEnd);
    }

    private static void compare(String source, String sequence) {
        long expected = oracle(source, sequence);
        long javaObserved = M3MinimumSubsequenceWindow.find(source, sequence);
        check(javaObserved == expected, "java mismatch source=" + source.length()
                + " sequence=" + sequence.length() + " expected=" + expected
                + " observed=" + javaObserved);
        if (nativeMode) {
            char[] s = source.toCharArray(), t = sequence.toCharArray();
            char[] beforeS = s.clone(), beforeT = t.clone();
            long nativeObserved = M3MinimumSubsequenceWindow.findNative(s, t);
            check(nativeObserved == expected, "JNI mismatch source=" + source.length()
                    + " sequence=" + sequence.length()
                    + " expected=" + expected + " observed=" + nativeObserved);
            check(Arrays.equals(s, beforeS) && Arrays.equals(t, beforeT),
                    "JNI modified input");
        }
        digest = Long.rotateLeft(digest ^ javaObserved ^ (source.length() * 31L)
                ^ (sequence.length() * 131L), 9) * 0x9e3779b97f4a7c15L;
        examples++;
    }

    private static List<String> allStrings(char[] alphabet, int maxLength) {
        List<String> out = new ArrayList<>();
        out.add("");
        for (int n = 1; n <= maxLength; n++) {
            int count = 1;
            for (int i = 0; i < n; i++) count *= alphabet.length;
            for (int x = 0; x < count; x++) {
                int v = x;
                char[] units = new char[n];
                for (int i = 0; i < n; i++) {
                    units[i] = alphabet[v % alphabet.length];
                    v /= alphabet.length;
                }
                out.add(new String(units));
            }
        }
        return out;
    }

    private static void refusal(boolean nativeRun) {
        check(M3MinimumSubsequenceWindow.find("", "") == 0L, "both empty");
        check(M3MinimumSubsequenceWindow.find("", "a") == -1L, "empty source");
        boolean threw = false;
        try { M3MinimumSubsequenceWindow.find(null, "a"); }
        catch (NullPointerException expected) { threw = true; }
        check(threw, "null source refusal");
        threw = false;
        try { M3MinimumSubsequenceWindow.find("abc", null); }
        catch (NullPointerException expected) { threw = true; }
        check(threw, "null sequence refusal");
        threw = false;
        try { M3MinimumSubsequenceWindow.find("a".repeat(1_000_001), "a"); }
        catch (IllegalArgumentException expected) { threw = true; }
        check(threw, "source length refusal");
        threw = false;
        try { M3MinimumSubsequenceWindow.find("x", "x".repeat(4_097)); }
        catch (IllegalArgumentException expected) { threw = true; }
        check(threw, "sequence length refusal");
        threw = false;
        try { M3MinimumSubsequenceWindow.find("a".repeat(8_000), "a".repeat(4_096)); }
        catch (IllegalArgumentException expected) { threw = true; }
        check(threw, "multiplication budget refusal");
        check(M3MinimumSubsequenceWindow.find("a".repeat(1_000_000), "z") == -1L,
                "million-unit maximum source");
        if (nativeRun) {
            threw = false;
            try { M3MinimumSubsequenceWindow.findNative(null, new char[] {'a'}); }
            catch (NullPointerException expected) { threw = true; }
            check(threw, "JNI null source refusal");
            threw = false;
            try { M3MinimumSubsequenceWindow.findNative(new char[] {'a'}, null); }
            catch (NullPointerException expected) { threw = true; }
            check(threw, "JNI null sequence refusal");
            threw = false;
            try { M3MinimumSubsequenceWindow.findNative(new char[1_000_001], new char[1]); }
            catch (IllegalArgumentException expected) { threw = true; }
            check(threw, "JNI source budget refusal");
            threw = false;
            try { M3MinimumSubsequenceWindow.findNative(new char[1], new char[4_097]); }
            catch (IllegalArgumentException expected) { threw = true; }
            check(threw, "JNI sequence budget refusal");
            threw = false;
            try { M3MinimumSubsequenceWindow.findNative(new char[8_000], new char[4_096]); }
            catch (IllegalArgumentException expected) { threw = true; }
            check(threw, "JNI multiplication budget refusal");
            check(M3MinimumSubsequenceWindow.findNative(
                    "a".repeat(1_000_000).toCharArray(), new char[] {'z'}) == -1L,
                    "JNI million-unit maximum source");
        }
    }

    private static void concurrency() throws Exception {
        AtomicReference<Throwable> failed = new AtomicReference<>();
        List<Thread> threads = new ArrayList<>();
        for (int t = 0; t < 8; t++) {
            final int id = t;
            Runnable body = () -> {
                try {
                    for (int i = 0; i < 300; i++) {
                        String source = "aaabcdebdde".repeat(5) + (char) (id + 'a');
                        String seq = (i & 1) == 0 ? "bde" : "eeb";
                        long expected = oracle(source, seq);
                        check(M3MinimumSubsequenceWindow.find(source, seq) == expected,
                                "concurrent Java");
                        if (nativeMode) {
                            check(M3MinimumSubsequenceWindow.findNative(
                                    source.toCharArray(), seq.toCharArray()) == expected,
                                    "concurrent JNI");
                        }
                    }
                } catch (Throwable error) {
                    failed.compareAndSet(null, error);
                }
            };
            threads.add((t & 1) == 0
                    ? Thread.ofPlatform().start(body)
                    : Thread.ofVirtual().start(body));
        }
        for (Thread thread : threads) thread.join();
        if (failed.get() != null) throw new AssertionError("concurrent source parity", failed.get());
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !(args[0].equals("java") || args[0].equals("native"))) {
            throw new IllegalArgumentException("mode must be java or native");
        }
        nativeMode = args[0].equals("native");
        if (nativeMode) {
            String path = System.getProperty("m3.subsequence.native");
            if (path == null || path.isBlank()) throw new IllegalArgumentException("native path");
            System.load(path);
        }
        compare("abcdebdde", "bde");
        compare("geeksforgeeks", "eksrg");
        compare("ababa", "aa");
        compare("aabb", "bb");
        compare("", "");
        compare("", "a");
        compare("\uD800\u0000\uDC00", "\uD800\uDC00");
        compare("\uD800\uDC00\uD800\uDC00", "\uD800\uDC00");
        compare("aaaaa", "aaaa");
        compare("abcabc", "bac");
        char[] basic = { 'a', 'b', '\u0000' };
        for (String source : allStrings(basic, 6)) {
            for (String sequence : allStrings(basic, 4)) {
                compare(source, sequence);
            }
        }
        Random random = new Random(SEED);
        for (int i = 0; i < 60_000; i++) {
            int n = random.nextInt(61);
            int m = random.nextInt(17);
            char[] s = new char[n], t = new char[m];
            for (int j = 0; j < n; j++) s[j] = ALPHABET[random.nextInt(ALPHABET.length)];
            for (int j = 0; j < m; j++) t[j] = ALPHABET[random.nextInt(ALPHABET.length)];
            if (m <= n && m != 0 && (random.nextInt(5) == 0)) {
                // Embed a guaranteed ordered match without assuming contiguous matching.
                int from = random.nextInt(n - m + 1);
                System.arraycopy(s, from, t, 0, m);
            }
            compare(new String(s), new String(t));
        }
        refusal(nativeMode);
        concurrency();
        long expectedAssertions = examples * (nativeMode ? 3 : 1)
                + (nativeMode ? 14 : 8) + (nativeMode ? 4_800 : 2_400);
        if (ASSERTIONS.get() != expectedAssertions) {
            throw new AssertionError("lost concurrent assertions: " + ASSERTIONS.get()
                    + " expected=" + expectedAssertions);
        }
        System.out.println("M3_MIN_SUBSEQUENCE_PASS mode=" + args[0]
                + " examples=" + examples + " assertions=" + ASSERTIONS.get()
                + " digest=" + Long.toUnsignedString(digest, 16));
    }
}
