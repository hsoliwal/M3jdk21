/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary Independent contracts for lazy stepped collate windows, tail policies,
 *          demand, replay, immutable snapshots, validation, and resource ownership
 * @modules jdk.m3.collections java.base/java.util.function.ext
 * @run main/othervm ObjectStreamCollateTest
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jdk.m3.collections.ObjectStream;
import jdk.m3.collections.ObjectStream.TailPolicy;

public final class ObjectStreamCollateTest {
    private static int checks;

    private ObjectStreamCollateTest() {
    }

    public static void main(String[] args) {
        finiteSliceOracle();
        capacityAndOrdinalBoundaries();
        defaultOverloads();
        eagerValidation();
        snapshotsAndReferences();
        replayAndLiveSources();
        demandAndGaps();
        finiteAndInfiniteShortCircuit();
        spliteratorContract();
        closeAndFailureOwnership();
        existingWindowsAndBatches();
        System.out.println("OBJECT_STREAM_COLLATE_PASS checks=" + checks);
    }

    /*
     * This oracle deliberately uses source indices and ordinary subList copies.
     * It shares neither a ring nor a source-iterator state machine with collate.
     */
    private static <T> List<List<T>> reference(
            List<T> source, int width, int step, TailPolicy policy) {
        List<List<T>> result = new ArrayList<>();
        for (long start = 0; start < source.size(); start += step) {
            int end = (int) Math.min((long) source.size(), start + width);
            if (policy == TailPolicy.DROP && end - start < width) break;
            result.add(new ArrayList<>(source.subList((int) start, end)));
        }
        return result;
    }

    private static void finiteSliceOracle() {
        for (int length = 0; length <= 40; length++) {
            List<Integer> values = new ArrayList<>();
            for (int i = 0; i < length; i++) values.add(i % 7 == 3 ? null : i % 5);
            ObjectStream<Integer> source = ObjectStream.snapshot(values);
            for (int width = 1; width <= 17; width++) {
                for (int step = 1; step <= 19; step++) {
                    for (TailPolicy policy : TailPolicy.values()) {
                        equal(reference(values, width, step, policy),
                                source.collate(width, step, policy).toBoundedList(length),
                                "slice oracle n=" + length + " width=" + width
                                        + " step=" + step + " tail=" + policy);
                    }
                }
            }
        }

        Random random = new Random(0x47524f4f56594cL);
        for (int trial = 0; trial < 600; trial++) {
            int length = random.nextInt(181);
            int width = 1 + random.nextInt(80);
            int step = 1 + random.nextInt(100);
            List<Integer> values = new ArrayList<>();
            for (int i = 0; i < length; i++) {
                values.add(random.nextInt(5) == 0 ? null : random.nextInt(11));
            }
            for (TailPolicy policy : TailPolicy.values()) {
                equal(reference(values, width, step, policy),
                        ObjectStream.snapshot(values).collate(width, step, policy)
                                .toBoundedList(length),
                        "seeded slice oracle trial=" + trial + " tail=" + policy);
            }
        }
    }

    private static void capacityAndOrdinalBoundaries() {
        List<Integer> values = sequence(173);
        int[] widths = {15, 16, 17, 31, 32, 33, 63, 64, 65, 127, 128, 129, 65_536};
        int[] steps = {1, 2, 15, 16, 17, 31, 63, 129, 174, Integer.MAX_VALUE};
        for (int width : widths) {
            for (int step : steps) {
                for (TailPolicy policy : TailPolicy.values()) {
                    equal(reference(values, width, step, policy),
                            ObjectStream.snapshot(values).collate(width, step, policy)
                                    .toBoundedList(values.size()),
                            "growth boundary width=" + width + " step=" + step
                                    + " tail=" + policy);
                }
            }
        }
        equal(List.of(List.of(0)), ObjectStream.snapshot(List.of(0))
                .collate(65_536, Integer.MAX_VALUE).toBoundedList(1),
                "maximum width and step preserve short included tail");
        equal(List.of(), ObjectStream.snapshot(List.of(0))
                .collate(65_536, Integer.MAX_VALUE, TailPolicy.DROP).toBoundedList(0),
                "maximum width drops short tail");
    }

