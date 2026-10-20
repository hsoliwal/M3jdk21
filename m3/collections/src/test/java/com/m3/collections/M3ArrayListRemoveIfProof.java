/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Frozen behavioral fixture for the canonical Synexia recipe.
 */
package com.m3.collections;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/** Run unchanged against the exact baseline and recipe candidate in separate JVMs. */
public final class M3ArrayListRemoveIfProof {
    private static final Field ELEMENTS;
    private static long assertions;
    private static long cases;

    static {
        try {
            ELEMENTS = M3ArrayList.class.getDeclaredField("elements");
            ELEMENTS.setAccessible(true);
        } catch (ReflectiveOperationException problem) {
            throw new ExceptionInInitializerError(problem);
        }
    }

    private M3ArrayListRemoveIfProof() { }

    private static final class Value {
        private final int ordinal;
        private Value(int ordinal) { this.ordinal = ordinal; }
        @Override public boolean equals(Object other) {
            throw new AssertionError("removeIf must not compare payload equality");
        }
        @Override public int hashCode() {
            throw new AssertionError("removeIf must not hash payloads");
        }
        @Override public String toString() { return "v" + ordinal; }
    }

    private static final class CallbackFailure extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    private static Object[] values(int length) {
        Object[] input = new Object[length];
        for (int i = 0; i < length; i++) {
            input[i] = i % 7 == 0 ? null : i % 5 == 0 ? input[i - 1] : new Value(i);
        }
        return input;
    }

    private static Object[] storage(M3ArrayList<?> list) {
        try { return (Object[]) ELEMENTS.get(list); }
        catch (IllegalAccessException problem) { throw new AssertionError(problem); }
    }

    private static void check(boolean condition, String label) {
        assertions++;
        if (!condition) throw new AssertionError(label);
    }

    private static void same(Object[] expected, List<?> actual, String label) {
        check(expected.length == actual.size(), label + " size");
        for (int i = 0; i < expected.length; i++) {
            check(expected[i] == actual.get(i), label + " identity at " + i);
        }
    }

    private static void tailClear(M3ArrayList<?> list) {
        Object[] array = storage(list);
        for (int i = list.size(); i < array.length; i++) {
            check(array[i] == null, "no retained reference in unused slot " + i);
        }
    }

    private static void throwsExactly(Class<? extends Throwable> expected, Runnable action) {
        try { action.run(); }
        catch (Throwable actual) {
            check(actual.getClass() == expected,
                    "expected " + expected.getName() + " got " + actual);
            return;
        }
        throw new AssertionError("missing " + expected.getName());
    }

    private static void ordinary(Object[] input, BitSet reject, int from, int to,
                                 boolean throughSubList) {
        cases++;
        M3ArrayList<Object> actual = new M3ArrayList<>(Arrays.asList(input));
        actual.ensureCapacity(input.length + 17);
        ArrayList<Object> expected = new ArrayList<>(Arrays.asList(input));
        Object[] retainedOwner = storage(actual);
        int capacity = actual.capacity();
        List<Object> actualView = throughSubList
                ? actual.subList(0, input.length).subList(from, to) : actual;
        List<Object> expectedView = throughSubList
                ? expected.subList(0, input.length).subList(from, to) : expected;
        Iterator<Object> oldIterator = actual.iterator();
        List<Object> oldView = actual.subList(0, actual.size());
        Object[] actualCalls = new Object[to - from];
        Object[] expectedCalls = new Object[to - from];
        int[] ai = {0};
        int[] ei = {0};
        boolean changed = actualView.removeIf(value -> {
            if (!throughSubList) check(actual.size() == input.length,
                    "root predicate planning precedes removal");
            actualCalls[ai[0]] = value;
            return reject.get(ai[0]++);
        });
        boolean oracleChanged = expectedView.removeIf(value -> {
            expectedCalls[ei[0]] = value;
            return reject.get(ei[0]++);
        });
        check(changed == oracleChanged, "removeIf boolean result");
        check(ai[0] == to - from && ai[0] == ei[0], "one callback per occurrence");
        for (int i = 0; i < ai[0]; i++) {
            check(actualCalls[i] == expectedCalls[i], "callback order/identity");
        }
        same(expected.toArray(), actual, "JDK normal removal");
        same(expectedView.toArray(), actualView, "backed view normal removal");
        check(storage(actual) == retainedOwner, "one original payload array owner");
        check(actual.capacity() == capacity, "removeIf retains capacity");
        tailClear(actual);
        if (changed) {
            throwsExactly(ConcurrentModificationException.class, oldView::size);
            throwsExactly(ConcurrentModificationException.class, oldIterator::next);
        } else {
            check(oldView.size() == input.length, "no-op view remains valid");
            if (input.length > 0) check(oldIterator.next() == input[0], "no-op iterator valid");
        }
    }

