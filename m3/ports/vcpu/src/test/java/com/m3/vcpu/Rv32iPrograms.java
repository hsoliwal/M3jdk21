/* SPDX-License-Identifier: Apache-2.0 */
/* Ported from hsoliwal/synexia-tornadovm m3/cpu-gpu@3bf826714a48d7fdf346ffde8a0fc25c525572c5 src/main/java/com/synexia/m3/cpugpu/Rv32iDemo.java (newBatch, sumOneToHundredProgram); see PROVENANCE.md. */
package com.m3.vcpu;

/** Sample guest programs from the donor demo; the CLI and its console output are not ported. */
final class Rv32iPrograms {
    private static final int MEMORY_BYTES_PER_CORE = 4096;

    private Rv32iPrograms() {
    }

    static Rv32iBatch newBatch(int cores) {
        Rv32iBatch batch = new Rv32iBatch(cores, MEMORY_BYTES_PER_CORE);
        int[] program = sumOneToHundredProgram();
        batch.loadWordsAllCores(0, program);
        for (int core = 0; core < cores; core++) {
            batch.setProgramCounter(core, 0);
        }
        return batch;
    }

    static int[] sumOneToHundredProgram() {
        return Rv32iAssembler.words(
                Rv32iAssembler.addi(1, 0, 0),
                Rv32iAssembler.addi(2, 0, 1),
                Rv32iAssembler.addi(3, 0, 101),
                Rv32iAssembler.add(1, 1, 2),
                Rv32iAssembler.addi(2, 2, 1),
                Rv32iAssembler.blt(2, 3, -8),
                Rv32iAssembler.addi(10, 1, 0),
                Rv32iAssembler.halt());
    }
}
