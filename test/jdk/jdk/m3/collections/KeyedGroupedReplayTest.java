/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary Bound strict keyed accumulators and explicit acknowledged replay
 *          on the existing scheduler
 * @modules jdk.m3.collections
 * @run main/othervm/timeout=180 KeyedGroupedReplayTest
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;
import jdk.m3.collections.KeyedParallelismPlan;
import jdk.m3.collections.ObjectStream;

public final class KeyedGroupedReplayTest {
    private static final AtomicInteger CHECKS = new AtomicInteger();
    private static final AtomicInteger GROUP_IDS = new AtomicInteger();
    private static final long WAIT_SECONDS = 10;

    private record Key(int value) {
        @Override public int hashCode() { return 7; }
    }
    private record Item(Key key, int value) { }
    private record Frame(long marker, int key, int value, int epoch) { }

    private static final class Accumulator {
        final Thread owner = Thread.currentThread();
        final ArrayList<Integer> values = new ArrayList<>();
    }

    public static void main(String[] args) throws Exception {
        strictGroupingAndCallbackPlacement();
        nullableStrictFoldAndCollector();
        pagedStateAndFirstKeyOrder();
        validationAndBudgets();
        failuresAndSourceClose();
        barriersAndReadAhead();
        replayAndImmutablePolicies();
        markerValidationAndBudgets();
        selfInterruptedCallbackIsRefused();
        cancelledWorkersAreJoined();
        System.out.println("KEYED_GROUPED_REPLAY_PASS checks=" + CHECKS.get());
    }

    private static void strictGroupingAndCallbackPlacement() {
        Thread caller = Thread.currentThread();
        Key first = new Key(1);
        Key second = new Key(2);
        List<Item> source = Arrays.asList(new Item(first, 10), new Item(second, 20),
                new Item(null, 30), new Item(new Key(1), 11), null,
                new Item(new Key(2), 21), new Item(new Key(1), 12));
        AtomicInteger keys = new AtomicInteger();
        AtomicInteger signals = new AtomicInteger();
        AtomicInteger seeds = new AtomicInteger();
        AtomicInteger accumulated = new AtomicInteger();
        AtomicInteger finished = new AtomicInteger();
        AtomicInteger closed = new AtomicInteger();
        var plan = tracked(source, ignored -> { }, closed::incrementAndGet)
                .parallelByKey(item -> {
                    same(caller, Thread.currentThread(), "key extraction on caller");
                    keys.incrementAndGet();
                    return item == null ? null : item.key();
                }).partitionSignal(key -> {
                    same(caller, Thread.currentThread(), "partition signal on caller");
                    signals.incrementAndGet();
                    return key == null ? Integer.MIN_VALUE : key.value();
                });
        Map<Key, List<Integer>> result = plan.reduceByKey(() -> {
            yes(Thread.currentThread() != caller, "seed on worker");
            seeds.incrementAndGet();
            return new Accumulator();
        }, (state, item) -> {
            same(state.owner, Thread.currentThread(), "single owner per key");
            state.values.add(item == null ? -1 : item.value());
            accumulated.incrementAndGet();
        }, state -> {
            same(caller, Thread.currentThread(), "finisher on caller");
            equal(1, closed.get(), "source closed before finisher");
            equal(source.size(), accumulated.get(), "all accumulation before finisher");
            finished.incrementAndGet();
            return state.values;
        }, 3, 2, source.size(), 3);
        equal(source.size(), keys.get(), "one key extraction per data item");
        equal(source.size(), signals.get(), "one partition signal per data item");
        equal(3, seeds.get(), "one seed per equal key including null");
        equal(3, finished.get(), "one finisher per key");
        equal(Arrays.asList(first, second, null), new ArrayList<>(result.keySet()),
                "first-key order");
        same(first, result.keySet().iterator().next(), "first equal key representative");
        equal(List.of(10, 11, 12), result.get(new Key(1)), "strict first key order");
        equal(List.of(20, 21), result.get(second), "strict second key order");
        equal(List.of(30, -1), result.get(null), "null keys and values");
        raises(UnsupportedOperationException.class, () -> result.put(first, List.of()));
        raises(UnsupportedOperationException.class,
                () -> result.entrySet().iterator().next().setValue(List.of()));
        raises(UnsupportedOperationException.class, result::clear);
        result.get(first).add(99);
        equal(List.of(10, 11, 12, 99), result.get(first),
                "caller accumulator payload remains mutable");
    }

