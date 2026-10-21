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
