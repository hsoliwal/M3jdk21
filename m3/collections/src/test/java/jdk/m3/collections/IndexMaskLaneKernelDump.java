// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Random;

/**
 * Reflective dump driver of the differential gate of recipe entryless-lane-kernel-20261106 (not a test).
 *
 * <p>Replays the same seeded key/value lanes, windows and signed ranges through a named lane-kernel class
 * (the donor {@code EntrylessLaneKernel} compiled from the donor tree, or the relocated
 * {@code IndexMaskCompactor}) and prints one digest over every result: selectJava/select slots,
 * selectIntoJava/selectInto counts and output lanes (with guard words), countMatching(+Java),
 * firstMatching(+Java), scanPageIntoJava/scanPageInto page cursors and pages, and the exception class
 * of a few declared invalid calls. Methods are resolved by name and parameter types only, so one
 * source serves both sides. Usage: {@code <class> <ops> <seed>}.
 */
public final class IndexMaskLaneKernelDump {
    private static final Class<?>[] QUERY = {long[].class, long[].class, byte[].class, int.class, int.class,
            long.class, long.class, long.class, long.class};
    private static final Class<?>[] INTO = {long[].class, long[].class, byte[].class, int.class, int.class,
            long.class, long.class, long.class, long.class, int[].class, int.class};

    private IndexMaskLaneKernelDump() { }

    public static void main(String[] args) throws Exception {
        Class<?> kernel = Class.forName(args[0]);
        int ops = Integer.parseInt(args[1]);
        long seed = Long.parseLong(args[2]);
        Method selectJava = kernel.getMethod("selectJava", QUERY);
        Method select = kernel.getMethod("select", QUERY);
        Method selectIntoJava = kernel.getMethod("selectIntoJava", INTO);
        Method selectInto = kernel.getMethod("selectInto", INTO);
        Method countJava = kernel.getMethod("countMatchingJava", QUERY);
        Method count = kernel.getMethod("countMatching", QUERY);
        Method firstJava = kernel.getMethod("firstMatchingJava", QUERY);
        Method first = kernel.getMethod("firstMatching", QUERY);
        Method pageJava = kernel.getMethod("scanPageIntoJava", INTO);
        Method page = kernel.getMethod("scanPageInto", INTO);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        ByteBuffer word = ByteBuffer.allocate(8);
        Random random = new Random(seed);
        long slots = 0, matches = 0, pages = 0;
        for (int op = 0; op < ops; op++) {
            int length = random.nextInt(16) == 0 ? 600 + random.nextInt(2000) : random.nextInt(300);
            long[] keys = new long[length];
            long[] values = new long[length];
            byte[] state = new byte[length];
            int spread = 1 + random.nextInt(3) * 500;
            for (int i = 0; i < length; i++) {
                keys[i] = extreme(random, random.nextInt(2 * spread + 1) - spread);
                values[i] = extreme(random, random.nextInt(2 * spread + 1) - spread);
                state[i] = (byte) (random.nextInt(4) == 0 ? random.nextInt(256) : 1);
            }
            int start = random.nextInt(length + 1);
            int cnt = random.nextInt(length - start + 1);
            long[] range = range(random, spread);
            slots += cnt;
            Object[] query = {keys, values, state, start, cnt, range[0], range[1], range[2], range[3]};
            int[] expected = (int[]) selectJava.invoke(null, query);
            int[] selected = (int[]) select.invoke(null, query);
            feed(digest, word, expected);
            feed(digest, word, selected);
            matches += expected.length;
            int offset = random.nextInt(3);
            int[] intoJava = new int[expected.length + offset + 2];
            int[] into = new int[expected.length + offset + 2];
            java.util.Arrays.fill(intoJava, -77);
            java.util.Arrays.fill(into, -77);
            feed(digest, word, (int) selectIntoJava.invoke(null, keys, values, state, start, cnt,
                    range[0], range[1], range[2], range[3], intoJava, offset));
            feed(digest, word, (int) selectInto.invoke(null, keys, values, state, start, cnt,
                    range[0], range[1], range[2], range[3], into, offset));
            feed(digest, word, intoJava);
            feed(digest, word, into);
            feed(digest, word, (int) countJava.invoke(null, query));
            feed(digest, word, (int) count.invoke(null, query));
            feed(digest, word, (int) firstJava.invoke(null, query));
            feed(digest, word, (int) first.invoke(null, query));
            int capacity = 1 + random.nextInt(17);
            int[] bufferJava = new int[capacity + 1];
            int[] buffer = new int[capacity + 1];
            int next = start;
            while (next < start + cnt) {
                java.util.Arrays.fill(bufferJava, -31);
                java.util.Arrays.fill(buffer, -31);
                long j = (long) pageJava.invoke(null, keys, values, state, next, start + cnt - next,
                        range[0], range[1], range[2], range[3], bufferJava, 1);
                long a = (long) page.invoke(null, keys, values, state, next, start + cnt - next,
                        range[0], range[1], range[2], range[3], buffer, 1);
                feed(digest, word, j);
                feed(digest, word, a);
                feed(digest, word, bufferJava);
                feed(digest, word, buffer);
                pages++;
                int following = (int) j;
                if (following <= next) throw new AssertionError("page did not progress at op " + op);
                next = following;
            }
            if (op % 1000 == 0) {
                rejection(digest, select, keys, values, state, 0, Integer.MAX_VALUE, 0, 0, 0, 0);
                rejection(digest, select, keys, values, state, 0, 0, 1, 0, 0, 0);
                rejection(digest, select, null, values, state, 0, 0, 0, 0, 0, 0);
            }
        }
        System.out.println("LANE_DUMP ops=" + ops + " seed=" + seed + " slots=" + slots + " matches=" + matches
                + " pages=" + pages + " digest=" + HexFormat.of().formatHex(digest.digest()));
    }

