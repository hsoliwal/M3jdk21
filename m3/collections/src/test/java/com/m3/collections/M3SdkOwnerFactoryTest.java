/* Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;
import org.junit.jupiter.api.Test;
/** Executes real current owner contracts rather than only verifying names. */
final class M3SdkOwnerFactoryTest {
    @Test void ownerFactoriesAndSortedViewContracts() {
        M3SdkOwnerFactorySelfTest.main(new String[0]);
    }
}
