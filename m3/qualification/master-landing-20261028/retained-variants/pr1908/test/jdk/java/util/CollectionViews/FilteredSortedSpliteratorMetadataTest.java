/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
/*
 * @test
 * @summary M3JDK21 owner-backed filtered sorted/Set spliterators retain JDK21 ordering metadata
 * @run main/othervm FilteredSortedSpliteratorMetadataTest
 */
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.CollectionViews;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeSet;

public class FilteredSortedSpliteratorMetadataTest {
    private static int checks;

    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message + " check=" + checks);
    }

    /**
     * A SortedSet owner may have a Spliterator implementation that omits
     * sorted metadata. The view must still honor the authoritative owner
     * comparator, without sorting/copying the stored elements again.
     */
    private static final class MetadataSparseSortedSet<E> extends AbstractSet<E>
            implements SortedSet<E> {
        private final SortedSet<E> owner;
        MetadataSparseSortedSet(SortedSet<E> source) { owner = source; }
        @Override public boolean add(E value) { return owner.add(value); }
        @Override public Comparator<? super E> comparator() { return owner.comparator(); }
        @Override public SortedSet<E> subSet(E from, E to) {
            return new MetadataSparseSortedSet<>(owner.subSet(from, to));
        }
        @Override public SortedSet<E> headSet(E high) {
            return new MetadataSparseSortedSet<>(owner.headSet(high));
        }
        @Override public SortedSet<E> tailSet(E low) {
            return new MetadataSparseSortedSet<>(owner.tailSet(low));
        }
        @Override public E first() { return owner.first(); }
        @Override public E last() { return owner.last(); }
        @Override public Iterator<E> iterator() { return owner.iterator(); }
        @Override public int size() { return owner.size(); }
        @Override public Spliterator<E> spliterator() {
            return Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED);
        }
    }

    private static void ownerComparatorAndFlags() {
        for (int length = 0; length < 500; length++) {
            for (Comparator<Integer> order : List.of(
                    Comparator.<Integer>naturalOrder(), Comparator.<Integer>reverseOrder())) {
                TreeSet<Integer> underlying = new TreeSet<>(order);
                for (int i = 0; i < length; i++) underlying.add(i);
                SortedSet<Integer> owner = new MetadataSparseSortedSet<>(underlying);
                SortedSet<Integer> view = CollectionViews.filtered(owner, n -> n % 7 != 0);
                Spliterator<Integer> scan = view.spliterator();
                check(scan.hasCharacteristics(Spliterator.ORDERED | Spliterator.SORTED
                        | Spliterator.DISTINCT), "missing owner sorted characteristics");
                check(scan.getComparator() == order, "comparator identity lost");
                check(!scan.hasCharacteristics(Spliterator.SIZED | Spliterator.SUBSIZED),
                        "filtered unknown cardinality incorrectly sized");
                List<Integer> values = new ArrayList<>();
                scan.forEachRemaining(values::add);
                check(values.equals(underlying.stream().filter(n -> n % 7 != 0).toList()),
                        "filtered encounter order changed");
            }
        }
    }

    private static void splitInheritsComparator() {
        TreeSet<Integer> owner = new TreeSet<>(Comparator.reverseOrder());
        for (int i = 0; i < 5000; i++) owner.add(i);
        SortedSet<Integer> view = CollectionViews.filtered((SortedSet<Integer>) owner,
                n -> n % 3 == 0);
        Spliterator<Integer> scan = view.spliterator();
        Spliterator<Integer> prefix = scan.trySplit();
        check(prefix != null, "parent split missing");
        check(prefix.hasCharacteristics(Spliterator.ORDERED | Spliterator.SORTED
                | Spliterator.DISTINCT), "split loses sorted flags");
        check(prefix.getComparator() == owner.comparator(), "split comparator changed");
        ArrayList<Integer> all = new ArrayList<>();
        prefix.forEachRemaining(all::add);
        scan.forEachRemaining(all::add);
        check(all.equals(owner.stream().filter(n -> n % 3 == 0).toList()),
                "split traversal lost encounter order");
    }

    private static void setDistinctness() {
        Set<Integer> backing = new java.util.HashSet<>(List.of(1, 2, 3, 4, 5));
        Set<Integer> selected = CollectionViews.filtered(backing, n -> n > 2);
        check(selected.spliterator().hasCharacteristics(Spliterator.DISTINCT),
                "filtered Set loses DISTINCT contract");
    }

    public static void main(String[] args) {
        ownerComparatorAndFlags();
        splitInheritsComparator();
        setDistinctness();
        System.out.println("PASS FilteredSortedSpliteratorMetadataTest checks=" + checks);
    }
}
