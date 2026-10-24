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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Standalone, deterministic checks against the actual M3ObjectStream/keyed-plan owners.
 * The gates exercise stable/null results, masked ring wrap, bounded admission and
 * failure of a lane whose queue would otherwise strand the coordinator.
 */
public final class M3KeyedOrderRingContract {
    private static int checks;

    private M3KeyedOrderRingContract() { }

    public static void main(String[] args) throws Exception {
        stableResultOrderAndNulls();
        boundedHeadOfLineAdmission();
        failedLaneCannotStrandProducer();
        System.out.println("M3KeyedOrderRingContract PASS checks=" + checks);
    }

    private static void stableResultOrderAndNulls() {
        int count = 4097;
        List<Integer> source = new ArrayList<>(count);
        for (int i = 0; i < count; i++) source.add(i);
        List<Integer> result = M3ObjectStream.snapshot(source)
                .parallelByKey(value -> value % 13)
                .mapOrdered(value -> value % 31 == 0 ? null : value, 13, 65, count);
        equal(count, result.size());
        for (int i = 0; i < count; i++) {
            equal(i % 31 == 0 ? null : Integer.valueOf(i), result.get(i));
        }
        boolean refused = false;
        try {
            result.add(100);
        } catch (UnsupportedOperationException expected) {
            refused = true;
        }
        check(refused, "read-only result");
    }

    private static void boundedHeadOfLineAdmission() throws Exception {
        int window = 8;
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AtomicInteger admissionCount = new AtomicInteger();
        ExecutorService call = Executors.newSingleThreadExecutor();
        try {
            List<Integer> values = new ArrayList<>();
            for (int i = 0; i < 1000; i++) values.add(i);
            Future<List<Integer>> future = call.submit(() ->
                    M3ObjectStream.snapshot(values)
                            .parallelByKey(value -> {
                                admissionCount.incrementAndGet();
                                return value == 0 ? 0 : 1;
                            })
                            .mapOrdered(value -> {
                                if (value == 0) {
                                    firstStarted.countDown();
                                    try {
                                        if (!releaseFirst.await(8, TimeUnit.SECONDS)) {
                                            throw new IllegalStateException("head-of-line gate expired");
                                        }
                                    } catch (InterruptedException interrupted) {
                                        Thread.currentThread().interrupt();
                                        throw new IllegalStateException(interrupted);
                                    }
                                }
                                return value;
                            }, 2, window, values.size()));
            check(firstStarted.await(3, TimeUnit.SECONDS), "first task started");
            // A bounded completion window must not let a blocked ordinal 0
            // accumulate unbounded mapped results through later completions.
            Thread.sleep(100);
            check(admissionCount.get() <= window,
                    "admission exceeded completion window: " + admissionCount.get());
            releaseFirst.countDown();
            List<Integer> result = future.get(8, TimeUnit.SECONDS);
            equal(values, result);
        } finally {
            releaseFirst.countDown();
            call.shutdownNow();
            check(call.awaitTermination(4, TimeUnit.SECONDS), "coordinator stopped");
        }
    }

    private static void failedLaneCannotStrandProducer() throws Exception {
        ExecutorService call = Executors.newSingleThreadExecutor();
        try {
            Integer[] values = new Integer[1024];
            Arrays.setAll(values, index -> index);
            Future<List<Integer>> future = call.submit(() ->
                    M3ObjectStream.snapshot(Arrays.asList(values))
                            .parallelByKey(value -> 0)
                            .mapOrdered(value -> {
                                if (value == 0) throw new IllegalStateException("lane-failure-sentinel");
                                return value;
                            }, 2, 64, values.length));
            try {
                future.get(5, TimeUnit.SECONDS);
                throw new AssertionError("mapper failure should escape");
            } catch (ExecutionException expected) {
                Throwable cause = expected.getCause();
                check(cause instanceof IllegalStateException,
                        "original mapper exception type: " + cause);
                check("lane-failure-sentinel".equals(cause.getMessage()),
                        "original mapper exception message: " + cause);
            } catch (TimeoutException blocked) {
                future.cancel(true);
                throw new AssertionError("failed keyed lane stranded the producer", blocked);
            }
        } finally {
            call.shutdownNow();
            check(call.awaitTermination(4, TimeUnit.SECONDS), "failure coordinator stopped");
        }
    }

    private static void equal(Object expected, Object actual) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }

    private static void check(boolean passes, String message) {
        checks++;
        if (!passes) throw new AssertionError(message);
    }
}
