/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 *
 * @test
 * @summary Bounded keyed joins, count chunks, centered windows and stable greatest top-K
 * @modules jdk.m3.collections/jdk.m3.collections
 * @run main/othervm ObjectStreamKeyedOperationsTest
 */
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;
import jdk.m3.collections.ObjectStream;

public final class ObjectStreamKeyedOperationsTest {
    private static int checks;
    private record Item(Integer key, int score, int id) { }

    public static void main(String[] args) {
        suppliedStreamOwnership();
        joinValuesAndOrder();
        joinLazinessAndOwnership();
        joinBoundsAndFailures();
        replayLiveAndOnce();
        centeredAndCountWindows();
        topKContracts();
        generatedOracles();
        System.out.println("ObjectStreamKeyedOperationsTest PASS: " + checks + " assertions");
    }

    private static <T> ObjectStream<T> plan(List<T> values) {
        return ObjectStream.snapshot(values);
    }

    private static void suppliedStreamOwnership() {
        fails(NullPointerException.class, () -> ObjectStream.fromStreamSupplier(null));
        AtomicInteger opens = new AtomicInteger();
        AtomicInteger closes = new AtomicInteger();
        AtomicInteger pulls = new AtomicInteger();
        ObjectStream<Number> supplied = ObjectStream.<Number>fromStreamSupplier(() -> {
            opens.incrementAndGet();
            return Stream.of(1, 2, 3).parallel().peek(n -> pulls.incrementAndGet())
                    .onClose(closes::incrementAndGet);
        });
        check(supplied.replayable(), "supplier explicitly promises replayability");
        eq(opens.get(), 0, "supplier construction is lazy");
        fails(NullPointerException.class, () -> supplied.filter(null));
        fails(NullPointerException.class, () -> supplied.parallelByKey(null));
        fails(NullPointerException.class, () -> supplied.parallelByKey(Number::intValue)
                .mapOrdered(null, 2, 2, 3));
        eq(opens.get(), 0, "callback guards precede supplier invocation");
        eq(supplied.limit(1).toBoundedList(1), List.of(1), "supplier short-circuit");
        eq(opens.get(), 1, "first traversal opens once");
        eq(closes.get(), 1, "short-circuit closes supplied source once");
        eq(pulls.get(), 1, "short-circuit consumes one element");
        eq(supplied.mapIndexed((n, index) -> index + ":" + n).toBoundedList(3),
                List.of("0:1", "1:2", "2:3"), "fresh supplied traversal resets index");
        eq(opens.get(), 2, "second traversal opens once");
        eq(closes.get(), 2, "second traversal closes once");
        try (Stream<Number> opened = supplied.stream()) {
            check(!opened.isParallel(), "supplied stream is sequentialized");
            eq(opens.get(), 3, "explicit stream opening invokes factory once");
            eq(pulls.get(), 4, "explicit stream opening does not consume");
        }
        eq(closes.get(), 3, "explicit close owns supplied traversal");
        ObjectStream.Mark<Number> mark = supplied.mark(3);
        eq(mark.reprocess().toBoundedList(3), List.of(1, 2, 3), "supplied source mark replay");
        eq(mark.reprocess().toBoundedList(3), List.of(1, 2, 3),
                "supplied source second mark replay");
        eq(opens.get(), 4, "captured mark replay does not reopen supplier");
        eq(closes.get(), 4, "mark capture closes supplied traversal");
        eq(supplied.parallelByKey(n -> n.intValue() & 1).mapOrdered(Number::intValue, 2, 2, 3),
                List.of(1, 2, 3), "fresh supplier admitted by existing keyed replay guard");
        eq(opens.get(), 5, "keyed traversal opens once");
        eq(closes.get(), 5, "keyed traversal closes once");
        eq(supplied.limit(0).count(), 0L, "supplier zero-demand result");
        eq(opens.get(), 6, "zero-demand evaluation still opens once");
        eq(closes.get(), 6, "zero-demand evaluation closes once");
        eq(pulls.get(), 10, "zero demand and mark replay do not pull supplied values");

        RuntimeException supplyFailure = new RuntimeException("supplier");
        AtomicInteger failedOpens = new AtomicInteger();
        ObjectStream<Integer> retry = ObjectStream.fromStreamSupplier(() -> {
            if (failedOpens.getAndIncrement() == 0) throw supplyFailure;
            return Stream.of(7);
        });
        check(fails(RuntimeException.class, retry::stream) == supplyFailure,
                "supplier failure identity");
        eq(retry.toBoundedList(1), List.of(7),
                "supplier failure leaves a future invocation available");
        eq(failedOpens.get(), 2, "one factory call per attempted traversal");
        AtomicInteger nullOpens = new AtomicInteger();
        ObjectStream<Integer> nullFactory = ObjectStream.fromStreamSupplier(() -> {
            nullOpens.incrementAndGet(); return null;
        });
        Throwable nullResult = fails(NullPointerException.class, nullFactory::stream);
        eq(nullResult.getMessage(), "stream factory returned null", "existing null-result guard");
        eq(nullOpens.get(), 1, "null-result factory invoked once");

        RuntimeException mapFailure = new RuntimeException("mapped");
        RuntimeException closeFailure = new RuntimeException("supplied close");
        AtomicInteger failingCloses = new AtomicInteger();
        ObjectStream<Integer> failing = ObjectStream.fromStreamSupplier(
                () -> Stream.of(1).onClose(() -> {
                    failingCloses.incrementAndGet(); throw closeFailure;
                }));
        Throwable failed = fails(RuntimeException.class,
                () -> failing.map(n -> { throw mapFailure; })
                .toBoundedList(1));
        check(failed == mapFailure, "supplied callback failure identity");
        eq(Arrays.asList(failed.getSuppressed()), List.of(closeFailure),
                "supplied close failure suppression");
        eq(failingCloses.get(), 1, "supplied failing traversal closes exactly once");
    }

