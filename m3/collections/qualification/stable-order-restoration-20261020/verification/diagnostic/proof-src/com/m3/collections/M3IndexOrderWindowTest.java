// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Runs the frozen projection/window oracle against the actual available owner backend. */
class M3IndexOrderWindowTest {
    @Test
    void stablePhysicalOrderAndMaskWindowContracts() throws Exception {
        M3IndexOrderWindowContract.main(new String[] {
            M3IndexMaskCompactor.nativeAvailable() ? "--native" : "--java"
        });
    }
}
