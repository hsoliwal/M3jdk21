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
import java.util.NoSuchElementException;

/**
 * Dynamic Li-Chao line envelope over an inclusive long coordinate domain.
 *
 * <p>Each allocated logical node is an integer index into parallel slope/intercept/child arrays.
 * No Java node object is retained. Arithmetic overflow throws.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/LongLiChaoTree.java}
 * blob {@code bf3eb06aa7af44a07762f2bc21915048112aa8c5} at com.synexia develop {@code ded4a4acdffe5a27e1f2cedb02126762816b9850} by
 * RELOCATE_M3_OWNER v2 (identifiers only; bytecode equal to the source modulo names).
 */
public final class LongLiChaoTree {
    public enum Mode { MIN, MAX }

    private final long minX;
    private final long maxX;
    private final Mode mode;

    private long[] slopes = new long[16];
    private long[] intercepts = new long[16];
    private int[] leftChild = new int[16];
    private int[] rightChild = new int[16];
    private int root;
    private int nodeCount = 1; // zero is null sentinel.
    private int lineCount;

    public LongLiChaoTree(long minX, long maxX, Mode mode) {
        if (minX > maxX) throw new IllegalArgumentException("minX > maxX");
        this.minX = minX;
        this.maxX = maxX;
        this.mode = java.util.Objects.requireNonNull(mode, "mode");
    }

    public long minX() { return minX; }
    public long maxX() { return maxX; }
    public int lineCount() { return lineCount; }
    public int nodeCount() { return nodeCount - 1; }

    public long payloadBytes() {
        return (long) (slopes.length + intercepts.length) * Long.BYTES
                + (long) (leftChild.length + rightChild.length) * Integer.BYTES;
    }

    public void addLine(long slope, long intercept) {
        lineCount++;
        if (root == 0) {
            root = allocate(slope, intercept);
            return;
        }
        root = insert(root, minX, maxX, slope, intercept);
    }

    public long query(long x) {
        if (x < minX || x > maxX) throw new IndexOutOfBoundsException(Long.toString(x));
        if (root == 0) throw new NoSuchElementException("no lines");
        long best = mode == Mode.MIN ? Long.MAX_VALUE : Long.MIN_VALUE;
        int node = root;
        long left = minX;
        long right = maxX;
        while (node != 0) {
            long value = evaluate(slopes[node], intercepts[node], x);
            if (better(value, best)) best = value;
            if (left == right) break;
            long mid = midpoint(left, right);
            if (x <= mid) {
                node = leftChild[node];
                right = mid;
            } else {
                node = rightChild[node];
                left = mid + 1L;
            }
        }
        return best;
    }

    private int insert(
            int node,
            long left,
            long right,
            long slope,
            long intercept) {
        long mid = midpoint(left, right);
        long currentAtLeft = evaluate(slopes[node], intercepts[node], left);
        long incomingAtLeft = evaluate(slope, intercept, left);
        long currentAtMid = evaluate(slopes[node], intercepts[node], mid);
        long incomingAtMid = evaluate(slope, intercept, mid);

        boolean leftBetter = better(incomingAtLeft, currentAtLeft);
        boolean midBetter = better(incomingAtMid, currentAtMid);

        if (midBetter) {
            long oldSlope = slopes[node];
            long oldIntercept = intercepts[node];
            slopes[node] = slope;
            intercepts[node] = intercept;
            slope = oldSlope;
            intercept = oldIntercept;
        }

        if (left == right) return node;

        // Allocation may replace the child lanes; resolve the destination after insertion.
        if (leftBetter != midBetter) {
            int child = leftChild[node];
            if (child == 0) {
                child = allocate(slope, intercept);
            } else {
                child = insert(child, left, mid, slope, intercept);
            }
            leftChild[node] = child;
        } else {
            long nextLeft = mid + 1L;
            int child = rightChild[node];
            if (child == 0) {
                child = allocate(slope, intercept);
            } else {
                child = insert(child, nextLeft, right, slope, intercept);
            }
            rightChild[node] = child;
        }
        return node;
    }

    private int allocate(long slope, long intercept) {
        ensure(nodeCount + 1);
        int node = nodeCount++;
        slopes[node] = slope;
        intercepts[node] = intercept;
        return node;
    }

    private void ensure(int required) {
        if (required <= slopes.length) return;
        int next = Math.max(required, slopes.length + (slopes.length >>> 1) + 1);
        slopes = Arrays.copyOf(slopes, next);
        intercepts = Arrays.copyOf(intercepts, next);
        leftChild = Arrays.copyOf(leftChild, next);
        rightChild = Arrays.copyOf(rightChild, next);
    }

    private long evaluate(long slope, long intercept, long x) {
        return Math.addExact(Math.multiplyExact(slope, x), intercept);
    }

    private boolean better(long left, long right) {
        return mode == Mode.MIN ? left < right : left > right;
    }

    /** Overflow-safe floor average. */
    private static long midpoint(long left, long right) {
        return (left & right) + ((left ^ right) >> 1);
    }
}