    private static void joinValuesAndOrder() {
        List<Integer> left = Arrays.asList(null, 1, 2, 1);
        List<Integer> right = Arrays.asList(1, null, 1);
        eq(plan(left).innerJoinByKey(plan(right), Function.identity(), Function.identity(),
                (a, b) -> a + ":" + b, 4, 3, 3, 2, 5).toBoundedList(5),
                List.of("null:null", "1:1", "1:1", "1:1", "1:1"), "inner multiplicity/order");
        eq(plan(left).leftJoinByKey(plan(right), Function.identity(), Function.identity(),
                (a, present, b) -> a + ":" + present + ":" + b, 4, 3, 3, 2, 6).toBoundedList(6),
                List.of("null:true:null", "1:true:1", "1:true:1", "2:false:null", "1:true:1",
                        "1:true:1"),
                "outer null presence/order");
        eq(plan(left).semiJoinByKey(plan(right), Function.identity(), Function.identity(),
                4, 3, 3, 2, 3).toBoundedList(3), Arrays.asList(null, 1, 1), "semi occurrences");
        eq(plan(left).antiJoinByKey(plan(right), Function.identity(), Function.identity(),
                4, 3, 3, 2, 1).toBoundedList(1), List.of(2), "anti occurrences");
        eq(plan(List.of(1, 1)).innerJoinByKey(plan(List.of(1)), Function.identity(),
                Function.identity(),
                (a, b) -> (String) null, 2, 1, 1, 1, 2).toBoundedList(2),
                Arrays.asList(null, null), "nullable join output");
        eq(plan(left).leftJoinByKey(plan(List.<Integer>of()), Function.identity(),
                Function.identity(),
                (a, present, b) -> present, 4, 0, 3, 0, 4).toBoundedList(4),
                List.of(false, false, false, false), "empty right outer");
        eq(plan(List.<Integer>of()).innerJoinByKey(plan(right), Function.identity(),
                Function.identity(),
                Integer::sum, 0, 3, 0, 2, 0).toBoundedList(0), List.of(), "empty left");
        ObjectStream<Integer> same = plan(left);
        fails(IllegalArgumentException.class, () -> same.innerJoinByKey(same,
                Function.identity(), Function.identity(), Integer::sum, 4, 4, 3, 3, 16));
        eq(same.toBoundedList(4), left, "alias rejection leaves source intact");
        List<Integer> payload = List.of(1, 2);
        eq(plan(payload).innerJoinByKey(plan(payload), Function.identity(), Function.identity(),
                Integer::sum, 2, 2, 2, 2, 2).toBoundedList(2), List.of(2, 4),
                "distinct plans may share data");
        Function<Integer, CollisionKey> collision = n -> n == null ? null : new CollisionKey(n);
        eq(plan(left).innerJoinByKey(plan(right), collision, collision,
                (a, b) -> a + ":" + b, 4, 3, 3, 2, 5).toBoundedList(5),
                List.of("null:null", "1:1", "1:1", "1:1", "1:1"),
                "exact equality under collisions");
    }