    private static void nullableStrictFoldAndCollector() {
        AtomicInteger seeds = new AtomicInteger();
        AtomicInteger calls = new AtomicInteger();
        Map<Integer, String> nullable = ObjectStream.snapshot(Arrays.asList(0, 1, 0, 1, null))
                .parallelByKey(value -> value).<String>foldByKey(() -> {
                    seeds.incrementAndGet();
                    return null;
                }, (state, value) -> {
                    equal(null, state, "nullable state retained without reseeding");
                    calls.incrementAndGet();
                    return null;
                }, 3, 1, 5, 3);
        equal(3, seeds.get(), "nullable seed once per key");
        equal(5, calls.get(), "nullable folder every item");
        equal(Arrays.asList(0, 1, null), new ArrayList<>(nullable.keySet()),
                "nullable result order");
        yes(nullable.containsKey(null), "null key retained");
        yes(nullable.containsValue(null), "null value retained");
        equal(null, nullable.getOrDefault(0, "absent"), "mapped null differs from absent");
        equal("absent", nullable.getOrDefault(3, "absent"), "absent default");

        Map<Integer, Integer> strict = ObjectStream.snapshot(List.of(1, 2, 3, 4, 5, 6))
                .parallelByKey(value -> value % 2)
                .foldByKey(() -> 100, (state, value) -> state - value, 2, 2, 6, 2);
        equal(91, strict.get(1).intValue(), "nonassociative odd fold");
        equal(88, strict.get(0).intValue(), "nonassociative even fold");

        AtomicInteger combined = new AtomicInteger();
        AtomicInteger finishers = new AtomicInteger();
        Thread caller = Thread.currentThread();
        Collector<Integer, ArrayList<Integer>, String> collector = Collector.of(
                ArrayList::new, List::add,
                (left, right) -> {
                    combined.incrementAndGet(); throw new AssertionError("combiner called");
                },
                values -> {
                    same(caller, Thread.currentThread(), "collector finisher thread");
                    finishers.incrementAndGet();
                    return values.toString();
                });
        Map<Integer, String> collected = ObjectStream.snapshot(Arrays.asList(2, 1, 4, null, 3))
                .parallelByKey(value -> value == null ? null : value % 2)
                .collectByKey(collector, 3, 2, 5, 3);
        equal("[2, 4]", collected.get(0), "collector even values");
        equal("[1, 3]", collected.get(1), "collector odd values");
        equal("[null]", collected.get(null), "collector null payload");
        equal(0, combined.get(), "collector never combines");
        equal(3, finishers.get(), "collector finishes each key once");

        Map<Integer, String> finishedNull = ObjectStream.snapshot(List.of(0, 0))
                .parallelByKey(value -> value).reduceByKey(ArrayList<Integer>::new, List::add,
                        ignored -> (String) null, 1, 1, 2, 1);
        yes(finishedNull.containsKey(0), "nullable finisher keeps mapping");
        equal(null, finishedNull.get(0), "nullable finisher result");
    }

    private static void pagedStateAndFirstKeyOrder() {
        int distinct = 1_031;
        List<Integer> values = new ArrayList<>();
        for (int key = 0; key < distinct; key++) values.add(key);
        for (int key = distinct - 1; key >= 0; key--) values.add(key);
        for (int flight : new int[] {1, 7, 64}) {
            AtomicInteger seeds = new AtomicInteger();
            Map<Integer, ArrayList<Integer>> grouped = ObjectStream.snapshot(values)
                    .parallelByKey(value -> value).collectByKey(() -> {
                        seeds.incrementAndGet();
                        return new ArrayList<Integer>();
                    }, List::add, 5, flight, values.size(), distinct);
            equal(distinct, seeds.get(), "paged seed cardinality");
            equal(distinct, grouped.size(), "paged key cardinality");
            int ordinal = 0;
            for (Map.Entry<Integer, ArrayList<Integer>> entry : grouped.entrySet()) {
                equal(ordinal, entry.getKey().intValue(), "paged first-key order");
                equal(List.of(ordinal, ordinal), entry.getValue(), "paged state visibility");
                ordinal++;
            }
        }
    }

