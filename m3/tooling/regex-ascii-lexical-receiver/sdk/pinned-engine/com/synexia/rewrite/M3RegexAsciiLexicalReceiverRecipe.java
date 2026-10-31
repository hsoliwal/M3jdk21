/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.synexia.rewrite;

/** Sealed numeric-lexeme correction; never broadens the admitted regex language. */
public final class M3RegexAsciiLexicalReceiverRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/regex-ascii-lexical-receiver-20261019/forward/";
    }
    @Override public String getDisplayName() { return "Correct M3 regex ASCII numeric admission"; }
    @Override public String getDescription() {
        return "Produces exact reviewed parser, test and component mapping candidates while "
                + "preserving existing fallback dialects and unchanged dependency owners.";
    }
}
