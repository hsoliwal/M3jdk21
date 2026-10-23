<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
=======
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
>>>>>>> 2a39197bf095212f7db4c53c74646e331beb1503
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
/** Runs the frozen projection/window oracle against the actual available owner backend. */
class M3IndexOrderWindowTest {
    @Test
    void stablePhysicalOrderAndMaskWindowContracts() throws Exception {
        M3IndexOrderWindowContract.main(new String[] {
            M3IndexMaskCompactor.nativeAvailable() ? "--native" : "--java"
        });
=======
final class M3IndexOrderWindowTest {
    @Test void stableOrderAndBoundedWindowContract() throws Exception {
        M3IndexOrderWindowContract.main(new String[] {"--java"});
>>>>>>> 2a39197bf095212f7db4c53c74646e331beb1503
    }
}