    private static void defaultOverloads() {
        for (int length = 0; length <= 19; length++) {
            List<Integer> source = sequence(length);
            for (int width = 1; width <= 9; width++) {
                equal(reference(source, width, width, TailPolicy.INCLUDE),
                        ObjectStream.snapshot(source).collate(width).toBoundedList(length),
                        "one-argument overload");
                for (int step = 1; step <= 9; step++) {
                    equal(reference(source, width, step, TailPolicy.INCLUDE),
                            ObjectStream.snapshot(source).collate(width, step)
                                    .toBoundedList(length),
                            "two-argument overload");
                }
            }
        }
        equal(List.of(List.of(1, 2, 3), List.of(2, 3, 4), List.of(3, 4, 5),
                        List.of(4, 5), List.of(5)),
                ObjectStream.snapshot(List.of(1, 2, 3, 4, 5))
                        .collate(3, 1).toBoundedList(5),
                "all overlapping partial tails");
    }

    private static void eagerValidation() {
        ProbeList values = new ProbeList(sequence(4));
        ObjectStream<Integer> source = ObjectStream.live(values);
        for (int width : new int[] {Integer.MIN_VALUE, -1, 0, 65_537, Integer.MAX_VALUE}) {
            expect(IllegalArgumentException.class, () -> source.collate(width),
                    "invalid default width");
            expect(IllegalArgumentException.class, () -> source.collate(width, 1),
                    "invalid stepped width");
            IllegalArgumentException failure = expect(IllegalArgumentException.class,
                    () -> source.collate(width, 0, null), "width validation precedes step");
            check(failure.getMessage().contains("width"), "width failure identifies width");
        }
        for (int step : new int[] {Integer.MIN_VALUE, -1, 0}) {
            expect(IllegalArgumentException.class, () -> source.collate(1, step),
                    "invalid step");
            IllegalArgumentException failure = expect(IllegalArgumentException.class,
                    () -> source.collate(1, step, null), "step validation precedes policy");
            check(failure.getMessage().contains("step"), "step failure identifies step");
        }
        expect(NullPointerException.class, () -> source.collate(1, 1, null),
                "null policy");
        equal(0, values.opens.get(), "invalid plans never open source");
        equal(0, values.reads.get(), "invalid plans never consume source");

        AtomicInteger closes = new AtomicInteger();
        ObjectStream<Integer> once = ObjectStream.once(Stream.of(1, 2).onClose(closes::incrementAndGet));
        expect(IllegalArgumentException.class, () -> once.collate(0), "failed validation retains once");
        expect(IllegalArgumentException.class, () -> once.collate(1, 0), "bad step retains once");
        expect(NullPointerException.class, () -> once.collate(1, 1, null), "bad policy retains once");
        equal(List.of(List.of(1, 2)), once.collate(2).toBoundedList(1),
                "valid plan can consume retained once");
        equal(1, closes.get(), "retained once closes once");
    }

    private static void snapshotsAndReferences() {
        Object first = new Object();
        Object last = new Object();
        List<Object> values = Arrays.asList(first, null, last, first);
        List<List<Object>> windows = ObjectStream.snapshot(values).collate(3, 1).toBoundedList(4);
        equal(reference(values, 3, 1, TailPolicy.INCLUDE), windows, "nulls and reference order");
        same(first, windows.get(0).get(0), "elements retain identity");
        same(last, windows.get(0).get(2), "last element retains identity");
        same(first, windows.get(3).get(0), "tail element retains identity");
        for (List<Object> window : windows) {
            expect(UnsupportedOperationException.class, () -> window.add(new Object()),
                    "window cannot append");
            expect(UnsupportedOperationException.class, () -> window.set(0, new Object()),
                    "window cannot replace");
            expect(UnsupportedOperationException.class, () -> window.remove(0),
                    "window cannot remove");
        }
        for (int i = 1; i < windows.size(); i++) {
            check(windows.get(i - 1) != windows.get(i), "each output has independent list ownership");
        }

        ObjectStream<List<Integer>> plan = ObjectStream.snapshot(sequence(100)).collate(17, 3);
        List<Integer> retained;
        try (Stream<List<Integer>> stream = plan.stream()) {
            Iterator<List<Integer>> iterator = stream.iterator();
            retained = iterator.next();
            while (iterator.hasNext()) iterator.next();
            equal(sequence(17), retained, "old window survives all ring advancement");
        }
        equal(sequence(17), retained, "old window survives close");
        List<List<Integer>> replay = plan.toBoundedList(34);
        equal(sequence(17), replay.get(0), "replay starts with original values");
        check(retained != replay.get(0), "replay owns fresh window lists");
    }

