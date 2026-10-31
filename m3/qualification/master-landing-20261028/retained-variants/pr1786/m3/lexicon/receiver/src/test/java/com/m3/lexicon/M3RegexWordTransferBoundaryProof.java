// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.lexicon;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;

/** Executable regression and baseline observations against the real production owner. */
public final class M3RegexWordTransferBoundaryProof {
    private static int checks;
    private static int failures;
    private M3RegexWordTransferBoundaryProof() { }
    @FunctionalInterface private interface Checked { void run() throws Exception; }
    private static void require(boolean value) {
        checks++;
        if (!value) throw new AssertionError("check " + checks);
    }
    private static void rejects(String label, Class<? extends Throwable> type, Checked action) {
        checks++;
        try { action.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError(label + ": wrong failure " + failure, failure);
        }
        failures++;
        System.out.println("FAIL accepted " + label);
    }
    private static M3RegexWordTransferRows.Dfa dfa(int states) {
        return new M3RegexWordTransferRows.Dfa() {
            public int stateCount() { return states; }
            public int next(int state, char unit) { return (state + unit) % states; }
            public boolean accepting(int state) { return state == 0; }
        };
    }
    private static void boundaries() throws Exception {
        var rows = M3RegexWordTransferRows.build(dfa(3), List.of("a", "b", ""));
        for (int state : new int[]{-1, 3, 4}) {
            rejects("step state " + state, IndexOutOfBoundsException.class, () -> rows.step(2, state));
            rejects("accept state " + state, IndexOutOfBoundsException.class, () -> rows.acceptsInside(2, state));
            rejects("empty run state " + state, IndexOutOfBoundsException.class, () -> rows.run(new int[0], state));
        }
        for (int cwid : new int[]{0, -1, 4, Integer.MAX_VALUE, Integer.MIN_VALUE}) {
            rejects("step CWID " + cwid, IndexOutOfBoundsException.class, () -> rows.step(cwid, 0));
            rejects("accept CWID " + cwid, IndexOutOfBoundsException.class, () -> rows.acceptsInside(cwid, 0));
        }
        // A legal flat-array address must not certify the two logical coordinates.
        rejects("zero CWID + state Q", IndexOutOfBoundsException.class, () -> rows.step(0, 3));
        rejects("CWID V+1 + negative state", IndexOutOfBoundsException.class, () -> rows.step(4, -1));
        var twoStates = M3RegexWordTransferRows.build(dfa(2), List.of("a", "b"));
        rejects("wrapped CWID multiplication", IndexOutOfBoundsException.class,
                () -> twoStates.step(Integer.MIN_VALUE + 1, 0));
        for (int q : new int[]{0, -1, Integer.MIN_VALUE}) {
            rejects("nonpositive states " + q, IllegalArgumentException.class,
                    () -> M3RegexWordTransferRows.build(dfa(q), List.of()));
        }
        for (int transition : new int[]{-1, 3, Integer.MAX_VALUE}) {
            var bad = new M3RegexWordTransferRows.Dfa() {
                public int stateCount() { return 3; }
                public int next(int state, char unit) { return transition; }
                public boolean accepting(int state) { return false; }
            };
            rejects("DFA result " + transition, IllegalArgumentException.class,
                    () -> M3RegexWordTransferRows.build(bad, List.of("a")));
        }
        byte[] image = rows.image(0x1122334455667788L, new byte[32]);
        for (int length = 0; length < image.length; length++) {
            final int n = length;
            rejects("truncated " + n, M3LexiconFormatException.class,
                    () -> M3RegexWordTransferRows.read(ByteBuffer.wrap(Arrays.copyOf(image, n))));
        }
        byte[] corrupt = image.clone(); corrupt[corrupt.length - 1] ^= 1;
        rejects("digest", M3LexiconFormatException.class,
                () -> M3RegexWordTransferRows.read(ByteBuffer.wrap(corrupt)));
        byte[] wrongState = image.clone(); ByteBuffer.wrap(wrongState).putInt(128, 3);
        seal(wrongState);
        rejects("rehashed invalid state", M3LexiconFormatException.class,
                () -> M3RegexWordTransferRows.read(ByteBuffer.wrap(wrongState)));
        byte[] oversized = image.clone();
        ByteBuffer.wrap(oversized).putInt(16, Integer.MAX_VALUE).putInt(20, Integer.MAX_VALUE);
        seal(oversized);
        rejects("overflow geometry", M3LexiconFormatException.class,
                () -> M3RegexWordTransferRows.read(ByteBuffer.wrap(oversized)));
        System.out.println("BOUNDARY checks=" + checks + " failures=" + failures);
        if (failures != 0) throw new AssertionError("invalid states/coordinates admitted: " + failures);
    }
    static void seal(byte[] image) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(image, 0, 96); digest.update(image, 128, image.length - 128);
        System.arraycopy(digest.digest(), 0, image, 96, 32);
    }
    private static void legacy() throws Exception {
        Random random = new Random(0x4d334c4558L);
        for (int trial = 0; trial < 256; trial++) {
            int q = 1 + random.nextInt(19);
            var dfa = dfa(q);
            List<String> words = List.of("", "x", "X", "a\0b", "\ud800", "\udc00", "\ud83d\ude42",
                    "नमस्ते", "東京", "e\u0301", "é", " ", "\r\n", "HTTPServer", "ß", "Σ");
            var rows = M3RegexWordTransferRows.build(dfa, words);
            byte[] key = new byte[32]; random.nextBytes(key); long generation = random.nextLong();
            byte[] image = rows.image(generation, key);
            ByteBuffer buffer = ByteBuffer.wrap(image).order(ByteOrder.LITTLE_ENDIAN);
            buffer.position(Math.min(37, image.length)); buffer.mark();
            var read = M3RegexWordTransferRows.read(buffer.asReadOnlyBuffer());
            require(buffer.position() == 37 && buffer.order() == ByteOrder.LITTLE_ENDIAN);
            buffer.reset();
            require(Arrays.equals(image, read.image(generation, key)));
            MessageDigest observations = MessageDigest.getInstance("SHA-256");
            for (int cwid = 1; cwid <= words.size(); cwid++) {
                String word = words.get(cwid - 1);
                for (int state = 0; state < q; state++) {
                    int expected = state; boolean accepted = false;
                    for (int i = 0; i < word.length(); i++) {
                        expected = dfa.next(expected, word.charAt(i));
                        accepted |= dfa.accepting(expected);
                    }
                    require(rows.step(cwid, state) == expected);
                    require(read.step(cwid, state) == expected);
                    require(rows.acceptsInside(cwid, state) == accepted);
                    require(read.acceptsInside(cwid, state) == accepted);
                    observations.update((byte) expected); observations.update((byte) (accepted ? 1 : 0));
                }
            }
            int[] sequence = new int[random.nextInt(101)];
            for (int i = 0; i < sequence.length; i++) sequence[i] = 1 + random.nextInt(words.size());
            for (int state = 0; state < q; state++) {
                int expected = state;
                for (int id : sequence) for (int i = 0; i < words.get(id - 1).length(); i++)
                    expected = dfa.next(expected, words.get(id - 1).charAt(i));
                require(rows.run(sequence, state) == expected);
                require(read.run(sequence, state) == expected);
                observations.update((byte) expected);
            }
            System.out.println("LEGACY " + trial + " " + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(image))
                    + " " + HexFormat.of().formatHex(observations.digest()));
        }
        System.out.println("LEGACY_PASS trials=256 checks=" + checks);
    }
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("legacy")) legacy(); else boundaries();
    }
}
