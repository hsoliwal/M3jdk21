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
package jdk.internal.m3.paradigm.context;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Bounded, deterministic UTF-8 cell encoding. Never deserializes Java objects.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/memory/MementoCodec.java}
 * blob {@code 7f27d51235c57fed9fcb0f3b13b22449fe522b0f} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MementoCodec {
    /** Maximum number of cells in one state. */
    public static final int MAX_CELLS = 10_000;
    /** Maximum encoded state size. */
    public static final int MAX_BYTES = 16 * 1024 * 1024;
    /** Maximum UTF-8 size of a single key or value. */
    public static final int MAX_TEXT_BYTES = 1024 * 1024;
    private static final int MAGIC = 0x434d3031;

    private MementoCodec() { }

    /** Defensive, sorted, immutable copy; null keys and values are rejected. */
    public static Map<String, String> copy(Map<String, String> cells) {
        Objects.requireNonNull(cells, "cells");
        if (cells.size() > MAX_CELLS) {
            throw new IllegalArgumentException("Too many memento cells");
        }
        TreeMap<String, String> result = new TreeMap<>();
        cells.forEach((key, value) -> result.put(
                Objects.requireNonNull(key, "cell key"),
                Objects.requireNonNull(value, "cell value")));
        return Collections.unmodifiableMap(result);
    }

    /** Encodes a canonical sorted map, preserving case and all string content. */
    public static byte[] encode(Map<String, String> cells) {
        Map<String, String> sorted = copy(cells);
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(MAGIC);
            out.writeInt(sorted.size());
            // Ordered binary I/O is deliberately sequential.
            for (Map.Entry<String, String> entry : sorted.entrySet()) {
                writeText(out, entry.getKey());
                writeText(out, entry.getValue());
                if (bytes.size() > MAX_BYTES) {
                    throw new IllegalArgumentException("Memento exceeds byte limit");
                }
            }
            out.flush();
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("In-memory encoding failed", exception);
        }
    }

    /** Decodes only this format, rejecting duplicate keys, trailing bytes and bad UTF-8. */
    public static Map<String, String> decode(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        if (bytes.length > MAX_BYTES) {
            throw new IllegalArgumentException("Memento exceeds byte limit");
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            if (in.readInt() != MAGIC) {
                throw new IllegalArgumentException("Unsupported memento encoding");
            }
            int count = in.readInt();
            if (count < 0 || count > MAX_CELLS) {
                throw new IllegalArgumentException("Invalid cell count");
            }
            TreeMap<String, String> result = new TreeMap<>();
            for (int index = 0; index < count; index++) {
                String key = readText(in);
                if (result.putIfAbsent(key, readText(in)) != null) {
                    throw new IllegalArgumentException("Duplicate cell key");
                }
            }
            if (in.read() != -1) {
                throw new IllegalArgumentException("Trailing memento bytes");
            }
            return Collections.unmodifiableMap(result);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Malformed memento", exception);
        }
    }

    /** SHA-256 of the canonical state; an integrity check, not authentication. */
    public static String digest(Map<String, String> cells) {
        return digest(encode(cells));
    }

    /** SHA-256 of bytes. */
    public static String digest(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    static void writeText(DataOutputStream out, String text) throws IOException {
        Objects.requireNonNull(text, "text");
        if (text.length() > MAX_TEXT_BYTES) {
            throw new IllegalArgumentException("Text exceeds byte limit");
        }
        byte[] encoded;
        try {
            ByteBuffer buffer = StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(java.nio.CharBuffer.wrap(Objects.requireNonNull(text, "text")));
            encoded = new byte[buffer.remaining()];
            buffer.get(encoded);
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("Unpaired UTF-16 surrogate", exception);
        }
        if (encoded.length > MAX_TEXT_BYTES) {
            throw new IllegalArgumentException("Text exceeds byte limit");
        }
        out.writeInt(encoded.length);
        out.write(encoded);
    }

    static String readText(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length < 0 || length > MAX_TEXT_BYTES || length > in.available()) {
            throw new IllegalArgumentException("Invalid text length");
        }
        byte[] bytes = in.readNBytes(length);
        return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString();
    }
}
