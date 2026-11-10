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

import java.util.Objects;

/**
 * Immutable boundary packet used to create one perception.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextPerceptionObservation.java}
 * blob {@code 405e5985fa4fd72b586e30b2b239a5c95cc793dd} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public record PerceptionObservation(
        String modality,
        long sequence,
        long observedAtMs,
        long ttlMs,
        String contentRoot,
        String evidenceRoot,
        int confidenceQ8) {

    public PerceptionObservation {
        modality = text(modality, "modality");
        if (sequence < 0 || observedAtMs < 0 || ttlMs <= 0) {
            throw new IllegalArgumentException("invalid perception observation clock");
        }
        contentRoot = hash(contentRoot, "contentRoot");
        evidenceRoot = hash(evidenceRoot, "evidenceRoot");
        if (confidenceQ8 < 0 || confidenceQ8 > 255) {
            throw new IllegalArgumentException("confidenceQ8 must be in [0,255]");
        }
    }

    private static String hash(String value, String name) {
        String normalized = text(value, name);
        if (!normalized.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(name + " must be lowercase SHA-256");
        }
        return normalized;
    }

    private static String text(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).strip();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }
}
