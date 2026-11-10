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
package jdk.internal.m3.paradigm.contract;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/** Canonical hashing/normalization helpers for the executable pattern layer.
 * @m3.pattern synexia:bop
 * @m3.algorithm Provides local UTF8 SHA256 text and trim/regex/sort normalization helpers; these deterministic String transformations do not authenticate inputs or establish collision-free serialization.
 * @m3.dataStructures MessageDigest, UTF8 bytes, HexFormat, String regexes and collection streams.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/patterns/PatternHash.java}
 * blob {@code b499e9718ee0e05289d708ac3147958295254abf} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PatternHash {
    /**
     * PatternHash declared constructor.
     * @m3.pattern synexia:bop
     * @m3.algorithm Executes an empty private constructor body; all declared helpers are static.
     * @m3.dataStructures No arguments or instance fields.
     */
    private PatternHash() {}

    /**
     * UTF8 SHA256 text digest.
     * @m3.pattern synexia:bop
     * @m3.algorithm Creates a SHA-256 MessageDigest, converts null to empty String with Objects.toString, hashes UTF8 bytes and returns lowercase hexadecimal; unavailable SHA-256 is wrapped as IllegalStateException. It performs no domain-length framing.
     * @m3.dataStructures String reference, byte array, MessageDigest and HexFormat-produced String.
     */
    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = Objects.toString(value, "").getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    /**
     * Trim and Java-regex whitespace normalization.
     * @m3.pattern synexia:bop
     * @m3.algorithm Maps null to empty String; otherwise applies String.trim then replaceAll with regex whitespace-plus and a single space. This is Java default regex whitespace and trim behavior rather than Unicode-wide normalization.
     * @m3.dataStructures Input String, regex pattern text and resulting String.
     */
    public static String text(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    /**
     * Distinct normalized String collection copy.
     * @m3.pattern synexia:bop
     * @m3.algorithm Maps null collection to List.of; otherwise filters null elements, normalizes text, filters blank results, removes exact duplicates, sorts naturally and collects with Stream.toList. The returned list is unmodifiable and natural String order is retained.
     * @m3.dataStructures Collection of String, stream pipeline, normalized values and immutable List.
     */
    public static List<String> sorted(Collection<String> values) {
        if (values == null) return List.of();
        return values.stream()
                .filter(Objects::nonNull)
                .map(PatternHash::text)
                .filter(value -> !value.isBlank())
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
    }
}
