// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Source-sealed Maven/JUnit adapter for the independent Java/JNI parity contract. */
final class M3ObjectPhysicalMaskTest {
    @Test void contracts() {
        M3ObjectPhysicalMaskSelfTest.main(new String[0]);
    }
}
