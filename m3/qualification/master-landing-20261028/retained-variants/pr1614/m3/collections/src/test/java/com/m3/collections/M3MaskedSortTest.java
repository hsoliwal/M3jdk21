// Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Runs the same source-bound physical-index oracle in the owning Maven test suite. */
final class M3MaskedSortTest {
    @Test void physicalIndexSelectionAndSorting() {
        M3MaskedSortContract.main(new String[0]);
    }
}
