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

import java.util.Map;

/**
 * Deterministic gate for explicitly declared invalid semantic promotions.
 *
 * <p>Unknown relations remain open. This gate is not a natural-language truth
 * classifier.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/locality/SemanticPromotionGate.java}
 * blob {@code f40996f218c97134c1a2574be7ea008448618fca} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class SemanticPromotionGate {
    /** Gate decision. */
    public enum Decision {
        TYPE_PRESERVED,
        REJECTED_UNSUPPORTED_PROMOTION,
        OPEN
    }

    /** Gate receipt. */
    public record Receipt(
            String fromType,
            String toType,
            Decision decision,
            String reason,
            boolean semanticTruthCertified) {
    }

    private static final Map<String, String> BLOCKED = Map.ofEntries(
            Map.entry("care=>ownership", "CARE != OWNERSHIP"),
            Map.entry("pair-salience=>jurisdiction", "PAIR_SALIENCE != JURISDICTION"),
            Map.entry("simulation=>historical-referent", "SIMULATION != HISTORICAL_REFERENT"),
            Map.entry("rendering=>intention", "RENDERING != INTENTION"),
            Map.entry("intensity=>truth", "INTENSITY != AUTHORITY"),
            Map.entry("comparison=>growth", "COMPARISON != GROWTH"),
            Map.entry("memory=>resolved-conflict", "MEMORY != RESOLVED_CONFLICT"),
            Map.entry("finite-ending=>meaningless", "FINITE != MEANINGLESS"),
            Map.entry("withholding=>necessary-chemistry", "WITHHOLDING != NECESSARY_CHEMISTRY"),
            Map.entry("attraction=>reciprocity", "ATTRACTION != RECIPROCITY"),
            Map.entry("absence=>relational-depth", "ABSENCE != RELATIONAL_DEPTH"),
            Map.entry("stable-state=>deficit", "STABILITY != DEFICIT"));

    private SemanticPromotionGate() {
    }

    public static Receipt assess(String fromType, String toType) {
        String from = normalize(fromType);
        String to = normalize(toType);
        if (from.equals(to)) {
            return new Receipt(from, to, Decision.TYPE_PRESERVED, "TYPE_PRESERVED", false);
        }
        String reason = BLOCKED.get(from + "=>" + to);
        if (reason != null) {
            return new Receipt(
                    from,
                    to,
                    Decision.REJECTED_UNSUPPORTED_PROMOTION,
                    reason,
                    false);
        }
        return new Receipt(
                from,
                to,
                Decision.OPEN,
                "UNDECLARED_RELATION_REMAINS_OPEN",
                false);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("semantic type");
        }
        return value.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
