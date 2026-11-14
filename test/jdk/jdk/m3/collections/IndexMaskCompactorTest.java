/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

/** JUnit entrypoint for the Java reference contract; JNI parity is a separate opt-in lane. */
final class IndexMaskCompactorTest {
    @Test
    void stableMaskSelectionContract() {
        IndexMaskCompactorSelfTest.main(new String[0]);
    }
}
