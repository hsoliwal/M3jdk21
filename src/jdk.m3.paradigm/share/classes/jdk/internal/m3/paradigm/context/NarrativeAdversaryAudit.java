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

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic audit of synthetic narrative failure modes discussed through
 * the Western-Romantica adversary family.
 *
 * <p>The names are structural diagnostics, never diagnoses of people or media.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/locality/NarrativeAdversaryAudit.java}
 * blob {@code ae88c842e33f9ea6a2459dd2d34e8f0de8afc325} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class NarrativeAdversaryAudit {
    public enum FailureMode {
        CONVENIENT_JOIN,
        MANUFACTURED_GRADIENT,
        CATCH_ALL_LABEL,
        DEFICIT_MARKETING,
        HERO_EXCEPTION,
        PAIR_LOCAL_ABSOLUTISM,
        MEMORY_ERASURE_AS_REPAIR,
        COMPARATIVE_SELF_LOOP
    }

    public record Input(
            boolean unsupportedJoin,
            boolean manufacturedGradient,
            boolean catchAllLabel,
            boolean baselineDeclaredDeficientWithoutDeterioration,
            boolean rulesBypassedForSalientPair,
            boolean worldDimmedForPair,
            boolean memoryErasureClaimedAsRepair,
            boolean selfComparedAgainstEscalatingProjection) {
    }

    public record Receipt(
            List<FailureMode> candidates,
            boolean negativeTestRequired,
            boolean noSpecialTreatmentRequired,
            boolean relationshipVerdict,
            boolean psychologicalVerdict,
            boolean semanticVerdict) {
        public Receipt {
            candidates = List.copyOf(candidates == null ? List.of() : candidates);
        }
    }

    private NarrativeAdversaryAudit() {}

    public static Receipt audit(Input input) {
        if (input == null) throw new IllegalArgumentException("input");
        List<FailureMode> modes = new ArrayList<>();
        if (input.unsupportedJoin()) modes.add(FailureMode.CONVENIENT_JOIN);
        if (input.manufacturedGradient()) modes.add(FailureMode.MANUFACTURED_GRADIENT);
        if (input.catchAllLabel()) modes.add(FailureMode.CATCH_ALL_LABEL);
        if (input.baselineDeclaredDeficientWithoutDeterioration()) {
            modes.add(FailureMode.DEFICIT_MARKETING);
        }
        if (input.rulesBypassedForSalientPair()) modes.add(FailureMode.HERO_EXCEPTION);
        if (input.worldDimmedForPair()) modes.add(FailureMode.PAIR_LOCAL_ABSOLUTISM);
        if (input.memoryErasureClaimedAsRepair()) modes.add(FailureMode.MEMORY_ERASURE_AS_REPAIR);
        if (input.selfComparedAgainstEscalatingProjection()) {
            modes.add(FailureMode.COMPARATIVE_SELF_LOOP);
        }
        return new Receipt(
                modes,
                input.catchAllLabel(),
                input.rulesBypassedForSalientPair() || input.worldDimmedForPair(),
                false,
                false,
                false);
    }
}
