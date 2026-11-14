/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 * @test
 * @summary Preserve collection mask/snapshot contracts across dense reference materialization
 * @modules java.base/java.util
 * @compile --patch-module java.base=${test.src} CollectionMaskReferenceCopyTest.java
 * @run main/othervm --patch-module java.base=${test.classes} java.util.CollectionMaskReferenceCopyTest
 * @run main/othervm -Xcheck:jni -Djdk.m3.collectionMasks.native=true --patch-module java.base=${test.classes} java.util.CollectionMaskReferenceCopyTest --jdk-native
 */
package java.util;

import java.util.function.Predicate;

/** Scalar expected results are deliberately independent of the optimized mask atom. */
public final class CollectionMaskReferenceCopyTest {
    private CollectionMaskReferenceCopyTest() { }
    private static long checks;
    private static boolean nativeMode;

    public static void main(String[] args) {
        nativeMode = args.length != 0;
        if (nativeMode && !"--jdk-native".equals(args[0])) System.load(args[0]);
        masks();
        snapshotContracts();
        randomizedSnapshots();
        System.out.println("PASS CollectionMaskReferenceCopyTest checks=" + checks
                + " native=" + nativeMode);
    }

    private static void masks() {
        Random random = new Random(0x195a2bc41L);
        int[] sizes = {0, 1, 2, 63, 64, 65, 127, 128, 129, 255, 256, 257,
                511, 512, 513, 1023, 1024, 4095, 4096, 4097, 8191, 8192, 8193};
        for (int size : sizes) {
            for (int kind = 0; kind < 8; kind++) {
                long[] words = new long[(size + 63) >>> 6];
                for (int i = 0; i < words.length; i++) {
                    words[i] = switch (kind) {
                        case 0 -> 0L;
                        case 1 -> -1L;
                        case 2 -> random.nextLong();
                        case 3 -> i % 7 == 0 ? -2L : -1L;
                        case 4 -> i % 2 == 0 ? -1L : 0L;
                        case 5 -> 1L << random.nextInt(64);
                        case 6 -> -1L ^ (1L << random.nextInt(64));
                        default -> i % 3 == 0 ? Long.MIN_VALUE : -1L;
                    };
                }
                long[] before = words.clone();
                checkMask(words, 0, size);
                for (int n = 0; n < 32; n++) {
                    int from = random.nextInt(size + 1);
                    int to = from + random.nextInt(size - from + 1);
                    checkMask(words, from, to);
                }
                truth(Arrays.equals(words, before), "source mask changed");
            }
        }
        for (int trial = 0; trial < 12000; trial++) {
            int length = random.nextInt(65);
            long[] words = new long[length];
            for (int i = 0; i < length; i++) {
                words[i] = switch (trial & 3) {
                    case 0 -> -1L;
                    case 1 -> random.nextLong();
                    case 2 -> random.nextInt(8) == 0 ? random.nextLong() : -1L;
                    default -> random.nextInt(8) == 0 ? random.nextLong() : 0L;
                };
            }
            int capacity = length << 6;
            int from = random.nextInt(capacity + 1);
            int to = from + random.nextInt(capacity - from + 1);
            checkMask(words, from, to);
        }
        refuses(NullPointerException.class, () -> M3CollectionMasks.selectedJava(null, 0, 0));
        refuses(IndexOutOfBoundsException.class, () -> M3CollectionMasks.selectedJava(new long[1], -1, 1));
        refuses(IndexOutOfBoundsException.class, () -> M3CollectionMasks.selectedJava(new long[1], 0, 65));
        refuses(IndexOutOfBoundsException.class, () -> M3CollectionMasks.selectedJava(new long[1], 2, 1));
        if (nativeMode) {
            refuses(NullPointerException.class, () -> M3CollectionMasks.selected0(null, 0, 0));
            refuses(IndexOutOfBoundsException.class, () -> M3CollectionMasks.selected0(new long[1], -1, 1));
            refuses(IndexOutOfBoundsException.class, () -> M3CollectionMasks.selected0(new long[1], 0, 65));
            refuses(IndexOutOfBoundsException.class, () -> M3CollectionMasks.selected0(new long[1], 2, 1));
        }
    }

    private static void checkMask(long[] words, int from, int to) {
        int count = 0;
        for (int i = from; i < to; i++) if ((words[i >>> 6] & (1L << (i & 63))) != 0) count++;
        int[] expected = new int[count];
        int out = 0;
        for (int i = from; i < to; i++) {
            if ((words[i >>> 6] & (1L << (i & 63))) != 0) expected[out++] = i;
        }
        truth(Arrays.equals(expected, M3CollectionMasks.selectedJava(words, from, to)), "Java mask range");
        if (nativeMode) {
            truth(Arrays.equals(expected, M3CollectionMasks.selectedNative(words, from, to)), "native mask range");
        }
    }

