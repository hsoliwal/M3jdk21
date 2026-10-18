/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

final class M3WrapperConvergenceTest {
    @Test void storedNullMayOccupyAnInteriorComparatorPosition() {
        M3WrapperConvergenceSelfTest.interiorNullNavigation();
    }
    @Test void boundedMapsAndMutableSetsAcceptNeighborQueries() {
        M3WrapperConvergenceSelfTest.boundedNavigation();
    }
    @Test void sortedEndpointMutationRefusesBeforeCallbacks() {
        M3WrapperConvergenceSelfTest.strictSortedEndpoints();
    }
    @Test void laterInputCaptureAndListCompositionAtomsRemainAvailable() {
        M3WrapperConvergenceSelfTest.retainedCurrentAtoms();
    }
}
