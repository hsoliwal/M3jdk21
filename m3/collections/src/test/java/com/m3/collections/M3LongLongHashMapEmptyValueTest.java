/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Runs the same unboxed Java oracle under Maven Surefire as the standalone JDK21 gate. */
class M3LongLongHashMapEmptyValueTest {
    @Test
    void emptyAndNonemptyValueLookupContracts() {
        M3LongLongHashMapEmptyValueProof.main(new String[0]);
    }
}
