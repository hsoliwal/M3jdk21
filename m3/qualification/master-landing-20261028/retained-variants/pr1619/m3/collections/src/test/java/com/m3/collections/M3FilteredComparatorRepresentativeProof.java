/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

/** The predicate sees the stored representative, never a comparator-equivalent alias. */
public final class M3FilteredComparatorRepresentativeProof {
    private static int cases;
    private M3FilteredComparatorRepresentativeProof() { }

    private static void check(boolean value, String label) {
        if (!value) throw new AssertionError(label);
        cases++;
    }

    private static void refused(Runnable action) {
        try { action.run(); }
        catch (UnsupportedOperationException required) { cases++; return; }
        throw new AssertionError("mutated a read-only filtered collection");
    }

    public static void main(String[] args) {
        String stored = new String("Alpha");
        String alias = new String("alpha");
        TreeSet<String> base = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        base.add(stored);
        AtomicReference<String> visited = new AtomicReference<>();
        Set<String> set = M3FilteredViews.filter((Set<String>) base, s -> {
            visited.set(s);
            return s == stored || s.equals("Beta");
        });
        check(base.contains(alias), "backing comparator membership");
        check(set.contains(alias), "generic Set must retain comparator membership");
        check(visited.get() == stored, "predicate received alias rather than stored value");
        check(set.iterator().next() == stored, "logical owner reference changed");
        refused(() -> set.remove("absent"));
        refused(set::clear);

        Set<String> nested = M3FilteredViews.filter(set, s -> s.length() == 5);
        check(nested.contains("ALPHA"), "nested general Set lost comparator equivalence");
        check(nested.size() == 1, "nested size");
        Collection<String> collection = M3FilteredViews.filter((Collection<String>) base,
                s -> s.equals("Alpha"));
        check(collection.contains("ALPHA"), "Collection overload lost comparator equivalence");
        Collection<String> nestedCollection = M3FilteredViews.filter(collection, s -> s.length() == 5);
        check(nestedCollection.contains("alpha"), "nested Collection lost stored representative");

        SortedSet<String> sorted = M3FilteredViews.filter((SortedSet<String>) base, s -> s == stored);
        check(sorted.contains("aLpHa"), "SortedSet comparator membership");
        NavigableSet<String> nav = M3FilteredViews.filter((NavigableSet<String>) base, s -> s == stored);
        check(nav.contains("ALPHA"), "NavigableSet comparator membership");
        Set<String> descending = M3FilteredViews.filter((Set<String>) base.descendingSet(), s -> s == stored);
        check(descending.contains("ALPHA"), "descending backing comparator membership");

        TreeSet<String> nullable = new TreeSet<>(Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER));
        nullable.add(null);
        nullable.add(stored);
        Set<String> nullSet = M3FilteredViews.filter((Set<String>) nullable, s -> true);
        check(nullSet.contains(null), "comparator-admitted null representative");
        check(nullSet.contains("ALPHA"), "nullable sorted comparator alias");
        check(!M3FilteredViews.filter((Set<String>) nullable, s -> s == null).contains("ALPHA"),
                "hidden sorted member visible");

        Set<String> identity = Collections.newSetFromMap(new IdentityHashMap<>());
        String twin = new String("Twin");
        identity.add(twin);
        Set<String> identityFiltered = M3FilteredViews.filter(identity, s -> true);
        check(identityFiltered.contains(twin), "identity representative");
        check(!identityFiltered.contains(new String("Twin")), "identity semantics changed");
        Set<String> regular = new HashSet<>();
        regular.add(twin);
        check(M3FilteredViews.filter(regular, s -> true).contains(new String("Twin")),
                "hash-set equals semantics changed");

        base.add(new String("Beta"));
        check(set.contains("beta"), "backed view did not reflect source mutation");
        check(!nested.contains("beta"), "nested predicate was ignored");
        for (int i = 0; i < 512; i++) {
            String name = "Key" + i;
            TreeSet<String> owner = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            String canonical = new String(name);
            owner.add(canonical);
            Set<String> view = M3FilteredViews.filter((Set<String>) owner, s -> s == canonical);
            check(view.contains(name.toLowerCase(java.util.Locale.ROOT)), "seeded comparator alias " + i);
        }
        System.out.println("M3 backed filtered Set comparator representative checks=" + cases);
    }
}
