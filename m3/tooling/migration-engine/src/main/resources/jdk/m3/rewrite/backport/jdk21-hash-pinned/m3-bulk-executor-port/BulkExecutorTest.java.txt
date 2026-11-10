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
 * @summary Verify the internal M3 bulk execution contract
 * @modules java.base/jdk.internal.vm.parallel
 * @run main BulkExecutorTest
 */
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jdk.internal.vm.parallel.BulkExecution;
import jdk.internal.vm.parallel.BulkExecutor;
import jdk.internal.vm.parallel.BulkTask;

public class BulkExecutorTest {
    private static final String ROOT = "0".repeat(64);
    public static void main(String[] args) {
        expect(IllegalArgumentException.class, () -> task(0, 0, List.of("a"), List.of("c")));
        expect(IllegalArgumentException.class, () -> task(4, 5, List.of("a"), List.of("c")));
        expect(IllegalArgumentException.class, () -> task(4, 0, List.of("a", "a"), List.of("c")));
        ArrayList<String> inputs = new ArrayList<>(List.of("a", "b"));
        BulkTask task = task(4, 0, inputs, List.of("c"));
        inputs.add("late");
        if (!task.inputs().equals(List.of("a", "b"))) throw new AssertionError("snapshot");
        BulkExecutor executor = (t, values) -> {
            int[] a = (int[]) values.get("a");
            int[] b = (int[]) values.get("b");
            int[] c = new int[Math.toIntExact(t.workItems())];
            for (int i = 0; i < c.length; i++) c[i] = a[i] + b[i];
            return new BulkExecution(Map.of("c", c), "cpu", ROOT);
        };
        BulkExecution result = executor.execute(task,
                Map.of("a", new int[]{1,2,3,4}, "b", new int[]{10,20,30,40}));
        if (!java.util.Arrays.equals((int[]) result.outputs().get("c"), new int[]{11,22,33,44}))
            throw new AssertionError("output");
    }
    private static BulkTask task(long workItems, long localWork, List<String> inputs, List<String> outputs) {
        return new BulkTask("kernel-context-int-vector-add", workItems, localWork, inputs, outputs);
    }
    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); } catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError(failure);
        }
        throw new AssertionError("missing " + type.getName());
    }
}
