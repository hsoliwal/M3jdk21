/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Standalone Java21 before/after oracle for stored-reference and comparator-key
 * semantics. No arbitrary Java predicate ever crosses JNI.
 */
public final class M3StoredRepresentativeContract {
    private static int checks;
    private M3StoredRepresentativeContract() { }

    private static void yes(boolean truth, String reason) {
        checks++;
        if (!truth) throw new AssertionError(reason);
    }
    private static void no(boolean truth, String reason) {
        yes(!truth, reason);
    }
    private static void eq(Object expected, Object actual, String reason) {
        yes(Objects.equals(expected, actual),
                reason + ": expected=" + expected + " actual=" + actual);
    }
    private static void refuses(Class<? extends Throwable> exception,
            Runnable action, String reason) {
        checks++;
        try {
            action.run();
        } catch (Throwable failed) {
            if (exception.isInstance(failed)) return;
            throw new AssertionError(reason + ": wrong exception " + failed, failed);
        }
        throw new AssertionError(reason + ": must reject");
    }
    private static boolean lowercase(String value) {
        return value == null || value.equals(value.toLowerCase(java.util.Locale.ROOT));
    }

    /** Expected old-source defects; this mode must NOT be treated as an acceptance pass. */
    private static void verifyOldSourceIsIncorrect() {
        NavigableSet<String> ownerSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        ownerSet.add("ADMIN");
        NavigableSet<String> oldSet = M3FilteredViews.filter(ownerSet,
                M3StoredRepresentativeContract::lowercase);
        yes(oldSet.contains("admin"), "baseline exposes hidden comparator-equal element");

        NavigableMap<String, Integer> ownerMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        ownerMap.put("ADMIN", 1);
        NavigableMap<String, Integer> oldMap = M3FilteredViews.filterEntries(ownerMap,
                (key, value) -> lowercase(key));
        yes(oldMap.containsKey("admin"), "baseline tests alias rather than stored key");
        System.out.println("M3_STORED_REPRESENTATIVE_BASELINE_DEFECT_CONFIRMED");
    }

    private static void readOnlySet() {
        NavigableSet<String> source = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        source.add("ADMIN");
        source.add("user");
        NavigableSet<String> view = M3FilteredViews.filter(source,
                M3StoredRepresentativeContract::lowercase);
        SortedSet<String> sorted = M3FilteredViews.filter((SortedSet<String>) source,
                M3StoredRepresentativeContract::lowercase);
        no(view.contains("admin"), "navigable hidden alias must be invisible");
        no(view.contains("ADMIN"), "navigable hidden own reference");
        no(sorted.contains("admin"), "sorted hidden comparator alias");
        yes(view.contains("USER"), "navigable must query real lowercase owner");
        yes(sorted.contains("USER"), "sorted must query real lowercase owner");
        eq(List.of("user"), new ArrayList<>(view), "visible iteration");
        refuses(UnsupportedOperationException.class,
                () -> view.remove("user"), "read-only removal refuses");
        source.remove("ADMIN");
        source.add("admin");
        yes(view.contains("ADMIN"), "live update and opposite-case alias");
        yes(sorted.contains("ADMIN"), "sorted live update and opposite-case alias");
        eq(List.of("admin", "user"), new ArrayList<>(view), "live ordered source");
        NavigableSet<String> tail = view.tailSet("admin", true);
        yes(tail.contains("ADMIN"), "range lookup real representative");
        yes(view.descendingSet().contains("ADMIN"), "descending lookup representative");
    }

    private static void readOnlyMap() {
        NavigableMap<String, Integer> source = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        source.put("ADMIN", 7);
        source.put("user", 11);
        NavigableMap<String, Integer> view = M3FilteredViews.filterEntries(source,
                (key, value) -> lowercase(key));
        SortedMap<String, Integer> sorted = M3FilteredViews.filterEntries(
                (SortedMap<String, Integer>) source, (key, value) -> lowercase(key));
        no(view.containsKey("admin"), "navigable hidden map query");
        no(sorted.containsKey("admin"), "sorted hidden map query");
        eq(null, view.get("admin"), "navigable hidden get");
        eq(null, sorted.get("admin"), "sorted hidden get");
        yes(view.containsKey("USER"), "navigable stored valid aliased key");
        yes(sorted.containsKey("USER"), "sorted stored valid aliased key");
        eq(11, view.get("USER"), "navigable stored value");
        eq(11, sorted.get("USER"), "sorted stored value");
        no(view.navigableKeySet().contains("admin"), "keySet must not expose hidden row");
        eq(List.of("user"), new ArrayList<>(view.navigableKeySet()), "matching keys");
        source.clear();
        source.put("admin", 3);
        yes(view.containsKey("ADMIN"), "navigable real key is lowercase");
        yes(sorted.containsKey("ADMIN"), "sorted real key is lowercase");
        eq(3, view.get("ADMIN"), "navigable read through comparator alias");
        eq(3, sorted.get("ADMIN"), "sorted read through comparator alias");
        yes(view.descendingMap().containsKey("ADMIN"), "descending representative lookup");
        yes(view.headMap("z", true).containsKey("ADMIN"), "bounded representative lookup");
    }

    private static void mutableSet() {
        NavigableSet<String> source = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        source.add("ADMIN");
        NavigableSet<String> view = M3MutableFilteredViews.filter(source,
                M3StoredRepresentativeContract::lowercase);
        no(view.contains("admin"), "mutable hidden stored element");
        no(view.remove("admin"), "remove must not destroy hidden comparator alias");
        yes(source.contains("ADMIN"), "raw hidden member remains");
        refuses(IllegalArgumentException.class, () -> view.add("ADMIN"),
                "reject invalid stored additions");
        source.clear();
        source.add("admin");
        yes(view.contains("ADMIN"), "mutable visible stored representative");
        yes(view.remove("ADMIN"), "alias may remove actual visible representative");
        yes(source.isEmpty(), "backed remove writes through");
    }

