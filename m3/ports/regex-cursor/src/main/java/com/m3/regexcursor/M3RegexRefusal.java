// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.regexcursor;

/**
 * Compile-time refusal: the pattern is outside the subset the cursor engines prove equal to the
 * stock engine. Cold path only; carries no stack trace.
 */
final class M3RegexRefusal extends RuntimeException {
    private static final long serialVersionUID = 1L;

    M3RegexRefusal(String reason) {
        super(reason, null, false, false);
    }
}
