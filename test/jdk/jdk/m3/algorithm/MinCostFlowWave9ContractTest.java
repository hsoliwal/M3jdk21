// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

//FLLDCIM: ORACLE - surefire entry of the relocated wave9 flow contract M3MinCostFlowWave9Contract (a main program); recipe m3/algorithms/recipe/census2-a5-20261106/relocate.py v1
/**
 * Runs the relocated wave9 flow contract {@link M3MinCostFlowWave9Contract} once in the test phase and asserts the
 * figure it prints: 3,100 seeded random networks compared with an exhaustive enumeration of every integral flow
 * (flow value and cost), the self-loop refusals of the instances that have one, input non-aliasing, one costed
 * reverse-arc reroute (census2 addition), the empty network and two refusals. The contract keeps its check counter
 * in a static field, so it runs once per JVM.
 */
class MinCostFlowWave9ContractTest {
    /** The figure of the mapped contract over the landed owner (recipe README: donor 138,244 = flow 6,204 + spatial). */
    private static final String FIGURE = "M3_MINCOST_FLOW_WAVE9_PASS checks=8070";

    @Test
    void wave9FlowFigure() {
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream saved = System.out;
        try (PrintStream sink = new PrintStream(captured, true, StandardCharsets.UTF_8)) {
            System.setOut(sink);
            MinCostFlowWave9Contract.main(new String[0]);
        } finally {
            System.setOut(saved);
        }
        assertEquals(FIGURE, captured.toString(StandardCharsets.UTF_8).strip());
    }
}
