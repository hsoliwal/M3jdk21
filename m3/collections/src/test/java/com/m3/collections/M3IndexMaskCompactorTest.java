/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** JUnit entrypoint for the Java reference contract; JNI parity is a separate opt-in lane. */
final class M3IndexMaskCompactorTest {
    @Test
    void stableMaskSelectionContract() {
        M3IndexMaskCompactorSelfTest.main(new String[0]);
    }
}
