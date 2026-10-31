// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.Objects;

/**
 * Compact change mask between two immutable M3File observations.
 *
 * <p>Metadata is intentionally used as the cheap first gate. SHA-256 changes are authoritative
 * only when both snapshots contain a hash. HASH_UNKNOWN means metadata changed but exact content
 * identity was not available on both sides.</p>
 */
public record M3FileDelta(M3FileSnapshot before, M3FileSnapshot after, int mask) {
    public static final int TYPE = 1;
    public static final int SIZE = 1 << 1;
    public static final int MODIFIED_TIME = 1 << 2;
    public static final int CONTENT = 1 << 3;
    public static final int HASH_UNKNOWN = 1 << 4;

    public M3FileDelta {
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        if ((mask & ~(TYPE | SIZE | MODIFIED_TIME | CONTENT | HASH_UNKNOWN)) != 0) {
            throw new IllegalArgumentException("unknown delta mask bits");
        }
    }

    static M3FileDelta between(M3FileSnapshot before, M3FileSnapshot after) {
        int changes = 0;
        M3File.Metadata left = before.metadata();
        M3File.Metadata right = after.metadata();

        if (left.regularFile() != right.regularFile()
                || left.directory() != right.directory()
                || left.symbolicLink() != right.symbolicLink()) {
            changes |= TYPE;
        }
        if (left.size() != right.size()) changes |= SIZE;
        if (!left.lastModified().equals(right.lastModified())) changes |= MODIFIED_TIME;

        if (before.hasContentHash() && after.hasContentHash()) {
            if (!before.contentHashEquals(after)) changes |= CONTENT;
        } else if ((changes & (TYPE | SIZE | MODIFIED_TIME)) != 0) {
            changes |= HASH_UNKNOWN;
        }
        return new M3FileDelta(before, after, changes);
    }

    public boolean changed() {
        return mask != 0;
    }

    public boolean exactContentChanged() {
        return (mask & CONTENT) != 0;
    }

    public boolean exactContentUnchanged() {
        return before.hasContentHash() && after.hasContentHash() && (mask & CONTENT) == 0;
    }

    public boolean requiresContentProbe() {
        return (mask & HASH_UNKNOWN) != 0;
    }
}
