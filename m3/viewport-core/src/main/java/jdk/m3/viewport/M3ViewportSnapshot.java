/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.viewport;

import java.util.Objects;

/**
 * Immutable array-backed resident viewport, detached from every toolkit widget.
 * The index within this object is a resident-window index, not a logical index.
 * No mutable backing lane is exposed; the small resident window is copied once
 * at the CPU/UI ownership boundary.
 */
public final class M3ViewportSnapshot {
    private final long modelVersion;
    private final long firstLogicalRow;
    private final int visibleOffset;
    private final int visibleCount;
    private final long[] logicalIds;
    private final int[] topPixels;
    private final int[] heights;
    private final long[] stateMasks;

    public M3ViewportSnapshot(long modelVersion, long firstLogicalRow,
            int visibleOffset, int visibleCount, long[] logicalIds,
            int[] topPixels, int[] heights, long[] stateMasks) {
        Objects.requireNonNull(logicalIds, "logicalIds");
        Objects.requireNonNull(topPixels, "topPixels");
        Objects.requireNonNull(heights, "heights");
        Objects.requireNonNull(stateMasks, "stateMasks");
        int length = logicalIds.length;
        if (modelVersion < 0 || firstLogicalRow < 0 ||
                topPixels.length != length || heights.length != length ||
                stateMasks.length != length || visibleOffset < 0 ||
                visibleOffset > length || visibleCount < 0 ||
                visibleCount > length - visibleOffset) {
            throw new IllegalArgumentException("invalid viewport window");
        }
        Math.addExact(firstLogicalRow, length);
        for (int height : heights) {
            if (height < 0) throw new IllegalArgumentException("negative row height");
        }
        this.modelVersion = modelVersion;
        this.firstLogicalRow = firstLogicalRow;
        this.visibleOffset = visibleOffset;
        this.visibleCount = visibleCount;
        this.logicalIds = logicalIds.clone();
        this.topPixels = topPixels.clone();
        this.heights = heights.clone();
        this.stateMasks = stateMasks.clone();
    }

    public long modelVersion() { return modelVersion; }
    public long firstLogicalRow() { return firstLogicalRow; }
    public int rowCount() { return logicalIds.length; }
    public int visibleOffset() { return visibleOffset; }
    public int visibleCount() { return visibleCount; }
    public long logicalId(int residentIndex) { return logicalIds[residentIndex]; }
    public int topPixel(int residentIndex) { return topPixels[residentIndex]; }
    public int height(int residentIndex) { return heights[residentIndex]; }
    public long stateMask(int residentIndex) { return stateMasks[residentIndex]; }

    /** Fast transfer into caller-owned scratch; no array aliases escape. */
    public void copyRows(int from, int count, long[] ids, int[] y,
            int[] extents, long[] states, int destinationOffset) {
        Objects.requireNonNull(ids, "ids");
        Objects.requireNonNull(y, "y");
        Objects.requireNonNull(extents, "extents");
        Objects.requireNonNull(states, "states");
        if (from < 0 || count < 0 || from > rowCount() - count ||
                destinationOffset < 0 || destinationOffset > ids.length - count ||
                destinationOffset > y.length - count ||
                destinationOffset > extents.length - count ||
                destinationOffset > states.length - count) {
            throw new IndexOutOfBoundsException("invalid viewport copy range");
        }
        System.arraycopy(logicalIds, from, ids, destinationOffset, count);
        System.arraycopy(topPixels, from, y, destinationOffset, count);
        System.arraycopy(heights, from, extents, destinationOffset, count);
        System.arraycopy(stateMasks, from, states, destinationOffset, count);
    }
}
