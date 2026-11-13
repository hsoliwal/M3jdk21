/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;

/**
 * Seeded before/after differential of qualification/filtered-views-owner-20261106: the same scenarios run on the
 * base and the head owners (M3FilteredViews, M3MutableFilteredViews) and every line is compared by classify.py.
 * A scenario picks an owner (M3TreeMap, JDK TreeMap, M3TreeSet, JDK TreeSet), a read-only or mutable view over an
 * optionally bounded and descending section, a cursor kind, an optional one-shot mutating predicate or comparator,
 * and 10 random steps (cursor hasNext/next/remove, owner nonstructural put, structural put/remove/clear, view
 * size/isEmpty/first). Each line: scenario, step, operation, result or exception, the predicate keys called and the
 * comparator call count since the previous line, and tags for the classifier (src, view, cursor, ext = structural
 * owner changes since the cursor opened, pm/cm = a predicate/comparator mutation happened since it opened).
 * usage: java -cp <classes>:<this> jdk.m3.collections.FilteredViewsOwnerDifferential <scenarios> <first seed>
 */
public final class FilteredViewsOwnerDifferential {
    private FilteredViewsOwnerDifferential() { }

    private static final Comparator<Integer> ORDER = Comparator.nullsFirst(Comparator.naturalOrder());

    /** Counting comparator with an optional one-shot action at a given call. */
    private static final class Counting implements Comparator<Integer> {
        int calls;
        int armedAt = -1;
        Runnable action;
        boolean fired;
        @Override public int compare(Integer left, Integer right) {
            calls++;
            if (calls == armedAt && action != null) {
                Runnable selected = action;
                action = null;
                fired = true;
                selected.run();
            }
            return ORDER.compare(left, right);
        }
    }

    private static final class Scenario {
        final Random random;
        final int id;
        final boolean map;
        final boolean m3;
        final boolean mutable;
        final Counting order = new Counting();
        final List<Integer> predicateCalls = new ArrayList<>();
        int predicateMutationAt = -1;
        int predicateCount;
        boolean predicateFired;
        NavigableMap<Integer, String> rootMap;
        NavigableSet<Integer> rootSet;
        Object view;
        Iterator<?> cursor;
        int cursorKind;
        int external;
        boolean pm;
        boolean cm;
        int lastCmp;
        int mutationKind;
        final StringBuilder out = new StringBuilder();

        Scenario(int id, long seed) {
            this.id = id;
            // Spread consecutive seeds first: java.util.Random's first draws of adjacent seeds are correlated
            // (unmixed, every M3 scenario drew a set and every JDK scenario a map).
            random = new Random(seed * 0x9E3779B97F4A7C15L + 0x632BE59BD9B4E019L);
            int source = random.nextInt(4);
            map = source < 2;
            m3 = (source & 1) == 0;
            mutable = random.nextBoolean();
        }
    }

    private static boolean visible(Integer key, int salt) {
        return key == null || Math.floorMod(key * 7 + salt, 3) != 0;
    }

    /** Owner mutation: 0 nonstructural put, 1 put new key, 2 remove a present key, 3 clear. */
    private static String mutate(Scenario s, int kind) {
        Integer fresh = 20 + s.random.nextInt(40);
        if (s.map) {
            Integer present = s.rootMap.isEmpty() ? fresh : s.rootMap.firstKey();
            switch (kind) {
                case 0 -> {
                    boolean empty = s.rootMap.isEmpty();
                    s.rootMap.put(present, "r" + s.random.nextInt(9));
                    return empty ? "put-new" : "put-existing"; // only a present key is a nonstructural write
                }
                case 1 -> { s.rootMap.put(fresh - 30, "n"); return "put-new"; }
                case 2 -> { s.rootMap.remove(present); return "remove"; }
                default -> { s.rootMap.clear(); return "clear"; }
            }
        }
        Integer present = s.rootSet.isEmpty() ? fresh : s.rootSet.first();
        switch (kind) {
            case 0, 1 -> { s.rootSet.add(fresh - 30); return "add-new"; }
            case 2 -> { s.rootSet.remove(present); return "remove"; }
            default -> { s.rootSet.clear(); return "clear"; }
        }
    }

