// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

/** Source-sealed Maven/JUnit adapter for the independent Java/JNI parity contract. */
final class ObjectPhysicalMaskTest {
    @Test void contracts() {
        ObjectPhysicalMaskSelfTest.main(new String[0]);
    }
}
