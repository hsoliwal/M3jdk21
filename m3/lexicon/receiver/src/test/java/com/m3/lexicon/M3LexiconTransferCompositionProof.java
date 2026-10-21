// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.lexicon;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** Independent direct-DFA oracle, all-entry-state composition and optional real JNI caller proof. */
public final class M3LexiconTransferCompositionProof {
    private static int checks;
    private static boolean nativeEnabled;
    private M3LexiconTransferCompositionProof() { }
    @FunctionalInterface private interface Action { void run() throws Exception; }
    private static native long inspectNative(M3RegexWordTransferRows table, int[] ids,
            int offset, int length, int state);

    private static void check(boolean valid) {
        checks++;
        if (!valid) throw new AssertionError("check " + checks);
    }
    private static void refuses(Class<? extends Throwable> type, Action action) throws Exception {
        checks++;
        try { action.run(); }
        catch (Exception error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("expected " + type.getName(), error);
        }
        throw new AssertionError("missing " + type.getName());
    }
    private static long oracle(M3RegexWordTransferRows.Dfa dfa, List<String> words,
            int[] ids, int offset, int length, int start) {
        int state = start;
        boolean accepting = false;
        for (int i = offset; i < offset + length; i++) {
            String word = words.get(ids[i] - 1);
            for (int c = 0; c < word.length(); c++) {
                state = dfa.next(state, word.charAt(c));
                accepting |= dfa.accepting(state);
            }
        }
        return (accepting ? 1L << 32 : 0) | state;
    }
    private static long observe(M3RegexWordTransferRows.TransferSummary summary, int state) {
        return (summary.acceptsInside(state) ? 1L << 32 : 0) | summary.step(state);
    }
    private static M3RegexWordTransferRows.Dfa randomDfa(Random random, int q, AtomicInteger calls) {
        int[][] next = new int[q][17];
        boolean[] accept = new boolean[q];
        for (int i = 0; i < q; i++) {
            accept[i] = random.nextBoolean();
            for (int j = 0; j < 17; j++) next[i][j] = random.nextInt(q);
        }
        return new M3RegexWordTransferRows.Dfa() {
            @Override public int stateCount() { return q; }
            @Override public int next(int state, char unit) { calls.incrementAndGet(); return next[state][unit % 17]; }
            @Override public boolean accepting(int state) { return accept[state]; }
        };
    }
    private static void composition() throws Exception {
        Random random = new Random(0x4d334c4558L);
        List<String> words = List.of("", "M3", " ", "lexicons", "\0", "\ud800", "\udc00",
                "😀", "नमस्ते", "中文", "a\r\nb", "é", "e\u0301", "regex");
        for (int q : new int[]{1, 2, 3, 7, 63, 64, 65, 129}) {
            for (int trial = 0; trial < 32; trial++) {
                AtomicInteger calls = new AtomicInteger();
                var dfa = randomDfa(random, q, calls);
                var table = M3RegexWordTransferRows.build(dfa, words);
                check(table.stateCount() == q && table.vocabulary() == words.size());
                int n = random.nextInt(13);
                int[] ids = new int[n + 2];
                ids[0] = -1; ids[n + 1] = Integer.MAX_VALUE; // invalid guards are outside the chosen range
                for (int i = 1; i <= n; i++) ids[i] = 1 + random.nextInt(words.size());
                int before = calls.get();
                var full = table.summarize(ids, 1, n);
                check(calls.get() == before); // no character/DFA rescan during phrase precomputation
                check(full.stateCount() == q);
                for (int state = 0; state < q; state++) {
                    long expected = oracle(dfa, words, ids, 1, n, state);
                    check(observe(full, state) == expected);
                    check(table.run(ids, 1, n, state) == (int) expected);
                    if (nativeEnabled && trial < 2) check(inspectNative(table, ids, 1, n, state) == expected);
                }
                for (int split = 0; split <= n; split++) {
                    var left = table.summarize(ids, 1, split);
                    var right = table.summarize(ids, 1 + split, n - split);
                    var joined = left.then(right);
                    for (int state = 0; state < q; state++) check(observe(joined, state) == observe(full, state));
                }
                int a = random.nextInt(n + 1), b = a + random.nextInt(n - a + 1);
                var first = table.summarize(ids, 1, a);
                var second = table.summarize(ids, 1 + a, b - a);
                var third = table.summarize(ids, 1 + b, n - b);
                var l = first.then(second).then(third);
                var r = first.then(second.then(third));
                for (int state = 0; state < q; state++) {
                    check(observe(l, state) == observe(r, state));
                    check(observe(l, state) == observe(full, state));
                }
                int expected0 = full.step(0);
                Arrays.fill(ids, 0);
                check(full.step(0) == expected0); // no input lane retention
            }
        }
    }
    private static void orderedAndEmpty() throws Exception {
        var dfa = new M3RegexWordTransferRows.Dfa() {
            @Override public int stateCount() { return 3; }
            @Override public int next(int state, char unit) {
                if (state == 2) return 2;
                if (unit == 'a') return 1;
                return state == 1 && unit == 'b' ? 2 : 0;
            }
            @Override public boolean accepting(int state) { return state == 2; }
        };
        var table = M3RegexWordTransferRows.build(dfa, List.of("a", "b", ""));
        var a = table.summarize(new int[]{1});
        var b = table.summarize(new int[]{2});
        check(a.then(b).acceptsInside(0));
        check(!b.then(a).acceptsInside(0)); // composition is NOT commutative
        var empty = table.summarize(new int[0]);
        var emptyWord = table.summarize(new int[]{3, 3});
        for (int q = 0; q < 3; q++) {
            check(empty.step(q) == q && !empty.acceptsInside(q));
            check(observe(emptyWord, q) == observe(empty, q));
        }
        check(!empty.acceptsInside(2)); // no zero-width match is invented at entry
        var another = M3RegexWordTransferRows.build(dfa, List.of("a", "b", ""));
        refuses(IllegalArgumentException.class, () -> a.then(another.summarize(new int[]{1})));
        refuses(NullPointerException.class, () -> a.then(null));
        for (int state : new int[]{-1, 3, Integer.MAX_VALUE}) {
            refuses(IndexOutOfBoundsException.class, () -> a.step(state));
            refuses(IndexOutOfBoundsException.class, () -> a.acceptsInside(state));
            refuses(IndexOutOfBoundsException.class, () -> table.run(new int[0], 0, 0, state));
        }
        for (int id : new int[]{0, -1, Integer.MIN_VALUE, 4, Integer.MAX_VALUE}) {
            refuses(IndexOutOfBoundsException.class, () -> table.summarize(new int[]{1, id}));
        }
        for (int[] range : new int[][]{{-1, 0}, {0, -1}, {2, 0}, {1, 1}, {Integer.MAX_VALUE, 1}, {0, Integer.MAX_VALUE}}) {
            refuses(IndexOutOfBoundsException.class, () -> table.summarize(new int[]{1}, range[0], range[1]));
            refuses(IndexOutOfBoundsException.class, () -> table.run(new int[]{1}, range[0], range[1], 0));
        }
        refuses(NullPointerException.class, () -> table.summarize(null));
        check(table.run(new int[]{1, 2}, 0) == table.run(new int[]{1, 2}, 0, 2, 0));
        if (nativeEnabled) {
            refuses(IndexOutOfBoundsException.class, () -> inspectNative(table, new int[]{0}, 0, 1, 0));
            refuses(IndexOutOfBoundsException.class, () -> inspectNative(table, new int[]{1}, 0, 1, 3));
            refuses(IndexOutOfBoundsException.class, () -> inspectNative(table, new int[]{1}, -1, 1, 0));
            refuses(NullPointerException.class, () -> inspectNative(table, null, 0, 0, 0));
        }
    }
    private static void identityAndImages() throws Exception {
        var dfa = randomDfa(new Random(55), 7, new AtomicInteger());
        var table = M3RegexWordTransferRows.build(dfa, List.of("ab", "", "नमस्ते", "\ud800"));
        byte[] pattern = new byte[32]; new Random(42).nextBytes(pattern);
        long generation = 0x1122334455667788L;
        byte[] bytes = table.image(generation, pattern);
        for (boolean direct : new boolean[]{false, true}) {
            ByteBuffer image = direct ? ByteBuffer.allocateDirect(bytes.length) : ByteBuffer.allocate(bytes.length);
            image.put(bytes).position(7).mark().order(ByteOrder.LITTLE_ENDIAN);
            var restored = M3RegexWordTransferRows.read(image, generation, pattern);
            check(image.position() == 7 && image.order() == ByteOrder.LITTLE_ENDIAN && image.reset().position() == 7);
            var summary = restored.summarize(new int[]{1, 3, 4, 2});
            for (int q = 0; q < 7; q++) check(observe(summary, q) == oracle(dfa, List.of("ab", "", "नमस्ते", "\ud800"), new int[]{1, 3, 4, 2}, 0, 4, q));
            refuses(M3LexiconFormatException.class, () -> M3RegexWordTransferRows.read(image, generation ^ 1, pattern));
            byte[] wrong = pattern.clone(); wrong[31] ^= 1;
            refuses(M3LexiconFormatException.class, () -> M3RegexWordTransferRows.read(image, generation, wrong));
            refuses(IllegalArgumentException.class, () -> M3RegexWordTransferRows.read(image, generation, new byte[31]));
            refuses(NullPointerException.class, () -> M3RegexWordTransferRows.read(image, generation, null));
            image.putInt(128, 6); // heap-copy reader result remains immutable after input mutation
            check(restored.step(1, 0) == table.step(1, 0));
        }
        for (int size : new int[]{0, 7, 8, 64, 127, bytes.length - 1}) {
            ByteBuffer truncated = ByteBuffer.wrap(Arrays.copyOf(bytes, size));
            refuses(M3LexiconFormatException.class, () -> M3RegexWordTransferRows.read(truncated, generation, pattern));
        }
        for (int at : new int[]{8, 16, 20, 24, 72, 80, 128}) {
            byte[] bad = bytes.clone(); ByteBuffer.wrap(bad).putInt(at, -1);
            M3LexiconTransferRegressionProof.rehash(bad);
            refuses(M3LexiconFormatException.class, () -> M3RegexWordTransferRows.read(ByteBuffer.wrap(bad)));
        }
        byte[] bad = bytes.clone(); bad[bad.length - 1] ^= 1;
        refuses(M3LexiconFormatException.class, () -> M3RegexWordTransferRows.read(ByteBuffer.wrap(bad)));
        check(Arrays.equals(bytes, M3RegexWordTransferRows.read(ByteBuffer.wrap(bytes), generation, pattern).image(generation, pattern)));
        var emptyTable = M3RegexWordTransferRows.build(dfa, List.of());
        var restoredEmpty = M3RegexWordTransferRows.read(ByteBuffer.wrap(emptyTable.image(generation, pattern)), generation, pattern);
        check(restoredEmpty.vocabulary() == 0 && restoredEmpty.summarize(new int[0]).step(6) == 6);
    }
    private static void concurrentRead() throws Exception {
        var dfa = randomDfa(new Random(9), 65, new AtomicInteger());
        var table = M3RegexWordTransferRows.build(dfa, List.of("hello", " ", "world"));
        var summary = table.summarize(new int[]{1, 2, 3});
        try (var workers = Executors.newFixedThreadPool(4)) {
            List<Callable<Boolean>> work = java.util.stream.IntStream.range(0, 16).mapToObj(i ->
                    (Callable<Boolean>) () -> {
                        for (int k = 0; k < 128; k++) {
                            int q = k % 65;
                            if (observe(summary, q) != observe(table.summarize(new int[]{1, 2, 3}), q)) return false;
                        }
                        return true;
                    }).toList();
            for (var future : workers.invokeAll(work)) check(future.get());
        }
    }
    public static void main(String[] args) throws Exception {
        checks = 0;
        nativeEnabled = args.length == 2 && args[0].equals("--jni");
        if (args.length != 0 && !nativeEnabled) throw new IllegalArgumentException("[--jni ABSOLUTE_LIBRARY]");
        if (nativeEnabled) System.load(args[1]);
        composition(); orderedAndEmpty(); identityAndImages(); concurrentRead();
        System.out.println("TRANSFER_COMPOSITION checks=" + checks + " seededCases=256 JNI=" + nativeEnabled);
    }
}
