// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Random;
import java.util.RandomAccess;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Behavioral proof for the explicit List owner at deep predicate composition.
 * Run the actual recipe output with Java21 and -Xss256k. This is a finite contract
 * check, not a benchmark or a claim about recursion inside a caller predicate.
 */
public final class M3DeepFilteredListsProof {
    private static long checks;

    private M3DeepFilteredListsProof() { }

    private static void check(boolean value, String label) {
        if (!value) throw new AssertionError(label);
        checks++;
    }

    private static void equal(Object expected, Object actual, String label) {
        check(Objects.equals(expected, actual), label + ": " + expected + " != " + actual);
    }

    private static <T extends Throwable> T expect(Class<T> type, Runnable action) {
        try { action.run(); }
        catch (Throwable error) {
            if (!type.isInstance(error)) throw new AssertionError("wrong failure", error);
            checks++;
            return type.cast(error);
        }
        throw new AssertionError("missing " + type.getName());
    }

    private static Object field(Object value, String name) throws ReflectiveOperationException {
        Field result = value.getClass().getDeclaredField(name);
        result.setAccessible(true);
        return result.get(value);
    }

    private static <E> List<E> nested(List<E> source, Predicate<? super E> predicate, int depth) {
        List<E> result = source;
        for (int index = 0; index < depth; index++) {
            result = M3FilteredLists.filtered(result, predicate);
        }
        return result;
    }

    private static void deepOwner(int depth) throws ReflectiveOperationException {
        ArrayList<Integer> backing = new ArrayList<>(Arrays.asList(2, -1));
        int[] calls = {0};
        Predicate<Integer> positive = value -> { calls[0]++; return value > 0; };
        List<Integer> view = nested(backing, positive, depth);
        equal(0, calls[0], "construction evaluated predicate");
        equal(1, view.size(), "deep size");
        equal(depth + 1, calls[0], "duplicate predicate occurrences or short circuit changed");
        check(field(view, "backing") == backing, "List retained another element owner");
        Object root = field(field(view, "predicate"), "root");
        equal(32 - Integer.numberOfLeadingZeros(depth - 1), field(root, "height"),
                "List did not use the shared logarithmic-height composition");
        check(!(view instanceof RandomAccess), "false random-access capability");

        List<Integer> rejected = M3FilteredLists.filtered(view, value -> false);
        equal(List.of(), new ArrayList<>(rejected), "rejecting suffix");
        backing.add(4);
        equal(List.of(2, 4), new ArrayList<>(view), "retained prefix changed or lost liveness");
        equal(List.of(), new ArrayList<>(rejected), "rejected view changed selection");
        equal(2, view.removeFirst(), "deep endpoint removal");
        view.clear();
        equal(List.of(-1), backing, "deep removal lost hidden elements");
    }

    private static void callbackContract() {
        for (int stop : new int[] {0, 1, 2, 7, 31, 32, 63, 127, 128, 255, 256}) {
            List<Integer> trace = new ArrayList<>();
            List<Integer> view = List.of(9);
            for (int index = 0; index < 257; index++) {
                int stage = index;
                view = M3FilteredLists.filtered(view, value -> {
                    trace.add(stage);
                    return stage != stop;
                });
            }
            equal(0, view.size(), "false predicate ignored");
            equal(java.util.stream.IntStream.rangeClosed(0, stop).boxed().toList(),
                    trace, "List predicate order or short circuit changed");

            trace.clear();
            RuntimeException marker = new IllegalArgumentException("predicate marker " + stop);
            List<Integer> throwing = List.of(9);
            for (int index = 0; index < 257; index++) {
                int stage = index;
                throwing = M3FilteredLists.filtered(throwing, value -> {
                    trace.add(stage);
                    if (stage == stop) throw marker;
                    return true;
                });
            }
            List<Integer> exact = throwing;
            check(expect(RuntimeException.class, exact::size) == marker,
                    "exception object replaced");
            equal(java.util.stream.IntStream.rangeClosed(0, stop).boxed().toList(),
                    trace, "exception callback order changed");
        }
        List<Integer> nulls = M3FilteredLists.filtered(Arrays.asList(null, 1, -1), Objects::nonNull);
        nulls = M3FilteredLists.filtered(nulls, value -> value > 0);
        equal(List.of(1), new ArrayList<>(nulls), "null guard moved after a dereference");
        List<Integer> existing = nulls;
        expect(NullPointerException.class, () -> M3FilteredLists.filtered(existing, null));
        expect(NullPointerException.class, () -> M3FilteredLists.filtered(null, Objects::nonNull));
    }