    private static void build(Scenario s) {
        int salt = s.random.nextInt(3);
        boolean bounded = s.random.nextBoolean();
        boolean descending = s.random.nextBoolean();
        if (s.random.nextInt(6) == 0) s.predicateMutationAt = 1 + s.random.nextInt(6);
        int comparatorOffset = s.random.nextInt(6) == 0 ? 1 + s.random.nextInt(24) : -1;
        s.mutationKind = s.random.nextInt(4);
        List<Integer> keys = new ArrayList<>();
        for (int key = -6; key <= 12; key++) if (s.random.nextInt(3) != 0) keys.add(key);
        if (s.random.nextBoolean()) keys.add(null);
        if (s.map) {
            s.rootMap = s.m3 ? new M3TreeMap<>(s.order) : new TreeMap<>(s.order);
            for (Integer key : keys) s.rootMap.put(key, key == null ? null : "v" + key);
            NavigableMap<Integer, String> part = bounded ? s.rootMap.subMap(-3, true, 8, false) : s.rootMap;
            if (descending) part = part.descendingMap();
            s.view = s.mutable
                    ? M3MutableFilteredViews.filterEntries(part, (key, value) -> test(s, key, salt))
                    : M3FilteredViews.filterEntries(part, (key, value) -> test(s, key, salt));
        } else {
            s.rootSet = s.m3 ? new M3TreeSet<>(s.order) : new TreeSet<>(s.order);
            s.rootSet.addAll(keys);
            NavigableSet<Integer> part = bounded ? s.rootSet.subSet(-3, true, 8, false) : s.rootSet;
            if (descending) part = part.descendingSet();
            s.view = s.mutable ? M3MutableFilteredViews.filter(part, key -> test(s, key, salt))
                    : M3FilteredViews.filter(part, key -> test(s, key, salt));
        }
        // Arm relative to the calls the setup made, so the one-shot action fires while the cursor opens
        // (small offsets) or while it steps, never during the owner's own construction.
        if (comparatorOffset > 0) {
            s.order.armedAt = s.order.calls + comparatorOffset;
            int kind = s.mutationKind;
            s.order.action = () -> { s.cm = true; mutate(s, kind); };
        }
        s.cursorKind = s.random.nextInt(s.map ? 4 : 2);
        s.out.append("s").append(s.id).append(" setup src=").append(s.m3 ? "M3" : "JDK").append(s.map ? "Map" : "Set")
                .append(" view=").append(s.mutable ? "mutable" : "readonly").append(" bounded=").append(bounded)
                .append(" descending=").append(descending).append(" pmAt=").append(s.predicateMutationAt)
                .append(" cmAt=").append(s.order.armedAt).append(" mutation=").append(s.mutationKind).append('\n');
    }

    private static boolean test(Scenario s, Integer key, int salt) {
        s.predicateCalls.add(key);
        s.predicateCount++;
        if (s.predicateCount == s.predicateMutationAt) {
            s.pm = true;
            mutate(s, s.mutationKind);
        }
        return visible(key, salt);
    }

    @SuppressWarnings("unchecked")
    private static Iterator<?> open(Scenario s) {
        s.external = 0;
        s.pm = false;
        s.cm = false;
        if (s.map) {
            NavigableMap<Integer, String> map = (NavigableMap<Integer, String>) s.view;
            return switch (s.cursorKind) {
                case 0 -> map.entrySet().iterator();
                case 1 -> map.values().iterator();
                case 2 -> map.navigableKeySet().iterator();
                default -> map.descendingMap().entrySet().iterator();
            };
        }
        NavigableSet<Integer> set = (NavigableSet<Integer>) s.view;
        return s.cursorKind == 0 ? set.iterator() : set.descendingIterator();
    }

    private static String render(Object item) {
        return item instanceof Map.Entry<?, ?> entry ? "key:" + entry.getKey() : String.valueOf(item);
    }

    private static void line(Scenario s, int step, String op, Supplier<String> action) {
        String result;
        try {
            result = action.get();
        } catch (RuntimeException failure) {
            result = "!" + failure.getClass().getSimpleName();
        }
        record(s, step, op, result);
        if (result.startsWith("!") && op.startsWith("cursor.")) {
            try {
                s.cursor = open(s);
                s.out.append("s").append(s.id).append(" t").append(step).append(" reopen\n");
            } catch (RuntimeException failure) {
                // Opening can fail too (treemap-aba-guard-20261107: a bounded owner lookup fails fast when the
                // comparator changes the structure during it): record it like an operation; reopen lazily.
                s.cursor = null;
                record(s, step, "reopen", "!" + failure.getClass().getSimpleName());
            }
        }
    }

