// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/FixedWordConcatenationSelfTest.java (blob d4278732aa39) by M3_SEARCH_RELOCATE v1.
package jdk.m3.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/** Independently authored brute-force oracle, not a commercial challenge solution. */
public final class FixedWordConcatenationSelfTest {
    private static final long SEED = 0x4d33574f5244434fL;
    private static final char[] ALPHABET = {'a', 'b', 'c', '\u0000', '\uD800', '\uDC00',
            '\u4e2d', '\u0301', '\uD83D', '\uDE00'};
    private static final AtomicLong CHECKS = new AtomicLong();
    private static long cases;
    private static long digest = 0x6a09e667f3bcc909L;
    private static boolean nativeMode;

    private FixedWordConcatenationSelfTest() { }

    private static void check(boolean condition, String info) {
        long index = CHECKS.incrementAndGet();
        if (!condition) throw new AssertionError("check=" + index + " " + info);
    }

    /** Independent O(n * wordCount * wordWidth) frequency-count reference. */
    private static int[] oracle(String source, String[] words) {
        int n = source.length();
        int width = words[0].length();
        int total = width * words.length;
        Map<String, Integer> required = new HashMap<>();
        for (String word : words) required.merge(word, 1, Integer::sum);
        List<Integer> result = new ArrayList<>();
        for (int start = 0; start + total <= n; start++) {
            Map<String, Integer> remaining = new HashMap<>(required);
            boolean success = true;
            for (int i = 0; i < words.length; i++) {
                String token = source.substring(start + i * width, start + (i + 1) * width);
                Integer need = remaining.get(token);
                if (need == null || need == 0) {
                    success = false;
                    break;
                }
                remaining.put(token, need - 1);
            }
            if (success) result.add(start);
        }
        return result.stream().mapToInt(Integer::intValue).toArray();
    }

    private static int[] nativeResult(String source, String[] words) {
        char[] sourceArray = source.toCharArray();
        char[] sourceBefore = sourceArray.clone();
        char[][] dictionary = new char[words.length][];
        char[][] saved = new char[words.length][];
        for (int i = 0; i < words.length; i++) {
            dictionary[i] = words[i].toCharArray();
            saved[i] = dictionary[i].clone();
        }
        int[] result = FixedWordConcatenation.findNative(sourceArray, dictionary);
        check(Arrays.equals(sourceBefore, sourceArray), "native source mutation");
        for (int i = 0; i < words.length; i++) {
            check(Arrays.equals(dictionary[i], saved[i]), "native word mutation " + i);
        }
        return result;
    }

    private static void compare(String source, String... words) {
        int[] expected = oracle(source, words);
        int[] observed = FixedWordConcatenation.find(source, words);
        check(Arrays.equals(expected, observed), "Java mismatch source=" + source.length());
        if (nativeMode) {
            int[] nativeObserved = nativeResult(source, words);
            check(Arrays.equals(expected, nativeObserved), "native mismatch source=" + source.length());
        }
        for (int v : observed) digest = Long.rotateLeft(digest ^ v, 7) * 0x9e3779b97f4a7c15L;
        digest = Long.rotateLeft(digest ^ source.length() ^ ((long) words.length << 32), 11);
        cases++;
    }

    private static void directed() {
        compare("barfoothefoobarman", "foo", "bar");
        compare("wordgoodgoodgoodbestword", "word", "good", "best", "word");
        compare("barfoofoobarthefoobarman", "bar", "foo", "the");
        compare("aaaaaa", "aa", "aa");
        compare("abababab", "ab", "ab");
        compare("a\u0000b\uD800a\u0000b\uD800", "a\u0000", "b\uD800");
        compare("\uD83D\uDE00\uD800\uDC00", "\uD83D\uDE00", "\uD800\uDC00");
        compare("", "x");
        compare("x", "xx");
        compare("aba", "a", "b");
        int[] a = FixedWordConcatenation.find("barfoothefoobarman", new String[] {"foo", "bar"});
        check(Arrays.equals(a, new int[] {0, 9}), "LC30 directed start offsets");
        int[] b = FixedWordConcatenation.find("aaaaaa", new String[] {"aa", "aa"});
        check(Arrays.equals(b, new int[] {0, 1, 2}), "overlap and multiplicity");
    }

