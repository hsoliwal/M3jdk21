// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

/** Runs the frozen projection/window oracle against the actual available owner backend. */
class IndexOrderWindowTest {
    @Test
    void stablePhysicalOrderAndMaskWindowContracts() throws Exception {
        IndexOrderWindowContract.main(new String[] {
            IndexMaskCompactor.nativeAvailable() ? "--native" : "--java"
        });
    }
}
