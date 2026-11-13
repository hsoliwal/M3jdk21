// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

/** Maven bridge to the original independent main-based Java 21 oracle. */
public final class LongOrderStatisticArrayGrowthJUnitTest {
    @Test
    void fullArrayGrowthAndLogicalClear() throws Exception {
        GrowthProbe.main(new String[0]);
        LongOrderStatisticArrayGrowthTest.main(new String[0]);
    }
}
