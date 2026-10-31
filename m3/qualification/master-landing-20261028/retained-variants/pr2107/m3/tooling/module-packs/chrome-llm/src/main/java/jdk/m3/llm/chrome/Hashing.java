/*
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0
 * Derived from Synexia 2ab439480ecb0643615e85b42def954dbf3b5627:
 * cpullm-opdag/java/chrome-ai-mechanical/src/main/java/jdk/internal/m3/cpullm/chrome/Hashing.java
 * Adapted for the M3JDK21-owned namespace and qualified summary contracts.
 */
package jdk.m3.llm.chrome;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/** Deterministic SHA-256 helpers with length-prefixed domain separation. */
public final class Hashing {
    private Hashing() {}

    public static String sha256Hex(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public static String hash(String domain, Object... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            update(digest, domain);
            for (Object value : values) update(digest, String.valueOf(value));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public static String hashList(String domain, List<String> values) {
        return hash(domain, values.size(), String.join("\u001f", values));
    }

    public static boolean isSha256(String value) {
        return value != null && value.matches("[0-9a-f]{64}");
    }

    /** Reject malformed UTF-16 rather than hashing replacement bytes as source identity. */
    static byte[] utf8(String value) {
        try {
            ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(value));
            byte[] bytes = new byte[encoded.remaining()];
            encoded.get(bytes);
            return bytes;
        } catch (CharacterCodingException invalid) {
            throw new IllegalArgumentException("malformed UTF-16 source", invalid);
        }
    }

    private static void update(MessageDigest digest, String value) {
        byte[] bytes = utf8(value);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }
}
