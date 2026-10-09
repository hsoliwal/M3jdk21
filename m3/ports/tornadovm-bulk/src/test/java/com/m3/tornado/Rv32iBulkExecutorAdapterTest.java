// SPDX-License-Identifier: Apache-2.0
package com.m3.tornado;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.synexia.m3.cpugpu.Rv32iAssembler;
import com.synexia.m3.cpugpu.Rv32iBatch;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import jdk.internal.vm.parallel.BulkExecution;
import jdk.internal.vm.parallel.BulkTask;

final class Rv32iBulkExecutorAdapterTest {
    @Test
    void cpuModeCopiesInputAndReturnsJdkReceipt() {
        Rv32iBatch input = batch(64);
        int[] beforeRegisters = input.snapshotRegisters();
        int[] beforeState = input.snapshotState();
        int[] beforeMemory = input.snapshotMemory();

        BulkExecution execution =
                new Rv32iBulkExecutorAdapter(
                                Rv32iBulkExecutorAdapter.Mode.CPU)
                        .execute(
                                task(64, 0),
                                Map.of(
                                        Rv32iBulkExecutorAdapter.INPUT_BATCH, input,
                                        Rv32iBulkExecutorAdapter.INPUT_BUDGET, 1024));

        Rv32iBatch output =
                (Rv32iBatch)
                        execution.outputs().get(
                                Rv32iBulkExecutorAdapter.OUTPUT_BATCH);
        assertNotSame(input, output);
        assertEquals(5050, output.exitCode(0));
        assertTrue(Arrays.equals(beforeRegisters, input.snapshotRegisters()));
        assertTrue(Arrays.equals(beforeState, input.snapshotState()));
        assertTrue(Arrays.equals(beforeMemory, input.snapshotMemory()));
        assertEquals("cpu-rv32im", execution.provider());
        assertTrue(execution.receiptRoot().matches("[0-9a-f]{64}"));
    }

    @Test
    void rejectsWrongOperationGeometryAndSchema() {
        Rv32iBatch input = batch(4);
        var adapter =
                new Rv32iBulkExecutorAdapter(
                        Rv32iBulkExecutorAdapter.Mode.CPU);
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        adapter.execute(
                                new BulkTask(
                                        "other",
                                        4,
                                        0,
                                        List.of("batch", "instructionBudget"),
                                        List.of("batch")),
                                Map.of("batch", input, "instructionBudget", 10)));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        adapter.execute(
                                task(3, 0),
                                Map.of("batch", input, "instructionBudget", 10)));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        adapter.execute(
                                task(4, 1),
                                Map.of("batch", input, "instructionBudget", 10)));
    }

    @Test
    void tornadoModeRequiresExistingProviderCpuParity() {
        Assumptions.assumeTrue(Boolean.getBoolean("m3.gpu"));
        Rv32iBatch input = batch(256);
        BulkExecution execution =
                new Rv32iBulkExecutorAdapter(
                                Rv32iBulkExecutorAdapter.Mode.TORNADO_VERIFIED)
                        .execute(
                                task(256, 64),
                                Map.of("batch", input, "instructionBudget", 1024));
        assertEquals("tornado-rv32im", execution.provider());
        assertEquals(
                5050,
                ((Rv32iBatch) execution.outputs().get("batch")).exitCode(0));
    }

    private static BulkTask task(long cores, long localWork) {
        return new BulkTask(
                Rv32iBulkExecutorAdapter.OPERATION_ID,
                cores,
                localWork,
                List.of(
                        Rv32iBulkExecutorAdapter.INPUT_BATCH,
                        Rv32iBulkExecutorAdapter.INPUT_BUDGET),
                List.of(Rv32iBulkExecutorAdapter.OUTPUT_BATCH));
    }

    private static Rv32iBatch batch(int cores) {
        Rv32iBatch batch = new Rv32iBatch(cores, 4096);
        int[] program =
                Rv32iAssembler.words(
                        Rv32iAssembler.addi(1, 0, 0),
                        Rv32iAssembler.addi(2, 0, 1),
                        Rv32iAssembler.addi(3, 0, 101),
                        Rv32iAssembler.add(1, 1, 2),
                        Rv32iAssembler.addi(2, 2, 1),
                        Rv32iAssembler.blt(2, 3, -8),
                        Rv32iAssembler.addi(10, 1, 0),
                        Rv32iAssembler.halt());
        batch.loadWordsAllCores(0, program);
        for (int core = 0; core < cores; core++) batch.setProgramCounter(core, 0);
        return batch;
    }
}
