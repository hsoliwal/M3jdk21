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
 * @summary Check M3 bulk output metadata without exposing mutable result arrays
 * @modules java.base/jdk.internal.vm.parallel
 * @run main BulkOutputMetadataTest
 */

import java.util.List;
import java.util.Map;
import java.util.Set;
import jdk.internal.vm.parallel.BulkExecution;
import jdk.internal.vm.parallel.BulkExecutorProvider;
import jdk.internal.vm.parallel.BulkScheduler;
import jdk.internal.vm.parallel.BulkTask;

public class BulkOutputMetadataTest {
    private static final String ROOT = "0".repeat(64);

    public static void main(String[] args) {
        int[] source = new int[1 << 18];
        source[0] = 123;
        source[source.length - 1] = 456;
        BulkExecution original = new BulkExecution(Map.of("result", source), "cpu", ROOT);
        source[0] = -1;
        Set<String> names = original.outputNames();
        if (!names.equals(Set.of("result"))) throw new AssertionError("names");
        expect(UnsupportedOperationException.class, () -> names.remove("result"));
        int[] read = (int[]) original.outputs().get("result");
        if (read[0] != 123 || read[read.length - 1] != 456) {
            throw new AssertionError("constructor snapshot");
        }
        read[0] = -2;
        if (((int[]) original.outputs().get("result"))[0] != 123) {
            throw new AssertionError("read boundary");
        }

        BulkTask task = new BulkTask("reviewed-add", 4, 0,
                List.of("input"), List.of("result"));
        BulkExecutorProvider provider = provider(original);
        BulkScheduler scheduler = new BulkScheduler(List.of(provider));
        Map<String, Object> inputs = Map.of("input", new int[4]);
        for (int i = 0; i < 64; i++) {
            if (scheduler.execute(task, inputs) != original) {
                throw new AssertionError("identity");
            }
        }

        BulkExecution extra = new BulkExecution(
                Map.of("result", new int[4], "unexpected", new int[4]),
                "cpu", ROOT);
        expect(IllegalStateException.class,
                () -> new BulkScheduler(List.of(provider(extra))).execute(task, inputs));
        BulkExecution missing = new BulkExecution(Map.of("unexpected", new int[4]), "cpu", ROOT);
        expect(IllegalStateException.class,
                () -> new BulkScheduler(List.of(provider(missing))).execute(task, inputs));
        System.out.println("PASS bulk output metadata and defensive boundaries");
    }

    private static BulkExecutorProvider provider(BulkExecution execution) {
        return new BulkExecutorProvider() {
            public String id() { return "cpu"; }
            public int priority() { return 1; }
            public boolean supports(BulkTask task) { return true; }
            public BulkExecution execute(BulkTask task, Map<String, Object> values) {
                return execution;
            }
        };
    }

    private static void expect(Class<? extends Throwable> expected, Runnable action) {
        try { action.run(); }
        catch (Throwable failure) {
            if (expected.isInstance(failure)) return;
            throw new AssertionError(failure);
        }
        throw new AssertionError("expected " + expected.getName());
    }
}
