// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

/**
 * Compiled numeric first-pass predicate.
 *
 * <p>Feature bits are lossy and may admit false positives. Exact hash/identity checks are still
 * first-pass checks: callers must use an exact verifier before treating a candidate as a semantic
 * match unless the raw identity itself is the complete semantic identity.</p>
 */
//FLLDCIM: RELOCATE - donor blob aa475998e712 at commit 3385d79a2968 (repository, path, URL and digests: m3/collections/qualification/object-signal-20261107/PROVENANCE.tsv); recipe m3/collections/qualification/object-signal-20261107/relocate.py v1
public record ObjectSignalQuery(
        int kindOrdinal,
        int domain,
        int minLength,
        int maxLength,
        long requiredBits,
        long forbiddenBits,
        long exactHash64,
        long exactIdentity,
        int requirements) {

    public static final int ANY_KIND = -1;
    public static final int ANY_DOMAIN = -1;
    public static final int UNBOUNDED = -1;

    public static final int REQUIRE_HASH = 1;
    public static final int REQUIRE_IDENTITY = 1 << 1;

    public ObjectSignalQuery {
        if (kindOrdinal < ANY_KIND || kindOrdinal >= ObjectProjectionKind.values().length) {
            throw new IllegalArgumentException("invalid kind ordinal");
        }
        if (domain < ANY_DOMAIN) throw new IllegalArgumentException("invalid domain");
        if (minLength < UNBOUNDED || maxLength < UNBOUNDED) {
            throw new IllegalArgumentException("invalid length bound");
        }
        if (minLength >= 0 && maxLength >= 0 && maxLength < minLength) {
            throw new IllegalArgumentException("maxLength < minLength");
        }
        if ((requiredBits & forbiddenBits) != 0L) {
            throw new IllegalArgumentException("same signal bit cannot be required and forbidden");
        }
    }

    public static ObjectSignalQuery exact(ObjectSignal target) {
        int length = target.hasLength() ? target.logicalLength() : UNBOUNDED;
        int requirements = 0;
        if (target.hasHash()) requirements |= REQUIRE_HASH;
        if (target.hasIdentity()) requirements |= REQUIRE_IDENTITY;
        return new ObjectSignalQuery(
            target.kind().ordinal(),
            target.domain(),
            length,
            length,
            target.featureBits(),
            0L,
            target.exactHash64(),
            target.rawIdentity(),
            requirements);
    }

    /**
     * Necessary-condition query for literal containment. The feature extractor is constructed so
     * a true String/byte containment relation cannot be rejected by the feature mask.
     */
    public static ObjectSignalQuery maybeContains(ObjectSignal needle) {
        return new ObjectSignalQuery(
            needle.kind().ordinal(),
            needle.domain(),
            needle.hasLength() ? needle.logicalLength() : UNBOUNDED,
            UNBOUNDED,
            needle.featureBits(),
            0L,
            0L,
            0L,
            0);
    }

    public static ObjectSignalQuery mask(int domain, long requiredBits, long forbiddenBits) {
        return new ObjectSignalQuery(
            ANY_KIND,
            domain,
            UNBOUNDED,
            UNBOUNDED,
            requiredBits,
            forbiddenBits,
            0L,
            0L,
            0);
    }

    public static ObjectSignalQuery anyMask(long requiredBits, long forbiddenBits) {
        return mask(ANY_DOMAIN, requiredBits, forbiddenBits);
    }

    boolean metadataMatches(
            byte candidateKind,
            int candidateDomain,
            int candidateLength,
            int candidateFlags,
            long candidateHash,
            long candidateIdentity) {
        if (kindOrdinal != ANY_KIND && candidateKind != (byte) kindOrdinal) return false;
        if (domain != ANY_DOMAIN && candidateDomain != domain) return false;

        if (minLength >= 0) {
            if ((candidateFlags & ObjectSignal.HAS_LENGTH) == 0 || candidateLength < minLength) return false;
        }
        if (maxLength >= 0) {
            if ((candidateFlags & ObjectSignal.HAS_LENGTH) == 0 || candidateLength > maxLength) return false;
        }
        if ((requirements & REQUIRE_HASH) != 0) {
            if ((candidateFlags & ObjectSignal.HAS_HASH) == 0 || candidateHash != exactHash64) return false;
        }
        if ((requirements & REQUIRE_IDENTITY) != 0) {
            if ((candidateFlags & ObjectSignal.HAS_IDENTITY) == 0 || candidateIdentity != exactIdentity) return false;
        }
        return true;
    }
}