    private static void replayAndLiveSources() {
        List<Integer> values = new ArrayList<>(List.of(1, 2, 3));
        ObjectStream<List<Integer>> captured = ObjectStream.snapshot(values).collate(2, 1);
        ObjectStream<List<Integer>> live = ObjectStream.live(values).collate(2, 1);
        check(captured.replayable(), "snapshot remains replayable");
        check(live.replayable(), "live remains replayable");
        values.set(0, 9);
        equal(List.of(List.of(1, 2), List.of(2, 3), List.of(3)),
                captured.toBoundedList(3), "snapshot is detached from collection edits");
        equal(List.of(List.of(9, 2), List.of(2, 3), List.of(3)),
                live.toBoundedList(3), "live sees edit before first traversal");
        values.add(4);
        equal(List.of(List.of(9, 2), List.of(2, 3), List.of(3, 4), List.of(4)),
                live.toBoundedList(4), "live sees edit before replay");

        ObjectStream<List<Integer>> plan = ObjectStream.snapshot(sequence(7)).collate(3, 1);
        try (Stream<List<Integer>> a = plan.stream(); Stream<List<Integer>> b = plan.stream()) {
            Iterator<List<Integer>> left = a.iterator();
            Iterator<List<Integer>> right = b.iterator();
            equal(List.of(0, 1, 2), left.next(), "first cursor first window");
            equal(List.of(1, 2, 3), left.next(), "first cursor second window");
            equal(List.of(0, 1, 2), right.next(), "second cursor independent start");
            equal(List.of(2, 3, 4), left.next(), "first cursor unaffected by second");
            equal(List.of(1, 2, 3), right.next(), "second cursor independent advance");
        }

        ObjectStream<List<Integer>> once = ObjectStream.once(Stream.of(1, 2, 3)).collate(2, 1);
        check(!once.replayable(), "one-shot remains one-shot");
        equal(List.of(List.of(1, 2), List.of(2, 3), List.of(3)),
                once.toBoundedList(3), "one-shot emits expected windows");
        expect(IllegalStateException.class, once::stream, "one-shot refuses replay");
    }

    private static void demandAndGaps() {
        ProbeList source = new ProbeList(sequence(8));
        ObjectStream<List<Integer>> plan = ObjectStream.live(source).collate(3, 1);
        equal(0, source.opens.get(), "plan construction is lazy");
        try (Stream<List<Integer>> stream = plan.stream()) {
            equal(1, source.opens.get(), "opening opens upstream exactly once");
            equal(0, source.reads.get(), "opening consumes no source elements");
            Iterator<List<Integer>> windows = stream.iterator();
            equal(0, source.reads.get(), "iterator creation consumes no source elements");
            check(windows.hasNext(), "first window is available");
            equal(3, source.reads.get(), "first demand reads exactly width");
            check(windows.hasNext(), "repeated hasNext preserves prepared window");
            equal(3, source.reads.get(), "repeated hasNext does not read ahead");
            equal(List.of(0, 1, 2), windows.next(), "prepared first window");
            equal(3, source.reads.get(), "next of prepared window does not read ahead");
            equal(List.of(1, 2, 3), windows.next(), "second overlapping window");
            equal(4, source.reads.get(), "overlap reads only newly required element");
        }
        equal(1, source.closes.get(), "partial traversal closes upstream");

        ProbeList gaps = new ProbeList(sequence(11));
        try (Stream<List<Integer>> stream = ObjectStream.live(gaps).collate(2, 5).stream()) {
            Iterator<List<Integer>> windows = stream.iterator();
            equal(List.of(0, 1), windows.next(), "first gapped window");
            equal(2, gaps.reads.get(), "no gap consumed after first window");
            equal(List.of(5, 6), windows.next(), "second gapped window");
            equal(7, gaps.reads.get(), "only requested gap and window consumed");
            equal(List.of(10), windows.next(), "gapped short final window");
            equal(11, gaps.reads.get(), "short final window stops at source end");
            check(!windows.hasNext(), "gapped stream exhausts");
            check(!windows.hasNext(), "exhaustion is stable");
            expect(NoSuchElementException.class, windows::next, "next after exhaustion");
            equal(11, gaps.reads.get(), "exhaustion never rereads values");
        }
    }