    private static void validationAndBudgets() {
        AtomicInteger observed = new AtomicInteger();
        AtomicInteger closed = new AtomicInteger();
        var plan = tracked(List.of(1), value -> observed.incrementAndGet(),
                closed::incrementAndGet).parallelByKey(value -> value);
        raises(IllegalArgumentException.class,
                () -> plan.collectByKey(ArrayList::new, List::add, 0, 1, 1, 1));
        raises(IllegalArgumentException.class,
                () -> plan.collectByKey(ArrayList::new, List::add, 257, 1, 1, 1));
        raises(IllegalArgumentException.class,
                () -> plan.collectByKey(ArrayList::new, List::add, 1, 0, 1, 1));
        raises(IllegalArgumentException.class,
                () -> plan.collectByKey(ArrayList::new, List::add, 1, 65_537, 1, 1));
        raises(IllegalArgumentException.class,
                () -> plan.collectByKey(ArrayList::new, List::add, 1, 1, -1, 1));
        raises(IllegalArgumentException.class,
                () -> plan.collectByKey(ArrayList::new, List::add, 1, 1, 1, -1));
        raises(IllegalArgumentException.class,
                () -> plan.collectByKey(ArrayList::new, List::add, 1, 1, 1, 1_048_577));
        raises(NullPointerException.class, () -> plan.collectByKey(
                (Supplier<ArrayList<Integer>>) null, List::add, 1, 1, 1, 1));
        raises(NullPointerException.class,
                () -> plan.collectByKey(ArrayList<Integer>::new, null, 1, 1, 1, 1));
        raises(NullPointerException.class,
                () -> plan.reduceByKey(ArrayList<Integer>::new, List::add, null, 1, 1, 1, 1));
        raises(NullPointerException.class, () -> plan.foldByKey(() -> 0, null, 1, 1, 1, 1));
        raises(NullPointerException.class, () -> plan.forEachKeyOrdered(null, 1, 1, 1));
        raises(NullPointerException.class,
                () -> plan.collectByKey((Collector<Integer, ?, Integer>) null, 1, 1, 1, 1));
        equal(0, observed.get(), "invalid invocation leaves source unopened");
        equal(0, closed.get(), "invalid invocation does not take source ownership");
        equal(Map.of(1, List.of(1)), plan.collectByKey(ArrayList::new, List::add, 1, 1, 1, 1),
                "plan remains usable after invalid arguments");
        equal(1, closed.get(), "valid invocation closes once");

        equal(Map.of(), ObjectStream.<Integer>snapshot(List.of()).parallelByKey(value -> value)
                .collectByKey(ArrayList::new, List::add, 1, 1, 0, 0),
                "zero limits accept empty source");
        AtomicInteger overflowKeys = new AtomicInteger();
        raises(IllegalStateException.class, () -> ObjectStream.snapshot(List.of(0, 1))
                .parallelByKey(value -> { overflowKeys.incrementAndGet(); return value; })
                .collectByKey(ArrayList::new, List::add, 1, 1, 1, 2));
        equal(1, overflowKeys.get(), "overflow item not keyed");
        raises(IllegalStateException.class, () -> ObjectStream.snapshot(List.of(0))
                .parallelByKey(value -> value).collectByKey(ArrayList::new, List::add, 1, 1, 1, 0));
        raises(IllegalStateException.class, () -> ObjectStream.snapshot(List.of(0, 1))
                .parallelByKey(value -> value).collectByKey(ArrayList::new, List::add, 1, 1, 2, 1));

        AtomicInteger signal = new AtomicInteger();
        raises(IllegalArgumentException.class, () -> ObjectStream.snapshot(List.of(1, 1))
                .parallelByKey(value -> value).partitionSignal(key -> signal.getAndIncrement())
                .collectByKey(ArrayList::new, List::add, 2, 1, 2, 1));
    }

