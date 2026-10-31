// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Runs the original Java 21 monotonic-index differential corpus in Maven. */
final class M3LongIndexNaturalRunTest {
    @Test void stableSortPreservesIndicesAndSlices() {
        M3LongIndexNaturalRunContract.main(new String[0]);
    }
}
