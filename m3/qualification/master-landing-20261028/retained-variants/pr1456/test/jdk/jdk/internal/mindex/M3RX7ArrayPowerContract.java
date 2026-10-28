/* SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026, Hitesh Soliwal and contributors.
 */
package jdk.internal.mindex;

import java.util.Arrays;
import java.util.Random;

/** Independent functional-graph and lexical-closure host/jtreg component probes. */
public final class M3RX7ArrayPowerContract {
    private static final String ROOT = "0123456789abcdef".repeat(4);
    private static long assertions;
    private static long fingerprint = 1;
    private static boolean nativeEnabled;
    private M3RX7ArrayPowerContract() { }

    public static void main(String[] args) {
        nativeEnabled = args.length != 0;
        if (nativeEnabled) System.load(args[0]);
        powers();
        refusals();
        lexicalClosures();
        compositeTransfers();
        System.out.println("RX7_ARRAY_POWER_PASS assertions=" + assertions
                + " native=" + nativeEnabled + " trace=" + Long.toUnsignedString(fingerprint));
    }

    private static void equal(long expected, long actual) {
        assertions++;
        if (expected != actual) throw new AssertionError("expected=" + expected + " actual=" + actual);
        // Transcript only, never a source identity, equality oracle or integrity proof.
        fingerprint = fingerprint * 31 + actual;
    }

    private static void yes(boolean condition) { equal(1, condition ? 1 : 0); }

    private static void refused(Class<? extends Throwable> type, Runnable operation) {
        try {
            operation.run();
        } catch (Throwable failure) {
            if (failure.getClass() != type) throw new AssertionError("wrong refusal", failure);
            assertions++;
            fingerprint = fingerprint * 31 + type.getName().hashCode();
            return;
        }
        throw new AssertionError("missing " + type.getName());
    }

    // Independent cycle/path oracle: no repeated squaring or production compose helper.
    private static int walk(short[] rows, int start, long count) {
        int[] first = new int[rows.length];
        int[] path = new int[rows.length];
        Arrays.fill(first, -1);
        int state = start;
        int used = 0;
        while (state >= 0 && first[state] < 0) {
            if (count == used) return state;
            first[state] = used;
            path[used++] = state;
            state = rows[state];
        }
        if (state < 0) return -1;
        int begin = first[state];
        return path[begin + (int) ((count - begin) % (used - begin))];
    }

    private static void checkPower(short[] rows, long count) {
        short[] original = rows.clone();
        short[] actual = M3RX7Transitions.repeat(rows, count);
        yes(actual != rows);
        yes(Arrays.equals(original, rows));
        short[] nativeRows = new short[rows.length];
        if (nativeEnabled) {
            Arrays.fill(nativeRows, Short.MIN_VALUE);
            M3RX7Transitions.repeat0(rows, count, nativeRows);
            yes(Arrays.equals(original, rows));
        }
        for (int state = 0; state < rows.length; state++) {
            equal(walk(rows, state, count), actual[state]);
            if (nativeEnabled) equal(actual[state], nativeRows[state]);
        }
    }

    private static void powers() {
        long[] exponents = {0, 1, 2, 3, 4, 7, 8, 31, 32, 63, 64,
                1L << 31, (1L << 62) - 1, 1L << 62, Long.MAX_VALUE};
        Random random = new Random(0x5258374152524159L);
        for (int size : new int[]{1, 2, 3, 7, 31, 63, 64, 65, 127, 256}) {
            for (int trial = 0; trial < 24; trial++) {
                short[] rows = new short[size];
                for (int state = 0; state < size; state++) rows[state] = (short) (random.nextInt(size + 1) - 1);
                for (long count : exponents) checkPower(rows, count);
                checkPower(rows, random.nextLong() & Long.MAX_VALUE);
            }
        }
        // Maximum admitted table: long cycle, absorbing sink and identity.
        for (int shape = 0; shape < 3; shape++) {
            short[] rows = new short[4096];
            for (int state = 0; state < rows.length; state++) {
                rows[state] = (short) (shape == 0 ? (state + 1) % rows.length : shape == 1 ? -1 : state);
            }
            for (long count : new long[]{0, 1, 2, 1L << 62, Long.MAX_VALUE}) checkPower(rows, count);
        }
    }