    private static void failuresAndSourceClose() throws Exception {
        for (Throwable marker : List.of(new IllegalStateException("worker marker"),
                new AssertionError("worker error"))) {
            AtomicInteger closed = new AtomicInteger();
            AtomicInteger finishes = new AtomicInteger();
            try (Invocation<Map<Integer, Integer>> call = new Invocation<>(() ->
                    tracked(List.of(1, 2, 3), ignored -> { }, closed::incrementAndGet)
                            .parallelByKey(value -> 0).reduceByKey(() -> new int[1],
                                    (state, value) -> rethrow(marker), state -> {
                                        finishes.incrementAndGet(); return state[0];
                                    }, 2, 4, 3, 1))) {
                same(marker, raises(ExecutionException.class, call::get).getCause(),
                        "worker failure identity");
                call.assertQuiescent();
            }
            equal(1, closed.get(), "worker failure closes source");
            equal(0, finishes.get(), "worker failure prevents finisher");
        }
        AtomicInteger closed = new AtomicInteger();
        raises(NullPointerException.class,
                () -> tracked(List.of(1), ignored -> { }, closed::incrementAndGet)
                .parallelByKey(value -> value)
                .collectByKey(() -> null, (state, value) -> { }, 1, 1, 1, 1));
        equal(1, closed.get(), "null seed closes source");

        IllegalStateException closeFailure = new IllegalStateException("close failure");
        AtomicInteger finishes = new AtomicInteger();
        same(closeFailure, raises(IllegalStateException.class,
                () -> tracked(List.of(1), ignored -> { },
                () -> { throw closeFailure; }).parallelByKey(value -> value)
                .reduceByKey(ArrayList<Integer>::new, List::add, state -> {
                    finishes.incrementAndGet(); return state;
                }, 1, 1, 1, 1)), "source close failure identity");
        equal(0, finishes.get(), "source close failure prevents finisher");

        IllegalArgumentException primary = new IllegalArgumentException("primary");
        IllegalStateException secondary = new IllegalStateException("secondary close");
        Throwable failure = raises(IllegalArgumentException.class,
                () -> tracked(List.of(1), ignored -> { },
                () -> { throw secondary; }).parallelByKey(value -> value)
                .collectByKey(ArrayList<Integer>::new,
                        (state, value) -> { throw primary; }, 1, 1, 1, 1));
        same(primary, failure, "primary failure retained");
        equal(1, failure.getSuppressed().length, "source close suppressed once");
        same(secondary, failure.getSuppressed()[0], "exact suppressed close failure");

        AtomicInteger finishClose = new AtomicInteger();
        RuntimeException finishFailure = new RuntimeException("finisher failure");
        same(finishFailure, raises(RuntimeException.class, () -> tracked(List.of(1), ignored -> { },
                finishClose::incrementAndGet).parallelByKey(value -> value)
                .reduceByKey(ArrayList<Integer>::new, List::add,
                        state -> { throw finishFailure; }, 1, 1, 1, 1)),
                "finisher failure identity");
        equal(1, finishClose.get(), "source closed before failing finisher");
    }

