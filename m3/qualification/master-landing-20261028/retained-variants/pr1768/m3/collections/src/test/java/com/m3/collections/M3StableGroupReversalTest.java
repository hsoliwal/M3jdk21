// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Execute the recipe-received main oracle in the module's existing JUnit5 lane. */
final class M3StableGroupReversalTest {
    @Test void stableDescendingRunsAndMasks() {
        M3StableGroupReversalSelfTest.main(new String[0]);
    }
}