    private static void cursorContract(int depth) {
        List<Integer> backing = new LinkedList<>(List.of(-1, 2, -3, 4, -5, 6, -7));
        List<Integer> view = nested(backing, value -> value % 2 == 0, depth);
        ListIterator<Integer> cursor = view.listIterator();
        check(cursor.hasNext() && !cursor.hasPrevious(), "initial cursor endpoints");
        equal(2, cursor.next(), "first selected value");
        check(cursor.hasNext() && cursor.hasPrevious(), "reversible probes");
        cursor.set(20);
        equal(List.of(-1, 20, -3, 4, -5, 6, -7), backing, "set targeted a prefetched element");
        cursor.remove();
        equal(List.of(-1, -3, 4, -5, 6, -7), backing, "remove targeted a prefetched element");
        equal(0, cursor.nextIndex(), "forward remove index");
        equal(4, cursor.next(), "next after removal");
        equal(4, cursor.previous(), "direction reversal");
        check(cursor.hasNext() && !cursor.hasPrevious(), "backward reversible probes");
        cursor.set(40);
        cursor.remove();
        equal(List.of(-1, -3, -5, 6, -7), backing, "backward remove lost hidden rows");
        equal(0, cursor.nextIndex(), "backward remove index");
        cursor.add(8);
        equal(List.of(-1, -3, 8, -5, 6, -7), backing, "cursor insertion boundary");
        expect(IllegalStateException.class, cursor::remove);
        equal(8, cursor.previous(), "previous after insertion");
        List<Integer> before = new ArrayList<>(backing);
        expect(IllegalArgumentException.class, () -> cursor.set(9));
        equal(before, backing, "rejected iterator set changed backing");
        cursor.set(10);
        equal(10, cursor.next(), "accepted iterator set");
        equal(6, cursor.next(), "last selected value");
        check(!cursor.hasNext(), "unexpected tail");
        expect(java.util.NoSuchElementException.class, cursor::next);

        RuntimeException marker = new IllegalStateException("probe failure");
        List<String> failing = nested(new ArrayList<>(List.of("ok", "bad", "later")), text -> {
            if (text.equals("bad")) throw marker;
            return true;
        }, depth);
        ListIterator<String> probe = failing.listIterator();
        equal("ok", probe.next(), "before predicate error");
        check(expect(RuntimeException.class, probe::hasNext) == marker, "probe wrapped predicate failure");
        expect(IllegalStateException.class, probe::hasNext);
        expect(IllegalStateException.class, probe::remove);
        equal("ok", failing.get(0), "failed iterator poisoned its view");
        equal("ok", failing.iterator().next(), "failed iterator poisoned a new iterator");
    }

    private static void rangesAndReadOnly(int depth) {
        ArrayList<Integer> backing = new ArrayList<>(List.of(-1, 2, -3, 4, -5, 6, -7));
        List<Integer> view = nested(backing, value -> value % 2 == 0, depth);
        List<Integer> range = view.subList(1, 3);
        List<Integer> reversed = range.reversed();
        equal(List.of(6, 4), new ArrayList<>(reversed), "deep reversed physical subList");
        equal(6, reversed.set(0, 60), "reversed set result");
        reversed.add(1, 40);
        equal(List.of(-1, 2, -3, 4, -5, 40, 60, -7), backing, "reversed insertion wrote wrong span");
        equal(List.of(2, 4, 40, 60), new ArrayList<>(view), "range mutation was detached");
        equal(4, reversed.remove(2), "reversed removal");
        equal(List.of(-1, 2, -3, -5, 40, 60, -7), backing, "range removal lost a hidden element");
        equal(List.of(60, 40), new ArrayList<>(reversed), "reversed range stopped being live");

        List<Integer> readOnly = M3FilteredLists.readOnlyFiltered(view, value -> true);
        equal(List.of(60, 40, 2), new ArrayList<>(readOnly.reversed()), "read-only reversed order");
        expect(UnsupportedOperationException.class, () -> readOnly.addAll(List.of()));
        expect(UnsupportedOperationException.class, () -> readOnly.removeIf(value -> false));
        expect(UnsupportedOperationException.class, () -> readOnly.reversed().subList(0, 0).clear());
        expect(UnsupportedOperationException.class, () -> readOnly.listIterator().add(12));
        List<Integer> exact = new ArrayList<>(backing);
        expect(IllegalArgumentException.class, () -> view.addAll(List.of(12, 13, 14)));
        equal(exact, backing, "rejected bulk insert was partly applied");

        ArrayList<Integer> other = new ArrayList<>(List.of(2, -1, 4, 6));
        List<Integer> otherView = nested(other, value -> value % 2 == 0, depth);
        List<Integer> ordinarySpan = otherView.subList(0, 2);
        other.add(8);
        expect(ConcurrentModificationException.class, ordinarySpan::size);
    }

