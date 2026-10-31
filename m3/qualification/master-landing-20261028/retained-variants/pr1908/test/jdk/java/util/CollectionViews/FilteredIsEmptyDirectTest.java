/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
/*
 * @test
 * @summary M3JDK21 filtered isEmpty uses live stored encounters and identical predicate order
 * @run main/othervm FilteredIsEmptyDirectTest
 */
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.CollectionViews;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public final class FilteredIsEmptyDirectTest {
    private static int checks;

    private static void check(boolean yes, String description) {
        checks++;
        if (!yes) throw new AssertionError(description + " check=" + checks);
    }

    private static void noSizeOrCachedPredicateResults() {
        ArrayList<Integer> stored = new ArrayList<>(List.of(1, 2, 3));
        AtomicInteger cursors = new AtomicInteger();
        Collection<Integer> owner = new AbstractCollection<>() {
            @Override public Iterator<Integer> iterator() {
                cursors.incrementAndGet();
                return stored.iterator();
            }
            @Override public int size() { throw new AssertionError("eager count"); }
        };
        AtomicBoolean allow = new AtomicBoolean(false);
        AtomicInteger seen = new AtomicInteger();
        Collection<Integer> filtered = CollectionViews.filtered(owner, x -> {
            seen.incrementAndGet();
            return allow.get() && x == 3;
        });
        check(filtered.isEmpty(), "initially no selected elements");
        check(cursors.get() == 1 && seen.get() == 3, "scan one iterator, all rejected");
        allow.set(true);
        check(!filtered.isEmpty(), "fresh predicate result");
        check(cursors.get() == 2 && seen.get() == 6, "same full scan to last");
        stored.add(3);
        stored.remove(Integer.valueOf(1));
        check(!filtered.isEmpty(), "view follows backing mutation");
        check(cursors.get() == 3 && seen.get() == 8, "re-evaluates only through first accepted");
        stored.clear();
        check(filtered.isEmpty() && cursors.get() == 4, "empty owner");
    }

    private static void exceptionsAndNestedView() {
        RuntimeException token = new IllegalArgumentException("same throw object");
        Collection<Integer> first = CollectionViews.filtered(
                new ArrayList<>(List.of(1, 2, 3, 4)), n -> n % 2 == 0);
        Collection<Integer> second = CollectionViews.filtered(first, n -> {
            if (n == 2) throw token;
            return n > 0;
        });
        try {
            second.isEmpty();
            throw new AssertionError("must propagate predicate exception");
        } catch (IllegalArgumentException error) {
            check(error == token, "exact predicate exception identity");
        }
        TreeSet<Integer> descending = new TreeSet<>((a, b) -> Integer.compare(b, a));
        descending.addAll(List.of(1, 2, 3, 4));
        check(!CollectionViews.filtered(descending, n -> n < 3).isEmpty(),
                "sorted/descending live view");
    }

    private static void randomizedReference() {
        Random rnd = new Random(0x63c3d77dL);
        for (int trial = 0; trial < 30_000; trial++) {
            List<Integer> values = new ArrayList<>();
            int n = rnd.nextInt(72), remainder = rnd.nextInt(11);
            for (int i = 0; i < n; i++) values.add(rnd.nextInt(51) - 25);
            List<Integer> actualCalls = new ArrayList<>();
            Predicate<Integer> selected = value -> {
                actualCalls.add(value);
                return Math.floorMod(value, 11) == remainder;
            };
            Collection<Integer> view = CollectionViews.filtered(values, selected);
            boolean expectedEmpty = true;
            List<Integer> referenceCalls = new ArrayList<>();
            for (Integer value : values) {
                referenceCalls.add(value);
                if (Math.floorMod(value, 11) == remainder) {
                    expectedEmpty = false;
                    break;
                }
            }
            check(view.isEmpty() == expectedEmpty, "selected terminal answer " + trial);
            check(actualCalls.equals(referenceCalls), "predicate order " + trial);
        }
    }

    public static void main(String[] args) {
        noSizeOrCachedPredicateResults();
        exceptionsAndNestedView();
        randomizedReference();
        System.out.println("PASS FilteredIsEmptyDirectTest checks=" + checks);
    }
}
