/* SPDX-License-Identifier: Apache-2.0 */
/* Ported from hsoliwal/synexia-tornadovm m3/cpu-gpu@3bf826714a48d7fdf346ffde8a0fc25c525572c5 src/main/java/com/synexia/m3/cpugpu/Rv32iBulkReceipt.java; see PROVENANCE.md. */
package com.m3.vcpu;

import java.util.Objects;

/**
 * Content-addressed execution receipt for one RV32IM bulk slice.
 *
 * <p>The receipt records what actually executed. A verified m3.parallel receipt is issued only after
 * an independent CPU run agrees on registers, architectural state and RAM.</p>
 *
 * @param mode              execution mode
 * @param cores             virtual core count
 * @param instructionBudget per-core instruction budget of the slice
 * @param localWork         work-group size; {@code 0} in CPU mode
 * @param inputRoot         batch state root before the slice
 * @param outputRoot        batch state root after the slice
 * @param verificationRoot  parity root
 * @param root              receipt root; {@code null} to compute
 */
public record Rv32iBulkReceipt(
        Mode mode,
        int cores,
        int instructionBudget,
        int localWork,
        String inputRoot,
        String outputRoot,
        String verificationRoot,
        String root) {

    /** Execution mode recorded in a receipt. */
    public enum Mode {
        /** Sequential CPU oracle only. */
        CPU,
        /** m3.parallel backend run admitted after full parity with the CPU oracle and the CPU reference backend. */
        PARALLEL_VERIFIED
    }

    public Rv32iBulkReceipt {
        Objects.requireNonNull(mode, "mode");
        if (cores < 1) throw new IllegalArgumentException("cores");
        if (instructionBudget < 1) throw new IllegalArgumentException("instructionBudget");
        if (localWork < 0) throw new IllegalArgumentException("localWork");
        inputRoot = Rv32iBulkExecutor.requireSha256(inputRoot, "inputRoot");
        outputRoot = Rv32iBulkExecutor.requireSha256(outputRoot, "outputRoot");
        verificationRoot = Rv32iBulkExecutor.requireSha256(verificationRoot, "verificationRoot");
        String expected =
                Rv32iBulkExecutor.receiptRoot(
                        mode,
                        cores,
                        instructionBudget,
                        localWork,
                        inputRoot,
                        outputRoot,
                        verificationRoot);
        if (root == null) {
            root = expected;
        } else if (!root.equals(expected)) {
            throw new IllegalArgumentException("receipt root mismatch");
        }
    }
}
