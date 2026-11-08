// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

/** Calls the same frozen standalone contract used by the target runtime gate. */
class WrapperConvergenceTest {
    @Test
    void strictReadOnlyListIterationAndRetainedFailureContracts() {
        WrapperConvergenceContract.run("readonly-policy");
    }

    //FLLDCIM: MRL - stale-contracts-20261106 W1: comparator-failure mode wired; check follows 79a8df95080/ef0beec8bfb
    @Test
    void comparatorOwnedFailurePropagatesThroughBoundedNavigation() {
        WrapperConvergenceContract.run("comparator-failure");
    }
}
