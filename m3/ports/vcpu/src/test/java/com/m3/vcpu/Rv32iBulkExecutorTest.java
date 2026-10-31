/* SPDX-License-Identifier: Apache-2.0 */
/* Ported from hsoliwal/synexia-tornadovm m3/cpu-gpu@3bf826714a48d7fdf346ffde8a0fc25c525572c5 src/test/java/com/synexia/m3/cpugpu/Rv32iBulkExecutorTest.java; see PROVENANCE.md. */
package com.m3.vcpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.parallel.Backend;

import org.junit.jupiter.api.Test;

final class Rv32iBulkExecutorTest {

    @Test
    void cpuReceiptsAreDeterministicForEquivalentBatches() {
        Rv32iBulkExecutor executor = new Rv32iBulkExecutor();
        Rv32iBatch first = batch(64);
        Rv32iBatch second = batch(64);

        Rv32iBulkReceipt a = executor.executeCpu(first, 1024);
        Rv32iBulkReceipt b = executor.executeCpu(second, 1024);

        assertEquals(a, b);
        assertEquals(Rv32iBulkReceipt.Mode.CPU, a.mode());
        assertEquals(64, a.cores());
        assertEquals(5050, first.exitCode(0));
        assertTrue(a.root().matches("[0-9a-f]{64}"));
    }

    @Test
    void rejectsInvalidBudgetsBeforeExecution() {
        Rv32iBulkExecutor executor = new Rv32iBulkExecutor();
        assertThrows(
                IllegalArgumentException.class,
                () -> executor.executeCpu(batch(1), 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> executor.executeVerified(batch(1), 1, Backend.reference(), 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> executor.executeVerified(batch(1), 0, Backend.reference(), 64));
    }

    @Test
    void parallelModeRequiresFullCpuParity() {
        Rv32iBatch actual = batch(256);
        Rv32iBatch oracle = batch(256);
        Rv32iBulkReceipt receipt =
                new Rv32iBulkExecutor()
                        .executeVerified(actual, 1024, Backend.reference(), 64);
        Rv32iBulkReceipt cpu = new Rv32iBulkExecutor().executeCpu(oracle, 1024);

        assertEquals(Rv32iBulkReceipt.Mode.PARALLEL_VERIFIED, receipt.mode());
        assertEquals(5050, actual.exitCode(0));
        assertEquals(64, receipt.localWork());
        assertTrue(receipt.verificationRoot().matches("[0-9a-f]{64}"));
        assertEquals(cpu.inputRoot(), receipt.inputRoot());
        assertEquals(cpu.outputRoot(), receipt.outputRoot());
        assertEquals(receipt, new Rv32iBulkExecutor().executeVerified(batch(256), 1024, Backend.reference(), 64));
    }

    @Test
    void receiptRejectsTamperedRoots() {
        Rv32iBulkReceipt receipt = new Rv32iBulkExecutor().executeCpu(batch(2), 64);
        assertThrows(IllegalArgumentException.class, () -> new Rv32iBulkReceipt(receipt.mode(), receipt.cores(),
                receipt.instructionBudget() + 1, receipt.localWork(), receipt.inputRoot(), receipt.outputRoot(),
                receipt.verificationRoot(), receipt.root()));
        assertThrows(IllegalArgumentException.class, () -> new Rv32iBulkReceipt(receipt.mode(), receipt.cores(),
                receipt.instructionBudget(), receipt.localWork(), "XYZ", receipt.outputRoot(),
                receipt.verificationRoot(), null));
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
        for (int core = 0; core < cores; core++) {
            batch.setProgramCounter(core, 0);
        }
        return batch;
    }
}
