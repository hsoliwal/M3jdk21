// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Owning SDK module executes the bounded path-mask independent oracle. */
final class M3IndexPathMaskBudgetTest {
    @Test
    void fullIntMaskedIndexPathWorkBudgetPreservesLegacyAndJniContracts() {
        M3IndexPathMaskBudgetSelfTest.main(new String[0]);
    }
}
