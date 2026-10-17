// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** JUnit wrapper for the actual M3 binary-heap mask receiving contract. */
final class M3IndexMaskPriorityTest {
    @Test
    void zeroIntermediatePrioritySelection() {
        M3IndexMaskPrioritySelfTest.main(new String[0]);
    }
}
