// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Random;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.function.Predicate;

public final class M3FilteredListViewSelfTest {
    private static long checks;

    private M3FilteredListViewSelfTest() { }

    public static void main(String[] args) throws Exception {
        runtimeCapability();
        indexedMutationKeepsHiddenPositions();
        removeUsesVisibleStoredRepresentative();
        listIteratorContract();
        subListIsBackedAndVisibleIndexed();
        defaultSortAndReplaceAll();
        snapshotSuperset();
        nestedFiltersFlattenToOwner();
        randomizedLiveParity();
        System.out.println("M3_FILTERED_LIST_PASS checks=" + checks);
    }

    private static void runtimeCapability() {
        Collection<Integer> erasedArray = new ArrayList<>(List.of(1, 2, 3, 4));
        Collection<Integer> arrayView = M3CollectionViews.filtered(erasedArray, x -> (x & 1) == 0);
        check(arrayView instanceof List<?>, "erased ArrayList keeps List");
        check(arrayView instanceof RandomAccess, "ArrayList marker preserved");

        List<Integer> linked = new LinkedList<>(List.of(1, 2, 3, 4));
        List<Integer> linkedView = M3CollectionViews.filtered(linked, x -> (x & 1) == 0);
        check(!(linkedView instanceof RandomAccess), "LinkedList must not gain RandomAccess");
        eq(linkedView, List.of(2, 4));
        check(linkedView.spliterator().hasCharacteristics(Spliterator.ORDERED), "ordered");
        check(!linkedView.spliterator().hasCharacteristics(Spliterator.SIZED), "unsized");
    }

    private static void indexedMutationKeepsHiddenPositions() {
        List<Integer> owner = new ArrayList<>(List.of(1, 2, 3, 4, 5));
        List<Integer> view = M3CollectionViews.filtered(owner, x -> (x & 1) == 0);
        eq(view, List.of(2, 4));
        check(view.get(1) == 4, "visible get");

        view.add(1, 6);
        eq(owner, List.of(1, 2, 3, 6, 4, 5));
        eq(view, List.of(2, 6, 4));

        check(view.set(1, 8) == 6, "set returns old visible value");
        eq(owner, List.of(1, 2, 3, 8, 4, 5));

        check(view.remove(1) == 8, "remove visible index");
        eq(owner, List.of(1, 2, 3, 4, 5));

        check(view.addAll(1, List.of(10, 12)), "indexed addAll");
        eq(owner, List.of(1, 2, 3, 10, 12, 4, 5));
        fails(IllegalArgumentException.class, () -> view.add(1, 7));
        eq(owner, List.of(1, 2, 3, 10, 12, 4, 5));

        view.add(view.size(), 14);
        check(owner.get(owner.size() - 1) == 14, "terminal boundary maps to owner end");
    }

    private static void removeUsesVisibleStoredRepresentative() {
        Box hidden = new Box(7, false);
        Box visible = new Box(7, true);
        List<Box> owner = new ArrayList<>(List.of(hidden, visible, new Box(9, true)));
        List<Box> view = M3CollectionViews.filtered(owner, Box::visible);

        check(view.contains(new Box(7, false)), "equal probe sees visible stored representative");
        check(view.remove(new Box(7, false)), "remove first visible equal occurrence");
        check(owner.size() == 2 && owner.get(0) == hidden, "hidden equal owner entry survives");
        check(view.indexOf(new Box(9, false)) == 0, "visible indexOf stored representative");
    }

