// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** Actual-owner callback tape and independent stable-order oracle; no JUnit required. */
public final class M3IndexSortTailContract {
    private static int checks;

    private M3IndexSortTailContract() { }

    private static final class Tape {
        private final List<String> events = new ArrayList<>();
        private final RuntimeException marker = new IllegalStateException("callback marker");
        private final int failAt;

        private Tape(int failAt) { this.failAt = failAt; }

        private void event(String event) {
            events.add(event);
            if (events.size() == failAt) throw marker;
        }
    }

    private static final class Key implements Comparable<Key> {
        private final int id;
        private final int value;
        private final Tape tape;

        private Key(int id, int value, Tape tape) {
            this.id = id;
            this.value = value;
            this.tape = tape;
        }

        @Override public int compareTo(Key other) {
            tape.event("compare:" + id + ":" + other.id);
            return Integer.compare(value, other.value);
        }
    }

    private static void sort(int[] positions, Tape tape, boolean natural) {
        int[] values = {4, 1, 4, 2, 3};
        Key[] keys = new Key[values.length];
        for (int i = 0; i < keys.length; i++) keys[i] = new Key(i, values[i], tape);
        Comparator<Key> comparator = natural ? null : (left, right) -> {
            tape.event("compare:" + left.id + ":" + right.id);
            return Integer.compare(left.value, right.value);
        };
        M3IndexSort.stable(positions, index -> {
            tape.event("read:" + index);
            return keys[index];
        }, comparator);
    }

    private static void exactCallbackTape(boolean natural) {
        // Hand-derived witness, including both unopposed tails and a stable tie.
        List<String> expected = List.of(
                "read:0", "read:1", "compare:0:1",
                "read:2", "read:3", "compare:2:3",
                "read:1", "read:3", "compare:1:3",
                "read:0", "read:3", "compare:0:3",
                "read:0", "read:2", "compare:0:2",
                "read:1", "read:4", "compare:1:4",
                "read:3", "read:4", "compare:3:4",
                "read:0", "read:4", "compare:0:4");
        Tape complete = new Tape(-1);
        int[] positions = {0, 1, 2, 3, 4};
        sort(positions, complete, natural);
        equal(expected, complete.events, "callback order");
        array(new int[]{1, 3, 4, 0, 2}, positions, "stable witness order");
        for (int event = 1; event <= expected.size(); event++) {
            Tape failing = new Tape(event);
            int[] interrupted = {0, 1, 2, 3, 4};
            try {
                sort(interrupted, failing, natural);
                throw new AssertionError("missing callback failure " + event);
            } catch (RuntimeException thrown) {
                if (thrown != failing.marker) {
                    throw new AssertionError("callback exception identity changed", thrown);
                }
                checks++;
            }
            equal(expected.subList(0, event), failing.events,
                    "no callback after exceptional prefix");
        }
    }

    private static void independentStableOrder() {
        Random random = new Random(0x4d335441494c2026L);
        int[] boundaries = {0, 1, 2, 3, 7, 8, 9, 31, 32, 33, 63, 64, 65, 127, 128, 129};
        for (int length : boundaries) {
            for (int trial = 0; trial < 12; trial++) {
                int[] payload = new int[Math.max(1, length)];
                for (int i = 0; i < payload.length; i++) {
                    payload[i] = switch (trial % 4) {
                        case 0 -> i;
                        case 1 -> -i;
                        case 2 -> 7;
                        default -> random.nextInt(9) - 4;
                    };
                }
                if (payload.length > 2 && trial == 3) {
                    payload[0] = Integer.MIN_VALUE;
                    payload[payload.length - 1] = Integer.MAX_VALUE;
                }
                int[] positions = new int[length];
                for (int i = 0; i < length; i++) {
                    positions[i] = trial < 8 ? i : random.nextInt(payload.length);
                }
                Integer[] oracle = Arrays.stream(positions).boxed().toArray(Integer[]::new);
                Arrays.sort(oracle, Comparator.comparingInt(index -> payload[index]));
                int[] expected = Arrays.stream(oracle).mapToInt(Integer::intValue).toArray();
                int[] actual = positions.clone();
                M3IndexSort.stable(actual, index -> payload[index], Integer::compare);
                array(expected, actual, "JDK stable index oracle");
                actual = positions.clone();
                M3IndexSort.stable(actual, index -> payload[index], null);
                array(expected, actual, "natural-order oracle");
            }
        }
    }

    private static void equal(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected=" + expected + " actual=" + actual);
        }
        checks++;
    }

    private static void array(int[] expected, int[] actual, String label) {
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(label + " expected=" + Arrays.toString(expected)
                    + " actual=" + Arrays.toString(actual));
        }
        checks++;
    }

    public static void main(String[] args) {
        exactCallbackTape(false);
        exactCallbackTape(true);
        independentStableOrder();
        System.out.println("M3_INDEX_SORT_TAIL_CONTRACT_PASS checks=" + checks);
    }
}
