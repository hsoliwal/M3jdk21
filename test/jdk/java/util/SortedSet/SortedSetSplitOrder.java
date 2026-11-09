/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
/*
 * @test
 * @summary Default SortedSet split batches preserve comparator metadata recursively
 * @run main SortedSetSplitOrder
 */
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.TreeSet;

public final class SortedSetSplitOrder {
    private static long checks;
    private SortedSetSplitOrder() { }

    private static final class DefaultSet<E> extends AbstractSet<E> implements SortedSet<E> {
        final SortedSet<E> owner;
        int comparatorReads;
        int iteratorReads;
        int sizeReads;
        DefaultSet(SortedSet<E> owner) { this.owner = owner; }
        @Override public Comparator<? super E> comparator() { comparatorReads++; return owner.comparator(); }
        @Override public Iterator<E> iterator() { iteratorReads++; return owner.iterator(); }
        @Override public int size() { sizeReads++; return owner.size(); }
        @Override public E first() { return owner.first(); }
        @Override public E last() { return owner.last(); }
        @Override public SortedSet<E> subSet(E a, E b) { return new DefaultSet<>(owner.subSet(a, b)); }
        @Override public SortedSet<E> headSet(E b) { return new DefaultSet<>(owner.headSet(b)); }
        @Override public SortedSet<E> tailSet(E a) { return new DefaultSet<>(owner.tailSet(a)); }
    }

    private static void check(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError(label);
    }

    private static void expect(Class<? extends Throwable> kind, Runnable call) {
        try { call.run(); } catch (Throwable actual) {
            check(actual.getClass() == kind, "exception: " + actual);
            return;
        }
        throw new AssertionError("missing " + kind.getName());
    }

    public static <E> void checkSplits(SortedSet<E> set) {
        Comparator<? super E> order = set.comparator();
        List<E> expected = new ArrayList<>(set);
        List<E> actual = new ArrayList<>();
        collect(set.spliterator(), order, actual);
        check(expected.equals(actual), "recursive encounter order");
    }

    private static <E> void collect(Spliterator<E> source, Comparator<? super E> order, List<E> result) {
        check(source.hasCharacteristics(Spliterator.SORTED | Spliterator.ORDERED | Spliterator.DISTINCT), "sorted characteristics");
        check(source.getComparator() == order, "split comparator lost");
        long before = source.estimateSize();
        Spliterator<E> prefix = source.trySplit();
        if (prefix == null) {
            long at = result.size();
            source.forEachRemaining(result::add);
            check(!source.tryAdvance(result::add), "exhaustion");
            check(result.size() - at == before, "leaf cardinality");
        } else {
            check(prefix.estimateSize() + source.estimateSize() == before, "split size conservation");
            collect(prefix, order, result);
            collect(source, order, result);
        }
    }

    private static void generated(boolean reversed) {
        for (Comparator<Integer> comparator : Arrays.<Comparator<Integer>>asList(null,
                Comparator.reverseOrder(), Comparator.comparingInt((Integer x) -> Math.abs(x)).thenComparingInt(x -> x),
                Comparator.nullsFirst(Comparator.naturalOrder()), Comparator.nullsLast(Comparator.reverseOrder()))) {
            for (int size : new int[]{0, 1, 2, 3, 63, 64, 65, 1023, 1024, 1025, 4097}) {
                TreeSet<Integer> values = new TreeSet<>(comparator);
                for (int i = 0; i < size; i++) values.add(i - size / 2);
                DefaultSet<Integer> set = new DefaultSet<>(values);
                checkSplits(set);
                check(new ArrayList<>(values).equals(set.parallelStream().toList()), "parallel order");
                check(values.stream().sorted().toList().equals(set.parallelStream().sorted().toList()), "parallel natural sort");
                if (size > 3) {
                    List<Integer> v = new ArrayList<>(values);
                    checkSplits(set.subSet(v.get(1), v.get(size - 1)));
                    checkSplits(set.headSet(v.get(size / 2)));
                    checkSplits(set.tailSet(v.get(size / 2)));
                    if (reversed) checkSplits(set.reversed());
                }
            }
        }
        TreeSet<Integer> nullable = new TreeSet<>(Comparator.nullsFirst(Comparator.reverseOrder()));
        nullable.addAll(Arrays.asList(null, 5, 3, -2));
        checkSplits(new DefaultSet<>(nullable));
        TreeSet<String> words = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        words.addAll(List.of("Ab", "aB", "cD", "Ef", "gH"));
        checkSplits(new DefaultSet<>(words));
    }

    private static void callbacksAndFailures() {
        TreeSet<Integer> values = new TreeSet<>(Comparator.reverseOrder());
        for (int i = 0; i < 100; i++) values.add(i);
        DefaultSet<Integer> set = new DefaultSet<>(values);
        Spliterator<Integer> root = set.spliterator();
        check(set.comparatorReads == 0 && set.sizeReads == 0 && set.iteratorReads == 0, "construction is lazy");
        values.add(100);
        Spliterator<Integer> prefix = root.trySplit();
        check(prefix != null && prefix.estimateSize() == 101, "late binding");
        check(set.comparatorReads == 0, "split does not call comparator");
        check(set.sizeReads == 1 && set.iteratorReads == 1, "source binding once");
        check(prefix.getComparator() == values.comparator() && set.comparatorReads == 1, "child comparator lazy");
        Spliterator<Integer> child = prefix.trySplit();
        check(child != null && set.comparatorReads == 1, "recursive split is lazy");
        check(child.getComparator() == values.comparator() && set.comparatorReads == 2, "recursive comparator");
        int reads = set.comparatorReads;
        child.forEachRemaining(x -> { });
        prefix.forEachRemaining(x -> { });
        root.forEachRemaining(x -> { });
        check(set.comparatorReads == reads, "traversal does not call comparator");
        expect(NullPointerException.class, () -> child.tryAdvance(null));
        expect(NullPointerException.class, () -> child.forEachRemaining(null));
        Spliterator<Integer> bound = set.spliterator();
        bound.estimateSize();
        values.add(101);
        expect(ConcurrentModificationException.class, () -> bound.tryAdvance(x -> { }));
        Spliterator<Integer> throwing = set.spliterator().trySplit();
        expect(IllegalStateException.class, () -> throwing.tryAdvance(x -> { throw new IllegalStateException("probe"); }));
        check(throwing.tryAdvance(x -> { }), "progress after consumer exception");
    }

    public static void main(String[] args) {
        generated(args.length == 0);
        callbacksAndFailures();
        System.out.println("SORTED_SET_SPLIT_PASS checks=" + checks);
    }
}
