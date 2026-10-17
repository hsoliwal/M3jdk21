/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;
import org.junit.jupiter.api.Test;
/** JUnit bridge for the exact first-party standalone collection contract corpus. */
final class M3CopyOnWriteBulkTest {
    @Test
    void copyOnWriteAndSortedBackingContracts() {
        M3CopyOnWriteBulkContract.main(new String[0]);
    }
}
