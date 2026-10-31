/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.vcpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.parallel.Backend;
import com.m3.parallel.Grid1D;
import com.m3.parallel.JavaKernels;
import com.m3.parallel.KernelId;
import com.m3.parallel.Plan;
import com.m3.parallel.Transfer;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Session semantics carried over from the donor TornadoVM session: residency, slicing and on-demand RAM. */
final class Rv32iSessionTest {

    @Test
    void sliceKernelIsRegisteredUnderItsStableId() {
        assertEquals(KernelId.of("m3.rv32im.slice"), Rv32iKernel.RV32IM_SLICE);
        assertSame(Rv32iKernel.RV32IM_SLICE, Rv32iKernel.kernelId());
        assertTrue(JavaKernels.lookup(Rv32iKernel.RV32IM_SLICE).isPresent());
        assertTrue(Backend.reference().supports(Rv32iKernel.RV32IM_SLICE));
    }

    @Test
    void planBindsTheFourStateBuffersWithDonorTransferModes() {
        Rv32iBatch batch = Rv32iPrograms.newBatch(5);
        try (Rv32iSession session = new Rv32iSession(batch, Backend.reference())) {
            Plan plan = session.executionPlan();
            assertEquals(Backend.REFERENCE_ID, plan.backend().id());
            assertEquals(new Grid1D(5, 64), plan.grid());
            assertEquals(64, plan.grid().launchSize());
            assertEquals(List.of(Transfer.ONCE, Transfer.ONCE, Transfer.ONCE, Transfer.EACH_LAUNCH),
                    plan.bindings().stream().map(Plan.Binding::transfer).toList());
            assertEquals(Rv32iKernel.ARG_COUNT, plan.bindings().size());
        }
    }

    @Test
    void stateStaysResidentAcrossSlicesAndRamDownloadsOnlyOnDemand() {
        Rv32iBatch batch = Rv32iPrograms.newBatch(3);
        try (Rv32iSession session = new Rv32iSession(batch, Backend.reference(), 2)) {
            session.runSlice(20);
            assertEquals(Rv32i.STATUS_YIELDED, batch.status(0));
            int firstRetired = batch.retiredInstructions(0);

            batch.writeWord(1, 2048, 0x1234_5678);
            batch.writeWord(1, 24, Rv32iAssembler.addi(10, 0, 9));
            session.runSlice(1000);
            assertEquals(Rv32i.STATUS_HALTED, batch.status(1));
            assertEquals(5050, batch.exitCode(1), "the device never saw the host-side program patch");
            assertTrue(batch.retiredInstructions(0) > firstRetired);
            assertEquals(0x1234_5678, batch.readWord(1, 2048), "RAM was not copied back implicitly");

            session.downloadMemory();
            assertEquals(0, batch.readWord(1, 2048));
            assertEquals(Rv32iAssembler.addi(10, 1, 0), batch.readWord(1, 24));
        }
    }

    @Test
    void rejectsInvalidUseLikeTheDonorSession() {
        Rv32iBatch batch = Rv32iPrograms.newBatch(1);
        assertThrows(NullPointerException.class, () -> new Rv32iSession(null, Backend.reference()));
        assertThrows(IllegalArgumentException.class, () -> new Rv32iSession(batch, Backend.reference(), 0));
        Rv32iSession session = new Rv32iSession(batch, Backend.reference());
        assertThrows(IllegalStateException.class, session::downloadMemory);
        assertThrows(IllegalStateException.class, session::synchronizeAll);
        assertThrows(IllegalArgumentException.class, () -> session.runSlice(0));
        session.runSlice(1000);
        assertEquals(5050, batch.exitCode(0));
        session.close();
        assertFalse(session.executionPlan().isOpen());
        assertThrows(IllegalStateException.class, () -> session.runSlice(1));
    }

    @Test
    void haltedAndTrappedCoresAreSkippedOnLaterSlices() {
        Rv32iBatch batch = new Rv32iBatch(2, 4096);
        batch.loadWords(0, 0, Rv32iAssembler.addi(10, 0, 3), Rv32iAssembler.halt(), Rv32iAssembler.addi(10, 0, 4));
        batch.loadWords(1, 0, Rv32iAssembler.ebreak(), Rv32iAssembler.halt());
        batch.setProgramCounter(0, 0);
        batch.setProgramCounter(1, 0);
        try (Rv32iSession session = new Rv32iSession(batch, Backend.reference(), 1)) {
            session.runSlice(10);
            session.runSlice(10);
        }
        assertEquals(Rv32i.STATUS_HALTED, batch.status(0));
        assertEquals(3, batch.exitCode(0));
        assertEquals(2, batch.retiredInstructions(0));
        assertEquals(8, batch.programCounter(0));
        assertEquals(Rv32i.STATUS_TRAPPED, batch.status(1));
        assertEquals(Rv32i.TRAP_BREAKPOINT, batch.trapCause(1));
        assertEquals(0, batch.trapValue(1));
        assertEquals(0, batch.retiredInstructions(1));
    }
}
