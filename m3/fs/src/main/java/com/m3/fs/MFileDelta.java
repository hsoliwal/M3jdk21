/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Immutable comparison of two observations of the same exact provider address.
 * The retained state is two observation references and primitive verdicts and
 * reasons. Comparisons allocate no retained map, payload, parser, or history.
 * SAME describes the stated evidence dimension, never a guarantee that the live
 * source remained stable before, between or after the observations.
 */
public final class MFileDelta {
    public enum State { UNKNOWN, SAME, DIFFERENT }
    public enum ContentEvidence { NONE, FULL_CONTENT_ID, AUTHORITATIVE_PROVIDER_TOKEN }

    public static final long PRESENCE_UNKNOWN = 1L;
    public static final long ADDED = 1L << 1;
    public static final long REMOVED = 1L << 2;
    public static final long TYPE_CHANGED = 1L << 3;
    public static final long SIZE_CHANGED = 1L << 4;
    public static final long MODIFIED_CHANGED = 1L << 5;
    public static final long METADATA_UNKNOWN = 1L << 6;
    public static final long METADATA_MATCH = 1L << 7;
    public static final long STAMP_UNAVAILABLE = 1L << 8;
    public static final long STAMP_DOMAIN_CHANGED = 1L << 9;
    public static final long PROVIDER_REVISION_MATCH = 1L << 10;
    public static final long PROVIDER_REVISION_CHANGED = 1L << 11;
    public static final long HEURISTIC_STAMP_MATCH = 1L << 12;
    public static final long HEURISTIC_STAMP_CHANGED = 1L << 13;
    public static final long CONTENT_UNKNOWN = 1L << 14;
    public static final long CONTENT_ID_MATCH = 1L << 15;
    public static final long CONTENT_ID_CHANGED = 1L << 16;
    public static final long PROVIDER_CONTENT_MATCH = 1L << 17;
    public static final long CONFLICTING_EVIDENCE = 1L << 18;
    public static final long STRUCTURE_UNKNOWN = 1L << 19;
    public static final long STRUCTURE_DOMAIN_CHANGED = 1L << 20;
    public static final long STRUCTURE_ID_MATCH = 1L << 21;
    public static final long STRUCTURE_ID_CHANGED = 1L << 22;
    public static final long METADATA_KNOWLEDGE_CHANGED = 1L << 23;
    public static final long CONTENT_SIZE_CHANGED = 1L << 24;

    private static final byte UNKNOWN = 0, SAME = 1, DIFFERENT = 2;
    private static final byte NO_CONTENT_EVIDENCE = 0, FULL_CONTENT_ID = 1, PROVIDER_TOKEN = 2;

    private final MFileObservation before;
    private final MFileObservation after;
    private final byte presence;
    private final byte metadata;
    private final byte providerRevision;
    private final byte content;
    private final byte contentEvidence;
    private final byte structure;
    private final long reasons;

    private MFileDelta(MFileObservation before, MFileObservation after, byte presence, byte metadata,
            byte providerRevision, byte content, byte contentEvidence, byte structure, long reasons) {
        this.before = before;
        this.after = after;
        this.presence = presence;
        this.metadata = metadata;
        this.providerRevision = providerRevision;
        this.content = content;
        this.contentEvidence = contentEvidence;
        this.structure = structure;
        this.reasons = reasons;
    }

