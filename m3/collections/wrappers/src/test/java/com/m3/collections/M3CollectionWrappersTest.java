/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Maven/JUnit entry point for the standalone purpose-wrapper proof. */
final class M3CollectionWrappersTest {
    @Test
    void purposeWrapperContracts() {
        M3CollectionWrappersSelfTest.main(new String[0]);
    }
}
