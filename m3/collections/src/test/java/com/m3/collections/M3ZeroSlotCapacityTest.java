// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** JUnit/owning-module adapter for the independently executable contract oracle. */
final class M3ZeroSlotCapacityTest {
    @Test void primitiveZeroKeyIsNotAProbeSlot() {
        M3ZeroSlotCapacityContract.main(new String[0]);
    }
}