    private static void barriersAndReadAhead() {
        int epochs = 3;
        int perEpoch = 53;
        List<Frame> frames = frames(epochs, perEpoch);
        Thread caller = Thread.currentThread();
        AtomicInteger pulled = new AtomicInteger();
        AtomicInteger classified = new AtomicInteger();
        AtomicInteger keys = new AtomicInteger();
        AtomicInteger acknowledged = new AtomicInteger();
        AtomicInteger closed = new AtomicInteger();
        AtomicInteger[] counts = {new AtomicInteger(), new AtomicInteger(), new AtomicInteger()};
        List<Long> markers = new ArrayList<>();
        var plan = tracked(frames, frame -> pulled.incrementAndGet(),
                closed::incrementAndGet).parallelByKey(frame -> {
                    yes(frame.marker() == -1, "marker never keyed");
                    keys.incrementAndGet();
                    return frame.key();
                }).partitionSignal(key -> key).markers(frame -> {
                    same(caller, Thread.currentThread(), "classifier caller thread");
                    classified.incrementAndGet();
                    return frame.marker();
                }, marker -> {
                    same(caller, Thread.currentThread(), "acknowledgement caller thread");
                    int epoch = Math.toIntExact(marker / 10 - 1);
                    equal(perEpoch, counts[epoch].get(), "barrier waits for prior data");
                    equal((epoch + 1) * (perEpoch + 1), pulled.get(),
                            "no pull past marker before acknowledgement");
                    if (epoch + 1 < epochs)
                        equal(0, counts[epoch + 1].get(), "no next-epoch action");
                    markers.add(marker);
                    acknowledged.incrementAndGet();
                }, frames.size());
        KeyedParallelismPlan.Receipt receipt = plan.forEachKeyOrdered(frame -> {
            equal(frame.epoch(), acknowledged.get(), "action waits for previous acknowledgement");
            counts[frame.epoch()].incrementAndGet();
        }, 3, 2, epochs * perEpoch);
        equal(epochs * perEpoch, receipt.delivered(), "receipt delivered count");
        equal(epochs, receipt.markersAcknowledged(), "receipt marker count");
        equal(30L, receipt.lastMarkerId(), "receipt last marker");
        equal(List.of(10L, 20L, 30L), markers, "marker order");
        equal(frames.size(), classified.get(), "one classification per frame");
        equal(epochs * perEpoch, keys.get(), "one key per data item");
        equal(1, closed.get(), "marked source closed once");
    }

    private static void replayAndImmutablePolicies() {
        List<Frame> frames = frames(3, 7);
        AtomicInteger keys = new AtomicInteger();
        List<Long> acknowledged = new ArrayList<>();
        var original = ObjectStream.snapshot(frames).parallelByKey(frame -> {
            keys.incrementAndGet(); return frame.key();
        }).markers(Frame::marker, acknowledged::add, frames.size());
        var replay = original.reprocessAfter(10).partitionSignal(key -> -key);
        List<Integer> tail = replay.mapOrdered(Frame::value, 3, 2, 14);
        equal(14, keys.get(), "replay skips key extraction before marker");
        equal(List.of(20L, 30L), acknowledged, "replay acknowledges only subsequent markers");
        List<Integer> expected = frames.stream()
                .filter(frame -> frame.marker() < 0 && frame.epoch() > 0)
                .map(Frame::value).toList();
        equal(expected, tail, "marked map preserves source order");
        keys.set(0);
        acknowledged.clear();
        equal(21, original.mapOrdered(Frame::value, 3, 2, 21).size(),
                "original immutable plan still complete");
        equal(21, keys.get(), "original key callbacks unchanged by replay configuration");
        equal(List.of(10L, 20L, 30L), acknowledged, "original acknowledgements unchanged");

        acknowledged.clear();
        keys.set(0);
        raises(IllegalArgumentException.class, () -> original.reprocessAfter(999)
                .forEachKeyOrdered(frame -> {
                    throw new AssertionError("missing replay performed action");
                }, 2, 1, 0));
        equal(0, keys.get(), "missing replay performs no key extraction");
        equal(List.of(), acknowledged, "missing replay performs no acknowledgements");
        KeyedParallelismPlan.Receipt end = original.reprocessAfter(30)
                .forEachKeyOrdered(frame -> {
                    throw new AssertionError("replay past final marker");
                }, 2, 1, 0);
        equal(0L, end.delivered(), "final marker replay is empty");
        equal(0L, end.markersAcknowledged(), "matching marker is not acknowledged");
        equal(-1L, end.lastMarkerId(), "no new marker sentinel");

        Map<Integer, ArrayList<Integer>> grouped = original.reprocessAfter(20)
                .collectByKey(ArrayList<Integer>::new,
                        (values, frame) -> values.add(frame.value()), 3, 2, 7, 3);
        equal(List.of(200, 203, 206), grouped.get(0), "stateful replay first group");
        equal(List.of(201, 204), grouped.get(1), "stateful replay second group");
        equal(List.of(202, 205), grouped.get(2), "stateful replay third group");

        AtomicInteger onceClosed = new AtomicInteger();
        var once = ObjectStream.once(frames.stream().onClose(onceClosed::incrementAndGet));
        raises(IllegalStateException.class, () -> once.parallelByKey(Frame::key));
        equal(0, onceClosed.get(), "one-shot keyed refusal leaves source ownership unchanged");
        equal(frames, once.toBoundedList(frames.size()),
                "one-shot source remains usable after keyed refusal");
        equal(1, onceClosed.get(), "one-shot source eventually closes once");
    }

