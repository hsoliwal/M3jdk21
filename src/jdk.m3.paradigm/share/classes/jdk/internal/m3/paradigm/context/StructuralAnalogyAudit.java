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
import java.util.List;
import java.util.TreeSet;

/**
 * Structural-shape comparison without identity promotion.
 *
 * <p>Useful for self-similar mythic, literary, technical or relational shapes.
 * A shared relation never certifies identity, equivalence, gender essence,
 * metaphysical truth or a relationship verdict.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/locality/StructuralAnalogyAudit.java}
 * blob {@code afaacd02d0df68d2e4302dd5c347ffbfdc895498} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class StructuralAnalogyAudit {
    public record Receipt(
            String leftLabel,
            String rightLabel,
            List<String> sharedRelations,
            boolean structuralResemblanceCandidate,
            boolean identityEquivalent,
            boolean genderEssentialized,
            boolean rolesLocked,
            boolean metaphysicalVerdict,
            boolean relationshipVerdict,
            boolean semanticVerdict) {
        public Receipt {
            sharedRelations = List.copyOf(sharedRelations);
        }
    }

    private StructuralAnalogyAudit() {
    }

    public static Receipt compare(
            String leftLabel, String rightLabel, List<String> sharedRelations) {
        String left = require(leftLabel, "leftLabel");
        String right = require(rightLabel, "rightLabel");
        if (left.equals(right)) {
            throw new IllegalArgumentException("STRUCTURAL_ANALOGY_REQUIRES_DISTINCT_LABELS");
        }
        TreeSet<String> relations = new TreeSet<>();
        if (sharedRelations != null) {
            sharedRelations.stream()
                    .map(value -> value == null ? "" : value.trim())
                    .filter(value -> !value.isBlank())
                    .forEach(relations::add);
        }
        List<String> normalized = Collections.unmodifiableList(List.copyOf(relations));
        return new Receipt(
                left,
                right,
                normalized,
                !normalized.isEmpty(),
                false,
                false,
                false,
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