    /** The scenario's cursor; reopened first when its last (re)open failed. */
    private static Iterator<?> cursor(Scenario s) {
        if (s.cursor == null) s.cursor = open(s);
        return s.cursor;
    }

    private static void record(Scenario s, int step, String op, String result) {
        int cmp = s.order.calls - s.lastCmp;
        s.lastCmp = s.order.calls;
        s.out.append("s").append(s.id).append(" t").append(step).append(' ').append(op).append(" -> ").append(result)
                .append(" | pred=").append(s.predicateCalls).append(" cmp=").append(cmp)
                .append(" | src=").append(s.m3 ? "M3" : "JDK").append(" view=").append(s.mutable ? "mutable" : "readonly")
                .append(" cursor=").append(s.map ? new String[] {"entries", "values", "keys", "descEntries"}[s.cursorKind]
                        : new String[] {"iterator", "descIterator"}[s.cursorKind])
                .append(" ext=").append(s.external).append(" pm=").append(s.pm ? 1 : 0).append(" cm=").append(s.cm ? 1 : 0)
                .append('\n');
        s.predicateCalls.clear();
    }

    private static void step(Scenario s, int step) {
        int choice = s.random.nextInt(12);
        if (choice <= 3) line(s, step, "cursor.hasNext", () -> String.valueOf(cursor(s).hasNext()));
        else if (choice <= 6) line(s, step, "cursor.next", () -> render(cursor(s).next()));
        else if (choice == 7) line(s, step, "cursor.remove", () -> { cursor(s).remove(); return "removed"; });
        else if (choice == 8) {
            int kind = s.random.nextInt(4);
            line(s, step, "owner.mutate", () -> {
                String done = mutate(s, kind);
                if (!done.equals("put-existing")) s.external++;
                return done;
            });
        } else if (choice == 9) line(s, step, "view.size", () -> String.valueOf(size(s)));
        else if (choice == 10) line(s, step, "view.isEmpty", () -> String.valueOf(isEmpty(s)));
        else line(s, step, "view.first", () -> first(s));
    }

    private static int size(Scenario s) {
        return s.map ? ((Map<?, ?>) s.view).size() : ((NavigableSet<?>) s.view).size();
    }

    private static boolean isEmpty(Scenario s) {
        return s.map ? ((Map<?, ?>) s.view).isEmpty() : ((NavigableSet<?>) s.view).isEmpty();
    }

    private static String first(Scenario s) {
        return s.map ? String.valueOf(((NavigableMap<?, ?>) s.view).firstEntry() == null ? "none"
                : ((NavigableMap<?, ?>) s.view).firstEntry().getKey())
                : String.valueOf(((NavigableSet<?>) s.view).isEmpty() ? "none" : ((NavigableSet<?>) s.view).first());
    }

    private static String run(int id, long seed) {
        Scenario s = new Scenario(id, seed);
        build(s);
        line(s, 0, "open", () -> { s.cursor = open(s); return "opened"; });
        for (int step = 1; step <= 10; step++) step(s, step);
        // Disarm a comparator action that never fired, so verifyInvariants (which compares keys) reports the
        // tree itself rather than running the action inside its own walk (FilteredViewsTreeMapRepro R1).
        boolean pending = s.order.action != null;
        s.order.action = null;
        String verify;
        try {
            if (s.rootMap instanceof M3TreeMap<?, ?> owner) owner.verifyInvariants();
            if (s.rootSet instanceof M3TreeSet<?> owner) owner.verifyInvariants();
            verify = "valid";
        } catch (AssertionError failure) {
            verify = "AssertionError:" + failure.getMessage();
        }
        s.out.append("s").append(id).append(" end owner=").append(s.map ? s.rootMap.keySet() : s.rootSet)
                .append(" pendingComparator=").append(pending).append(" verify=").append(verify).append('\n');
        return s.out.toString();
    }

    public static void main(String[] args) {
        int scenarios = Integer.parseInt(args[0]);
        long first = Long.parseLong(args[1]);
        StringBuilder all = new StringBuilder();
        for (int id = 0; id < scenarios; id++) all.append(run(id, first + id));
        System.out.print(all);
    }
}
