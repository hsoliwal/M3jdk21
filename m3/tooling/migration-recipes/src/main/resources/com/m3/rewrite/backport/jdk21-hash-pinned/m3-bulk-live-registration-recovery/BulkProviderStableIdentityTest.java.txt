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
 * @summary Verify frozen identity and priority of M3 bulk scheduler registrations
 * @modules java.base/jdk.internal.vm.parallel
 * @run main BulkProviderStableIdentityTest
 */

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import jdk.internal.vm.parallel.BulkExecution;
import jdk.internal.vm.parallel.BulkExecutorProvider;
import jdk.internal.vm.parallel.BulkScheduler;
import jdk.internal.vm.parallel.BulkTask;

public class BulkProviderStableIdentityTest {
    private static final String ROOT = "0".repeat(64);

    public static void main(String[] args) {
        AtomicReference<String> id = new AtomicReference<>("stable");
        AtomicInteger priority = new AtomicInteger(10);
        AtomicBoolean changeOnSupport = new AtomicBoolean(false);
        AtomicBoolean changeOnExecution = new AtomicBoolean(false);
        BulkExecutorProvider provider = new BulkExecutorProvider() {
            public String id() { return id.get(); }
            public int priority() { return priority.get(); }
            public boolean supports(BulkTask task) {
                if (changeOnSupport.get()) priority.set(11);
                return true;
            }
            public BulkExecution execute(BulkTask task, Map<String, Object> values) {
                if (changeOnExecution.get()) id.set("changed");
                return new BulkExecution(Map.of("c", new int[4]), "stable", ROOT);
            }
        };
        BulkTask task = new BulkTask("kernel-context-int-vector-add", 4, 0,
                List.of("a", "b"), List.of("c"));
        BulkScheduler scheduler = new BulkScheduler(List.of(provider));
        require(scheduler.providerIds().equals(List.of("stable")), "initial IDs");
        id.set("changed");
        require(scheduler.providerIds().equals(List.of("stable")), "frozen IDs");
        expect(IllegalStateException.class, () -> scheduler.select(task));
        id.set("stable");
        priority.set(20);
        expect(IllegalStateException.class, () -> scheduler.select(task));
        priority.set(10);
        changeOnSupport.set(true);
        expect(IllegalStateException.class, () -> scheduler.select(task));
        changeOnSupport.set(false);
        priority.set(10);
        changeOnExecution.set(true);
        expect(IllegalStateException.class,
                () -> scheduler.execute(task, Map.of("a", new int[4], "b", new int[4])));
        changeOnExecution.set(false);
        id.set("stable");
        require(scheduler.execute(task, Map.of("a", new int[4], "b", new int[4]))
                .provider().equals("stable"), "stable result");
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
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
