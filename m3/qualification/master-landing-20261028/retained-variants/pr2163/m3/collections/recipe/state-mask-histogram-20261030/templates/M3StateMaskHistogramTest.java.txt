/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Owning module JUnit hook for the standalone deterministic Java/JNI contract. */
public final class M3StateMaskHistogramTest {
    @Test public void differentialAndMaskAlgebra() {
        M3StateMaskHistogramSelfTest.main(new String[0]);
    }
}
