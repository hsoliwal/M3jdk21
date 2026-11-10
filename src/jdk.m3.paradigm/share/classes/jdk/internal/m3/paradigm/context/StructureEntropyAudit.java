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
import java.util.Set;
import java.util.TreeSet;

/**
 * Deterministic audits for comparator loops, radiance gradients, negative
 * tests, agency support, repair claims and narrator boundaries.
 *
 * <p>The name StructureEntropy is a project term. These receipts are not
 * thermodynamic measurements and not psychological verdicts.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/locality/StructureEntropyAudit.java}
 * blob {@code 12b293fa72b108759189aca61107d477f8d7c1b9} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class StructureEntropyAudit {
    public record ComparatorLoopReceipt(
            boolean baselineReturned,
            boolean peakDistinct,
            boolean referenceFrameChanged,
            boolean localStateDeteriorated,
            boolean deficitClaimed,
            boolean relativeDimmingWithoutDeterioration,
            boolean manufacturedDeficitCandidate,
            boolean eventDensityIsStructuralDelta,
            boolean referenceFrameChangeIsLocalDeterioration,
            boolean yearningAmplificationProven,
            boolean diagnosticOnly,
            boolean semanticVerdict) {
    }

    public record RadianceGradientReceipt(
            boolean endpointAChanged,
            boolean endpointBChanged,
            boolean rankingChanged,
            boolean rankingOnlyChange,
            boolean dimmingInjected,
            boolean manufacturedMystery,
            boolean inducedInstability,
            boolean observedReaction,
            boolean mutualObservability,
            boolean manufacturedGradientCandidate,
            boolean stateGroundedDifferentialCandidate,
            boolean inducedReactionObserved,
            boolean instabilityWasNecessary,
            boolean differenceRequiresCompetition,
            boolean radianceRequiresEndpointDimming,
            boolean pairSalienceGrantsWorldJurisdiction,
            boolean diagnosticOnly,
            boolean semanticVerdict) {
    }

    public record NegativeTestObservation(
            String outcome,
            String classification,
            boolean rejected,
            boolean competingExplanation) {
        public NegativeTestObservation {
            outcome = normalize(outcome);
            classification = normalize(classification);
        }
    }

    public record NegativeTestReceipt(
            int observationCount,
            int distinctOutcomes,
            int distinctClassifications,
            int rejectedCount,
            int competingCount,
            boolean catchAllParserCandidate,
            boolean negativeTestMissing,
            boolean theoryExplainsEveryOutcome,
            boolean falsifiable,
            boolean diagnosticOnly,
            boolean semanticVerdict) {
    }

    public record AgencySupportReceipt(
            boolean supportOffered,
            boolean otherAgencyRetained,
            boolean guidanceOnly,
            boolean selfErasure,
            boolean saviorCentrality,
            boolean blameTolerated,
            boolean blameAcceptedAsTrue,
            boolean annexationRisk,
            boolean boundedSupportCandidate,
            boolean boundedGuidanceCandidate,
            boolean blameToleranceWithoutTruthAdmission,
            boolean helpRequiresSelfConsumption,
            boolean assistanceAnnexesRecovery,
            boolean guidanceSubstitutesAgency,
            boolean diagnosticOnly,
            boolean semanticVerdict) {
    }

    public record RepairClaimReceipt(
            boolean conflictOccurred,
            boolean memoryErased,
            boolean reconnected,
            boolean accountabilityPerformed,
            boolean structuralIssueResolved,
            boolean evidenceOfRepair,
            boolean memoryErasureIsRepair,
            boolean reconnectionIsRepair,
            boolean reconnectionWithoutRepairCandidate,
            boolean resetLoopCandidate,
            boolean diagnosticOnly,
            boolean semanticVerdict) {
    }

    public record NarratorBoundaryReceipt(
            String narratorLocality,
            List<String> firstPersonReports,
            List<String> claimsAboutOther,
            List<String> externalEvidenceRefs,
            boolean narratorExperiencePreserved,
            boolean otherClaimsRequireIndependentGrounding,
            boolean claimsAboutOtherAutomaticallyFact,
            boolean narratorTheoryAutomaticallyFact,
            boolean narratorExperienceAutomaticallyInvalid,
            boolean diagnosticOnly,
            boolean semanticVerdict) {
    }

    public record OpenRelationVerdictReceipt(
            boolean reciprocalObservability,
            boolean reciprocalSignificance,
            boolean mutualSalience,
            boolean affectiveDifferential,
            String proposedLabel,
            List<String> acceptedBy,
            List<String> rejectedBy,
            List<String> requiredLocalities,
            boolean labelSourceGrounded,
            boolean labelAdmitted,
            String relationalType,
            String closure,
            boolean reciprocalSignificanceRequiresFinalLabel,
            boolean diagnosticOnly,
            boolean semanticVerdict) {
    }

    private StructureEntropyAudit() {
    }

    public static ComparatorLoopReceipt diagnoseComparatorLoop(
            String stateBefore,
            String statePeak,
            String stateAfter,
            String comparatorBefore,
            String comparatorAfter,
            boolean localStateDeteriorated,
            boolean deficitClaimed) {
        String before = normalize(stateBefore);
        String peak = normalize(statePeak);
        String after = normalize(stateAfter);
        String comparator0 = normalize(comparatorBefore);
        String comparator1 = normalize(comparatorAfter);
        boolean baselineReturned = !before.isBlank() && before.equals(after);
        boolean peakDistinct = !peak.isBlank() && !before.isBlank() && !peak.equals(before);
        boolean referenceFrameChanged = !comparator0.isBlank()
                && !comparator1.isBlank()
                && !comparator0.equals(comparator1);
        boolean relativeDimming = baselineReturned
                && referenceFrameChanged
                && !localStateDeteriorated;
        return new ComparatorLoopReceipt(
                baselineReturned,
                peakDistinct,
                referenceFrameChanged,
                localStateDeteriorated,
                deficitClaimed,
                relativeDimming,
                relativeDimming && deficitClaimed,
                false,
                false,
                false,
                true,
                false);
    }

    public static RadianceGradientReceipt auditRadianceGradient(
            String endpointABefore,
            String endpointAAfter,
            String endpointBBefore,
            String endpointBAfter,
            String rankingBefore,
            String rankingAfter,
            boolean endpointADimmed,
            boolean endpointBDimmed,
            boolean environmentDimmed,
            boolean manufacturedMystery,
            boolean inducedInstability,
            boolean observedReaction,
            boolean mutualObservability) {
        String a0 = normalize(endpointABefore);
        String a1 = normalize(endpointAAfter);
        String b0 = normalize(endpointBBefore);
        String b1 = normalize(endpointBAfter);
        boolean aChanged = !a0.isBlank() && !a1.isBlank() && !a0.equals(a1);
        boolean bChanged = !b0.isBlank() && !b1.isBlank() && !b0.equals(b1);
        String r0 = normalize(rankingBefore);
        String r1 = normalize(rankingAfter);
        boolean rankingChanged = !r0.isBlank() && !r1.isBlank() && !r0.equals(r1);
        boolean rankingOnlyChange = rankingChanged && !aChanged && !bChanged;
        boolean dimmingInjected = endpointADimmed || endpointBDimmed || environmentDimmed;
        boolean manufacturedGradient =
                dimmingInjected || rankingOnlyChange || manufacturedMystery;
        return new RadianceGradientReceipt(
                aChanged,
                bChanged,
                rankingChanged,
                rankingOnlyChange,
                dimmingInjected,
                manufacturedMystery,
                inducedInstability,
                observedReaction,
                mutualObservability,
                manufacturedGradient,
                mutualObservability && !manufacturedGradient && (aChanged || bChanged),
                inducedInstability && observedReaction,
                false,
                false,
                false,
                false,
                true,
                false);
    }

    public static NegativeTestReceipt auditNegativeTest(
            List<NegativeTestObservation> observations) {
        List<NegativeTestObservation> items =
                observations == null ? List.of() : List.copyOf(observations);
        long rejected = items.stream().filter(NegativeTestObservation::rejected).count();
        long competing =
                items.stream().filter(NegativeTestObservation::competingExplanation).count();
        long outcomes = items.stream()
                .map(NegativeTestObservation::outcome)
                .filter(value -> !value.isBlank())
                .distinct()
                .count();
        long classifications = items.stream()
                .map(NegativeTestObservation::classification)
                .filter(value -> !value.isBlank())
                .distinct()
                .count();
        boolean catchAll = items.size() > 1
                && outcomes > 1
                && classifications == 1
                && rejected == 0
                && competing == 0;
        return new NegativeTestReceipt(
                items.size(),
                Math.toIntExact(outcomes),
                Math.toIntExact(classifications),
                Math.toIntExact(rejected),
                Math.toIntExact(competing),
                catchAll,
                catchAll,
                catchAll,
                rejected > 0 || competing > 0 || classifications > 1,
                true,
                false);
    }

    public static AgencySupportReceipt auditAgencySupport(
            boolean supportOffered,
            boolean otherAgencyRetained,
            boolean guidanceOnly,
            boolean selfErasure,
            boolean saviorCentrality,
            boolean blameTolerated,
            boolean blameAcceptedAsTrue) {
        boolean annexationRisk =
                supportOffered && (!otherAgencyRetained || selfErasure || saviorCentrality);
        return new AgencySupportReceipt(
                supportOffered,
                otherAgencyRetained,
                guidanceOnly,
                selfErasure,
                saviorCentrality,
                blameTolerated,
                blameAcceptedAsTrue,
                annexationRisk,
                supportOffered
                        && otherAgencyRetained
                        && !selfErasure
                        && !saviorCentrality,
                supportOffered && guidanceOnly && otherAgencyRetained && !selfErasure,
                blameTolerated && !blameAcceptedAsTrue,
                false,
                false,
                false,
                true,
                false);
    }

    public static RepairClaimReceipt auditRepairClaim(
            boolean conflictOccurred,
            boolean memoryErased,
            boolean reconnected,
            boolean accountabilityPerformed,
            boolean structuralIssueResolved) {
        boolean evidence =
                conflictOccurred && accountabilityPerformed && structuralIssueResolved;
        return new RepairClaimReceipt(
                conflictOccurred,
                memoryErased,
                reconnected,
                accountabilityPerformed,
                structuralIssueResolved,
                evidence,
                false,
                false,
                reconnected && !evidence,
                conflictOccurred && memoryErased && reconnected && !evidence,
                true,
                false);
    }

    public static NarratorBoundaryReceipt preserveNarratorBoundary(
            String narratorLocality,
            List<String> firstPersonReports,
            List<String> claimsAboutOther,
            List<String> externalEvidenceRefs) {
        List<String> reports = normalizedList(firstPersonReports);
        List<String> claims = normalizedList(claimsAboutOther);
        return new NarratorBoundaryReceipt(
                nullable(narratorLocality),
                reports,
                claims,
                normalizedList(externalEvidenceRefs),
                !reports.isEmpty(),
                !claims.isEmpty(),
                false,
                false,
                false,
                true,
                false);
    }

    public static OpenRelationVerdictReceipt buildOpenRelationVerdict(
            boolean reciprocalObservability,
            boolean reciprocalSignificance,
            boolean mutualSalience,
            boolean affectiveDifferential,
            String proposedLabel,
            List<String> acceptedBy,
            List<String> rejectedBy,
            List<String> requiredLocalities,
            boolean labelSourceGrounded) {
        String proposed = nullable(proposedLabel);
        List<String> accepted = normalizedList(acceptedBy);
        List<String> rejected = normalizedList(rejectedBy);
        List<String> required = normalizedList(requiredLocalities);
        Set<String> acceptedSet = Set.copyOf(accepted);
        boolean allRequiredAccepted =
                !required.isEmpty() && required.stream().allMatch(acceptedSet::contains);
        boolean labelAdmitted = proposed != null
                && labelSourceGrounded
                && allRequiredAccepted
                && rejected.isEmpty();
        return new OpenRelationVerdictReceipt(
                reciprocalObservability,
                reciprocalSignificance,
                mutualSalience,
                affectiveDifferential,
                proposed,
                accepted,
                rejected,
                required,
                labelSourceGrounded,
                labelAdmitted,
                labelAdmitted ? proposed : null,
                labelAdmitted ? "LOCAL_LABEL_ADMITTED" : "OPEN",
                false,
                true,
                false);
    }

    private static List<String> normalizedList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        TreeSet<String> out = new TreeSet<>();
        values.stream()
                .map(StructureEntropyAudit::normalize)
                .filter(value -> !value.isBlank())
                .forEach(out::add);
        return Collections.unmodifiableList(List.copyOf(out));
    }

    private static String nullable(String value) {
        String normalized = normalize(value);
        return normalized.isBlank() ? null : normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }
}