    private static void refusals() {
        refused(NullPointerException.class, () -> M3RX7Transitions.repeat(null, -1));
        refused(IllegalArgumentException.class, () -> M3RX7Transitions.repeat(new short[0], 0));
        refused(IllegalArgumentException.class, () -> M3RX7Transitions.repeat(new short[4097], 1));
        refused(IllegalArgumentException.class, () -> M3RX7Transitions.repeat(new short[]{-2}, 0));
        refused(IllegalArgumentException.class, () -> M3RX7Transitions.repeat(new short[]{1}, 0));
        refused(IllegalArgumentException.class, () -> M3RX7Transitions.repeat(new short[]{0}, -1));
        refused(IndexOutOfBoundsException.class, () -> M3RX7Transitions.tokens(
                new short[]{-1}, 1, 1, new int[]{0, 1}, 0, 2));
        if (!nativeEnabled) return;
        short[] output = {123};
        refused(IllegalArgumentException.class, () -> M3RX7Transitions.repeat0(new short[]{-2}, 0, output));
        equal(123, output[0]);
        refused(IllegalArgumentException.class, () -> M3RX7Transitions.repeat0(new short[]{1}, 0, output));
        equal(123, output[0]);
        refused(IllegalArgumentException.class, () -> M3RX7Transitions.repeat0(null, 1, output));
        equal(123, output[0]);
        refused(IllegalArgumentException.class, () -> M3RX7Transitions.repeat0(new short[]{0}, -1, output));
        equal(123, output[0]);
        short[] same = {0};
        refused(IllegalArgumentException.class, () -> M3RX7Transitions.repeat0(same, 0, same));
        equal(0, same[0]);
        refused(IllegalArgumentException.class, () -> M3RX7Transitions.repeat0(same, 2, new short[2]));
        refused(IllegalArgumentException.class, () -> M3RX7Transitions.compose0(same, same, same));
        equal(0, same[0]);
        short[] cycle = {1, 0};
        short[] squared = new short[2];
        M3RX7Transitions.compose0(cycle, cycle, squared);
        equal(0, squared[0]); equal(1, squared[1]);
    }

