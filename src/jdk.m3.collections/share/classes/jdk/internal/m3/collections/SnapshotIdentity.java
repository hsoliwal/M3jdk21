/*******************************************************************************
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

import java.util.Locale;
import java.util.Objects;

/**
 * Immutable content identity for an externally sourced snapshot.
 *
 * <p>This record binds a logical source, an upstream version/sequence token, a SHA-256 content
 * digest and the number of projected items. It intentionally contains no timestamp. Wall-clock
 * time may be recorded separately as audit metadata, but must not silently become business
 * ordering authority.</p>
 *
 * @param sourceId stable logical source identifier
 * @param sourceVersion upstream version, sequence, cursor or other source-owned identity token
 * @param contentSha256 SHA-256 of the exact canonical snapshot bytes/projection
 * @param itemCount number of projected items represented by the snapshot
 * @since 1.0.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/io/SnapshotIdentity.java}
 * blob {@code fa8c5b410ef8da4193cdc290ee7c90b6526663b1} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common non-language batch b28 (canonical, utils, semantic, convergence, io, extracted, validation, relationbits) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public record SnapshotIdentity(
        String sourceId,
        String sourceVersion,
        String contentSha256,
        long itemCount) {

    /**
     * Creates a validated canonical snapshot identity.
     *
     * @throws NullPointerException if a string field is null
     * @throws IllegalArgumentException if an identifier is blank, the digest is malformed or the
     *         item count is negative
     */
    public SnapshotIdentity {
        sourceId = requireText(sourceId, "sourceId");
        sourceVersion = requireText(sourceVersion, "sourceVersion");
        contentSha256 = requireSha256(contentSha256);
        if (itemCount < 0) {
            throw new IllegalArgumentException("itemCount must be >= 0");
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }

    private static String requireSha256(String value) {
        Objects.requireNonNull(value, "contentSha256");
        if (value.length() != 64) {
            throw new IllegalArgumentException("contentSha256 must contain 64 hexadecimal characters");
        }
        for (int index = 0; index < value.length(); index++) {
            char c = value.charAt(index);
            boolean hex = (c >= '0' && c <= '9')
                    || (c >= 'a' && c <= 'f')
                    || (c >= 'A' && c <= 'F');
            if (!hex) {
                throw new IllegalArgumentException("contentSha256 must be hexadecimal");
            }
        }
        return value.toLowerCase(Locale.ROOT);
    }
}