    private static void exhaustiveMasks() {
        for (int length = 0; length <= 12; length++) {
            Object[] input = values(length);
            for (int bits = 0; bits < 1 << length; bits++) {
                ordinary(input, BitSet.valueOf(new long[]{bits}), 0, length, false);
            }
        }
        for (int length = 0; length <= 8; length++) {
            Object[] input = values(length);
            for (int from = 0; from <= length; from++) {
                for (int to = from; to <= length; to++) {
                    for (int bits = 0; bits < 1 << (to - from); bits++) {
                        ordinary(input, BitSet.valueOf(new long[]{bits}), from, to, true);
                    }
                }
            }
        }
    }

    private static void largeAndSeededMasks() {
        int[] lengths = {31, 32, 63, 64, 65, 127, 128, 129, 255, 256, 257, 1024, 4096};
        for (int length : lengths) {
            for (int pattern = 0; pattern < 8; pattern++) {
                BitSet reject = new BitSet();
                for (int i = 0; i < length; i++) {
                    boolean rejected = switch (pattern) {
                        case 0 -> false;
                        case 1 -> true;
                        case 2 -> i == 0;
                        case 3 -> i == length - 1;
                        case 4 -> (i & 1) == 0;
                        case 5 -> i >= length / 4 && i < 3 * length / 4;
                        case 6 -> (i & 63) == 63;
                        default -> (i & 127) >= 64;
                    };
                    if (rejected) reject.set(i);
                }
                ordinary(values(length), reject, 0, length, false);
            }
        }
        Random random = new Random(0x4e425f4d335f3231L);
        for (int trial = 0; trial < 1500; trial++) {
            int length = random.nextInt(400);
            boolean view = trial % 4 == 0;
            int from = view ? random.nextInt(length + 1) : 0;
            int to = view ? from + random.nextInt(length - from + 1) : length;
            BitSet reject = new BitSet();
            for (int i = 0; i < to - from; i++) if (random.nextBoolean()) reject.set(i);
            ordinary(values(length), reject, from, to, view);
        }
    }

    private static void callbackFailures() {
        for (int length = 1; length <= 16; length++) {
            for (int stop = 0; stop < length; stop++) {
                cases++;
                Object[] input = values(length);
                M3ArrayList<Object> actual = new M3ArrayList<>(Arrays.asList(input));
                ArrayList<Object> expected = new ArrayList<>(Arrays.asList(input));
                Iterator<Object> iterator = actual.iterator();
                Object[] owner = storage(actual);
                int failAt = stop;
                int[] ai = {0};
                int[] ei = {0};
                throwsExactly(CallbackFailure.class, () -> actual.removeIf(value -> {
                    if (ai[0]++ == failAt) throw new CallbackFailure();
                    return true;
                }));
                throwsExactly(CallbackFailure.class, () -> expected.removeIf(value -> {
                    if (ei[0]++ == failAt) throw new CallbackFailure();
                    return true;
                }));
                check(ai[0] == stop + 1 && ai[0] == ei[0], "failure stops callbacks");
                same(expected.toArray(), actual, "predicate failure JDK root parity");
                check(storage(actual) == owner, "failure retains owner");
                check(iterator.next() == input[0], "failure retains epoch");
                tailClear(actual);
            }
        }
        for (int length : new int[]{0, 7}) {
            cases++;
            M3ArrayList<Object> actual = new M3ArrayList<>(Arrays.asList(values(length)));
            Object[] before = actual.toArray();
            throwsExactly(NullPointerException.class, () -> actual.removeIf(null));
            same(before, actual, "null predicate");
            tailClear(actual);
        }
    }

    private static void nonStructuralCallbacks() {
        for (int trigger = 0; trigger < 7; trigger++) {
            for (int target = 0; target < 7; target++) {
                cases++;
                Object[] input = values(7);
                Object replacement = new Value(99);
                M3ArrayList<Object> actual = new M3ArrayList<>(Arrays.asList(input));
                ArrayList<Object> expected = new ArrayList<>(Arrays.asList(input));
                int at = trigger;
                int slot = target;
                int[] ai = {0};
                int[] ei = {0};
                Object[] observed = new Object[7];
                Object[] oracleObserved = new Object[7];
                actual.removeIf(value -> {
                    observed[ai[0]] = value;
                    if (ai[0] == at) actual.set(slot, replacement);
                    return (ai[0]++ & 1) == 0;
                });
                expected.removeIf(value -> {
                    oracleObserved[ei[0]] = value;
                    if (ei[0] == at) expected.set(slot, replacement);
                    return (ei[0]++ & 1) == 0;
                });
                same(expected.toArray(), actual, "nonstructural callback updates survive");
                for (int i = 0; i < 7; i++) check(observed[i] == oracleObserved[i], "set trace");
                tailClear(actual);
            }
        }
    }

