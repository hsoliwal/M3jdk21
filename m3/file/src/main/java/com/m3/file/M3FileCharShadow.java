// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.Objects;

/**
 * Char layer of a shadow stack, fed by {@link M3FileUtf8Shadow} while the main byte stream moves.
 *
 * <p>Layered parsing hooks in here: the token shadow, or an M3AST builder supplied by {@code m3/ast}
 * (no dependency from {@code m3/file}). One abstract method; {@link #finish()} runs once at the end.</p>
 */
@FunctionalInterface
public interface M3FileCharShadow {
    void update(char[] chars, int offset, int length);

    default void finish() {
    }

    default M3FileCharShadow andThen(M3FileCharShadow next) {
        Objects.requireNonNull(next, "next");
        M3FileCharShadow first = this;
        return new M3FileCharShadow() {
            @Override
            public void update(char[] chars, int offset, int length) {
                first.update(chars, offset, length);
                next.update(chars, offset, length);
            }

            @Override
            public void finish() {
                first.finish();
                next.finish();
            }
        };
    }

    /** Whitespace-insensitive token-stream hash and token count. */
    static M3FileTokenShadow tokens() {
        return new M3FileTokenShadow();
    }

    /** One-read paragraph structure, retained as bounded primitive offset/index/mask lanes. */
    static M3Grammar grammar(int maxParagraphs) {
        return M3Grammar.paragraphs(maxParagraphs);
    }

    /** Collects the decoded text; for small files and tests. */
    static M3FileTextShadow text() {
        return new M3FileTextShadow();
    }
}
