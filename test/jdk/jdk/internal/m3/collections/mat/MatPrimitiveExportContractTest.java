/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections.mat;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Runs the same target contract through the existing collections Maven suite. */
final class MatPrimitiveExportContractTest {
    @Test
    void immutablePrimitiveExportsPreserveTheirContracts() {
        assertTrue(MatPrimitiveExportContract.verify() > 211_200L,
                "the retained oracle and the target edge cases must all run");
    }
}
