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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Portable content address for an explicitly canonical immutable value.
 *
 * @param typeName canonical Java type name
 * @param schema codec schema/version
 * @param sha256 SHA-256 over type, schema and canonical bytes
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextImmutableKey.java}
 * blob {@code 7aa1979fbcc5099ba540bb8e5ed81bc4985c773d} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public record ImmutableKey(
        String typeName,
        String schema,
        String sha256)
        implements Comparable<ImmutableKey> {

    private static final HexFormat HEX = HexFormat.of();

    public ImmutableKey {
        typeName = require(typeName, "typeName");
        schema = require(schema, "schema");
        sha256 = require(sha256, "sha256");
        if (sha256.length() != 64) {
            throw new IllegalArgumentException(
                    "sha256 must contain 64 hexadecimal characters");
        }
    }

    static <T> ImmutableKey of(
            ImmutableCodec<T> codec,
            byte[] canonicalBytes) {
        Objects.requireNonNull(codec, "codec");
        Objects.requireNonNull(
                canonicalBytes,
                "canonicalBytes");
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
        update(digest, codec.type().getName());
        digest.update((byte) 0);
        update(digest, codec.schema());
        digest.update((byte) 0);
        digest.update(canonicalBytes);
        return new ImmutableKey(
                codec.type().getName(),
                codec.schema(),
                HEX.formatHex(digest.digest()));
    }

    public String id() {
        return typeName + "|" + schema + "|" + sha256;
    }

    @Override
    public int compareTo(ImmutableKey other) {
        Objects.requireNonNull(other, "other");
        int byType = typeName.compareTo(other.typeName);
        if (byType != 0) {
            return byType;
        }
        int bySchema = schema.compareTo(other.schema);
        return bySchema != 0
                ? bySchema
                : sha256.compareTo(other.sha256);
    }

    private static void update(
            MessageDigest digest,
            String value) {
        digest.update(
                value.getBytes(
                        StandardCharsets.UTF_8));
    }

    private static String require(
            String value,
            String name) {
        String normalized =
                Objects.requireNonNull(
                        value,
                        name).strip();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    name + " must not be blank");
        }
        return normalized;
    }
}
