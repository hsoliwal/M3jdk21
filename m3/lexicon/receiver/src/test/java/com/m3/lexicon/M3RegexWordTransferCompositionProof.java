// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.lexicon;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** Direct UTF-16 oracle, serial/composed/repeated transfers, and sidecar binding refusals. */
public final class M3RegexWordTransferCompositionProof {
    private static long checks;
    private M3RegexWordTransferCompositionProof() { }
    @FunctionalInterface private interface Checked { void run() throws Exception; }
    private static void check(boolean value) {
        checks++;
        if (!value) throw new AssertionError("check " + checks);
    }
    private static void refuses(Class<? extends Throwable> type, Checked action) throws Exception {
        checks++;
        try { action.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("wrong refusal", failure);
        }
        throw new AssertionError("missing refusal " + type.getSimpleName());
    }
    private static M3RegexWordTransferRows.Dfa dfa(int q, int salt) {
        return new M3RegexWordTransferRows.Dfa() {
            public int stateCount() { return q; }
            public int next(int state, char unit) {
                return Math.floorMod(state * 31 + unit * 17 + salt, q);
            }
            public boolean accepting(int state) { return (state % 3) == 0; }
        };
    }
    private static long direct(M3RegexWordTransferRows.Dfa dfa, List<String> words, int[] ids,
            int from, int to, int start, int copies) {
        int state = start; boolean accepted = false;
        for (int repeat = 0; repeat < copies; repeat++) {
            for (int i = from; i < to; i++) {
                String word = words.get(ids[i] - 1);
                for (int u = 0; u < word.length(); u++) {
                    state = dfa.next(state, word.charAt(u));
                    accepted |= dfa.accepting(state);
                }
            }
        }
        return (accepted ? 1L << 32 : 0) | state;
    }
    private static void agrees(M3RegexWordTransferRows.Summary summary, int state, long expected) {
        check(summary.step(state) == (int) expected);
        check(summary.acceptsInside(state) == ((expected >>> 32) != 0));
    }
    private static void composition() {
        Random random = new Random(0x525857303031L);
        List<String> words = List.of("", "x", "X", " ", "\0", "\r\n", "\ud800", "\udc00",
                "\ud83d\ude42", "e\u0301", "é", "नमस्ते", "東京", "ß", "Σ", "HTTPServer");
        for (int trial = 0; trial < 10_000; trial++) {
            int q = 1 + random.nextInt(31);
            var dfa = dfa(q, random.nextInt(1000));
            var rows = M3RegexWordTransferRows.build(dfa, words);
            check(rows.stateCount() == q);
            int[] ids = new int[random.nextInt(33)];
            for (int i = 0; i < ids.length; i++) ids[i] = 1 + random.nextInt(words.size());
            int first = random.nextInt(ids.length + 1);
            int second = first + random.nextInt(ids.length - first + 1);
            var a = rows.summarize(ids, 0, first);
            var b = rows.summarize(ids, first, second);
            var c = rows.summarize(ids, second, ids.length);
            var left = a.then(b).then(c);
            var right = a.then(b.then(c));
            var whole = rows.summarize(ids);
            var repeated = b.repeat(3);
            var identity = whole.repeat(0);
            for (int state = 0; state < q; state++) {
                long expected = direct(dfa, words, ids, 0, ids.length, state, 1);
                agrees(left, state, expected); agrees(right, state, expected); agrees(whole, state, expected);
                check(rows.run(ids, state) == (int) expected);
                check(rows.run(ids, first, second, state) == (int) direct(dfa, words, ids, first, second, state, 1));
                agrees(repeated, state, direct(dfa, words, ids, first, second, state, 3));
                agrees(identity, state, state); // No initial/zero-width acceptance is invented.
                agrees(identity.then(whole), state, expected);
                agrees(whole.then(identity), state, expected);
            }
            Arrays.fill(ids, 0); // Summary must not retain the caller's mutable coordinate lane.
            for (int state = 0; state < q; state++) check(left.step(state) == whole.step(state));
        }
        System.out.println("COMPOSITION seededCases=10000 checks=" + checks);
    }
    private static void wideStateLanes() {
        List<String> words = List.of("", "a", "\ud83d", "\ude42", " ", "東京");
        int[] ids = {2, 3, 4, 5, 6, 1};
        for (int q : new int[]{63, 64, 65, 127, 128, 129, 257}) {
            var dfa = dfa(q, 7);
            var rows = M3RegexWordTransferRows.build(dfa, words);
            var left = rows.summarize(ids, 0, 3);
            var right = rows.summarize(ids, 3, ids.length);
            var combined = left.then(right);
            for (int copies : new int[]{0, 1, 2, 3, 4, 7, 16, 65}) {
                var powered = combined.repeat(copies);
                for (int state = 0; state < q; state++)
                    agrees(powered, state, direct(dfa, words, ids, 0, ids.length, state, copies));
            }
        }
        System.out.println("WIDE_LANES stateCounts=63,64,65,127,128,129,257");
    }

