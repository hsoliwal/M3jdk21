// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ast;

import org.junit.jupiter.api.Test;

/** Source range projection against the real public jdk.compiler AST API. */
final class M3SyntaxJavacTest {
    @Test void javacParserProjection() throws Exception {
        M3SyntaxJavacContract.main(new String[0]);
    }
}
