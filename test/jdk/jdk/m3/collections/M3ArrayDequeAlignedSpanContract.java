/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.Random;

/** Independent Java Deque oracle for the original dual-lane M3 dynamic ring. */
public final class M3ArrayDequeAlignedSpanContract {
    private static long checks;
    private static int serial;
    private M3ArrayDequeAlignedSpanContract() { }

    private static final class Token {
        final int id;
        Token(int id) { this.id = id; }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void verify(M3ArrayDeque<Token> actual, ArrayDeque<Token> expected,
            IdentityHashMap<Token, Integer> flags) {
        check(actual.size() == expected.size(), "size");
        Object[] observed = actual.toArray();
        Object[] oracle = expected.toArray();
        check(observed.length == oracle.length, "copy length");
        int[] selected = actual.selectedPhysicalIndices(
                M3ArrayDeque.LIVE, 0, IndexMaskCompactor.Backend.JAVA);
        check(selected.length == oracle.length, "mask selected count");
        for (int i = 0; i < oracle.length; i++) {
            Token token = (Token) observed[i];
            check(token == oracle[i], "reference identity and order");
            check(actual.stateAt(i) == flags.get(token), "stored state follows identity");
            check(selected[i] == actual.physicalIndex(i), "physical mask coordinate");
        }
        check(actual.capacity() >= actual.size(), "capacity");
    }

    private static void scenario(int initialCapacity, int seed) {
        var actual = new M3ArrayDeque<Token>(initialCapacity);
        var expected = new ArrayDeque<Token>();
        var flags = new IdentityHashMap<Token, Integer>();
        Random random = new Random(0x5a86e421L ^ seed ^ initialCapacity);
        for (int step = 0; step < 560; step++) {
            int op = random.nextInt(12);
            switch (op) {
                case 0, 1, 2, 3 -> {
                    Token item = new Token(serial++);
                    if (op < 2) {
                        actual.addFirst(item);
                        expected.addFirst(item);
                    } else {
                        actual.addLast(item);
                        expected.addLast(item);
                    }
                    flags.put(item, M3ArrayDeque.LIVE);
                }
                case 4 -> check(actual.pollFirst() == expected.pollFirst(), "pollFirst");
                case 5 -> check(actual.pollLast() == expected.pollLast(), "pollLast");
                case 6, 7 -> {
                    Object[] available = expected.toArray();
                    Token wanted = available.length == 0 || random.nextInt(5) == 0
                            ? new Token(-1) : (Token) available[random.nextInt(available.length)];
                    boolean actualRemoved = op == 6 ? actual.removeFirstOccurrence(wanted)
                            : actual.removeLastOccurrence(wanted);
                    boolean expectedRemoved = op == 6 ? expected.removeFirstOccurrence(wanted)
                            : expected.removeLastOccurrence(wanted);
                    check(actualRemoved == expectedRemoved, "occurrence remove");
                }
                case 8 -> {
                    if (!expected.isEmpty()) {
                        int i = random.nextInt(expected.size());
                        Token stored = (Token) expected.toArray()[i];
                        int prior = flags.get(stored);
                        int state = 2 << random.nextInt(7);
                        check(actual.updateState(i, state, 0) == prior, "prior state");
                        flags.put(stored, prior | state);
                    }
                }
                case 9 -> {
                    int divisor = 2 + random.nextInt(4);
                    check(actual.removeIf(x -> x.id % divisor == 0)
                            == expected.removeIf(x -> x.id % divisor == 0), "removeIf");
                }
                case 10 -> {
                    actual.clear();
                    expected.clear();
                    check(actual.size() == 0, "clear");
                }
                default -> verify(actual.clone(), expected, flags);
            }
            verify(actual, expected, flags);
        }
    }

    public static long run() {
        checks = 0;
        serial = 0;
        for (int capacity = 0; capacity <= 32; capacity++) {
            for (int seed = 0; seed < 4; seed++) scenario(capacity, seed);
        }
        return checks;
    }

    public static void main(String[] ignored) {
        System.out.println("PASS M3ArrayDeque aligned ring: checks=" + run());
    }
}
