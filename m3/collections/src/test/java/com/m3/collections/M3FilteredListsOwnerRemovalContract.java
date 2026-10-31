// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/** Differential oracle for source-owned visible and physical List removal. */
public final class M3FilteredListsOwnerRemovalContract {
    private M3FilteredListsOwnerRemovalContract() { }
    private static long checks;
    private static void check(boolean yes) {
        checks++;
        if (!yes) throw new AssertionError("owner removal failed check=" + checks);
    }
    private record Item(int key, boolean selected) {
        @Override public boolean equals(Object other) {
            return other instanceof Item item && key == item.key;
        }
        @Override public int hashCode() { return key; }
    }
    private static final class IteratorOnlyList<E> extends AbstractList<E> {
        private final ArrayList<E> values = new ArrayList<>();
        @Override public E get(int slot) { return values.get(slot); }
        @Override public int size() { return values.size(); }
        @Override public boolean add(E element) { return values.add(element); }
        @Override public Iterator<E> iterator() { return values.iterator(); }
    }
    private static List<Item> backing(int kind, List<Item> original) {
        return switch (kind) {
            case 0 -> new ArrayList<>(original);
            case 1 -> new LinkedList<>(original);
            case 2 -> new CopyOnWriteArrayList<>(original);
            case 3 -> { IteratorOnlyList<Item> list = new IteratorOnlyList<>();
                list.addAll(original); yield list; }
            default -> throw new AssertionError("invalid list kind");
        };
    }
    private static void sameSlots(List<Item> actual, List<Item> expected) {
        check(actual.size() == expected.size());
        for (int i = 0; i < expected.size(); i++) check(actual.get(i) == expected.get(i));
    }
    private static void knownEdges() {
        Item hidden = new Item(7, false), visible = new Item(7, true);
        for (int kind = 0; kind < 4; kind++) {
            List<Item> original = backing(kind, List.of(hidden, visible));
            check(M3FilteredLists.filtered(original, Item::selected).remove(0) == visible);
            sameSlots(original, List.of(hidden));
            original = backing(kind, List.of(hidden, visible));
            ListIterator<Item> it = M3FilteredLists.filtered(original, Item::selected).listIterator();
            check(it.next() == visible);
            it.hasNext(); it.hasPrevious();
            it.remove();
            sameSlots(original, List.of(hidden));
        }
        List<Item> immutable = M3FilteredLists.filtered(
                Collections.unmodifiableList(List.of(visible)), Item::selected);
        boolean rejected = false;
        try { immutable.remove(0); }
        catch (UnsupportedOperationException expected) { rejected = true; }
        check(rejected);
    }
    public static long runAll() {
        checks = 0;
        knownEdges();
        Random r = new Random(20261025L);
        for (int trial = 0; trial < 5000; trial++) {
            ArrayList<Item> initial = new ArrayList<>();
            int n = 1 + r.nextInt(30);
            for (int i = 0; i < n; i++) initial.add(new Item(r.nextInt(8), r.nextBoolean()));
            ArrayList<Integer> selected = new ArrayList<>();
            for (int i = 0; i < initial.size(); i++) if (initial.get(i).selected()) selected.add(i);
            if (selected.isEmpty()) continue;
            int logical = r.nextInt(selected.size());
            ArrayList<Item> expected = new ArrayList<>(initial);
            Item selectedElement = expected.remove((int) selected.get(logical));
            for (int kind = 0; kind < 4; kind++) {
                List<Item> list = backing(kind, initial);
                check(M3FilteredLists.filtered(list, Item::selected).remove(logical) == selectedElement);
                sameSlots(list, expected);
                list = backing(kind, initial);
                ListIterator<Item> it = M3FilteredLists.filtered(list, Item::selected).listIterator();
                for (int i = 0; i <= logical; i++) {
                    Item item = it.next();
                    if (i == logical) check(item == selectedElement);
                }
                it.hasNext();
                it.remove();
                sameSlots(list, expected);
            }
        }
        return checks;
    }
    public static void main(String[] ignored) {
        System.out.println("M3_FILTERED_LIST_OWNER_REMOVAL_PASS checks=" + runAll());
    }
}
