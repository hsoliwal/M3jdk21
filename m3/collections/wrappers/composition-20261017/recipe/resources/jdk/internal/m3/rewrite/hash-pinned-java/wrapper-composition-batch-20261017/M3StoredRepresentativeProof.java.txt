/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Differential oracle for live filtered stored-representative membership/deletion.
 * Main-style test intentionally needs no second collection library or test dependency.
 */
public final class M3StoredRepresentativeProof {
    private static int checks;
    private M3StoredRepresentativeProof() {}

    private static final class Slot {
        final int id;
        final boolean admitted;
        Slot(int id, boolean admitted) { this.id = id; this.admitted = admitted; }
        @Override public boolean equals(Object other) {
            return other instanceof Slot slot && slot.id == id;
        }
        @Override public int hashCode() { return 1; } // Deliberate maximal hash collision.
        @Override public String toString() { return "Slot(" + id + "," + admitted + ")"; }
    }

    private static void equal(Object expected, Object actual, String reason) {
        checks++;
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(reason + " expected=" + expected + " actual=" + actual);
        }
    }
    private static void same(Object expected, Object actual, String reason) {
        checks++;
        if (expected != actual) throw new AssertionError(reason);
    }
    private static void truth(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError(reason);
    }
    private static boolean expectedVisible(Collection<Slot> source, Slot probe) {
        for (Slot value : source) if (probe.equals(value) && value.admitted) return true;
        return false;
    }
    private static boolean removeExpected(List<Slot> source, Slot probe) {
        for (int i = 0; i < source.size(); i++) {
            Slot actual = source.get(i);
            if (probe.equals(actual) && actual.admitted) {
                source.remove(i);
                return true;
            }
        }
        return false;
    }
    private static void identities(List<Slot> expected, List<Slot> actual, String reason) {
        equal(expected.size(), actual.size(), reason + " length");
        for (int i = 0; i < actual.size(); i++) same(expected.get(i), actual.get(i), reason + " #" + i);
    }
    private static void deterministic() {
        Slot hidden = new Slot(7, false);
        Slot visible = new Slot(7, true);
        Slot hiddenProbe = new Slot(7, false);
        Slot visibleProbe = new Slot(7, true);
        ArrayList<Slot> source = new ArrayList<>(Arrays.asList(hidden, visible));
        Collection<Slot> selected = M3CollectionViews.filtered(source, value -> value.admitted);
        truth(selected.contains(hiddenProbe), "search stored representative not external probe");
        truth(selected.remove(hiddenProbe), "remove stored visible occurrence");
        equal(1, source.size(), "hidden equal survivor length");
        same(hidden, source.get(0), "hidden equal survivor reference");
        truth(!selected.contains(visibleProbe), "hidden owner does not become visible with admissible probe");
        truth(!selected.remove(visibleProbe), "hidden owner survives admissible probe removal");
        same(hidden, source.get(0), "hidden owner identity unchanged");

        ArrayList<Slot> reversed = new ArrayList<>(Arrays.asList(visible, hidden));
        truth(M3CollectionViews.filtered(reversed, value -> value.admitted).remove(hiddenProbe),
                "remove visible earliest occurrence");
        equal(1, reversed.size(), "reverse order survivor length");
        same(hidden, reversed.get(0), "reverse order hidden survivor");
        CopyOnWriteArrayList<Slot> cow = new CopyOnWriteArrayList<>(Arrays.asList(hidden, visible));
        truth(M3CollectionViews.filtered(cow, value -> value.admitted).remove(hiddenProbe),
                "writable CopyOnWriteArrayList has nonwritable iterator");
        same(hidden, cow.get(0), "COW hidden sibling must survive");

        Set<Slot> concealed = new HashSet<>(); concealed.add(hidden);
        Set<Slot> filteredHidden = M3CollectionViews.filtered(concealed, value -> value.admitted);
        truth(!filteredHidden.contains(visibleProbe), "set probe cannot expose hidden key");
        truth(!filteredHidden.remove(visibleProbe), "set probe cannot remove hidden key");
        truth(concealed.contains(hidden), "set still contains hidden key");
        Set<Slot> shown = new HashSet<>(); shown.add(visible);
        Set<Slot> filteredShown = M3CollectionViews.filtered(shown, value -> value.admitted);
        truth(filteredShown.contains(hiddenProbe), "set probe must find visible key");
        truth(filteredShown.equals(Set.of(hiddenProbe)), "set equals uses visible stored representative");
        truth(filteredShown.remove(hiddenProbe), "set remove finds visible key");
        truth(shown.isEmpty(), "set physical owner empty after removal");

        NavigableSet<Slot> sorted = new TreeSet<>(java.util.Comparator.comparingInt(value -> value.id));
        sorted.add(hidden);
        NavigableSet<Slot> sortedView = M3CollectionViews.filtered(sorted, value -> value.admitted);
        truth(!sortedView.contains(visibleProbe), "sorted comparator stored representative unchanged");
        truth(!sortedView.remove(visibleProbe), "sorted hidden element not deleted");
        truth(sorted.contains(hiddenProbe), "sorted owner survives");
        sorted.clear(); sorted.add(visible);
        truth(sortedView.contains(hiddenProbe), "sorted visible despite concealed probe flag");
        truth(sortedView.remove(hiddenProbe), "sorted visible removal");
    }

    private static void randomDifferential() {
        Random random = new Random(20261017L);
        Predicate<Slot> predicate = slot -> slot.admitted;
        for (int trial = 0; trial < 1200; trial++) {
            int n = random.nextInt(32);
            ArrayList<Slot> stored = new ArrayList<>(), expected = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                Slot entry = new Slot(random.nextInt(9), random.nextBoolean());
                stored.add(entry); expected.add(entry);
            }
            Collection<Slot> filtered = M3CollectionViews.filtered(stored, predicate);
            for (int probeNumber = 0; probeNumber < 8; probeNumber++) {
                Slot probe = new Slot(random.nextInt(9), random.nextBoolean());
                boolean expectedContains = expectedVisible(expected, probe);
                equal(expectedContains, filtered.contains(probe), "random contains trial=" + trial);
                if ((probeNumber & 1) == 0) {
                    boolean expectedRemoved = removeExpected(expected, probe);
                    equal(expectedRemoved, filtered.remove(probe), "random remove trial=" + trial);
                    identities(expected, stored, "random backing trial=" + trial);
                }
            }
        }
    }

    private static void noEagerPredicate() {
        ArrayList<Slot> backing = new ArrayList<>(Arrays.asList(new Slot(1, false), new Slot(2, true)));
        int[] calls = {0};
        Collection<Slot> filtered = M3CollectionViews.filtered(backing, x -> {
            calls[0]++;
            if (x.id == 1) throw new AssertionError("unrelated predicate evaluated");
            return x.admitted;
        });
        truth(filtered.contains(new Slot(2, false)), "matched stored value");
        equal(1, calls[0], "predicate evaluated only on matching occurrence");
        truth(!filtered.contains(new Slot(3, true)), "absent probe");
        equal(1, calls[0], "absent probe requires no predicate");
    }

    public static void main(String[] args) {
        deterministic();
        randomDifferential();
        noEagerPredicate();
        System.out.println("PASS M3_STORED_REPRESENTATIVE checks=" + checks + " seeds=1200");
    }
}
