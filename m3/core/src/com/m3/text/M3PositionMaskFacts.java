/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Metadata-only receiver for Synexia's immutable UTF-16 position-mask projection.
 *
 * <p>The donor owns the image bytes and the Java/JNI execution. This target record
 * preserves the source revision, geometry, digest, and measured AUTO route policy
 * without copying a mask image into M3JDK or claiming native execution.</p>
 */
public final class M3PositionMaskFacts {
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final int SCHEMA_VERSION = 1;

    private final String sourceRevision;
    private final int rowCount;
    private final int blockCount;
    private final int entryCount;
    private final long primitivePayloadBytes;
    private final String rootHash;
    private final int autoNativeHistogramMaxRows;
    private final int nativeSlabRows;

    private M3PositionMaskFacts(
            String sourceRevision,
            int rowCount,
            int blockCount,
            int entryCount,
            long primitivePayloadBytes,
            String rootHash,
            int autoNativeHistogramMaxRows,
            int nativeSlabRows) {
        this.sourceRevision = sourceRevision;
        this.rowCount = rowCount;
        this.blockCount = blockCount;
        this.entryCount = entryCount;
        this.primitivePayloadBytes = primitivePayloadBytes;
        this.rootHash = rootHash;
        this.autoNativeHistogramMaxRows = autoNativeHistogramMaxRows;
        this.nativeSlabRows = nativeSlabRows;
    }

    /**
     * Admits only validated metadata from the source-owned projection.
     *
     * <p>This method does not admit image bytes, JNI handles, or candidate results.</p>
     */
    public static M3PositionMaskFacts fromPrecomputed(
            String sourceRevision,
            int rowCount,
            int blockCount,
            int entryCount,
            long primitivePayloadBytes,
            String rootHash,
            int autoNativeHistogramMaxRows,
            int nativeSlabRows) {
        String revision = text(sourceRevision, "sourceRevision");
        String digest = text(rootHash, "rootHash");
        if (!SHA256.matcher(digest).matches()) {
            throw new IllegalArgumentException("rootHash must be lowercase SHA-256 hex");
        }
        if (rowCount < 0 || blockCount < 0 || entryCount < 0 || primitivePayloadBytes < 0) {
            throw new IllegalArgumentException("negative position-mask geometry");
        }
        if (autoNativeHistogramMaxRows <= 0 || nativeSlabRows <= 0) {
            throw new IllegalArgumentException("non-positive route geometry");
        }
        long expectedPayloadBytes = 68L
                + 8L * rowCount
                + 4L * blockCount
                + 10L * entryCount;
        if (entryCount > 64L * blockCount
                || primitivePayloadBytes != expectedPayloadBytes
                || expectedPayloadBytes > Integer.MAX_VALUE - 8L) {
            throw new IllegalArgumentException("inconsistent position-mask image geometry");
        }
        return new M3PositionMaskFacts(
                revision,
                rowCount,
                blockCount,
                entryCount,
                primitivePayloadBytes,
                digest,
                autoNativeHistogramMaxRows,
                nativeSlabRows);
    }

    public int schemaVersion() {
        return SCHEMA_VERSION;
    }

    public String sourceRevision() {
        return sourceRevision;
    }

    public int rowCount() {
        return rowCount;
    }

    public int blockCount() {
        return blockCount;
    }

    public int entryCount() {
        return entryCount;
    }

    public long primitivePayloadBytes() {
        return primitivePayloadBytes;
    }

    public String rootHash() {
        return rootHash;
    }

    public int autoNativeHistogramMaxRows() {
        return autoNativeHistogramMaxRows;
    }

    public int nativeSlabRows() {
        return nativeSlabRows;
    }

    /**
     * Returns the measured native candidate policy without claiming native availability.
     */
    public boolean autoNativeHistogramCandidate(int selectedRows) {
        if (selectedRows < 0) {
            throw new IllegalArgumentException("negative selected row count");
        }
        return selectedRows > 0 && selectedRows <= autoNativeHistogramMaxRows;
    }

    public boolean metadataOnly() {
        return true;
    }

    private static String text(String value, String name) {
        String checked = Objects.requireNonNull(value, name + " is null");
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(name + " is empty");
        }
        return checked;
    }
}
