/* SPDX-License-Identifier: Apache-2.0 */

/**
 * RV32IM virtual CPU on the M3 parallel API: ISA constants, structure-of-arrays batch state, the sequential CPU
 * oracle, the assembler and ELF loader, the {@code m3.rv32im.slice} kernel, sessions and verified receipts.
 * Ported from cpu-gpu-rv32im (Apache-2.0); see PROVENANCE.md.
 */
module m3.vcpu {
    requires transitive m3.parallel;

    exports com.m3.vcpu;
}
