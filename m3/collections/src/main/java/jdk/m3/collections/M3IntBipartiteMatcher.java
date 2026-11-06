/*******************************************************************************
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
package jdk.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Hopcroft-Karp maximum cardinality matching over a frozen bipartite CSR graph.
 *
 * <p>All graph and search state is primitive arrays. Augmenting-path DFS is iterative.
 *
 * <p>Lineage: absorbed from Synexia {@code synexia-primitives/src/main/java/com/synexia/primitives/IntBipartiteMatcher.java}
 * blob {@code ff70d6cc7710ad481b3d6285e1b693ad68859c87} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_SYNEXIA_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3IntBipartiteMatcher {
    private static final int INF = Integer.MAX_VALUE;

    private final int leftCount;
    private final int rightCount;
    private final int[] offsets;
    private final int[] rights;

    private final int[] pairLeft;
    private final int[] pairRight;
    private final int[] distance;
    private final int[] queue;
    private final int[] stackLeft;
    private final int[] stackEdge;
    private final int[] stackChosenRight;
    private int matchingSize;

    public M3IntBipartiteMatcher(
            int leftCount, int rightCount, int[] leftVertices, int[] rightVertices) {
        if (leftCount < 0 || rightCount < 0) throw new IllegalArgumentException("negative partition size");
        Objects.requireNonNull(leftVertices, "leftVertices");
        Objects.requireNonNull(rightVertices, "rightVertices");
        if (leftVertices.length != rightVertices.length) {
            throw new IllegalArgumentException("edge lane length mismatch");
        }
        this.leftCount = leftCount;
        this.rightCount = rightCount;

        int[] degree = new int[leftCount];
        for (int i = 0; i < leftVertices.length; i++) {
            int left = leftVertices[i];
            int right = rightVertices[i];
            if (left < 0 || left >= leftCount) throw new IndexOutOfBoundsException("left=" + left);
            if (right < 0 || right >= rightCount) throw new IndexOutOfBoundsException("right=" + right);
            degree[left]++;
        }

        offsets = new int[leftCount + 1];
        for (int i = 0; i < leftCount; i++) offsets[i + 1] = offsets[i] + degree[i];
        rights = new int[leftVertices.length];
        int[] cursor = offsets.clone();
        for (int i = 0; i < leftVertices.length; i++) {
            rights[cursor[leftVertices[i]]++] = rightVertices[i];
        }

        pairLeft = new int[leftCount];
        pairRight = new int[rightCount];
        distance = new int[leftCount];
        queue = new int[leftCount];
        stackLeft = new int[leftCount];
        stackEdge = new int[leftCount];
        stackChosenRight = new int[leftCount];
        clearMatching();
    }

    public int leftCount() { return leftCount; }
    public int rightCount() { return rightCount; }
    public int edgeCount() { return rights.length; }
    public int matchingSize() { return matchingSize; }
    public long payloadBytes() {
        return (long) (
                offsets.length + rights.length + pairLeft.length + pairRight.length
                        + distance.length + queue.length + stackLeft.length
                        + stackEdge.length + stackChosenRight.length) * Integer.BYTES;
    }

    public int maximumMatching() {
        clearMatching();
        while (buildLayers()) {
            for (int left = 0; left < leftCount; left++) {
                if (pairLeft[left] < 0 && augment(left)) matchingSize++;
            }
        }
        return matchingSize;
    }

    public int matchedRight(int left) {
        if (left < 0 || left >= leftCount) throw new IndexOutOfBoundsException(left);
        return pairLeft[left];
    }

    public int matchedLeft(int right) {
        if (right < 0 || right >= rightCount) throw new IndexOutOfBoundsException(right);
        return pairRight[right];
    }

    private void clearMatching() {
        Arrays.fill(pairLeft, -1);
        Arrays.fill(pairRight, -1);
        matchingSize = 0;
    }

    private boolean buildLayers() {
        int head = 0;
        int tail = 0;
        for (int left = 0; left < leftCount; left++) {
            if (pairLeft[left] < 0) {
                distance[left] = 0;
                queue[tail++] = left;
            } else {
                distance[left] = INF;
            }
        }

        boolean freeRightReachable = false;
        while (head < tail) {
            int left = queue[head++];
            int nextDistance = distance[left] + 1;
            for (int edge = offsets[left]; edge < offsets[left + 1]; edge++) {
                int matchedLeft = pairRight[rights[edge]];
                if (matchedLeft < 0) {
                    freeRightReachable = true;
                } else if (distance[matchedLeft] == INF) {
                    distance[matchedLeft] = nextDistance;
                    queue[tail++] = matchedLeft;
                }
            }
        }
        return freeRightReachable;
    }

    private boolean augment(int start) {
        int depth = 0;
        stackLeft[0] = start;
        stackEdge[0] = offsets[start];

        while (depth >= 0) {
            int left = stackLeft[depth];
            int edge = stackEdge[depth];
            if (edge >= offsets[left + 1]) {
                distance[left] = INF;
                depth--;
                continue;
            }
            stackEdge[depth] = edge + 1;
            int right = rights[edge];
            int matchedLeft = pairRight[right];

            if (matchedLeft < 0) {
                pairLeft[left] = right;
                pairRight[right] = left;
                for (int level = depth - 1; level >= 0; level--) {
                    int ancestorLeft = stackLeft[level];
                    int pathRight = stackChosenRight[level];
                    pairLeft[ancestorLeft] = pathRight;
                    pairRight[pathRight] = ancestorLeft;
                }
                return true;
            }

            if (distance[matchedLeft] == distance[left] + 1) {
                stackChosenRight[depth] = right;
                stackLeft[++depth] = matchedLeft;
                stackEdge[depth] = offsets[matchedLeft];
            }
        }
        return false;
    }
}