    private static void joinLazinessAndOwnership() {
        Traced<Integer> left = new Traced<>(List.of(2, 1, 2));
        Traced<Integer> right = new Traced<>(List.of(2, 2, 1));
        AtomicInteger leftKeys = new AtomicInteger();
        AtomicInteger rightKeys = new AtomicInteger();
        AtomicInteger projections = new AtomicInteger();
        ObjectStream<String> joined =
                ObjectStream.live(left).innerJoinByKey(ObjectStream.live(right),
                n -> { leftKeys.incrementAndGet(); return n; },
                n -> { rightKeys.incrementAndGet(); return n; }, (a, b) -> {
                    check(left.closes == 1 && right.closes == 1, "close before projection");
                    projections.incrementAndGet();
                    return a + ":" + b;
                }, 3, 3, 2, 2, 5);
        eq(left.opens + right.opens, 0, "plan construction is lazy");
        try (Stream<String> stream = joined.stream()) {
            eq(left.opens + right.opens, 2, "open both traversals");
            eq(left.pulls + right.pulls, 0, "stream opening does not traverse");
            Spliterator<String> cursor = stream.parallel().spliterator();
            check(cursor.trySplit() == null, "join cannot split");
            eq(left.pulls + right.pulls, 0, "split probe is lazy");
            ArrayList<String> first = new ArrayList<>();
            check(cursor.tryAdvance(first::add), "first projection");
            eq(first, List.of("2:2"), "first left/right occurrence");
            eq(leftKeys.get(), 3, "left key once per source");
            eq(rightKeys.get(), 3, "right key once per source");
            eq(projections.get(), 1, "short circuit projects one result");
        }
        eq(left.closes + right.closes, 2, "close handlers exactly once");

        Traced<Integer> zeroLeft = new Traced<>(List.of(1));
        Traced<Integer> zeroRight = new Traced<>(List.of(1));
        ObjectStream<Integer> zero =
                ObjectStream.live(zeroLeft).innerJoinByKey(ObjectStream.live(zeroRight),
                Function.identity(), Function.identity(), Integer::sum, 1, 1, 1, 1, 1);
        eq(zero.limit(0).toBoundedList(0), List.of(), "zero demand");
        eq(zeroLeft.pulls + zeroRight.pulls, 0, "zero demand pulls none");
        eq(zeroLeft.closes + zeroRight.closes, 2, "zero demand closes both");

        AtomicInteger statefulPulls = new AtomicInteger();
        ObjectStream<Integer> sorted = ObjectStream.once(Stream.of(3, 2, 1).parallel()
                .peek(n -> statefulPulls.incrementAndGet()).sorted());
        ObjectStream<Integer> statefulJoin = sorted.innerJoinByKey(plan(List.of(2)),
                Function.identity(), Function.identity(), Integer::sum, 3, 1, 3, 1, 1);
        try (Stream<Integer> opened = statefulJoin.stream()) {
            eq(statefulPulls.get(), 0, "parallel stateful source opening remains lazy");
            eq(opened.toList(), List.of(4), "stateful input result");
        }
    }

