/*
 * @test
 * @summary Typed filtered traversal, saturated counts and stable reference snapshots
 * @modules java.base/java.util
 * @build RunSearchJni
 * @run main/othervm -Xss256k TraversalContractsTest all
 * @run main/othervm/timeout=180 TraversalContractsTest size
 */
// Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;

/** Stock-JDK patched-module proof is deliberately distinct from source-built JDK acceptance. */
public final class TraversalContractsTest {
    private static long checks;
    private static long tape = 1;
    private TraversalContractsTest() { }
    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
    private static void eq(Object actual, Object expected) {
        check(Objects.equals(actual, expected), "expected=" + expected + " actual=" + actual);
        tape = 31 * tape + Objects.hashCode(actual);
    }
    @FunctionalInterface private interface Action { void run() throws Exception; }
    private static void fails(Class<? extends Throwable> type, Action action) throws Exception {
        try { action.run(); }
        catch (Throwable failure) {
            if (failure instanceof InvocationTargetException e) failure = e.getCause();
            check(type.isInstance(failure), "wrong failure: " + failure); return;
        }
        throw new AssertionError("missing failure: " + type);
    }
    /** Explicit weak-metadata fixture: value/order owner remains a real JDK set. */
    private static class ThinSet<E> extends AbstractSet<E> {
        final Set<E> owner;
        ThinSet(Set<E> owner) { this.owner = owner; }
        @Override public int size() { return owner.size(); }
        @Override public Iterator<E> iterator() { return owner.iterator(); }
        @Override public Spliterator<E> spliterator() {
            return Spliterators.spliteratorUnknownSize(iterator(), 0);
        }
    }
    private static final class ThinSorted<E> extends ThinSet<E> implements SortedSet<E> {
        private final SortedSet<E> sorted;
        ThinSorted(SortedSet<E> sorted) { super(sorted); this.sorted = sorted; }
        @Override public Comparator<? super E> comparator() { return sorted.comparator(); }
        @Override public E first() { return sorted.first(); }
        @Override public E last() { return sorted.last(); }
        @Override public SortedSet<E> subSet(E from, E to) { return new ThinSorted<>(sorted.subSet(from, to)); }
        @Override public SortedSet<E> headSet(E to) { return new ThinSorted<>(sorted.headSet(to)); }
        @Override public SortedSet<E> tailSet(E from) { return new ThinSorted<>(sorted.tailSet(from)); }
    }
    private static <E> void metadata(Spliterator<E> cursor, int facts, Comparator<? super E> comparator) {
        check(cursor.hasCharacteristics(facts), "MISSING_TYPED_CHARACTERISTICS flags=" + cursor.characteristics());
        check((cursor.characteristics() & (Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.CONCURRENT)) == 0,
                "false size/concurrency promise");
        eq(cursor.getExactSizeIfKnown(), -1L);
        if ((facts & Spliterator.SORTED) != 0) check(cursor.getComparator() == comparator, "COMPARATOR_IDENTITY");
    }
    private static <E> void split(Spliterator<E> cursor, int facts, Comparator<? super E> comparator,
            List<E> output, int depth) {
        metadata(cursor, facts, comparator);
        Spliterator<E> prefix = depth < 12 ? cursor.trySplit() : null;
        if (prefix != null) { split(prefix, facts, comparator, output, depth + 1); split(cursor, facts, comparator, output, depth + 1); }
        else cursor.forEachRemaining(output::add);
    }
    private static void typedMetadata() throws Exception {
        int sortedFacts = Spliterator.DISTINCT | Spliterator.SORTED | Spliterator.ORDERED;
        List<Comparator<Integer>> orders = Arrays.asList(null, Comparator.reverseOrder(),
                Comparator.nullsFirst(Comparator.naturalOrder()), Comparator.nullsLast(Comparator.naturalOrder()),
                Comparator.comparingInt((Integer i) -> i == null ? 0 : i < 0 ? i : i + 1));
        Random random = new Random(0x74797065644cL);
        for (int seed = 0; seed < 64; seed++) {
            for (Comparator<Integer> order : orders) {
                NavigableSet<Integer> owner = new TreeSet<>(order);
                for (int i = 0; i < 70; i++) owner.add(random.nextInt(100) - 50);
                if (order != null && order != orders.get(1)) owner.add(null);
                List<NavigableSet<Integer>> ranges = new ArrayList<>();
                ranges.add(owner); ranges.add(owner.descendingSet());
                if (!owner.isEmpty()) {
                    ranges.add(owner.tailSet(owner.first(), false));
                    ranges.add(owner.headSet(owner.last(), true).descendingSet());
                }
                Predicate<Integer> predicate = i -> i == null || (i & 3) != 1;
                for (NavigableSet<Integer> range : ranges) {
                    NavigableSet<Integer> view = CollectionViews.filtered(CollectionViews.filtered(range, i -> true), predicate);
                    List<Integer> expected = new ArrayList<>();
                    for (Integer i : range) if (predicate.test(i)) expected.add(i);
                    List<Integer> actual = new ArrayList<>();
                    split(view.spliterator(), sortedFacts, range.comparator(), actual, 0);
                    eq(actual, expected); eq(view.parallelStream().toList(), expected);
                    eq(view.stream().distinct().toList(), expected);
                    Collection<Integer> erased = CollectionViews.filtered((Collection<Integer>) view, i -> true);
                    check(erased instanceof NavigableSet<?>, "lost runtime capability");
                    metadata(erased.spliterator(), sortedFacts, range.comparator());
                    SortedSet<Integer> thin = CollectionViews.filtered((SortedSet<Integer>) new ThinSorted<>(range), predicate);
                    actual.clear(); split(thin.spliterator(), sortedFacts, range.comparator(), actual, 0); eq(actual, expected);
                }
            }
        }
        Set<Integer> set = CollectionViews.filtered(new ThinSet<>(new LinkedHashSet<>(List.of(1,2,3,4))), i -> (i & 1) == 0);
        List<Integer> selected = new ArrayList<>(); split(set.spliterator(), Spliterator.DISTINCT, null, selected, 0);
        eq(selected, List.of(2,4));
        Collection<Integer> list = CollectionViews.filtered(Arrays.asList(1,1,2,2,3), i -> i < 3);
        check(!list.spliterator().hasCharacteristics(Spliterator.DISTINCT), "list duplicates falsely distinct");
        eq(list.stream().distinct().toList(), List.of(1,2));
        fails(IllegalStateException.class, () -> list.spliterator().getComparator());
        AtomicInteger predicates = new AtomicInteger();
        NavigableSet<Integer> view = CollectionViews.filtered(new TreeSet<>(List.of(1,2,3)), x -> { predicates.incrementAndGet(); return true; });
        Spliterator<Integer> cursor = view.spliterator();
        metadata(cursor, sortedFacts, null); cursor.estimateSize(); cursor.trySplit();
        eq(predicates.get(), 0); fails(NullPointerException.class, () -> cursor.tryAdvance(null)); eq(predicates.get(), 0);
        RuntimeException failure = new IllegalArgumentException("same predicate exception");
        Collection<Integer> failing = CollectionViews.filtered(List.of(1), i -> { throw failure; });
        try { failing.spliterator().tryAdvance(i -> { }); throw new AssertionError("missing predicate exception"); }
        catch (RuntimeException ex) { check(ex == failure, "predicate exception identity"); }
    }
    private record Tagged(int key, int occurrence) { }
    private static void legacy() throws Exception {
        Random random = new Random(0x6c6567616379L);
        for (int seed = 0; seed < 128; seed++) {
            ArrayList<Integer> owner = new ArrayList<>();
            for (int i = 0; i < 80; i++) owner.add(random.nextInt(40));
            int threshold = random.nextInt(40);
            Predicate<Integer> predicate = i -> i % 2 == 0 && i >= threshold;
            Collection<Integer> view = CollectionViews.filtered(CollectionViews.filtered(owner, i -> i % 2 == 0), i -> i >= threshold);
            List<Integer> expected = owner.stream().filter(predicate).toList();
            eq(view.stream().toList(), expected); eq(view.parallelStream().toList(), expected);
            eq(view.size(), expected.size()); eq(view.isEmpty(), expected.isEmpty());
            for (int i = 0; i < 40; i++) eq(view.contains(i), expected.contains(i));
            eq(CollectionViews.filteredSnapshot(owner, predicate), expected);
            eq(CollectionViews.filteredSortedSnapshot(owner, predicate, null), expected.stream().sorted().toList());
            NavigableSet<Integer> root = new TreeSet<>(owner);
            NavigableSet<Integer> range = root.subSet(5, true, 35, false).descendingSet();
            NavigableSet<Integer> filter = CollectionViews.filtered(range, predicate);
            NavigableSet<Integer> oracle = new TreeSet<>(range.comparator());
            for (Integer i : range) if (predicate.test(i)) oracle.add(i);
            for (int query = -2; query < 42; query++) {
                eq(filter.lower(query), oracle.lower(query)); eq(filter.floor(query), oracle.floor(query));
                eq(filter.ceiling(query), oracle.ceiling(query)); eq(filter.higher(query), oracle.higher(query));
            }
            view.clear(); check(owner.stream().noneMatch(predicate), "visible clear failed");
        }
        List<Tagged> rows = List.of(new Tagged(2,0),new Tagged(1,1),new Tagged(2,2),new Tagged(1,3));
        Collection<Tagged> noSize = new AbstractCollection<>() {
            @Override public int size() { throw new AssertionError("source size prepass"); }
            @Override public Iterator<Tagged> iterator() { return rows.iterator(); }
        };
        AtomicInteger calls = new AtomicInteger();
        List<Tagged> sorted = CollectionViews.filteredSortedSnapshot(noSize, i -> { calls.incrementAndGet(); return true; }, Comparator.comparingInt(Tagged::key));
        eq(calls.get(), rows.size()); eq(sorted.stream().map(Tagged::occurrence).toList(), List.of(1,3,0,2));
        check(sorted.getFirst() == rows.get(1), "reference identity copied");
        fails(UnsupportedOperationException.class, () -> sorted.add(rows.getFirst()));
        ArrayList<Integer> root = new ArrayList<>(Arrays.asList(null,0,1,2,3,4));
        Collection<Integer> even = CollectionViews.filtered(root, i -> i == null || (i & 1) == 0);
        eq(even.iterator().next(), null); fails(UnsupportedOperationException.class, () -> even.iterator().remove());
        fails(IllegalArgumentException.class, () -> even.addAll(Arrays.asList(6,7))); eq(root.size(),6);
        even.removeIf(i -> i != null && i > 0); eq(root,Arrays.asList(null,0,1,3));
        check(even.remove(null),"null removal"); eq(root,List.of(0,1,3));
        Collection<Integer> deep = List.of(42);
        for (int i = 0; i < 30000; i++) deep = CollectionViews.filtered(deep, Objects::nonNull);
        eq(deep.stream().toList(),List.of(42)); eq(deep.size(),1);
        check(CollectionViews.class.getClassLoader() == null,"not bootstrap loaded");
        eq(CollectionViews.class.getModule().getName(),"java.base");
    }
    private static void size() {
        long occurrences = (long) Integer.MAX_VALUE + 17;
        long[] called = {0};
        Collection<Integer> virtual = new AbstractCollection<>() {
            @Override public int size() { return Integer.MAX_VALUE; }
            @Override public Iterator<Integer> iterator() {
                return new Iterator<>() {
                    private long remaining = occurrences;
                    @Override public boolean hasNext() { return remaining != 0; }
                    @Override public Integer next() {
                        if (remaining == 0) throw new NoSuchElementException(); remaining--; return 1;
                    }
                };
            }
        };
        int count = CollectionViews.filtered(virtual, i -> { called[0]++; return true; }).size();
        eq(called[0], occurrences);
        check(count == Integer.MAX_VALUE, "COUNT_SATURATION actual=" + count);
        System.out.println("SATURATED occurrences=" + called[0] + " size=" + count);
    }
    private static void jni(String library) throws Exception {
        System.load(library);
        Method first = RunSearchJni.class.getDeclaredMethod("first",int[].class,int.class,int.class); first.setAccessible(true);
        Random random = new Random(261017);
        for (int length : new int[]{0,1,2,511,512,513,1024,1025,16384,65536}) {
            TreeSet<Integer> source = new TreeSet<>(); for (int i = 0; i < length; i++) source.add(i);
            NavigableSet<Integer> filtered = CollectionViews.filtered(source, i -> (i & 7) < 3);
            List<Integer> ordered = CollectionViews.sortedSnapshot(filtered.descendingSet(), null);
            int[] pairs = new int[2 * ordered.size() + 8]; int runs = 0;
            for (int value : ordered) {
                if (runs != 0 && pairs[2*runs-1] == value) pairs[2*runs-1]++;
                else { pairs[2*runs] = value; pairs[2*runs+1] = value+1; runs++; }
            }
            pairs[2*runs] = -99; pairs[2*runs+1] = Integer.MIN_VALUE;
            int[] original = pairs.clone();
            for (int i = 0; i < 258; i++) {
                int query = i == 0 ? Integer.MIN_VALUE : i == 1 ? Integer.MAX_VALUE : random.nextInt(65540)-2;
                int expected = 0; while (expected < runs && pairs[2*expected+1] <= query) expected++;
                eq(first.invoke(null,pairs,runs,query),expected);
            }
            check(Arrays.equals(pairs,original),"native input mutation");
            if (runs > 0) {
                pairs[2*runs-1] = 65537; int n = runs;
                fails(IllegalArgumentException.class, () -> first.invoke(null,pairs,n,-1));
            }
        }
        for (int[] malformed : new int[][]{null,{0},{1,1},{0,65537},{3,9,1,2},{0,2,2,3}}) {
            int n = malformed == null ? 0 : Math.max(1,malformed.length/2);
            fails(IllegalArgumentException.class, () -> first.invoke(null,malformed,n,0));
        }
        fails(IllegalArgumentException.class, () -> first.invoke(null,new int[0],-1,0));
    }
    public static void main(String[] args) throws Exception {
        String mode = args.length == 0 ? "all" : args[0];
        switch (mode) {
            case "all" -> { legacy(); typedMetadata(); }
            case "legacy" -> legacy();
            case "metadata" -> typedMetadata();
            case "size" -> size();
            case "jni" -> jni(args[1]);
            default -> throw new IllegalArgumentException("unknown test mode");
        }
        System.out.println("PASS mode="+mode+" checks="+checks+" tape="+Long.toHexString(tape));
    }
}
