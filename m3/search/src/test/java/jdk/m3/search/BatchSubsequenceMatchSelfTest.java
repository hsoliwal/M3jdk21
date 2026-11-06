// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/BatchSubsequenceMatchSelfTest.java (blob 6a1f92fdeb13) by M3_SEARCH_RELOCATE v1.
package jdk.m3.search;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

/** Independent one-pattern-at-a-time oracle, no Maven dependencies. */
public final class BatchSubsequenceMatchSelfTest {
    private static final char[] ALPHABET = {'a', 'b', 'c', '\u0000', '\uD800', '\uDC00', '\u4E2D'};
    private static int checks;
    private static long digest = 0x1234abcdeL;
    private static int cases;
    private static boolean nativeMode;

    private BatchSubsequenceMatchSelfTest() { }

    private static void check(boolean value, String description) {
        checks++;
        if (!value) throw new AssertionError("check " + checks + ": " + description);
    }

    private static long oracle(String source, String[] words) {
        long result = 0;
        for (String word : words) {
            int at = 0;
            for (int i = 0; i < source.length() && at < word.length(); i++) {
                if (source.charAt(i) == word.charAt(at)) at++;
            }
            if (at == word.length()) result++;
        }
        return result;
    }

    private static char[] packed(String[] words) {
        int total = 0;
        for (String w : words) total += w.length();
        char[] result = new char[total];
        int p = 0;
        for (String word : words) {
            word.getChars(0, word.length(), result, p);
            p += word.length();
        }
        return result;
    }

    private static int[] offsets(String[] words) {
        int[] result = new int[words.length + 1];
        for (int i = 0; i < words.length; i++) result[i+1] = result[i] + words[i].length();
        return result;
    }

    private static void compare(String source, String[] words) {
        long expected = oracle(source, words);
        long observed = M3BatchSubsequenceMatch.count(source, words);
        check(expected == observed, "Java oracle case=" + cases);
        if (nativeMode) {
            char[] src = source.toCharArray();
            char[] data = packed(words);
            int[] ends = offsets(words);
            char[] srcBefore = src.clone();
            char[] dataBefore = data.clone();
            int[] endsBefore = ends.clone();
            long actual = M3BatchSubsequenceMatch.countNative(src, data, ends);
            check(expected == actual, "JNI oracle case=" + cases);
            check(Arrays.equals(src, srcBefore) && Arrays.equals(data, dataBefore)
                    && Arrays.equals(ends, endsBefore), "JNI inputs changed");
        }
        digest = Long.rotateLeft(digest ^ (expected + 31L * words.length + source.length()), 7)
                * 0x9e3779b97f4a7c15L;
        cases++;
    }

    private static void rejectsJava() {
        boolean fail = false;
        try { M3BatchSubsequenceMatch.count(null, new String[0]); }
        catch (NullPointerException expected) { fail = true; }
        check(fail, "Java null source");
        fail = false;
        try { M3BatchSubsequenceMatch.count("abc", null); }
        catch (NullPointerException expected) { fail = true; }
        check(fail, "Java null array");
        fail = false;
        try { M3BatchSubsequenceMatch.count("abc", new String[]{"a", null}); }
        catch (NullPointerException expected) { fail = true; }
        check(fail, "Java null entry");
        fail = false;
        try { M3BatchSubsequenceMatch.count("a".repeat(1_000_001), new String[]{""}); }
        catch (IllegalArgumentException expected) { fail = true; }
        check(fail, "Java source cap");
        fail = false;
        try { M3BatchSubsequenceMatch.count("", new String[65_537]); }
        catch (IllegalArgumentException expected) { fail = true; }
        check(fail, "Java count cap");
        fail = false;
        try { M3BatchSubsequenceMatch.count("", new String[]{"a".repeat(1_000_001)}); }
        catch (IllegalArgumentException expected) { fail = true; }
        check(fail, "Java total cap");
    }

