/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Re-entrant lookup contract of M3TreeMap and M3TreeSet (qualification/treemap-aba-guard-20261107).
 * Every operation shape below locates a slot through the user comparator. For each comparator
 * call N of the operation, a one-shot comparator action clears the owner and inserts 50 and 60
 * at call N, so the slots the lookup may still hold are released and reused by other keys (the
 * slot-reuse ABA of the #2641 review: remove(2) erased 60 and returned "v60"). The outcome must
 * be either fail-fast (ConcurrentModificationException, the owner holding exactly the action's
 * content) or exactly the outcome of running the action first and the operation second; the AVL
 * invariants hold either way. Modes: {@code all} (default) asserts, {@code report} prints every
 * case with its verdict and never fails.
 */
public final class M3TreeMapReentrantLookupSelfTest {
    private static final String CME = "!ConcurrentModificationException";
    private static final String[] VARIANTS = {"root", "sub", "descSub"};

    private M3TreeMapReentrantLookupSelfTest() { }

    /** Counting comparator with a one-shot action at a chosen call. */
    private static final class Reentrant implements Comparator<Integer> {
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
            return Integer.compare(left, right);
        }
    }

    /** One owner (keys 1..7, key 2 in slot 1 below the root 4) and the view an operation runs on. */
    private static final class Fixture {
        final M3TreeMap<Integer, String> map;
        final M3TreeSet<Integer> set;
        final NavigableMap<Integer, String> mapView;
        final NavigableSet<Integer> setView;
        Map.Entry<Integer, String> held;

        Fixture(int kind, int variant, Reentrant order) {
            if (kind == SET) {
                map = null;
                mapView = null;
                set = new M3TreeSet<>(order);
                for (int key = 1; key <= 7; key++) set.add(key);
                NavigableSet<Integer> view = variant == 0 ? set : set.subSet(0, true, 100, true);
                setView = variant == 2 ? view.descendingSet() : view;
            } else {
                set = null;
                setView = null;
                map = kind == LAZY ? new M3TreeMap<>(order, (key, progress) -> "l" + key) : new M3TreeMap<>(order);
                for (int key = 1; key <= 7; key++) map.put(key, "v" + key);
                NavigableMap<Integer, String> view = variant == 0 ? map : map.subMap(0, true, 100, true);
                mapView = variant == 2 ? view.descendingMap() : view;
            }
        }

        /** The comparator action: release every slot and reuse two of them for other keys. */
        void reuse() {
            if (map != null) {
                map.clear();
                map.put(50, "v50");
                map.put(60, "v60");
            } else {
                set.clear();
                set.add(50);
                set.add(60);
            }
        }

        String content() { return map != null ? map.toString() : set.toString(); }

        String verify() {
            try {
                if (map != null) map.verifyInvariants(); else set.verifyInvariants();
                return "valid";
            } catch (AssertionError failure) {
                return "AssertionError:" + failure.getMessage();
            }
        }
    }

    private static final int MAP = 0;
    private static final int SET = 1;
    private static final int LAZY = 2;

    private record Op(String name, int kind, Consumer<Fixture> prepare, Function<Fixture, Object> body) { }

    private record Outcome(String result, String content, String verify, boolean fired, int calls) {
        boolean sameAs(Outcome other) { return result.equals(other.result) && content.equals(other.content); }
    }

    private static Op map(String name, Function<NavigableMap<Integer, String>, Object> body) {
        return new Op(name, MAP, fixture -> { }, fixture -> body.apply(fixture.mapView));
    }

    /** A lazy-value owner (provider loads "l" + key); its views are M3TreeMap instances. */
    private static Op lazy(String name, Function<M3TreeMap<Integer, String>, Object> body) {
        return new Op(name, LAZY, fixture -> { }, fixture -> body.apply((M3TreeMap<Integer, String>) fixture.mapView));
    }

    private static Op set(String name, Function<NavigableSet<Integer>, Object> body) {
        return new Op(name, SET, fixture -> { }, fixture -> body.apply(fixture.setView));
    }

    private static List<Integer> keys(Iterable<Integer> source) {
        List<Integer> out = new ArrayList<>();
        for (Integer key : source) out.add(key);
        return out;
    }

    private static List<String> values(Iterable<String> source) {
        List<String> out = new ArrayList<>();
        for (String value : source) out.add(value);
        return out;
    }

    private static List<Op> ops() {
        List<Op> ops = new ArrayList<>(List.of(
                map("remove", view -> view.remove(2)),
                map("remove(k,v)", view -> view.remove(2, "v60")),
                map("keySet.remove", view -> view.keySet().remove(2)),
                map("entrySet.remove", view -> view.entrySet().remove(Map.entry(2, "v60"))),
                map("entrySet.contains", view -> view.entrySet().contains(Map.entry(2, "v60"))),
                map("replace", view -> view.replace(2, "r")),
                map("replace(k,old,new)", view -> view.replace(2, "v60", "r")),
                map("putIfAbsent", view -> view.putIfAbsent(2, "p")),
                map("computeIfAbsent", view -> view.computeIfAbsent(2, key -> "c")),
                map("computeIfPresent", view -> view.computeIfPresent(2, (key, old) -> old + "!")),
                map("compute(remove)", view -> view.compute(2, (key, old) -> null)),
                map("compute(write)", view -> view.compute(2, (key, old) -> old == null ? "c" : old + "!")),
                map("merge(remove)", view -> view.merge(2, "m", (old, given) -> null)),
                map("get", view -> view.get(2)),
                map("getOrDefault", view -> view.getOrDefault(2, "d")),
                map("containsKey", view -> view.containsKey(2)),
                map("put", view -> view.put(2, "p")),
                map("remove(6)", view -> view.remove(6)),
                map("get(6)", view -> view.get(6)),
                map("put(6)", view -> view.put(6, "p")),
                map("ceilingEntry", view -> view.ceilingEntry(2)),
                map("floorKey", view -> view.floorKey(2)),
                map("floorKey(6)", view -> view.floorKey(6)),
                map("higherKey", view -> view.higherKey(2)),
                map("firstEntry", view -> view.firstEntry()),
                map("lastKey", view -> view.lastKey()),
                map("pollFirstEntry", view -> view.pollFirstEntry()),
                map("pollLastEntry", view -> view.pollLastEntry()),
                map("size", view -> view.size()),
                map("isEmpty", view -> view.isEmpty()),
                map("clear", view -> { view.clear(); return "cleared"; }),
                map("keys", view -> keys(view.keySet())),
                map("values", view -> values(view.values())),
                set("set.remove", view -> view.remove(2)),
                set("set.remove(6)", view -> view.remove(6)),
                set("set.contains", view -> view.contains(2)),
                set("set.add", view -> view.add(2)),
                set("set.ceiling", view -> view.ceiling(2)),
                set("set.lower", view -> view.lower(3)),
                set("set.floor(6)", view -> view.floor(6)),
                set("set.first", view -> view.first()),
                set("set.pollFirst", view -> view.pollFirst()),
                set("set.pollLast", view -> view.pollLast()),
                set("set.size", view -> view.size()),
                set("set.clear", view -> { view.clear(); return "cleared"; }),
                set("set.keys", view -> keys(view)),
                set("mutableFiltered.iterator",
                        view -> drain(MutableFilteredViews.filter(view, key -> key % 2 == 1).iterator())),
                set("mutableFiltered.descendingIterator",
                        view -> drain(MutableFilteredViews.filter(view, key -> key % 2 == 1).descendingIterator())),
                lazy("defer", view -> view.defer(2)),
                lazy("defer(6)", view -> view.defer(6)),
                lazy("touch", view -> view.touch(2)),
                lazy("valueFlags", view -> view.valueFlags(2))));
        ops.add(new Op("entry.setValue", MAP, fixture -> {
            for (Map.Entry<Integer, String> entry : fixture.mapView.entrySet()) {
                if (entry.getKey() == 2) fixture.held = entry;
            }
        }, fixture -> fixture.held.setValue("s")));
        return ops;
    }

    /** Runs one case; armedAt counts the operation's own comparator calls (0 = never fire). */
    private static Outcome attempt(Op op, int variant, int armedAt, boolean actionFirst) {
        Reentrant order = new Reentrant();
        Fixture fixture = new Fixture(op.kind(), variant, order);
        op.prepare().accept(fixture);
        if (actionFirst) fixture.reuse();
        int before = order.calls;
        if (armedAt > 0) {
            order.armedAt = before + armedAt;
            order.action = fixture::reuse;
        }
        String result;
        int calls;
        try {
            Object value = op.body().apply(fixture);
            calls = order.calls - before;
            result = String.valueOf(value);
        } catch (RuntimeException failure) {
            calls = order.calls - before;
            result = "!" + failure.getClass().getSimpleName();
        }
        boolean fired = order.fired;
        order.action = null;
        return new Outcome(result, fixture.content(), fixture.verify(), fired, calls);
    }

    private static String verdict(Outcome outcome, Outcome plain, Outcome reference, String reused) {
        if (!outcome.verify().equals("valid")) return "DAMAGED";
        if (!outcome.fired()) return outcome.sameAs(plain) ? "PASS" : "WRONG";
        if (outcome.result().equals(CME)) return outcome.content().equals(reused) ? "PASS" : "WRONG";
        if (outcome.result().endsWith(CME)) {
            // A drained cursor that failed fast: what it returned before must be a prefix of the plain walk.
            String partial = outcome.result().substring(0, outcome.result().length() - CME.length() - 1);
            return plain.result().startsWith(partial) && outcome.content().equals(reused) ? "PASS" : "WRONG";
        }
        return outcome.sameAs(reference) ? "PASS" : "WRONG";
    }

    /** Every element a cursor returns, in order; a failure ends the list with "!" and its class. */
    private static String drain(java.util.Iterator<?> cursor) {
        List<Object> seen = new ArrayList<>();
        try {
            while (cursor.hasNext()) seen.add(cursor.next());
            return seen.toString();
        } catch (RuntimeException failure) {
            return seen + "!" + failure.getClass().getSimpleName();
        }
    }

    /**
     * Cursor creation over a bounded view never fails for a structural change (the iterator throws
     * at its first call), but a ConcurrentModificationException the comparator throws by itself,
     * without changing the structure, propagates from creation with its identity.
     */
    private static String creation(boolean structural) {
        Reentrant order = new Reentrant();
        Fixture fixture = new Fixture(MAP, 1, order);
        RuntimeException own = new java.util.ConcurrentModificationException("comparator's own");
        order.armedAt = order.calls + 1;
        order.action = structural ? fixture::reuse : () -> { throw own; };
        java.util.Iterator<Map.Entry<Integer, String>> cursor;
        try {
            cursor = fixture.mapView.entrySet().iterator();
        } catch (RuntimeException failure) {
            order.action = null;
            return structural ? "WRONG creation threw " + failure : failure == own ? "PASS" : "WRONG identity " + failure;
        }
        order.action = null;
        if (!structural) return "WRONG creation completed";
        try {
            cursor.hasNext();
            return "WRONG first call completed";
        } catch (java.util.ConcurrentModificationException expected) {
            return fixture.content().equals("{50=v50, 60=v60}") && fixture.verify().equals("valid") ? "PASS"
                    : "WRONG state " + fixture.content();
        }
    }

    public static void main(String[] args) {
        boolean report = args.length > 0 && args[0].equals("report");
        List<String> failures = new ArrayList<>();
        int cases = 0;
        for (boolean structural : new boolean[] {true, false}) {
            String name = structural ? "creation(structural)" : "creation(comparator's own CME)";
            String verdict = creation(structural);
            String row = "sub\t" + name + "\t1\t" + (verdict.equals("PASS") ? "PASS" : "WRONG") + "\t" + verdict;
            cases++;
            if (!verdict.equals("PASS")) failures.add(row);
            if (report) System.out.println(row);
        }
        for (Op op : ops()) {
            for (int variant = 0; variant < VARIANTS.length; variant++) {
                Outcome plain = attempt(op, variant, 0, false);
                Outcome reference = attempt(op, variant, 0, true);
                String reused = attempt(new Op("none", op.kind(), fixture -> { }, fixture -> "none"),
                        variant, 0, true).content();
                for (int call = 1; call <= plain.calls(); call++) {
                    Outcome outcome = attempt(op, variant, call, false);
                    String verdict = verdict(outcome, plain, reference, reused);
                    String row = VARIANTS[variant] + "\t" + op.name() + "\t" + call + "\t" + verdict + "\t"
                            + outcome.result() + "\t" + outcome.content() + "\t" + outcome.verify()
                            + "\treference=" + reference.result() + " " + reference.content();
                    cases++;
                    if (!verdict.equals("PASS")) failures.add(row);
                    if (report) System.out.println(row);
                }
            }
        }
        System.out.println("M3TreeMapReentrantLookupSelfTest cases=" + cases + " failures=" + failures.size());
        if (!report && !failures.isEmpty()) {
            throw new AssertionError(failures.size() + " re-entrant lookup cases failed, first: " + failures.get(0));
        }
    }
}
