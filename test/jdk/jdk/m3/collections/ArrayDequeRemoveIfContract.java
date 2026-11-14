/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/** Independent public-contract regression, frozen before the removeIf recipe pass. */
public final class ArrayDequeRemoveIfContract {
    private static long checks;
    private static int cases;

    private ArrayDequeRemoveIfContract() { }

    private record Token(int id) implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
    }

    public static void main(String[] args) throws Exception {
        run();
        System.out.println("M3_ARRAY_DEQUE_REMOVE_IF_PASS cases=" + cases + " checks=" + checks);
    }

    public static long run() throws Exception {
        checks = 0;
        cases = 0;
        exhaustiveMasks();
        throwingCallbacks();
        wordBoundaries();
        reentrantCallbacks();
        nullAndNestedCallbacks();
        repeatedFilteringAndGrowth();
        serializationAfterCompaction();
        return checks;
    }

    private static void exhaustiveMasks() {
        for (int size = 0; size <= 8; size++) {
            int capacity = size + 2;
            for (int head = 0; head < (size == 0 ? 1 : capacity); head++) {
                for (int mask = 0; mask < 1 << size; mask++) {
                    boolean[] rejected = new boolean[size];
                    for (int i = 0; i < size; i++) rejected[i] = (mask & (1 << i)) != 0;
                    filterCase(size, capacity, head, rejected, -1);
                }
            }
        }
    }

    private static void throwingCallbacks() {
        for (int size = 1; size <= 6; size++) {
            for (int mask = 0; mask < 1 << size; mask++) {
                boolean[] rejected = new boolean[size];
                for (int i = 0; i < size; i++) rejected[i] = (mask & (1 << i)) != 0;
                for (int failure = 0; failure < size; failure++) {
                    filterCase(size, size + 2, size + 1, rejected, failure);
                }
            }
        }
    }

    private static void wordBoundaries() {
        for (int size : new int[] {1, 2, 7, 31, 32, 33, 63, 64, 65, 127, 128,
                129, 191, 192, 193, 255, 256, 257, 1023, 1024, 1025, 4096}) {
            for (int head : new int[] {0, 1, size / 2, size + 1}) {
                for (int shape = 0; shape < 7; shape++) {
                    boolean[] rejected = new boolean[size];
                    for (int i = 0; i < size; i++) {
                        rejected[i] = switch (shape) {
                            case 0 -> false;
                            case 1 -> true;
                            case 2 -> (i & 1) == 0;
                            case 3 -> i == size - 1;
                            case 4 -> i == 0 || i == size - 1;
                            case 5 -> i >= size / 2 && (i & 7) == 3;
                            default -> i == 63 || i == 64 || i == 65 || i == 127 || i == 128;
                        };
                    }
                    filterCase(size, size + 2, head, rejected, -1);
                    if (size >= 64 && shape == 6) {
                        filterCase(size, size + 2, head, rejected, size - 1);
                    }
                }
            }
        }
    }

    private static void filterCase(int size, int capacity, int head,
            boolean[] rejected, int throwAt) {
        List<Token> before = tokens(size);
        M3ArrayDeque<Token> deque = ring(before, capacity, head);
        long revision = deque.revision();
        Iterator<Token> forward = deque.iterator();
        Iterator<Token> reverse = deque.descendingIterator();
        int[] calls = {0};
        RuntimeException sentinel = new IllegalStateException("frozen callback failure");
        boolean changed = false;
        try {
            changed = deque.removeIf(token -> {
                int i = calls[0]++;
                same(before.get(i), token, "callback occurrence identity");
                equal(revision, deque.revision(), "callbacks precede commit");
                equal(size, deque.size(), "callback size");
                if (size < 16 || i == 0 || i == size / 2 || i == size - 1) {
                    verify(deque, before, allIndices(size), capacity, head);
                }
                if (i == throwAt) throw sentinel;
                return rejected[i];
            });
            check(throwAt < 0, "expected callback failure");
        } catch (RuntimeException failure) {
            same(sentinel, failure, "original thrown instance");
            check(throwAt >= 0, "unexpected callback failure");
        }
        equal(throwAt < 0 ? size : throwAt + 1, calls[0], "exact callback count");
        List<Token> expected = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            if (throwAt >= 0 || !rejected[i]) {
                expected.add(before.get(i));
                indices.add(i);
            }
        }
        verify(deque, expected, indices, capacity, expected.isEmpty() ? 0 : head);
        boolean removed = expected.size() != size;
        check(changed == removed, "changed result");
        equal(revision + (removed ? 1 : 0), deque.revision(), "single commit revision");
        if (removed) {
            expect(ConcurrentModificationException.class, forward::hasNext);
            expect(ConcurrentModificationException.class, reverse::hasNext);
        } else {
            for (int i = 0; i < size; i++) {
                same(before.get(i), forward.next(), "unmodified forward iterator");
                same(before.get(size - 1 - i), reverse.next(), "unmodified reverse iterator");
            }
            check(!forward.hasNext() && !reverse.hasNext(), "iterator exhaustion");
        }
        cases++;
    }

    private static void reentrantCallbacks() {
        List<Consumer<M3ArrayDeque<Token>>> mutations = List.of(
                q -> q.addFirst(new Token(-10)), q -> q.addLast(new Token(-11)),
                M3ArrayDeque::removeFirst, M3ArrayDeque::removeLast, M3ArrayDeque::clear,
                q -> q.updateState(0, M3ArrayDeque.DIRTY, 0),
                q -> q.removeIf(ignored -> true));
        for (Consumer<M3ArrayDeque<Token>> mutation : mutations) {
            for (int at : new int[] {0, 3, 7}) {
                for (boolean rejectEarlier : new boolean[] {false, true}) {
                    List<Token> values = tokens(8);
                    M3ArrayDeque<Token> actual = ring(values, 10, 9);
                    M3ArrayDeque<Token> expected = ring(values, 10, 9);
                    mutation.accept(expected);
                    int[] calls = {0};
                    expect(ConcurrentModificationException.class, () -> actual.removeIf(token -> {
                        int i = calls[0]++;
                        if (i == at) mutation.accept(actual);
                        return rejectEarlier;
                    }));
                    equal(at + 1, calls[0], "stop at reentrant mutation");
                    compareOwners(expected, actual, "callback-only mutation survives");
                    cases++;
                }
            }
        }
        M3ArrayDeque<Token> deque = ring(tokens(8), 10, 9);
        RuntimeException sentinel = new IllegalArgumentException("mutate then throw");
        try {
            deque.removeIf(token -> { deque.clear(); throw sentinel; });
            throw new AssertionError("missing sentinel");
        } catch (RuntimeException failure) {
            same(sentinel, failure, "throw precedes revision check");
        }
        equal(0, deque.size(), "callback clear retained");
        cases++;
    }

    private static void nullAndNestedCallbacks() {
        for (int size : new int[] {0, 1, 65}) {
            M3ArrayDeque<Token> deque = ring(tokens(size), size + 2, size == 0 ? 0 : 1);
            long revision = deque.revision();
            expect(NullPointerException.class, () -> deque.removeIf(null));
            expect(NullPointerException.class, () -> deque.addFirst(null));
            expect(NullPointerException.class, () -> deque.addLast(null));
            equal(revision, deque.revision(), "null refusal preserves epoch");
            int[] callbacks = {0};
            check(!deque.removeIf(token -> {
                callbacks[0]++;
                deque.updateState(0, 0, 0);
                check(!deque.removeIf(ignored -> false), "nested no-op result");
                return false;
            }), "outer no-op result");
            equal(size, callbacks[0], "nested outer count");
            equal(revision, deque.revision(), "nested no-op revision");
            cases++;
        }
    }

    private static void repeatedFilteringAndGrowth() {
        Random random = new Random(0x34de9e75L);
        M3ArrayDeque<Token> deque = new M3ArrayDeque<>(1);
        List<Token> expected = new ArrayList<>();
        List<Integer> expectedFlags = new ArrayList<>();
        int next = 0;
        for (int step = 0; step < 1200; step++) {
            int additions = random.nextInt(8);
            for (int i = 0; i < additions; i++) {
                Token token = new Token(next++);
                int flags = (token.id * 38 + 10) & 0xfe;
                if (random.nextBoolean()) {
                    deque.addFirst(token); expected.add(0, token); expectedFlags.add(0, flags | 1);
                    deque.updateState(0, flags, 0);
                } else {
                    deque.addLast(token); expected.add(token); expectedFlags.add(flags | 1);
                    deque.updateState(deque.size() - 1, flags, 0);
                }
            }
            int divisor = 2 + random.nextInt(7);
            int residue = random.nextInt(divisor);
            boolean changed = deque.removeIf(token -> token.id % divisor == residue);
            boolean referenceChanged = false;
            for (int i = expected.size() - 1; i >= 0; i--) {
                if (expected.get(i).id % divisor == residue) {
                    expected.remove(i); expectedFlags.remove(i); referenceChanged = true;
                }
            }
            check(changed == referenceChanged, "trace result");
            equal(expected.size(), deque.size(), "trace size");
            Object[] actual = deque.toArray();
            for (int i = 0; i < actual.length; i++) {
                same(expected.get(i), actual[i], "trace identity order");
                equal(expectedFlags.get(i), deque.stateAt(i), "trace flags");
            }
            cases++;
        }
    }

    private static void serializationAfterCompaction() throws Exception {
        M3ArrayDeque<Token> deque = ring(tokens(129), 131, 100);
        check(deque.removeIf(token -> (token.id & 3) == 1), "serialized case removes");
        M3ArrayDeque<Token> clone = deque.clone();
        Object[] expected = deque.toArray();
        Object[] cloned = clone.toArray();
        for (int i = 0; i < expected.length; i++) {
            same(expected[i], cloned[i], "shallow clone after filter");
            equal(deque.stateAt(i), clone.stateAt(i), "clone flags after filter");
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(deque); }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            M3ArrayDeque<?> restored = (M3ArrayDeque<?>) in.readObject();
            Object[] actual = restored.toArray();
            equal(expected.length, actual.length, "serialized count");
            for (int i = 0; i < actual.length; i++) {
                check(expected[i].equals(actual[i]), "serialized value order");
                equal(deque.stateAt(i), restored.stateAt(i), "serialized flags");
            }
        }
        cases++;
    }

    private static List<Token> tokens(int size) {
        List<Token> values = new ArrayList<>();
        Token repeated = new Token(-1);
        for (int i = 0; i < size; i++) values.add((i & 3) == 0 ? repeated : new Token(i));
        return values;
    }

    private static List<Integer> allIndices(int size) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < size; i++) result.add(i);
        return result;
    }

    private static M3ArrayDeque<Token> ring(List<Token> logical, int capacity, int head) {
        M3ArrayDeque<Token> deque = new M3ArrayDeque<>(capacity);
        Token filler = new Token(-2);
        for (int slot = 0; slot < capacity; slot++) {
            int index = (slot - head + capacity) % capacity;
            deque.addLast(index < logical.size() ? logical.get(index) : filler);
        }
        for (int i = 0; i < head; i++) deque.addLast(deque.removeFirst());
        while (deque.size() > logical.size()) deque.removeLast();
        for (int i = 0; i < deque.size(); i++) deque.updateState(i, (i * 38 + 10) & 0xfe, 0);
        return deque;
    }

    private static void verify(M3ArrayDeque<Token> actual, List<Token> values,
            List<Integer> originalIndices, int capacity, int head) {
        equal(values.size(), actual.size(), "logical size");
        equal(capacity, actual.capacity(), "unchanged capacity");
        if (!values.isEmpty()) equal(head, actual.physicalIndex(0), "retained head");
        Object[] exported = actual.toArray();
        for (int i = 0; i < values.size(); i++) {
            same(values.get(i), exported[i], "logical identity order");
            equal(((originalIndices.get(i) * 38 + 10) & 0xfe) | 1, actual.stateAt(i), "occurrence flags");
        }
        Iterator<Token> backwards = actual.descendingIterator();
        for (int i = values.size() - 1; i >= 0; i--) same(values.get(i), backwards.next(), "reverse order");
        check(!backwards.hasNext(), "reverse exhaustion");
    }

    private static void compareOwners(M3ArrayDeque<Token> expected,
            M3ArrayDeque<Token> actual, String message) {
        equal(expected.revision(), actual.revision(), message + " revision");
        equal(expected.size(), actual.size(), message + " size");
        equal(expected.capacity(), actual.capacity(), message + " capacity");
        Object[] first = expected.toArray();
        Object[] second = actual.toArray();
        for (int i = 0; i < first.length; i++) {
            check(first[i].equals(second[i]), message + " value");
            equal(expected.stateAt(i), actual.stateAt(i), message + " flags");
            equal(expected.physicalIndex(i), actual.physicalIndex(i), message + " physical position");
        }
    }

    private static void same(Object expected, Object actual, String message) {
        check(expected == actual, message);
    }

    private static void equal(long expected, long actual, String message) {
        check(expected == actual, message + ": " + expected + " != " + actual);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("expected " + type.getName(), failure);
        }
        throw new AssertionError("expected " + type.getName());
    }
}