    private static void markerValidationAndBudgets() {
        var empty = ObjectStream.<Frame>snapshot(List.of()).parallelByKey(Frame::key);
        raises(IllegalStateException.class, () -> empty.reprocessAfter(0));
        raises(IllegalArgumentException.class, () -> empty.reprocessAfter(-1));
        raises(NullPointerException.class, () -> empty.markers(null, ignored -> { }, 0));
        raises(NullPointerException.class, () -> empty.markers(Frame::marker, null, 0));
        raises(IllegalArgumentException.class,
                () -> empty.markers(Frame::marker, ignored -> { }, -1));
        equal(new KeyedParallelismPlan.Receipt(0, 0, -1),
                empty.markers(Frame::marker, ignored -> { }, 0)
                .forEachKeyOrdered(ignored -> { }, 1, 1, 0), "empty marked receipt");

        List<Frame> markers = List.of(new Frame(0, 0, 0, 0), new Frame(Long.MAX_VALUE, 0, 0, 0));
        List<Long> acknowledged = new ArrayList<>();
        KeyedParallelismPlan.Receipt receipt = ObjectStream.snapshot(markers)
                .parallelByKey(frame -> { throw new AssertionError("marker keyed"); })
                .markers(Frame::marker, acknowledged::add, 2).forEachKeyOrdered(frame -> {
                    throw new AssertionError("marker delivered");
                }, 1, 1, 0);
        equal(List.of(0L, Long.MAX_VALUE), acknowledged, "full nonnegative marker range");
        equal(2L, receipt.markersAcknowledged(), "consecutive markers");

        for (List<Frame> malformed : List.of(
                List.of(new Frame(-2, 0, 0, 0)),
                List.of(new Frame(1, 0, 0, 0), new Frame(1, 0, 0, 0)),
                List.of(new Frame(2, 0, 0, 0), new Frame(1, 0, 0, 0)))) {
            raises(IllegalArgumentException.class, () -> ObjectStream.snapshot(malformed)
                    .parallelByKey(Frame::key)
                    .markers(Frame::marker, ignored -> { }, malformed.size())
                    .forEachKeyOrdered(ignored -> { }, 1, 1, 0));
        }
        AtomicInteger classified = new AtomicInteger();
        AtomicInteger keyed = new AtomicInteger();
        raises(IllegalStateException.class,
                () -> ObjectStream.snapshot(List.of(new Frame(-1, 0, 1, 0)))
                .parallelByKey(frame -> { keyed.incrementAndGet(); return frame.key(); })
                .markers(frame -> { classified.incrementAndGet(); return frame.marker(); },
                        ignored -> { }, 0)
                .forEachKeyOrdered(ignored -> { }, 1, 1, 1));
        equal(0, classified.get(), "frame bound before classifier");
        equal(0, keyed.get(), "frame bound before key");
        raises(IllegalStateException.class, () -> ObjectStream.snapshot(frames(2, 3))
                .parallelByKey(frame -> { keyed.incrementAndGet(); return frame.key(); })
                .markers(Frame::marker, ignored -> { }, 3).reprocessAfter(10)
                .forEachKeyOrdered(ignored -> { }, 1, 1, 3));
        equal(0, keyed.get(), "replay frame budget protects skip without keys");

        AtomicInteger pulled = new AtomicInteger();
        AtomicInteger closed = new AtomicInteger();
        RuntimeException failure = new RuntimeException("ack failure");
        same(failure, raises(RuntimeException.class, () -> tracked(frames(2, 1),
                frame -> pulled.incrementAndGet(), closed::incrementAndGet)
                .parallelByKey(Frame::key).markers(Frame::marker, ignored -> { throw failure; }, 4)
                .forEachKeyOrdered(ignored -> { }, 1, 1, 2)), "ack failure identity");
        equal(2, pulled.get(), "ack failure prevents next frame pull");
        equal(1, closed.get(), "ack failure closes source");

        AtomicInteger acknowledgements = new AtomicInteger();
        RuntimeException worker = new RuntimeException("before marker");
        same(worker, raises(RuntimeException.class, () -> ObjectStream.snapshot(frames(1, 1))
                .parallelByKey(Frame::key)
                .markers(Frame::marker, ignored -> acknowledgements.incrementAndGet(), 2)
                .forEachKeyOrdered(ignored -> { throw worker; }, 1, 1, 1)),
                "failure before marker identity");
        equal(0, acknowledgements.get(), "worker failure prevents acknowledgement");
    }