    private static void joinBoundsAndFailures() {
        for (int bound = 0; bound < 5; bound++) {
            Traced<Integer> left = new Traced<>(List.of(1));
            Traced<Integer> right = new Traced<>(List.of(1));
            int[] caps = {1, 1, 1, 1, 1};
            caps[bound] = -1;
            fails(IllegalArgumentException.class, () -> ObjectStream.live(left).innerJoinByKey(
                    ObjectStream.live(right), Function.identity(), Function.identity(),
                    Integer::sum,
                    caps[0], caps[1], caps[2], caps[3], caps[4]));
            eq(left.opens + right.opens, 0, "eager negative-bound refusal");
        }
        for (int bound = 0; bound < 5; bound++) {
            Traced<Integer> left = new Traced<>(List.of(1, 2));
            Traced<Integer> right = new Traced<>(List.of(1, 2));
            int[] caps = {2, 2, 2, 2, 2};
            caps[bound] = 1;
            AtomicInteger projected = new AtomicInteger();
            fails(IllegalStateException.class, () -> ObjectStream.live(left).innerJoinByKey(
                    ObjectStream.live(right), Function.identity(), Function.identity(), (a, b) -> {
                        projected.incrementAndGet(); return a + b;
                    }, caps[0], caps[1], caps[2], caps[3], caps[4]).toBoundedList(4));
            eq(projected.get(), 0, "all bounds precede joiner");
            eq(left.closes + right.closes, 2, "bound failure closes both");
        }
        Traced<Integer> outerLeft = new Traced<>(List.of(1, 2));
        Traced<Integer> outerRight = new Traced<>(List.of(1, 1));
        AtomicInteger outerProjections = new AtomicInteger();
        fails(IllegalStateException.class, () -> ObjectStream.live(outerLeft).leftJoinByKey(
                ObjectStream.live(outerRight), Function.identity(), Function.identity(),
                (a, present, b) -> {
                    outerProjections.incrementAndGet(); return a;
                }, 2, 2, 2, 1, 2).count());
        eq(outerProjections.get(), 0, "outer matched and unmatched cardinality precedes joiner");
        eq(outerLeft.closes + outerRight.closes, 2, "outer result cap closes both");

        Traced<Integer> semiLeft = new Traced<>(List.of(1, 1, 2));
        Traced<Integer> semiRight = new Traced<>(List.of(1, 1));
        AtomicInteger semiProjections = new AtomicInteger();
        fails(IllegalStateException.class, () -> ObjectStream.live(semiLeft).semiJoinByKey(
                ObjectStream.live(semiRight), Function.identity(), Function.identity(),
                3, 2, 2, 1, 1)
                .map(n -> { semiProjections.incrementAndGet(); return n; }).count());
        eq(semiProjections.get(), 0, "semi occurrence cardinality precedes downstream projection");
        eq(semiLeft.closes + semiRight.closes, 2, "semi result cap closes both");

        Traced<Integer> antiLeft = new Traced<>(List.of(1, 1, 2));
        Traced<Integer> antiRight = new Traced<>(List.of(2));
        AtomicInteger antiProjections = new AtomicInteger();
        fails(IllegalStateException.class, () -> ObjectStream.live(antiLeft).antiJoinByKey(
                ObjectStream.live(antiRight), Function.identity(), Function.identity(),
                3, 1, 2, 1, 1)
                .map(n -> { antiProjections.incrementAndGet(); return n; }).count());
        eq(antiProjections.get(), 0, "anti occurrence cardinality precedes downstream projection");
        eq(antiLeft.closes + antiRight.closes, 2, "anti result cap closes both");

        AtomicInteger productCalls = new AtomicInteger();
        Integer[] many = new Integer[50_000];
        fails(IllegalStateException.class, () -> ObjectStream.arrayWindow(many, 0, many.length)
                .innerJoinByKey(ObjectStream.arrayWindow(many, 0, many.length),
                        Function.identity(), Function.identity(), (a, b) -> {
                            productCalls.incrementAndGet(); return a;
                        }, many.length, many.length, 1, 1, Integer.MAX_VALUE).count());
        eq(productCalls.get(), 0, "long Cartesian cardinality before projection");

        RuntimeException keyFailure = new RuntimeException("key");
        RuntimeException leftClose = new RuntimeException("left close");
        RuntimeException rightClose = new RuntimeException("right close");
        Traced<Integer> failedLeft = new Traced<>(List.of(1));
        Traced<Integer> failedRight = new Traced<>(List.of(1));
        failedLeft.closeFailure = leftClose;
        failedRight.closeFailure = rightClose;
        Throwable got = fails(RuntimeException.class,
                () -> ObjectStream.live(failedLeft).innerJoinByKey(
                ObjectStream.live(failedRight), n -> { throw keyFailure; }, Function.identity(),
                Integer::sum, 1, 1, 1, 1, 1).count());
        check(got == keyFailure, "key failure identity");
        eq(Arrays.asList(got.getSuppressed()), List.of(rightClose, leftClose),
                "close suppression order");
        eq(failedRight.pulls, 0, "left failure does not traverse right");
        eq(failedLeft.closes + failedRight.closes, 2, "key failure closes both");

        Traced<Integer> closingLeft = new Traced<>(List.of(1));
        Traced<Integer> closingRight = new Traced<>(List.of(1));
        RuntimeException primaryClose = new RuntimeException("primary close");
        RuntimeException secondaryClose = new RuntimeException("secondary close");
        closingRight.closeFailure = primaryClose;
        closingLeft.closeFailure = secondaryClose;
        AtomicInteger closedProjection = new AtomicInteger();
        Throwable closed = fails(RuntimeException.class,
                () -> ObjectStream.live(closingLeft).innerJoinByKey(
                ObjectStream.live(closingRight), Function.identity(), Function.identity(),
                (a, b) -> {
                    closedProjection.incrementAndGet(); return a;
                }, 1, 1, 1, 1, 1).count());
        check(closed == primaryClose, "close failure identity");
        eq(Arrays.asList(closed.getSuppressed()), List.of(secondaryClose),
                "second close suppressed");
        eq(closedProjection.get(), 0, "closure must succeed before joiner");

        Traced<Integer> openLeft = new Traced<>(List.of(1));
        Traced<Integer> openRight = new Traced<>(List.of(1));
        RuntimeException openFailure = new RuntimeException("open");
        RuntimeException openLeftClose = new RuntimeException("opened left close");
        openRight.openFailure = openFailure;
        openLeft.closeFailure = openLeftClose;
        Throwable opening = fails(RuntimeException.class,
                () -> ObjectStream.live(openLeft).innerJoinByKey(
                ObjectStream.live(openRight), Function.identity(), Function.identity(),
                Integer::sum,
                1, 1, 1, 1, 1).stream());
        check(opening == openFailure, "second open identity");
        eq(Arrays.asList(opening.getSuppressed()), List.of(openLeftClose),
                "second open suppression");
        eq(openLeft.closes, 1, "failed second open closes first");

        Traced<Integer> mapLeft = new Traced<>(List.of(1));
        Traced<Integer> mapRight = new Traced<>(List.of(1));
        Error projectionFailure = new AssertionError("projection");
        check(fails(AssertionError.class, () -> ObjectStream.live(mapLeft).innerJoinByKey(
                ObjectStream.live(mapRight), Function.identity(), Function.identity(),
                (a, b) -> { throw projectionFailure; },
                1, 1, 1, 1, 1).count()) == projectionFailure,
                "projection Error identity");
        eq(mapLeft.closes + mapRight.closes, 2, "projection failure sees closed inputs");
    }

