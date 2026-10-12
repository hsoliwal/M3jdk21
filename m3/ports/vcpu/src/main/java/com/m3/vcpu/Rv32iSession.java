/* SPDX-License-Identifier: Apache-2.0 */
/* Ported from hsoliwal/synexia-tornadovm m3/cpu-gpu@3bf826714a48d7fdf346ffde8a0fc25c525572c5 src/main/java/com/synexia/m3/cpugpu/TornadoRv32iSession.java; see PROVENANCE.md. */
package com.m3.vcpu;

import com.m3.parallel.Backend;
import com.m3.parallel.Grid1D;
import com.m3.parallel.IntBuffer;
import com.m3.parallel.Plan;
import com.m3.parallel.Result;
import com.m3.parallel.Transfer;

import java.util.Objects;

/**
 * Persistent m3.parallel execution session. Architectural state stays on the device between slices;
 * registers/state are copied back after each slice, while RAM is copied back only on demand.
 *
 * <p>Transfer policy, as in the donor TornadoVM session: memory, registers and state are uploaded once
 * ({@link Transfer#ONCE}); config is uploaded before every slice ({@link Transfer#EACH_LAUNCH}); nothing is copied
 * back implicitly. Host-side changes to the batch after the first slice are not seen by the device.</p>
 */
public final class Rv32iSession implements AutoCloseable {
    private static final int DEFAULT_LOCAL_WORK_SIZE = 64;

    private final Rv32iBatch batch;
    private final IntBuffer memory;
    private final IntBuffer registers;
    private final IntBuffer state;
    private final IntBuffer config;
    private final Plan plan;
    private Result lastResult;

    /**
     * Opens a session with work-group size 64.
     *
     * @param batch   batch updated by every slice
     * @param backend executing backend
     */
    public Rv32iSession(Rv32iBatch batch, Backend backend) {
        this(batch, backend, DEFAULT_LOCAL_WORK_SIZE);
    }

    /**
     * Opens a session.
     *
     * @param batch         batch updated by every slice
     * @param backend       executing backend
     * @param localWorkSize work-group size, {@code > 0}
     */
    public Rv32iSession(Rv32iBatch batch, Backend backend, int localWorkSize) {
        if (batch == null) {
            throw new NullPointerException("batch");
        }
        Objects.requireNonNull(backend, "backend");
        if (localWorkSize <= 0) {
            throw new IllegalArgumentException("localWorkSize must be > 0");
        }
        this.batch = batch;
        this.memory = IntBuffer.of(batch.rawMemory());
        this.registers = IntBuffer.of(batch.rawRegisters());
        this.state = IntBuffer.of(batch.rawState());
        this.config = IntBuffer.of(batch.coreCount(), batch.memoryWordsPerCore(), 1);
        this.plan = plan(backend, batch.coreCount(), localWorkSize, memory, registers, state, config);
    }

    /**
     * Builds the {@link Rv32iKernel#RV32IM_SLICE} plan over the four state buffers. The grid is the core count rounded
     * up to {@code localWorkSize}; padded work-items return immediately.
     *
     * @param backend       executing backend
     * @param coreCount     number of virtual cores
     * @param localWorkSize work-group size
     * @param memory        guest RAM, {@link Transfer#ONCE}
     * @param registers     register files, {@link Transfer#ONCE}
     * @param state         architectural state, {@link Transfer#ONCE}
     * @param config        core count, words per core and budget, {@link Transfer#EACH_LAUNCH}
     * @return an open plan
     */
    public static Plan plan(Backend backend, int coreCount, int localWorkSize, IntBuffer memory, IntBuffer registers,
            IntBuffer state, IntBuffer config) {
        return Plan.builder(backend)
                .kernel(Rv32iKernel.kernelId())
                .grid(new Grid1D(coreCount, localWorkSize))
                .bind(memory, Transfer.ONCE)
                .bind(registers, Transfer.ONCE)
                .bind(state, Transfer.ONCE)
                .bind(config, Transfer.EACH_LAUNCH)
                .build();
    }

    /**
     * Runs every READY or YIELDED core for at most {@code instructionBudget} instructions and copies registers and
     * state back into the batch.
     *
     * @param instructionBudget per-core budget, {@code > 0}
     * @return the batch
     */
    public Rv32iBatch runSlice(int instructionBudget) {
        if (instructionBudget <= 0) {
            throw new IllegalArgumentException("instructionBudget must be > 0");
        }
        config.set(Rv32i.CONFIG_INSTRUCTION_BUDGET, instructionBudget);
        lastResult = plan.execute();
        lastResult.download(registers, state);
        batch.replaceRegistersAndState(registers.toArray(), state.toArray());
        return batch;
    }

    /**
     * Copies device RAM back to the batch. Call only when the host needs guest memory contents;
     * omitting this transfer is important for large resident batches.
     *
     * @return the batch
     */
    public Rv32iBatch downloadMemory() {
        requireExecution();
        lastResult.download(memory);
        batch.replaceMemory(memory.toArray());
        return batch;
    }

    /**
     * Copies device RAM, registers and state back to the batch.
     *
     * @return the batch
     */
    public Rv32iBatch synchronizeAll() {
        requireExecution();
        lastResult.download(memory, registers, state);
        batch.replaceMemory(memory.toArray());
        batch.replaceRegistersAndState(registers.toArray(), state.toArray());
        return batch;
    }

    /**
     * Returns the underlying plan.
     *
     * @return the plan
     */
    public Plan executionPlan() {
        return plan;
    }

    @Override
    public void close() {
        plan.close();
    }

    private void requireExecution() {
        if (lastResult == null) {
            throw new IllegalStateException("runSlice must execute before downloading device state");
        }
    }
}