    private static long extreme(Random random, long value) {
        return switch (random.nextInt(64)) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2 -> Long.MIN_VALUE + 1;
            case 3 -> Long.MAX_VALUE - 1;
            default -> value;
        };
    }

    /** Inclusive key and value ranges; one in eight is the full signed span (the native elision path). */
    private static long[] range(Random random, int spread) {
        long[] range = new long[4];
        for (int lane = 0; lane < 2; lane++) {
            int shape = random.nextInt(8);
            if (shape == 0) {
                range[2 * lane] = Long.MIN_VALUE;
                range[2 * lane + 1] = Long.MAX_VALUE;
            } else if (shape == 1) {
                long point = extreme(random, random.nextInt(2 * spread + 1) - spread);
                range[2 * lane] = point;
                range[2 * lane + 1] = point;
            } else {
                long a = extreme(random, random.nextInt(2 * spread + 1) - spread);
                long b = extreme(random, random.nextInt(2 * spread + 1) - spread);
                range[2 * lane] = Math.min(a, b);
                range[2 * lane + 1] = Math.max(a, b);
            }
        }
        return range;
    }

    private static void rejection(MessageDigest digest, Method select, long[] keys, long[] values, byte[] state,
            int start, int count, long keyMin, long keyMax, long valueMin, long valueMax) throws Exception {
        try {
            select.invoke(null, keys, values, state, start, count, keyMin, keyMax, valueMin, valueMax);
            digest.update((byte) 0);
        } catch (InvocationTargetException refused) {
            digest.update(refused.getCause().getClass().getName().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    private static void feed(MessageDigest digest, ByteBuffer word, int[] lane) {
        feed(digest, word, lane.length);
        for (int value : lane) feed(digest, word, value);
    }

    private static void feed(MessageDigest digest, ByteBuffer word, int value) {
        word.clear();
        word.putInt(value);
        digest.update(word.array(), 0, 4);
    }

    private static void feed(MessageDigest digest, ByteBuffer word, long value) {
        word.clear();
        word.putLong(value);
        digest.update(word.array(), 0, 8);
    }
}