    /**
     * Compares independently observed evidence; metadata equality is never used
     * to infer content equality. Complete RAW_BYTES identity and size take
     * precedence over provider tokens, including contradictory strong tokens.
     * Contradictory known lengths prevent a token-only equality conclusion.
     */
    public static MFileDelta between(MFileObservation before, MFileObservation after) {
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        if (!before.sourceKey().equals(after.sourceKey())) throw new IllegalArgumentException("observations refer to different sources");

        long reasons = 0;
        int left = before.flags(), right = after.flags();
        byte presence = UNKNOWN;
        if (both(left, right, MFileObservation.EXISTS_KNOWN)) {
            presence = ((left ^ right) & MFileObservation.EXISTS) == 0 ? SAME : DIFFERENT;
            if (presence == DIFFERENT) reasons |= (right & MFileObservation.EXISTS) != 0 ? ADDED : REMOVED;
        } else reasons |= PRESENCE_UNKNOWN;

        boolean changed = presence == DIFFERENT;
        boolean incomplete = presence == UNKNOWN;
        if (before.presence() != MFileObservation.Presence.ABSENT
                || after.presence() != MFileObservation.Presence.ABSENT) {
            if (both(left, right, MFileObservation.TYPE_KNOWN)) {
                if (((left ^ right) & MFileObservation.TYPE_BITS) != 0) { reasons |= TYPE_CHANGED; changed = true; }
            } else incomplete = true;
            if (both(left, right, MFileObservation.SIZE_KNOWN)) {
                if (before.sizeValue() != after.sizeValue()) { reasons |= SIZE_CHANGED; changed = true; }
            } else incomplete = true;
            if (both(left, right, MFileObservation.MODIFIED_KNOWN)) {
                if (before.modifiedValue() != after.modifiedValue()) { reasons |= MODIFIED_CHANGED; changed = true; }
            } else incomplete = true;
        }
        if (((left ^ right) & MFileObservation.KNOWN_BITS) != 0) reasons |= METADATA_KNOWLEDGE_CHANGED;
        byte metadata = changed ? DIFFERENT : incomplete ? UNKNOWN : SAME;
        if (metadata == UNKNOWN) reasons |= METADATA_UNKNOWN;
        else if (metadata == SAME) reasons |= METADATA_MATCH;

        MFileObservation.ChangeStamp oldStamp = before.changeStamp(), newStamp = after.changeStamp();
        byte providerRevision = UNKNOWN;
        if (!oldStamp.available() || !newStamp.available()) reasons |= STAMP_UNAVAILABLE;
        else if (!oldStamp.sameDomain(newStamp)) reasons |= STAMP_DOMAIN_CHANGED;
        else if (oldStamp.authoritative() && newStamp.authoritative()) {
            providerRevision = oldStamp.token().equals(newStamp.token()) ? SAME : DIFFERENT;
            reasons |= providerRevision == SAME ? PROVIDER_REVISION_MATCH : PROVIDER_REVISION_CHANGED;
        } else reasons |= oldStamp.token().equals(newStamp.token()) ? HEURISTIC_STAMP_MATCH : HEURISTIC_STAMP_CHANGED;

        M3ObjectRef oldContent = before.contentReferenceOrNull(), newContent = after.contentReferenceOrNull();
        byte content = UNKNOWN, evidence = NO_CONTENT_EVIDENCE;
        if (oldContent != null && newContent != null) {
            boolean sameId = oldContent.id().equals(newContent.id());
            boolean sameLength = oldContent.size() == newContent.size();
            content = sameId && sameLength ? SAME : DIFFERENT;
            evidence = FULL_CONTENT_ID;
            reasons |= sameId ? CONTENT_ID_MATCH : CONTENT_ID_CHANGED;
            if (!sameLength) reasons |= CONTENT_SIZE_CHANGED;
            if (sameId && !sameLength) reasons |= CONFLICTING_EVIDENCE;
            if (content == DIFFERENT && providerRevision == SAME) reasons |= CONFLICTING_EVIDENCE;
        } else if (providerRevision == SAME && before.allowsContent() && after.allowsContent()) {
            long oldSize = oldContent != null ? oldContent.size()
                    : (left & MFileObservation.SIZE_KNOWN) != 0 ? before.sizeValue() : -1;
            long newSize = newContent != null ? newContent.size()
                    : (right & MFileObservation.SIZE_KNOWN) != 0 ? after.sizeValue() : -1;
            if (oldSize >= 0 && newSize >= 0 && oldSize != newSize) reasons |= CONFLICTING_EVIDENCE;
            else {
                content = SAME;
                evidence = PROVIDER_TOKEN;
                reasons |= PROVIDER_CONTENT_MATCH;
            }
        }
        if (content == UNKNOWN) reasons |= CONTENT_UNKNOWN;

        byte structure = UNKNOWN;
        if (before.manifestOrNull() != null && after.manifestOrNull() != null) {
            if (!sameStructureMethod(before, after)) reasons |= STRUCTURE_DOMAIN_CHANGED;
            else {
                structure = before.sameStructureReference(after) ? SAME : DIFFERENT;
                reasons |= structure == SAME ? STRUCTURE_ID_MATCH : STRUCTURE_ID_CHANGED;
            }
        }
        if (structure == UNKNOWN) reasons |= STRUCTURE_UNKNOWN;
        return new MFileDelta(before, after, presence, metadata, providerRevision, content, evidence, structure, reasons);
    }

    /** Validates and compares first, then invokes the consumer exactly once; its failure propagates. */
    public static MFileDelta between(MFileObservation before, MFileObservation after, Consumer<? super MFileDelta> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        MFileDelta result = between(before, after);
        consumer.accept(result);
        return result;
    }

    public MFileObservation before() { return before; }
    public MFileObservation after() { return after; }
    public State presence() { return state(presence); }

    /**
     * SAME requires equal known presence and, when present, known equal type,
     * size and modification time. Any observed difference is DIFFERENT; missing
     * fields otherwise yield UNKNOWN. This is independent of byte equality.
     */
    public State metadata() { return state(metadata); }
    public State providerRevision() { return state(providerRevision); }
    public State content() { return state(content); }
    public State structure() { return state(structure); }
    public ContentEvidence contentEvidence() {
        return switch (contentEvidence) {
            case FULL_CONTENT_ID -> ContentEvidence.FULL_CONTENT_ID;
            case PROVIDER_TOKEN -> ContentEvidence.AUTHORITATIVE_PROVIDER_TOKEN;
            default -> ContentEvidence.NONE;
        };
    }
    public long reasons() { return reasons; }

    /** Returns true if any of the supplied reason bits is present. */
    public boolean hasReason(long mask) { return (reasons & mask) != 0; }

    private static boolean both(int left, int right, int flag) { return (left & right & flag) != 0; }
    private static State state(byte value) {
        return switch (value) {
            case SAME -> State.SAME;
            case DIFFERENT -> State.DIFFERENT;
            default -> State.UNKNOWN;
        };
    }

    private static boolean sameStructureMethod(MFileObservation before, MFileObservation after) {
        if (!before.structureNameValue().equals(after.structureNameValue())
                || before.structureKindValue() != after.structureKindValue()) return false;
        M3PrecomputeKey left = before.structureKeyOrNull(), right = after.structureKeyOrNull();
        if (!left.type().equals(right.type()) || !left.algorithm().equals(right.algorithm())
                || left.algorithmVersion() != right.algorithmVersion()
                || !left.configuration().equals(right.configuration())) return false;
        List<M3ContentId> oldInputs = left.inputs(), newInputs = right.inputs();
        if (oldInputs.size() != newInputs.size()) return false;
        // Input zero is separately validated against each observation's full raw
        // content. It may differ when two raw revisions have the same structure.
        for (int i = 1; i < oldInputs.size(); i++) if (!oldInputs.get(i).equals(newInputs.get(i))) return false;
        return true;
    }
}
