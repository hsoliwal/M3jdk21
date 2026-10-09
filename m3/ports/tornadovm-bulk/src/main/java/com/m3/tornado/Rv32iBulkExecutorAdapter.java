// SPDX-License-Identifier: Apache-2.0
package com.m3.tornado;

import com.synexia.m3.cpugpu.Rv32iBatch;
import com.synexia.m3.cpugpu.Rv32iBulkExecutor;
import com.synexia.m3.cpugpu.Rv32iBulkReceipt;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import jdk.internal.vm.parallel.BulkExecution;
import jdk.internal.vm.parallel.BulkExecutor;
import jdk.internal.vm.parallel.BulkTask;

/**
 * Optional M3JDK adapter from the internal bulk scheduler contract to the verified TornadoVM
 * RV32IM provider. The input batch is never mutated; execution happens on a private copy.
 */
public final class Rv32iBulkExecutorAdapter implements BulkExecutor {
    public static final String OPERATION_ID = "m3.rv32im.slice";
    public static final String INPUT_BATCH = "batch";
    public static final String INPUT_BUDGET = "instructionBudget";
    public static final String OUTPUT_BATCH = "batch";

    public enum Mode {
        CPU("cpu-rv32im"),
        TORNADO_VERIFIED("tornado-rv32im");

        private final String provider;

        Mode(String provider) {
            this.provider = provider;
        }

        String provider() {
            return provider;
        }
    }

    private final Mode mode;
    private final Rv32iBulkExecutor delegate;

    public Rv32iBulkExecutorAdapter(Mode mode) {
        this(mode, new Rv32iBulkExecutor());
    }

    Rv32iBulkExecutorAdapter(Mode mode, Rv32iBulkExecutor delegate) {
        this.mode = Objects.requireNonNull(mode, "mode");
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public BulkExecution execute(BulkTask task, Map<String, Object> values) {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(values, "values");
        if (!OPERATION_ID.equals(task.operationId())) {
            throw new IllegalArgumentException("unsupported operationId");
        }
        if (!task.inputs().equals(List.of(INPUT_BATCH, INPUT_BUDGET))
                || !task.outputs().equals(List.of(OUTPUT_BATCH))
                || !values.keySet().equals(Set.of(INPUT_BATCH, INPUT_BUDGET))) {
            throw new IllegalArgumentException("RV32IM bulk schema");
        }
        Object batchValue = values.get(INPUT_BATCH);
        Object budgetValue = values.get(INPUT_BUDGET);
        if (!(batchValue instanceof Rv32iBatch input)) {
            throw new IllegalArgumentException("batch");
        }
        if (!(budgetValue instanceof Integer budget) || budget < 1) {
            throw new IllegalArgumentException("instructionBudget");
        }
        if (task.workItems() != input.coreCount()) {
            throw new IllegalArgumentException("workItems/coreCount mismatch");
        }

        Rv32iBatch output = input.copy();
        Rv32iBulkReceipt receipt;
        if (mode == Mode.CPU) {
            if (task.localWork() != 0) {
                throw new IllegalArgumentException("CPU localWork must be zero");
            }
            receipt = delegate.executeCpu(output, budget);
        } else {
            if (task.localWork() < 1 || task.localWork() > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Tornado localWork");
            }
            try {
                receipt = delegate.executeVerifiedTornado(
                        output,
                        budget,
                        Math.toIntExact(task.localWork()));
            } catch (RuntimeException | Error failure) {
                throw failure;
            } catch (Exception failure) {
                throw new IllegalStateException("TornadoVM RV32IM execution failed", failure);
            }
            if (receipt.mode() != Rv32iBulkReceipt.Mode.TORNADO_VERIFIED) {
                throw new IllegalStateException("unverified TornadoVM receipt");
            }
        }

        return new BulkExecution(
                Map.of(OUTPUT_BATCH, output),
                mode.provider(),
                receipt.root());
    }
}
