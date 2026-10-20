// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

/**
 * How far a file-system scan descends at a time, and the facade residency of the tree.
 *
 * <p>Directories at {@code maxDepth} below the scan start are recorded as rows without children
 * ({@link M3FileFlag#SCANNED} clear); {@link M3FileTree#scanChildren(int)} expands one of them later with
 * the same depth. Ids stay stable across expansions; pre-order positions shift.</p>
 *
 * @param maxDepth levels scanned below the start per scan ({@code Integer.MAX_VALUE} = eager)
 * @param residencyCapacity facade residency table size, a power of two
 */
public record M3FileScanSpec(int maxDepth, int residencyCapacity) {
    public static final M3FileScanSpec EAGER = new M3FileScanSpec(Integer.MAX_VALUE, M3FileTree.DEFAULT_RESIDENCY);

    public M3FileScanSpec {
        if (maxDepth < 0) throw new IllegalArgumentException("negative maxDepth: " + maxDepth);
        if (residencyCapacity < 1 || Integer.bitCount(residencyCapacity) != 1) {
            throw new IllegalArgumentException("residency capacity must be a power of two: " + residencyCapacity);
        }
    }

    /** Lazy spec: {@code depth} levels per scan, default residency. */
    public static M3FileScanSpec lazy(int depth) {
        return new M3FileScanSpec(depth, M3FileTree.DEFAULT_RESIDENCY);
    }
}
