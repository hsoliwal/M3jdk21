// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;
import org.junit.jupiter.api.Test;
/** Maven/JUnit receiver for real M3 multi-stage index-path mask semantics. */
final class M3IndexPathImpossibleStageTest {
    @Test void multiStageContradictionsAndStrictOffsetValidation() {
        M3IndexPathImpossibleStageSelfTest.main(new String[0]);
    }
}
