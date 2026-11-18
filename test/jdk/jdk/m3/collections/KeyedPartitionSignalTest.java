/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary Recover immutable custom key partition signals without changing default routing
 * @modules jdk.m3.collections
 * @run main/othervm KeyedPartitionSignalTest
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;
import jdk.m3.collections.KeyedParallelismPlan;
import jdk.m3.collections.ObjectStream;

/** Tests the public owner using real collection, snapshot, selector and queue implementations. */
public final class KeyedPartitionSignalTest {
    private static final AtomicInteger CHECKS = new AtomicInteger();

    private record Key(int id, int signal, int hash) {
        @Override public int hashCode() { return hash; }
    }

    private record Row(Key key, int ordinal) { }

    private KeyedPartitionSignalTest() { }

    public static void main(String[] args) throws Exception {
        defaultPolicyUnchanged();
        rawSignalsAndImmutablePlans();
        callbackValidationOrder();
        actualLanesAndOrderedResults();
        nullKeysAndValues();
        failureIdentityAndSourceClosure();
        System.out.println("KEYED_PARTITION_SIGNAL_PASS checks=" + CHECKS.get());
    }

    private static void defaultPolicyUnchanged() {
        var plan = ObjectStream.<Key>snapshot(List.of()).parallelByKey(key -> key);
        int[] hashes = {Integer.MIN_VALUE, -65_537, -65_536, -1, 0, 1,
                65_535, 65_536, 65_537, Integer.MAX_VALUE};
        for (int partitions = 1; partitions <= 256; partitions++) {
            equal(0, plan.laneForKey(null, partitions), "default null routing");
            for (int hash : hashes) {
                Key key = new Key(0, 73, hash);
                int smeared = hash ^ (hash >>> 16);
                equal(Math.floorMod(smeared, partitions),
                        plan.laneForKey(key, partitions), "existing smeared hash");
            }
        }
    }

    private static void rawSignalsAndImmutablePlans() {
        var original = ObjectStream.<Key>snapshot(List.of()).parallelByKey(key -> key);
        ToIntFunction<Object> supertypeSignal = key -> ((Key) key).signal();
        var custom = original.partitionSignal(supertypeSignal);
        var replacement = custom.partitionSignal(key -> -17);
        check(original != custom && custom != replacement, "each policy returns a new plan");
        int[] signals = {Integer.MIN_VALUE, -65_537, -65_536, -17, -1, 0, 1,
                65_535, 65_536, 65_537, 65_539, Integer.MAX_VALUE};
        for (int partitions = 1; partitions <= 256; partitions++) {
            for (int signal : signals) {
                Key key = new Key(1, signal, 0);
                equal(Math.floorMod(signal, partitions),
                        custom.laneForKey(key, partitions), "raw signal floorMod");
                equal(Math.floorMod(-17, partitions),
                        replacement.laneForKey(key, partitions), "replacement policy");
                equal(0, original.laneForKey(key, partitions), "original plan remains unchanged");
            }
        }
        AtomicInteger calls = new AtomicInteger();
        var lazy = original.partitionSignal(key -> {
            calls.incrementAndGet();
            return key == null ? Integer.MIN_VALUE : key.signal();
        });
        equal(0, calls.get(), "configuration does not evaluate routing");
        equal(Math.floorMod(Integer.MIN_VALUE, 3), lazy.laneForKey(null, 3),
                "custom null callback owns routing");
        equal(1, calls.get(), "one callback for one diagnostic lookup");
    }

    private static void callbackValidationOrder() {
        CloseTrackingList<Integer> values = new CloseTrackingList<>(List.of(1, 2));
        AtomicInteger keys = new AtomicInteger();
        AtomicInteger signals = new AtomicInteger();
        var plan = ObjectStream.live(values).parallelByKey(value -> {
            keys.incrementAndGet();
            return value;
        }).partitionSignal(key -> {
            signals.incrementAndGet();
            return key;
        });
        expect(NullPointerException.class, () -> plan.partitionSignal(null));
        expect(IllegalArgumentException.class, () -> plan.laneForKey(1, 0));
        expect(IllegalArgumentException.class, () -> plan.laneForKey(1, 257));
        expect(NullPointerException.class, () -> plan.mapOrdered(null, 0, 0, -1));
        expect(IllegalArgumentException.class, () -> plan.mapOrdered(x -> x, 0, 2, 2));
        expect(IllegalArgumentException.class, () -> plan.mapOrdered(x -> x, 257, 2, 2));
        expect(IllegalArgumentException.class, () -> plan.mapOrdered(x -> x, 2, 0, 2));
        expect(IllegalArgumentException.class, () -> plan.mapOrdered(x -> x, 2, 65_537, 2));
        expect(IllegalArgumentException.class, () -> plan.mapOrdered(x -> x, 2, 2, -1));
        equal(0, values.opens.get(), "invalid arguments do not open the source");
        equal(0, keys.get(), "invalid arguments do not extract keys");
        equal(0, signals.get(), "invalid arguments do not invoke the signal");
        equal(List.of(1, 2), plan.mapOrdered(x -> x, 1, 1, 2),
                "minimum lane and admission settings remain usable");
        equal(2, keys.get(), "extract once per successful input");
        equal(2, signals.get(), "signal once per successful extracted key");
        equal(1, values.closes.get(), "successful traversal closes source");
    }

