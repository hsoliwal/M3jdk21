/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */

/*
 * @test
 * @summary M3JDK21 native indexed-array live state-mask views and stable sorted positions
 * @modules java.base/jdk.internal.m3
 * @run main/othervm M3StateMaskedIndexedViewTest
 * @run main/othervm -Xint M3StateMaskedIndexedViewTest
 */
import jdk.internal.m3.M3IndexedArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;

public class M3StateMaskedIndexedViewTest {
    private static int checks;
    private record Item(int rank, int ordinal) { }

    public static void main(String[] args) {
        visibilityAndIdentity();
        epochSeparation();
        comparatorOrderAndSafety();
        randomizedLeaseOracle();
        System.out.println("M3StateMaskedIndexedViewTest PASS checks=" + checks);
    }

    private static void visibilityAndIdentity() {
        M3IndexedArray<Item> owner = new M3IndexedArray<>();
        Item a = new Item(2, 0);
        Item b = new Item(1, 1);
        Item c = new Item(2, 2);
        long ah = owner.append(a);
        long bh = owner.append(b);
        long ch = owner.append(c);

        List<Item> unleased = owner.maskedStates(M3IndexedArray.LIVE, M3IndexedArray.LEASED);
        List<Item> shared = owner.maskedStates(M3IndexedArray.SHARED, 0);
        sameIdentity(List.of(a, b, c), unleased, "all live unleased");
        sameIdentity(List.of(), shared, "no shared slot");
        owner.retain(bh);
        sameIdentity(List.of(a, c), unleased, "leased slot excluded");
        sameIdentity(List.of(), shared, "single lease not shared");
        owner.retain(bh);
        sameIdentity(List.of(b), shared, "second lease sets shared bit");
        require((owner.stateMask(bh) & M3IndexedArray.SHARED) != 0, "shared mask");
        owner.release(bh);
        sameIdentity(List.of(), shared, "shared bit cleared");
        owner.release(bh);
        sameIdentity(List.of(a, b, c), unleased, "released slot visible");
        expect(IllegalArgumentException.class, () -> owner.maskedStates(1, 1));
        expect(IllegalArgumentException.class,
                () -> owner.maskedSortedStates(0x80000000, 0x80000000, Comparator.comparingInt(Item::rank)));
        expect(NullPointerException.class, () -> owner.maskedSortedStates(0, 0, null));
        sameIdentity(List.of(), owner.maskedStates(0x80000000, 0), "unknown high bit selects no row");
        sameIdentity(List.of(a, b, c), owner.maskedStates(0, 0x80000000),
                "unknown high bit excludes no row");

        require(owner.remove(ah), "remove first");
        expect(IllegalArgumentException.class, () -> owner.get(ah));
        Item d = new Item(9, 3);
        long dh = owner.append(d);
        require((int) dh == (int) ah && dh != ah, "slot reuse never resurrects handle");
        sameIdentity(List.of(b, c, d), unleased, "logical order independent of slot reuse");
        require(owner.handleAt(0) == bh && owner.handleAt(1) == ch, "physical positions retained");
    }

    private static void epochSeparation() {
        M3IndexedArray<Integer> owner = new M3IndexedArray<>();
        long first = owner.append(2);
        owner.append(1);
        List<Integer> legacy = owner.sorted(Integer::compare);
        List<Integer> aware = owner.maskedSortedStates(M3IndexedArray.LIVE,
                M3IndexedArray.LEASED, Integer::compare);
        Iterator<Integer> legacyIterator = legacy.iterator();
        Iterator<Integer> awareIterator = aware.iterator();
        List<Integer> oldRange = aware.subList(0, 1);
        Spliterator<Integer> oldSplit = aware.spliterator();
        require(oldSplit.estimateSize() == 2, "state split binds before mutation");
        long unchangedValueEpoch = owner.version();
        owner.retain(first);
        require(owner.version() == unchangedValueEpoch, "leases leave value epoch unchanged");
        require(legacyIterator.next() == 1, "value-only iterator survives lease");
        expect(ConcurrentModificationException.class, awareIterator::hasNext);
        expect(ConcurrentModificationException.class, oldRange::size);
        expect(ConcurrentModificationException.class, oldSplit::estimateSize);
        sameIdentity(List.of(1), aware, "state-dependent root refreshes after lease");
        owner.release(first);
        sameIdentity(List.of(1, 2), aware, "state-dependent root refreshes after release");
        Iterator<Integer> next = aware.iterator();
        owner.replace(first, 4);
        expect(ConcurrentModificationException.class, next::next);
        sameIdentity(List.of(1, 4), aware, "value replacement still invalidates all views");
    }

