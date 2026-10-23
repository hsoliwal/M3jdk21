// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.Arrays;
import java.util.Objects;

/** Immutable metadata-first observation of one M3File, optionally carrying exact SHA-256 content. */
public final class M3FileSnapshot {
    private final M3File.Metadata metadata;
    private final byte[] sha256;

    M3FileSnapshot(M3File.Metadata metadata, byte[] sha256) {
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        if (sha256 != null && sha256.length != 32) {
            throw new IllegalArgumentException("SHA-256 must contain 32 bytes");
        }
        this.sha256 = sha256 == null ? null : sha256.clone();
    }

    public M3File.Metadata metadata() {
        return metadata;
    }

    public boolean hasContentHash() {
        return sha256 != null;
    }

    public byte[] contentSha256() {
        if (sha256 == null) throw new IllegalStateException("snapshot has no content hash");
        return sha256.clone();
    }

    boolean contentHashEquals(M3FileSnapshot other) {
        return sha256 != null && other.sha256 != null && Arrays.equals(sha256, other.sha256);
    }

    public M3FileDelta deltaTo(M3FileSnapshot next) {
        return M3FileDelta.between(this, Objects.requireNonNull(next, "next"));
    }
}
