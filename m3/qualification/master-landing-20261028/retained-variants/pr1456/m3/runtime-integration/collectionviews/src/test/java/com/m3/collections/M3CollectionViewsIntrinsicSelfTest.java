/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/** Pure-owner callback parity and explicit interoperability across opaque owner boundaries. */
public final class M3CollectionViewsIntrinsicSelfTest {
    private static final long SEED = 0x4d33434f4c4c2121L;
    private static final int SCENARIOS = 384;
    private static long checks;
    private static String route;

    private M3CollectionViewsIntrinsicSelfTest() { }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    private static boolean intrinsic(int layer) {
        return route.equals("direct") || route.equals("mixed") && (layer & 1) == 0;
    }

    @SuppressWarnings("unchecked")
    private static <E> Collection<E> filter(Collection<E> source, Predicate<? super E> predicate,
                                           int layer) {
        Collection<E> result;
        boolean direct = intrinsic(layer);
        if (source instanceof NavigableSet<?> && layer % 4 == 0) {
            result = direct ? java.util.CollectionViews.filtered((NavigableSet<E>) source, predicate)
                    : M3CollectionViews.filtered((NavigableSet<E>) source, predicate);
        } else if (source instanceof SortedSet<?> && layer % 3 == 0) {
            result = direct ? java.util.CollectionViews.filtered((SortedSet<E>) source, predicate)
                    : M3CollectionViews.filtered((SortedSet<E>) source, predicate);
        } else if (source instanceof Set<?> && layer % 2 == 0) {
            result = direct ? java.util.CollectionViews.filtered((Set<E>) source, predicate)
                    : M3CollectionViews.filtered((Set<E>) source, predicate);
        } else {
            result = direct ? java.util.CollectionViews.filtered(source, predicate)
                    : M3CollectionViews.filtered(source, predicate);
        }
        if (direct) {
            check(result.getClass().getClassLoader() == null, "intrinsic bootstrap-owned implementation");
            check(result.getClass().getModule() == Object.class.getModule(), "intrinsic java.base owner");
        }
        return result;
    }

    private static final class Trace {
        final StringBuilder text = new StringBuilder();
        final RuntimeException marker;
        final char category;
        final int at;
        int calls;
        boolean active;

        Trace(int scenario) {
            category = new char[] {'p', 'c', 'a', 'h', 'n', 's', 'r', 'o'}[scenario & 7];
            at = 1 + scenario % 23;
            marker = (scenario & 8) == 0 ? new IllegalArgumentException("opaque marker")
                    : new IndexOutOfBoundsException("opaque bounds marker");
        }

        void event(char kind, Object value) {
            text.append(kind).append('(').append(value).append(");");
            if (active && kind == category && ++calls == at) throw marker;
        }

        void result(Object value) { text.append('=').append(value).append(';'); }

        void failure(Throwable failure) {
            text.append('!').append(failure.getClass().getName())
                    .append(':').append(failure == marker);
            if (failure.getCause() != null) text.append('/').append(failure.getCause().getClass().getName())
                    .append(':').append(failure.getCause() == marker);
            text.append(';');
        }
    }

    private static final class ObservedCollection extends AbstractCollection<Integer> {
        final List<Integer> values;
        final Trace trace;

        ObservedCollection(Collection<Integer> values, Trace trace) {
            this.values = new ArrayList<>(values);
            this.trace = trace;
        }

        @Override public Iterator<Integer> iterator() {
            trace.event('o', "iterator");
            Iterator<Integer> cursor = values.iterator();
            return new Iterator<>() {
                @Override public boolean hasNext() { trace.event('h', "hasNext"); return cursor.hasNext(); }
                @Override public Integer next() { trace.event('n', "next"); return cursor.next(); }
                @Override public void remove() { trace.event('r', "remove"); cursor.remove(); }
            };
        }
        @Override public int size() { trace.event('s', "size"); return values.size(); }
        @Override public boolean contains(Object value) { trace.event('o', value); return values.contains(value); }
        @Override public boolean add(Integer value) { trace.event('o', value); return values.add(value); }
    }

