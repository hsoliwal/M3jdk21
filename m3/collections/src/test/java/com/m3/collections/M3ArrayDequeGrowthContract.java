/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** Independent product regression for the original generic deque's copy boundaries. */
public final class M3ArrayDequeGrowthContract {
    private static long checks;
    private static int layouts;
    private static int growthLayouts;
    private static final boolean NATIVE = Boolean.getBoolean("m3.test.native");

    private M3ArrayDequeGrowthContract() { }

    private record Token(int id) implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
    }

    public static void main(String[] args) throws Exception {
        long passed = run();
        System.out.println("M3_ARRAY_DEQUE_GROWTH_PASS checks=" + passed
                + " exportLayouts=" + layouts + " growthLayouts=" + growthLayouts
                + " backend=" + (NATIVE ? "NATIVE_REQUIRED" : "JAVA"));
    }

    public static long run() throws Exception {
        checks = 0;
        layouts = 0;
        growthLayouts = 0;
        exhaustiveLayouts();
        growthAtBothEnds();
        typedFailurePrefixes();
        serializationAndSelfReference();
        seededDequeTrace();
        return checks;
    }

    private static void exhaustiveLayouts() {
        int[] capacities = {0, 1, 2, 3, 4, 5, 7, 8, 9, 15, 16, 17, 31, 32, 33, 63, 64, 65};
        for (int capacity : capacities) {
            for (int head = 0; head < Math.max(1, capacity); head++) {
                for (int size = 0; size <= capacity; size++) {
                    // Empty deques reset the head; test that single reachable empty layout once.
                    if (size == 0 && head != 0) continue;
                    List<Token> expected = tokens(size);
                    M3ArrayDeque<Token> deque = ring(expected, capacity, head, new Token(-1));
                    List<Integer> states = states(size);
                    installStates(deque, states);
                    long revision = deque.revision();
                    equal(capacity, deque.capacity(), "constructor capacity");
                    if (size != 0) equal(head, deque.physicalIndex(0), "reachable ring head");
                    assertLogical(deque, expected, states);
                    assertArrays(deque, expected);
                    assertMask(deque, states);
                    M3ArrayDeque<Token> clone = deque.clone();
                    equal(size, clone.capacity(), "compact clone capacity");
                    equal(0, clone.revision(), "fresh clone revision");
                    assertLogical(clone, expected, states);
                    for (int i = 0; i < size; i++) equal(i, clone.physicalIndex(i), "clone layout");
                    if (size != 0) {
                        clone.updateState(0, 0, 0xfe);
                        equal(states.get(0), deque.stateAt(0), "clone state lane independent");
                        same(expected.get(0), clone.pollFirst(), "clone removal");
                        equal(size, deque.size(), "clone size independent");
                    }
                    equal(revision, deque.revision(), "copy operations preserve revision");
                    equal(capacity, deque.capacity(), "copy operations preserve capacity");
                    layouts++;
                }
            }
        }
    }

    private static void growthAtBothEnds() {
        int[] capacities = {0, 1, 2, 3, 5, 8, 15, 16, 17, 31, 32, 33, 63, 64, 65,
                127, 128, 129, 1023, 1024, 1025};
        for (int capacity : capacities) {
            int[] heads = capacity <= 129 ? ascending(Math.max(1, capacity))
                    : new int[] {0, 1, capacity / 2, capacity - 1};
            for (int head : heads) {
                for (boolean front : new boolean[] {false, true}) {
                    List<Token> expected = tokens(capacity);
                    M3ArrayDeque<Token> deque = ring(expected, capacity, head, new Token(-1));
                    List<Integer> flags = states(capacity);
                    installStates(deque, flags);
                    Iterator<Token> iterator = deque.iterator();
                    Iterator<Token> reverse = deque.descendingIterator();
                    long revision = deque.revision();
                    Token extra = new Token(-2);
                    if (front) {
                        deque.addFirst(extra);
                        expected.add(0, extra);
                        flags.add(0, M3ArrayDeque.LIVE);
                    } else {
                        deque.addLast(extra);
                        expected.add(extra);
                        flags.add(M3ArrayDeque.LIVE);
                    }
                    equal(Math.max(8, capacity + capacity / 2 + 1), deque.capacity(),
                            "unchanged growth sequence");
                    equal(revision + 1, deque.revision(), "single structural growth revision");
                    expect(ConcurrentModificationException.class, iterator::hasNext);
                    expect(ConcurrentModificationException.class, reverse::hasNext);
                    assertLogical(deque, expected, flags);
                    assertArrays(deque, expected);
                    assertMask(deque, flags);
                    for (Token token : expected) same(token, deque.removeFirst(), "growth drain order");
                    equal(0, deque.size(), "growth drain empty");
                    growthLayouts++;
                }
            }
        }
    }

    private static void typedFailurePrefixes() {
        for (int capacity : new int[] {1, 5, 8, 17}) {
            for (int head = 0; head < capacity; head++) {
                for (int bad = 0; bad < capacity; bad++) {
                    List<Object> source = new ArrayList<>();
                    for (int i = 0; i < capacity; i++) source.add(i == bad ? Integer.valueOf(42) : "s" + i);
                    M3ArrayDeque<Object> deque = ring(source, capacity, head, "filler");
                    String[] target = new String[capacity + 2];
                    Arrays.fill(target, "untouched");
                    long revision = deque.revision();
                    expect(ArrayStoreException.class, () -> deque.toArray(target));
                    // Java's partial-copy contract: the compatible prefix was stored; the suffix
                    // and null terminator remain untouched when the first incompatible value fails.
                    for (int i = 0; i < target.length; i++) {
                        same(i < bad ? source.get(i) : "untouched", target[i], "typed failing prefix");
                    }
                    equal(revision, deque.revision(), "typed failure preserves owner");
                    Object[] snapshot = deque.toArray();
                    for (int i = 0; i < source.size(); i++) same(source.get(i), snapshot[i], "typed failure content");
                }
            }
        }
        M3ArrayDeque<String> empty = new M3ArrayDeque<>();
        String[] target = {"first", "second"};
        same(target, empty.toArray(target), "empty typed identity");
        same(null, target[0], "empty null terminator");
        same("second", target[1], "empty extra tail");
        expect(NullPointerException.class, () -> empty.toArray((String[]) null));
    }

    private static void serializationAndSelfReference() throws Exception {
        for (int capacity : new int[] {0, 1, 5, 16, 65}) {
            for (int size : new int[] {0, capacity / 2, capacity}) {
                for (int head : new int[] {0, Math.max(0, capacity - 1)}) {
                    List<Token> expected = tokens(size);
                    List<Integer> flags = states(size);
                    M3ArrayDeque<Token> original = ring(expected, capacity, head, new Token(-1));
                    installStates(original, flags);
                    M3ArrayDeque<?> restored = roundTrip(original);
                    equal(size, restored.size(), "serialized size");
                    equal(size, restored.capacity(), "serialized compact capacity");
                    equal(0, restored.revision(), "serialized fresh revision");
                    Object[] actual = restored.toArray();
                    for (int i = 0; i < size; i++) {
                        check(expected.get(i).equals(actual[i]), "serialized value");
                        equal(flags.get(i), restored.stateAt(i), "serialized state");
                    }
                }
            }
        }
        M3ArrayDeque<Object> self = new M3ArrayDeque<>(1);
        self.addLast(self);
        M3ArrayDeque<Object> clone = self.clone();
        same(self, clone.getFirst(), "shallow self clone refers to original");
        same(self, self.toArray()[0], "self export");
        M3ArrayDeque<?> restored = roundTrip(self);
        same(restored, restored.getFirst(), "serialized self reference");
    }

    private static M3ArrayDeque<?> roundTrip(M3ArrayDeque<?> source) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(source); }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (M3ArrayDeque<?>) in.readObject();
        }
    }

    private static void seededDequeTrace() {
        Random random = new Random(0x5eed6a7L);
        M3ArrayDeque<Token> actual = new M3ArrayDeque<>(3);
        ArrayDeque<Token> reference = new ArrayDeque<>();
        List<Integer> flags = new ArrayList<>();
        for (int step = 0; step < 20_000; step++) {
            int action = reference.size() > 256 ? 2 : random.nextInt(9);
            switch (action) {
                case 0, 1 -> {
                    Token value = new Token(step);
                    if (action == 0) {
                        actual.addFirst(value); reference.addFirst(value); flags.add(0, M3ArrayDeque.LIVE);
                    } else {
                        actual.addLast(value); reference.addLast(value); flags.add(M3ArrayDeque.LIVE);
                    }
                }
                case 2 -> {
                    same(reference.pollFirst(), actual.pollFirst(), "random first removal");
                    if (!flags.isEmpty()) flags.remove(0);
                }
                case 3 -> {
                    same(reference.pollLast(), actual.pollLast(), "random last removal");
                    if (!flags.isEmpty()) flags.remove(flags.size() - 1);
                }
                case 4 -> {
                    if (!flags.isEmpty()) {
                        int index = random.nextInt(flags.size());
                        int bits = random.nextInt(128) << 1;
                        equal(flags.get(index), actual.updateState(index, bits, (~bits) & 0xfe), "random previous bits");
                        flags.set(index, bits | M3ArrayDeque.LIVE);
                    }
                }
                case 5 -> {
                    if (!reference.isEmpty()) {
                        int index = random.nextInt(reference.size());
                        Token value = new ArrayList<>(reference).get(index);
                        check(reference.removeFirstOccurrence(value) == actual.removeFirstOccurrence(value), "random occurrence");
                        flags.remove(index);
                    }
                }
                case 6 -> {
                    if (random.nextInt(13) == 0) { actual.clear(); reference.clear(); flags.clear(); }
                }
                case 7 -> {
                    if (!reference.isEmpty()) {
                        Iterator<Token> a = actual.iterator(), b = reference.iterator();
                        same(b.next(), a.next(), "random iterator value");
                        a.remove(); b.remove(); flags.remove(0);
                    }
                }
                default -> {
                    expect(NullPointerException.class, () -> actual.addLast(null));
                    expect(NullPointerException.class, () -> actual.addFirst(null));
                }
            }
            equal(reference.size(), actual.size(), "random size");
            if ((step & 31) == 0) {
                List<Token> values = new ArrayList<>(reference);
                assertLogical(actual, values, flags);
                assertArrays(actual, values);
                assertLogical(actual.clone(), values, flags);
            }
        }
        assertLogical(actual, new ArrayList<>(reference), flags);
    }

    private static <E> M3ArrayDeque<E> ring(List<E> logical, int capacity, int head, E filler) {
        M3ArrayDeque<E> result = new M3ArrayDeque<>(capacity);
        for (int physical = 0; physical < capacity; physical++) {
            int logicalIndex = (physical - head + capacity) % capacity;
            result.addLast(logicalIndex < logical.size() ? logical.get(logicalIndex) : filler);
        }
        for (int i = 0; i < head; i++) result.addLast(result.removeFirst());
        while (result.size() > logical.size()) result.removeLast();
        return result;
    }

    private static List<Token> tokens(int size) {
        List<Token> result = new ArrayList<>();
        for (int i = 0; i < size; i++) result.add(new Token(i));
        return result;
    }

    private static List<Integer> states(int size) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < size; i++) result.add(((i * 38 + 10) & 0xfe) | M3ArrayDeque.LIVE);
        return result;
    }

    private static void installStates(M3ArrayDeque<?> deque, List<Integer> states) {
        for (int i = 0; i < states.size(); i++) deque.updateState(i, states.get(i) & 0xfe, 0);
    }

    private static void assertLogical(M3ArrayDeque<Token> deque, List<Token> expected, List<Integer> states) {
        equal(expected.size(), deque.size(), "logical size");
        Iterator<Token> forward = deque.iterator();
        Iterator<Token> reverse = deque.descendingIterator();
        for (int i = 0; i < expected.size(); i++) {
            same(expected.get(i), forward.next(), "forward object identity");
            same(expected.get(expected.size() - 1 - i), reverse.next(), "reverse object identity");
            equal(states.get(i), deque.stateAt(i), "logical state alignment");
        }
        check(!forward.hasNext() && !reverse.hasNext(), "iterator exhaustion");
    }

    private static void assertArrays(M3ArrayDeque<Token> deque, List<Token> expected) {
        Object[] objectArray = deque.toArray();
        equal(expected.size(), objectArray.length, "object export length");
        Token[] shortArray = new Token[0];
        Token[] exactArray = new Token[expected.size()];
        Token[] longArray = new Token[expected.size() + 2];
        Token sentinel = new Token(-9);
        Arrays.fill(longArray, sentinel);
        Token[] grown = deque.toArray(shortArray);
        same(exactArray, deque.toArray(exactArray), "exact array reuse");
        same(longArray, deque.toArray(longArray), "oversized array reuse");
        equal(expected.size(), grown.length, "typed export size");
        check(grown.getClass() == Token[].class, "typed runtime array component");
        for (int i = 0; i < expected.size(); i++) {
            same(expected.get(i), objectArray[i], "object export identity");
            same(expected.get(i), grown[i], "typed new export identity");
            same(expected.get(i), exactArray[i], "typed exact export identity");
            same(expected.get(i), longArray[i], "typed oversized export identity");
        }
        same(null, longArray[expected.size()], "typed null terminator");
        same(sentinel, longArray[expected.size() + 1], "typed extra tail preserved");
        if (!expected.isEmpty()) {
            objectArray[0] = null;
            same(expected.get(0), deque.getFirst(), "export array independent");
        }
    }

    private static void assertMask(M3ArrayDeque<?> deque, List<Integer> states) {
        int[] result = deque.selectedPhysicalIndices(M3ArrayDeque.VISIBLE, M3ArrayDeque.DIRTY,
                NATIVE ? M3IndexMaskCompactor.Backend.NATIVE_REQUIRED : M3IndexMaskCompactor.Backend.JAVA);
        int count = 0;
        for (int i = 0; i < states.size(); i++) {
            int bits = states.get(i);
            if ((bits & M3ArrayDeque.VISIBLE) != 0 && (bits & M3ArrayDeque.DIRTY) == 0) {
                check(count < result.length, "mask result has slot");
                equal(deque.physicalIndex(i), result[count++], "mask preserves physical order");
            }
        }
        equal(count, result.length, "mask result count");
    }

    private static int[] ascending(int count) {
        int[] result = new int[count];
        for (int i = 0; i < count; i++) result[i] = i;
        return result;
    }

    private static void same(Object expected, Object actual, String label) { check(expected == actual, label); }
    private static void equal(long expected, long actual, String label) { check(expected == actual, label + ": " + expected + " != " + actual); }
    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("expected " + type.getName() + ", got " + failure, failure);
        }
        throw new AssertionError("expected " + type.getName() + ", operation returned normally");
    }
}
