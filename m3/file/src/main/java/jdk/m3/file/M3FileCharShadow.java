// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.file;
//FLLDCIM: RESTORE - source m3/qualification/master-landing-20261028/retained-variants/pr1619/m3/file/src/main/java/jdk/m3/file/M3FileCharShadow.java blob 4a4f35bc5446ce412b7ded8e29e170eeee4c0541 (retained PR #1619 head; parent 0cd0944136c1 blob a3391903ddb43d16ae82a5f43781b7237e5b6806 https://github.com/hsoliwal/M3jdk21/blob/0cd0944136c1/m3/file/src/main/java/com/m3/file/M3FileCharShadow.java); recipe m3/file/qualification/retained-port-20261107 RESTORE_EXACT

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