    private static final class SortedOnly extends AbstractSet<Integer> implements SortedSet<Integer> {
        final SortedSet<Integer> source;
        SortedOnly(SortedSet<Integer> source) { this.source = source; }
        @Override public Iterator<Integer> iterator() { return source.iterator(); }
        @Override public int size() { return source.size(); }
        @Override public boolean contains(Object value) { return source.contains(value); }
        @Override public boolean add(Integer value) { return source.add(value); }
        @Override public boolean remove(Object value) { return source.remove(value); }
        @Override public Comparator<? super Integer> comparator() { return source.comparator(); }
        @Override public Integer first() { return source.first(); }
        @Override public Integer last() { return source.last(); }
        @Override public SortedSet<Integer> subSet(Integer from, Integer to) { return new SortedOnly(source.subSet(from, to)); }
        @Override public SortedSet<Integer> headSet(Integer to) { return new SortedOnly(source.headSet(to)); }
        @Override public SortedSet<Integer> tailSet(Integer from) { return new SortedOnly(source.tailSet(from)); }
    }

    private static Collection<Integer> source(int scenario, Trace trace,
                                               Comparator<Integer> comparator) {
        List<Integer> values = Arrays.asList(-4, 1, 2, null, 2, 6, -2, 7);
        return switch (scenario % 8) {
            case 0 -> new ArrayList<>(values);
            case 1 -> new LinkedHashSet<>(values);
            case 2 -> { TreeSet<Integer> result = new TreeSet<>(comparator); result.addAll(values); yield result; }
            case 3 -> Collections.unmodifiableCollection(new ArrayList<>(values));
            case 4 -> new CopyOnWriteArrayList<>(values);
            case 5 -> new CopyOnWriteArraySet<>(values);
            case 6 -> {
                TreeSet<Integer> result = new TreeSet<>(comparator); result.addAll(values);
                yield new SortedOnly(result);
            }
            case 7 -> new ObservedCollection(values, trace);
            default -> throw new AssertionError();
        };
    }

    private static List<Integer> snapshot(Collection<Integer> source, Predicate<Integer> predicate,
                                           Comparator<Integer> comparator, int selector) {
        boolean direct = intrinsic(selector);
        return switch (selector % 3) {
            case 0 -> direct ? java.util.CollectionViews.sortedSnapshot(source, comparator)
                    : M3CollectionViews.sortedSnapshot(source, comparator);
            case 1 -> direct ? java.util.CollectionViews.filteredSnapshot(source, predicate)
                    : M3CollectionViews.filteredSnapshot(source, predicate);
            case 2 -> direct ? java.util.CollectionViews.filteredSortedSnapshot(source, predicate, comparator)
                    : M3CollectionViews.filteredSortedSnapshot(source, predicate, comparator);
            default -> throw new AssertionError();
        };
    }

    private static void metadata(Spliterator<Integer> cursor, Trace trace,
                                  Comparator<Integer> expectedComparator) {
        trace.result(cursor.characteristics());
        trace.result(cursor.estimateSize());
        if (cursor.hasCharacteristics(Spliterator.SORTED)) {
            trace.result(cursor.getComparator() == expectedComparator);
            check(cursor.getComparator() == expectedComparator, "split child comparator identity");
        }
        check(!cursor.hasCharacteristics(Spliterator.SIZED), "selection remains unsized");
        check(!cursor.hasCharacteristics(Spliterator.SUBSIZED), "selection children remain unsized");
    }

