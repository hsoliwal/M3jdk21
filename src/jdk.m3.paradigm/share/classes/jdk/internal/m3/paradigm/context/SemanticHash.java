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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Deterministic length-prefixed SHA-256 and canonical token helpers.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/cognition/SemanticHash.java}
 * blob {@code 81c553f3dfd8a2f9060d3a1cbac76a79a7199631} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class SemanticHash {
    private SemanticHash() {}

    public static String sha256(String domain, Iterable<String> fields) {
        Objects.requireNonNull(domain, "domain");
        Objects.requireNonNull(fields, "fields");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            put(digest, domain);
            for (String field : fields) put(digest, Objects.requireNonNull(field, "field"));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    public static String sha256(String domain, String... fields) {
        return sha256(domain, java.util.List.of(fields));
    }

    public static String requireId(String value, String name) {
        if (value == null || !value.matches("[A-Za-z0-9][A-Za-z0-9_.:-]{0,127}")) {
            throw new IllegalArgumentException(name + " is not a valid identifier");
        }
        return value;
    }

    public static String requireSha(String value, String name) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(name + " must be a lowercase SHA-256 digest");
        }
        return value;
    }

    public static String normalizeToken(String value) {
        Objects.requireNonNull(value, "value");
        String normalized = value.strip().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (normalized.isBlank()) throw new IllegalArgumentException("token normalizes to blank");
        return normalized;
    }

    public static SortedSet<String> canonicalTokens(Collection<String> values) {
        Objects.requireNonNull(values, "values");
        TreeSet<String> result = new TreeSet<>();
        values.forEach(value -> result.add(normalizeToken(value)));
        return Collections.unmodifiableSortedSet(result);
    }

    private static void put(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }
}