    private static void replayLiveAndOnce() {
        ArrayList<Integer> values = new ArrayList<>(List.of(1, 2));
        ObjectStream<Integer> live = ObjectStream.live(values).innerJoinByKey(plan(List.of(1, 3)),
                Function.identity(), Function.identity(), Integer::sum, 4, 2, 4, 2, 4);
        check(live.replayable(), "replayable join");
        eq(live.toBoundedList(4), List.of(2), "first live traversal");
        values.add(3);
        eq(live.toBoundedList(4), List.of(2, 6), "fresh live traversal");
        ObjectStream<Integer> once = ObjectStream.once(Stream.of(1, 2))
                .innerJoinByKey(plan(List.of(2)),
                Function.identity(), Function.identity(), Integer::sum, 2, 1, 2, 1, 1);
        check(!once.replayable(), "one-shot property");
        eq(once.toBoundedList(1), List.of(4), "one-shot result");
        fails(IllegalStateException.class, () -> once.toBoundedList(1));
        ObjectStream<List<Integer>> mapped = ObjectStream.live(values).mapWindowsByKey(
                n -> n & 1, 1, 1, (center, window) -> window, 4, 2);
        check(mapped.replayable(), "window replay property");
        eq(mapped.toBoundedList(4), List.of(List.of(1, 3), List.of(2), List.of(1, 3)),
                "live window first");
        values.set(0, 5);
        eq(mapped.toBoundedList(4), List.of(List.of(5, 3), List.of(2), List.of(5, 3)),
                "live window refresh");
        ObjectStream<Integer> onceWindow = ObjectStream.once(Stream.of(1)).mapWindowsByKey(
                Function.identity(), 0, 0, (center, window) -> center, 1, 1);
        eq(onceWindow.toBoundedList(1), List.of(1), "one-shot window");
        fails(IllegalStateException.class, () -> onceWindow.toBoundedList(1));
    }

