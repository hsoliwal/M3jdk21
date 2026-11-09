/*
 * @test
 * @summary M3 java.util filtered views admit one-shot batches and own CopyOnWrite bulk removal
 * @run main FilteredOnePassCowSupersetTest
 */
/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.CollectionViews;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

public final class FilteredOnePassCowSupersetTest {
    private static long checks;
    private FilteredOnePassCowSupersetTest() { }

    private static void check(boolean value, String why) {
        checks++;
        if (!value) throw new AssertionError(why);
    }

    private static void eq(Object expected, Object actual, String why) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(why + ": expected=" + expected + ", actual=" + actual);
        }
    }

    private static final class Once<E> extends AbstractCollection<E> {
        private final List<E> values;
        private int opens;
        private Once(List<E> values) { this.values = values; }
        @Override public Iterator<E> iterator() {
            if (opens++ != 0) throw new AssertionError("source traversed twice");
            return values.iterator();
        }
        @Override public int size() { throw new AssertionError("eager source size"); }
        private int opens() { return opens; }
    }

    private static void onePassAdmission() {
        List<Integer> owner = new ArrayList<>(Arrays.asList(null, 0, 1, 2));
        Collection<Integer> filtered =
                CollectionViews.filtered((Collection<Integer>) owner,
                        value -> value == null || value % 2 == 0);
        Once<Integer> accepted = new Once<>(Arrays.asList(4, null, 6));
        check(filtered.addAll(accepted), "accepted one-shot batch");
        eq(1, accepted.opens(), "one iterator opening");
        eq(Arrays.asList(null, 0, 1, 2, 4, null, 6), owner,
                "null and order are preserved");
        Once<Integer> rejected = new Once<>(List.of(8, 9, 10));
        boolean refused = false;
        try {
            filtered.addAll(rejected);
        } catch (IllegalArgumentException expected) {
            refused = true;
        }
        check(refused, "rejected batch");
        eq(1, rejected.opens(), "rejection never restarts caller input");
        eq(Arrays.asList(null, 0, 1, 2, 4, null, 6), owner,
                "no partial owner write on invalid admission");
    }

    private static void cowListAndSet() {
        CopyOnWriteArrayList<Integer> owner =
                new CopyOnWriteArrayList<>(List.of(1, 2, 3, 4, 5, 6));
        Collection<Integer> filtered =
                CollectionViews.filtered((Collection<Integer>) owner, n -> n % 2 == 0);
        check(filtered.removeIf(n -> n >= 4), "COW list removeIf");
        eq(List.of(1, 2, 3, 5), List.copyOf(owner), "hidden odd values stay");
        check(filtered.removeAll(List.of(2)), "COW list removeAll");
        eq(List.of(1, 3, 5), List.copyOf(owner), "visible match removed");
        owner.addAll(List.of(7, 8, 9, 10));
        check(filtered.retainAll(List.of(10)), "COW list retainAll");
        eq(List.of(1, 3, 5, 7, 9, 10), List.copyOf(owner), "COW retains hidden values");
        filtered.clear();
        eq(List.of(1, 3, 5, 7, 9), List.copyOf(owner), "COW clear touches only selected");

        CopyOnWriteArraySet<Integer> set =
                new CopyOnWriteArraySet<>(List.of(1, 2, 3, 4, 5, 6));
        Collection<Integer> selected = CollectionViews.filtered(
                (Collection<Integer>) set, n -> n % 2 == 0);
        check(selected.removeIf(n -> n >= 4), "COW set removeIf");
        eq(List.of(1, 2, 3, 5), List.copyOf(set), "COW set order and hidden members");
        selected.clear();
        eq(List.of(1, 3, 5), List.copyOf(set), "COW set selected clear");
    }

    private static void sortedBackendUntouched() {
        NavigableSet<Integer> owner = new TreeSet<>(List.of(1, 2, 3, 4, 5, 6));
        NavigableSet<Integer> view = CollectionViews.filtered(owner, n -> n % 2 == 0);
        check(view.tailSet(2, true).removeIf(n -> n >= 4), "sorted range mutation");
        eq(List.of(1, 2, 3, 5), List.copyOf(owner), "backed comparator order");
        eq(2, view.first(), "visible first");
    }

    public static void main(String[] ignored) {
        onePassAdmission();
        cowListAndSet();
        sortedBackendUntouched();
        System.out.println("PASS FilteredOnePassCowSupersetTest checks=" + checks);
    }
}
