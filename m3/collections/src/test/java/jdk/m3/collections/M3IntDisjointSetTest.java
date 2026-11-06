/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Existing primitive-module JUnit bridge to protected independent Java21 contracts. */
class M3IntDisjointSetTest {
    @Test
    void originalUnionFindAndComponentContracts() {
        assertEquals("checks=838808,operations=500000,sha256="
                + "500853dd93feaa2d74bbf201eea9ab0ce847b689c119a73b7ccef2a774c6abb9",
                M3IntDisjointSetSelfTest.run());
    }

    @Test
    void onePassPathHalvingStructureIsActive() {
        assertEquals(29L, M3IntDisjointSetStructuralSelfTest.run());
    }
}
