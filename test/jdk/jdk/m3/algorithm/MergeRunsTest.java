// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * mergeSorted over runs with a packed-entry heap, and insert over an array snapshot, against the
 * previous methods (kept below verbatim): the same output (identity order among equal keys), the
 * same comparator call sequence and the same monitor ticks, on ArrayList and LinkedList inputs.
 */
//FLLDCIM: LEETCODIFY — differential proof for mergeSorted and insert
final class MergeRunsTest {

    record Item(int key, int id) {}

    /** The previous mergeSorted, verbatim apart from its name. */
    static <T> List<T> referenceMerge(List<? extends List<T>> lists,
            Comparator<? super T> comparator, ProgressMonitor monitor) {
        Objects.requireNonNull(lists, "lists");
        Objects.requireNonNull(comparator, "comparator");
        AlgorithmSupport.begin(monitor, "merge-k-sorted", ProgressMonitor.UNKNOWN);
        try {
            record Cursor<T>(int list, int index, T value) {}
            PriorityQueue<Cursor<T>> heap = new PriorityQueue<>(
                    (left, right) -> comparator.compare(left.value(), right.value()));
            for (int list = 0; list < lists.size(); list++) {
                List<T> values = lists.get(list);
                if (!values.isEmpty()) {
                    heap.add(new Cursor<>(list, 0, values.getFirst()));
                }
            }
            List<T> result = new ArrayList<>();
            while (!heap.isEmpty()) {
                AlgorithmSupport.tick(monitor);
                Cursor<T> cursor = heap.remove();
                result.add(cursor.value());
                int next = cursor.index() + 1;
                List<T> source = lists.get(cursor.list());
                if (next < source.size()) {
                    heap.add(new Cursor<>(cursor.list(), next, source.get(next)));
                }
            }
            return List.copyOf(result);
        } finally {
            monitor.done();
        }
    }

    /** The previous insert, verbatim apart from its name. */
    static List<IntervalGreedyAlgorithms.Interval> referenceInsert(
            List<IntervalGreedyAlgorithms.Interval> sortedNonOverlapping,
            IntervalGreedyAlgorithms.Interval inserted, ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "insert-interval", sortedNonOverlapping.size());
        try {
            List<IntervalGreedyAlgorithms.Interval> result =
                    new ArrayList<>(sortedNonOverlapping.size() + 1);
            int index = 0;
            while (index < sortedNonOverlapping.size()
                    && sortedNonOverlapping.get(index).end() < inserted.start()) {
                AlgorithmSupport.tick(monitor);
                result.add(sortedNonOverlapping.get(index++));
            }
            int start = inserted.start();
            int end = inserted.end();
            while (index < sortedNonOverlapping.size()
                    && sortedNonOverlapping.get(index).start() <= end) {
                AlgorithmSupport.tick(monitor);
                IntervalGreedyAlgorithms.Interval current = sortedNonOverlapping.get(index++);
                start = Math.min(start, current.start());
                end = Math.max(end, current.end());
            }
            result.add(new IntervalGreedyAlgorithms.Interval(start, end));
            while (index < sortedNonOverlapping.size()) {
                AlgorithmSupport.tick(monitor);
                result.add(sortedNonOverlapping.get(index++));
            }
            return List.copyOf(result);
        } finally {
            monitor.done();
        }
    }

    static final class Ticks implements ProgressMonitor {
        int ticks;
        @Override public void worked(int work) {
            ticks += work;
        }
    }

    interface Merge {
        List<Item> run(List<List<Item>> lists, Comparator<Item> comparator, ProgressMonitor m);
    }

    static String merge(Merge merge, List<List<Item>> lists) {
        StringBuilder calls = new StringBuilder();
        Comparator<Item> byKey = (a, b) -> {
            calls.append(a.id()).append('/').append(b.id()).append(' ');
            return Integer.compare(a.key(), b.key());
        };
        Ticks ticks = new Ticks();
        return merge.run(lists, byKey, ticks) + " | " + calls + " | " + ticks.ticks;
    }

    @Test
    void mergeAgainstPriorityQueue() {
        for (int seed = 0; seed < 3_000; seed++) {
            Random random = new Random(seed);
            List<List<Item>> lists = new ArrayList<>();
            int id = 0;
            for (int l = random.nextInt(9); l > 0; l--) {
                List<Item> run = random.nextBoolean() ? new ArrayList<>() : new LinkedList<>();
                int key = random.nextInt(5);
                for (int i = random.nextInt(seed % 30 == 0 ? 300 : 12); i > 0; i--) {
                    key += random.nextInt(3);
                    run.add(new Item(key, id++));
                }
                lists.add(run);
            }
            assertEquals(merge(MergeRunsTest::referenceMerge, lists),
                    merge(HeapAlgorithms::mergeSorted, lists), "seed " + seed);
        }
    }

    @Test
    void insertAgainstIndexedScan() {
        for (int seed = 0; seed < 3_000; seed++) {
            Random random = new Random(seed);
            List<IntervalGreedyAlgorithms.Interval> intervals =
                    random.nextBoolean() ? new ArrayList<>() : new LinkedList<>();
            int at = random.nextInt(10);
            for (int i = random.nextInt(15); i > 0; i--) {
                int start = at + 1 + random.nextInt(4);
                at = start + random.nextInt(5);
                intervals.add(new IntervalGreedyAlgorithms.Interval(start, at));
            }
            int start = random.nextInt(at + 10);
            var inserted = new IntervalGreedyAlgorithms.Interval(start, start + random.nextInt(20));
            Ticks a = new Ticks();
            Ticks b = new Ticks();
            assertEquals(referenceInsert(intervals, inserted, a) + " " ,
                    IntervalGreedyAlgorithms.insert(intervals, inserted, b) + " ", "seed " + seed);
            assertEquals(a.ticks, b.ticks, "seed " + seed + " ticks");
        }
    }
}