    private static void centeredAndCountWindows() {
        List<Integer> values = Arrays.asList(1, 2, 3, null, 4, 5, null);
        Function<Integer, Integer> key = n -> n == null ? null : n & 1;
        AtomicInteger keys = new AtomicInteger();
        Traced<Integer> input = new Traced<>(values);
        Map<Integer, List<List<Integer>>> chunks = ObjectStream.live(input).countWindowsByKey(
                n -> { keys.incrementAndGet(); return key.apply(n); },
                2, ObjectStream.TailPolicy.INCLUDE, 7, 3);
        eq(keys.get(), 7, "count-window key once");
        eq(input.closes, 1, "count-window source closed");
        eq(new ArrayList<>(chunks.keySet()), Arrays.asList(1, 0, null),
                "chunk key encounter order");
        eq(chunks.get(1), List.of(List.of(1, 3), List.of(5)), "count chunks");
        eq(chunks.get(0), List.of(List.of(2, 4)), "interleaved even chunks");
        eq(chunks.get(null), Collections.singletonList(Arrays.asList(null, null)), "null chunk");
        fails(UnsupportedOperationException.class, () -> chunks.put(7, List.of()));
        fails(UnsupportedOperationException.class, () -> chunks.get(1).add(List.of()));
        fails(UnsupportedOperationException.class, () -> chunks.get(1).getFirst().add(9));
        eq(plan(values).countWindowsByKey(key, 4, ObjectStream.TailPolicy.DROP, 7, 3).get(1),
                List.of(), "DROP retains observed key with no full chunk");

        AtomicInteger windowKeys = new AtomicInteger();
        AtomicInteger calls = new AtomicInteger();
        Traced<Integer> centered = new Traced<>(values);
        ObjectStream<List<Integer>> windows = ObjectStream.live(centered).mapWindowsByKey(
                n -> { windowKeys.incrementAndGet(); return key.apply(n); }, 1, 1,
                (center, window) -> {
                    check(centered.closes == 1, "centered close before mapper");
                    calls.incrementAndGet();
                    return window;
                }, 7, 3);
        eq(windowKeys.get(), 0, "window plan is lazy");
        try (Stream<List<Integer>> stream = windows.stream()) {
            eq(centered.pulls, 0, "window opening is lazy");
            Spliterator<List<Integer>> cursor = stream.parallel().spliterator();
            check(cursor.trySplit() == null, "centered windows cannot split");
            eq(centered.pulls, 0, "centered split does not pull");
            ArrayList<List<Integer>> first = new ArrayList<>();
            check(cursor.tryAdvance(first::add), "centered first value");
            eq(first, List.of(List.of(1, 3)), "truncated leading window");
            eq(windowKeys.get(), 7, "centered key once");
            eq(calls.get(), 1, "window projection is lazy");
            fails(UnsupportedOperationException.class, () -> first.getFirst().clear());
        }
        List<List<Integer>> expected = Arrays.asList(List.of(1, 3), List.of(2, 4), List.of(1, 3, 5),
                Arrays.asList(null, null), List.of(2, 4), List.of(3, 5), Arrays.asList(null, null));
        eq(plan(values).mapWindowsByKey(key, 1, 1, (center, window) -> window, 7, 3)
                .toBoundedList(7),
                expected, "source-aligned centered windows");
        eq(plan(values).mapWindowsByKey(key, 0, 0, (center, window) -> center, 7, 3)
                .toBoundedList(7),
                values, "zero radius includes center only");
        eq(plan(values).mapWindowsByKey(key, 0, 1, (center, window) -> (String) null, 7, 3).count(),
                7L, "nullable mapped results still invoke mapper");
        AtomicInteger zeroCalls = new AtomicInteger();
        Traced<Integer> zero = new Traced<>(List.of(1));
        eq(ObjectStream.live(zero).mapWindowsByKey(Function.identity(), 0, 0,
                (center, window) -> { zeroCalls.incrementAndGet(); return center; }, 1, 1)
                .limit(0).toBoundedList(0), List.of(), "zero window demand");
        eq(zeroCalls.get() + zero.pulls, 0, "zero window demand callbacks");
        eq(zero.closes, 1, "zero window demand closes source");

        fails(IllegalArgumentException.class, () -> plan(values).countWindowsByKey(key, 0,
                ObjectStream.TailPolicy.INCLUDE, 7, 3));
        fails(IllegalArgumentException.class, () -> plan(values).mapWindowsByKey(key, -1, 0,
                (center, window) -> center, 7, 3));
        fails(IllegalArgumentException.class, () -> plan(values).mapWindowsByKey(key, 65535, 1,
                (center, window) -> center, 7, 3));
        fails(IllegalStateException.class, () -> plan(values).countWindowsByKey(key, 2,
                ObjectStream.TailPolicy.INCLUDE, 6, 3));
        fails(IllegalStateException.class, () -> plan(values).countWindowsByKey(key, 2,
                ObjectStream.TailPolicy.INCLUDE, 7, 2));
        Traced<Integer> bound = new Traced<>(values);
        AtomicInteger mapperCalls = new AtomicInteger();
        fails(IllegalStateException.class, () -> ObjectStream.live(bound).mapWindowsByKey(key, 1, 1,
                (center, window) -> { mapperCalls.incrementAndGet(); return center; },
                7, 2).count());
        eq(mapperCalls.get(), 0, "centered budget before mapper");
        eq(bound.closes, 1, "centered budget closes input");
        RuntimeException failure = new RuntimeException("window mapper");
        Traced<Integer> failed = new Traced<>(List.of(1));
        check(fails(RuntimeException.class, () -> ObjectStream.live(failed).mapWindowsByKey(
                Function.identity(), 0, 0, (center, window) -> { throw failure; },
                1, 1).count()) == failure,
                "window mapper identity");
        eq(failed.closes, 1, "failed window mapper source closed");
    }

