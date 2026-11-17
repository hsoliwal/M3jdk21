// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.regexcursor;

/**
 * Compile-time refusal: the pattern is outside the subset the cursor engines prove equal to the
 * stock engine. Cold path only; carries no stack trace.
 */
final class RegexRefusal extends RuntimeException {
    private static final long serialVersionUID = 1L;

    RegexRefusal(String reason) {
        super(reason, null, false, false);
    }
}
