/* SPDX-License-Identifier: Apache-2.0 */
/* Ported from hsoliwal/synexia-tornadovm m3/cpu-gpu@3bf826714a48d7fdf346ffde8a0fc25c525572c5 src/test/java/com/synexia/m3/cpugpu/TornadoRv32iDifferentialTest.java; see PROVENANCE.md. */
package com.m3.vcpu;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.parallel.Backend;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.IntStream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

/**
 * The m3.parallel CPU reference backend versus the sequential oracle. The first test is the donor GPU differential
 * test, ungated because the backend is always present; the others run the full-ISA corpus on multi-core batches.
 */
final class Rv32iParallelDifferentialTest {
    @Test
    void matchesIndependentCpuOracleAcrossManyCores() {
        Rv32iBatch actual = Rv32iPrograms.newBatch(256);
        Rv32iBatch expected = actual.copy();
        new CpuRv32iEngine().runSlice(expected, 1024);

        try (Rv32iSession session = new Rv32iSession(actual, Backend.reference())) {
            session.runSlice(1024);
            session.synchronizeAll();
        }

        assertArrayEquals(expected.snapshotRegisters(), actual.snapshotRegisters());
        assertArrayEquals(expected.snapshotState(), actual.snapshotState());
        assertArrayEquals(expected.snapshotMemory(), actual.snapshotMemory());
    }

    @ParameterizedTest(name = "cores={0} local={1}")
    @CsvSource({"256, 64", "256, 1", "300, 64", "300, 7", "512, 256", "1031, 32"})
    void fullInstructionSetCorpusHasIdenticalStateRootsEverySlice(int cores, int localWork) {
        Rv32iBatch expected = Rv32iCorpus.batch(cores, Rv32iCorpus.SEED + cores);
        Rv32iBatch actual = expected.copy();
        CpuRv32iEngine oracle = new CpuRv32iEngine();
        try (Rv32iSession session = new Rv32iSession(actual, Backend.reference(), localWork)) {
            for (int budget : Rv32iCorpus.SLICE_BUDGETS) {
                oracle.runSlice(expected, budget);
                session.runSlice(budget);
                session.synchronizeAll();
                assertEquals(Rv32iBulkExecutor.stateRoot(expected), Rv32iBulkExecutor.stateRoot(actual),
                        "state root after budget " + budget);
                assertArrayEquals(expected.snapshotRegisters(), actual.snapshotRegisters());
                assertArrayEquals(expected.snapshotState(), actual.snapshotState());
                assertArrayEquals(expected.snapshotMemory(), actual.snapshotMemory());
            }
        }
        assertCorpusOutcomes(expected);
    }

    @Test
    void randomProgramsMatchUnderManySeeds() {
        CpuRv32iEngine oracle = new CpuRv32iEngine();
        IntStream.range(0, 8).forEach(round -> {
            Rv32iBatch expected = Rv32iCorpus.batch(384, 0xC0FFEEL * (round + 1));
            Rv32iBatch actual = expected.copy();
            try (Rv32iSession session = new Rv32iSession(actual, Backend.reference(), 64)) {
                for (int budget : new int[] {3, 40, 1000}) {
                    oracle.runSlice(expected, budget);
                    session.runSlice(budget);
                }
                session.synchronizeAll();
            }
            assertEquals(Rv32iBulkExecutor.stateRoot(expected), Rv32iBulkExecutor.stateRoot(actual),
                    "round " + round);
        });
    }

    private static void assertCorpusOutcomes(Rv32iBatch batch) {
        Set<Integer> causes = new TreeSet<>();
        int halted = 0;
        for (int core = 0; core < batch.coreCount(); core++) {
            if (batch.status(core) == Rv32i.STATUS_TRAPPED) {
                causes.add(batch.trapCause(core));
            } else if (batch.status(core) == Rv32i.STATUS_HALTED) {
                halted++;
            }
        }
        assertEquals(Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8), causes, "every RISC-V trap cause is exercised");
        assertTrue(halted > 0, "some cores halt through XM3_HALT");
    }
}
