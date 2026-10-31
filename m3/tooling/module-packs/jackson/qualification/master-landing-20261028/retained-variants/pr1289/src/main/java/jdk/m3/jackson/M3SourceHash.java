// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
// Adapted from Synexia M3FileSourceFacts.sourceSha256; compiler-context facts are excluded.
package jdk.m3.jackson;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Exact source identity; original bytes and decoded code units have separate domains. */
final class M3SourceHash {
    private static final byte[] DOMAIN =
            "M3_FILE_SOURCE_UTF16_V1".getBytes(StandardCharsets.US_ASCII);

    private M3SourceHash() { }

    static String utf16(String source) {
        MessageDigest digest = sha256();
        digest.update(DOMAIN);
        int size = source.length();
        digest.update((byte) (size >>> 24));
        digest.update((byte) (size >>> 16));
        digest.update((byte) (size >>> 8));
        digest.update((byte) size);
        for (int i = 0; i < size; i++) {
            char unit = source.charAt(i);
            digest.update((byte) (unit >>> 8));
            digest.update((byte) unit);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    static String bytes(byte[] bytes) {
        return HexFormat.of().formatHex(sha256().digest(bytes));
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }
}
