// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Qualifies caller-visible results, directional predicate prefixes and failure identity. */
class M3FirstVisibleAtomTest {
    @Test
    void preservesFirstVisibleCallbackAndFailureTape() {
        // Near-entry navigation was intentionally changed to preserve comparator
        // failures. The contract checks real JDK results and legal predicate
        // prefixes, then injects both exception kinds at every observed callback.
        M3FirstVisibleAtomContract.Verification report = M3FirstVisibleAtomContract.verifyContracts();
        assertEquals(64, report.normalCases(), "complete finite selection matrix");
    }
}
