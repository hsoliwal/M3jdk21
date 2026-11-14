// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.file;
//FLLDCIM: MRL - ADAPT HOIST_DEFAULT_RESIDENCY - source m3/qualification/master-landing-20261028/retained-variants/pr1619/m3/file/src/main/java/jdk/m3/file/M3FileScanSpec.java blob dcd5d77e7f6c925f9292c77db8f5ecb8aee00e13 (retained PR #1619 head; parent 0cd0944136c1 blob b5dc653926e55d74d5d92b1a29b007effded1c53 https://github.com/hsoliwal/M3jdk21/blob/0cd0944136c1/m3/file/src/main/java/com/m3/file/M3FileScanSpec.java); recipe m3/file/qualification/tree-lanes-port-20261107 ADAPT:HOIST_DEFAULT_RESIDENCY

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
    //FLLDCIM: MRL - ADAPT HOIST_DEFAULT_RESIDENCY: constant DEFAULT_RESIDENCY = 4096 of the held tree owner (m3/qualification/master-landing-20261028/retained-variants/pr1619/m3/file/src/main/java/jdk/m3/file/M3FileTree.java blob 4191310ae3d76ecf9c65125b47ab226036f17e20, retained PR #1619 head) hoisted into the spec; both references rewritten; recipe m3/file/qualification/tree-lanes-port-20261107
    /** Default facade residency table size (hoisted from the held tree owner). */
    public static final int DEFAULT_RESIDENCY = 4096;

    public static final M3FileScanSpec EAGER = new M3FileScanSpec(Integer.MAX_VALUE, DEFAULT_RESIDENCY);

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
        return new M3FileScanSpec(depth, DEFAULT_RESIDENCY);
    }

    /** This spec with archives below the root expanded. */
    public M3FileScanSpec withArchives() {
        return new M3FileScanSpec(maxDepth, residencyCapacity, true);
    }
}
