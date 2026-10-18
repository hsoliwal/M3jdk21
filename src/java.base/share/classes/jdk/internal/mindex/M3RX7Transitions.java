/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.mindex;

import java.util.Arrays;
import java.util.Objects;

/** Primitive state-transfer and vocabulary-closure kernels of the existing RX7 owner. */
final class M3RX7Transitions {
    private static final int MAX_STATES = 4096;
    private static final int MAX_CELLS = 1 << 20;
    private static final long MAX_WORK = 32L << 20;

    private M3RX7Transitions() { }

    /** Reverse reachability through exactly the supplied columns, including an empty suffix. */
    static byte[] useful(short[] rows, int states, int columns, byte[] accepting) {
        checkTable(rows, states, columns);
        checkFlags(accepting, states);
        int[] offsets = new int[states + 1];
        int edges = 0;
        for (short target : rows) {
            checkState(target, states);
            if (target >= 0) {
                offsets[target + 1]++;
                edges++;
            }
        }
        for (int state = 0; state < states; state++) {
            offsets[state + 1] += offsets[state];
        }
        int[] cursor = Arrays.copyOf(offsets, states);
        int[] sources = new int[edges];
        for (int cell = 0; cell < rows.length; cell++) {
            int target = rows[cell];
            if (target >= 0) sources[cursor[target]++] = cell / columns;
        }
        byte[] useful = accepting.clone();
        int[] queue = new int[states];
        int head = 0, tail = 0;
        for (int state = 0; state < states; state++) {
            if (useful[state] != 0) queue[tail++] = state;
        }
        while (head < tail) {
            int target = queue[head++];
            for (int edge = offsets[target]; edge < offsets[target + 1]; edge++) {
                int source = sources[edge];
                if (useful[source] == 0) {
                    useful[source] = 1;
                    queue[tail++] = source;
                }
            }
        }
        return useful;
    }

    /** State-major masks; unused bits in the last word remain zero. */
    static long[] masks(short[] rows, int states, int tokens, byte[] useful) {
        checkTable(rows, states, tokens);
        checkFlags(useful, states);
        int words = (int) (((long) tokens + 63) >>> 6);
        long[] result = new long[states * words];
        for (int state = 0; state < states; state++) {
            int base = state * tokens;
            int maskBase = state * words;
            for (int token = 0; token < tokens; token++) {
                int target = rows[base + token];
                checkState(target, states);
                if (target >= 0 && useful[target] != 0) {
                    result[maskBase + (token >>> 6)] |= 1L << (token & 63);
                }
            }
        }
        return result;
    }

    /**
     * Compose a selected token-id range into one row per entry state. The owning
     * TokenIndex supplies an immutable, already-validated table. Null refuses work
     * beyond the existing 32 Mi state/token budget. No caller array is retained.
     */
    static short[] tokens(short[] rows, int states, int tokens,
            int[] ids, int offset, int length) {
        checkTable(rows, states, tokens);
        Objects.requireNonNull(ids, "ids");
        Objects.checkFromIndexSize(offset, length, ids.length);
        for (int i = offset, end = offset + length; i < end; i++) {
            Objects.checkIndex(ids[i], tokens);
        }
        if ((long) states * length > MAX_WORK) return null;
        short[] result = identity(states);
        for (int state = 0; state < states; state++) {
            int next = state;
            for (int i = offset, end = offset + length; i < end && next >= 0; i++) {
                next = rows[next * tokens + ids[i]];
            }
            result[state] = (short) next;
        }
        return result;
    }

    /** Left-to-right composition: the result for q is right[left[q]]. */
    static short[] compose(short[] left, short[] right) {
        int states = checkTransfer(left);
        if (checkTransfer(right) != states) {
            throw new IllegalArgumentException("different transfer dimensions");
        }
        short[] result = new short[states];
        composeJava(left, right, result);
        return result;
    }

    /** Nonnegative power with bounded live rows and no expanded token sequence. */
    static short[] repeat(short[] rows, long count) {
        int states = checkTransfer(rows);
        if (count < 0) throw new IllegalArgumentException("negative repeat count");
        if (count == 0) return identity(states);
        if (count == 1) return rows.clone();
        short[] result = identity(states);
        short[] power = rows.clone();
        short[] scratch = new short[states];
        for (long remaining = count; remaining != 0; remaining >>>= 1) {
            if ((remaining & 1) != 0) {
                composeJava(result, power, scratch);
                short[] previous = result;
                result = scratch;
                scratch = previous;
            }
            if (remaining > 1) {
                composeJava(power, power, scratch);
                short[] previous = power;
                power = scratch;
                scratch = previous;
            }
        }
        return result;
    }

    private static short[] identity(int states) {
        short[] rows = new short[states];
        for (int state = 0; state < states; state++) rows[state] = (short) state;
        return rows;
    }

    private static void composeJava(short[] left, short[] right, short[] result) {
        for (int state = 0; state < left.length; state++) {
            int intermediate = left[state];
            result[state] = intermediate < 0 ? -1 : right[intermediate];
        }
    }

    private static int checkTransfer(short[] rows) {
        Objects.requireNonNull(rows, "rows");
        int states = rows.length;
        if (states < 1 || states > MAX_STATES) {
            throw new IllegalArgumentException("invalid bounded transfer dimensions");
        }
        for (short state : rows) checkState(state, states);
        return states;
    }

    private static void checkTable(short[] rows, int states, int columns) {
        Objects.requireNonNull(rows, "rows");
        if (states < 1 || states > MAX_STATES || columns < 0
                || (long) states * columns != rows.length || rows.length > MAX_CELLS) {
            throw new IllegalArgumentException("invalid bounded transition dimensions");
        }
    }

    private static void checkFlags(byte[] flags, int states) {
        Objects.requireNonNull(flags, "flags");
        if (flags.length != states) throw new IllegalArgumentException("invalid state flags");
        for (byte flag : flags) {
            if (flag != 0 && flag != 1) throw new IllegalArgumentException("invalid state flag");
        }
    }

    private static void checkState(int state, int states) {
        if (state < -1 || state >= states) {
            throw new IllegalArgumentException("invalid transition state");
        }
    }

    // Qualified native candidates for future platform-specific selection. Measured
    // admitted sizes favor the Java kernels, so production dispatch stays in Java
    // and works with older libjava binaries without either new symbol. The existing
    // RX7 token-row native dispatch is independent and unchanged.
    static native void compose0(short[] left, short[] right, short[] result);
    static native void repeat0(short[] rows, long count, short[] result);
}
