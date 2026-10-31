/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.function.Predicate;
import java.util.concurrent.atomic.AtomicInteger;

/** Standalone randomized differential oracle with no test-library dependency. */
public final class M3GenericArrayListSelfTest {
    private static long checks;
    private M3GenericArrayListSelfTest() { }
    private static void check(boolean condition, String what) {
        checks++;
        if (!condition) throw new AssertionError(what + " at check=" + checks);
    }
    private static void expect(Class<? extends Throwable> type, Runnable runnable) {
        checks++;
        try { runnable.run(); }
        catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("expected " + type + ", got " + error, error);
        }
        throw new AssertionError("expected " + type);
    }
    private static Integer value(Random r) {
        return r.nextInt(8) == 0 ? null : r.nextInt(71) - 35;
    }
    private static void same(M3ArrayList<Integer> m3, ArrayList<Integer> oracle) {
        check(m3.size() == oracle.size(), "size");
        check(m3.capacity() >= m3.size(), "capacity >= logical size");
        check(m3.equals(oracle), "equals JDK ArrayList");
        check(oracle.equals(m3), "reverse equals");
        check(m3.hashCode() == oracle.hashCode(), "hashCode");
        check(Arrays.equals(m3.toArray(), oracle.toArray()), "unboxed reference array contents");
        check(Arrays.equals(m3.toArray(new Integer[0]), oracle.toArray(new Integer[0])), "typed array");
        check(m3.isEmpty() == oracle.isEmpty(), "isEmpty");
        int at = 0;
        for (Integer item : m3) { check(java.util.Objects.equals(item, oracle.get(at++)), "iterator"); }
        check(at == oracle.size(), "iteration length");
    }
    private static void basicMutations() {
        Random random = new Random(0x6d3a7f23L);
        for (int round = 0; round < 95; round++) {
            M3ArrayList<Integer> current = new M3ArrayList<>();
            ArrayList<Integer> baseline = new ArrayList<>();
            for (int step = 0; step < 450; step++) {
                int op = random.nextInt(14);
                switch (op) {
                    case 0, 1 -> { Integer item = value(random); current.add(item); baseline.add(item); }
                    case 2 -> { Integer item = value(random); int index = random.nextInt(current.size() + 1);
                        current.add(index, item); baseline.add(index, item); }
                    case 3 -> { if (!baseline.isEmpty()) { int index = random.nextInt(baseline.size());
                        Integer item = value(random);
                        check(java.util.Objects.equals(current.set(index, item), baseline.set(index, item)), "set returns previous"); } }
                    case 4 -> { if (!baseline.isEmpty()) { int index = random.nextInt(baseline.size());
                        check(java.util.Objects.equals(current.remove(index), baseline.remove(index)), "remove returns previous"); } }
                    case 5 -> { Integer item = value(random);
                        check(current.remove(item) == baseline.remove(item), "remove occurrence"); }
                    case 6 -> { ArrayList<Integer> batch = new ArrayList<>();
                        for (int n = random.nextInt(6); n > 0; n--) batch.add(value(random));
                        int index = random.nextInt(current.size() + 1);
                        check(current.addAll(index, batch) == baseline.addAll(index, batch), "addAll indexed"); }
                    case 7 -> { if (baseline.size() < 80 && random.nextInt(9) == 0) {
                        current.addAll(current); baseline.addAll(baseline); } }
                    case 8 -> { int from = random.nextInt(current.size() + 1);
                        int to = from + random.nextInt(current.size() - from + 1);
                        check(current.subList(from, to).equals(baseline.subList(from, to)), "sublist before clearing");
                        current.subList(from, to).clear(); baseline.subList(from, to).clear(); }
                    case 9 -> { Predicate<Integer> p = item -> item == null || (item & 3) == 0;
                        check(current.removeIf(p) == baseline.removeIf(p), "removeIf result"); }
                    case 10 -> { Comparator<Integer> order = Comparator.nullsFirst(Comparator.naturalOrder());
                        current.sort(order); baseline.sort(order); }
                    case 11 -> { current.trimToSize(); baseline.trimToSize(); }
                    case 12 -> { current.ensureCapacity(current.size() + random.nextInt(24)); }
                    case 13 -> { if (random.nextInt(17) == 0) { current.clear(); baseline.clear(); } }
                    default -> throw new AssertionError(op);
                }
                same(current, baseline);
            }
        }
    }
    private static void behavioralContracts() throws Exception {
        M3ArrayList<String> values = new M3ArrayList<>(List.of("a", "b", "c"));
        Iterator<String> iterator = values.iterator();
        values.add("d");
        expect(ConcurrentModificationException.class, iterator::next);
        List<String> section = values.subList(1, 3);
        section.remove(0);
        check(values.equals(List.of("a", "c", "d")), "sublist write-through");
        ListIterator<String> cursor = values.listIterator();
        check(cursor.next().equals("a"), "list iterator next");
        cursor.add("new");
        check(values.equals(List.of("a", "new", "c", "d")), "list iterator addition");
        cursor.previous();
        cursor.set("replaced");
        check(values.get(1).equals("replaced"), "list iterator set");
        M3ArrayList<String> clone = values.clone();
        clone.set(1, "copy");
        check(values.get(1).equals("replaced"), "cloned backing isolation");
        check(clone.get(1).equals("copy"), "cloned contents");
        M3ArrayList<Integer> original = new M3ArrayList<>();
        original.add(null); original.add(1); original.add(null);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) { output.writeObject(original); }
        Object reopened;
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            reopened = input.readObject();
        }
        check(reopened instanceof M3ArrayList<?>, "own serialized form");
        check(original.equals(reopened), "serialized values including null");
        M3ArrayList<Integer> fails = new M3ArrayList<>(Arrays.asList(1, 2, null, 3));
        expect(IllegalStateException.class, () -> fails.removeIf(item -> {
            if (item == null) throw new IllegalStateException("predicate failure");
            return item > 1;
        }));
        check(fails.equals(Arrays.asList(1, 2, null, 3)), "removeIf exception atomicity");
        expect(IndexOutOfBoundsException.class, () -> fails.add(-1, 2));
        expect(IndexOutOfBoundsException.class, () -> fails.get(fails.size()));
        expect(IllegalArgumentException.class, () -> new M3ArrayList<>(-1));
        expect(NullPointerException.class, () -> fails.addAll((List<Integer>) null));
        Integer[] longer = new Integer[10]; Arrays.fill(longer, 99);
        check(fails.toArray(longer) == longer && longer[fails.size()] == null, "typed array terminator");
    }
    private static void verifySnapshot(Integer[] incoming, Random random) {
        M3IndexedListSnapshot<Integer> snapshot = M3IndexedListSnapshot.copyOf(incoming);
        Integer[] original = incoming.clone();
        Arrays.fill(incoming, 12345);
        check(snapshot.equals(Arrays.asList(original)), "snapshot of caller array is detached");
        int from = random.nextInt(original.length + 1);
        int to = from + random.nextInt(original.length - from + 1);
        M3IndexedListSnapshot<Integer> view = snapshot.subList(from, to);
        ArrayList<Integer> expected = new ArrayList<>(Arrays.asList(original).subList(from, to));
        check(view.equals(expected), "slice logical positions");
        AtomicInteger invocations = new AtomicInteger();
        M3IndexedListSnapshot<Integer> filtered = view.filtered(item -> {
            invocations.incrementAndGet();
            return item == null || item >= 0;
        });
        List<Integer> filteredOracle = new ArrayList<>();
        for (Integer item : expected) if (item == null || item >= 0) filteredOracle.add(item);
        check(filtered.equals(filteredOracle), "filtered JDK contract");
        check(invocations.get() == expected.size(), "predicate exactly once per candidate");
        Comparator<Integer> order = Comparator.nullsFirst(Comparator.naturalOrder());
        List<Integer> sortOracle = new ArrayList<>(filteredOracle);
        sortOracle.sort(order);
        M3IndexedListSnapshot<Integer> sorted = filtered.sorted(order);
        check(sorted.equals(sortOracle), "indirect stable sort equals JDK");
        check(filtered.equals(filteredOracle), "sorting does not change filtered view");
        check(sorted.subList(0, sorted.size()).equals(sorted), "identity sublist");
        check(Arrays.equals(sorted.toArray(), sortOracle.toArray()), "sorted materialization");
        check(invocations.get() == expected.size(), "no repeat filter across sorting/reads");
        BitSet visible = filtered.physicalMaskCopy();
        for (int physical = 0; physical < original.length; physical++) {
            boolean shouldSee = false;
            for (int i = 0; i < filtered.size(); i++) {
                if (filtered.physicalIndex(i) == physical) shouldSee = true;
            }
            check(visible.get(physical) == shouldSee, "bitmap maps physical occurrence, not object equality");
            check(filtered.includesPhysicalIndex(physical) == shouldSee, "physical membership");
        }
        visible.flip(0, original.length);
        check(filtered.physicalMaskCopy().equals(sorted.physicalMaskCopy()), "bitset defensive copy");
        if (!sorted.isEmpty()) {
            int index = random.nextInt(sorted.size());
            int slot = sorted.physicalIndex(index);
            check(java.util.Objects.equals(sorted.get(index), original[slot]), "physical index resolves payload");
            check(sorted.subList(index, index + 1).includesPhysicalIndex(slot), "slice membership");
        }
        expect(UnsupportedOperationException.class, () -> filtered.add(null));
        expect(UnsupportedOperationException.class, () -> filtered.set(0, null));
        expect(UnsupportedOperationException.class, () -> filtered.addAll(List.of()));
        expect(UnsupportedOperationException.class, () -> filtered.removeIf(item -> false));
        expect(UnsupportedOperationException.class, () -> filtered.sort(order));
        expect(UnsupportedOperationException.class, filtered::clear);
        expect(UnsupportedOperationException.class, () -> filtered.subList(0, 0).addAll(List.of()));
        expect(IndexOutOfBoundsException.class, () -> filtered.subList(0, filtered.size() + 1));
        if (M3IndexMaskCompactor.nativeAvailable()) {
            check(filtered.equals(view.filtered(item -> item == null || item >= 0,
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED)), "real JNI select parity");
        } else {
            AtomicInteger rejectedCalls = new AtomicInteger();
            expect(UnsatisfiedLinkError.class, () -> view.filtered(item -> {
                rejectedCalls.incrementAndGet(); return true;
            }, M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
            check(rejectedCalls.get() == 0, "missing required JNI refuses before callback");
        }
    }
    private static void objectProjection() {
        Random random = new Random(0x12dd5e89a3L);
        verifySnapshot(new Integer[]{null, -2, -2, 0, 3, null, 3}, random);
        verifySnapshot(new Integer[0], random);
        for (int trial = 0; trial < 370; trial++) {
            Integer[] input = new Integer[random.nextInt(205)];
            for (int i = 0; i < input.length; i++) input[i] = value(random);
            verifySnapshot(input, random);
        }
        M3ArrayList<Integer> fromFactory = M3GenericCollections.arrayList(List.of(9, 4, 7));
        check(fromFactory.equals(List.of(9, 4, 7)), "real generic SDK factory");
        check(M3GenericCollections.filteredSnapshot(List.of(1, 2, 3), i -> i > 1)
                .equals(List.of(2, 3)), "factory filtered snapshot");
        check(M3GenericCollections.sortedSnapshot(List.of(3, 1, 2), Comparator.naturalOrder())
                .equals(List.of(1, 2, 3)), "factory sorted snapshot");
        M3ArrayList<Integer> owner = new M3ArrayList<>(List.of(1, 2, 3));
        M3IndexedListSnapshot<Integer> frozen = owner.snapshot();
        owner.set(1, 999);
        check(frozen.equals(List.of(1, 2, 3)), "mutable owner detached snapshot");
        M3IndexedListSnapshot<Integer> original = M3IndexedListSnapshot.copyOf(new Integer[]{3, 2, 1});
        check(original.sorted().equals(List.of(1, 2, 3)), "natural ordering");
        expect(NullPointerException.class, () -> M3IndexedListSnapshot.copyOf((Integer[]) null));
    }
    public static void main(String[] args) throws Exception {
        basicMutations();
        behavioralContracts();
        objectProjection();
        System.out.println("PASS M3GenericArrayListSelfTest checks=" + checks
                + " native=" + M3IndexMaskCompactor.nativeAvailable());
    }
}
