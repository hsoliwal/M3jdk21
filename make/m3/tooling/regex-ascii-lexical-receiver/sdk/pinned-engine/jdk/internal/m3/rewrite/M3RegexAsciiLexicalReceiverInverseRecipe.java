/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.rewrite;

/** Sealed numeric-lexeme correction; never broadens the admitted regex language. */
public final class M3RegexAsciiLexicalReceiverInverseRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/jdk/internal/m3/rewrite/regex-ascii-lexical-receiver-20261019/inverse/";
    }
    @Override public String getDisplayName() { return "Replay the original M3 regex numeric admission fixture"; }
    @Override public String getDescription() {
        return "Recovers exact original parser, test and component mapping fixtures while "
                + "preserving existing fallback dialects and unchanged dependency owners.";
    }
}
