/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: GPL-2.0-only
 */
/*
 * @test
 * @summary M3IO borrowed buffers, masked page offsets and logical projection parity
 * @modules java.base/jdk.internal.m3.io
 * @run main/othervm M3IOTest
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import jdk.internal.m3.io.M3IO;

public final class M3IOTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        copy();
        zeroProgress();
        exactRead();
        maskedOffsets();
        projectionAndSeams();
        randomizedProjections();
        invalidMetadataDoesNotWrite();
        System.out.println("M3IO passed checks=" + checks);
    }

    private static void copy() throws Exception {
        for (int length : new int[] {0, 1, 7, 4096, 65_537}) {
            byte[] input = new byte[length];
            new Random(1729 + length).nextBytes(input);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            long count = M3IO.transfer(new ByteArrayInputStream(input), output, new byte[17]);
            check(count == input.length, "transfer length");
            check(Arrays.equals(input, output.toByteArray()), "transfer bytes");
        }
        rejects(IllegalArgumentException.class,
                () -> M3IO.transfer(new ByteArrayInputStream(new byte[0]),
                        new ByteArrayOutputStream(), new byte[0]));
    }

    private static void zeroProgress() throws Exception {
        byte[] input = new byte[] {0, 1, (byte) 255, 4, 6};
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        long count = M3IO.transfer(zeroFirst(input), output, new byte[2]);
        check(count == input.length, "zero-progress transfer count");
        check(Arrays.equals(input, output.toByteArray()), "zero-progress transfer bytes");
    }

    private static InputStream zeroFirst(byte[] input) {
        return new ByteArrayInputStream(input) {
            private boolean first = true;
            @Override public synchronized int read(byte[] dst, int offset, int length) {
                if (first && length != 0) {
                    first = false;
                    return 0;
                }
                return super.read(dst, offset, length);
            }
        };
    }

    private static void exactRead() throws Exception {
        byte[] destination = new byte[8];
        Arrays.fill(destination, (byte) 9);
        M3IO.readExactly(zeroFirst(new byte[] {1, 2, 3}), destination, 2, 3);
        check(Arrays.equals(destination, new byte[] {9, 9, 1, 2, 3, 9, 9, 9}),
                "borrowed destination offset and zero-progress fallback");
        M3IO.readExactly(new ByteArrayInputStream(new byte[0]), destination, 8, 0);
        rejects(EOFException.class,
                () -> M3IO.readExactly(new ByteArrayInputStream(new byte[] {1}),
                        destination, 0, 2));
        rejects(IndexOutOfBoundsException.class,
                () -> M3IO.readExactly(new ByteArrayInputStream(new byte[0]),
                        destination, 7, 2));
    }

    private static void maskedOffsets() throws Exception {
        byte[][] pages = {bytes("xHELLOy"), null, bytes("*WORLD*")};
        int[] offsets = {1, 0, 1};
        int[] lengths = {5, 0, 5};
        long[] active = {0b101};
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        long count = M3IO.writeMasked(output, pages, offsets, lengths, active);
        check(count == 10, "masked length");
        check("HELLOWORLD".equals(output.toString(StandardCharsets.UTF_8)),
                "masked ranges do not materialize row entries");
        check(pages[1] == null, "masked-off page remains absent");
    }

    private static void projectionAndSeams() throws Exception {
        byte[][] pages = new byte[70][];
        int[] offsets = new int[70];
        int[] lengths = new int[70];
        pages[0] = bytes("Ax"); offsets[0] = 0; lengths[0] = 1;
        pages[65] = bytes("zB"); offsets[65] = 1; lengths[65] = 1;
        pages[69] = bytes("CC"); offsets[69] = 0; lengths[69] = 2;
        long[] active = {1L, (1L << 1) | (1L << 5)};
        int[] order = {69, 1, 0, 65, 69};
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        long count = M3IO.writeProjected(output, pages, offsets, lengths, order, 0, order.length, active);
        check(count == 6, "projected byte count and repeated physical slot");
        check("CCABCC".equals(output.toString(StandardCharsets.UTF_8)),
                "logical order and word-boundary mask");
        output.reset();
        check(M3IO.writeProjected(output, pages, offsets, lengths, order, 1, 4, active) == 2,
                "logical subrange length");
        check("AB".equals(output.toString(StandardCharsets.UTF_8)), "logical subrange payload");
    }

    private static void randomizedProjections() throws Exception {
        Random random = new Random(84192);
        for (int run = 0; run < 500; run++) {
            int size = 1 + random.nextInt(140);
            byte[][] pages = new byte[size][];
            int[] offsets = new int[size];
            int[] lengths = new int[size];
            long[] live = new long[(size + 63) >>> 6];
            for (int slot = 0; slot < size; slot++) {
                if (random.nextBoolean()) {
                    byte[] page = new byte[16 + random.nextInt(32)];
                    random.nextBytes(page);
                    pages[slot] = page;
                    offsets[slot] = random.nextInt(page.length + 1);
                    lengths[slot] = random.nextInt(page.length - offsets[slot] + 1);
                    live[slot >>> 6] |= 1L << (slot & 63);
                }
            }
            int[] order = new int[1 + random.nextInt(100)];
            for (int index = 0; index < order.length; index++) order[index] = random.nextInt(size);
            int from = random.nextInt(order.length + 1);
            int to = from + random.nextInt(order.length - from + 1);
            ByteArrayOutputStream expected = new ByteArrayOutputStream();
            for (int logical = from; logical < to; logical++) {
                int physical = order[logical];
                if ((live[physical >>> 6] & (1L << (physical & 63))) != 0) {
                    expected.write(pages[physical], offsets[physical], lengths[physical]);
                }
            }
            ByteArrayOutputStream actual = new ByteArrayOutputStream();
            long count = M3IO.writeProjected(actual, pages, offsets, lengths, order, from, to, live);
            check(count == expected.size(), "randomized projected byte count");
            check(Arrays.equals(actual.toByteArray(), expected.toByteArray()),
                    "randomized projected byte content");
        }
    }

    private static void invalidMetadataDoesNotWrite() throws Exception {
        byte[][] pages = {bytes("one"), bytes("two")};
        int[] offsets = {0, 1};
        int[] lengths = {3, 3};
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        rejects(IndexOutOfBoundsException.class,
                () -> M3IO.writeMasked(output, pages, offsets, lengths, new long[] {0b11}));
        check(output.size() == 0, "preflight preserves output on invalid range");
        rejects(IndexOutOfBoundsException.class,
                () -> M3IO.writeProjected(output, pages, offsets, lengths,
                        new int[] {0, 5}, 0, 2, new long[] {0b11}));
        check(output.size() == 0, "preflight preserves output on invalid index");
        rejects(IllegalArgumentException.class,
                () -> M3IO.writeMasked(output, pages, offsets, lengths, new long[0]));
        check(output.size() == 0, "short mask rejected");
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private interface ThrowingAction { void run() throws Exception; }
    private static void rejects(Class<? extends Throwable> expected, ThrowingAction action) throws Exception {
        try {
            action.run();
            throw new AssertionError("expected " + expected.getName());
        } catch (Exception caught) {
            if (!expected.isInstance(caught)) {
                throw new AssertionError("unexpected " + caught, caught);
            }
            checks++;
        }
    }

    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
}
