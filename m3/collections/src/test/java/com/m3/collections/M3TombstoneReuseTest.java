/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Maven/Surefire adapter for the standalone, JDK21-executable contract. */
final class M3TombstoneReuseTest {
    @Test void tombstoneReusePreservesPrimitiveSetMapContracts() {
        M3TombstoneReuseSelfTest.main(new String[0]);
    }
}
