// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Stream observer that publishes one immutable fact after the primary file stream reaches EOF.
 *
 * <p>Parser/index owners may implement this interface directly and keep their own incremental
 * state. M3File sees only the final fixed-width receipt and never owns parser-specific objects.</p>
 */
public interface M3FileFactObserver extends M3StreamObserver {
    String factName();

    M3FileFact fact();

    /** SHA-256 of the exact bytes observed by this layer. Useful as a reusable baseline observer. */
    static M3FileFactObserver sha256(String name) {
        String checked = new M3FileFact(name, new byte[M3FileFact.SHA256_BYTES]).name();
        return new M3FileFactObserver() {
            private final MessageDigest digest = newDigest();
            private byte[] completed;

            @Override public String factName() {
                return checked;
            }

            @Override public void onBytes(byte[] bytes, int offset, int length) {
                if (completed != null) throw new IllegalStateException("fact observer completed");
                Objects.checkFromIndexSize(offset, length, Objects.requireNonNull(bytes, "bytes").length);
                digest.update(bytes, offset, length);
            }

            @Override public void onEof() {
                if (completed == null) completed = digest.digest();
            }

            @Override public M3FileFact fact() {
                if (completed == null) {
                    throw new IllegalStateException("fact is unavailable before EOF");
                }
                return new M3FileFact(checked, completed);
            }
        };
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }
}
