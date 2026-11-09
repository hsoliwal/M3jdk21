/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file).
 */
package java.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Objects;

/**
 * An opt-in SHA-256 content capability for objects with explicitly defined bytes.
 *
 * <p>{@code Object.toAdapter(SHA256Adapter.class)} supports {@code String}
 * (UTF-8 with the standard replacement policy), {@code byte[]} (all bytes),
 * and {@link ByteBuffer} (the range selected by position and limit when
 * adapted). Arbitrary objects have no canonical byte representation and
 * therefore return {@code null}. A subclass can override {@code toAdapter}
 * to supply a domain-specific encoding without exposing private fields.
 *
 * <p>An adapter retains the original content owner, not an array copy or a
 * digest cache. Every {@link #digest()} reads the current bytes and returns
 * a new 32-byte result. A ByteBuffer adapter fixes its range at adaptation
 * while preserving its caller's position, limit and mark. Mutating sources
 * require external synchronization for a coherent digest; no per-object state,
 * native pointer or listener is introduced.
 *
 * <p>This capability delegates to the JDK SHA-256 provider, including
 * available HotSpot intrinsics, rather than replacing a cryptographic
 * implementation with a separate unverified native algorithm.
 *
 * @since 21
 */
@FunctionalInterface
public interface SHA256Adapter {
    /** SHA-256 digest size in bytes. */
    int DIGEST_BYTES = 32;

    /**
     * Returns a fresh SHA-256 digest for the currently observed content.
     *
     * @return a new array of {@value #DIGEST_BYTES} digest bytes
     */
    byte[] digest();

    /**
     * Returns a lower-case hexadecimal SHA-256 digest.
     *
     * @return the digest as lower-case hexadecimal text
     */
    default String hexDigest() {
        return HexFormat.of().formatHex(digest());
    }

    /**
     * Compares against expected digest bytes using the JDK's digest comparator.
     *
     * @param expected the expected digest bytes
     * @return {@code true} if the current digest equals {@code expected}
     */
    default boolean matches(byte[] expected) {
        Objects.requireNonNull(expected, "expected");
        return MessageDigest.isEqual(digest(), expected);
    }

    /**
     * Returns a typed SHA-256 capability when source bytes are defined,
     * otherwise {@code null}. No arbitrary object serialization is attempted.
     *
     * @param source the content owner to adapt
     * @return the adapter, or {@code null} if {@code source} has no defined bytes
     */
    static SHA256Adapter of(Object source) {
        Objects.requireNonNull(source, "source");
        if (source instanceof SHA256Adapter adapter) {
            return adapter;
        }
        if (source instanceof byte[] bytes) {
            return () -> newDigest().digest(bytes);
        }
        if (source instanceof String text) {
            return () -> newDigest().digest(text.getBytes(StandardCharsets.UTF_8));
        }
        if (source instanceof ByteBuffer buffer) {
            // Fix coordinates, but never advance the supplied buffer's cursor.
            ByteBuffer range = buffer.slice();
            return () -> {
                MessageDigest digest = newDigest();
                digest.update(range.duplicate());
                return digest.digest();
            };
        }
        return null;
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 provider unavailable", impossible);
        }
    }
}