    private static void rejectsNative() {
        boolean fail = false;
        try { M3BatchSubsequenceMatch.countNative(null, new char[0], new int[]{0}); }
        catch (NullPointerException expected) { fail = true; }
        check(fail, "JNI null");
        fail = false;
        try { M3BatchSubsequenceMatch.countNative(new char[0], new char[0], new int[0]); }
        catch (IllegalArgumentException expected) { fail = true; }
        check(fail, "JNI no offsets");
        fail = false;
        try { M3BatchSubsequenceMatch.countNative(new char[0], new char[2], new int[]{0, 1}); }
        catch (IllegalArgumentException expected) { fail = true; }
        check(fail, "JNI final mismatch");
        fail = false;
        try { M3BatchSubsequenceMatch.countNative(new char[0], new char[2], new int[]{0, 2, 1, 2}); }
        catch (IllegalArgumentException expected) { fail = true; }
        check(fail, "JNI nonmonotone offsets");
        fail = false;
        try { M3BatchSubsequenceMatch.countNative(new char[1_000_001], new char[0], new int[]{0}); }
        catch (IllegalArgumentException expected) { fail = true; }
        check(fail, "JNI source cap");
        fail = false;
        try { M3BatchSubsequenceMatch.countNative(new char[0], new char[0], new int[65_538]); }
        catch (IllegalArgumentException expected) { fail = true; }
        check(fail, "JNI count cap");
    }

    private static String randomString(Random random, int n) {
        char[] result = new char[n];
        for (int i = 0; i < n; i++) result[i] = ALPHABET[random.nextInt(ALPHABET.length)];
        return new String(result);
    }

    private static void concurrency() throws InterruptedException {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread[] threads = new Thread[8];
        for (int t = 0; t < threads.length; t++) {
            final int id = t;
            Runnable work = () -> {
                try {
                    for (int i = 0; i < 200; i++) {
                        String source = "aaabcdbbca".repeat(4) + (char) ('a' + id);
                        String[] patterns = {"a", "aaa", "bcd", "bbbc", "", "", "z",
                                i % 2 == 0 ? "abc" : "cba", "aa"};
                        long expected = oracle(source, patterns);
                        if (M3BatchSubsequenceMatch.count(source, patterns) != expected) {
                            throw new AssertionError("concurrent Java divergence");
                        }
                        if (nativeMode && M3BatchSubsequenceMatch.countNative(source.toCharArray(),
                                packed(patterns), offsets(patterns)) != expected) {
                            throw new AssertionError("concurrent JNI divergence");
                        }
                    }
                } catch (Throwable thrown) {
                    failure.compareAndSet(null, thrown);
                }
            };
            threads[t] = (t & 1) == 0 ? Thread.ofPlatform().start(work)
                    : Thread.ofVirtual().start(work);
        }
        for (Thread thread : threads) thread.join();
        check(failure.get() == null, "concurrent native/Java parity: " + failure.get());
    }

    public static void main(String[] args) throws InterruptedException {
        nativeMode = args.length == 1 && args[0].equals("native");
        if (args.length != 1 || !(args[0].equals("java") || nativeMode)) {
            throw new IllegalArgumentException("java/native mode");
        }
        if (nativeMode) {
            String library = System.getProperty("m3.batch.subsequence.library");
            if (library == null || library.isBlank()) throw new IllegalArgumentException("native path");
            System.load(library);
        }
        compare("abcde", new String[]{"a", "bb", "acd", "ace"});
        compare("aaaa", new String[]{"", "a", "aa", "aaa", "aaaa", "aaaaa", "aa"});
        compare("", new String[]{"", "", "a"});
        compare("ab", new String[]{"aa", "ba", "ab", "a", "b", ""});
        compare("\uD800\u0000\uDC00", new String[]{"\uD800\uDC00", "\u0000", "\uDC00", "\uD800\uD800"});
        compare("", new String[0]);
        Random random = new Random(0x4d33535542534551L);
        for (int i = 0; i < 12_000; i++) {
            String source = randomString(random, random.nextInt(65));
            String[] words = new String[random.nextInt(40)];
            for (int j = 0; j < words.length; j++) {
                if (j != 0 && random.nextInt(7) == 0) words[j] = words[random.nextInt(j)];
                else words[j] = randomString(random, random.nextInt(14));
            }
            compare(source, words);
        }
        String[] maximal = new String[65_536];
        Arrays.fill(maximal, "");
        compare("", maximal);
        compare("a".repeat(1_000_000), new String[]{"a", "aa", "aaa", "b"});
        rejectsJava();
        if (nativeMode) rejectsNative();
        concurrency();
        System.out.println("M3_BATCH_SUBSEQUENCE_PASS mode=" + args[0]
                + " cases=" + cases + " checks=" + checks
                + " digest=" + Long.toUnsignedString(digest,16));
    }
}
