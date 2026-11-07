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
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.util.Objects;

/**
 * Stateless admission for primitive signal-evidence lanes.
 *
 * <p>This owner grants no comparator, domain-equivalence, or metric certificate.
 * A {@link #CERTIFICATE_REQUIRED} result is a refusal to take the shortcut until
 * a separately qualified proof has been checked against its source, epoch,
 * authoritative operation, and domain. An exact fact is only an exact fact,
 * not permission to use it as a semantic ordering key.
 *
 * <p>All codes fit one byte; arrays belong to the producer. No per-row object,
 * graph node, boxed key, or runtime reflection is created by this class.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/M3SignalEvidenceAdmission.java}
 * blob {@code 636772afa97ab8e9104b5011bfc87eeda36b6ab0} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3SignalEvidenceAdmission {
    public static final byte UNKNOWN = 0;
    public static final byte EXACT = 1;
    public static final byte LOWER_BOUND = 2;
    public static final byte UPPER_BOUND = 3;
    public static final byte OBSERVED = 4;
    public static final byte ESTIMATED = 5;
    public static final byte HEURISTIC = 6;

    public static final byte HINT = 0;
    public static final byte READ_EXACT_FACT = 1;
    public static final byte DECIDE_ORDER = 2;
    public static final byte PRUNE_FROM_LOWER_BOUND = 3;
    public static final byte PRUNE_FROM_UPPER_BOUND = 4;
    public static final byte SUBSTITUTE_BEHAVIOR = 5;

    public static final byte REFUSE_UNKNOWN = 0;
    public static final byte REFUSE_STALE = 1;
    public static final byte REFUSE_KIND = 2;
    public static final byte ADVISORY_ONLY = 3;
    public static final byte EXACT_FACT_ONLY = 4;
    public static final byte CERTIFICATE_REQUIRED = 5;

    private M3SignalEvidenceAdmission() { }

    /**
     * Interpret a producer-declared kind at one owner-controlled generation.
     * The producer must guarantee that its revision changes when dependencies
     * change; a matching number does not authenticate the producer.
     */
    public static byte assess(byte kind, long capturedRevision, long ownerRevision,
            byte intent) {
        requireKind(kind);
        requireIntent(intent);
        return assessValid(kind, capturedRevision, ownerRevision, intent);
    }

    /**
     * Batch interpretation of aligned primitive metadata columns.
     * All geometry and evidence-kind checks happen before the first destination
     * write. The source kind lane must not alias the destination status lane.
     * Source payloads and source row ordering are never changed.
     *
     * <p>Neither this method nor its results certify a comparator, a metric,
     * an execution trace, object equality, or atom substitution.
     */
    public static void assessBatch(byte[] kinds, long[] capturedRevisions,
            long[] ownerRevisions, int from, int count, byte intent,
            byte[] destination, int outputOffset) {
        Objects.requireNonNull(kinds, "kinds");
        Objects.requireNonNull(capturedRevisions, "capturedRevisions");
        Objects.requireNonNull(ownerRevisions, "ownerRevisions");
        Objects.requireNonNull(destination, "destination");
        requireIntent(intent);
        if (kinds.length != capturedRevisions.length
                || kinds.length != ownerRevisions.length) {
            throw new IllegalArgumentException("evidence lane length mismatch");
        }
        Objects.checkFromIndexSize(from, count, kinds.length);
        Objects.checkFromIndexSize(outputOffset, count, destination.length);
        if (kinds == destination) {
            throw new IllegalArgumentException("output aliases evidence-kind lane");
        }
        for (int i = from; i < from + count; i++) {
            requireKind(kinds[i]);
        }
        for (int i = 0; i < count; i++) {
            int row = from + i;
            destination[outputOffset + i] = assessValid(kinds[row],
                    capturedRevisions[row], ownerRevisions[row], intent);
        }
    }

    private static byte assessValid(byte kind, long capturedRevision,
            long ownerRevision, byte intent) {
        if (kind == UNKNOWN) return REFUSE_UNKNOWN;
        if (capturedRevision != ownerRevision) return REFUSE_STALE;
        return switch (intent) {
            case HINT -> ADVISORY_ONLY;
            case READ_EXACT_FACT -> kind == EXACT ? EXACT_FACT_ONLY : REFUSE_KIND;
            case DECIDE_ORDER, SUBSTITUTE_BEHAVIOR ->
                    kind == EXACT ? CERTIFICATE_REQUIRED : REFUSE_KIND;
            case PRUNE_FROM_LOWER_BOUND ->
                    kind == LOWER_BOUND ? CERTIFICATE_REQUIRED : REFUSE_KIND;
            case PRUNE_FROM_UPPER_BOUND ->
                    kind == UPPER_BOUND ? CERTIFICATE_REQUIRED : REFUSE_KIND;
            default -> throw new AssertionError("validated intent");
        };
    }

    private static void requireKind(byte kind) {
        if (kind < UNKNOWN || kind > HEURISTIC) {
            throw new IllegalArgumentException("unknown evidence kind: " + kind);
        }
    }

    private static void requireIntent(byte intent) {
        if (intent < HINT || intent > SUBSTITUTE_BEHAVIOR) {
            throw new IllegalArgumentException("unknown evidence intent: " + intent);
        }
    }
}
