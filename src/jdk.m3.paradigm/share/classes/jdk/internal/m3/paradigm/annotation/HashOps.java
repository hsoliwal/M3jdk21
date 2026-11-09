/* ******************************************************************************
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
/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jdk.internal.m3.paradigm.annotation;

import jdk.internal.m3.collections.PlaceholderLogger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Objects;
/**
 * HashOps - Common hash computation utilities.
 *
 * <p>
 * Centralized hash helpers for SHA-256, quick hash extraction, and byte-to-hex
 * conversion used across the module.
 *
 * <h2>Usage Examples</h2>
 *
 * <pre>{@code
 *
 * // Compute SHA-256 hash
 * String hash = HashOps.sha256("content");
 * // Result: "sha256:a1b2c3d4..."
 *
 * // Extract quick hash from full hash
 * String quick = HashOps.extractQuickHash("sha256:a1b2c3d4e5f6g7h8i9j0...");
 * // Result: "a1b2c3d4e5f6g7h8"
 *
 * // Convert bytes to hex
 * byte[] bytes = "hello".getBytes();
 * String hex = HashOps.bytesToHex(bytes);
 * }</pre>
 *
 * @since m3-anop 1.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/utils/HashOps.java}
 * blob {@code 4b688ee945b22d07f124f074614aedbce5dd4ae8} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class HashOps {
    private static final PlaceholderLogger log = PlaceholderLogger.of(HashOps.class);


    private static final String SHA256_ALGORITHM = "SHA-256";
    private static final String SHA256_PREFIX = "sha256:";
    private static final int QUICK_HASH_LENGTH = 16;
    private static final int PREFIX_LENGTH = 7; // "sha256:".length()
    private static final int SHA256_HEX_LENGTH = 64;
    private static final int MD5_HEX_LENGTH = 16;
    private static final long FNV1A_OFFSET_BASIS = 0xcbf29ce484222325L;
    private static final long FNV1A_PRIME = 0x100000001b3L;

    // Verified at class load to ensure SHA-256 is available per Java SE spec
    private static final MessageDigest SHA256_PROTOTYPE;

    static {
        try {
            SHA256_PROTOTYPE = MessageDigest.getInstance(SHA256_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            ExceptionInInitializerError error =
                    new ExceptionInInitializerError("SHA-256 algorithm must be available per Java SE spec");
            error.initCause(e);
            throw error;
        }
    }

    private HashOps() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Compute SHA-256 hash of content.
     *
     * @param content
     *            the content to hash
     * @return hash string in format "sha256:{64-char-hex}"
     */
    public static String sha256(String content) {
        Objects.requireNonNull(content, "content");
        // Clone prototype for thread safety - NoSuchAlgorithmException impossible per
        // static init
        try {
            MessageDigest digest = (MessageDigest) SHA256_PROTOTYPE.clone();
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return SHA256_PREFIX + bytesToHex(hash);
        } catch (CloneNotSupportedException e) {
            // MessageDigest.clone() is supported - this should never happen
            throw new AssertionError("MessageDigest clone must be supported", e);
        }
    }

    /**
     * Verify that content matches hash.
     *
     * <p>
     * Uses constant-time comparison to prevent timing attacks.
     *
     * @param content
     *            the content to verify
     * @param expectedHash
     *            the expected hash
     * @return true if hash matches
     */
    public static boolean verifyHash(String content, String expectedHash) {
        String actualHash = sha256(content);
        // Constant-time comparison to prevent timing attacks
        return MessageDigest.isEqual(
                actualHash.getBytes(StandardCharsets.UTF_8), expectedHash.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Convert byte array to hexadecimal string.
     *
     * @param bytes
     *            the bytes to convert
     * @return hex string (lowercase)
     */
    public static String bytesToHex(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        var hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(String.format(Locale.ROOT, "%02x", b));
        }
        return hex.toString();
    }

    /**
     * Extract quick hash from full SHA-256 hash.
     *
     * <p>
     * Returns the first 16 hex characters after the "sha256:" prefix.
     *
     * @param fullHash
     *            the full hash (format: "sha256:{64-char-hex}")
     * @return first 16 chars of hash, or empty string if invalid
     */
    public static String extractQuickHash(String fullHash) {
        if (fullHash == null || fullHash.length() < PREFIX_LENGTH + QUICK_HASH_LENGTH) {
            return "";
        }
        return fullHash.substring(PREFIX_LENGTH, PREFIX_LENGTH + QUICK_HASH_LENGTH);
    }

    /**
     * Check if hash is in valid format (sha256:{64-char-hex}).
     *
     * @param hash
     *            the hash to validate
     * @return true if hash is valid
     */
    public static boolean isValidHash(String hash) {
        if (hash == null || !hash.startsWith(SHA256_PREFIX)) {
            return false;
        }
        String hexPart = hash.substring(PREFIX_LENGTH);
        if (hexPart.length() != SHA256_HEX_LENGTH) {
            return false;
        }
        try {
            Long.parseLong(hexPart, MD5_HEX_LENGTH);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Get the SHA-256 prefix used in hash strings.
     *
     * @return the prefix
     */
    public static String getSha256Prefix() {
        return SHA256_PREFIX;
    }

    /**
     * Compute FNV-1a hash of content (used for quick hash).
     *
     * <p>
     * Alternative to xxHash64 using standard Java libraries. Returns 16-char hex
     * string.
     *
     * @param content
     *            the content
     * @return quick hash in format "xxhash64:{16-char-hex}"
     */
    public static String fnv1aHash(String content) {
        Objects.requireNonNull(content, "content");
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        long hash = fnv1aHashBytes(bytes);
        return "xxhash64:" + Long.toHexString(hash);
    }

    private static long fnv1aHashBytes(byte[] bytes) {
        long hash = FNV1A_OFFSET_BASIS; // FNV-1a offset
        for (byte b : bytes) {
            hash ^= b;
            hash *= FNV1A_PRIME; // FNV-1a prime
        }
        return hash;
    }
}

