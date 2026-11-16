/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 */
/*
 * @test
 * @summary Explicit bulk provider selection preserves registration and in-flight ownership
 * @modules java.base/jdk.internal.vm.parallel
 * @run main BulkProviderSelectionTest
 */

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import jdk.internal.vm.parallel.BulkExecution;
import jdk.internal.vm.parallel.BulkExecutorProvider;
import jdk.internal.vm.parallel.BulkScheduler;
import jdk.internal.vm.parallel.BulkTask;

public class BulkProviderSelectionTest {
    private static final String ROOT = "0".repeat(64);
    private static final BulkTask TASK = new BulkTask("int-add", 3, 0,
            List.of("a", "b"), List.of("c"));

    public static void main(String[] args) throws Exception {
        explicitSelectionIsLocal();
        selectionNeverFallsBack();
        registrationRemainsFrozen();
        inFlightExecutionKeepsItsProvider();
    }

    private static void explicitSelectionIsLocal() {
        Provider first = new Provider("first", 20);
        Provider second = new Provider("second", 10);
        BulkScheduler automatic = new BulkScheduler(List.of(second, first));
        BulkScheduler selected = automatic.withProvider("second");
        check(selected.select(TASK) == second, "explicit selection");
        check(first.probes.get() == 0, "unselected provider was consulted");
        result(selected.execute(TASK, inputs()), "second");
        check(first.calls.get() == 0, "unselected provider executed");
        result(automatic.execute(TASK, inputs()), "first");
        result(selected.withProvider("first").execute(TASK, inputs()), "first");
        result(selected.execute(TASK, inputs()), "second");
        check(selected.providerIds().equals(List.of("first", "second")),
                "selection lost original registrations");
        expect(NullPointerException.class, () -> automatic.withProvider(null));
        expect(IllegalArgumentException.class, () -> automatic.withProvider(""));
        expect(IllegalArgumentException.class, () -> automatic.withProvider("bad/id"));
        expect(IllegalArgumentException.class, () -> automatic.withProvider("unknown"));
    }

    private static void selectionNeverFallsBack() {
        Provider first = new Provider("first", 20);
        Provider second = new Provider("second", 10);
        BulkScheduler selected = new BulkScheduler(List.of(first, second)).withProvider("second");
        second.supported = false;
        expect(IllegalStateException.class, () -> selected.execute(TASK, inputs()));
        check(second.calls.get() == 0, "unsupported provider executed");
        check(first.probes.get() == 0 && first.calls.get() == 0,
                "unsupported selection fell back");
        second.supported = true;
        RuntimeException original = new IllegalStateException("provider failure");
        second.failure = original;
        try {
            selected.execute(TASK, inputs());
            throw new AssertionError("provider failure missing");
        } catch (RuntimeException caught) {
            check(caught == original, "provider failure was replaced");
        }
        check(first.probes.get() == 0 && first.calls.get() == 0,
                "failed execution fell back");
    }

    private static void registrationRemainsFrozen() {
        Provider provider = new Provider("stable", 1);
        BulkScheduler automatic = new BulkScheduler(List.of(provider));
        BulkScheduler selected = automatic.withProvider("stable");
        provider.priority = 2;
        expect(IllegalStateException.class, () -> selected.select(TASK));
        expect(IllegalStateException.class, () -> automatic.withProvider("stable"));
        check(provider.probes.get() == 0, "changed priority reached provider");
        provider.priority = 1;
        provider.id = "changed";
        expect(IllegalStateException.class, () -> selected.execute(TASK, inputs()));
        check(provider.calls.get() == 0, "changed identity executed");
        provider.id = "stable";
        provider.mutateOnProbe = true;
        expect(IllegalStateException.class, () -> selected.select(TASK));
        check(provider.calls.get() == 0, "probe mutation escaped admission");
    }

    private static void inFlightExecutionKeepsItsProvider() throws Exception {
        Provider first = new Provider("first", 20);
        Provider second = new Provider("second", 10);
        first.entered = new CountDownLatch(1);
        first.release = new CountDownLatch(1);
        BulkScheduler scheduler = new BulkScheduler(List.of(first, second));
        BulkScheduler firstSelection = scheduler.withProvider("first");
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            Future<BulkExecution> running = worker.submit(() -> firstSelection.execute(TASK, inputs()));
            check(first.entered.await(10, TimeUnit.SECONDS), "first provider did not start");
            BulkScheduler secondSelection = firstSelection.withProvider("second");
            result(secondSelection.execute(TASK, inputs()), "second");
            first.release.countDown();
            result(running.get(10, TimeUnit.SECONDS), "first");
            check(firstSelection.select(TASK) == first, "existing selection was mutated");
            check(secondSelection.select(TASK) == second, "new selection was lost");
        } finally {
            first.release.countDown();
            worker.shutdownNow();
            check(worker.awaitTermination(10, TimeUnit.SECONDS), "worker did not terminate");
        }
    }

    private static Map<String, Object> inputs() {
        return Map.of("a", new int[] {Integer.MAX_VALUE, -7, 3},
                "b", new int[] {1, 5, -3});
    }

    private static void result(BulkExecution execution, String id) {
        check(execution.provider().equals(id), "execution provider");
        int[] values = (int[]) execution.output("c");
        check(java.util.Arrays.equals(values, new int[] {Integer.MIN_VALUE, -2, 0}),
                "execution result");
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try {
            action.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("unexpected failure", failure);
        }
        throw new AssertionError("missing " + type.getName());
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class Provider implements BulkExecutorProvider {
        private volatile String id;
        private volatile int priority;
        private boolean supported = true;
        private boolean mutateOnProbe;
        private RuntimeException failure;
        private CountDownLatch entered;
        private CountDownLatch release;
        private final AtomicInteger probes = new AtomicInteger();
        private final AtomicInteger calls = new AtomicInteger();

        Provider(String id, int priority) {
            this.id = id;
            this.priority = priority;
        }

        public String id() { return id; }
        public int priority() { return priority; }
        public boolean supports(BulkTask task) {
            probes.incrementAndGet();
            if (mutateOnProbe) priority++;
            return supported;
        }

        public BulkExecution execute(BulkTask task, Map<String, Object> values) {
            calls.incrementAndGet();
            if (failure != null) throw failure;
            if (entered != null) {
                entered.countDown();
                try {
                    check(release.await(10, TimeUnit.SECONDS), "provider release timed out");
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(interrupted);
                }
            }
            int[] a = (int[]) values.get("a");
            int[] b = (int[]) values.get("b");
            int[] c = new int[a.length];
            for (int i = 0; i < c.length; i++) c[i] = a[i] + b[i];
            return new BulkExecution(Map.of("c", c), id, ROOT);
        }
    }
}
