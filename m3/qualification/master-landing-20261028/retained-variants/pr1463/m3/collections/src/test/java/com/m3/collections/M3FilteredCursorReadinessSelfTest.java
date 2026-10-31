/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Selection-callback differential against the generic view over JDK TreeMap,
 * with independent slot-reuse, bounded-comparator and iterator-state witnesses.
 * Mutable LiveEntry.getValue() has its own visibility check; selection comparisons
 * deliberately project the generic entry's key and read its JDK owner directly.
 */
public final class M3FilteredCursorReadinessSelfTest {
    private static long checks;

    private M3FilteredCursorReadinessSelfTest() { }

    private static void check(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError(label + " check=" + checks);
    }

    private static void same(Object expected, Object actual, String label) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(label + " expected=" + expected + " actual=" + actual);
        }
    }

    private static Throwable expect(Class<? extends Throwable> type, Runnable action, String label) {
        checks++;
        try {
            action.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) return failure;
            throw new AssertionError(label + " expected=" + type + " actual=" + failure, failure);
        }
        throw new AssertionError(label + " expected=" + type);
    }

    private static <K, V> NavigableMap<K, V> view(NavigableMap<K, V> owner,
            BiPredicate<? super K, ? super V> predicate, boolean mutable) {
        return mutable ? M3MutableFilteredViews.filterEntries(owner, predicate)
                : M3FilteredViews.filterEntries(owner, predicate);
    }

    private static <E> NavigableSet<E> view(NavigableSet<E> owner,
            Predicate<? super E> predicate, boolean mutable) {
        return mutable ? M3MutableFilteredViews.filter(owner, predicate)
                : M3FilteredViews.filter(owner, predicate);
    }

    private static NavigableMap<Integer, String> section(NavigableMap<Integer, String> owner,
            boolean bounded, boolean descending) {
        NavigableMap<Integer, String> part = bounded ? owner.subMap(-3, true, 6, false) : owner;
        return descending ? part.descendingMap() : part;
    }

    private static boolean visible(Integer key) { return key == null || key % 3 == 0; }

    private static void readiness(boolean mutable) {
        for (boolean bounded : new boolean[] {false, true}) {
            for (boolean descending : new boolean[] {false, true}) {
                for (boolean values : new boolean[] {false, true}) {
                    String label = "ready mutable=" + mutable + " bounded=" + bounded
                            + " descending=" + descending + " values=" + values;
                    Comparator<Integer> comparator = Comparator.nullsFirst(Comparator.naturalOrder());
                    TreeMap<Integer, String> jdk = new TreeMap<>(comparator);
                    M3TreeMap<Integer, String> owner = new M3TreeMap<>(comparator);
                    for (Integer key : Arrays.asList(null, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6)) {
                        String value = key == null || key == 0 ? null : new String("v" + key);
                        jdk.put(key, value);
                        owner.put(key, value);
                    }
                    NavigableMap<Integer, String> expectedSource = section(jdk, bounded, descending);
                    NavigableMap<Integer, String> actualSource = section(owner, bounded, descending);
                    List<Integer> expectedCalls = new ArrayList<>();
                    List<Integer> actualCalls = new ArrayList<>();
                    Iterator<Map.Entry<Integer, String>> expected = view(expectedSource,
                            (key, value) -> { expectedCalls.add(key); return visible(key); }, mutable)
                            .entrySet().iterator();
                    NavigableMap<Integer, String> actualView = view(actualSource,
                            (key, value) -> { actualCalls.add(key); return visible(key); }, mutable);
                    Iterator<?> actual = values ? actualView.values().iterator()
                            : actualView.entrySet().iterator();
                    same(List.of(), actualCalls, label + " construction is lazy");
                    if (mutable) expect(IllegalStateException.class, actual::remove, label + " early remove");
                    else expect(UnsupportedOperationException.class, actual::remove, label + " readonly remove");
                    while (expected.hasNext()) {
                        check(actual.hasNext(), label + " first prepare");
                        same(expectedCalls, actualCalls, label + " first selection");
                        for (int repeat = 0; repeat < 3; repeat++) {
                            check(expected.hasNext() && actual.hasNext(), label + " repeated readiness");
                            same(expectedCalls, actualCalls, label + " no repeated callback");
                        }
                        Integer key = expected.next().getKey();
                        Object observed = actual.next();
                        if (values) check(expectedSource.get(key) == observed, label + " payload identity");
                        else check(key == ((Map.Entry<?, ?>) observed).getKey(), label + " key identity");
                        same(expectedCalls, actualCalls, label + " next consumes prepared selection");
                    }
                    for (int repeat = 0; repeat < 3; repeat++) {
                        check(!actual.hasNext(), label + " exhausted");
                        same(expectedCalls, actualCalls, label + " exhausted callbacks");
                    }
                    same(new ArrayList<>(expectedSource.keySet()), actualCalls, label + " one visit per key");
                    expect(NoSuchElementException.class, actual::next, label + " exhausted next");
                    owner.verifyInvariants();
                }
            }
        }
    }

    private static void removeAfterPrefetch() {
        for (boolean descending : new boolean[] {false, true}) {
            for (boolean values : new boolean[] {false, true}) {
                M3TreeMap<Integer, String> owner = new M3TreeMap<>();
                for (int key : new int[] {4, 2, 6, 1, 3, 5, 7}) owner.put(key, "v" + key);
                List<Integer> calls = new ArrayList<>();
                NavigableMap<Integer, String> source = owner.subMap(1, true, 7, true);
                if (descending) source = source.descendingMap();
                boolean reverse = descending;
                NavigableMap<Integer, String> filtered = view(source, (key, value) -> {
                    calls.add(key);
                    return reverse ? key <= 4 : key >= 4;
                }, true);
                Iterator<?> cursor = values ? filtered.values().iterator() : filtered.entrySet().iterator();
                Object first = cursor.next();
                same(values ? "v4" : 4, values ? first : ((Map.Entry<?, ?>) first).getKey(), "two-child remove");
                check(cursor.hasNext(), "prefetch before remove");
                List<Integer> preparedCalls = new ArrayList<>(calls);
                cursor.remove();
                check(!owner.containsKey(4), "last returned key removed");
                check(cursor.hasNext(), "prepared successor survives erase");
                same(preparedCalls, calls, "remove keeps prepared visibility");
                expect(IllegalStateException.class, cursor::remove, "double remove");
                List<Object> remaining = new ArrayList<>();
                while (cursor.hasNext()) {
                    Object value = cursor.next();
                    remaining.add(values ? value : ((Map.Entry<?, ?>) value).getKey());
                }
                List<?> expected = descending ? List.of(3, 2, 1) : List.of(5, 6, 7);
                if (values) expected = expected.stream().map(key -> "v" + key).toList();
                same(expected, remaining, "successor physical identity after erase");
                owner.verifyInvariants();
            }
        }
        M3TreeMap<Integer, String> owner = new M3TreeMap<>(Comparator.nullsFirst(Comparator.naturalOrder()));
        owner.put(null, null);
        owner.put(1, "one");
        Iterator<String> cursor = view(owner, (key, value) -> true, true).values().iterator();
        same(null, cursor.next(), "null payload returned");
        check(cursor.hasNext(), "prefetched nonnull after null key");
        cursor.remove();
        check(!owner.containsKey(null), "null key is removable");
        same("one", cursor.next(), "null removal keeps pending slot");
    }

    private record Key(int rank, String spelling) { }

    private static void retainedRepresentatives(boolean mutable) {
        M3TreeMap<Key, String> owner = new M3TreeMap<>(Comparator.comparingInt(Key::rank));
        Key stored = new Key(1, "stored");
        owner.put(stored, "before");
        owner.put(new Key(1, "alias"), "after");
        AtomicInteger calls = new AtomicInteger();
        NavigableMap<Key, String> filtered = view(owner, (key, value) -> {
            check(key == stored, "predicate receives comparator-retained representative");
            calls.incrementAndGet();
            return true;
        }, mutable);
        Iterator<Map.Entry<Key, String>> cursor = filtered.descendingMap().entrySet().iterator();
        check(cursor.hasNext() && cursor.hasNext(), "representative readiness");
        Map.Entry<Key, String> entry = cursor.next();
        check(entry.getKey() == stored, "entry preserves comparator-retained key");
        same(1, calls.get(), "selection predicate runs once");
        same("after", entry.getValue(), "current retained payload");
        if (mutable) {
            same("after", entry.setValue("replacement"), "live entry mutation");
            same("replacement", owner.get(stored), "entry writes through");
        } else expect(UnsupportedOperationException.class, () -> entry.setValue("rejected"), "immutable entry");
    }

    private static void reentrant(boolean mutable) {
        for (boolean accepted : new boolean[] {false, true}) {
            for (int mutation = 0; mutation < 3; mutation++) {
                for (boolean values : new boolean[] {false, true}) {
                    M3TreeMap<Integer, String> owner = new M3TreeMap<>();
                    for (int key = 1; key <= 3; key++) owner.put(key, "v" + key);
                    int change = mutation;
                    NavigableMap<Integer, String> filtered = view(owner, (key, value) -> {
                        if (change == 0) owner.remove(key);
                        else if (change == 1) { owner.clear(); owner.put(42, "reused"); }
                        else owner.put(100, "inserted");
                        return accepted;
                    }, mutable);
                    Iterator<?> cursor = values ? filtered.values().iterator() : filtered.entrySet().iterator();
                    expect(ConcurrentModificationException.class, cursor::hasNext,
                            "predicate mutation before accepting or following a map slot");
                    expect(ConcurrentModificationException.class, cursor::next, "invalidated map next");
                    owner.verifyInvariants();
                }
                M3TreeSet<Integer> owner = new M3TreeSet<>();
                owner.addAll(List.of(1, 2, 3));
                int change = mutation;
                Iterator<Integer> cursor = view(owner, key -> {
                    if (change == 0) owner.remove(key);
                    else if (change == 1) { owner.clear(); owner.add(42); }
                    else owner.add(100);
                    return accepted;
                }, mutable).descendingIterator();
                expect(ConcurrentModificationException.class, cursor::hasNext,
                        "predicate mutation before accepting or following a set slot");
                expect(ConcurrentModificationException.class, cursor::next, "invalidated set next");
                owner.verifyInvariants();
            }
        }
        for (boolean values : new boolean[] {false, true}) {
            M3TreeMap<Integer, String> owner = new M3TreeMap<>();
            owner.put(1, "one");
            RuntimeException marker = new IllegalArgumentException("original predicate failure");
            NavigableMap<Integer, String> filtered = view(owner, (key, value) -> {
                owner.clear();
                throw marker;
            }, mutable);
            Iterator<?> cursor = values ? filtered.values().iterator() : filtered.entrySet().iterator();
            check(expect(IllegalArgumentException.class, cursor::hasNext, "predicate failure") == marker,
                    "predicate exception identity precedes revision check");
            expect(ConcurrentModificationException.class, cursor::hasNext, "subsequent invalidated cursor");
        }
    }

    private static final class ReentrantComparator implements Comparator<Integer> {
        private Runnable action;
        @Override public int compare(Integer left, Integer right) {
            Runnable selected = action;
            action = null;
            if (selected != null) selected.run();
            return Comparator.nullsFirst(Comparator.<Integer>naturalOrder()).compare(left, right);
        }
    }

    private static void boundedComparator(boolean mutable) {
        for (boolean atConstruction : new boolean[] {false, true}) {
            for (boolean values : new boolean[] {false, true}) {
                ReentrantComparator comparator = new ReentrantComparator();
                M3TreeMap<Integer, String> owner = new M3TreeMap<>(comparator);
                for (int key = 1; key <= 3; key++) owner.put(key, "v" + key);
                NavigableMap<Integer, String> filtered = view(owner.subMap(0, true, 4, true),
                        (key, value) -> true, mutable);
                if (atConstruction) comparator.action = owner::clear;
                Iterator<?> cursor = values ? filtered.values().iterator() : filtered.entrySet().iterator();
                if (atConstruction) {
                    expect(ConcurrentModificationException.class, cursor::hasNext, "capture epoch before firstSlot");
                } else {
                    check(cursor.hasNext(), "prepare before comparator mutation");
                    comparator.action = owner::clear;
                    expect(ConcurrentModificationException.class, cursor::next, "bounded nextSlot comparator mutation");
                }
                owner.verifyInvariants();
            }
        }
    }

    private static void externalChanges(boolean mutable) {
        for (boolean values : new boolean[] {false, true}) {
            M3TreeMap<Integer, String> owner = new M3TreeMap<>();
            owner.put(1, "one");
            AtomicInteger calls = new AtomicInteger();
            NavigableMap<Integer, String> filtered = view(owner,
                    (key, value) -> { calls.incrementAndGet(); return true; }, mutable);
            Iterator<?> cursor = values ? filtered.values().iterator() : filtered.entrySet().iterator();
            check(cursor.hasNext(), "prepare before nonstructural replacement");
            owner.put(1, "replacement");
            Object result = cursor.next();
            same(1, calls.get(), "replacement preserves prepared selection");
            same("replacement", values ? result : ((Map.Entry<?, ?>) result).getValue(), "live replacement");
            check(!cursor.hasNext(), "exhausted before structural change");
            owner.put(2, "two");
            expect(ConcurrentModificationException.class, cursor::hasNext, "exhausted cursor checks revision");
            cursor = values ? filtered.values().iterator() : filtered.entrySet().iterator();
            check(cursor.hasNext(), "ready before owner clear and slot reuse");
            owner.clear();
            owner.put(9, "nine");
            expect(ConcurrentModificationException.class, cursor::next, "prepared cursor checks revision");
            if (mutable) expect(ConcurrentModificationException.class, cursor::remove, "remove checks revision");
        }
    }

    public static void main(String[] args) {
        checks = 0;
        String mode = args.length == 0 ? "all" : args[0];
        if (!List.of("all", "readonly-ready", "mutable-ready", "readonly-reentrant", "mutable-reentrant")
                .contains(mode)) throw new IllegalArgumentException("unknown mode: " + mode);
        for (boolean mutable : new boolean[] {false, true}) {
            String prefix = mutable ? "mutable" : "readonly";
            if (mode.equals("all") || mode.equals(prefix + "-ready")) {
                readiness(mutable);
                retainedRepresentatives(mutable);
                externalChanges(mutable);
                if (mutable) removeAfterPrefetch();
            }
            if (mode.equals("all") || mode.equals(prefix + "-reentrant")) {
                reentrant(mutable);
                boundedComparator(mutable);
            }
        }
        System.out.println("M3FilteredCursorReadinessSelfTest mode=" + mode + " checks=" + checks);
    }
}