    private static final class Representative {
        final int key;
        final boolean visible;
        Representative(int key, boolean visible) { this.key = key; this.visible = visible; }
        @Override public boolean equals(Object other) {
            return other instanceof Representative value && key == value.key;
        }
        @Override public int hashCode() { return key; }
        @Override public String toString() { return key + ":" + visible; }
    }

    private static void storedRepresentatives(int depth) {
        Representative hidden = new Representative(1, false);
        Representative first = new Representative(1, true);
        Representative second = new Representative(1, true);
        ArrayList<Representative> backing = new ArrayList<>(List.of(hidden, first, second));
        List<Representative> view = nested(backing, value -> value.visible, depth);
        check(view.get(0) == first && view.get(1) == second, "stored references or duplicates changed");
        check(view.remove(new Representative(1, false)), "visible equal stored occurrence was not removed");
        check(backing.size() == 2 && backing.get(0) == hidden && backing.get(1) == second,
                "query predicate or hidden equal representative affected remove");
        view.clear();
        check(backing.size() == 1 && backing.getFirst() == hidden, "hidden equal representative removed");
    }

    private static void compiledRegexAndStableSort(int depth) {
        Predicate<String> prepared = Pattern.compile("a.*").asMatchPredicate();
        ArrayList<String> backing = new ArrayList<>(Arrays.asList(
                null, "beta", new String("a2"), "a1", "alpha", "A0", new String("a2"),
                "\ud83d\ude00", "a\n", "a\u00e9"));
        List<String> view = M3FilteredLists.filtered(backing, Objects::nonNull);
        for (int stage = 1; stage < depth; stage++) {
            view = M3FilteredLists.filtered(view, stage % 257 == 1 ? prepared : Objects::nonNull);
        }
        List<String> selected = backing.stream().filter(Objects::nonNull).filter(prepared).toList();
        equal(selected, new ArrayList<>(view), "compiled full-match predicate composition");
        Comparator<String> byLength = Comparator.comparingInt(String::length);
        List<String> snapshot = M3CollectionViews.filteredSortedSnapshot(view, value -> true, byLength);
        ArrayList<String> oracle = new ArrayList<>(selected);
        oracle.sort(byLength);
        equal(oracle, snapshot, "stable filtered sorted snapshot");
        for (int index = 0; index < oracle.size(); index++) {
            check(oracle.get(index) == snapshot.get(index), "stable comparator tie changed representative");
        }
        backing.add("a0");
        equal(selected.size() + 1, view.size(), "compiled predicate view became detached");
        equal(oracle, snapshot, "sorted snapshot retained live source");
        equal("a0", view.removeLast(), "compiled predicate view removal");
        check(backing.contains(null) && backing.contains("a\n"), "regex-hidden rows removed");
        List<String> selectedView = view;
        expect(IllegalArgumentException.class, () -> selectedView.add("beta"));
    }

    private static List<Integer> selected(Collection<Integer> backing, Predicate<Integer> predicate) {
        ArrayList<Integer> result = new ArrayList<>();
        for (Integer value : backing) if (predicate.test(value)) result.add(value);
        return result;
    }

    private static int physical(List<Integer> backing, Predicate<Integer> predicate, int index) {
        int visible = 0;
        for (int position = 0; position < backing.size(); position++) {
            if (predicate.test(backing.get(position)) && visible++ == index) return position;
        }
        if (visible == index) return backing.size();
        throw new AssertionError("bad oracle index");
    }

