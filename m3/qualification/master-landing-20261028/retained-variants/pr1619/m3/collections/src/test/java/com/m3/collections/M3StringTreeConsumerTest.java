/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

final class M3StringTreeConsumerTest {
    @Test
    void existingOwnersRetainStringAndComparatorContracts() throws ReflectiveOperationException {
        M3StringTreeConsumerContract.run(Boolean.getBoolean("m3.requireStringStorage"));
    }
}
