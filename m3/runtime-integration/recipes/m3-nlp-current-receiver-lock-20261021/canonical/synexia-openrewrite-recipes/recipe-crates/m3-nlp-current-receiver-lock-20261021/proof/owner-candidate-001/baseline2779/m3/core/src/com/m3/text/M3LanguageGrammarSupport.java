/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.util.Locale;
import java.util.Set;

/**
 * Target-side immutable projection of Synexia Language.hasGrammarSupport().
 *
 * <p>This owns only the support signal. Grammar rules and rule data remain
 * outside the M3 string runtime and remain governed by their source provider.</p>
 */
public final class M3LanguageGrammarSupport {
    public static final String SOURCE_ID = "translate.rows";
    public static final String TARGET_FIELD = "translation_grammar_supported";
    private static final Set<String> SUPPORTED = Set.of(
            "ca", "de", "el", "en", "es", "fr", "it", "nl",
            "pl", "pt", "ro", "ru", "sk", "sl", "ta", "uk");

    private M3LanguageGrammarSupport() { }

    /** Return the source-compatible grammar support signal for an ISO code. */
    public static boolean supports(String languageCode) {
        return languageCode != null
                && SUPPORTED.contains(languageCode.trim().toLowerCase(Locale.ROOT));
    }

    /** Stable immutable supported-code view for exporters and tests. */
    public static Set<String> supportedCodes() {
        return SUPPORTED;
    }
}
