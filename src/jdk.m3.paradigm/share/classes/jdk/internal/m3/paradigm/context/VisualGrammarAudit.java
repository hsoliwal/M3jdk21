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

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Deterministic visual-grammar receipt for typography, placement and render
 * projections.
 *
 * <p>The receipt can record that composition carries meaning while keeping
 * labels non-sovereign: visual salience is not semantic authority and a
 * screenshot is not the executable artifact that produced it.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/locality/VisualGrammarAudit.java}
 * blob {@code 5e7a89bc43cfc66d3daf49b8d1604179af0434a7} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class VisualGrammarAudit {
    public record Receipt(
            String artifactId,
            String label,
            Map<String, String> cues,
            boolean compositionCarriesMeaning,
            boolean labelClaimsSemanticClosure,
            boolean screenshot,
            boolean executableArtifact,
            boolean compressionWithoutForeclosureCandidate,
            boolean visualLabelIsSemanticClosure,
            boolean screenshotIsExecutableArtifact,
            boolean semanticVerdict) {
        public Receipt {
            cues = Collections.unmodifiableMap(new TreeMap<>(cues));
        }
    }

    private VisualGrammarAudit() {
    }

    public static Receipt audit(
            String artifactId,
            String label,
            Map<String, String> cues,
            boolean compositionCarriesMeaning,
            boolean labelClaimsSemanticClosure,
            boolean screenshot,
            boolean executableArtifact) {
        String id = require(artifactId, "artifactId");
        String normalizedLabel = label == null ? "" : label.trim();
        Map<String, String> normalizedCues = new TreeMap<>();
        if (cues != null) {
            cues.forEach((key, value) -> {
                String k = key == null ? "" : key.trim();
                String v = value == null ? "" : value.trim();
                if (!k.isBlank()) {
                    normalizedCues.put(k, v);
                }
            });
        }
        boolean openCompression =
                compositionCarriesMeaning && !labelClaimsSemanticClosure && !normalizedLabel.isBlank();
        return new Receipt(
                id,
                normalizedLabel,
                normalizedCues,
                compositionCarriesMeaning,
                labelClaimsSemanticClosure,
                screenshot,
                executableArtifact,
                openCompression,
                false,
                false,
                false);
    }

    private static String require(String value, String field) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field);
        }
        return normalized;
    }
}