    @SuppressWarnings("unchecked")
    private static String scenario(int number) {
        Trace trace = new Trace(number);
        Comparator<Integer> comparator = (left, right) -> {
            trace.event('c', left + ":" + right);
            return Comparator.nullsFirst(Comparator.<Integer>naturalOrder()).compare(left, right);
        };
        Collection<Integer> owner = source(number, trace, comparator);
        Collection<Integer> selected = owner;
        for (int layer = 0; layer <= number % 5; layer++) {
            int ordinal = layer;
            selected = filter(selected, value -> {
                trace.event('p', ordinal + ":" + value);
                return value == null || (value & 1) == 0;
            }, layer);
        }
        final Collection<Integer> view = selected;
        trace.text.setLength(0);
        trace.active = true;
        Random random = new Random(SEED + number);
        for (int step = 0; step < 24; step++) {
            int operation = step < 4 ? step : random.nextInt(20);
            Integer probe = step % 7 == 0 ? null : random.nextInt(12) - 4;
            trace.text.append('[').append(operation).append(':').append(probe).append(']');
            try {
                switch (operation) {
                    case 0 -> trace.result(view.size());
                    case 1 -> trace.result(view.contains(probe));
                    case 2 -> {
                        Iterator<Integer> cursor = view.iterator();
                        trace.result(cursor.hasNext()); trace.result(cursor.hasNext());
                        trace.result(cursor.next()); cursor.remove();
                    }
                    case 3 -> trace.result(view.add(probe));
                    case 4 -> trace.result(view.remove(probe));
                    case 5 -> trace.result(view.removeIf(value -> {
                        trace.event('a', value); return Objects.equals(value, probe);
                    }));
                    case 6 -> trace.result(view.removeAll(new ObservedCollection(Arrays.asList(probe, 2), trace)));
                    case 7 -> trace.result(view.retainAll(new ObservedCollection(Arrays.asList(probe, 2, null), trace)));
                    case 8 -> trace.result(view.addAll(new ObservedCollection(Arrays.asList(2, 4, probe), trace)));
                    case 9 -> {
                        Spliterator<Integer> cursor = view.spliterator();
                        metadata(cursor, trace, comparator);
                        trace.result(cursor.tryAdvance(value -> trace.event('a', value)));
                        trace.result(cursor.tryAdvance(value -> trace.event('a', value)));
                    }
                    case 10 -> {
                        Spliterator<Integer> cursor = view.spliterator();
                        Spliterator<Integer> first = cursor.trySplit();
                        trace.result(first != null);
                        if (first != null) { metadata(first, trace, comparator); first.forEachRemaining(value -> trace.event('a', value)); }
                        cursor.forEachRemaining(value -> trace.event('a', value));
                    }
                    case 11 -> {
                        List<Integer> result = snapshot(view, value -> {
                            trace.event('a', value); return value == null || value >= 0;
                        }, comparator, number + step);
                        trace.result(result);
                        try { result.add(2); throw new AssertionError("snapshot must reject mutation"); }
                        catch (UnsupportedOperationException expected) { check(true, "snapshot readonly policy"); }
                    }
                    case 12 -> {
                        if (view instanceof NavigableSet<?> set) {
                            NavigableSet<Integer> ordered = (NavigableSet<Integer>) set;
                            trace.result(ordered.lower(probe)); trace.result(ordered.floor(probe));
                            trace.result(ordered.ceiling(probe)); trace.result(ordered.higher(probe));
                        } else if (view instanceof SortedSet<?> set) {
                            trace.result(set.first()); trace.result(set.last());
                        } else trace.result(view.isEmpty());
                    }
                    case 13 -> {
                        if (view instanceof NavigableSet<?> set) {
                            NavigableSet<Integer> ordered = ((NavigableSet<Integer>) set).headSet(probe, true).descendingSet();
                            trace.result(ordered.size()); trace.result(ordered.contains(2));
                        } else if (view instanceof SortedSet<?> set) {
                            trace.result(((SortedSet<Integer>) set).tailSet(probe).size());
                        } else trace.result(Arrays.toString(view.toArray()));
                    }
                    case 14 -> { trace.result(owner.add(probe)); trace.result(view.contains(probe)); }
                    case 15 -> trace.result(view.stream().toList());
                    case 16 -> {
                        Iterator<Integer> cursor = view.iterator();
                        trace.result(cursor.hasNext()); trace.result(owner.add(probe));
                        trace.result(cursor.next()); trace.result(cursor.hasNext());
                    }
                    case 17 -> trace.result(view.containsAll(Arrays.asList(probe, 2)));
                    case 18 -> view.spliterator().tryAdvance(null);
                    case 19 -> { view.clear(); trace.result("cleared"); }
                    default -> throw new AssertionError();
                }
            } catch (RuntimeException failure) {
                trace.failure(failure);
            }
        }
        return number + ":" + trace.text + "\n";
    }