    private static void topKContracts() {
        List<Item> values = List.of(new Item(1, 8, 0), new Item(null, 5, 1),
                new Item(1, 8, 2), new Item(2, 1, 3), new Item(1, 9, 4), new Item(null, 5, 5));
        Traced<Item> source = new Traced<>(values);
        AtomicInteger keys = new AtomicInteger();
        AtomicInteger comparisons = new AtomicInteger();
        Map<Integer, List<Item>> result = ObjectStream.live(source).topKByKey(
                item -> { keys.incrementAndGet(); return item.key(); }, 2, (a, b) -> {
                    check(source.closes == 1, "top-K close before comparator");
                    comparisons.incrementAndGet();
                    return Integer.compare(a.score(), b.score());
                }, 6, 3);
        eq(keys.get(), 6, "top-K key once");
        check(comparisons.get() > 0, "heap comparison witness");
        eq(new ArrayList<>(result.keySet()), Arrays.asList(1, null, 2), "top-K first-key order");
        eq(result.get(1), List.of(values.get(4), values.get(0)),
                "greatest and stable tied survivor");
        eq(result.get(null), List.of(values.get(1), values.get(5)), "stable equal scores");
        fails(UnsupportedOperationException.class, () -> result.clear());
        fails(UnsupportedOperationException.class, () -> result.get(1).clear());
        Map<Integer, List<Item>> zero = plan(values).topKByKey(Item::key, 0,
                (a, b) -> { throw new AssertionError("zero-K comparator"); }, 6, 3);
        eq(new ArrayList<>(zero.keySet()), Arrays.asList(1, null, 2), "zero-K observed keys");
        for (List<Item> group : zero.values()) eq(group, List.of(), "zero-K empty output");
        eq(plan(Arrays.asList(null, 2, null, 1)).topKByKey(n -> null, 4,
                Comparator.nullsFirst(Comparator.<Integer>naturalOrder()), 4, 1).get(null),
                Arrays.asList(2, 1, null, null), "nullable top-K payload");
        fails(IllegalArgumentException.class, () -> plan(values).topKByKey(Item::key, -1,
                Comparator.comparingInt(Item::score), 6, 3));
        fails(IllegalArgumentException.class, () -> plan(values).topKByKey(Item::key, 65537,
                Comparator.comparingInt(Item::score), 6, 3));
        Traced<Item> bound = new Traced<>(values);
        AtomicInteger boundComparisons = new AtomicInteger();
        fails(IllegalStateException.class, () -> ObjectStream.live(bound).topKByKey(Item::key, 2,
                (a, b) -> { boundComparisons.incrementAndGet(); return 0; }, 6, 2));
        eq(boundComparisons.get(), 0, "top-K budget precedes comparisons");
        eq(bound.closes, 1, "top-K budget closes source");
        RuntimeException failure = new RuntimeException("comparator");
        Traced<Item> failed = new Traced<>(values);
        check(fails(RuntimeException.class, () -> ObjectStream.live(failed).topKByKey(Item::key, 2,
                (a, b) -> { throw failure; }, 6, 3)) == failure, "comparator identity");
        eq(failed.closes, 1, "comparator failure source closed");
    }