    private static void selfInterruptedCallbackIsRefused() throws Exception {
        AtomicInteger closed = new AtomicInteger();
        try (Invocation<KeyedParallelismPlan.Receipt> call = new Invocation<>(() ->
                tracked(List.of(1), ignored -> { }, closed::incrementAndGet)
                        .parallelByKey(value -> value).forEachKeyOrdered(value ->
                                Thread.currentThread().interrupt(), 1, 1, 1))) {
            Throwable failure = raises(ExecutionException.class, call::get).getCause();
            yes(failure instanceof IllegalStateException, "self-interrupted callback refuses");
            yes(failure.getCause() instanceof InterruptedException,
                    "worker interruption cause retained");
            call.assertQuiescent();
        }
        equal(1, closed.get(), "self-interrupted callback closes source");
    }

    private static void cancelledWorkersAreJoined() throws Exception {
        CountDownLatch fourthKey = new CountDownLatch(1);
        CountDownLatch interruptedWorker = new CountDownLatch(1);
        CountDownLatch holdWorker = new CountDownLatch(1);
        AtomicBoolean restoredInterrupt = new AtomicBoolean();
        AtomicInteger closed = new AtomicInteger();
        try (Invocation<Map<Integer, Integer>> call = new Invocation<>(() -> {
            try {
                return tracked(List.of(0, 1, 2, 3, 4), ignored -> { }, closed::incrementAndGet)
                        .parallelByKey(value -> {
                            if (value == 3) fourthKey.countDown(); return 0;
                        })
                        .foldByKey(() -> 0, (state, value) -> {
                            try {
                                holdWorker.await();
                                throw new AssertionError("worker released without cancellation");
                            } catch (InterruptedException interruption) {
                                interruptedWorker.countDown();
                                Thread.currentThread().interrupt();
                                throw new IllegalStateException(interruption);
                            }
                        }, 2, 4, 5, 1);
            } catch (RuntimeException failure) {
                restoredInterrupt.set(Thread.currentThread().isInterrupted());
                throw failure;
            }
        })) {
            yes(fourthKey.await(WAIT_SECONDS, TimeUnit.SECONDS),
                    "coordinator reaches saturated lane");
            call.thread.interrupt();
            Throwable failure = raises(ExecutionException.class, call::get).getCause();
            yes(failure instanceof IllegalStateException, "interruption wrapped consistently");
            yes(failure.getCause() instanceof InterruptedException, "interruption cause retained");
            yes(restoredInterrupt.get(), "caller interrupt flag restored");
            yes(interruptedWorker.getCount() == 0, "worker interrupted before return");
            call.assertQuiescent();
        }
        equal(1, closed.get(), "interruption closes source");
    }