    private static void mutableMap() {
        NavigableMap<String, Integer> source = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        source.put("ADMIN", 1);
        NavigableMap<String, Integer> view = M3MutableFilteredViews.filterEntries(
                source, (key, value) -> lowercase(key) && value >= 0);
        no(view.containsKey("admin"), "mutable hidden map alias");
        eq(null, view.get("admin"), "mutable hidden get");
        eq(null, view.remove("admin"), "mutable hidden remove");
        eq(1, source.get("ADMIN"), "hidden row preserved");
        refuses(IllegalArgumentException.class, () -> view.put("admin", 2),
                "put cannot mutate hidden uppercase representative through accepted lowercase alias");
        eq(1, source.get("ADMIN"), "refused put leaves source unchanged");
        LinkedHashMap<String, Integer> one = new LinkedHashMap<>();
        one.put("admin", 2);
        refuses(IllegalArgumentException.class, () -> view.putAll(one),
                "bulk write through stored hidden representative must reject");
        eq(1, source.get("ADMIN"), "refused bulk write is atomic");
        source.clear();
        source.put("admin", 2);
        eq(2, view.put("ADMIN", 3), "put must validate true stored lowercase key");
        eq(3, source.get("admin"), "stored row updated");
        one.clear();
        one.put("ADMIN", 4);
        view.putAll(one);
        eq(4, source.get("admin"), "bulk source representative accepts uppercase alias");
        yes(view.containsKey("ADMIN"), "visible after bulk update");
        eq(4, view.get("ADMIN"), "lookup after bulk update");
        eq(1, view.entrySet().size(), "no new alias entry created");
    }

    private static void collidingNewBulkKeys() {
        NavigableMap<String, Integer> source = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        NavigableMap<String, Integer> view = M3MutableFilteredViews.filterEntries(source,
                (key, value) -> "ADMIN".equals(key) ? value < 0
                        : "admin".equals(key) && value >= 0);
        LinkedHashMap<String, Integer> distinctAliases = new LinkedHashMap<>();
        distinctAliases.put("ADMIN", -1);
        distinctAliases.put("admin", 2);
        refuses(IllegalArgumentException.class, () -> view.putAll(distinctAliases),
                "bulk preflight must inspect effective first inserted key");
        yes(source.isEmpty(), "invalid comparator-equivalent batch must not partially commit");

        NavigableMap<String, Integer> another = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        another.put("admin", 1);
        NavigableMap<String, Integer> visible = M3MutableFilteredViews.filterEntries(
                another, (key, value) -> lowercase(key) && value >= 0);
        distinctAliases.clear();
        distinctAliases.put("ADMIN", 2);
        distinctAliases.put("admin", 3);
        visible.putAll(distinctAliases);
        eq(3, another.get("admin"), "preexisting true stored key governs alias batch");
        eq(1, another.size(), "one comparator-equivalent entry");
    }

    private record Key(String id, boolean keep) {
        @Override public boolean equals(Object other) {
            return other instanceof Key k && id.equals(k.id);
        }
        @Override public int hashCode() { return id.hashCode(); }
    }

    private static void identityAndDuplicateMembers() {
        Key hidden = new Key("same", false);
        Key visible = new Key("same", true);
        Set<Key> identity = Collections.newSetFromMap(new IdentityHashMap<>());
        identity.add(hidden);
        identity.add(visible);
        Set<Key> filtered = M3FilteredViews.filter(identity, Key::keep);
        no(filtered.contains(hidden), "identity-owned hidden equals-equal reference");
        yes(filtered.contains(visible), "identity-owned visible equals-equal reference");
        no(filtered.contains(new Key("same", true)), "identity source excludes foreign instance");

        Collection<Key> duplicates = new ArrayList<>(Arrays.asList(hidden, visible));
        Collection<Key> projected = M3FilteredViews.filter(duplicates, Key::keep);
        yes(projected.contains(new Key("same", false)),
                "Collection contains if any stored equals member is visible");
        duplicates.remove(visible);
        no(projected.contains(new Key("same", true)), "last visible equals member removed");
    }

    private static void nullRepresentatives() {
        Comparator<String> keys = Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER);
        NavigableSet<String> set = new TreeSet<>(keys);
        set.add(null);
        set.add("ADMIN");
        NavigableSet<String> view = M3FilteredViews.filter(set, Objects::isNull);
        yes(view.contains(null), "null stored set reference");
        no(view.contains("admin"), "non-null hidden comparator alias");

        NavigableMap<String, Integer> map = new TreeMap<>(keys);
        map.put(null, 1);
        map.put("ADMIN", 2);
        NavigableMap<String, Integer> visible = M3MutableFilteredViews.filterEntries(
                map, (key, value) -> key == null);
        yes(visible.containsKey(null), "null real stored map key");
        eq(1, visible.get(null), "null map value lookup");
        no(visible.containsKey("admin"), "non-null hidden map alias");
        eq(null, visible.remove("admin"), "non-null hidden map remains");
        eq(2, map.get("ADMIN"), "hidden map entry intact");
    }

    public static void main(String[] args) {
        if (args.length == 1 && "--baseline-red".equals(args[0])) {
            verifyOldSourceIsIncorrect();
            return;
        }
        readOnlySet();
        readOnlyMap();
        mutableSet();
        mutableMap();
        collidingNewBulkKeys();
        identityAndDuplicateMembers();
        nullRepresentatives();
        System.out.println("M3_STORED_REPRESENTATIVE_CONTRACT_PASS assertions=" + checks);
    }
}
