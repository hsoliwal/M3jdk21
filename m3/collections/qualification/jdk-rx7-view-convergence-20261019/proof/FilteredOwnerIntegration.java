/* Copyright (c) 2026, Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
import com.m3.collections.M3FilteredNavigableSet;
import com.m3.collections.M3FilteredViews;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;

/** Unchanged real M3 views over the patched JDK owner, including nullable navigation. */
public final class FilteredOwnerIntegration {
    private static long checks;
    private static boolean splits = true;
    private FilteredOwnerIntegration() { }
    private static void check(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError(label);
    }
    private static void readonly(Runnable call) {
        try { call.run(); } catch (UnsupportedOperationException expected) { checks++; return; }
        throw new AssertionError("mutation accepted");
    }
    private static Integer navigate(List<Integer> values, Integer query,
            Comparator<? super Integer> order, boolean forward, boolean inclusive) {
        Integer best = null;
        for (Integer value : values) {
            int c = order.compare(value, query);
            if (forward ? c > 0 || (inclusive && c == 0) : c < 0 || (inclusive && c == 0)) {
                if (forward) return value;
                best = value;
            }
        }
        return best;
    }
    private static void checkNavigation(NavigableSet<Integer> backing, Predicate<Integer> predicate) {
        for (NavigableSet<Integer> view : List.of(M3FilteredViews.filter(backing, predicate),
                M3FilteredNavigableSet.view(backing, predicate))) {
            List<Integer> expected = new ArrayList<>();
            for (Integer x : backing) if (predicate.test(x)) expected.add(x);
            check(expected.equals(new ArrayList<>(view)), "visible sequence");
            if (splits) {
                SortedSetSplitOrder.checkSplits(view);
                SortedSetSplitOrder.checkSplits(java.util.Collections.unmodifiableSortedSet(view));
                SortedSetSplitOrder.checkSplits(java.util.Collections.synchronizedSortedSet(view));
            }
            for (Integer q : Arrays.asList(null, -20, -4, -3, -2, -1, 0, 1, 2, 3, 4, 20)) {
                check(Objects.equals(navigate(expected, q, backing.comparator(), true, false), view.higher(q)), "higher " + q);
                check(Objects.equals(navigate(expected, q, backing.comparator(), true, true), view.ceiling(q)), "ceiling " + q);
                check(Objects.equals(navigate(expected, q, backing.comparator(), false, true), view.floor(q)), "floor " + q);
                check(Objects.equals(navigate(expected, q, backing.comparator(), false, false), view.lower(q)), "lower " + q);
            }
            readonly(() -> view.clear());
            readonly(() -> view.add(9));
            readonly(() -> view.remove(999));
            readonly(() -> view.removeIf(x -> false));
            readonly(() -> view.pollFirst());
            Iterator<Integer> cursor = view.iterator();
            if (cursor.hasNext()) { cursor.next(); readonly(cursor::remove); }
        }
    }
    private static void lazyAndLive() {
        TreeSet<Integer> data = new TreeSet<>(Comparator.reverseOrder());
        data.addAll(List.of(1, 2, 3, 4));
        int[] reads = {0};
        NavigableSet<Integer> view = M3FilteredViews.filter(data, x -> { reads[0]++; return x % 2 == 0; });
        Spliterator<Integer> root = view.spliterator();
        check(reads[0] == 0, "no eager predicate");
        data.add(6);
        root.getComparator();
        check(reads[0] == 0, "metadata does not evaluate predicate");
        List<Integer> actual = new ArrayList<>();
        Spliterator<Integer> prefix = root.trySplit();
        int boundReads = reads[0];
        check(prefix != null && prefix.getComparator() == data.comparator(), "filter split order");
        check(reads[0] == boundReads, "child comparator no predicate calls");
        prefix.forEachRemaining(actual::add);
        check(reads[0] == boundReads, "detached child does not reevaluate predicate");
        root.forEachRemaining(actual::add);
        check(actual.equals(List.of(6, 4, 2)), "late-bound live filtered sequence");
        check(reads[0] == boundReads + 1, "root preserves trailing rejected callback");
        data.add(8);
        check(view.first() == 8, "future operations see backing changes");
        TreeMap<Integer, String> map = new TreeMap<>(Comparator.reverseOrder());
        for (int i = 0; i < 40; i++) map.put(i, "v" + i);
        NavigableMap<Integer, String> filtered = M3FilteredViews.filterEntries(map, (k, v) -> k % 2 == 0);
        SortedSetSplitOrder.checkSplits(filtered.navigableKeySet());
        SortedSetSplitOrder.checkSplits(filtered.descendingKeySet());
        var entry = filtered.firstEntry();
        readonly(() -> entry.setValue("changed"));
        map.put(entry.getKey(), "later");
        check(entry.getValue().equals("v" + entry.getKey()), "entry snapshot lifetime");
    }
    public static void main(String[] args) {
        splits = args.length == 0;
        Comparator<Integer> order = Comparator.comparingInt(x -> x == null ? 0 : x);
        TreeSet<Integer> values = new TreeSet<>(order);
        values.addAll(Arrays.asList(-4, -3, -2, -1, null, 1, 2, 3, 4));
        for (int mask = 0; mask < 512; mask++) {
            final int selected = mask;
            Predicate<Integer> predicate = x -> (selected & (1 << (x == null ? 4 : x + 4))) != 0;
            for (NavigableSet<Integer> range : List.of(values, values.descendingSet(),
                    values.subSet(-3, true, 3, true), values.subSet(-3, false, 3, false).descendingSet(),
                    values.headSet(null, true), values.tailSet(null, false))) {
                checkNavigation(range, predicate);
            }
        }
        if (splits) lazyAndLive();
        System.out.println("FILTERED_OWNER_INTEGRATION_PASS checks=" + checks);
    }
}