    private static void generatedOracles() {
        Random random = new Random(0x4b3d99c2L);
        for (int trial = 0; trial < 700; trial++) {
            List<Item> left = generated(random, random.nextInt(24));
            List<Item> right = generated(random, random.nextInt(24));
            ArrayList<String> inner = new ArrayList<>();
            ArrayList<String> outer = new ArrayList<>();
            ArrayList<Item> semi = new ArrayList<>();
            ArrayList<Item> anti = new ArrayList<>();
            for (Item a : left) {
                boolean matched = false;
                for (Item b : right) {
                    if (!Objects.equals(a.key(), b.key())) continue;
                    matched = true;
                    inner.add(a.id() + ":" + b.id());
                    outer.add(a.id() + ":true:" + b.id());
                }
                if (matched) semi.add(a);
                else { anti.add(a); outer.add(a.id() + ":false:null"); }
            }
            eq(plan(left).innerJoinByKey(plan(right), Item::key, Item::key,
                    (a, b) -> a.id() + ":" + b.id(), left.size(), right.size(), 6, 6, inner.size())
                    .toBoundedList(inner.size()), inner, "generated inner oracle");
            eq(plan(left).leftJoinByKey(plan(right), Item::key, Item::key,
                    (a, present, b) -> a.id() + ":" + present + ":" + (present ? b.id() : null),
                    left.size(), right.size(), 6, 6, outer.size()).toBoundedList(outer.size()),
                    outer,
                    "generated outer oracle");
            eq(plan(left).semiJoinByKey(plan(right), Item::key, Item::key,
                    left.size(), right.size(), 6, 6, semi.size()).toBoundedList(semi.size()), semi,
                    "generated semi oracle");
            eq(plan(left).antiJoinByKey(plan(right), Item::key, Item::key,
                    left.size(), right.size(), 6, 6, anti.size()).toBoundedList(anti.size()), anti,
                    "generated anti oracle");

            LinkedHashMap<Integer, List<Item>> groups = new LinkedHashMap<>();
            for (Item item : left)
                groups.computeIfAbsent(item.key(), ignored -> new ArrayList<>()).add(item);
            int width = 1 + random.nextInt(5);
            for (ObjectStream.TailPolicy tail : ObjectStream.TailPolicy.values()) {
                LinkedHashMap<Integer, List<List<Item>>> expected = new LinkedHashMap<>();
                for (Map.Entry<Integer, List<Item>> entry : groups.entrySet()) {
                    ArrayList<List<Item>> chunks = new ArrayList<>();
                    for (int from = 0; from < entry.getValue().size(); from += width) {
                        int to = Math.min(from + width, entry.getValue().size());
                        if (to - from == width || tail == ObjectStream.TailPolicy.INCLUDE) {
                            chunks.add(new ArrayList<>(entry.getValue().subList(from, to)));
                        }
                    }
                    expected.put(entry.getKey(), chunks);
                }
                Map<Integer, List<List<Item>>> actual = plan(left).countWindowsByKey(Item::key,
                        width, tail, left.size(), 6);
                eq(actual, expected, "generated chunk oracle");
                eq(new ArrayList<>(actual.keySet()), new ArrayList<>(expected.keySet()),
                        "generated chunk key order");
            }
            int before = random.nextInt(4);
            int after = random.nextInt(4);
            ArrayList<List<Item>> neighborhoods = new ArrayList<>();
            for (Item item : left) {
                List<Item> group = groups.get(item.key());
                int at = group.indexOf(item);
                neighborhoods.add(new ArrayList<>(group.subList(Math.max(0, at - before),
                        Math.min(group.size(), at + after + 1))));
            }
            eq(plan(left).mapWindowsByKey(Item::key, before, after, (center, window) -> window,
                    left.size(), 6).toBoundedList(left.size()), neighborhoods,
                    "generated centered oracle");
            int k = random.nextInt(7);
            LinkedHashMap<Integer, List<Item>> greatest = new LinkedHashMap<>();
            for (Map.Entry<Integer, List<Item>> entry : groups.entrySet()) {
                ArrayList<Item> sorted = new ArrayList<>(entry.getValue());
                sorted.sort(Comparator.comparingInt(Item::score).reversed());
                greatest.put(entry.getKey(),
                        new ArrayList<>(sorted.subList(0, Math.min(k, sorted.size()))));
            }
            Map<Integer, List<Item>> actual = plan(left).topKByKey(Item::key, k,
                    Comparator.comparingInt(Item::score), left.size(), 6);
            eq(actual, greatest, "generated stable top-K oracle");
            eq(new ArrayList<>(actual.keySet()), new ArrayList<>(greatest.keySet()),
                    "generated top-K key order");
        }
    }

    private static List<Item> generated(Random random, int size) {
        ArrayList<Item> result = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            int key = random.nextInt(6);
            result.add(new Item(key == 5 ? null : key, random.nextInt(4), i));
        }
        return result;
    }

    private static final class CollisionKey {
        private final int value;
        CollisionKey(int value) { this.value = value; }
        @Override public int hashCode() { return 0; }
        @Override public boolean equals(Object other) {
            return other instanceof CollisionKey key && value == key.value;
        }
    }

    private static final class Traced<T> extends AbstractCollection<T> {
        private final List<T> values;
        int opens;
        int pulls;
        int closes;
        RuntimeException openFailure;
        RuntimeException closeFailure;
        Traced(List<T> values) { this.values = values; }
        @Override public int size() { return values.size(); }
        @Override public Iterator<T> iterator() { return values.iterator(); }
        @Override public Stream<T> stream() {
            opens++;
            if (openFailure != null) throw openFailure;
            return values.stream().peek(value -> pulls++).onClose(() -> {
                closes++;
                if (closeFailure != null) throw closeFailure;
            });
        }
    }

    private static void eq(Object actual, Object expected, String label) {
        check(Objects.equals(actual, expected), label + ": " + actual + " != " + expected);
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    private static Throwable fails(Class<? extends Throwable> expected, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable failure) {
            if (expected.isInstance(failure)) return failure;
            throw new AssertionError("expected " + expected.getName(), failure);
        }
        throw new AssertionError("expected " + expected.getName());
    }
}
