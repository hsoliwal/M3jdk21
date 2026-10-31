// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

/**
 * How far a file-system scan descends at a time, the facade residency of the tree, and whether archives
 * found below a directory root become sub-trees.
 *
 * <p>Directories at {@code maxDepth} below the scan start are recorded as rows without children
 * ({@link M3FileFlag#SCANNED} clear); {@link M3FileTree#scanChildren(int)} expands one of them later with
 * the same depth. Ids stay stable across expansions; pre-order positions shift. With
 * {@code expandArchives}, files typed {@code zip/jar/war/ear} are listed from their central directory
 * (nested archives streamed) during the scan; a file that does not open as an archive stays a file.</p>
 *
 * @param maxDepth levels scanned below the start per scan ({@code Integer.MAX_VALUE} = eager)
 * @param residencyCapacity facade residency table size, a power of two
 * @param expandArchives list archives below a directory root as sub-trees
 */
public record M3FileScanSpec(int maxDepth, int residencyCapacity, boolean expandArchives) {
    public static final M3FileScanSpec EAGER = new M3FileScanSpec(Integer.MAX_VALUE, M3FileTree.DEFAULT_RESIDENCY);

    public M3FileScanSpec {
        if (maxDepth < 0) throw new IllegalArgumentException("negative maxDepth: " + maxDepth);
        if (residencyCapacity < 1 || Integer.bitCount(residencyCapacity) != 1) {
            throw new IllegalArgumentException("residency capacity must be a power of two: " + residencyCapacity);
        }
    }

    public M3FileScanSpec(int maxDepth, int residencyCapacity) {
        this(maxDepth, residencyCapacity, false);
    }

    /** Lazy spec: {@code depth} levels per scan, default residency. */
    public static M3FileScanSpec lazy(int depth) {
        return new M3FileScanSpec(depth, M3FileTree.DEFAULT_RESIDENCY);
    }

    /** This spec with archives below the root expanded. */
    public M3FileScanSpec withArchives() {
        return new M3FileScanSpec(maxDepth, residencyCapacity, true);
    }
}
