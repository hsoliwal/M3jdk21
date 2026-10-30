/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Makes the unchanged deterministic collection oracle discoverable by Jupiter. */
final class M3Flat3MapTest {
    @Test
    void originalContract() throws Exception {
        M3Flat3MapContract.main(new String[0]);
    }
}