    private static void reentrantCallbacks() {
        for (int action = 0; action < 6; action++) {
            for (int trigger : new int[]{0, 2, 5}) {
                cases++;
                M3ArrayList<Object> actual = new M3ArrayList<>(Arrays.asList(values(6)));
                actual.ensureCapacity(20);
                int operation = action;
                int[] calls = {0};
                Object[][] afterCallbackMutation = {null};
                throwsExactly(ConcurrentModificationException.class, () -> actual.removeIf(value -> {
                    if (calls[0]++ == trigger) {
                        switch (operation) {
                            case 0 -> actual.add(new Value(20));
                            case 1 -> actual.remove(actual.size() - 1);
                            case 2 -> actual.clear();
                            case 3 -> actual.ensureCapacity(actual.capacity() + 1);
                            case 4 -> actual.trimToSize();
                            default -> actual.removeIf(item -> item == null);
                        }
                        afterCallbackMutation[0] = actual.toArray();
                    }
                    return true;
                }));
                check(calls[0] == trigger + 1, "M3 immediate reentrant refusal");
                same(afterCallbackMutation[0], actual, "callback mutation survives, no outer commit");
                tailClear(actual);
            }
        }
        cases++;
        Object[] input = values(8);
        M3ArrayList<Object> actual = new M3ArrayList<>(Arrays.asList(input));
        int[] calls = {0};
        actual.removeIf(value -> {
            check(!actual.removeIf(inner -> false), "nested no-op is permitted");
            return (calls[0]++ & 1) == 0;
        });
        same(new Object[]{input[1], input[3], input[5], input[7]}, actual, "nested no-op");
        tailClear(actual);

        cases++;
        M3ArrayList<Object> mutatesThenThrows = new M3ArrayList<>(Arrays.asList(values(5)));
        Object[][] changed = {null};
        throwsExactly(CallbackFailure.class, () -> mutatesThenThrows.removeIf(value -> {
            mutatesThenThrows.add(new Value(44));
            changed[0] = mutatesThenThrows.toArray();
            throw new CallbackFailure();
        }));
        same(changed[0], mutatesThenThrows, "callback exception precedes epoch refusal");
        tailClear(mutatesThenThrows);
    }

    private static void inheritedViewContract() {
        cases++;
        Object[] input = values(6);
        M3ArrayList<Object> actual = new M3ArrayList<>(Arrays.asList(input));
        // LinkedList's inherited view iterator is the JDK oracle for this established
        // AbstractList-view behavior; concrete ArrayList.SubList has a distinct override.
        LinkedList<Object> inheritedOracle = new LinkedList<>(Arrays.asList(input));
        ArrayList<Object> concreteArrayList = new ArrayList<>(Arrays.asList(input));
        int[] ai = {0};
        int[] ii = {0};
        int[] ci = {0};
        throwsExactly(CallbackFailure.class, () -> actual.subList(1, 5).removeIf(value -> {
            if (ai[0]++ == 2) throw new CallbackFailure();
            return true;
        }));
        throwsExactly(CallbackFailure.class, () -> inheritedOracle.subList(1, 5).removeIf(value -> {
            if (ii[0]++ == 2) throw new CallbackFailure();
            return true;
        }));
        throwsExactly(CallbackFailure.class, () -> concreteArrayList.subList(1, 5).removeIf(value -> {
            if (ci[0]++ == 2) throw new CallbackFailure();
            return true;
        }));
        same(inheritedOracle.toArray(), actual, "inherited subList failure prefix");
        check(actual.size() == 4 && concreteArrayList.size() == 6,
                "known baseline ArrayList.SubList exception-atomicity distinction");
        check(ai[0] == 3 && ii[0] == 3 && ci[0] == 3, "view failure trace");
        tailClear(actual);

        cases++;
        M3ArrayList<Object> reentrant = new M3ArrayList<>(Arrays.asList(input));
        Object[][] callbackState = {null};
        throwsExactly(ConcurrentModificationException.class, () -> reentrant.subList(1, 5).removeIf(value -> {
            reentrant.add(new Value(67));
            callbackState[0] = reentrant.toArray();
            return true;
        }));
        same(callbackState[0], reentrant, "view reentrant no iterator removal");
        tailClear(reentrant);

        for (int from = 0; from <= 6; from++) {
            for (int to = from; to <= 6; to++) {
                cases++;
                M3ArrayList<Object> m3 = new M3ArrayList<>(Arrays.asList(input));
                ArrayList<Object> jdk = new ArrayList<>(Arrays.asList(input));
                m3.subList(from, to).clear();
                jdk.subList(from, to).clear();
                same(jdk.toArray(), m3, "unaffected range removal");
                tailClear(m3);
            }
        }
    }

    public static void main(String[] arguments) {
        exhaustiveMasks();
        largeAndSeededMasks();
        callbackFailures();
        nonStructuralCallbacks();
        reentrantCallbacks();
        inheritedViewContract();
        System.out.println("M3ArrayListRemoveIfProof PASS cases=" + cases + " assertions=" + assertions);
        System.out.println("Preserved baseline: root rejects structural callbacks immediately; inherited subList retains prefix removals on callback failure.");
        System.out.println("Unused Object[] slots are directly verified null; GC scheduling and whole-JDK integration are outside this finite proof.");
    }
}
