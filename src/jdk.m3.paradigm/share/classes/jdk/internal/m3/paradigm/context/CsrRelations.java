/* ******************************************************************************
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
package jdk.internal.m3.paradigm.context;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable compressed-sparse-row context relation graph with primitive traversal.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextCsrRelations.java}
 * blob {@code 988c036a15cf12e82bd1ec97ed816dae067793e2} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class CsrRelations {
    private final int[] offsets;
    private final int[] targets;
    private final byte[] kinds;

    private CsrRelations(int[] offsets, int[] targets, byte[] kinds) {
        this.offsets = offsets;
        this.targets = targets;
        this.kinds = kinds;
    }

    public static CsrRelations of(
            int nodeCount,
            int[] from,
            int[] to,
            byte[] relationKinds) {
        if (nodeCount < 0) {
            throw new IllegalArgumentException("nodeCount must be nonnegative");
        }
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(relationKinds, "relationKinds");
        if (from.length != to.length || from.length != relationKinds.length) {
            throw new IllegalArgumentException("edge arrays must have equal length");
        }
        int[] offsets = new int[nodeCount + 1];
        for (int source : from) {
            offsets[Objects.checkIndex(source, nodeCount) + 1]++;
        }
        for (int i = 1; i < offsets.length; i++) {
            offsets[i] = Math.addExact(offsets[i], offsets[i - 1]);
        }
        int[] cursor = offsets.clone();
        int[] targets = new int[to.length];
        byte[] kinds = new byte[to.length];
        for (int i = 0; i < from.length; i++) {
            Objects.checkIndex(to[i], nodeCount);
            int slot = cursor[from[i]]++;
            targets[slot] = to[i];
            kinds[slot] = relationKinds[i];
        }
        return new CsrRelations(offsets, targets, kinds);
    }

    public int nodeCount() { return offsets.length - 1; }
    public int edgeCount() { return targets.length; }

    public int degree(int node) {
        Objects.checkIndex(node, nodeCount());
        return offsets[node + 1] - offsets[node];
    }

    public int targetAt(int node, int localEdge) {
        Objects.checkIndex(node, nodeCount());
        int start = offsets[node];
        int degree = offsets[node + 1] - start;
        return targets[start + Objects.checkIndex(localEdge, degree)];
    }

    public int kindOrdinalAt(int node, int localEdge) {
        Objects.checkIndex(node, nodeCount());
        int start = offsets[node];
        int degree = offsets[node + 1] - start;
        return Byte.toUnsignedInt(kinds[start + Objects.checkIndex(localEdge, degree)]);
    }

    public <E extends Enum<E>> E kindAt(Class<E> type, int node, int localEdge) {
        E[] universe = Objects.requireNonNull(type, "type").getEnumConstants();
        int ordinal = kindOrdinalAt(node, localEdge);
        if (universe == null || ordinal >= universe.length) {
            throw new IllegalArgumentException("relation kind universe");
        }
        return universe[ordinal];
    }

    /** One bit per relation-kind ordinal. Ordinals 0..63 are supported. */
    public static long relationMask(Enum<?>... kinds) {
        long mask = 0L;
        for (Enum<?> kind : kinds) {
            int ordinal = Objects.requireNonNull(kind, "kind").ordinal();
            if (ordinal >= Long.SIZE) {
                throw new IllegalArgumentException("relation enum exceeds 64 kinds");
            }
            mask |= 1L << ordinal;
        }
        return mask;
    }

    public int[] breadthFirst(int start, long allowedKinds, int maximum) {
        Objects.checkIndex(start, nodeCount());
        requireMaximum(maximum);
        int[] queue = new int[Math.min(nodeCount(), maximum)];
        int head = 0;
        int tail = 0;
        long[] visited = new long[(nodeCount() + 63) >>> 6];
        mark(visited, start);
        queue[tail++] = start;
        while (head < tail) {
            int node = queue[head++];
            for (int edge = offsets[node]; edge < offsets[node + 1]; edge++) {
                int kind = Byte.toUnsignedInt(kinds[edge]);
                if (kind >= Long.SIZE || (allowedKinds & (1L << kind)) == 0L) continue;
                int target = targets[edge];
                if (!marked(visited, target)) {
                    if (tail == queue.length) {
                        throw new IllegalArgumentException("traversal exceeds maximum");
                    }
                    mark(visited, target);
                    queue[tail++] = target;
                }
            }
        }
        return Arrays.copyOf(queue, tail);
    }

    public int[] depthFirst(int start, long allowedKinds, int maximum) {
        Objects.checkIndex(start, nodeCount());
        requireMaximum(maximum);
        int[] stack = new int[Math.min(nodeCount(), maximum)];
        int top = 0;
        int[] result = new int[stack.length];
        int count = 0;
        long[] visited = new long[(nodeCount() + 63) >>> 6];
        mark(visited, start);
        stack[top++] = start;
        while (top != 0) {
            int node = stack[--top];
            if (count == result.length) {
                throw new IllegalArgumentException("traversal exceeds maximum");
            }
            result[count++] = node;
            for (int edge = offsets[node + 1] - 1; edge >= offsets[node]; edge--) {
                int kind = Byte.toUnsignedInt(kinds[edge]);
                int target = targets[edge];
                if (kind < Long.SIZE
                        && (allowedKinds & (1L << kind)) != 0L
                        && !marked(visited, target)) {
                    if (top == stack.length) {
                        throw new IllegalArgumentException("traversal exceeds maximum");
                    }
                    mark(visited, target);
                    stack[top++] = target;
                }
            }
        }
        return Arrays.copyOf(result, count);
    }

    public int[] offsetsCopy() { return offsets.clone(); }
    public int[] targetsCopy() { return targets.clone(); }
    public byte[] kindsCopy() { return kinds.clone(); }

    public long logicalPayloadBytes() {
        return offsets.length * (long) Integer.BYTES
                + targets.length * (long) Integer.BYTES
                + kinds.length;
    }

    private void requireMaximum(int maximum) {
        if (maximum < 1 || maximum > Math.max(1, nodeCount())) {
            throw new IllegalArgumentException("invalid maximum");
        }
    }

    private static boolean marked(long[] bits, int index) {
        return (bits[index >>> 6] & 1L << (index & 63)) != 0L;
    }

    private static void mark(long[] bits, int index) {
        bits[index >>> 6] |= 1L << (index & 63);
    }
}
