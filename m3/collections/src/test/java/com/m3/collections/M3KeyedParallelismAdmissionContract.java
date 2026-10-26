/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Finite independent oracle for failed-lane admission, ordering, bounds and interruption. */
public final class M3KeyedParallelismAdmissionContract {
    private static int checks;

    private M3KeyedParallelismAdmissionContract() { }

    public static void main(String[] args) throws Exception {
        failedFullLanePreservesExceptionIdentity(new IllegalStateException("mapper failure"));
        failedFullLanePreservesExceptionIdentity(new AssertionError("mapper error"));
        boundedSuccessPreservesOrderAndCallbackDemand();
        interruptionStillCancelsWaitingAdmission();
        System.out.println("M3_KEYED_ADMISSION_PASS checks=" + checks);
    }

    private static void failedFullLanePreservesExceptionIdentity(Throwable marker) throws Exception {
        CountDownLatch releaseFailure = new CountDownLatch(1);
        AtomicInteger keyCalls = new AtomicInteger();
        AtomicInteger mapperCalls = new AtomicInteger();
        FutureTask<Throwable> outcome = new FutureTask<>(() -> {
            try {
                M3ObjectStream.snapshot(List.of(0, 1, 2, 3)).parallelByKey(item -> {
                    keyCalls.incrementAndGet();
                    // Two partitions and four in-flight slots give this lane two queue slots.
                    // Reaching item 3 proves item 0 was taken and items 1/2 filled the queue.
                    if (item == 3) releaseFailure.countDown();
                    return 0;
                }).mapOrdered(item -> {
                    mapperCalls.incrementAndGet();
                    if (item == 0) {
                        await(releaseFailure);
                        throwUnchecked(marker);
                    }
                    return item;
                }, 2, 4, 4);
                return null;
            } catch (Throwable failure) {
                return failure;
            }
        });
        Thread coordinator = new Thread(outcome, "m3-keyed-failure-oracle");
        coordinator.start();
        try {
            Throwable actual;
            try {
                actual = outcome.get(5, TimeUnit.SECONDS);
            } catch (TimeoutException timeout) {
                throw new AssertionError("failed full lane trapped the coordinator before failure delivery", timeout);
            }
            require(actual == marker, "failure identity must cross the completion boundary unchanged");
            equal(4, keyCalls.get(), "source/key admission demand");
            equal(1, mapperCalls.get(), "no queued mapper runs after its lane failed");
        } finally {
            coordinator.interrupt();
            releaseFailure.countDown();
            coordinator.join(5_000);
            require(!coordinator.isAlive(), "coordinator cleanup is bounded");
        }
    }

    private static void boundedSuccessPreservesOrderAndCallbackDemand() {
        List<Integer> source = new ArrayList<>();
        List<Integer> expected = new ArrayList<>();
        for (int i = 0; i < 128; i++) {
            source.add(i);
            expected.add(i * 2);
        }
        for (int[] geometry : new int[][] {{1, 1}, {2, 1}, {2, 4}, {8, 3}, {4, 8}, {4, 32}}) {
            AtomicInteger keys = new AtomicInteger();
            AtomicInteger mapped = new AtomicInteger();
            AtomicBoolean reordered = new AtomicBoolean();
            int[] lastByKey = {-1, -1, -1, -1};
            List<Integer> actual = M3ObjectStream.snapshot(source).parallelByKey(item -> {
                keys.incrementAndGet();
                return item % 4;
            }).mapOrdered(item -> {
                int key = item % 4;
                if (lastByKey[key] >= item) reordered.set(true);
                lastByKey[key] = item;
                mapped.incrementAndGet();
                Thread.yield();
                return item * 2;
            }, geometry[0], geometry[1], source.size());
            equal(expected, actual, "source-ordinal result order");
            equal(source.size(), keys.get(), "one key extraction per input");
            equal(source.size(), mapped.get(), "one mapper call per input");
            require(!reordered.get(), "equal-key callbacks retain source order");
            expect(UnsupportedOperationException.class, () -> actual.add(256));
        }
        List<Integer> nullable = Arrays.asList(null, 1, null, 2, 3, null);
        List<Integer> actual = M3ObjectStream.snapshot(nullable).parallelByKey(item -> item)
                .mapOrdered(item -> item == null ? null : item * 2, 2, 3, nullable.size());
        equal(Arrays.asList(null, 2, null, 4, 6, null), actual, "null keys, inputs and mapped values");
    }

    private static void interruptionStillCancelsWaitingAdmission() throws Exception {
        CountDownLatch mapperEntered = new CountDownLatch(1);
        CountDownLatch releaseMapper = new CountDownLatch(1);
        AtomicBoolean restoredInterrupt = new AtomicBoolean();
        FutureTask<Throwable> outcome = new FutureTask<>(() -> {
            try {
                M3ObjectStream.snapshot(List.of(0, 1, 2, 3)).parallelByKey(item -> 0)
                        .mapOrdered(item -> {
                            mapperEntered.countDown();
                            await(releaseMapper);
                            return item;
                        }, 2, 4, 4);
                return null;
            } catch (Throwable failure) {
                restoredInterrupt.set(Thread.currentThread().isInterrupted());
                return failure;
            }
        });
        Thread coordinator = new Thread(outcome, "m3-keyed-interrupt-oracle");
        coordinator.start();
        try {
            require(mapperEntered.await(5, TimeUnit.SECONDS), "mapper started before cancellation");
            coordinator.interrupt();
            Throwable failure = outcome.get(5, TimeUnit.SECONDS);
            require(failure instanceof IllegalStateException, "interrupt wrapper type");
            require(failure.getCause() instanceof InterruptedException, "original interrupt cause");
            require(restoredInterrupt.get(), "coordinator interrupt status is restored");
        } finally {
            coordinator.interrupt();
            releaseMapper.countDown();
            coordinator.join(5_000);
            require(!coordinator.isAlive(), "interrupted coordinator cleanup is bounded");
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("test mapper interrupted", interrupted);
        }
    }

    private static void throwUnchecked(Throwable failure) {
        if (failure instanceof RuntimeException runtime) throw runtime;
        if (failure instanceof Error error) throw error;
        throw new AssertionError("unexpected checked marker", failure);
    }

    private static void equal(Object expected, Object actual, String message) {
        require(Objects.equals(expected, actual), message + ": expected=" + expected + ", actual=" + actual);
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try {
            action.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("wrong exception", failure);
        }
        throw new AssertionError("expected " + type.getSimpleName());
    }
}
