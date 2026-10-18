/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

final class M3ObjectStreamContractTest {
    @Test
    void allOperatorFamiliesPreserveTheirPublishedContracts() {
        M3ObjectStreamContract.main(new String[0]);
    }
}
