/* *****************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.m3.collections.range;

import java.util.Arrays;
import java.util.Objects;

/**
 * Append-only persistent point-update/range-sum tree over [0,size).
 *
 * <p>Each logical tree node is an integer index into parallel left/right/sum
 * arrays. Version 0 is the all-zero tree. Updating any prior version appends
 * only O(log n) primitive nodes and one root integer.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PersistentLongSumTree.java}
 * blob {@code 04193577670e9dcd328880830774dcc03252a6e0} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class PersistentLongSumTree {
    private final int size;
    private int[] roots;
    private int versionCount = 1;

    private int[] left;
    private int[] right;
    private long[] sums;
    private int nodeCount = 1; // node 0 is the implicit all-zero sentinel.

    //FLLDCIM: MRL - census3 P3-009, donor MIndexPersistentLongSumTree blob cc78be1a0a82: forkAdd path lanes, presized by the restored constructor and allocated by forkAdd otherwise
    private int[] scratchNodes;
    private long[] scratchSums;
    private boolean[] scratchRight;

    public PersistentLongSumTree(int size) {
        if (size < 0) throw new IllegalArgumentException("size < 0");
        this.size = size;
        roots = new int[8];
        left = new int[16];
        right = new int[16];
        sums = new long[16];
    }

    //FLLDCIM: MRL - census3 P3-009, donor MIndexPersistentLongSumTree blob cc78be1a0a82: presizing constructor (positive length, expected additional versions)
    public PersistentLongSumTree(int length, int expectedAdditionalVersions) {
        if (length <= 0) {
            throw new IllegalArgumentException("length must be positive");
        }
        if (expectedAdditionalVersions < 0) {
            throw new IllegalArgumentException("expectedAdditionalVersions");
        }
        this.size = length;

        int pathNodes = pathNodeCount(length);
        scratchNodes = new int[pathNodes];
        scratchSums = new long[pathNodes];
        scratchRight = new boolean[pathNodes];

        roots = new int[Math.max(1, Math.addExact(expectedAdditionalVersions, 1))];

        long estimatedNodes = 1L
                + (long) Math.max(1, expectedAdditionalVersions) * pathNodes;
        int initialNodes = (int) Math.min(
                PackedCapacity.MAX,
                Math.max(8L, estimatedNodes));
        left = new int[initialNodes];
        right = new int[initialNodes];
        sums = new long[initialNodes];
    }

    public int size() { return size; }
    //FLLDCIM: MRL - census3 P3-009, donor MIndexPersistentLongSumTree blob cc78be1a0a82: donor name of size()
    public int length() { return size; }
    public int versionCount() { return versionCount; }
    public int nodeCount() { return nodeCount; }
    public long payloadBytes() {
        return (long) (roots.length + left.length + right.length) * Integer.BYTES
                + (long) sums.length * Long.BYTES;
    }
    public long usedPayloadBytes() {
        return (long) versionCount * Integer.BYTES
                + (long) nodeCount * (Integer.BYTES * 2L + Long.BYTES);
    }

    public int set(int baseVersion, int index, long value) {
        checkVersion(baseVersion);
        checkIndex(index);
        int root = update(roots[baseVersion], 0, size, index, value);
        return appendVersion(root);
    }

    public int add(int baseVersion, int index, long delta) {
        long current = rangeSum(baseVersion, index, index + 1);
        return set(baseVersion, index, current + delta);
    }

    public long get(int version, int index) {
        checkIndex(index);
        return rangeSum(version, index, index + 1);
    }

    public long rangeSum(int version, int fromInclusive, int toExclusive) {
        checkVersion(version);
        checkRange(fromInclusive, toExclusive);
        if (fromInclusive == toExclusive || size == 0) return 0L;
        return query(roots[version], 0, size, fromInclusive, toExclusive);
    }

    public long total(int version) {
        checkVersion(version);
        return sums[roots[version]];
    }

    /**
     * Forks {@code baseVersion} and adds {@code delta} to one position.
     *
     * <p>Arithmetic is validated along the complete path before any version/node state changes.
     * A zero delta shares the base root.
     */
    //FLLDCIM: MRL - census3 P3-009, donor MIndexPersistentLongSumTree blob cc78be1a0a82: checked fork-add
    public int forkAdd(int baseVersion, int index, long delta) {
        checkVersion(baseVersion);
        Objects.checkIndex(index, size);

        ensureRootCapacity(Math.addExact(versionCount, 1));
        if (delta == 0L) {
            roots[versionCount] = roots[baseVersion];
            return versionCount++;
        }
        if (scratchNodes == null) {
            int pathNodes = pathNodeCount(size);
            scratchNodes = new int[pathNodes];
            scratchSums = new long[pathNodes];
            scratchRight = new boolean[pathNodes];
        }

        int oldNode = roots[baseVersion];
        int from = 0;
        int to = size;
        int depth = 0;

        while (true) {
            scratchNodes[depth] = oldNode;
            scratchSums[depth] = Math.addExact(sums[oldNode], delta);
            if (to - from == 1) {
                break;
            }
            int middle = (from + to) >>> 1;
            boolean goRight = index >= middle;
            scratchRight[depth] = goRight;
            if (goRight) {
                oldNode = right[oldNode];
                from = middle;
            } else {
                oldNode = left[oldNode];
                to = middle;
            }
            depth++;
        }

        ensureNodeCapacity(Math.addExact(nodeCount, depth + 1));

        int newChild = 0;
        for (int level = depth; level >= 0; level--) {
            int source = scratchNodes[level];
            int created = nodeCount++;
            left[created] = left[source];
            right[created] = right[source];
            sums[created] = scratchSums[level];

            if (level < depth) {
                if (scratchRight[level]) {
                    right[created] = newChild;
                } else {
                    left[created] = newChild;
                }
            }
            newChild = created;
        }

        roots[versionCount] = newChild;
        return versionCount++;
    }

    //FLLDCIM: MRL - census3 P3-009, donor MIndexPersistentLongSumTree blob cc78be1a0a82: root node id of a version
    public int rootNode(int version) {
        checkVersion(version);
        return roots[version];
    }

    private int update(int node, int leftBound, int rightBound, int index, long value) {
        int copy = allocateNode(left[node], right[node], sums[node]);
        if (rightBound - leftBound == 1) {
            sums[copy] = value;
            return copy;
        }

        int mid = (leftBound + rightBound) >>> 1;
        if (index < mid) {
            left[copy] = update(left[copy], leftBound, mid, index, value);
        } else {
            right[copy] = update(right[copy], mid, rightBound, index, value);
        }
        sums[copy] = sums[left[copy]] + sums[right[copy]];
        return copy;
    }

    private long query(
            int node, int leftBound, int rightBound,
            int from, int to) {
        if (node == 0 || to <= leftBound || rightBound <= from) return 0L;
        if (from <= leftBound && rightBound <= to) return sums[node];
        int mid = (leftBound + rightBound) >>> 1;
        return query(left[node], leftBound, mid, from, to)
                + query(right[node], mid, rightBound, from, to);
    }

    private int allocateNode(int leftNode, int rightNode, long sum) {
        ensureNodeCapacity(nodeCount + 1);
        int node = nodeCount++;
        left[node] = leftNode;
        right[node] = rightNode;
        sums[node] = sum;
        return node;
    }

    private int appendVersion(int root) {
        ensureRootCapacity(versionCount + 1);
        roots[versionCount] = root;
        return versionCount++;
    }

    private void ensureNodeCapacity(int required) {
        if (required <= left.length) return;
        int next = Math.max(required, left.length + (left.length >>> 1) + 1);
        left = Arrays.copyOf(left, next);
        right = Arrays.copyOf(right, next);
        sums = Arrays.copyOf(sums, next);
    }

    private void ensureRootCapacity(int required) {
        if (required <= roots.length) return;
        int next = Math.max(required, roots.length + (roots.length >>> 1) + 1);
        roots = Arrays.copyOf(roots, next);
    }

    private void checkVersion(int version) {
        if (version < 0 || version >= versionCount) {
            throw new IndexOutOfBoundsException("version=" + version);
        }
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException(index);
    }

    private void checkRange(int fromInclusive, int toExclusive) {
        if (fromInclusive < 0 || fromInclusive > toExclusive || toExclusive > size) {
            throw new IndexOutOfBoundsException(fromInclusive + ".." + toExclusive);
        }
    }

    //FLLDCIM: MRL - census3 P3-009, donor MIndexPersistentLongSumTree blob cc78be1a0a82: root-to-leaf path length
    private static int pathNodeCount(int length) {
        int nodes = 1;
        long span = 1L;
        while (span < length) {
            span <<= 1;
            nodes++;
        }
        return nodes;
    }

    /** Local array-capacity bound of the presizing constructor, keeping this kernel JDK-only. */
    //FLLDCIM: MRL - census3 P3-009, donor MIndexPersistentLongSumTree blob cc78be1a0a82: PackedCapacity (grow is PRESENT_EVOLVED as ensureNodeCapacity/ensureRootCapacity)
    private static final class PackedCapacity {
        private static final int MAX = Integer.MAX_VALUE - 8;

        private PackedCapacity() {}
    }
}