    private static void differential(int seeds) {
        for (int seed = 0; seed < seeds; seed++) {
            Random random = new Random(seed);
            int hiddenResidue = Math.floorMod(seed, 5);
            Predicate<Integer> accepted = value -> value != null && value >= 0
                    && Math.floorMod(value, 5) != hiddenResidue;
            List<Integer> backing = seed % 2 == 0 ? new ArrayList<>() : new LinkedList<>();
            for (int index = 0; index < 24; index++) backing.add(random.nextInt(41) - 20);
            backing.add(null);
            ArrayList<Integer> model = new ArrayList<>(backing);
            List<Integer> view = nested(backing, accepted, 257 + seed % 64);
            for (int operation = 0; operation < 32; operation++) {
                List<Integer> visible = selected(model, accepted);
                equal(visible, new ArrayList<>(view), "differential selection " + seed + "/" + operation);
                equal(model, backing, "differential backing " + seed + "/" + operation);
                int value = 100 + random.nextInt(100);
                while (!accepted.test(value)) value++;
                int kind = visible.isEmpty() ? 0 : random.nextInt(6);
                switch (kind) {
                    case 0 -> {
                        int index = random.nextInt(visible.size() + 1);
                        model.add(physical(model, accepted, index), value);
                        view.add(index, value);
                    }
                    case 1 -> {
                        int index = random.nextInt(visible.size());
                        Integer expected = model.set(physical(model, accepted, index), value);
                        equal(expected, view.set(index, value), "differential set result");
                    }
                    case 2 -> {
                        int index = random.nextInt(visible.size());
                        Integer expected = model.remove(physical(model, accepted, index));
                        equal(expected, view.remove(index), "differential remove result");
                    }
                    case 3 -> {
                        int index = random.nextInt(visible.size() + 1);
                        ListIterator<Integer> cursor = view.listIterator(index);
                        for (int back = index - 1; back >= 0; back--) {
                            check(cursor.hasPrevious(), "missing previous");
                            equal(visible.get(back), cursor.previous(), "differential backwards");
                        }
                        for (Integer expected : visible) {
                            check(cursor.hasNext(), "missing next");
                            equal(expected, cursor.next(), "differential forwards");
                        }
                        check(!cursor.hasNext(), "extra differential tail");
                    }
                    case 4 -> {
                        ArrayList<Integer> reversed = new ArrayList<>(visible);
                        Collections.reverse(reversed);
                        equal(reversed, new ArrayList<>(view.reversed()), "differential reversed");
                        int from = random.nextInt(visible.size() + 1);
                        int to = from + random.nextInt(visible.size() - from + 1);
                        equal(visible.subList(from, to), new ArrayList<>(view.subList(from, to)),
                                "differential physical span");
                    }
                    case 5 -> {
                        model.add(value);
                        backing.add(value);
                        model.add(-1);
                        backing.add(-1);
                    }
                    default -> throw new AssertionError("unreachable operation");
                }
            }
            equal(selected(model, accepted), new ArrayList<>(view), "final differential selection");
            model.removeIf(accepted);
            view.clear();
            equal(model, backing, "differential hidden survivors");
        }
    }

    public static void run(int depth, int seeds) throws ReflectiveOperationException {
        if (depth < 2 || seeds < 1) throw new IllegalArgumentException("depth >= 2 and seeds >= 1 required");
        checks = 0;
        deepOwner(depth);
        callbackContract();
        cursorContract(depth);
        rangesAndReadOnly(depth);
        storedRepresentatives(depth);
        compiledRegexAndStableSort(depth);
        differential(seeds);
        System.out.println("M3DeepFilteredListsProof checks=" + checks + " depth=" + depth + " seeds=" + seeds);
    }

    public static void main(String[] args) throws ReflectiveOperationException {
        if (args.length > 0 && args[0].equals("--baseline-overflow")) {
            int depth = args.length > 1 ? Integer.parseInt(args[1]) : 65536;
            List<Integer> original = nested(List.of(1), value -> true, depth);
            expect(StackOverflowError.class, original::size);
            System.out.println("Original M3FilteredLists overflow reproduced at depth=" + depth);
            return;
        }
        int depth = args.length > 0 ? Integer.parseInt(args[0]) : 65536;
        int seeds = args.length > 1 ? Integer.parseInt(args[1]) : 192;
        run(depth, seeds);
    }
}
