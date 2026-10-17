/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Native qualification is run separately, with a required real library and -Xcheck:jni. */
public final class M3SdkFilterWindowTest {
    @Test void capturedBulkInputsPreserveFilteredContracts() {
        M3BulkCaptureContract.main(new String[0]);
    }
    @Test void boundedJavaMaskScansPreserveEncounterOrder() throws Exception {
        M3BudgetedMaskWindowContract.main(new String[0]);
    }
}