    private static void lexicalClosures() {
        Random random = new Random(0x455053494c4f4eL);
        for (int states : new int[]{1, 2, 3, 7, 63, 64, 65, 127, 128, 129, 255, 256}) {
            for (int trial = 0; trial < 12; trial++) {
                int[] offsets = new int[states + 1];
                int[] targets = new int[states * 2];
                int[] kinds = new int[targets.length];
                long[] spellings = new long[targets.length];
                int[] flags = new int[targets.length];
                byte[] finals = new byte[states];
                for (int state = 0; state < states; state++) {
                    offsets[state] = state * 2;
                    finals[state] = (byte) (random.nextInt(5) == 0 ? 1 : 0);
                    for (int j = 0; j < 2; j++) {
                        int edge = state * 2 + j;
                        targets[edge] = random.nextInt(states);
                        kinds[edge] = j == 0 ? -2 : 1;
                        spellings[edge] = -1;
                    }
                }
                offsets[states] = targets.length;
                M3RX7.LexicalPlan plan = M3RX7.fromLexicalNfa(ROOT, 0,
                        offsets, targets, kinds, spellings, flags, flags, finals);
                for (int sample = 0; sample < 32; sample++) {
                    long[] frontier = new long[(states + 63) >>> 6];
                    boolean[] reachable = new boolean[states];
                    for (int state = 0; state < states; state++) {
                        reachable[state] = random.nextBoolean();
                        if (reachable[state]) frontier[state >>> 6] |= 1L << (state & 63);
                    }
                    boolean changed;
                    do {
                        changed = false;
                        for (int state = 0; state < states; state++) {
                            for (int edge = offsets[state]; edge < offsets[state + 1]; edge++) {
                                if (reachable[state] && kinds[edge] == -2 && !reachable[targets[edge]]) {
                                    reachable[targets[edge]] = true;
                                    changed = true;
                                }
                            }
                        }
                    } while (changed);
                    boolean expected = false;
                    for (int state = 0; state < states; state++) expected |= reachable[state] && finals[state] != 0;
                    long[] snapshot = frontier.clone();
                    equal(expected ? 1 : 0, plan.accepts(frontier) ? 1 : 0);
                    yes(Arrays.equals(snapshot, frontier));
                }
                int[] tokenKinds = new int[12]; Arrays.fill(tokenKinds, 1);
                long[] ids = new long[12]; int[] tokenFlags = new int[12];
                boolean whole = plan.matches(tokenKinds, ids, tokenFlags);
                for (int split = 0; split <= 12; split++) {
                    long[] frontier = plan.initialStates();
                    plan.advance(frontier, tokenKinds, ids, tokenFlags, 0, split);
                    plan.advance(frontier, tokenKinds, ids, tokenFlags, split, split);
                    plan.advance(frontier, tokenKinds, ids, tokenFlags, split, 12);
                    equal(whole ? 1 : 0, plan.accepts(frontier) ? 1 : 0);
                }
                refused(NullPointerException.class, () -> plan.accepts(null));
                refused(IllegalArgumentException.class, () -> plan.accepts(new long[0]));
                if ((states & 63) != 0) {
                    long[] bad = new long[(states + 63) >>> 6];
                    bad[bad.length - 1] = 1L << (states & 63);
                    refused(IllegalArgumentException.class, () -> plan.accepts(bad));
                }
            }
        }
        // Duplicate epsilon paths must be OR/union, never XOR cancellation.
        M3RX7.LexicalPlan duplicate = M3RX7.fromLexicalNfa(ROOT, 0,
                new int[]{0, 2, 2}, new int[]{1, 1}, new int[]{-2, -2},
                new long[]{-1, -1}, new int[2], new int[2], new byte[]{0, 1});
        yes(duplicate.accepts(new long[]{1}));
    }

    private static void compositeTransfers() {
        short[] transitions = new short[3 * 128];
        Arrays.fill(transitions, (short) -1);
        transitions['a'] = 1;
        transitions[128 + 'b'] = 0;
        M3RX7 plan = M3RX7.fromAsciiDfa(ROOT, 0, transitions, new byte[]{1, 0, 0});
        M3RX7.TokenIndex tokens = plan.prepareTokenIndex(new byte[]{'a', 'b'}, new int[]{0, 1}, new int[]{1, 1});
        yes(tokens != null);
        for (int pairs = 0; pairs < 48; pairs++) {
            String text = "ab".repeat(pairs);
            int[] ids = new int[text.length()];
            for (int i = 0; i < ids.length; i++) ids[i] = i & 1;
            for (int split = 0; split <= ids.length; split++) {
                M3RX7.Transfer transfer = tokens.transfer(ids, 0, split).then(tokens.transfer(ids, split, ids.length - split));
                yes(transfer.matches());
                equal(ids.length, transfer.length());
                yes(transfer.repeated(17).matches());
                equal(ids.length * 17L, transfer.repeated(17).length());
                equal(1, plan.matchesChunksCandidate(new CharSequence[]{text, text},
                        new int[]{0, split}, new int[]{split, text.length() - split}));
            }
        }
        M3RX7.Transfer unit = tokens.transfer(new int[]{0, 1});
        refused(ArithmeticException.class, () -> unit.repeated(Long.MAX_VALUE));
        yes(unit.repeated(Long.MAX_VALUE / 2).matches());
        M3RX7 foreign = M3RX7.fromAsciiDfa(ROOT, 0, transitions, new byte[]{1, 0, 0});
        M3RX7.Transfer other = foreign.prepareTokenIndex(new byte[]{'a'}, new int[]{0}, new int[]{1}).transfer(new int[]{0});
        refused(IllegalArgumentException.class, () -> unit.then(other));
    }
}
