// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

final class M3IndexOrderWindowTest {
    @Test void stableOrderAndBoundedWindowContract() throws Exception {
        M3IndexOrderWindowContract.main(new String[] {"--java"});
    }
}
