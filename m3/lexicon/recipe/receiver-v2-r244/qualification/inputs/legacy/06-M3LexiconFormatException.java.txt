// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.lexicon;

import java.io.IOException;

/** Refusal raised by an M3Lexicons image verifier; an image is never partially admitted. */
public final class M3LexiconFormatException extends IOException {
    private static final long serialVersionUID = 1L;

    /** @param reason fixed refusal text naming the violated rule */
    public M3LexiconFormatException(String reason) {
        super(reason);
    }

    static void require(boolean valid, String reason) throws M3LexiconFormatException {
        if (!valid) {
            throw new M3LexiconFormatException(reason);
        }
    }
}
