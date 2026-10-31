/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.SplittableRandom;

/**
 * Reference models of the Boolean relation and transfer-function atoms, written from their
 * contracts with java.util.BitSet / plain arrays (RELATION ORACLE v1). They are deliberately
 * naive: element-by-element products, fixed-point iteration and cycle-detecting chain walks.
 */
final class RelationOracle {
    /** Bits per relation row word. */
    static final int ROW_BITS = Long.SIZE;
    /** Marker of an undefined transfer target. */
    static final short DEAD = -1;

    private RelationOracle() {
    }

    /** Boolean product: result[q] = OR of right[s] over every s in left[q]. */
    static long[] compose(long[] left, long[] right) {
        long[] result = new long[left.length];
        for (int q = 0; q < left.length; q++) {
            BitSet active = BitSet.valueOf(new long[] {left[q]});
            long row = 0L;
            for (int s = active.nextSetBit(0); s >= 0; s = active.nextSetBit(s + 1)) {
                row |= right[s];
            }
            result[q] = row;
        }
        return result;
    }

    /** k-fold product of rows with itself; k = 0 is the identity relation. */
    static long[] power(long[] rows, long count) {
        long[] result = identity(rows.length);
        for (long i = 0; i < count; i++) {
            result = compose(result, rows);
        }
        return result;
    }

    /** Identity relation of {@code states} states. */
    static long[] identity(int states) {
        long[] rows = new long[states];
        for (int q = 0; q < states; q++) {
            rows[q] = 1L << q;
        }
        return rows;
    }

    /** Random relation over {@code states} states with the given bit density in [0, 1]. */
    static long[] randomRelation(SplittableRandom random, int states, double density) {
        long[] rows = new long[states];
        for (int q = 0; q < states; q++) {
            for (int s = 0; s < states; s++) {
                if (random.nextDouble() < density) {
                    rows[q] |= 1L << s;
                }
            }
        }
        return rows;
    }

    /** Transfer function composition left-to-right: result[q] = right[left[q]], dead stays dead. */
    static short[] composeTransfer(short[] left, short[] right) {
        short[] result = new short[left.length];
        for (int q = 0; q < left.length; q++) {
            result[q] = left[q] < 0 ? DEAD : right[left[q]];
        }
        return result;
    }

    /**
     * count-fold transfer power by walking each state's chain with cycle detection (independent
     * of square-and-multiply): the walk stops at a dead state or folds the cycle arithmetically.
     */
    static short[] powerTransfer(short[] rows, long count) {
        short[] result = new short[rows.length];
        for (int q = 0; q < rows.length; q++) {
            result[q] = walk(rows, q, count);
        }
        return result;
    }

    private static short walk(short[] rows, int start, long count) {
        Map<Integer, Long> seenAt = new HashMap<>();
        int state = start;
        long step = 0;
        while (step < count) {
            if (state < 0) {
                return DEAD;
            }
            Long first = seenAt.putIfAbsent(state, step);
            if (first != null) {
                long cycle = step - first;
                long remaining = (count - step) % cycle;
                for (long i = 0; i < remaining; i++) {
                    state = state < 0 ? DEAD : rows[state];
                }
                return (short) state;
            }
            state = rows[state];
            step++;
        }
        return (short) state;
    }

    /** Random transfer function (targets in [-1, states)). */
    static short[] randomTransfer(SplittableRandom random, int states, double deadRate) {
        short[] rows = new short[states];
        for (int q = 0; q < states; q++) {
            rows[q] = random.nextDouble() < deadRate ? DEAD : (short) random.nextInt(states);
        }
        return rows;
    }

    /** Random transition table of states x columns, targets in [-1, states). */
    static short[] randomTable(SplittableRandom random, int states, int columns, double deadRate) {
        short[] rows = new short[states * columns];
        for (int cell = 0; cell < rows.length; cell++) {
            rows[cell] = random.nextDouble() < deadRate ? DEAD : (short) random.nextInt(states);
        }
        return rows;
    }

    /** Reverse reachability by fixed-point iteration: a state is useful if a column leads to a useful state. */
    static byte[] useful(short[] rows, int states, int columns, byte[] accepting) {
        byte[] useful = accepting.clone();
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int cell = 0; cell < rows.length; cell++) {
                int target = rows[cell];
                int source = cell / columns;
                if (target >= 0 && useful[target] != 0 && useful[source] == 0) {
                    useful[source] = 1;
                    changed = true;
                }
            }
        }
        return useful;
    }

    /** State-major token masks: bit t of state q is set when rows[q][t] targets a useful state. */
    static long[] masks(short[] rows, int states, int tokens, byte[] useful) {
        int words = (tokens + ROW_BITS - 1) / ROW_BITS;
        long[] result = new long[states * words];
        for (int q = 0; q < states; q++) {
            BitSet bits = new BitSet(tokens);
            for (int t = 0; t < tokens; t++) {
                int target = rows[q * tokens + t];
                if (target >= 0 && useful[target] != 0) {
                    bits.set(t);
                }
            }
            long[] packed = bits.toLongArray();
            System.arraycopy(packed, 0, result, q * words, packed.length);
        }
        return result;
    }

    /** Runs the id sequence from every state; dead stops the run. */
    static short[] tokens(short[] rows, int states, int tokens, int[] ids, int offset, int length) {
        short[] result = new short[states];
        for (int q = 0; q < states; q++) {
            int state = q;
            for (int i = offset; i < offset + length && state >= 0; i++) {
                state = rows[state * tokens + ids[i]];
            }
            result[q] = (short) state;
        }
        return result;
    }

    /** BFS over the reverse graph, used as a second reference for {@link #useful}. */
    static BitSet reverseReachable(short[] rows, int columns, BitSet seeds) {
        BitSet seen = (BitSet) seeds.clone();
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        seeds.stream().forEach(queue::add);
        while (!queue.isEmpty()) {
            int target = queue.removeFirst();
            for (int cell = 0; cell < rows.length; cell++) {
                int source = cell / columns;
                if (rows[cell] == target && !seen.get(source)) {
                    seen.set(source);
                    queue.add(source);
                }
            }
        }
        return seen;
    }

    static String describe(long[] rows) {
        return Arrays.toString(rows);
    }
}
