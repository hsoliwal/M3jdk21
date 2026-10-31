/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Runs the same independent oracle on the owning module's real Java implementation. */
class M3LongIndexMonotoneTest {
    @Test
    void stableMonotoneEncounterWindows() {
        M3LongIndexMonotoneContract.main(new String[0]);
    }
}
