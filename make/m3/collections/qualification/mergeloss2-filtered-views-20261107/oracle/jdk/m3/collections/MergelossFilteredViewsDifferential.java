/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Seeded before/after differential of qualification/mergeloss2-filtered-views-20261107: the same scenarios run on
 * the base and the head classes of M3FilteredViews and M3MutableFilteredViews and classify.py compares every line.
 * A scenario picks an owner (JDK TreeSet, M3TreeSet, JDK TreeMap, M3TreeMap) over case-insensitive (optionally
 * reversed) String keys whose comparator counts its calls, a section (whole, sub, head, tail, descending), a chain of
 * 1-4 read-only filter layers through the Collection, Set, SortedSet and NavigableSet overloads (every 40th scenario
 * 60000 NavigableSet layers), or a mutable view, and 12 random operations: iteration, descendingIterator, size,
 * isEmpty, first, last, lower/floor/ceiling/higher, contains with an exact, a comparator-alias or an absent probe,
 * map get/containsKey/entry navigation, mutable put/remove/poll. Each line: scenario, step, operation and tags (view
 * shape, probe kind, deep), the result or exception type, the predicate tape (stored-object ids) and the comparator
 * call count of the operation.
 * usage: java -cp <classes>:<this> jdk.m3.collections.MergelossFilteredViewsDifferential <scenarios> <seed>
 */
public final class MergelossFilteredViewsDifferential {
    private MergelossFilteredViewsDifferential() { }

    private static final String[] WORDS = {"alpha", "Beta", "GAMMA", "delta", "Epsilon", "zeta", "Eta", "theta",
        "iota", "Kappa", "lambda", "MU"};
    private static final int DEEP = 60_000;

    private static final class Counting implements Comparator<String> {
        int calls;
        final boolean reversed;
        Counting(boolean reversed) { this.reversed = reversed; }
        @Override public int compare(String left, String right) {
            calls++;
            int order = String.CASE_INSENSITIVE_ORDER.compare(left, right);
            return reversed ? -order : order;
        }
    }

    private static final class Run {
        final Random random;
        final Counting order;
        final Map<String, Integer> ids = new IdentityHashMap<>();
        final List<String> tape = new ArrayList<>();
        final StringBuilder out;
        final int scenario;
        int step;
        Run(int scenario, long seed, StringBuilder out) {
            this.scenario = scenario;
            this.random = new Random(seed * 0x9E3779B97F4A7C15L + scenario);
            this.order = new Counting(random.nextInt(4) == 0);
            this.out = out;
        }
        String id(Object value) {
            if (!(value instanceof String text)) return String.valueOf(value);
            Integer known = ids.get(text);
            return known == null ? "new:" + text : "#" + known + ":" + text;
        }
        Predicate<String> predicate(int layer) {
            int salt = random.nextInt(5);
            return value -> {
                tape.add(layer + "/" + id(value));
                return value == null || ((value.length() + layer + salt) % 3) != 0;
            };
        }
        void line(String op, String tags, Supplier<Object> action) {
            tape.clear();
            int before = order.calls;
            String result;
            try {
                result = render(action.get());
            } catch (StackOverflowError deep) {
                result = "EXC:StackOverflowError";
            } catch (RuntimeException failure) {
                result = "EXC:" + failure.getClass().getSimpleName();
            }
            String shown = tape.size() > 24 ? tape.subList(0, 24) + "...(" + tape.size() + ")" : tape.toString();
            out.append("s=").append(scenario).append(" t=").append(step++).append(' ').append(op).append(' ')
               .append(tags).append(" -> ").append(result).append(" pred=").append(shown)
               .append(" cmp=").append(order.calls - before).append('\n');
        }
        String render(Object value) {
            if (value instanceof Iterator<?> cursor) {
                List<String> items = new ArrayList<>();
                while (cursor.hasNext() && items.size() < 64) items.add(id(cursor.next()));
                return items.toString();
            }
            if (value instanceof Map.Entry<?, ?> entry) return id(entry.getKey()) + "=" + entry.getValue();
            return id(value);
        }
    }

    private static String probe(Run run, String[] kind) {
        String word = WORDS[run.random.nextInt(WORDS.length)];
        switch (run.random.nextInt(3)) {
            case 0 -> { kind[0] = "exact"; return word; }
            case 1 -> { kind[0] = "alias"; return run.random.nextBoolean() ? word.toUpperCase() : word.toLowerCase(); }
            default -> { kind[0] = "absent"; return word + "x"; }
        }
    }

    private static NavigableSet<String> section(Run run, NavigableSet<String> set, int shape) {
        boolean reversed = run.order.reversed;
        String low = reversed ? "s" : "b";
        String high = reversed ? "b" : "s";
        return switch (shape) {
            case 0 -> set;
            case 1 -> set.subSet(low, true, high, false);
            case 2 -> set.headSet(high, false);
            case 3 -> set.tailSet(low, true);
            default -> set.descendingSet();
        };
    }

