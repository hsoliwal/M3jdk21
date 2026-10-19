/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Forced bounded-queue schedules; every test owns and joins its complete thread group. */
final class M3KeyedParallelismAdmissionTest {
    private static final long WAIT_SECONDS = 5;
    private static final AtomicInteger GROUP_IDS = new AtomicInteger();

    @Test
    void runtimeFailureInSaturatedLaneIsPropagated() throws Exception {
        failureInSaturatedLane(new IllegalStateException("exact runtime marker"));
    }

    @Test
    void errorFailureInSaturatedLaneIsPropagated() throws Exception {
        failureInSaturatedLane(new AssertionError("exact error marker"));
    }

    private static void failureInSaturatedLane(Throwable marker) throws Exception {
        CountDownLatch fourthKey = new CountDownLatch(1);
        AtomicInteger keys = new AtomicInteger();
        AtomicInteger mapped = new AtomicInteger();
        try (Invocation<List<Integer>> invocation = new Invocation<>(() ->
                M3ObjectStream.snapshot(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8))
                        .parallelByKey(value -> {
                            keys.incrementAndGet();
                            if (value == 3) fourthKey.countDown();
                            return 0;
                        }).mapOrdered(value -> {
                            mapped.incrementAndGet();
                            if (value == 0) {
                                await(fourthKey);
                                rethrow(marker);
                            }
                            throw new AssertionError("failed lane mapped another item");
                        }, 4, 8, 9))) {
            // The first mapper owns item 0, and items 1/2 fill its two-slot lane.
            // Extracting item 3 releases the failure while global admission is only 3/8.
            ExecutionException thrown = assertThrows(ExecutionException.class, invocation::get);
            assertSame(marker, thrown.getCause());
            assertEquals(4, keys.get());
            assertEquals(1, mapped.get());
            invocation.awaitQuiescent();
        }
    }

    @Test
    void saturatedAdmissionPreservesOrderAndKeyOccurrenceCounts() throws Exception {
        List<Integer> input = Arrays.asList(null, 0, 1, 2, 3, 4, 5, 6, 7, null);
        AtomicInteger keys = new AtomicInteger();
        AtomicInteger mapped = new AtomicInteger();
        try (Invocation<List<Integer>> invocation = new Invocation<>(() ->
                M3ObjectStream.snapshot(input).parallelByKey(value -> {
                    keys.incrementAndGet();
                    return null;
                }).mapOrdered(value -> {
                    mapped.incrementAndGet();
                    return value;
                }, 4, 8, input.size()))) {
            List<Integer> result = invocation.get();
            assertEquals(input, result);
            for (int i = 0; i < input.size(); i++) assertSame(input.get(i), result.get(i));
            assertEquals(input.size(), keys.get());
            assertEquals(input.size(), mapped.get());
            assertThrows(UnsupportedOperationException.class, () -> result.add(99));
            invocation.awaitQuiescent();
        }
    }

    @Test
    void interruptedSaturatedAdmissionCancelsWorkers() throws Exception {
        CountDownLatch fourthKey = new CountDownLatch(1);
        CountDownLatch keepMapperWaiting = new CountDownLatch(1);
        CountDownLatch mapperInterrupted = new CountDownLatch(1);
        AtomicBoolean restoredInterrupt = new AtomicBoolean();
        try (Invocation<List<Integer>> invocation = new Invocation<>(() -> {
            try {
                return M3ObjectStream.snapshot(List.of(0, 1, 2, 3, 4))
                        .parallelByKey(value -> {
                            if (value == 3) fourthKey.countDown();
                            return 0;
                        }).mapOrdered(value -> {
                            try {
                                keepMapperWaiting.await();
                                throw new AssertionError("mapper released without cancellation");
                            } catch (InterruptedException interrupted) {
                                mapperInterrupted.countDown();
                                Thread.currentThread().interrupt();
                                throw new IllegalStateException(interrupted);
                            }
                        }, 4, 8, 5);
            } catch (RuntimeException failure) {
                restoredInterrupt.set(Thread.currentThread().isInterrupted());
                throw failure;
            }
        })) {
            assertTrue(fourthKey.await(WAIT_SECONDS, TimeUnit.SECONDS));
            invocation.thread.interrupt();
            ExecutionException thrown = assertThrows(ExecutionException.class, invocation::get);
            IllegalStateException failure = assertInstanceOf(IllegalStateException.class, thrown.getCause());
            assertInstanceOf(InterruptedException.class, failure.getCause());
            assertTrue(restoredInterrupt.get());
            assertTrue(mapperInterrupted.await(WAIT_SECONDS, TimeUnit.SECONDS));
            invocation.awaitQuiescent();
        }
    }

    @Test
    void freedLaneCapacityDoesNotRequireMapperCompletion() throws Exception {
        // The private atom is the scheduling seam. The queue remains a real bounded
        // ArrayBlockingQueue; its test subclass only fixes the order of public calls.
        Class<?> workType = Class.forName("com.m3.collections.M3KeyedParallelismPlan$Work");
        Class<?> resultsType = Class.forName("com.m3.collections.M3KeyedParallelismPlan$OrderedResults");
        Constructor<?> workConstructor = workType.getDeclaredConstructor(long.class, Object.class, boolean.class);
        Constructor<?> resultsConstructor = resultsType.getDeclaredConstructor();
        workConstructor.setAccessible(true);
        resultsConstructor.setAccessible(true);
        Method admit = M3KeyedParallelismPlan.class.getDeclaredMethod("admit", BlockingQueue.class,
                workType, BlockingQueue.class, resultsType);
        Method runLane = M3KeyedParallelismPlan.class.getDeclaredMethod("runLane", BlockingQueue.class,
                BlockingQueue.class, Function.class);
        admit.setAccessible(true);
        runLane.setAccessible(true);
        Object work0 = workConstructor.newInstance(0L, 0, false);
        Object work1 = workConstructor.newInstance(1L, 1, false);
        Object work2 = workConstructor.newInstance(2L, 2, false);
        Object end = workConstructor.newInstance(-1L, null, true);
        Object results = resultsConstructor.newInstance();
        CapacityRaceQueue lane = new CapacityRaceQueue();
        lane.put(work0);
        lane.put(work1);
        BlockingQueue<Object> completed = new ArrayBlockingQueue<>(8);
        CountDownLatch nextKey = new CountDownLatch(1);
        List<Integer> mapperOrder = new ArrayList<>();
        Function<Integer, Integer> keyExtractor = value -> {
            if (value == 3) nextKey.countDown();
            return 0;
        };
        Function<Integer, Integer> mapper = value -> {
            if (value == 0) await(nextKey);
            mapperOrder.add(value);
            return value;
        };

        try (Invocation<Integer> invocation = new Invocation<>(() -> {
            FutureTask<Void> worker = new FutureTask<>(() -> {
                invoke(runLane, lane, completed, mapper);
                return null;
            });
            // This is the actual lane atom, run in the fixture-owned thread group.
            Thread laneThread = new Thread(worker, "m3-keyed-race-worker");
            laneThread.start();
            int drained = (Integer) invoke(admit, lane, work2, completed, results);
            keyExtractor.apply(3);
            lane.put(end);
            worker.get(WAIT_SECONDS, TimeUnit.SECONDS);
            return drained;
        })) {
            assertEquals(0, invocation.get());
            assertEquals(List.of(0, 1, 2), mapperOrder);
            assertEquals(3, completed.size());
            assertTrue(lane.isEmpty());
            invocation.awaitQuiescent();
        }
    }

    private static Object invoke(Method method, Object... arguments) throws Exception {
        try {
            return method.invoke(null, arguments);
        } catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof Exception exception) throw exception;
            if (cause instanceof Error error) throw error;
            throw new AssertionError(cause);
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(WAIT_SECONDS * 4, TimeUnit.SECONDS)) {
                throw new AssertionError("bounded schedule latch timed out");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
        }
    }

    private static void rethrow(Throwable marker) {
        if (marker instanceof RuntimeException runtime) throw runtime;
        if (marker instanceof Error error) throw error;
        throw new AssertionError(marker);
    }

    private static final class CapacityRaceQueue extends ArrayBlockingQueue<Object> {
        private static final long serialVersionUID = 1L;
        private final transient CountDownLatch fullOfferObserved = new CountDownLatch(1);
        private final transient CountDownLatch firstDequeued = new CountDownLatch(1);
        private final AtomicBoolean firstOffer = new AtomicBoolean(true);
        private final AtomicBoolean firstTake = new AtomicBoolean(true);

        private CapacityRaceQueue() { super(2); }

        @Override
        public boolean offer(Object value) {
            return afterOffer(super.offer(value));
        }

        @Override
        public boolean offer(Object value, long timeout, TimeUnit unit) throws InterruptedException {
            return afterOffer(super.offer(value, timeout, unit));
        }

        private boolean afterOffer(boolean accepted) {
            if (!accepted && firstOffer.compareAndSet(true, false)) {
                fullOfferObserved.countDown();
                // Return the failed offer only after dequeue has freed real capacity.
                await(firstDequeued);
            }
            return accepted;
        }

        @Override
        public Object take() throws InterruptedException {
            boolean first = firstTake.compareAndSet(true, false);
            if (first) await(fullOfferObserved);
            Object work = super.take();
            if (first) firstDequeued.countDown();
            return work;
        }
    }

    private static final class Invocation<V> implements AutoCloseable {
        private final ThreadGroup group = new ThreadGroup("m3-keyed-test-" + GROUP_IDS.incrementAndGet());
        private final FutureTask<V> task;
        private final Thread thread;

        private Invocation(Callable<V> operation) {
            task = new FutureTask<>(operation);
            thread = new Thread(group, task, "m3-keyed-test-coordinator");
            thread.start();
        }

        private V get() throws Exception {
            return task.get(WAIT_SECONDS, TimeUnit.SECONDS);
        }

        private void awaitQuiescent() throws InterruptedException {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS);
            for (;;) {
                List<Thread> live = Thread.getAllStackTraces().keySet().stream()
                        .filter(candidate -> candidate.getThreadGroup() == group && candidate.isAlive()).toList();
                if (live.isEmpty()) return;
                long remainingNanos = deadline - System.nanoTime();
                assertTrue(remainingNanos > 0, () -> "owner did not stop its workers: " + live);
                for (Thread remaining : live) {
                    remaining.join(Math.max(1L, Math.min(20L,
                            TimeUnit.NANOSECONDS.toMillis(remainingNanos))));
                }
            }
        }

        @Override
        public void close() {
            task.cancel(true);
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS);
            boolean interrupted = false;
            try {
                for (;;) {
                    List<Thread> live = Thread.getAllStackTraces().keySet().stream()
                            .filter(candidate -> candidate.getThreadGroup() == group && candidate.isAlive()).toList();
                    if (live.isEmpty()) return;
                    for (Thread remaining : live) remaining.interrupt();
                    long remainingNanos = deadline - System.nanoTime();
                    assertTrue(remainingNanos > 0, () -> "fixture left live threads: " + live);
                    for (Thread remaining : live) {
                        try {
                            remaining.join(Math.max(1L, Math.min(20L,
                                    TimeUnit.NANOSECONDS.toMillis(remainingNanos))));
                        } catch (InterruptedException cancellation) {
                            interrupted = true;
                        }
                    }
                }
            } finally {
                if (interrupted) Thread.currentThread().interrupt();
            }
        }
    }
}
