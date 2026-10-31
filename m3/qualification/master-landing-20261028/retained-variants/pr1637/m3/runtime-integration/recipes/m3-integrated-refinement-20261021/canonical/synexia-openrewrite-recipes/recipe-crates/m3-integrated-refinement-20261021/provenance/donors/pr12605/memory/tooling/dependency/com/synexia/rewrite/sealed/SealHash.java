// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite.sealed;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Length-framed identities and strict UTF-8 at all snapshot boundaries. */
public final class SealHash {
    private SealHash() { }
    public static String bytes(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    public static String text(String text) { return bytes(utf8(text)); }
    public static String frame(String... fields) {
        StringBuilder b = new StringBuilder("SEALED-FRAME/1\n");
        for (String field : fields) b.append(field.length()).append(':').append(field);
        return text(b.toString());
    }
    public static String require(String value) {
        if (value == null || !value.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("INVALID_SHA256");
        return value;
    }
    public static byte[] utf8(String text) {
        try {
            ByteBuffer data = StandardCharsets.UTF_8.newEncoder().encode(CharBuffer.wrap(text));
            byte[] b = new byte[data.remaining()]; data.get(b); return b;
        } catch (CharacterCodingException invalid) { throw new IllegalArgumentException("INVALID_UTF8", invalid); }
    }
    public static String decode(byte[] bytes) {
        try { return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString(); }
        catch (CharacterCodingException invalid) { throw new IllegalArgumentException("INVALID_UTF8", invalid); }
    }
}
