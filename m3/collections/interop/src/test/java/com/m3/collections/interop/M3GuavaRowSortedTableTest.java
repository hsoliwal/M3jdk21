/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections.interop;

import org.junit.jupiter.api.Test;

/** JUnit gate over the actual Guava Table API and current M3 storage owners. */
final class M3GuavaRowSortedTableTest {
    @Test void indexedSortedRowMapDifferential() {
        M3GuavaRowSortedTableContract.main(new String[0]);
    }
}
