<<<<<<< HEAD
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Calls the same frozen standalone contract used by the target runtime gate. */
class M3WrapperConvergenceTest {
    @Test
    void strictReadOnlyListIterationAndRetainedFailureContracts() {
        M3WrapperConvergenceContract.run("readonly-policy");
    }
}
||||||| merged common ancestors
=======
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
>>>>>>> d46d46ab7f20a4cf782e72d353a105f634e10692
