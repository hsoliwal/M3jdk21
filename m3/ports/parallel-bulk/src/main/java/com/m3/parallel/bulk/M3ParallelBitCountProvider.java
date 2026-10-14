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
 * Bounded one-kernel bridge for independent 32-bit population counts.
 *
 * <p>The operation computes {@link Integer#bitCount(int)} for every signed int input,
 * including negative values interpreted as 32-bit two's-complement patterns. It neither
 * mutates the input nor infers source semantics from a bitmap count. The CPU reference
 * remains the exact oracle, and explicit accelerator requests cannot silently fall back.</p>
 */
public final class M3ParallelBitCountProvider implements BulkExecutorProvider {
    public static final String OPERATION_ID = "m3.parallel.int-bitcount.v1";
    public static final String INPUT = "bits";
    public static final String OUTPUT = "counts";
    public static final int MAX_ITEMS = 1 << 18;

    private static final KernelId KERNEL = KernelId.of(OPERATION_ID);
    private static final JavaKernel JAVA_ORACLE = (index, buffers) -> {
        if (buffers.length != 2) {
            throw new IllegalArgumentException("bitcount needs two int lanes");
        }
        if (index < buffers[1].length()) {
            buffers[1].set(index, Integer.bitCount(buffers[0].get(index)));
        }
    };

    static {
        JavaKernels.register(KERNEL, JAVA_ORACLE);
    }

    private final Backend backend;
    private final String providerId;
    private final int priority;

    public M3ParallelBitCountProvider(Backend backend) {
        this.backend = Objects.requireNonNull(backend, "backend");
        if (!backend.supports(KERNEL)) {
            throw new IllegalArgumentException("backend cannot run bitcount kernel: " + backend.id());
        }
        providerId = "m3-bitcount-" + backend.id();
        priority = backend.isReference() ? 10 : Math.addExact(20, backend.score());
    }

    public static M3ParallelBitCountProvider reference() {
        return new M3ParallelBitCountProvider(Backend.reference());
    }

    public static M3ParallelBitCountProvider requiredAccelerator() {
        return new M3ParallelBitCountProvider(Backend.select(Backend.Policy.ACCELERATOR));
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
                && task.inputs().equals(List.of(INPUT))
                && task.outputs().equals(List.of(OUTPUT))
                && (backend.isReference()
                    ? task.localWork() == 0
                    : task.localWork() >= 1 && task.localWork() <= task.workItems());
    }

    @Override
    public BulkExecution execute(BulkTask task, Map<String, Object> values) {
        if (!supports(task)) {
            throw new IllegalArgumentException("unadmitted bitcount task");
        }
        Objects.requireNonNull(values, "values");
        if (!values.keySet().equals(Set.of(INPUT))) {
            throw new IllegalArgumentException("bitcount input schema");
        }
        if (!(values.get(INPUT) instanceof int[] input)) {
            throw new IllegalArgumentException("bitcount input must be int[]");
        }
        int length = Math.toIntExact(task.workItems());
        if (input.length != length) {
            throw new IllegalArgumentException("bitcount input length differs from workItems");
        }

        IntBuffer source = IntBuffer.of(input); // input is snapshotted
        IntBuffer target = IntBuffer.allocate(length);
        int local = backend.isReference() ? 1 : Math.toIntExact(task.localWork());
        try (Plan plan = Plan.builder(backend)
                .kernel(KERNEL)
                .grid(Grid1D.of(length, local))
                .bind(source, Transfer.ONCE)
                .bind(target, Transfer.ON_DEMAND)
                .build()) {
            Verified receipt = Verified.run(plan);
            return new BulkExecution(Map.of(OUTPUT, target.toArray()), id(), receipt.root());
        }
    }
}
