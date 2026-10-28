/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * @test
 * @summary Filtered List removes the visible physical occurrence using the supported mutation form
 * @run main FilteredListRemoveContractTest
 */
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.CollectionViews;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public final class FilteredListRemoveContractTest {
    private record Item(int key, boolean visible) {
        @Override public boolean equals(Object value) {
            return value instanceof Item item && key == item.key;
        }
        @Override public int hashCode() { return key; }
    }

    private static final class IteratorOnlyList<E> extends AbstractList<E> {
        private final ArrayList<E> items = new ArrayList<>();
        @Override public E get(int index) { return items.get(index); }
        @Override public int size() { return items.size(); }
        @Override public boolean add(E item) { return items.add(item); }
        @Override public E remove(int index) {
            throw new UnsupportedOperationException("index removal unsupported");
        }
        @Override public Iterator<E> iterator() { return items.iterator(); }
    }

    private static long checks;
    private static void check(boolean value) {
        checks++;
        if (!value) throw new AssertionError("check=" + checks);
    }

    private static Collection<Item> filtered(List<Item> list) {
        return CollectionViews.filtered((Collection<Item>) list, Item::visible);
    }

    private static void knownCases() {
        Item hidden = new Item(8, false);
        Item shown = new Item(8, true);
        Item probe = new Item(8, false);
        IteratorOnlyList<Item> iteratorOnly = new IteratorOnlyList<>();
        iteratorOnly.add(hidden);
        iteratorOnly.add(shown);
        Collection<Item> view = filtered(iteratorOnly);
        check(view.remove(probe));
        check(iteratorOnly.size() == 1 && iteratorOnly.get(0) == hidden);
        check(!view.remove(probe));

        for (List<Item> list : List.<List<Item>>of(
                new ArrayList<>(), new LinkedList<>(), new CopyOnWriteArrayList<>())) {
            list.add(hidden);
            list.add(shown);
            Collection<Item> candidate = filtered(list);
            check(candidate.remove(probe));
            check(list.size() == 1 && list.get(0) == hidden);
            check(!candidate.remove(probe));
        }
    }

    private static void randomDifferential() {
        Random random = new Random(0x5EED2026L);
        for (int trial = 0; trial < 10_000; trial++) {
            int count = 1 + random.nextInt(30);
            ArrayList<Item> original = new ArrayList<>();
            for (int index = 0; index < count; index++) {
                original.add(new Item(random.nextInt(12), random.nextBoolean()));
            }
            Item probe = new Item(random.nextInt(12), false);
            ArrayList<Item> expected = new ArrayList<>(original);
            int physical = -1;
            for (int index = 0; index < expected.size(); index++) {
                Item stored = expected.get(index);
                if (Objects.equals(probe, stored) && stored.visible()) {
                    physical = index;
                    break;
                }
            }
            boolean found = physical >= 0;
            if (found) expected.remove(physical);
            for (int variant = 0; variant < 4; variant++) {
                List<Item> owner;
                if (variant == 0) owner = new ArrayList<>(original);
                else if (variant == 1) owner = new LinkedList<>(original);
                else if (variant == 2) owner = new CopyOnWriteArrayList<>(original);
                else {
                    IteratorOnlyList<Item> custom = new IteratorOnlyList<>();
                    custom.addAll(original);
                    owner = custom;
                }
                check(filtered(owner).remove(probe) == found);
                check(owner.size() == expected.size());
                for (int index = 0; index < expected.size(); index++) {
                    check(owner.get(index) == expected.get(index));
                }
            }
        }
    }

    public static void main(String[] ignored) {
        knownCases();
        randomDifferential();
        System.out.println("FilteredListRemoveContractTest PASS checks=" + checks);
    }
}
