/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/** Independent public-contract corpus for the combined original collection owners. */
public final class M3CollectionReconcileContract {
    private static long checks, digest = 37;
    private M3CollectionReconcileContract() { }
    private static void check(boolean value, String detail) {
        checks++;
        if (!value) throw new AssertionError(detail + " check=" + checks);
    }
    private static void eq(Object expected, Object actual) {
        check(Objects.equals(expected, actual), "expected=" + expected + "; actual=" + actual);
        digest = 31 * digest + Objects.hashCode(actual);
    }
    private static void refuses(Class<? extends Throwable> type, String message, Runnable action) {
        try { action.run(); }
        catch (RuntimeException | LinkageError failure) {
            check(type.isInstance(failure), "wrong refusal: " + failure);
            if (message != null) eq(message, failure.getMessage());
            return;
        }
        throw new AssertionError("missing refusal: " + type.getName());
    }
    private static Comparator<Integer> comparator(int profile) {
        return switch (profile) {
            case 0 -> Comparator.naturalOrder();
            case 1 -> Comparator.reverseOrder();
            case 2 -> Comparator.nullsFirst(Comparator.naturalOrder());
            case 3 -> Comparator.nullsLast(Comparator.naturalOrder());
            default -> (a, b) -> Long.compare(a == null ? 0 : 2L * a + 1,
                                               b == null ? 0 : 2L * b + 1);
        };
    }
    private static Integer neighbor(NavigableSet<Integer> set, Integer value, int op) {
        return switch (op) {
            case 0 -> set.lower(value); case 1 -> set.floor(value);
            case 2 -> set.ceiling(value); case 3 -> set.higher(value);
            default -> throw new AssertionError(op);
        };
    }
    private static void nullWitness() {
        var source = new TreeSet<Integer>(comparator(4));
        source.addAll(Arrays.asList(-1, null, 1));
        var view = M3FilteredViews.filter(source, Objects::nonNull);
        eq(-1, view.floor(null));
        eq(1, view.ceiling(null));
    }
    private static void navigationMatrix() {
        for (int profile = 0; profile < 5; profile++) {
            Comparator<Integer> order = comparator(profile);
            List<Integer> universe = profile < 2 ? List.of(-2, -1, 0, 1, 2)
                    : Arrays.asList(-2, -1, null, 0, 1, 2);
            var fences = new ArrayList<Integer>(List.of(-4, -1, 1, 4));
            var queries = new ArrayList<Integer>(List.of(Integer.MIN_VALUE, -4, -2, -1, 0, 1, 2, 4, Integer.MAX_VALUE));
            if (profile >= 2) { fences.add(null); queries.add(null); }
            fences.sort(order);
            for (int state = 0; state < (1 << universe.size()); state++) {
                var source = new TreeSet<Integer>(order);
                for (int i = 0; i < universe.size(); i++) if ((state & (1 << i)) != 0) source.add(universe.get(i));
                for (int lo = 0; lo < fences.size(); lo++) for (int hi = lo; hi < fences.size(); hi++) {
                    for (int inclusivity = 0; inclusivity < 4; inclusivity++) {
                        var range = source.subSet(fences.get(lo), (inclusivity & 1) != 0,
                                fences.get(hi), (inclusivity & 2) != 0);
                        for (int selection = 0; selection < 4; selection++) {
                            final int which = selection;
                            Predicate<Integer> first = x -> which == 0 || x == null || (x & 1) == 0;
                            Predicate<Integer> second = x -> which < 2 || (which == 2 ? x != null : x == null);
                            var view = M3FilteredViews.filter(M3FilteredViews.filter(range, first), second);
                            var expected = new TreeSet<Integer>(order);
                            for (Integer x : range) if (first.test(x) && second.test(x)) expected.add(x);
                            for (int direction = 0; direction < 2; direction++) {
                                var actual = direction == 0 ? view : view.descendingSet();
                                var oracle = direction == 0 ? expected : expected.descendingSet();
                                eq(new ArrayList<>(oracle), new ArrayList<>(actual));
                                eq(oracle.size(), actual.size());
                                eq(oracle.isEmpty(), actual.isEmpty());
                                for (Integer query : queries) for (int op = 0; op < 4; op++) {
                                    eq(neighbor(oracle, query, op), neighbor(actual, query, op));
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    private static void terminalsAndComposition() {
        var source = new ArrayList<Integer>(Arrays.asList(null, 1, 2, 3, 4, 2));
        var calls = new ArrayList<Integer>();
        Collection<Integer> view = M3FilteredViews.filter(source, x -> {
            calls.add(x); return x != null && (x & 1) == 0;
        });
        eq(List.of(), calls);
        eq(false, view.isEmpty()); eq(Arrays.asList(null, 1, 2), calls);
        calls.clear(); eq(3, view.size()); eq(source, calls);
        calls.clear();
        refuses(UnsupportedOperationException.class, "read-only filtered view", view::clear);
        refuses(UnsupportedOperationException.class, "read-only filtered view", () -> view.addAll(List.of()));
        refuses(UnsupportedOperationException.class, "read-only filtered view", () -> view.removeIf(x -> false));
        eq(List.of(), calls);
        var frozen = M3IndexedListSnapshot.copyOf(view).sorted(Comparator.reverseOrder());
        source.add(6); eq(4, view.size()); eq(List.of(4, 2, 2), new ArrayList<>(frozen));
        AtomicInteger ordinal = new AtomicInteger();
        var child = frozen.filtered(x -> ordinal.getAndIncrement() != 1);
        eq(3, ordinal.get()); eq(List.of(4, 2), new ArrayList<>(child));
        eq(1, child.physicalIndex(0)); eq(2, child.physicalIndex(1));
        var failure = new IllegalStateException("caller failure");
        ordinal.set(0);
        try {
            frozen.filtered(x -> { if (ordinal.incrementAndGet() == 2) throw failure; return true; });
            throw new AssertionError("lost callback failure");
        } catch (IllegalStateException caught) { check(caught == failure, "callback exception identity"); }
        eq(2, ordinal.get());
        var map = new TreeMap<Integer, String>();
        map.put(0, "hidden"); map.put(1, "a"); map.put(2, "b");
        var filteredMap = M3FilteredViews.filterEntries(map, (k, v) -> k > 0);
        eq(false, filteredMap.isEmpty()); eq(1, filteredMap.firstKey());
        var a = filteredMap.firstEntry(); var b = filteredMap.firstEntry();
        check(a != b, "fresh immutable map entry"); eq(a, b);
        refuses(UnsupportedOperationException.class, null, () -> a.setValue("bad"));
        map.put(1, "changed"); eq("a", a.getValue()); eq("changed", filteredMap.firstEntry().getValue());
    }
    private static void aliasWitness() {
        int[] rows = {7}, offsets = {0}, needs = {0}, denies = {0};
        int[][] planes = {{0}};
        refuses(IllegalArgumentException.class, "mask/output alias", () ->
                M3IndexPathMasks.scanWindow(rows, offsets, planes, needs, denies,
                        0, 1, 1, 0, needs, 0, 1, M3IndexPathMasks.Backend.JAVA));
        eq(0, needs[0]);
    }
    private static void aliasCases() {
        int[] rows = {7}, offsets = {0}, needs = {0}, denies = {0};
        int[][] planes = {{0}};
        for (var backend : M3IndexPathMasks.Backend.values()) {
            for (int length : new int[]{0, 1}) for (int count : new int[]{0, 1}) {
                for (int[] output : new int[][]{needs, denies}) {
                    refuses(IllegalArgumentException.class, "mask/output alias", () ->
                        M3IndexPathMasks.scanWindow(rows, offsets, planes, needs, denies,
                            0, count, 1, 0, output, 0, length, backend));
                    eq(0, needs[0]); eq(0, denies[0]);
                }
            }
        }
    }
    private static void pathMatrix(M3IndexPathMasks.Backend backend) {
        int[] rules = {0, 1, 2, 3, 128, 255, Integer.MIN_VALUE, -1};
        for (int count : new int[]{0, 1, 2, 7, 31, 64, 129}) {
            for (int stride : new int[]{-3, -1, 1, 3}) {
                int capacity = Math.max(1, count * Math.abs(stride) + 6);
                int[] rows = new int[capacity], positions = new int[capacity];
                int[][] planes = new int[2][capacity];
                for (int i = 0; i < capacity; i++) {
                    rows[i] = i % 3 == 0 ? Integer.MIN_VALUE : (i % 5 == 0 ? Integer.MAX_VALUE : i % 7);
                    positions[i] = capacity - i - 1;
                    planes[0][i] = i % 4 == 0 ? -1 : i * 0x1020101;
                    planes[1][i] = Integer.rotateLeft(planes[0][i], 13);
                }
                int first = count == 0 ? capacity : stride > 0 ? 2 : 2 + (count - 1) * -stride;
                for (int required : rules) for (int forbidden : rules) {
                    int[] needs = {required, required & 3}, denies = {forbidden, 0};
                    var expected = new ArrayList<Integer>();
                    for (int i = 0; i < count; i++) {
                        int at = first + i * stride, slot = positions[at];
                        if ((planes[0][slot] & required) == required && (planes[0][slot] & forbidden) == 0
                                && (planes[1][slot] & needs[1]) == needs[1]) expected.add(rows[at]);
                    }
                    int[] detached = M3IndexPathMasks.select(rows, positions, planes, needs, denies, first, count, stride, backend);
                    eq(expected.size(), detached.length);
                    for (int i = 0; i < detached.length; i++) eq(expected.get(i), detached[i]);
                    int next = 0, collected = 0;
                    do {
                        int[] output = new int[11]; Arrays.fill(output, 0x514e);
                        long result = M3IndexPathMasks.scanWindow(rows, positions, planes, needs, denies,
                                first, count, stride, next, output, 2, 7, backend);
                        int stop = M3IndexPathMasks.nextOrdinal(result), written = M3IndexPathMasks.written(result);
                        check(stop >= next && stop <= count && (stop > next || stop == count), "cursor progress");
                        for (int i = 0; i < written; i++) eq(expected.get(collected++), output[2 + i]);
                        for (int i = 0; i < output.length; i++) if (i < 2 || i >= 2 + written) eq(0x514e, output[i]);
                        next = stop;
                    } while (next < count);
                    eq(expected.size(), collected);
                    eq(required, needs[0]); eq(forbidden, denies[0]);
                }
            }
        }
    }
    public static void main(String[] args) {
        String mode = args.length == 0 ? "java" : args[0];
        if (mode.equals("null-witness")) nullWitness();
        else if (mode.equals("alias-witness")) aliasWitness();
        else if (mode.equals("missing")) {
            check(!M3IndexPathMasks.nativeAvailable(), "native library unexpectedly loaded"); aliasCases();
        } else {
            nullWitness(); navigationMatrix(); terminalsAndComposition(); aliasWitness(); aliasCases();
            if (mode.equals("native")) check(M3IndexPathMasks.nativeAvailable(), "native required");
            pathMatrix(mode.equals("native") ? M3IndexPathMasks.Backend.NATIVE_REQUIRED : M3IndexPathMasks.Backend.JAVA);
        }
        System.out.println("COLLECTION_RECONCILE_PASS mode=" + mode + " checks=" + checks + " digest=" + Long.toUnsignedString(digest));
    }
}
