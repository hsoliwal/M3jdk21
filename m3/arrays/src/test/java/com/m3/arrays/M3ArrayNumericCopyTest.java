/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0 */
package com.m3.arrays;

import org.junit.jupiter.api.Test;

final class M3ArrayNumericCopyTest {
    @Test
    void numericCopyMatchesDefaultInterfaceBehavior() throws InterruptedException {
        M3ArrayNumericCopySelfTest.runContract();
    }
}
