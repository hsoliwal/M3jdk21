<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
=======
/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
>>>>>>> 9038bbc66331c3dcd4766fb424916c6fc4b61765
package com.m3.collections;

import org.junit.jupiter.api.Test;

<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
/** Calls the same frozen standalone contract used by the target runtime gate. */
class M3WrapperConvergenceTest {
    @Test
    void strictReadOnlyListIterationAndRetainedFailureContracts() {
        M3WrapperConvergenceContract.run("readonly-policy");
=======
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
>>>>>>> 9038bbc66331c3dcd4766fb424916c6fc4b61765
    }
}
