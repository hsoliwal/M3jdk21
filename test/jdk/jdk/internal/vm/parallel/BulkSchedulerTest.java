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
 * @summary Verify deterministic M3 bulk provider scheduling
 * @modules java.base/jdk.internal.vm.parallel
 * @run main BulkSchedulerTest
 */

import java.util.List;
import java.util.Map;
import jdk.internal.vm.parallel.BulkExecution;
import jdk.internal.vm.parallel.BulkExecutorProvider;
import jdk.internal.vm.parallel.BulkScheduler;
import jdk.internal.vm.parallel.BulkTask;

public class BulkSchedulerTest {
    private static final String ROOT = "0".repeat(64);

    public static void main(String[] args) {
        BulkExecutorProvider low = provider("z-low", 10, true, "c");
        BulkExecutorProvider alpha = provider("a-high", 20, true, "c");
        BulkExecutorProvider beta = provider("b-high", 20, true, "c");
        BulkScheduler scheduler = new BulkScheduler(List.of(low, beta, alpha));
        if (!scheduler.providerIds().equals(List.of("a-high", "b-high", "z-low")))
            throw new AssertionError("order");
        if (scheduler.select(task()) != alpha) throw new AssertionError("selection");

        expect(IllegalArgumentException.class,
                () -> new BulkScheduler(List.of(provider("same", 1, true, "c"),
                        provider("same", 2, true, "c"))));
        expect(IllegalStateException.class,
                () -> new BulkScheduler(List.of(provider("none", 1, false, "c"))).select(task()));
        expect(IllegalArgumentException.class,
                () -> scheduler.execute(task(), Map.of("a", new int[4])));

        BulkExecution ok = scheduler.execute(task(),
                Map.of("a", new int[4], "b", new int[4]));
        if (!ok.provider().equals("a-high")) throw new AssertionError("provider");

        expect(IllegalStateException.class,
                () -> new BulkScheduler(List.of(provider("good", 1, true, "wrong")))
                        .execute(task(), Map.of("a", new int[4], "b", new int[4])));
        expect(IllegalStateException.class,
                () -> new BulkScheduler(List.of(mismatchedIdentity()))
                        .execute(task(), Map.of("a", new int[4], "b", new int[4])));

        snapshotInputsBeforeProviderSelection();
    }


    private static void snapshotInputsBeforeProviderSelection() {
        int[] original = { 1, 2, 3, 4 };
        java.util.Map<String, Object> caller = new java.util.HashMap<>();
        caller.put("a", original);
        caller.put("b", new int[] { 10, 20, 30, 40 });
        // The old API allows unused null-valued entries. Preserve that boundary.
        caller.put("optional", null);

        BulkExecutorProvider adversarial = new BulkExecutorProvider() {
            public String id() { return "stable"; }
            public int priority() { return 50; }
            public boolean supports(BulkTask task) {
                caller.put("a", new int[] { 9, 9, 9, 9 });
                return true;
            }
            public BulkExecution execute(BulkTask task, Map<String, Object> values) {
                if (values.get("a") != original) throw new AssertionError("binding drift");
                if (!values.containsKey("optional") || values.get("optional") != null)
                    throw new AssertionError("extra nullable binding lost");
                expect(UnsupportedOperationException.class,
                        () -> values.put("a", new int[] { 8, 8, 8, 8 }));
                return new BulkExecution(Map.of("c", new int[4]), id(), ROOT);
            }
        };
        BulkExecution result = new BulkScheduler(List.of(adversarial))
                .execute(task(), caller);
        if (!result.provider().equals("stable")) throw new AssertionError("selected provider");
    }

    private static BulkTask task() {
        return new BulkTask("kernel-context-int-vector-add", 4, 0,
                List.of("a", "b"), List.of("c"));
    }

    private static BulkExecutorProvider provider(
            String id, int priority, boolean supports, String output) {
        return new BulkExecutorProvider() {
            public String id() { return id; }
            public int priority() { return priority; }
            public boolean supports(BulkTask task) { return supports; }
            public BulkExecution execute(BulkTask task, Map<String, Object> values) {
                return new BulkExecution(Map.of(output, new int[4]), id, ROOT);
            }
        };
    }

    private static BulkExecutorProvider mismatchedIdentity() {
        return new BulkExecutorProvider() {
            public String id() { return "declared"; }
            public int priority() { return 1; }
            public boolean supports(BulkTask task) { return true; }
            public BulkExecution execute(BulkTask task, Map<String, Object> values) {
                return new BulkExecution(Map.of("c", new int[4]), "other", ROOT);
            }
        };
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try {
            action.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError(failure);
        }
        throw new AssertionError("missing " + type.getName());
    }
}