    private static void finiteAndInfiniteShortCircuit() {
        for (int step : new int[] {1, 2, 7, Integer.MAX_VALUE}) {
            ProbeList source = new ProbeList(sequence(20));
            equal(List.of(List.of(0, 1, 2)),
                    ObjectStream.live(source).collate(3, step).limit(1).toBoundedList(1),
                    "one-window short circuit step=" + step);
            equal(3, source.reads.get(), "short circuit does not consume next gap or overlap");
            equal(1, source.closes.get(), "short circuit closes source");
        }
        AtomicInteger reads = new AtomicInteger();
        AtomicInteger closes = new AtomicInteger();
        ObjectStream<Integer> infinite = ObjectStream.once(
                Stream.iterate(0, value -> value + 1)
                        .peek(value -> reads.incrementAndGet()).onClose(closes::incrementAndGet));
        equal(List.of(List.of(0, 1, 2), List.of(2, 3, 4),
                        List.of(4, 5, 6), List.of(6, 7, 8)),
                infinite.collate(3, 2).limit(4).toBoundedList(4),
                "finite prefix of infinite source");
        equal(9, reads.get(), "infinite source reads exactly requested prefix");
        equal(1, closes.get(), "infinite source closes after bounded prefix");

        ProbeList unused = new ProbeList(sequence(3));
        equal(List.of(), ObjectStream.live(unused).collate(2, 1).limit(0).toBoundedList(0),
                "zero demand has no output");
        equal(0, unused.reads.get(), "zero demand does not consume source");
        equal(1, unused.closes.get(), "zero demand closes opened source");

        ProbeList parallel = new ProbeList(sequence(100));
        try (Stream<List<Integer>> stream = ObjectStream.live(parallel).collate(3, 1).stream()) {
            equal(List.of(List.of(0, 1, 2)), stream.parallel().limit(1).toList(),
                    "parallel request preserves bounded encounter-order prefix");
            equal(3, parallel.reads.get(), "parallel request does not prefetch later windows");
        }
    }

    private static void spliteratorContract() {
        ProbeList source = new ProbeList(sequence(4));
        try (Stream<List<Integer>> stream = ObjectStream.live(source).collate(2, 1).stream()) {
            check(!stream.isParallel(), "opened collate is sequential");
            Spliterator<List<Integer>> spliterator = stream.spliterator();
            check(spliterator.hasCharacteristics(Spliterator.ORDERED | Spliterator.NONNULL),
                    "windows are ordered and nonnull");
            check(!spliterator.hasCharacteristics(Spliterator.SIZED),
                    "collate does not advertise unproven size");
            same(null, spliterator.trySplit(), "split refuses speculative traversal");
            equal(0, source.reads.get(), "split attempt consumes no source elements");
            expect(NullPointerException.class, () -> spliterator.tryAdvance(null),
                    "null consumer fails eagerly");
            equal(0, source.reads.get(), "invalid consumer consumes no source elements");
            List<List<Integer>> output = new ArrayList<>();
            spliterator.forEachRemaining(output::add);
            equal(reference(sequence(4), 2, 1, TailPolicy.INCLUDE), output,
                    "direct spliterator output");
            check(!spliterator.tryAdvance(output::add), "exhausted spliterator remains exhausted");
            expect(NullPointerException.class, () -> spliterator.tryAdvance(null),
                    "exhausted spliterator still rejects null consumer");
        }
    }

