// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ast;

import org.junit.jupiter.api.Test;

/** Run exact-source structural interning oracles under the product Maven module. */
final class M3SyntaxGreenPoolTest {
    @Test void losslessInterningAcrossGenerations() {
        M3SyntaxGreenPoolContract.main(new String[0]);
    }
}
