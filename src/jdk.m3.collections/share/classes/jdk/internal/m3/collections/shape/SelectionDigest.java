/* *****************************************************************************
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
package jdk.internal.m3.collections.shape;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Canonical ASCII policy records and SHA-256; not an authenticity mechanism.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/SelectionDigest.java}
 * blob {@code 8151c9e6b6c9e42a49a219911d154c178003f9d6} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 2 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class SelectionDigest {
    private SelectionDigest() { }
    static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }
    static String candidate(CollectionCandidate c) {
        CandidateProfile p = c.profile();
        return c.id() + "|" + c.kind() + "|" + c.order() + "|" + c.concurrency()
                + "|" + c.valueEquality() + "|" + c.identityEquality() + "|" + c.nullable()
                + "|" + c.maximumSize() + "|" + p.sizeBand() + "|" + p.lookup() + "|"
                + p.mutation() + "|" + p.traversal() + "|" + p.memory() + "|" + p.growth();
    }
    static String request(CollectionRequest request) {
        CollectionRequirements r = request.required();
        CollectionPreferences p = request.preferred();
        return r.kind() + "|" + r.order() + "|" + r.equality() + "|" + r.concurrency()
                + "|" + r.allowNull() + "|" + r.maximumSize() + "|" + p.expectedSize()
                + "|" + p.lookup() + "|" + p.mutation() + "|" + p.traversal()
                + "|" + p.memory() + "|" + p.growth();
    }
}
