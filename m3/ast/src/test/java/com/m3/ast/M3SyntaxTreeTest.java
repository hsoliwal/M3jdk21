// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ast;

import org.junit.jupiter.api.Test;

/** Maven/JUnit entrypoint for the standalone parser-neutral projection oracle. */
final class M3SyntaxTreeTest {
    @Test void parserNeutralProjectionContract() {
        M3SyntaxTreeContract.main(new String[0]);
    }
}