    private static List<String> words(char[] alphabet, int length) {
        List<String> out = new ArrayList<>();
        int count = 1;
        for (int i = 0; i < length; i++) count *= alphabet.length;
        for (int bits = 0; bits < count; bits++) {
            char[] value = new char[length];
            int n = bits;
            for (int i = 0; i < length; i++) {
                value[i] = alphabet[n % alphabet.length];
                n /= alphabet.length;
            }
            out.add(new String(value));
        }
        return out;
    }

    private static void exhaustive() {
        char[] basis = {'a', 'b'};
        List<String> sources = new ArrayList<>();
        sources.add("");
        for (int i = 1; i <= 6; i++) sources.addAll(words(basis, i));
        for (String source : sources) {
            for (int width = 1; width <= 2; width++) {
                List<String> universe = words(basis, width);
                for (int count = 1; count <= 3; count++) {
                    int combinations = 1;
                    for (int i = 0; i < count; i++) combinations *= universe.size();
                    for (int selector = 0; selector < combinations; selector++) {
                        int value = selector;
                        String[] selected = new String[count];
                        for (int i = 0; i < count; i++) {
                            selected[i] = universe.get(value % universe.size());
                            value /= universe.size();
                        }
                        compare(source, selected);
                    }
                }
            }
        }
    }

    private static void randomized() {
        Random random = new Random(SEED);
        for (int iteration = 0; iteration < 30000; iteration++) {
            int length = random.nextInt(85);
            char[] units = new char[length];
            for (int i = 0; i < length; i++) units[i] = ALPHABET[random.nextInt(ALPHABET.length)];
            String source = new String(units);
            int width = 1 + random.nextInt(5);
            int number = 1 + random.nextInt(6);
            String[] dictionary = new String[number];
            for (int i = 0; i < number; i++) {
                if (i > 0 && random.nextInt(5) == 0) {
                    dictionary[i] = dictionary[random.nextInt(i)];
                } else if (length >= width && random.nextInt(3) == 0) {
                    int start = random.nextInt(length - width + 1);
                    dictionary[i] = source.substring(start, start + width);
                } else {
                    char[] word = new char[width];
                    for (int j = 0; j < width; j++) word[j] = ALPHABET[random.nextInt(ALPHABET.length)];
                    dictionary[i] = new String(word);
                }
            }
            compare(source, dictionary);
        }
    }

