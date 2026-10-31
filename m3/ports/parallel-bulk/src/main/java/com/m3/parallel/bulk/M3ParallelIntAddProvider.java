/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors.
 */
package com.m3.parallel.bulk;

import com.m3.parallel.Backend;
import com.m3.parallel.Grid1D;
import com.m3.parallel.IntBuffer;
import com.m3.parallel.JavaKernel;
import com.m3.parallel.JavaKernels;
import com.m3.parallel.KernelId;
import com.m3.parallel.Plan;
import com.m3.parallel.Transfer;
import com.m3.parallel.Verified;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import jdk.internal.vm.parallel.BulkExecution;
import jdk.internal.vm.parallel.BulkExecutorProvider;
import jdk.internal.vm.parallel.BulkTask;

/**
 * Optional, bounded bridge from the internal JDK bulk scheduler to M3's existing data-parallel
 * execution fabric. The only admitted operation is a reviewed integer-lane addition. Caller
 * arrays are snapshotted before device admission; no arbitrary lambda or Runnable is offloaded.
 *
 * <p>Every launch uses {@link Verified#run(Plan)}. In particular, the non-reference result is
 * admitted only if its complete buffers agree with the independently executed Java reference.
 * The adapter never falls back to the reference backend after an explicitly chosen accelerator
 * fails.</p>
 */
public final class M3ParallelIntAddProvider implements BulkExecutorProvider {
    public static final String OPERATION_ID = "m3.parallel.int-add.v1";
    public static final String LEFT = "left";
    public static final String RIGHT = "right";
    public static final String SUM = "sum";
    public static final int MAX_ITEMS = 1 << 18;

    private static final KernelId KERNEL = KernelId.of(OPERATION_ID);
    private static final JavaKernel JAVA_ORACLE = (index, buffers) -> {
        if (buffers.length != 3) {
            throw new IllegalArgumentException("integer-add kernel needs three lanes");
        }
        if (index < buffers[2].length()) {
            buffers[2].set(index, buffers[0].get(index) + buffers[1].get(index));
        }
    };

    static {
        // A competing implementation under the same ID must fail closed.
        JavaKernels.register(KERNEL, JAVA_ORACLE);
    }

    private final Backend backend;
    private final String providerId;
    private final int priority;

    public M3ParallelIntAddProvider(Backend backend) {
        this.backend = Objects.requireNonNull(backend, "backend");
        if (!backend.supports(KERNEL)) {
            throw new IllegalArgumentException("backend does not support reviewed kernel: " + backend.id());
        }
        providerId = "m3-parallel-" + backend.id();
        priority = backend.isReference() ? 10 : Math.addExact(20, backend.score());
    }

    public static M3ParallelIntAddProvider reference() {
        return new M3ParallelIntAddProvider(Backend.reference());
    }

    /** Selects a required accelerator; never chooses the Java reference as a fallback. */
    public static M3ParallelIntAddProvider requiredAccelerator() {
        return new M3ParallelIntAddProvider(Backend.select(Backend.Policy.ACCELERATOR));
    }

    @Override
    public String id() {
        return providerId;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public boolean supports(BulkTask task) {
        return task != null
                && OPERATION_ID.equals(task.operationId())
                && task.workItems() >= 1
                && task.workItems() <= MAX_ITEMS
                && task.inputs().equals(List.of(LEFT, RIGHT))
                && task.outputs().equals(List.of(SUM))
                && (backend.isReference()
                    ? task.localWork() == 0
                    : task.localWork() >= 1 && task.localWork() <= task.workItems());
    }

    @Override
    public BulkExecution execute(BulkTask task, Map<String, Object> values) {
        if (!supports(task)) {
            throw new IllegalArgumentException("unadmitted M3 parallel integer-add task");
        }
        Objects.requireNonNull(values, "values");
        if (!values.keySet().equals(Set.of(LEFT, RIGHT))) {
            throw new IllegalArgumentException("integer-add input schema");
        }
        if (!(values.get(LEFT) instanceof int[] left)
                || !(values.get(RIGHT) instanceof int[] right)) {
            throw new IllegalArgumentException("integer-add requires primitive int[] input lanes");
        }
        int n = Math.toIntExact(task.workItems());
        if (left.length != n || right.length != n) {
            throw new IllegalArgumentException("input length/workItems mismatch");
        }

        // IntBuffer.of copies all input values. Every execute owns a new Plan and output image.
        IntBuffer a = IntBuffer.of(left);
        IntBuffer b = IntBuffer.of(right);
        IntBuffer c = IntBuffer.allocate(n);
        int local = backend.isReference() ? 1 : Math.toIntExact(task.localWork());
        try (Plan plan = Plan.builder(backend)
                .kernel(KERNEL)
                .grid(Grid1D.of(n, local))
                .bind(a, Transfer.ONCE)
                .bind(b, Transfer.ONCE)
                .bind(c, Transfer.ON_DEMAND)
                .build()) {
            Verified receipt = Verified.run(plan);
            return new BulkExecution(Map.of(SUM, c.toArray()), id(), receipt.root());
        }
    }
}
