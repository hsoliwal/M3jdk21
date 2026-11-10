// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.util.Objects;

/**
 * Prepared UTF-16 literal search over the running JDK String facade.
 *
 * <p>The compiled plan owns only the literal and primitive failure offsets. It never
 * owns or copies a source text payload. On M3JDK 21 the String facade remains backed
 * by the canonical M3 String owner; search coordinates are ordinary UTF-16 indexes,
 * exactly matching {@link String#contains(CharSequence)} semantics for valid Unicode.</p>
 */
final class NativeUtf16Literal {
    private NativeUtf16Literal() { }

    static Compiled compile(String literal) {
        Canonical.validUnicode(literal);
        return new Compiled(literal);
    }

    static final class Compiled {
        private final String literal;
        private final int[] failure;

        private Compiled(String literal) {
            this.literal = Objects.requireNonNull(literal, "literal");
            this.failure = failure(literal);
        }

        boolean contains(String source) {
            Canonical.validUnicode(source);
            if (literal.isEmpty()) return true;
            for (int at = 0, matched = 0; at < source.length(); at++) {
                char value = source.charAt(at);
                while (matched > 0 && value != literal.charAt(matched)) {
                    matched = failure[matched - 1];
                }
                if (value == literal.charAt(matched) && ++matched == literal.length()) {
                    return true;
                }
            }
            return false;
        }

        private static int[] failure(String literal) {
            int[] result = new int[literal.length()];
            for (int at = 1, prefix = 0; at < literal.length(); at++) {
                char value = literal.charAt(at);
                while (prefix > 0 && value != literal.charAt(prefix)) {
                    prefix = result[prefix - 1];
                }
                if (value == literal.charAt(prefix)) prefix++;
                result[at] = prefix;
            }
            return result;
        }
    }
}