    private static void comparatorOrderAndSafety() {
        M3IndexedArray<Item> owner = new M3IndexedArray<>();
        Item[] a = new Item[64];
        long[] handles = new long[a.length];
        for (int i = 0; i < a.length; i++) {
            a[i] = new Item(i % 7, i);
            handles[i] = owner.append(a[i]);
        }
        for (int i = 0; i < a.length; i += 3) owner.retain(handles[i]);
        List<Item> sorted = owner.maskedSortedStates(M3IndexedArray.LIVE,
                M3IndexedArray.LEASED, Comparator.comparingInt(Item::rank));
        ArrayList<Item> expected = new ArrayList<>();
        for (int i = 0; i < a.length; i++) if (i % 3 != 0) expected.add(a[i]);
        expected.sort(Comparator.comparingInt(Item::rank));
        sameIdentity(expected, sorted, "stable physical-index merge order");

        ArrayList<Item> split = new ArrayList<>();
        collectSplits(sorted.spliterator(), split);
        sameIdentity(expected, split, "split over primitive directory");
        ListIterator<Item> reverse = sorted.listIterator(sorted.size());
        for (int i = expected.size() - 1; i >= 0; i--) {
            require(reverse.previous() == expected.get(i), "reverse index identity");
        }
        AtomicInteger predicates = new AtomicInteger();
        // Value-only APIs do not inherit state-sensitive invalidation.
        List<Item> valueOnly = owner.filtered(item -> {
            predicates.incrementAndGet();
            return true;
        });
        require(valueOnly.size() == a.length, "old predicate API unchanged");
        int oldVisits = predicates.get();
        owner.release(handles[0]);
        require(valueOnly.size() == a.length && predicates.get() == oldVisits,
                "lease-only change does not rerun legacy predicate");
        sameIdentity(List.of(a[0]), owner.maskedStates(M3IndexedArray.LIVE,
                M3IndexedArray.LEASED).subList(0, 1), "released item appears");

        M3IndexedArray<Integer> reentrant = new M3IndexedArray<>();
        long handle = reentrant.append(2);
        reentrant.append(1);
        List<Integer> changing = reentrant.maskedSortedStates(M3IndexedArray.LIVE, 0,
                (l, r) -> {
                    reentrant.retain(handle);
                    return Integer.compare(l, r);
                });
        expect(ConcurrentModificationException.class, changing::size);
        reentrant.release(handle);
    }

    private static void randomizedLeaseOracle() {
        Random random = new Random(0x4d3353544cL);
        M3IndexedArray<Item> owner = new M3IndexedArray<>();
        ArrayList<Item> logical = new ArrayList<>();
        ArrayList<Long> handles = new ArrayList<>();
        ArrayList<Integer> leases = new ArrayList<>();
        List<Item> free = owner.maskedStates(M3IndexedArray.LIVE, M3IndexedArray.LEASED);
        List<Item> shared = owner.maskedStates(M3IndexedArray.SHARED, 0);
        List<Item> sorted = owner.maskedSortedStates(M3IndexedArray.LIVE,
                M3IndexedArray.LEASED, Comparator.comparingInt(Item::rank));
        int ordinal = 0;
        for (int run = 0; run < 1200; run++) {
            int op = handles.isEmpty() ? 0 : random.nextInt(5);
            if (op == 0) {
                Item item = new Item(random.nextInt(23) - 11, ordinal++);
                long handle = owner.append(item);
                logical.add(item);
                handles.add(handle);
                leases.add(0);
            } else if (op == 1) {
                int index = random.nextInt(handles.size());
                owner.retain(handles.get(index));
                leases.set(index, leases.get(index) + 1);
            } else if (op == 2) {
                int index = random.nextInt(handles.size());
                if (leases.get(index) > 0) {
                    owner.release(handles.get(index));
                    leases.set(index, leases.get(index) - 1);
                }
            } else if (op == 3) {
                int index = random.nextInt(handles.size());
                if (leases.get(index) == 0) {
                    Item value = new Item(random.nextInt(23) - 11, ordinal++);
                    require(owner.replace(handles.get(index), value), "random replace");
                    logical.set(index, value);
                }
            } else {
                int index = random.nextInt(handles.size());
                if (leases.get(index) == 0) {
                    require(owner.remove(handles.remove(index)), "random remove");
                    logical.remove(index);
                    leases.remove(index);
                }
            }
            if (run % 7 == 0) {
                ArrayList<Item> available = new ArrayList<>();
                ArrayList<Item> multiplyLeased = new ArrayList<>();
                for (int index = 0; index < logical.size(); index++) {
                    if (leases.get(index) == 0) available.add(logical.get(index));
                    if (leases.get(index) > 1) multiplyLeased.add(logical.get(index));
                }
                sameIdentity(available, free, "random unleased projection");
                sameIdentity(multiplyLeased, shared, "random shared projection");
                available.sort(Comparator.comparingInt(Item::rank));
                sameIdentity(available, sorted, "random stable sorted projection");
            }
        }
    }

    private static <T> void collectSplits(Spliterator<T> source, List<T> into) {
        Spliterator<T> prefix = source.trySplit();
        if (prefix != null) {
            collectSplits(prefix, into);
            collectSplits(source, into);
        } else {
            source.forEachRemaining(into::add);
        }
    }

    private static <T> void sameIdentity(List<T> expected, List<T> actual, String message) {
        require(expected.size() == actual.size(), message + " size expected="
                + expected.size() + " actual=" + actual.size());
        for (int i = 0; i < expected.size(); i++) {
            require(expected.get(i) == actual.get(i), message + " index " + i);
        }
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try {
            action.run();
        } catch (Throwable caught) {
            if (type.isInstance(caught)) return;
            throw new AssertionError("expected " + type.getSimpleName() + ": " + caught, caught);
        }
        throw new AssertionError("expected " + type.getSimpleName());
    }

    private static void require(boolean ok, String reason) {
        checks++;
        if (!ok) throw new AssertionError(reason);
    }
}