    private static void scenario(int id, long seed, StringBuilder out) {
        Run run = new Run(id, seed, out);
        int owner = run.random.nextInt(4);
        NavigableSet<String> keys;
        NavigableMap<String, Integer> map = null;
        switch (owner) {
            case 0 -> keys = new TreeSet<>(run.order);
            case 1 -> keys = new M3TreeSet<>(run.order);
            case 2 -> { map = new TreeMap<>(run.order); keys = map.navigableKeySet(); }
            default -> { map = new M3TreeMap<>(run.order); keys = map.navigableKeySet(); }
        }
        for (int i = 0; i < WORDS.length; i++) {
            if (run.random.nextInt(3) == 0) continue;
            String stored = new String(WORDS[i]);
            run.ids.put(stored, i);
            if (map != null) map.put(stored, i); else keys.add(stored);
        }
        int shape = run.random.nextInt(5);
        boolean deep = id % 40 == 39;
        boolean mutable = !deep && run.random.nextInt(4) == 0;
        String tags = "owner=" + owner + " shape=" + shape + (deep ? " deep" : "") + (mutable ? " mutable" : "");
        if (map != null && run.random.nextBoolean() && !deep) {
            mapOps(run, map, shape, mutable, tags);
            return;
        }
        NavigableSet<String> base = section(run, keys, shape);
        if (mutable) {
            setOps(run, M3MutableFilteredViews.filter(base, run.predicate(0)), true, tags + " layers=m");
            return;
        }
        int layers = deep ? DEEP : 1 + run.random.nextInt(4);
        Collection<String> view = base;
        StringBuilder kinds = new StringBuilder();
        for (int layer = 0; layer < layers; layer++) {
            int kind = deep ? 3 : run.random.nextInt(4);
            if (kind >= 2 && !(view instanceof NavigableSet<?>)) kind = run.random.nextInt(2);
            if (kind == 1 && !(view instanceof Set<?>)) kind = 0;
            if (!deep) kinds.append("CSZN".charAt(kind));
            Predicate<String> predicate = deep ? value -> true : run.predicate(layer);
            view = switch (kind) {
                case 0 -> M3FilteredViews.filter(view, predicate);
                case 1 -> M3FilteredViews.filter((Set<String>) view, predicate);
                case 2 -> {
                    SortedSet<String> sorted = (NavigableSet<String>) view;
                    yield M3FilteredViews.filter(sorted, predicate);
                }
                default -> M3FilteredViews.filter((NavigableSet<String>) view, predicate);
            };
        }
        setOps(run, view, false, tags + " layers=" + (deep ? "N" + DEEP : kinds.toString()));
    }

    private static void setOps(Run run, Collection<String> view, boolean mutable, String tags) {
        String[] kind = new String[1];
        for (int step = 0; step < 12; step++) {
            String probe = probe(run, kind);
            String t = tags + " probe=" + kind[0];
            switch (run.random.nextInt(mutable ? 12 : 10)) {
                case 0 -> run.line("iterator", t, view::iterator);
                case 1 -> run.line("size", t, view::size);
                case 2 -> run.line("isEmpty", t, view::isEmpty);
                case 3, 4 -> run.line("contains", t, () -> view.contains(probe));
                case 5 -> run.line("descendingIterator", t,
                        () -> view instanceof NavigableSet<String> nav ? nav.descendingIterator() : "n/a");
                case 6 -> run.line("last", t, () -> view instanceof SortedSet<String> sorted ? sorted.last() : "n/a");
                case 7 -> run.line("first", t, () -> view instanceof SortedSet<String> sorted ? sorted.first() : "n/a");
                case 8 -> run.line("navigate", t, () -> view instanceof NavigableSet<String> nav
                        ? List.of(String.valueOf(run.id(nav.lower(probe))), String.valueOf(run.id(nav.floor(probe))),
                                String.valueOf(run.id(nav.ceiling(probe))), String.valueOf(run.id(nav.higher(probe))))
                        : "n/a");
                case 9 -> run.line("descendingSet", t, () -> view instanceof NavigableSet<String> nav
                        ? nav.descendingSet().iterator() : "n/a");
                case 10 -> run.line("remove", t, () -> view.remove(probe));
                default -> run.line("pollFirst", t, () -> ((NavigableSet<String>) view).pollFirst());
            }
        }
    }

    private static void mapOps(Run run, NavigableMap<String, Integer> map, int shape, boolean mutable, String tags) {
        Predicate<String> keys = run.predicate(0);
        NavigableMap<String, Integer> view = mutable
                ? M3MutableFilteredViews.filterEntries(map, (key, value) -> keys.test(key))
                : M3FilteredViews.filterEntries(map, (key, value) -> keys.test(key));
        NavigableMap<String, Integer> section = shape == 4 ? view.descendingMap() : view;
        String[] kind = new String[1];
        for (int step = 0; step < 12; step++) {
            String probe = probe(run, kind);
            String t = tags + " map probe=" + kind[0];
            int value = step;
            switch (run.random.nextInt(mutable ? 10 : 8)) {
                case 0 -> run.line("entries", t, () -> section.entrySet().iterator());
                case 1 -> run.line("get", t, () -> section.get(probe));
                case 2 -> run.line("containsKey", t, () -> section.containsKey(probe));
                case 3 -> run.line("ceilingEntry", t, () -> section.ceilingEntry(probe));
                case 4 -> run.line("lowerKey", t, () -> section.lowerKey(probe));
                case 5 -> run.line("lastKey", t, section::lastKey);
                case 6 -> run.line("keyDescending", t, () -> section.navigableKeySet().descendingIterator());
                case 7 -> run.line("values", t, () -> section.values().iterator());
                case 8 -> run.line("put", t, () -> section.put(probe, value));
                default -> run.line("pollFirstEntry", t, section::pollFirstEntry);
            }
        }
    }

    public static void main(String[] args) {
        int scenarios = Integer.parseInt(args[0]);
        long seed = Long.parseLong(args[1]);
        StringBuilder out = new StringBuilder();
        for (int scenario = 0; scenario < scenarios; scenario++) {
            scenario(scenario, seed, out);
            if (out.length() > 1 << 20) { System.out.print(out); out.setLength(0); }
        }
        System.out.print(out);
    }
}
