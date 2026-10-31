// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Retains the long-range percentage regression in the original owning Maven module. */
final class M3ProgressAdaptersPercentageTest {
    @Test void exactLongPercentageAndCallbackContracts() throws Exception {
        M3ProgressAdaptersPercentageSelfTest.verify();
    }
}
