/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Behaviour (4) of qualification/filtered-views-owner-20261106: does a comparator that clears
 * the owner break M3TreeMap's ordering invariant? Each line is "case result"; the run is
 * deterministic and needs only the module classes (javac/java -cp target/classes).
 */
public final class FilteredViewsTreeMapRepro {
    private FilteredViewsTreeMapRepro() { }

    /** One-shot re-entrant comparator, the shape of the P2-098 self test's ReentrantComparator. */
    private static final class Reentrant implements Comparator<Integer> {
        private Runnable action;
        @Override public int compare(Integer left, Integer right) {
            Runnable selected = action;
            action = null;
            if (selected != null) selected.run();
            return Comparator.nullsFirst(Comparator.<Integer>naturalOrder()).compare(left, right);
        }
    }

    private static M3TreeMap<Integer, String> owner(Reentrant order) {
        M3TreeMap<Integer, String> owner = new M3TreeMap<>(order);
        for (int key = 1; key <= 3; key++) owner.put(key, "v" + key);
        return owner;
    }

    private static String verify(M3TreeMap<Integer, String> owner) {
        try {
            owner.verifyInvariants();
            return "valid";
        } catch (AssertionError failure) {
            return "AssertionError:" + failure.getMessage();
        }
    }

    private static String run(Runnable action) {
        try {
            action.run();
            return "returned";
        } catch (RuntimeException failure) {
            return failure.getClass().getSimpleName();
        }
    }

    /** The owner state after the comparator ran: size, keys and a JDK TreeMap differential. */
    private static String state(M3TreeMap<Integer, String> owner) {
        TreeMap<Integer, String> jdk = new TreeMap<>(owner);
        return "size=" + owner.size() + " keys=" + new ArrayList<>(owner.keySet()) + " jdkEqual=" + jdk.equals(owner);
    }

    public static void main(String[] args) {
        // R1: verifyInvariants itself calls the comparator; a pending clear runs inside its walk.
        Reentrant order = new Reentrant();
        M3TreeMap<Integer, String> owner = owner(order);
        order.action = owner::clear;
        System.out.println("R1 verify with pending comparator clear: " + verify(owner));
        System.out.println("R1 then: " + state(owner) + " verify=" + verify(owner));
        for (int key = 5; key <= 7; key++) owner.put(key, "v" + key);
        System.out.println("R1 reuse: " + state(owner) + " verify=" + verify(owner));
        // R2: a comparator clear consumed inside tree navigation (bounded first slot, lookup, insert).
        String[] names = {"subMap.firstKey", "get", "put"};
        for (int operation = 0; operation < names.length; operation++) {
            order = new Reentrant();
            M3TreeMap<Integer, String> target = owner(order);
            order.action = target::clear;
            int op = operation;
            String result = run(() -> {
                if (op == 0) target.subMap(0, true, 4, true).firstKey();
                else if (op == 1) target.get(2);
                else target.put(9, "v9");
            });
            System.out.println("R2 " + names[operation] + " -> " + result + "; " + state(target)
                    + " verify=" + verify(target));
        }
        // R3: the P2-098 sequence on the mutable filtered view: prepare, arm, next, verify.
        for (boolean values : new boolean[] {false, true}) {
            order = new Reentrant();
            M3TreeMap<Integer, String> target = owner(order);
            NavigableMap<Integer, String> filtered = M3MutableFilteredViews.filterEntries(
                    target.subMap(0, true, 4, true), (key, value) -> true);
            Iterator<?> cursor = values ? filtered.values().iterator() : filtered.entrySet().iterator();
            boolean ready = cursor.hasNext();
            Reentrant armed = order;
            armed.action = target::clear;
            String next = run(() -> {
                Object item = cursor.next();
                if (item instanceof Map.Entry<?, ?> entry) entry.getKey();
            });
            System.out.println("R3 values=" + values + " ready=" + ready + " next -> " + next
                    + " comparatorConsumedByNext=" + (armed.action == null) + " verify=" + verify(target)
                    + " verifyAgain=" + verify(target) + " " + state(target));
        }
        // R4 (found by the seeded differential, base and head alike): the comparator mutates the owner
        // inside the owner's own lookup-then-write operation, which then erases or writes a stale slot.
        String[] writes = {"remove", "keySet.remove", "set.remove", "remove(k,v)", "replace", "pollFirst(bounded)",
            "clear(bounded)", "remove(self)"};
        for (int operation = 0; operation < writes.length; operation++) {
            order = new Reentrant();
            M3TreeMap<Integer, String> target = owner(order);
            M3TreeSet<Integer> set = new M3TreeSet<>(order);
            set.addAll(java.util.List.of(1, 2, 3));
            int op = operation;
            order.action = op == 2 ? set::clear : op == 7 ? () -> target.remove(2) : target::clear;
            String result = run(() -> {
                switch (op) {
                    case 0 -> target.remove(2);
                    case 1 -> target.keySet().remove(2);
                    case 2 -> set.remove(2);
                    case 3 -> target.remove(2, "v2");
                    case 4 -> target.replace(2, "r2");
                    case 5 -> target.subMap(0, true, 4, true).pollFirstEntry();
                    case 6 -> target.subMap(0, true, 4, true).clear();
                    default -> target.remove(2);
                }
            });
            String verified;
            try {
                if (op == 2) set.verifyInvariants(); else target.verifyInvariants();
                verified = "valid";
            } catch (AssertionError failure) {
                verified = "AssertionError:" + failure.getMessage();
            }
            System.out.println("R4 " + writes[operation] + " with comparator "
                    + (op == 7 ? "remove(2)" : "clear") + " -> " + result + "; verify=" + verified);
        }
    }
}
