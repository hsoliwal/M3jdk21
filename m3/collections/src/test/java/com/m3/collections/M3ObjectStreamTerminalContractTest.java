/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

final class M3ObjectStreamTerminalContractTest {
    @Test
    void standardTerminalsRemainOrderedBoundedAndLifecycleSafe() {
        M3ObjectStreamTerminalContract.main(new String[0]);
    }
}