    private static <E> ObjectStream<E> tracked(List<? extends E> values,
            Consumer<? super E> pulled, Runnable close) {
        return ObjectStream.live(new TrackedList<>(values, pulled, close));
    }

    /** A real replayable Collection with a fresh close-tracked stream per open. */
    private static final class TrackedList<E> extends ArrayList<E> {
        private static final long serialVersionUID = 1L;
        private final transient Consumer<? super E> pulled;
        private final transient Runnable close;

        private TrackedList(List<? extends E> values, Consumer<? super E> pulled, Runnable close) {
            super(values);
            this.pulled = pulled;
            this.close = close;
        }

        @Override public Stream<E> stream() { return super.stream().peek(pulled).onClose(close); }
    }

    private static List<Frame> frames(int epochs, int count) {
        List<Frame> frames = new ArrayList<>();
        for (int epoch = 0; epoch < epochs; epoch++) {
            for (int index = 0; index < count; index++)
                frames.add(new Frame(-1, index % 3, epoch * 100 + index, epoch));
            frames.add(new Frame((epoch + 1L) * 10, 0, 0, epoch));
        }
        return frames;
    }

    @FunctionalInterface
    private interface Checked { void run() throws Exception; }

    private static <E extends Throwable> E raises(Class<E> type, Checked action) {
        try {
            action.run();
        } catch (Throwable failure) {
            yes(type.isInstance(failure), "expected " + type.getName() + " but caught " + failure);
            return type.cast(failure);
        }
        throw new AssertionError("missing " + type.getName());
    }

    private static void rethrow(Throwable failure) {
        if (failure instanceof RuntimeException runtime) throw runtime;
        if (failure instanceof Error error) throw error;
        throw new AssertionError(failure);
    }

    private static void yes(boolean value, String message) {
        CHECKS.incrementAndGet();
        if (!value) throw new AssertionError(message);
    }

    private static void equal(Object expected, Object actual, String message) {
        CHECKS.incrementAndGet();
        if (!Objects.equals(expected, actual))
            throw new AssertionError(message + ": " + expected + " != " + actual);
    }

    private static void equal(long expected, long actual, String message) {
        CHECKS.incrementAndGet();
        if (expected != actual)
            throw new AssertionError(message + ": " + expected + " != " + actual);
    }

    private static void same(Object expected, Object actual, String message) {
        CHECKS.incrementAndGet();
        if (expected != actual) throw new AssertionError(message + ": identity differs");
    }

    private static final class Invocation<V> implements AutoCloseable {
        private final ThreadGroup group = new ThreadGroup(
                "m3-grouped-replay-test-" + GROUP_IDS.incrementAndGet());
        private final FutureTask<V> task;
        private final Thread thread;

        private Invocation(Callable<V> operation) {
            task = new FutureTask<>(operation);
            thread = new Thread(group, task, "m3-grouped-replay-coordinator");
            thread.start();
        }

        private V get() throws Exception { return task.get(WAIT_SECONDS, TimeUnit.SECONDS); }

        private void assertQuiescent() throws InterruptedException {
            thread.join(TimeUnit.SECONDS.toMillis(WAIT_SECONDS));
            List<Thread> live = live();
            yes(live.isEmpty(), "new terminal leaves no live owned workers: " + live);
        }

        private List<Thread> live() {
            return Thread.getAllStackTraces().keySet().stream()
                    .filter(candidate -> candidate.getThreadGroup() == group && candidate.isAlive())
                    .toList();
        }

        @Override public void close() {
            task.cancel(true);
            boolean interrupted = false;
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS);
            try {
                for (;;) {
                    List<Thread> live = live();
                    if (live.isEmpty()) return;
                    for (Thread remaining : live) remaining.interrupt();
                    if (System.nanoTime() >= deadline)
                        throw new AssertionError("test leaked workers: " + live);
                    for (Thread remaining : live) {
                        try { remaining.join(20); }
                        catch (InterruptedException interruption) { interrupted = true; }
                    }
                }
            } finally {
                if (interrupted) Thread.currentThread().interrupt();
            }
        }
    }
}
