// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import java.util.Objects;

/**
 * One object's numeric first-pass projection.
 *
 * <p>The source object's raw identity is never rewritten. Masks/features are separate derived
 * signals, so admission/rejection cannot corrupt or reinterpret caller-visible IDs.</p>
 */
//FLLDCIM: RELOCATE - donor blob 90323e1c792f at commit 3385d79a2968 (repository, path, URL and digests: m3/collections/qualification/object-signal-20261107/PROVENANCE.tsv); recipe m3/collections/qualification/object-signal-20261107/relocate.py v1
public record ObjectSignal(
        ObjectProjectionKind kind,
        int domain,
        int logicalLength,
        long exactHash64,
        long featureBits,
        long rawIdentity,
        int flags) {

    public static final int HAS_LENGTH = 1;
    public static final int HAS_HASH = 1 << 1;
    public static final int HAS_IDENTITY = 1 << 2;

    public ObjectSignal {
        Objects.requireNonNull(kind, "kind");
        if (domain < 0) throw new IllegalArgumentException("domain must be non-negative");
        if (logicalLength < -1) throw new IllegalArgumentException("logicalLength must be -1 or non-negative");
        if ((flags & HAS_LENGTH) != 0 && logicalLength < 0) {
            throw new IllegalArgumentException("HAS_LENGTH requires a non-negative logicalLength");
        }
    }

    public boolean hasLength() { return (flags & HAS_LENGTH) != 0; }
    public boolean hasHash() { return (flags & HAS_HASH) != 0; }
    public boolean hasIdentity() { return (flags & HAS_IDENTITY) != 0; }
}