    private static void refusal() {
        expect(NullPointerException.class, () -> FixedWordConcatenation.find(null, new String[]{"a"}));
        expect(NullPointerException.class, () -> FixedWordConcatenation.find("a", null));
        expect(NullPointerException.class, () -> FixedWordConcatenation.find("a", new String[]{null}));
        expect(IllegalArgumentException.class, () -> FixedWordConcatenation.find("a", new String[0]));
        expect(IllegalArgumentException.class, () -> FixedWordConcatenation.find("a", new String[]{""}));
        expect(IllegalArgumentException.class, () -> FixedWordConcatenation.find("ab", new String[]{"a", "bb"}));
        expect(IllegalArgumentException.class, () -> FixedWordConcatenation.find("a".repeat(8193), new String[]{"a"}));
        expect(IllegalArgumentException.class, () -> FixedWordConcatenation.find("abc", new String[]{"a".repeat(17)}));
        String[] tooMany = new String[33];
        Arrays.fill(tooMany, "a");
        expect(IllegalArgumentException.class, () -> FixedWordConcatenation.find("abc", tooMany));
        check(FixedWordConcatenation.find("a".repeat(8192), new String[]{"z"}).length == 0,
                "max source budget");
        check(FixedWordConcatenation.find("a".repeat(8192), new String[]{"a"}).length == 8192,
                "max output budget");
        if (nativeMode) {
            expect(NullPointerException.class, () -> FixedWordConcatenation.findNative(null, new char[][]{{'a'}}));
            expect(NullPointerException.class, () -> FixedWordConcatenation.findNative(new char[]{'a'}, null));
            expect(NullPointerException.class, () -> FixedWordConcatenation.findNative(new char[]{'a'}, new char[][]{null}));
            expect(IllegalArgumentException.class, () -> FixedWordConcatenation.findNative(new char[]{'a'}, new char[0][]));
            expect(IllegalArgumentException.class, () -> FixedWordConcatenation.findNative(new char[]{'a'}, new char[][]{{}}));
            expect(IllegalArgumentException.class, () -> FixedWordConcatenation.findNative(new char[]{'a'}, new char[][]{{'a'}, {'b', 'b'}}));
            expect(IllegalArgumentException.class, () -> FixedWordConcatenation.findNative(new char[8193], new char[][]{{'a'}}));
            expect(IllegalArgumentException.class, () -> FixedWordConcatenation.findNative(new char[]{'a'}, new char[][]{new char[17]}));
            char[][] many = new char[33][];
            for (int i = 0; i < many.length; i++) many[i] = new char[]{'a'};
            expect(IllegalArgumentException.class, () -> FixedWordConcatenation.findNative(new char[]{'a'}, many));
            check(FixedWordConcatenation.findNative("a".repeat(8192).toCharArray(), new char[][]{{'a'}}).length == 8192,
                    "native max output budget");
        }
    }

    private static void expect(Class<? extends Throwable> type, Runnable task) {
        try {
            task.run();
        } catch (Throwable error) {
            check(type.isInstance(error), "expected " + type.getSimpleName() + " got " + error);
            return;
        }
        throw new AssertionError("expected " + type.getSimpleName() + " refusal");
    }

    private static void concurrency() throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        List<Thread> threads = new ArrayList<>();
        for (int t = 0; t < 8; t++) {
            int id = t;
            Runnable run = () -> {
                try {
                    for (int i = 0; i < 180; i++) {
                        String source = "abababababa\u0000".repeat(4) + (char) ('a' + id);
                        String[] words = (i & 1) == 0 ? new String[]{"ab", "ba"} : new String[]{"aa", "ab"};
                        int[] expected = oracle(source, words);
                        check(Arrays.equals(expected, FixedWordConcatenation.find(source, words)), "parallel java");
                        if (nativeMode) check(Arrays.equals(expected,
                                FixedWordConcatenation.findNative(source.toCharArray(),
                                        new char[][]{words[0].toCharArray(), words[1].toCharArray()})), "parallel native");
                    }
                } catch (Throwable error) {
                    failure.compareAndSet(null, error);
                }
            };
            threads.add((t & 1) == 0 ? Thread.ofPlatform().start(run) : Thread.ofVirtual().start(run));
        }
        for (Thread thread : threads) thread.join();
        if (failure.get() != null) throw new AssertionError("parallel parity", failure.get());
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !(args[0].equals("java") || args[0].equals("native"))) {
            throw new IllegalArgumentException("mode must be java or native");
        }
        nativeMode = args[0].equals("native");
        if (nativeMode) {
            String path = System.getProperty("m3.concat.native");
            if (path == null || path.isBlank()) throw new IllegalArgumentException("missing native library");
            System.load(path);
        }
        directed();
        exhaustive();
        randomized();
        refusal();
        concurrency();
        System.out.println("M3_FIXED_WORD_CONCAT_PASS mode=" + args[0] + " cases=" + cases
                + " checks=" + CHECKS.get() + " digest=" + Long.toUnsignedString(digest, 16));
    }
}