    private record Token(int group, int order) { }

    private static void snapshotContracts() {
        List<Token> source = new ArrayList<>(Arrays.asList(new Token(2, 0), null,
                new Token(1, 2), new Token(2, 3), new Token(1, 4), null));
        List<Token> original = new ArrayList<>(source);
        List<Token> calls = new ArrayList<>();
        Predicate<Token> predicate = value -> { calls.add(value); return value == null || value.order != 3; };
        Comparator<Token> comparator = Comparator.nullsLast(Comparator.comparingInt(Token::group));
        List<Token> expected = new ArrayList<>();
        for (Token value : original) if (value == null || value.order != 3) expected.add(value);
        expected.sort(comparator);
        List<Token> result = Collections.filteredSortedSnapshot(source, predicate, (a, b) -> {
            truth(calls.size() == original.size(), "comparator ran before predicate completion");
            return comparator.compare(a, b);
        });
        eq(original, calls, "predicate encounter order and count");
        eq(original, source, "source unchanged");
        eq(expected, result, "stable sorted snapshot");
        truth(result.get(0) == original.get(2), "first tie identity");
        truth(result.get(1) == original.get(4), "second tie identity");
        source.clear();
        eq(expected, result, "detached after source mutation");
        refuses(UnsupportedOperationException.class, () -> result.add(null));
        refuses(UnsupportedOperationException.class, () -> result.set(0, null));
        refuses(UnsupportedOperationException.class, () -> result.remove(0));
        List<Token> reverse = new ArrayList<>(expected);
        Collections.reverse(reverse);
        eq(reverse, result.reversed(), "reversed snapshot");
        eq(expected.subList(1, expected.size()), result.subList(1, result.size()), "snapshot slice");
        eq(reverse.subList(1, reverse.size()), result.reversed().subList(1, result.size()), "reversed slice");
        Object[] exported = result.toArray();
        exported[0] = null;
        eq(expected, result, "export detached");
        eq(original, Collections.filteredSnapshot(original, value -> true), "all-pass capture");
        eq(List.of(), Collections.filteredSnapshot(original, value -> false), "all-reject capture");

        RuntimeException expectedFailure = new IllegalArgumentException("predicate witness");
        int[] observed = {0};
        try {
            Collections.filteredSortedSnapshot(original, value -> {
                if (++observed[0] == 4) throw expectedFailure;
                return true;
            }, (a, b) -> { throw new AssertionError("comparator after predicate failure"); });
            throw new AssertionError("missing predicate failure");
        } catch (RuntimeException failure) {
            truth(failure == expectedFailure, "predicate exception identity");
            truth(observed[0] == 4, "predicate stopped at failing occurrence");
        }
    }

    private static void randomizedSnapshots() {
        Random random = new Random(0x1e3b2c4L);
        for (int trial = 0; trial < 3000; trial++) {
            int length = random.nextInt(1100);
            List<Token> source = new ArrayList<>();
            List<Token> expected = new ArrayList<>();
            for (int i = 0; i < length; i++) {
                Token value = random.nextInt(20) == 0 ? null : new Token(random.nextInt(8), i);
                source.add(value);
            }
            int threshold = trial & 7;
            for (Token value : source) if (value == null || value.group >= threshold) expected.add(value);
            int[] calls = {0};
            List<Token> actual = Collections.filteredSnapshot(source, value -> {
                truth(value == source.get(calls[0]++), "random predicate identity/order");
                return value == null || value.group >= threshold;
            });
            eq(expected, actual, "random stable filter");
            truth(calls[0] == source.size(), "random callback count");
            for (int i = 0; i < expected.size(); i++) truth(expected.get(i) == actual.get(i), "random identity");
        }
    }

    private static void eq(Object expected, Object actual, String why) {
        truth(Objects.equals(expected, actual), why);
    }
    private static void truth(boolean result, String why) {
        checks++;
        if (!result) throw new AssertionError(why);
    }
    private static void refuses(Class<? extends Throwable> kind, Runnable action) {
        checks++;
        try { action.run(); } catch (Throwable failure) {
            if (kind.isInstance(failure)) return;
            throw new AssertionError("wrong exception: " + kind, failure);
        }
        throw new AssertionError("missing " + kind);
    }
}