    private static void contracts() {
        check(java.util.CollectionViews.class.getClassLoader() == null, "bootstrap intrinsic owner");
        check(java.util.CollectionViews.class.getModule() == Object.class.getModule(), "java.base placement");
        List<Integer> events = new ArrayList<>();
        Predicate<Integer> predicate = M3CollectionViews.orderedAnd(
                value -> { events.add(1); return true; }, value -> { events.add(2); return false; });
        check(!predicate.test(7) && events.equals(List.of(1, 2)), "package Predicate helper");
        BiPredicate<Integer, Integer> pair = M3CollectionViews.orderedAnd(
                (left, right) -> { events.add(3); return false; },
                (left, right) -> { throw new AssertionError("short circuit"); });
        check(!pair.test(1, 2) && events.equals(List.of(1, 2, 3)), "package BiPredicate helper");
        check(M3CollectionViews.incrementSize(Integer.MAX_VALUE) == Integer.MAX_VALUE,
                "package saturated size helper");
        check(M3CollectionViews.incrementSize(Integer.MAX_VALUE - 1) == Integer.MAX_VALUE,
                "package size endpoint");

        final class Opaque implements Predicate<Integer> {
            @Override public boolean test(Integer value) { return true; }
            @Override public Predicate<Integer> and(Predicate<? super Integer> other) {
                throw new AssertionError("opaque virtual combinator must not run");
            }
        }
        int[] next = {0};
        Collection<Integer> deep = new ArrayDeque<>(List.of(2));
        deep = filter(deep, new Opaque(), 0);
        for (int layer = 0; layer < 65_536; layer++) {
            int ordinal = layer;
            deep = filter(deep, value -> {
                check(next[0]++ == ordinal, "persistent predicate encounter order");
                return true;
            }, layer + 1);
        }
        check(deep.contains(2), "deep mixed factory composition");
        check(next[0] == 65_536, "no duplicate or skipped callbacks");

        RuntimeException sentinel = new IllegalArgumentException("same predicate failure");
        Iterator<Integer> cursor = filter(List.of(1, 2), value -> { throw sentinel; }, 0).iterator();
        try { cursor.hasNext(); throw new AssertionError("predicate must fail"); }
        catch (RuntimeException failure) { check(failure == sentinel, "same thrown instance"); }
        check(!cursor.hasNext(), "incumbent prepared cursor remains exhausted after opaque failure");
    }

    @SuppressWarnings("unchecked")
    private static Collection<Integer> opaqueBoundary(Collection<Integer> source) {
        // Standard JDK wrappers provide an independent opaque-owner reference.
        // Mixed cases use valid Integer/null values, so checked wrappers introduce
        // no extra type failure or bulk-capture admission into these operations.
        if (source instanceof NavigableSet<?>) {
            return Collections.checkedNavigableSet((NavigableSet<Integer>) source, Integer.class);
        }
        if (source instanceof SortedSet<?>) {
            return Collections.checkedSortedSet((SortedSet<Integer>) source, Integer.class);
        }
        if (source instanceof Set<?>) return Collections.checkedSet((Set<Integer>) source, Integer.class);
        return Collections.checkedCollection(source, Integer.class);
    }

    @SuppressWarnings("unchecked")
    private static void mixedOperation(Collection<Integer> view, Collection<Integer> owner,
                                       int operation, StringBuilder tape, RuntimeException marker) {
        try {
            switch (operation) {
                case 0 -> tape.append("size=").append(view.size());
                case 1 -> tape.append("values=").append(new ArrayList<>(view));
                case 2 -> tape.append("contains=").append(view.contains(4));
                case 3 -> tape.append("remove=").append(view.remove(4));
                case 4 -> tape.append("removeIf=").append(view.removeIf(value -> {
                    tape.append("r(").append(value).append(");");
                    return Objects.equals(value, 4);
                }));
                case 5 -> {
                    if (view instanceof NavigableSet<?> set) {
                        NavigableSet<Integer> ordered = (NavigableSet<Integer>) set;
                        tape.append("floor=").append(ordered.floor(5)).append(";higher=").append(ordered.higher(null));
                        tape.append(";range=").append(ordered.headSet(6, true).descendingSet());
                    } else if (view instanceof SortedSet<?> set) {
                        tape.append("range=").append(((SortedSet<Integer>) set).headSet(6));
                    } else tape.append("empty=").append(view.isEmpty());
                }
                case 6 -> { view.clear(); tape.append("cleared"); }
                case 7 -> tape.append("snapshot=").append(M3CollectionViews.filteredSnapshot(view, value -> {
                    tape.append("s(").append(value).append(");");
                    return true;
                }));
                case 8 -> { tape.append("add=").append(owner.add(8)); tape.append(";live=").append(view.contains(8)); }
                case 9 -> {
                    Iterator<Integer> cursor = view.iterator();
                    try {
                        while (cursor.hasNext()) tape.append("next=").append(cursor.next()).append(';');
                    } catch (RuntimeException failure) {
                        tape.append("iterator-failure=").append(failure == marker).append(';');
                        check(failure == marker, "same opaque predicate failure across owners");
                    }
                    tape.append("retry=").append(cursor.hasNext());
                }
                default -> throw new AssertionError();
            }
        } catch (RuntimeException failure) {
            tape.append('!').append(failure.getClass().getName()).append(':').append(failure == marker);
        }
        tape.append(";owner=").append(new ArrayList<>(owner));
    }