    private static void scale() {
        List<String> words = new ArrayList<>();
        for (int i = 0; i < 60000; i++) words.add("w" + Integer.toString(100000 + i));
        var dfa = dfa(17, 11);
        var rows = M3RegexWordTransferRows.build(dfa, words);
        int[] ids = new int[words.size()];
        for (int i = 0; i < ids.length; i++) ids[i] = i + 1;
        var summary = rows.summarize(ids);
        var halves = rows.summarize(ids, 0, 30000).then(rows.summarize(ids, 30000, ids.length));
        for (int state = 0; state < 17; state++) {
            long expected = direct(dfa, words, ids, 0, ids.length, state, 1);
            agrees(summary, state, expected);
            agrees(halves, state, expected);
            check(rows.run(ids, state) == (int) expected);
        }
        for (int id : ids) {
            long expected = direct(dfa, words, new int[]{id}, 0, 1, 0, 1);
            check(rows.step(id, 0) == (int) expected);
            check(rows.acceptsInside(id, 0) == ((expected >>> 32) != 0));
        }
        System.out.println("SCALE syntheticWords=60000 states=17 cells=1020000 checks=" + checks);
    }

    private static void imageBinding() throws Exception {
        var rows = M3RegexWordTransferRows.build(dfa(7, 3), List.of("", "a", "\ud800", "東京"));
        byte[] key = new byte[32]; Arrays.fill(key, (byte) 0x7b);
        long generation = 0x123456789abcdef0L;
        byte[] image = rows.image(generation, key);
        ByteBuffer direct = ByteBuffer.allocateDirect(image.length).put(image);
        direct.position(17); direct.mark(); direct.order(ByteOrder.LITTLE_ENDIAN);
        var read = M3RegexWordTransferRows.read(direct.asReadOnlyBuffer(), generation, key);
        check(direct.position() == 17 && direct.order() == ByteOrder.LITTLE_ENDIAN); direct.reset();
        check(Arrays.equals(image, read.image(generation, key)));
        refuses(M3LexiconFormatException.class,
                () -> M3RegexWordTransferRows.read(ByteBuffer.wrap(image), generation ^ 1, key));
        refuses(M3LexiconFormatException.class,
                () -> M3RegexWordTransferRows.read(ByteBuffer.wrap(image), generation ^ Long.MIN_VALUE, key));
        byte[] wrong = key.clone(); wrong[31] ^= 1;
        refuses(M3LexiconFormatException.class,
                () -> M3RegexWordTransferRows.read(ByteBuffer.wrap(image), generation, wrong));
        refuses(IllegalArgumentException.class,
                () -> M3RegexWordTransferRows.read(ByteBuffer.wrap(image), generation, new byte[31]));
        refuses(NullPointerException.class,
                () -> M3RegexWordTransferRows.read(ByteBuffer.wrap(image), generation, null));
        for (int length = 0; length < image.length; length++) {
            byte[] truncated = Arrays.copyOf(image, length);
            refuses(M3LexiconFormatException.class,
                    () -> M3RegexWordTransferRows.read(ByteBuffer.wrap(truncated), generation, key));
        }
        // Matching caller identity does not waive the digest or target-state gates.
        byte[] changed = image.clone(); changed[128] ^= 1;
        refuses(M3LexiconFormatException.class,
                () -> M3RegexWordTransferRows.read(ByteBuffer.wrap(changed), generation, key));
        byte[] invalid = image.clone(); ByteBuffer.wrap(invalid).putInt(128, 7);
        M3RegexWordTransferBoundaryProof.seal(invalid);
        refuses(M3LexiconFormatException.class,
                () -> M3RegexWordTransferRows.read(ByteBuffer.wrap(invalid), generation, key));
        // Source mutation AFTER admission cannot change the retained primitive arrays.
        Arrays.fill(image, (byte) 0); Arrays.fill(key, (byte) 0);
        check(read.step(2, 0) == rows.step(2, 0));
    }
    private static void limitsAndOwnership() throws Exception {
        var rows = M3RegexWordTransferRows.build(dfa(3, 0), List.of("", "a", "b"));
        var foreign = M3RegexWordTransferRows.build(dfa(3, 1), List.of("", "a", "b"));
        var s = rows.summarize(new int[]{2, 3});
        refuses(IllegalArgumentException.class, () -> s.then(foreign.summarize(new int[]{2})));
        // Even equal geometry and equal table bytes cannot implicitly change ownership.
        var reloaded = M3RegexWordTransferRows.read(ByteBuffer.wrap(rows.image(1, new byte[32])));
        refuses(IllegalArgumentException.class, () -> s.then(reloaded.summarize(new int[]{2})));
        refuses(NullPointerException.class, () -> s.then(null));
        refuses(IllegalArgumentException.class, () -> s.repeat(-1));
        for (int state : new int[]{-1, 3, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            refuses(IndexOutOfBoundsException.class, () -> s.step(state));
            refuses(IndexOutOfBoundsException.class, () -> s.acceptsInside(state));
            refuses(IndexOutOfBoundsException.class, () -> rows.run(new int[0], 0, 0, state));
        }
        int[] ids = {2, 0, 3};
        refuses(IndexOutOfBoundsException.class, () -> rows.summarize(ids));
        refuses(IndexOutOfBoundsException.class, () -> rows.summarize(ids, -1, 1));
        refuses(IndexOutOfBoundsException.class, () -> rows.summarize(ids, 2, 1));
        refuses(IndexOutOfBoundsException.class, () -> rows.summarize(ids, 0, 4));
        refuses(IndexOutOfBoundsException.class, () -> rows.run(ids, 0, 3, 0));
        check(rows.summarize(ids, 0, 1).step(0) == rows.step(2, 0));
        var empty = rows.summarize(ids, 1, 1);
        for (int q = 0; q < 3; q++) agrees(empty, q, q);
        var noWords = M3RegexWordTransferRows.build(dfa(3, 0), List.of());
        for (int q = 0; q < 3; q++) agrees(noWords.summarize(new int[0]), q, q);
        check(s.repeat(1) == s);
        var cycle = new M3RegexWordTransferRows.Dfa() {
            public int stateCount() { return 5; }
            public int next(int state, char unit) { return (state + 1) % 5; }
            public boolean accepting(int state) { return state == 0; }
        };
        var repeated = M3RegexWordTransferRows.build(cycle, List.of("x")).summarize(new int[]{1}).repeat(Long.MAX_VALUE);
        for (int q = 0; q < 5; q++) {
            check(repeated.step(q) == (q + Long.MAX_VALUE % 5) % 5);
            check(repeated.acceptsInside(q));
        }
    }
    private static void noRescanAndConcurrency() throws Exception {
        AtomicInteger characterReads = new AtomicInteger();
        CharSequence word = new CharSequence() {
            public int length() { return 3; }
            public char charAt(int index) { characterReads.incrementAndGet(); return "abc".charAt(index); }
            public CharSequence subSequence(int start, int end) { throw new AssertionError("materialized"); }
            public String toString() { throw new AssertionError("materialized"); }
        };
        var rows = M3RegexWordTransferRows.build(dfa(7, 1), List.of(word));
        int reads = characterReads.get();
        var summary = rows.summarize(new int[]{1, 1, 1});
        var repeated = summary.repeat(1000000);
        check(characterReads.get() == reads);
        try (var workers = Executors.newFixedThreadPool(4)) {
            List<java.util.concurrent.Future<Long>> work = new ArrayList<>();
            for (int task = 0; task < 16; task++) work.add(workers.submit(() -> {
                long value = 0;
                for (int i = 0; i < 10000; i++) {
                    int state = i % 7;
                    value += repeated.step(state);
                    value += repeated.acceptsInside(state) ? 1 : 0;
                }
                return value;
            }));
            long expected = work.getFirst().get();
            for (var future : work) check(future.get() == expected);
        }
        check(characterReads.get() == reads);
        System.out.println("NO_RESCAN characterReads=" + reads + " concurrentTasks=16 queriesPerTask=10000");
    }
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("scale")) { scale(); return; }
        composition(); wideStateLanes(); imageBinding(); limitsAndOwnership(); noRescanAndConcurrency();
        System.out.println("COMPOSITION_PASS checks=" + checks + " modelCalls=0 nativeCalls=0");
    }
}
