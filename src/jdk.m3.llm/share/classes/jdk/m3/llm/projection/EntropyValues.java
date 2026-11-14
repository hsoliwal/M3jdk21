// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.projection;

import jdk.m3.llm.internal.Sha256;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/** Strict UTF-8 encoding and length-framed identities; no delimiter ambiguity. */
final class EntropyValues {
    private EntropyValues() {}

    static byte[] utf8(String value) {
        Objects.requireNonNull(value, "text");
        try {
            ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder().encode(CharBuffer.wrap(value));
            byte[] bytes = new byte[encoded.remaining()];
            encoded.get(bytes);
            return bytes;
        } catch (CharacterCodingException failure) {
            throw new IllegalArgumentException("INVALID_UTF16", failure);
        }
    }

    static String text(String value, int maximumBytes, boolean empty) {
        byte[] bytes = utf8(value);
        if (bytes.length > maximumBytes || (!empty && value.isBlank()) || value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("TEXT_BOUND");
        }
        return value;
    }

    static int compare(String left, String right) {
        return Arrays.compareUnsigned(utf8(left), utf8(right));
    }

    static String digest(String value) {
        return Sha256.digest(utf8(value));
    }

    static String framed(String... values) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(buffer)) {
                for (String value : values) {
                    byte[] bytes = utf8(value);
                    out.writeInt(bytes.length);
                    out.write(bytes);
                }
            }
            return Sha256.digest(buffer.toByteArray());
        } catch (IOException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
