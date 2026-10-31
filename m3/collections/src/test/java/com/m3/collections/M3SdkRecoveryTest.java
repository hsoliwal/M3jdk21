/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** JUnit adapter for the reusable M3 SDK partition/bag/filter contract oracle. */
final class M3SdkRecoveryTest {
    @Test void exactSdkRecoveryContract() {
        M3SdkRecoverySelfTest.main(new String[0]);
    }
}
