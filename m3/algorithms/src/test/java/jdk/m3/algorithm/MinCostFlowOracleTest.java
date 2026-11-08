// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

//FLLDCIM: ORACLE - surefire entry of the relocated donor oracle M3MinCostFlowOracle (a main program, body byte for byte); recipe/mincost-flow-20261106 gate 2
/**
 * Runs the relocated donor oracle {@link M3MinCostFlowOracle} once in the test phase and asserts the figure it
 * prints: 6,671 executable checks (exhaustive small networks: exact flow and cost against enumeration of every
 * integral flow, conservation, capacity, rerouting through reverse arcs, deterministic replay, detached results,
 * four refusals). The oracle keeps its check counter in a static field, so it runs once per JVM.
 */
class MinCostFlowOracleTest {
    /** The donor's own figure (crate m3-cpu-mincost-flow-20261031, VERIFICATION.tsv: 6671 JIT, 6671 -Xint). */
    private static final String DONOR_FIGURE = "M3_MINCOST_FLOW_PASS checks=6671";

    @Test
    void donorOracleFigure() {
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream saved = System.out;
        try (PrintStream sink = new PrintStream(captured, true, StandardCharsets.UTF_8)) {
            System.setOut(sink);
            MinCostFlowOracle.main(new String[0]);
        } finally {
            System.setOut(saved);
        }
        assertEquals(DONOR_FIGURE, captured.toString(StandardCharsets.UTF_8).strip());
    }
}
