// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.lexicon;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Runs unchanged against the pinned incumbent and the candidate; no third-party dependencies. */
public final class M3LexiconTransferRegressionProof {
    private static int checks;
    private static int failures;
    private M3LexiconTransferRegressionProof() { }
    @FunctionalInterface private interface Action { void run() throws Exception; }

    static M3RegexWordTransferRows.Dfa dfa(int count, int badNext, AtomicInteger accepted) {
        return new M3RegexWordTransferRows.Dfa() {
            @Override public int stateCount() { return count; }
            @Override public int next(int state, char unit) {
                return badNext == Integer.MIN_VALUE ? (state + unit) % count : badNext;
            }
            @Override public boolean accepting(int state) {
                accepted.incrementAndGet();
                return state == 1;
            }
        };
    }
    private static void refuses(String label, Class<? extends Throwable> type, Action action) {
        checks++;
        try {
            action.run();
            failures++;
            System.out.println("FAIL accepted " + label);
        } catch (Throwable failure) {
            if (!type.isInstance(failure)) {
                failures++;
                System.out.println("FAIL wrong exception " + label + " " + failure.getClass().getSimpleName());
            }
        }
    }
    private static void check(boolean valid, String label) {
        checks++;
        if (!valid) { failures++; System.out.println("FAIL " + label); }
    }
    static byte[] rehash(byte[] image) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(image, 0, 96);
        digest.update(image, 128, image.length - 128);
        System.arraycopy(digest.digest(), 0, image, 96, 32);
        return image;
    }
    public static void main(String[] args) throws Exception {
        checks = 0; failures = 0;
        var table = M3RegexWordTransferRows.build(dfa(3, Integer.MIN_VALUE, new AtomicInteger()),
                List.of("a", "b", ""));
        if (args.length > 0 && args[0].equals("--legacy")) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (int q = 1; q <= 75; q++) {
                var words = List.of("", "\0", "a", "ab", "\ud800", "\udc00", "😀", "नमस्ते", "é");
                var current = M3RegexWordTransferRows.build(dfa(q, Integer.MIN_VALUE, new AtomicInteger()), words);
                byte[] image = current.image(0x1122334455667788L, new byte[32]);
                digest.update(image);
                var restored = M3RegexWordTransferRows.read(ByteBuffer.wrap(image));
                for (int id = 1; id <= words.size(); id++) {
                    for (int state = 0; state < q; state++) {
                        check(current.step(id, state) == restored.step(id, state), "legacy step");
                        check(current.acceptsInside(id, state) == restored.acceptsInside(id, state), "legacy accept");
                    }
                }
                for (int state = 0; state < q; state++) {
                    check(current.run(new int[]{1, 7, 4, 8, 2, 1}, state)
                            == restored.run(new int[]{1, 7, 4, 8, 2, 1}, state), "legacy run");
                }
            }
            System.out.println("LEGACY_IMAGES " + HexFormat.of().formatHex(digest.digest()));
        } else {
            refuses("column Q aliases next word", IndexOutOfBoundsException.class, () -> table.step(1, 3));
            refuses("column -1 aliases previous word", IndexOutOfBoundsException.class, () -> table.step(2, -1));
            refuses("accept column Q aliases next word", IndexOutOfBoundsException.class, () -> table.acceptsInside(1, 3));
            refuses("accept column -1 aliases previous word", IndexOutOfBoundsException.class, () -> table.acceptsInside(2, -1));
            refuses("zero word plus shifted column", IndexOutOfBoundsException.class, () -> table.step(0, 3));
            refuses("empty run invalid state", IndexOutOfBoundsException.class, () -> table.run(new int[0], -1));
            var q2 = M3RegexWordTransferRows.build(dfa(2, Integer.MIN_VALUE, new AtomicInteger()), List.of("a"));
            refuses("overflowing negative word", IndexOutOfBoundsException.class,
                    () -> q2.step(-2147483647, 0));
            refuses("zero state count", IllegalArgumentException.class,
                    () -> M3RegexWordTransferRows.build(dfa(0, 0, new AtomicInteger()), List.of("a")));
            refuses("negative state count with empty vocab", IllegalArgumentException.class,
                    () -> M3RegexWordTransferRows.build(dfa(-1, 0, new AtomicInteger()), List.of()));
            for (int bad : new int[]{-1, 3, Integer.MAX_VALUE}) {
                AtomicInteger callbacks = new AtomicInteger();
                refuses("invalid DFA transition " + bad, IllegalArgumentException.class,
                        () -> M3RegexWordTransferRows.build(dfa(3, bad, callbacks), List.of("a")));
                check(callbacks.get() == 0, "accepting called with invalid state " + bad);
            }
            for (int at : new int[]{12, 28, 88}) {
                byte[] image = table.image(7, new byte[32]);
                ByteBuffer.wrap(image).putInt(at, 17);
                rehash(image);
                refuses("noncanonical metadata " + at, M3LexiconFormatException.class,
                        () -> M3RegexWordTransferRows.read(ByteBuffer.wrap(image)));
            }
            byte[] padding = table.image(7, new byte[32]);
            int acceptAt = Math.toIntExact(ByteBuffer.wrap(padding).getLong(80));
            ByteBuffer.wrap(padding).putLong(acceptAt, ByteBuffer.wrap(padding).getLong(acceptAt) | Long.MIN_VALUE);
            rehash(padding);
            refuses("nonzero acceptance padding", M3LexiconFormatException.class,
                    () -> M3RegexWordTransferRows.read(ByteBuffer.wrap(padding)));
        }
        System.out.println("TRANSFER_REGRESSION checks=" + checks + " failures=" + failures);
        if (failures != 0) throw new AssertionError("transfer regressions=" + failures);
    }
}