    private static void actualLanesAndOrderedResults() {
        int[] signals = {0, 65_536, 65_537, 3, 65_539};
        int rowsPerKey = 64;
        List<Row> input = new ArrayList<>();
        List<Integer> expected = new ArrayList<>();
        for (int ordinal = 0; ordinal < rowsPerKey; ordinal++) {
            for (int id = 0; id < signals.length; id++) {
                input.add(new Row(new Key(id, signals[id], 0), ordinal));
                expected.add(ordinal * signals.length + id);
            }
        }
        Thread coordinator = Thread.currentThread();
        AtomicInteger keyCalls = new AtomicInteger();
        AtomicInteger signalCalls = new AtomicInteger();
        var configured = ObjectStream.snapshot(input).parallelByKey(row -> {
            check(Thread.currentThread() == coordinator, "key extraction runs on coordinator");
            keyCalls.incrementAndGet();
            return row.key();
        }).partitionSignal(key -> {
            check(Thread.currentThread() == coordinator, "signal runs on coordinator");
            signalCalls.incrementAndGet();
            return key.signal();
        });
        for (int replay = 0; replay < 2; replay++) {
            CountDownLatch lanesStarted = new CountDownLatch(3);
            Map<Integer, Thread> laneThreads = new ConcurrentHashMap<>();
            AtomicInteger[] next = new AtomicInteger[signals.length];
            Arrays.setAll(next, ignored -> new AtomicInteger());
            List<Integer> actual = configured.mapOrdered(row -> {
                int lane = Math.floorMod(row.key().signal(), 3);
                Thread worker = Thread.currentThread();
                Thread previous = laneThreads.putIfAbsent(lane, worker);
                check(previous == null || previous == worker,
                        "keys with equal normalized signals use the same worker");
                check(worker != coordinator, "mapper uses a worker thread");
                equal(row.ordinal(), next[row.key().id()].getAndIncrement(), "per-key FIFO");
                if (row.ordinal() == 0 && row.key().id() < 3) {
                    lanesStarted.countDown();
                    await(lanesStarted, "three separately routed keys overlap");
                }
                return row.ordinal() * signals.length + row.key().id();
            }, 3, 12, input.size());
            equal(expected, actual, "global results retain source order");
            equal(3, laneThreads.size(), "all expected physical lanes observed");
            equal(3, new HashSet<>(laneThreads.values()).size(), "distinct lanes have distinct workers");
            for (AtomicInteger count : next) {
                equal(rowsPerKey, count.get(), "every per-key occurrence processed");
            }
            expect(UnsupportedOperationException.class, () -> actual.add(-1));
            System.out.println("KEYED_PARTITION_LANES replay=" + replay + " threads="
                    + laneThreads.entrySet().stream().sorted(Map.Entry.comparingByKey())
                            .map(entry -> entry.getKey() + ":" + entry.getValue().getName()).toList());
        }
        equal(input.size() * 2, keyCalls.get(), "one extraction per row per replay");
        equal(input.size() * 2, signalCalls.get(), "one signal per row per replay");
    }

    private static void nullKeysAndValues() {
        List<Integer> input = Arrays.asList(null, 1, null, 2, 3);
        AtomicInteger signalCalls = new AtomicInteger();
        AtomicInteger nullCalls = new AtomicInteger();
        var plan = ObjectStream.snapshot(input).parallelByKey(value -> value)
                .partitionSignal(key -> {
                    signalCalls.incrementAndGet();
                    if (key == null) {
                        nullCalls.incrementAndGet();
                        return Integer.MIN_VALUE;
                    }
                    return key * 65_537;
                });
        equal(Arrays.asList(null, 2, null, 4, 6),
                plan.mapOrdered(value -> value == null ? null : value * 2, 3, 2, input.size()),
                "null payloads and null mapped values survive custom routing");
        equal(input.size(), signalCalls.get(), "null keys still count once per occurrence");
        equal(2, nullCalls.get(), "null keys reach the client signal");
    }

