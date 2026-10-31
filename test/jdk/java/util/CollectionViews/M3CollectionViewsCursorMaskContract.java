/*
 * @test
 * @summary Filtered CollectionViews cursor bit-state preserves callback/iterator contracts
 * @modules java.base/java.util:open
 * @run main/othervm -Xss256k M3CollectionViewsCursorMaskContract
 */
/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.CollectionViews;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeSet;

/** Exact direct java.base owner (not an external collection wrapper). */
public final class M3CollectionViewsCursorMaskContract {
    private static long checks;

    private M3CollectionViewsCursorMaskContract() { }

    public static void main(String[] args) {
        nullAndDuplicateSelection();
        deeplyNestedPredicateOrder();
        exceptionAfterPreparedState();
        sortedLiveViews();
        System.out.println("PASS M3CollectionViewsCursorMaskContract checks=" + checks);
    }

    private static void nullAndDuplicateSelection() {
        List<Integer> root = new ArrayList<>(Arrays.asList(null, 1, 2, null, 2, 3, 4));
        Collection<Integer> filtered = CollectionViews.filtered(
                (Collection<Integer>) root, v -> v == null || (v & 1) == 0);
        Iterator<Integer> it = filtered.iterator();
        check(it.hasNext() && it.hasNext(), "prepared next cached");
        eq(null, it.next(), "null is a selected value");
        eq(2, it.next(), "first equal-valued occurrence");
        refuses(UnsupportedOperationException.class, it::remove,
                "JDK filtered iterator removal");
        eq(null, it.next(), "second null");
        eq(2, it.next(), "duplicate occurrence");
        eq(4, it.next(), "final selected");
        check(!it.hasNext() && !it.hasNext(), "prepared terminal flag");
        root.add(6);
        eq(Arrays.asList(null, 2, null, 2, 4, 6), copy(filtered),
                "later iterator observes live changes");
    }

    private static void deeplyNestedPredicateOrder() {
        Collection<Integer> root = new ArrayList<>(List.of(1, 2, 3));
        List<String> trace = new ArrayList<>();
        Collection<Integer> filtered = root;
        for (int index = 0; index < 512; index++) {
            int at = index;
            filtered = CollectionViews.filtered(filtered, v -> {
                trace.add(at + ":" + v);
                return (v & 1) == 0;
            });
        }
        Iterator<Integer> it = filtered.iterator();
        check(trace.isEmpty(), "metadata construction does not invoke callbacks");
        eq(2, it.next(), "deep filter first selected");
        eq(513, trace.size(), "one rejected then 512 acceptance callbacks");
        eq("0:1", trace.get(0), "innermost rejection");
        for (int i = 0; i < 512; i++) eq(i + ":2", trace.get(i + 1), "ordered nested callback");
        check(!it.hasNext(), "nested terminal exhaustion");
    }

    private static void exceptionAfterPreparedState() {
        int[] calls = {0};
        Collection<Integer> filtered = CollectionViews.filtered(
                (Collection<Integer>) new ArrayList<>(List.of(1, 2, 3)),
                v -> {
                    calls[0]++;
                    if (v == 1) throw new IllegalArgumentException("user predicate");
                    return true;
                });
        Iterator<Integer> it = filtered.iterator();
        refuses(IllegalArgumentException.class, it::hasNext, "original callback exception");
        check(!it.hasNext(), "PREPARED remains set on exception");
        eq(1, calls[0], "failed callback is not repeated by hasNext");
        refuses(java.util.NoSuchElementException.class, it::next,
                "next honors original prepared-empty state");
    }

    private static void sortedLiveViews() {
        NavigableSet<Integer> root = new TreeSet<>(List.of(7, 2, 5, 4, 6, 1, 3));
        NavigableSet<Integer> filtered = CollectionViews.filtered(root, v -> v % 2 == 0);
        eq(List.of(2, 4, 6), copy(filtered), "sorted encounter order");
        eq(List.of(6, 4, 2), copy(filtered.descendingSet()),
                "descending navigable comparator metadata");
        root.add(8);
        eq(List.of(2, 4, 6, 8), copy(filtered), "sorted backing remains live");
    }

    private static <E> List<E> copy(Iterable<E> elements) {
        List<E> result = new ArrayList<>();
        for (E value : elements) result.add(value);
        return result;
    }

    private static void check(boolean truth, String why) {
        checks++;
        if (!truth) throw new AssertionError(why);
    }

    private static void eq(Object expected, Object actual, String why) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(why + " expected=" + expected + ", actual=" + actual);
        }
    }

    private static void refuses(Class<? extends Throwable> expected, Runnable task, String why) {
        checks++;
        try {
            task.run();
        } catch (Throwable error) {
            if (expected.isInstance(error)) return;
            throw new AssertionError(why + " unexpected failure", error);
        }
        throw new AssertionError(why + " expected " + expected.getName());
    }
}
