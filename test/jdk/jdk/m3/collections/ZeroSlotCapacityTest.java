// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

/** JUnit/owning-module adapter for the independently executable contract oracle. */
final class ZeroSlotCapacityTest {
    @Test void primitiveZeroKeyIsNotAProbeSlot() {
        ZeroSlotCapacityContract.main(new String[0]);
    }
}
