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
package jdk.internal.m3.collections.mat;

import java.util.Locale;
import java.util.Objects;

/**
 * Provenance pin for an operator-supplied finite-lexicon source file.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatLexiconSource.java}
 * blob {@code 0fdfc6ce229c44a0e1171e0862361b0ba18c9a1d} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public record MatLexiconSource(
        String provider,
        String dataset,
        String revision,
        String licenseId,
        String sha256Hex) {

    public MatLexiconSource {
        provider = requireText(provider, "provider");
        dataset = requireText(dataset, "dataset");
        revision = requireText(revision, "revision");
        licenseId = requireText(licenseId, "licenseId");
        sha256Hex = requireSha256(sha256Hex);
    }

    private static String requireText(String value, String name) {
        String text = Objects.requireNonNull(value, name).strip();
        if (text.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return text;
    }

    private static String requireSha256(String value) {
        String hash = Objects.requireNonNull(value, "sha256Hex")
                .strip()
                .toLowerCase(Locale.ROOT);
        if (!hash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("sha256Hex must contain exactly 64 hex digits");
        }
        return hash;
    }
}
