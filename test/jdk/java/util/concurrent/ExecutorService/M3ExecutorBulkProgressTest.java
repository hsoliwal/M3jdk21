/*
 * Copyright (c) 2026, Hitesh Soliwal and Contributors to the Synexia Project.
 * All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * SPDX-License-Identifier: GPL-2.0-only
 */
 
/*
 * @test
 * @summary M3 monitored executor bulk delegates Future ordering, cancellation and progress scope
 * @run main/othervm M3ExecutorBulkProgressTest
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ProgressMonitor;
import java.util.concurrent.ProgressMonitors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class M3ExecutorBulkProgressTest {
    private static int checks;
    private static final class Monitor implements ProgressMonitor {
        volatile boolean cancelled;
        final AtomicInteger starts = new AtomicInteger();
        final AtomicInteger finishes = new AtomicInteger();
        @Override public boolean isCancelled() { return cancelled; }
        @Override public void begin(String name, long total) { starts.incrementAndGet(); }
        @Override public void done() { finishes.incrementAndGet(); }
    }
    @FunctionalInterface private interface Attempt { void run() throws Exception; }
    private static void yes(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
    private static void fails(Class<? extends Throwable> type, Attempt call) throws Exception {
        checks++;
        try {
            call.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("unexpected failure " + failure, failure);
        }
        throw new AssertionError("expected " + type.getName());
    }
    public static void main(String[] ignored) throws Exception {
        Monitor monitor = new Monitor();
        try (ExecutorService executor = Executors.newFixedThreadPool(3)) {
            List<Callable<Integer>> input = new ArrayList<>();
            for (int i = 0; i < 30; i++) {
                final int row = i;
                input.add(() -> {
                    if (ProgressMonitors.current() != monitor) throw new AssertionError("borrowed scope");
                    return row;
                });
            }
            List<Future<Integer>> ordered = executor.invokeAllMonitored(input, monitor);
            yes(ordered.size() == 30, "Future count");
            for (int i = 0; i < ordered.size(); i++) {
                yes(ordered.get(i).get() == i, "Future encounter order");
            }
            yes(executor.submit(ProgressMonitors::current).get() != monitor,
                    "borrowed scope restored on worker");
            yes(executor.invokeAnyMonitored(List.<Callable<Integer>>of(
                    () -> { throw new IllegalStateException("failed"); },
                    () -> 17), monitor) == 17, "first successful result");
            List<Future<Integer>> exceptional = executor.invokeAllMonitored(
                    List.<Callable<Integer>>of(() -> {
                        throw new IllegalArgumentException("task failure");
                    }), monitor);
            fails(ExecutionException.class, () -> exceptional.get(0).get());

            yes(executor.invokeAllMonitored(List.<Callable<Integer>>of(), monitor)
                    .isEmpty(), "empty invokeAll");
            fails(IllegalArgumentException.class,
                    () -> executor.invokeAnyMonitored(List.<Callable<Integer>>of(), monitor));
            fails(NullPointerException.class,
                    () -> executor.invokeAllMonitored(null, monitor));
            fails(NullPointerException.class,
                    () -> executor.invokeAnyMonitored(input, null));
            fails(NullPointerException.class,
                    () -> executor.invokeAllMonitored(input, 1, null, monitor));
            fails(NullPointerException.class,
                    () -> executor.invokeAnyMonitored(input, 1, null, monitor));
            List<Callable<Integer>> withNull = Arrays.asList(() -> 1, null);
            fails(NullPointerException.class,
                    () -> executor.invokeAllMonitored(withNull, monitor));
            fails(NullPointerException.class,
                    () -> executor.invokeAnyMonitored(withNull, monitor));

            monitor.cancelled = true;
            List<Future<Integer>> cancelled = executor.invokeAllMonitored(
                    List.<Callable<Integer>>of(() -> 99), monitor);
            fails(ExecutionException.class, () -> cancelled.get(0).get());
            monitor.cancelled = false;
            yes(executor.invokeAnyMonitored(List.<Callable<Integer>>of(() -> 21),
                    2, TimeUnit.SECONDS, monitor) == 21, "timed success");
            List<Future<Integer>> timed = executor.invokeAllMonitored(
                    List.<Callable<Integer>>of(() -> {
                        Thread.sleep(10_000);
                        return 1;
                    }), 50, TimeUnit.MILLISECONDS, monitor);
            yes(timed.size() == 1 && timed.get(0).isCancelled(),
                    "timed bulk cancellation");
            fails(TimeoutException.class, () -> executor.invokeAnyMonitored(
                    List.<Callable<Integer>>of(() -> {
                        Thread.sleep(10_000);
                        return 1;
                    }), 50, TimeUnit.MILLISECONDS, monitor));
            yes(executor.submit(ProgressMonitors::current).get() != monitor,
                    "worker scope restored after timeout");
        }
        yes(monitor.starts.get() == 0 && monitor.finishes.get() == 0,
                "borrowed monitor ownership");
        try (ExecutorService rejected = Executors.newSingleThreadExecutor()) {
            rejected.shutdown();
            fails(RejectedExecutionException.class, () -> rejected.invokeAllMonitored(
                    List.<Callable<Integer>>of(() -> 1), monitor));
        }
        System.out.println("PASS M3 monitored bulk executor: checks=" + checks);
    }
}
