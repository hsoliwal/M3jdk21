/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** JUnit entrypoint for deterministic Java; real JNI is qualified separately. */
final class M3IndexPathMasksTest {
    @Test
    void maskedIndexPathsPreserveDetachedOrderAndBounds() {
        M3IndexPathMasksSelfTest.main(new String[0]);
    }
}
