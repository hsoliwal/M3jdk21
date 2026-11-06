// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

final class M3LongRangeAlgebraTest {
    @Test
    void detachedRangeAlgebraMatchesIndependentBooleanSpans() {
        M3LongRangeAlgebraSelfTest.run();
    }
}