    private static String mixedContracts() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        Comparator<Integer> comparator = Comparator.nullsFirst(Comparator.naturalOrder());
        for (int kind = 0; kind < 7; kind++) {
            for (int direction = 0; direction < 2; direction++) {
                for (int operation = 0; operation < 10; operation++) {
                    RuntimeException marker = new IllegalArgumentException("same mixed predicate failure");
                    String[] tapes = new String[2];
                    for (int actual = 0; actual < 2; actual++) {
                        StringBuilder tape = new StringBuilder();
                        int[] count = {0};
                        int op = operation;
                        Predicate<Integer> first = value -> {
                            tape.append("p(").append(value).append(");");
                            if (op == 9 && ++count[0] == 3) throw marker;
                            return value == null || (value & 1) == 0;
                        };
                        Predicate<Integer> second = value -> {
                            tape.append("q(").append(value).append(");");
                            return value == null || value >= 4;
                        };
                        Collection<Integer> owner = source(kind, new Trace(0), comparator);
                        Collection<Integer> view;
                        if (actual == 0) {
                            Collection<Integer> inner = M3CollectionViews.filtered(owner, first);
                            view = M3CollectionViews.filtered(opaqueBoundary(inner), second);
                        } else {
                            Collection<Integer> inner = filter(owner, first, direction);
                            view = filter(inner, second, direction + 1);
                        }
                        mixedOperation(view, owner, operation, tape, marker);
                        tapes[actual] = tape.toString();
                    }
                    check(tapes[0].equals(tapes[1]), "opaque mixed-owner operation "
                            + kind + ":" + direction + ":" + operation + "\nreference=" + tapes[0] + "\nactual=" + tapes[1]);
                    digest.update((kind + ":" + direction + ":" + operation + ":" + tapes[1] + "\n")
                            .getBytes(StandardCharsets.UTF_8));
                }
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 2 || !List.of("baseline", "direct", "mixed").contains(args[0])
                || args.length == 2 && !args[1].equals("--tapes")) {
            throw new IllegalArgumentException("baseline|direct|mixed [--tapes]");
        }
        route = args[0];
        checks = 0;
        if (route.equals("mixed")) {
            String digest = mixedContracts();
            System.out.println("M3_COLLECTION_VIEWS_MIXED_PASS checks=" + checks
                    + " scenarios=140 transcript=" + digest
                    + " runtime=HOST_JDK21_PATCHED_JAVA_BASE opaqueOwners=true");
            return;
        }
        contracts();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (int number = 0; number < SCENARIOS; number++) {
            String tape = scenario(number);
            digest.update(tape.getBytes(StandardCharsets.UTF_8));
            if (args.length == 2) System.out.print(tape);
        }
        System.out.println("M3_COLLECTION_VIEWS_INTRINSIC_PASS route=" + route + " checks=" + checks
                + " scenarios=" + SCENARIOS + " seed=" + SEED
                + " transcript=" + HexFormat.of().formatHex(digest.digest())
                + " runtime=HOST_JDK21_PATCHED_JAVA_BASE");
    }
}
