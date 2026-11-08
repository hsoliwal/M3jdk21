/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

/** Makes the unchanged deterministic collection oracle discoverable by Jupiter. */
final class Flat3MapTest {
    @Test
    void originalContract() throws Exception {
        Flat3MapContract.main(new String[0]);
    }
}
