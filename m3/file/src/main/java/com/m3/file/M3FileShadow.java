// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.Objects;

/**
 * Shadow of a main byte stream: sees every byte as it is read or written, with no second I/O.
 *
 * <p>Pattern of {@code java.security.DigestInputStream} and {@code java.util.zip.CheckedInputStream},
 * generalised to any number of layers over one pass: raw hashes, a line index, a UTF-8 char layer that
 * feeds {@link M3FileCharShadow}s (tokens, an M3AST builder supplied by {@code m3/ast}). One abstract
 * method; {@link #finish()} runs once when the main stream ends (EOF on input, close on output).</p>
 */
@FunctionalInterface
public interface M3FileShadow {
    void update(byte[] bytes, int offset, int length);

    /** The main stream ended; results are complete from here on. */
    default void finish() {
    }

    /** This shadow, then {@code next}, for every block. */
    default M3FileShadow andThen(M3FileShadow next) {
        Objects.requireNonNull(next, "next");
        M3FileShadow first = this;
        return new M3FileShadow() {
            @Override
            public void update(byte[] bytes, int offset, int length) {
                first.update(bytes, offset, length);
                next.update(bytes, offset, length);
            }

            @Override
            public void finish() {
                first.finish();
                next.finish();
            }
        };
    }

    /** SHA-256 of the raw bytes (#1567 {@code Fingerprint} word order). */
    static M3FileDigestShadow sha256() {
        return new M3FileDigestShadow("SHA-256");
    }

    static M3FileDigestShadow digest(String algorithm) {
        return new M3FileDigestShadow(algorithm);
    }

    /** CRC32C (intrinsic) of the raw bytes: the fast per-entry hash. */
    static M3FileCrcShadow crc32c() {
        return new M3FileCrcShadow();
    }

    /** Byte offsets of line starts (LF, CR, CRLF), valid for ASCII-compatible encodings. */
    static M3FileLineShadow lines() {
        return new M3FileLineShadow();
    }

    /** POSIX {@code wc} counts (bytes, newlines, words). */
    static M3FileWcShadow wc() {
        return new M3FileWcShadow();
    }

    /** UTF-8 char layer feeding {@code layers}; malformed input becomes U+FFFD as in {@code new String(bytes, UTF_8)}. */
    static M3FileUtf8Shadow utf8(M3FileCharShadow... layers) {
        return new M3FileUtf8Shadow(layers);
    }
}