    private static void listIteratorContract() {
        List<Integer> owner = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6));
        List<Integer> view = M3CollectionViews.filtered(owner, x -> (x & 1) == 0);
        ListIterator<Integer> iterator = view.listIterator(1);

        check(iterator.nextIndex() == 1 && iterator.previousIndex() == 0, "iterator indexes");
        check(iterator.next() == 4, "iterator next");
        iterator.set(8);
        eq(view, List.of(2, 8, 6));

        iterator.add(10);
        eq(view, List.of(2, 8, 10, 6));
        check(iterator.previous() == 10, "iterator previous after add");
        iterator.remove();
        eq(view, List.of(2, 8, 6));

        check(iterator.previous() == 8, "iterator previous");
        iterator.set(12);
        eq(view, List.of(2, 12, 6));
        fails(IllegalArgumentException.class, () -> iterator.add(3));
        fails(UnsupportedOperationException.class, () -> {
            Iterator<Integer> plain = view.iterator();
            plain.next();
            plain.remove();
        });
    }

    private static void subListIsBackedAndVisibleIndexed() {
        List<Integer> owner = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6, 7, 8));
        List<Integer> view = M3CollectionViews.filtered(owner, x -> (x & 1) == 0);
        List<Integer> middle = view.subList(1, 3);
        eq(middle, List.of(4, 6));

        middle.add(1, 10);
        eq(view, List.of(2, 4, 10, 6, 8));
        eq(owner, List.of(1, 2, 3, 4, 5, 10, 6, 7, 8));

        middle.set(0, 12);
        eq(view, List.of(2, 12, 10, 6, 8));
        check(middle.remove(Integer.valueOf(10)), "sublist remove");
        eq(view, List.of(2, 12, 6, 8));
    }

    private static void defaultSortAndReplaceAll() {
        List<Integer> owner = new ArrayList<>(List.of(3, -1, 1, -2, 2));
        List<Integer> view = M3CollectionViews.filtered(owner, x -> x > 0);
        view.sort(null);
        eq(view, List.of(1, 2, 3));
        eq(owner, List.of(1, -1, 2, -2, 3));

        view.replaceAll(x -> x * 2);
        eq(view, List.of(2, 4, 6));
        eq(owner, List.of(2, -1, 4, -2, 6));
        check(view.hashCode() == List.of(2, 4, 6).hashCode(), "List hashCode");
        check(view.equals(List.of(2, 4, 6)), "List equality");
    }

    private static void snapshotSuperset() {
        List<String> source = new ArrayList<>(List.of("b", "A", "a", "B"));
        int[] calls = {0};
        Collection<String> counted = new java.util.AbstractCollection<>() {
            @Override public Iterator<String> iterator() {
                calls[0]++;
                return source.iterator();
            }
            @Override public int size() {
                throw new AssertionError("snapshot must not pre-size by source.size()");
            }
        };

        List<String> selected =
                M3CollectionViews.filteredSnapshot(counted, value -> value.compareToIgnoreCase("b") < 0);
        eq(selected, List.of("A", "a"));
        check(calls[0] == 1, "filteredSnapshot one source iterator");
        fails(UnsupportedOperationException.class, () -> selected.add("x"));

        calls[0] = 0;
        List<String> sorted =
                M3CollectionViews.filteredSortedSnapshot(
                        counted, value -> true, String.CASE_INSENSITIVE_ORDER);
        eq(sorted, List.of("A", "a", "b", "B"));
        check(calls[0] == 1, "filteredSortedSnapshot one source iterator");
        source.add("0");
        eq(sorted, List.of("A", "a", "b", "B"));
    }

    private static void nestedFiltersFlattenToOwner() throws Exception {
        List<Integer> owner = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6));
        List<Integer> even = M3CollectionViews.filtered(owner, x -> (x & 1) == 0);
        List<Integer> nested = M3CollectionViews.filtered(even, x -> x > 2);
        eq(nested, List.of(4, 6));
        owner.add(8);
        eq(nested, List.of(4, 6, 8));

        Field backing = null;
        for (Class<?> type = nested.getClass(); type != null && backing == null; type = type.getSuperclass()) {
            try { backing = type.getDeclaredField("backing"); }
            catch (NoSuchFieldException ignored) { }
        }
        check(backing != null, "backing field exists");
        backing.setAccessible(true);
        check(backing.get(nested) == owner, "nested filter flattened to authoritative owner");

        for (Class<?> type = nested.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                check(!field.getType().isArray(), "filtered list retains no index/element array");
            }
        }
    }

    private static void randomizedLiveParity() {
        Predicate<Integer> visible = x -> x != null && Math.floorMod(x, 3) != 0;
        for (int seed = 0; seed < 128; seed++) {
            Random random = new Random(0x4d334c495354L ^ seed);
            List<Integer> owner = seed % 2 == 0 ? new ArrayList<>() : new LinkedList<>();
            for (int i = 0; i < 20; i++) owner.add(random.nextInt(41) - 20);
            List<Integer> view = M3CollectionViews.filtered(owner, visible);

            for (int step = 0; step < 80; step++) {
                assertVisible(owner, view, visible);
                switch (random.nextInt(5)) {
                    case 0 -> {
                        int value;
                        do value = random.nextInt(61) - 30; while (!visible.test(value));
                        view.add(random.nextInt(view.size() + 1), value);
                    }
                    case 1 -> {
                        if (!view.isEmpty()) {
                            int value;
                            do value = random.nextInt(61) - 30; while (!visible.test(value));
                            view.set(random.nextInt(view.size()), value);
                        }
                    }
                    case 2 -> {
                        if (!view.isEmpty()) view.remove(random.nextInt(view.size()));
                    }
                    case 3 -> owner.add(random.nextInt(61) - 30);
                    default -> {
                        if (!view.isEmpty()) {
                            Integer candidate = view.get(random.nextInt(view.size()));
                            check(view.indexOf(candidate) >= 0, "indexOf visible");
                            check(view.lastIndexOf(candidate) >= view.indexOf(candidate), "lastIndexOf");
                        }
                    }
                }
            }
            assertVisible(owner, view, visible);
        }
    }

    private static void assertVisible(
            List<Integer> owner, List<Integer> view, Predicate<Integer> predicate) {
        List<Integer> expected = new ArrayList<>();
        for (Integer element : owner) if (predicate.test(element)) expected.add(element);
        eq(view, expected);
        check(view.size() == expected.size(), "visible size");
    }

    private record Box(int key, boolean visible) {
        @Override public boolean equals(Object other) {
            return other instanceof Box box && key == box.key;
        }
        @Override public int hashCode() { return key; }
    }

    private static void eq(Object actual, Object expected) {
        checks++;
        if (!Objects.equals(actual, expected)) {
            throw new AssertionError("expected " + expected + " but was " + actual);
        }
    }

    private static void check(boolean state, String message) {
        checks++;
        if (!state) throw new AssertionError(message);
    }

    private static void fails(Class<? extends Throwable> expected, Runnable operation) {
        checks++;
        try { operation.run(); }
        catch (Throwable failure) {
            if (expected.isInstance(failure)) return;
            throw new AssertionError("expected " + expected + " got " + failure, failure);
        }
        throw new AssertionError("expected " + expected.getSimpleName());
    }
}
