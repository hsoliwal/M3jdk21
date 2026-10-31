/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Exercises the original primitive owners and the method-image contract oracle. */
public final class M3MethodSignalImageTest {
    @Test public void sealedMethodImageAndGraphDifferential() {
        M3MethodSignalImageSelfTest.runAll();
    }
}