    private static void closeAndFailureOwnership() {
        ProbeList unconsumed = new ProbeList(sequence(5));
        Stream<List<Integer>> unopenedTraversal = ObjectStream.live(unconsumed).collate(3, 1).stream();
        unopenedTraversal.close();
        unopenedTraversal.close();
        equal(0, unconsumed.reads.get(), "closing unconsumed stream reads nothing");
        equal(1, unconsumed.closes.get(), "closing twice closes upstream once");

        AtomicInteger closes = new AtomicInteger();
        IllegalStateException upstreamFailure = new IllegalStateException("source marker");
        IllegalArgumentException closeFailure = new IllegalArgumentException("close marker");
        Stream<Integer> failingSource = Stream.of(0, 1, 2, 3).map(value -> {
            if (value == 2) throw upstreamFailure;
            return value;
        }).onClose(() -> {
            closes.incrementAndGet();
            throw closeFailure;
        });
        IllegalStateException actual = expect(IllegalStateException.class,
                () -> ObjectStream.once(failingSource).collate(3, 1).toBoundedList(4),
                "source callback failure propagates");
        same(upstreamFailure, actual, "source failure retains identity");
        equal(1, closes.get(), "failed traversal closes upstream once");
        equal(1, actual.getSuppressed().length, "close failure is suppressed");
        same(closeFailure, actual.getSuppressed()[0], "suppressed close failure retains identity");

        ProbeList consumerSource = new ProbeList(sequence(5));
        RuntimeException consumerFailure = new RuntimeException("consumer marker");
        RuntimeException observed = expect(RuntimeException.class, () -> {
            try (Stream<List<Integer>> stream = ObjectStream.live(consumerSource).collate(2, 1).stream()) {
                stream.forEach(window -> { throw consumerFailure; });
            }
        }, "downstream callback failure propagates");
        same(consumerFailure, observed, "downstream failure retains identity");
        equal(2, consumerSource.reads.get(), "consumer failure does not prefetch");
        equal(1, consumerSource.closes.get(), "consumer failure closes upstream");

        ProbeList budgetSource = new ProbeList(sequence(8));
        expect(IllegalStateException.class,
                () -> ObjectStream.live(budgetSource).collate(3, 1).toBoundedList(1),
                "existing materialization budget is retained");
        equal(1, budgetSource.closes.get(), "budget failure closes upstream");

        AtomicInteger onceClosed = new AtomicInteger();
        ObjectStream<List<Integer>> once = ObjectStream.once(
                Stream.of(1, 2).onClose(onceClosed::incrementAndGet)).collate(2, 1);
        try (Stream<List<Integer>> stream = once.stream()) {
            check(!stream.isParallel(), "unconsumed once opens sequentially");
        }
        equal(1, onceClosed.get(), "unconsumed once closes");
        expect(IllegalStateException.class, once::stream, "opening claims one-shot even without demand");
    }

    private static void existingWindowsAndBatches() {
        List<Integer> values = Arrays.asList(1, null, 3, 4, 5);
        ObjectStream<Integer> source = ObjectStream.snapshot(values);
        equal(reference(values, 3, 1, TailPolicy.DROP), source.window(3).toBoundedList(3),
                "existing full-window operator retains its contract");
        equal(reference(values, 3, 3, TailPolicy.INCLUDE),
                source.batch(3).toBoundedList(2), "existing include batch retains its contract");
        equal(reference(values, 3, 3, TailPolicy.DROP),
                source.batch(3, TailPolicy.DROP).toBoundedList(1),
                "existing drop batch retains its contract");
    }

    private static List<Integer> sequence(int length) {
        return IntStream.range(0, length).boxed().toList();
    }

    private static void equal(Object expected, Object actual, String label) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(label + ": expected=" + expected + " actual=" + actual);
        }
    }

    private static void same(Object expected, Object actual, String label) {
        checks++;
        if (expected != actual) throw new AssertionError(label + ": identities differ");
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    private static <E extends Throwable> E expect(Class<E> type, Runnable action, String label) {
        checks++;
        try {
            action.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) return type.cast(failure);
            throw new AssertionError(label + ": wrong failure " + failure, failure);
        }
        throw new AssertionError(label + ": expected " + type.getSimpleName());
    }

    private static final class ProbeList extends ArrayList<Integer> {
        private static final long serialVersionUID = 1L;
        final AtomicInteger opens = new AtomicInteger();
        final AtomicInteger reads = new AtomicInteger();
        final AtomicInteger closes = new AtomicInteger();

        ProbeList(List<Integer> values) {
            super(values);
        }

        @Override public Stream<Integer> stream() {
            opens.incrementAndGet();
            return super.stream().peek(value -> reads.incrementAndGet())
                    .onClose(closes::incrementAndGet);
        }
    }
}