    private static void failureIdentityAndSourceClosure() throws Exception {
        signalFailure(new IllegalStateException("signal runtime failure"), null);
        signalFailure(new AssertionError("signal error"), null);
        signalFailure(new IllegalArgumentException("signal with close failure"),
                new IllegalStateException("source close failure"));

        CloseTrackingList<Integer> values = new CloseTrackingList<>(List.of(1));
        AtomicInteger signals = new AtomicInteger();
        RuntimeException keyFailure = new IllegalStateException("key failure");
        var plan = ObjectStream.live(values).<Integer>parallelByKey(value -> {
            throw keyFailure;
        }).partitionSignal(key -> {
            signals.incrementAndGet();
            return key;
        });
        Throwable actual = capture(() -> plan.mapOrdered(value -> value, 2, 2, 1));
        check(actual == keyFailure, "key exception identity remains intact");
        equal(0, signals.get(), "failed extraction does not invoke routing");
        equal(1, values.closes.get(), "failed key extraction closes the source");
        awaitNoWorkers();
    }

    private static void signalFailure(Throwable failure, RuntimeException closeFailure)
            throws Exception {
        CloseTrackingList<Integer> values = new CloseTrackingList<>(List.of(1));
        values.closeFailure = closeFailure;
        AtomicInteger keys = new AtomicInteger();
        AtomicInteger signals = new AtomicInteger();
        AtomicInteger mappers = new AtomicInteger();
        var original = ObjectStream.live(values).parallelByKey(value -> {
            keys.incrementAndGet();
            return value;
        });
        var failing = original.partitionSignal(key -> {
            signals.incrementAndGet();
            if (failure instanceof RuntimeException runtime) throw runtime;
            throw (Error) failure;
        });
        Throwable actual = capture(() -> failing.mapOrdered(value -> {
            mappers.incrementAndGet();
            return value;
        }, 3, 3, 1));
        check(actual == failure, "signal exception or error identity remains intact");
        equal(1, values.opens.get(), "one source opened for failure");
        equal(1, values.closes.get(), "failing routing closes the source exactly once");
        equal(1, keys.get(), "one key extracted before signal failure");
        equal(1, signals.get(), "failing signal is not retried");
        equal(0, mappers.get(), "failed signal never admits its mapper");
        if (closeFailure != null) {
            equal(1, actual.getSuppressed().length, "close failure is suppressed");
            check(actual.getSuppressed()[0] == closeFailure, "close failure identity retained");
        }
        awaitNoWorkers();
        values.closeFailure = null;
        equal(List.of(1), original.mapOrdered(value -> value, 2, 2, 1),
                "failed derived policy leaves original plan usable");
        equal(1, signals.get(), "original plan does not retain the custom callback");
    }

    private static final class CloseTrackingList<E> extends ArrayList<E> {
        private static final long serialVersionUID = 1L;
        private final AtomicInteger opens = new AtomicInteger();
        private final AtomicInteger closes = new AtomicInteger();
        private RuntimeException closeFailure;

        private CloseTrackingList(Collection<? extends E> source) { super(source); }

        @Override public Stream<E> stream() {
            opens.incrementAndGet();
            return super.stream().onClose(() -> {
                closes.incrementAndGet();
                if (closeFailure != null) throw closeFailure;
            });
        }
    }

    private static void await(CountDownLatch latch, String message) {
        try {
            check(latch.await(5, TimeUnit.SECONDS), message);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError("unexpected worker interruption", interrupted);
        }
    }

    private static void awaitNoWorkers() throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            boolean live = Thread.getAllStackTraces().keySet().stream().anyMatch(thread ->
                    thread.isAlive() && thread.getName().startsWith("m3-key-lane-"));
            if (!live) {
                check(true, "worker cancellation completed");
                return;
            }
            Thread.sleep(5);
        }
        throw new AssertionError("keyed worker survived failed traversal");
    }

    private static Throwable capture(Runnable operation) {
        try {
            operation.run();
        } catch (RuntimeException | Error failure) {
            return failure;
        }
        throw new AssertionError("expected failure");
    }

    private static void expect(Class<? extends Throwable> expected, Runnable operation) {
        Throwable failure = capture(operation);
        check(expected.isInstance(failure), "expected " + expected + ", received " + failure);
    }

    private static void equal(Object expected, Object actual, String message) {
        check(Objects.equals(expected, actual), message + ": expected=" + expected + ", actual=" + actual);
    }

    private static void check(boolean success, String message) {
        CHECKS.incrementAndGet();
        if (!success) throw new AssertionError(message);
    }
}
