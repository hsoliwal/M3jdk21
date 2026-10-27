/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * @test
 * @summary Filtered Deque retains backing-owner membership and removes correct visible occurrence
 * @run main com.m3.collections.M3FilteredDequeOwnerMembershipContract
 */
package com.m3.collections;
import java.util.*;
import java.util.function.Predicate;

public final class M3FilteredDequeOwnerMembershipContract {
    private static long checks;
    private static void check(boolean condition, String label) {
        ++checks;
        if (!condition) throw new AssertionError(label + " at check=" + checks);
    }
    record Item(int key, boolean selected) {
        @Override public boolean equals(Object value) {
            return value instanceof Item item && key == item.key;
        }
        @Override public int hashCode() { return key; }
    }
    static class IdentityDeque extends LinkedList<Item> {
        private static final long serialVersionUID = 1L;
        @Override public boolean contains(Object candidate) {
            for (Item item : this) if (item == candidate) return true;
            return false;
        }
    }
    private static boolean expectedRemove(Deque<Item> src, Item query, boolean tail, boolean identityOwner) {
        if (identityOwner && !src.contains(query)) return false;
        Iterator<Item> iterator = tail ? src.descendingIterator() : src.iterator();
        while (iterator.hasNext()) {
            Item item = iterator.next();
            if (Objects.equals(query, item) && item.selected()) { iterator.remove(); return true; }
        }
        return false;
    }
    private static boolean expectedContains(Deque<Item> src, Item query, boolean identityOwner) {
        if (identityOwner && !src.contains(query)) return false;
        for (Item x : src) if (Objects.equals(query,x) && x.selected()) return true;
        return false;
    }
    static void verifySequence(Deque<Item> expected, Deque<Item> actual, String label) {
        check(expected.size() == actual.size(), label + " size");
        Iterator<Item> a = expected.iterator(), b = actual.iterator();
        while (a.hasNext()) check(a.next() == b.next(), label + " identity/order");
        check(!b.hasNext(), label + " end");
    }
    static void knownCases() {
        Item hidden = new Item(7, false), first = new Item(7, true), last = new Item(7, true);
        Item alias = new Item(7, true);
        LinkedList<Item> ordinary = new LinkedList<>(List.of(hidden, first, last));
        Deque<Item> view = M3FilteredDeques.filtered(ordinary, Item::selected);
        check(view.contains(alias), "ordinary equals alias visible");
        check(view.remove(alias), "ordinary first delete");
        check(ordinary.getFirst() == hidden && ordinary.getLast() == last, "hidden preserved first");
        check(view.removeLastOccurrence(alias), "ordinary last delete");
        check(ordinary.size() == 1 && ordinary.getFirst() == hidden, "hidden preserved last");
        check(!view.contains(alias), "ordinary nothing visible");
        IdentityDeque identity = new IdentityDeque();
        identity.add(hidden); identity.add(first); identity.add(last);
        Deque<Item> selected = M3FilteredDeques.filtered(identity, Item::selected);
        check(!selected.contains(alias), "identity alias absent");
        check(!selected.remove(alias), "identity alias cannot remove");
        check(!selected.removeLastOccurrence(alias), "identity alias cannot removeLast");
        check(identity.size() == 3, "identity no mutation on alias");
        check(selected.contains(first), "identity stored first visible");
        check(selected.remove(first), "identity stored first remove");
        check(identity.size() == 2 && identity.getFirst() == hidden && identity.getLast() == last,
                "identity retained exact members");
        check(selected.removeLastOccurrence(last), "identity stored last remove");
        check(identity.size() == 1 && identity.getFirst() == hidden, "identity hidden retained");
        check(!selected.remove(hidden), "identity hidden remove refused");
        LinkedList<Item> withNull = new LinkedList<>();
        withNull.add(null); withNull.add(hidden);
        Deque<Item> nullView = M3FilteredDeques.filtered(withNull, x -> x == null || x.selected());
        check(nullView.contains(null), "null containment");
        check(nullView.remove(null), "null remove");
        check(withNull.size()==1 && withNull.getFirst()==hidden, "null exact removal");
    }
    static void randomDifferential(boolean identityOwner) {
        Random random = new Random(identityOwner ? 0x12345ABCL : 0x5EED2026L);
        for (int trial = 0; trial < 12_000; trial++) {
            Deque<Item> expected = identityOwner ? new IdentityDeque() : new LinkedList<>();
            Deque<Item> actual = identityOwner ? new IdentityDeque() : new LinkedList<>();
            int length = random.nextInt(24);
            List<Item> members = new ArrayList<>();
            for (int i=0;i<length;i++) {
                Item element = new Item(random.nextInt(8), random.nextBoolean());
                expected.addLast(element); actual.addLast(element); members.add(element);
            }
            Deque<Item> filter = M3FilteredDeques.filtered(actual, Item::selected);
            Item probe = (identityOwner && !members.isEmpty() && random.nextBoolean())
                    ? members.get(random.nextInt(members.size()))
                    : new Item(random.nextInt(8), random.nextBoolean());
            check(filter.contains(probe) == expectedContains(expected,probe,identityOwner), "contains differential");
            boolean tail = random.nextBoolean();
            boolean expectedRemoved = expectedRemove(expected,probe,tail,identityOwner);
            boolean actualRemoved = tail ? filter.removeLastOccurrence(probe) : filter.remove(probe);
            check(expectedRemoved == actualRemoved, "remove differential");
            verifySequence(expected,actual, "sequence differential");
        }
    }
    public static void main(String[] args) {
        knownCases(); randomDifferential(false); randomDifferential(true);
        System.out.println("M3FilteredDequeOwnerMembershipContract PASS checks="+checks);
    }
}
