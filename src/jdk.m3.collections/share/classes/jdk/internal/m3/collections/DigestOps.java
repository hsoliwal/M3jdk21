/* *****************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Dependency-light cryptographic digest operations shared across Synexia modules.
 *
 * <p>This class intentionally contains only JDK-backed digest mechanics. Heavyweight hashing,
 * Spark/DataFrame helpers, non-cryptographic hashes, and application-specific canonicalization
 * remain outside this owner. The narrow surface lets Java 21 applications such as TruckMate use
 * one SHA-256 implementation without pulling the broader {@code HashOps} dependency graph.
 *
 * <p>Callers own semantic canonicalization. A digest proves byte identity; it does not establish
 * business equivalence, ordering, authorization, or source authority.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/utils/DigestOps.java}
 * blob {@code 8d1e90e012037bd11e9e2a994d7cbaefba00927e} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common non-language batch b28 (canonical, utils, semantic, convergence, io, extracted, validation, relationbits) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class DigestOps {

    private static final String SHA_256 = "SHA-256";
    private static final int BUFFER_BYTES = 16 * 1024;
    private static final HexFormat HEX = HexFormat.of();

    private DigestOps() {
        throw new AssertionError("No instances");
    }

    /** Returns the SHA-256 digest of the supplied bytes. */
    public static byte[] sha256(byte[] value) {
        Objects.requireNonNull(value, "value");
        return newDigest().digest(value);
    }

    /** Returns the lowercase hexadecimal SHA-256 digest of the supplied bytes. */
    public static String sha256Hex(byte[] value) {
        return HEX.formatHex(sha256(value));
    }

    /** Returns the SHA-256 digest of the UTF-8 encoding of the supplied text. */
    public static byte[] sha256Utf8(String value) {
        Objects.requireNonNull(value, "value");
        return sha256(value.getBytes(StandardCharsets.UTF_8));
    }

    /** Returns the lowercase hexadecimal SHA-256 digest of the supplied UTF-8 text. */
    public static String sha256HexUtf8(String value) {
        return HEX.formatHex(sha256Utf8(value));
    }

    /**
     * Streams all bytes from {@code input} into SHA-256 without closing the caller-owned stream.
     */
    public static byte[] sha256(InputStream input) throws IOException {
        Objects.requireNonNull(input, "input");
        MessageDigest digest = newDigest();
        byte[] buffer = new byte[BUFFER_BYTES];
        int read;
        while ((read = input.read(buffer)) != -1) {
            if (read > 0) {
                digest.update(buffer, 0, read);
            }
        }
        return digest.digest();
    }

    /**
     * Streams all bytes from {@code input} into SHA-256 and returns lowercase hexadecimal output.
     * The stream remains open.
     */
    public static String sha256Hex(InputStream input) throws IOException {
        return HEX.formatHex(sha256(input));
    }

    /** Returns the SHA-256 digest of the exact bytes stored at {@code path}. */
    public static byte[] sha256(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        try (InputStream input = Files.newInputStream(path)) {
            return sha256(input);
        }
    }

    /** Returns the lowercase hexadecimal SHA-256 digest of the exact file bytes at {@code path}. */
    public static String sha256Hex(Path path) throws IOException {
        return HEX.formatHex(sha256(path));
    }

    /** Returns {@code true} only for a lowercase or uppercase 64-character hexadecimal SHA-256. */
    public static boolean isSha256Hex(String value) {
        if (value == null || value.length() != 64) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean decimal = c >= '0' && c <= '9';
            boolean lower = c >= 'a' && c <= 'f';
            boolean upper = c >= 'A' && c <= 'F';
            if (!decimal && !lower && !upper) {
                return false;
            }
        }
        return true;
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance(SHA_256);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JDK does not provide SHA-256", e);
        }
    }
}
