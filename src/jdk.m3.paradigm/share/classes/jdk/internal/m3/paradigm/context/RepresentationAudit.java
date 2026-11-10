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
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Deterministic audits for rendering, compression, snapshot, symbolic-role and
 * source-custody boundaries.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/locality/RepresentationAudit.java}
 * blob {@code d8816ddae97d35d9bfbc37fa7dde4bde3f9c409f} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class RepresentationAudit {
    private static final Pattern SHA256 = Pattern.compile("[a-f0-9]{64}");

    public record RenderedAffordanceReceipt(
            String locality,
            String cue,
            boolean selectedByLocality,
            boolean systemInferredEmotion,
            boolean inferredIdentity,
            boolean inferredMotive,
            boolean inferredRelation,
            boolean localityCueCandidate,
            boolean semanticOverreachCandidate,
            boolean renderedAffordanceIsSourceIntention,
            boolean themeChoiceIsIdentity,
            boolean themeChoiceIsMotive,
            boolean themeChoiceIsRelationState,
            boolean colorCodingIsOntology,
            boolean diagnosticOnly,
            boolean semanticVerdict) {
    }

    public record CompressionReceipt(
            String label,
            List<String> sourceDimensions,
            List<String> retainedDimensions,
            List<String> lostDimensions,
            List<String> reopenedDimensions,
            boolean closureClaimed,
            boolean allSourceDimensionsRetained,
            boolean compressionWithoutForeclosureCandidate,
            boolean collapseCandidate,
            boolean compressionIsEquivalence,
            boolean labelIsReferent,
            boolean omissionIsProofOfAbsence,
            boolean diagnosticOnly,
            boolean semanticVerdict) {
    }

    public record RenderSnapshotReceipt(
            String artifactId,
            String stateId,
            String previousSnapshotId,
            String theme,
            String sourceRef,
            String imageReceiptRef,
            boolean sourceMutable,
            boolean snapshotImmutable,
            boolean snapshotIsCurrentState,
            boolean snapshotHasCurrentJurisdiction,
            boolean screenshotIsExecutableArtifact,
            boolean diagnosticOnly,
            boolean semanticVerdict) {
    }

    public record CreativeConstraintReceipt(
            String locality,
            String operation,
            boolean optionalExpressionOccurred,
            boolean externallyCompelled,
            boolean constraintBreaksOnAssimilation,
            boolean optionalExpressionAllowed,
            boolean expressionRequired,
            boolean expressionForbidden,
            boolean optionalExpressionIsAssimilation,
            boolean compulsoryAssimilationCandidate,
            boolean localityProtectedByVisibleFailure,
            boolean diagnosticOnly,
            boolean semanticVerdict) {
    }

    public record PairingCueReceipt(
            String left,
            String right,
            String cue,
            String sourceRelation,
            String relationshipClaim,
            boolean adjacencyObserved,
            String relationType,
            boolean relationshipClaimAdmitted,
            boolean adjacencyIsRelationshipVerdict,
            boolean visualPairIsRomanticCouple,
            boolean layoutChangesSalience,
            boolean diagnosticOnly,
            boolean semanticVerdict) {
    }

    public record SymbolicRoleReceipt(
            String shape,
            String leftRole,
            String rightRole,
            String leftReferent,
            String rightReferent,
            boolean structuralAnalogyRecorded,
            boolean referentIdentityAssigned,
            boolean genderEssenceAssigned,
            boolean destinyAssigned,
            boolean shapeIsIdentity,
            boolean diagnosticOnly,
            boolean semanticVerdict) {
    }

    public enum CustodyStatus {
        RAW_GIT_BYTES_VERIFIED,
        HASH_ACCOUNTED_NOT_GIT_RAW,
        UNVERIFIED
    }

    public record SourceCustodyReceipt(
            String name,
            long bytes,
            String sha256,
            boolean hashAccounted,
            boolean rawGitBytesPresent,
            boolean exactReadbackVerified,
            boolean rawCustodyProven,
            CustodyStatus custodyStatus,
            boolean hashAccountedEqualsRawBytesPresent,
            boolean drivePointerEqualsGitBytes,
            boolean diagnosticOnly,
            boolean semanticVerdict) {
    }

    public record FamilyCoverageReceipt(
            List<String> requiredFamilies,
            List<String> implementedFamilies,
            List<String> missingFamilies,
            List<String> extraFamilies,
            boolean coverageComplete,
            boolean coverageReceiptIsHistoricalOmniscience,
            boolean inaccessibleHistoryFabricated,
            boolean diagnosticOnly,
            boolean semanticVerdict) {
    }

    private RepresentationAudit() {
    }

    public static RenderedAffordanceReceipt auditRenderedAffordance(
            String locality,
            String cue,
            boolean selectedByLocality,
            boolean systemInferredEmotion,
            boolean inferredIdentity,
            boolean inferredMotive,
            boolean inferredRelation) {
        String loc = nullable(locality);
        String visualCue = nullable(cue);
        boolean overreach = systemInferredEmotion
                || inferredIdentity
                || inferredMotive
                || inferredRelation;
        return new RenderedAffordanceReceipt(
                loc,
                visualCue,
                selectedByLocality,
                systemInferredEmotion,
                inferredIdentity,
                inferredMotive,
                inferredRelation,
                loc != null && visualCue != null,
                overreach,
                false,
                false,
                false,
                false,
                false,
                true,
                false);
    }

    public static CompressionReceipt auditCompression(
            String label,
            List<String> sourceDimensions,
            List<String> retainedDimensions,
            List<String> reopenedDimensions,
            boolean closureClaimed) {
        List<String> source = normalizedList(sourceDimensions);
        List<String> retained = normalizedList(retainedDimensions);
        Set<String> retainedSet = Set.copyOf(retained);
        List<String> lost = source.stream().filter(value -> !retainedSet.contains(value)).toList();
        List<String> reopened = normalizedList(reopenedDimensions);
        boolean allRetained = lost.isEmpty();
        return new CompressionReceipt(
                nullable(label),
                source,
                retained,
                lost,
                reopened,
                closureClaimed,
                allRetained,
                !closureClaimed && (allRetained || !reopened.isEmpty()),
                closureClaimed && !lost.isEmpty(),
                false,
                false,
                false,
                true,
                false);
    }

    public static RenderSnapshotReceipt recordRenderSnapshot(
            String artifactId,
            String stateId,
            String previousSnapshotId,
            String theme,
            String sourceRef,
            String imageReceiptRef,
            boolean sourceMutable,
            boolean snapshotImmutable) {
        return new RenderSnapshotReceipt(
                requireText(artifactId, "artifactId"),
                requireText(stateId, "stateId"),
                nullable(previousSnapshotId),
                nullable(theme),
                nullable(sourceRef),
                nullable(imageReceiptRef),
                sourceMutable,
                snapshotImmutable,
                false,
                false,
                false,
                true,
                false);
    }

    public static CreativeConstraintReceipt auditCreativeConstraint(
            String locality,
            String operation,
            boolean optionalExpressionOccurred,
            boolean externallyCompelled,
            boolean constraintBreaksOnAssimilation) {
        String loc = nullable(locality);
        return new CreativeConstraintReceipt(
                loc,
                nullable(operation),
                optionalExpressionOccurred,
                externallyCompelled,
                constraintBreaksOnAssimilation,
                true,
                false,
                false,
                false,
                externallyCompelled,
                loc != null && constraintBreaksOnAssimilation,
                true,
                false);
    }

    public static PairingCueReceipt auditPairingCue(
            String left,
            String right,
            String cue,
            String sourceRelation,
            String relationshipClaim) {
        String l = nullable(left);
        String r = nullable(right);
        String relation = nullable(sourceRelation);
        String claim = nullable(relationshipClaim);
        return new PairingCueReceipt(
                l,
                r,
                nullable(cue),
                relation,
                claim,
                l != null && r != null,
                relation,
                relation != null && relation.equals(claim),
                false,
                false,
                nullable(cue) != null,
                true,
                false);
    }

    public static SymbolicRoleReceipt auditSymbolicRoleProjection(
            String shape,
            String leftRole,
            String rightRole,
            String leftReferent,
            String rightReferent) {
        String normalizedShape = nullable(shape);
        String left = nullable(leftRole);
        String right = nullable(rightRole);
        return new SymbolicRoleReceipt(
                normalizedShape,
                left,
                right,
                nullable(leftReferent),
                nullable(rightReferent),
                normalizedShape != null && left != null && right != null,
                false,
                false,
                false,
                false,
                true,
                false);
    }

    public static SourceCustodyReceipt auditSourceCustody(
            String name,
            long bytes,
            String sha256,
            boolean hashAccounted,
            boolean rawGitBytesPresent,
            boolean exactReadbackVerified) {
        if (bytes < 0) {
            throw new IllegalArgumentException("bytes");
        }
        String hash = requireText(sha256, "sha256").toLowerCase(Locale.ROOT);
        if (!SHA256.matcher(hash).matches()) {
            throw new IllegalArgumentException("sha256");
        }
        boolean proven = rawGitBytesPresent && exactReadbackVerified;
        CustodyStatus status = proven
                ? CustodyStatus.RAW_GIT_BYTES_VERIFIED
                : hashAccounted
                        ? CustodyStatus.HASH_ACCOUNTED_NOT_GIT_RAW
                        : CustodyStatus.UNVERIFIED;
        return new SourceCustodyReceipt(
                requireText(name, "name"),
                bytes,
                hash,
                hashAccounted,
                rawGitBytesPresent,
                exactReadbackVerified,
                proven,
                status,
                false,
                false,
                true,
                false);
    }

    public static FamilyCoverageReceipt auditFamilyCoverage(
            List<String> implementedFamilies,
            List<String> requiredFamilies) {
        List<String> implemented = normalizedList(implementedFamilies);
        List<String> required = normalizedList(requiredFamilies);
        Set<String> implementedSet = Set.copyOf(implemented);
        Set<String> requiredSet = Set.copyOf(required);
        List<String> missing = required.stream()
                .filter(value -> !implementedSet.contains(value))
                .toList();
        List<String> extra = implemented.stream()
                .filter(value -> !requiredSet.contains(value))
                .toList();
        return new FamilyCoverageReceipt(
                required,
                implemented,
                missing,
                extra,
                missing.isEmpty(),
                false,
                false,
                true,
                false);
    }

    static List<String> normalizedList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        TreeSet<String> out = new TreeSet<>();
        values.stream()
                .map(RepresentationAudit::normalize)
                .filter(value -> !value.isBlank())
                .forEach(out::add);
        return Collections.unmodifiableList(List.copyOf(out));
    }

    static String requireText(String value, String name) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(name);
        }
        return normalized;
    }

    static String nullable(String value) {
        String normalized = normalize(value);
        return normalized.isBlank() ? null : normalized;
    }

    static String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }
}
