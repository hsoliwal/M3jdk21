/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * @test
 * @summary Backed filtered bulk removal must honor the owner's optional List operations
 * @run main FilteredBulkMutationContractTest
 */
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.CollectionViews;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Predicate;

public final class FilteredBulkMutationContractTest {
    private static long checks;

    private record Item(int key, boolean visible, int occurrence) {
        @Override public boolean equals(Object candidate) {
            return candidate instanceof Item item && key == item.key;
        }
        @Override public int hashCode() { return key; }
    }

    private static final class IndexOnlyList<E> extends AbstractList<E> {
        private final ArrayList<E> elements = new ArrayList<>();
        @Override public E get(int index) { return elements.get(index); }
        @Override public int size() { return elements.size(); }
        @Override public boolean add(E element) { return elements.add(element); }
        @Override public E remove(int index) { return elements.remove(index); }
        @Override public Iterator<E> iterator() {
            Iterator<E> cursor = elements.iterator();
            return new Iterator<>() {
                @Override public boolean hasNext() { return cursor.hasNext(); }
                @Override public E next() { return cursor.next(); }
                @Override public void remove() { throw new UnsupportedOperationException("iterator mutation"); }
            };
        }
    }

    private static final class IteratorOnlyList<E> extends AbstractList<E> {
        private final ArrayList<E> elements = new ArrayList<>();
        @Override public E get(int index) { return elements.get(index); }
        @Override public int size() { return elements.size(); }
        @Override public boolean add(E element) { return elements.add(element); }
        @Override public E remove(int index) { throw new UnsupportedOperationException("index mutation"); }
        @Override public Iterator<E> iterator() { return elements.iterator(); }
    }

    private static final class CopyOnWriteListSubclass<E> extends CopyOnWriteArrayList<E> { }
    private static final class CopyOnWriteSetSubclass<E> extends CopyOnWriteArraySet<E> { }

    private static void check(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError("check=" + checks + ": " + reason);
    }

    private static List<Item> newList(int variant) {
        return switch (variant) {
            case 0 -> new ArrayList<>();
            case 1 -> new LinkedList<>();
            case 2 -> new CopyOnWriteArrayList<>();
            case 3 -> new CopyOnWriteListSubclass<>();
            case 4 -> new IndexOnlyList<>();
            case 5 -> new IteratorOnlyList<>();
            default -> throw new AssertionError("unknown List owner");
        };
    }

    private static void randomizedOwnerContracts() {
        Random random = new Random(0x63238CA5L);
        for (int variant = 0; variant < 6; variant++) {
            for (int trial = 0; trial < 2500; trial++) {
                int n = random.nextInt(38);
                ArrayList<Item> original = new ArrayList<>();
                for (int i = 0; i < n; i++) {
                    original.add(new Item(random.nextInt(10), random.nextBoolean(), i));
                }
                List<Item> actual = newList(variant);
                actual.addAll(original);
                ArrayList<Item> expected = new ArrayList<>(original);
                int operation = random.nextInt(4);
                int selectedKey = random.nextInt(10);
                Predicate<Item> delete = switch (operation) {
                    case 0 -> item -> true;
                    case 1 -> item -> item.key() % 3 == selectedKey % 3;
                    case 2 -> item -> item.key() == selectedKey;
                    case 3 -> item -> item.key() != selectedKey;
                    default -> throw new AssertionError();
                };
                expected.removeIf(item -> item.visible() && delete.test(item));
                Collection<Item> filtered = CollectionViews.filtered((Collection<Item>) actual, Item::visible);
                boolean changed = switch (operation) {
                    case 0 -> filtered.removeIf(item -> true);
                    case 1 -> filtered.removeIf(delete);
                    case 2 -> filtered.removeAll(List.of(new Item(selectedKey, false, -1)));
                    case 3 -> filtered.retainAll(List.of(new Item(selectedKey, false, -1)));
                    default -> throw new AssertionError();
                };
                check(changed == (expected.size() != n), "mutation status " + variant);
                check(actual.size() == expected.size(), "physical size " + variant);
                for (int index = 0; index < expected.size(); index++) {
                    check(actual.get(index) == expected.get(index), "stored occurrence " + variant);
                }
            }
        }
    }

    private static void subtypeAndClearContracts() {
        CopyOnWriteSetSubclass<Item> set = new CopyOnWriteSetSubclass<>();
        Item visible = new Item(2, true, 1);
        Item hidden = new Item(3, false, 2);
        set.add(visible);
        set.add(hidden);
        Collection<Item> selected = CollectionViews.filtered((Collection<Item>) set, Item::visible);
        check(selected.removeIf(item -> true), "CopyOnWriteArraySet subclass mutation");
        check(set.size() == 1 && set.contains(hidden), "hidden Set member preserved");
        IndexOnlyList<Item> indexOnly = new IndexOnlyList<>();
        indexOnly.add(hidden);
        indexOnly.add(visible);
        CollectionViews.filtered((Collection<Item>) indexOnly, Item::visible).clear();
        check(indexOnly.size() == 1 && indexOnly.get(0) == hidden,
                "iterator-unsupported List clear preserves hidden slot");
    }

    public static void main(String[] args) {
        randomizedOwnerContracts();
        subtypeAndClearContracts();
        System.out.println("FilteredBulkMutationContractTest PASS checks=" + checks);
    }
}
