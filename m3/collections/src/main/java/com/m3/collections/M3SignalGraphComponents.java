/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Precomputed strongly connected components over attributed M3 compiler call candidates.
 *
 * <p>The original {@link M3SignalRelations} is the sole graph-edge owner. This derived image
 * stores only primitive component, member, mask, and condensation-DAG lanes. Both Kosaraju
 * passes are iterative, so even a large call chain does not consume Java recursion stack.
 * Component IDs are assigned by ascending minimum source type ordinal, independently of edge
 * insertion order. This models <em>recorded type-level candidates</em>, not a complete runtime
 * call graph or a proof of dynamic dispatch, reflection, or object identity.
 */
public final class M3SignalGraphComponents {
    private final int[] componentOf;
    private final int[] memberOffsets;
    private final int[] members;
    private final int[] dagOffsets;
    private final int[] dagTargets;
    private final int[] incomingDegree;
    private final int[] topological;
    private final int[] depth;
    private final long[] cycles;
    private final long[] internalSites;
    private final long[] incomingSites;
    private final long[] outgoingSites;
    private final boolean includingDispatch;

    private M3SignalGraphComponents(int[] componentOf, int[] memberOffsets, int[] members,
            int[] dagOffsets, int[] dagTargets, int[] incomingDegree, int[] topological,
            int[] depth, long[] cycles, long[] internalSites, long[] incomingSites,
            long[] outgoingSites, boolean includingDispatch) {
        this.componentOf = componentOf;
        this.memberOffsets = memberOffsets;
        this.members = members;
        this.dagOffsets = dagOffsets;
        this.dagTargets = dagTargets;
        this.incomingDegree = incomingDegree;
        this.topological = topological;
        this.depth = depth;
        this.cycles = cycles;
        this.internalSites = internalSites;
        this.incomingSites = incomingSites;
        this.outgoingSites = outgoingSites;
        this.includingDispatch = includingDispatch;
    }

    /**
     * Derive a deterministic primitive CSR of known-type component dependencies.
     * DISPATCH_CANDIDATE is admitted only when explicitly requested. External call targets
     * absent from the input relation image are not invented and remain unknown.
     * Complexity: O(types + edges + admitted edges log admitted edges), scratch
     * O(types + admitted edges). Never retains per-node/edge Java objects or JNI pointers.
     */
    public static M3SignalGraphComponents from(
            M3SignalRelations source, boolean includeDispatchCandidates) {
        Objects.requireNonNull(source, "source");
        int rows = source.rowCount();
        if (rows == Integer.MAX_VALUE) throw new IllegalArgumentException("too many type rows");
        int[] outgoing = new int[rows + 1];
        int[] reversed = new int[rows + 1];
        for (int row = 0; row < rows; row++) {
            for (int edge = 0; edge < source.outgoingEdges(row); edge++) {
                if (!admit(source.kind(row, edge), includeDispatchCandidates)) continue;
                int target = source.target(row, edge);
                outgoing[row + 1] = Math.addExact(outgoing[row + 1], 1);
                reversed[target + 1] = Math.addExact(reversed[target + 1], 1);
            }
        }
        for (int row = 0; row < rows; row++) {
            outgoing[row + 1] = Math.addExact(outgoing[row], outgoing[row + 1]);
            reversed[row + 1] = Math.addExact(reversed[row], reversed[row + 1]);
        }
        int edges = outgoing[rows];
        int[] targets = new int[edges];
        int[] reverseTargets = new int[edges];
        int[] next = Arrays.copyOf(outgoing, rows);
        int[] reverseNext = Arrays.copyOf(reversed, rows);
        for (int row = 0; row < rows; row++) {
            for (int edge = 0; edge < source.outgoingEdges(row); edge++) {
                if (!admit(source.kind(row, edge), includeDispatchCandidates)) continue;
                int target = source.target(row, edge);
                targets[next[row]++] = target;
                reverseTargets[reverseNext[target]++] = row;
            }
        }

        int[] finish = finishingOrder(rows, outgoing, targets);
        int[] rawComponents = new int[rows];
        Arrays.fill(rawComponents, -1);
        int[] minimum = new int[rows];
        Arrays.fill(minimum, Integer.MAX_VALUE);
        int[] stack = new int[rows];
        int rawCount = 0;
        for (int i = rows - 1; i >= 0; i--) {
            int root = finish[i];
            if (rawComponents[root] >= 0) continue;
            int size = 1;
            stack[0] = root;
            rawComponents[root] = rawCount;
            while (size > 0) {
                int node = stack[--size];
                minimum[rawCount] = Math.min(minimum[rawCount], node);
                for (int edge = reversed[node]; edge < reversed[node + 1]; edge++) {
                    int parent = reverseTargets[edge];
                    if (rawComponents[parent] < 0) {
                        rawComponents[parent] = rawCount;
                        stack[size++] = parent;
                    }
                }
            }
            rawCount++;
        }

        // Stable IDs do not depend on DFS traversal or original edge insertion order.
        int[] oldAtMinimum = new int[rows];
        Arrays.fill(oldAtMinimum, -1);
        for (int raw = 0; raw < rawCount; raw++) oldAtMinimum[minimum[raw]] = raw;
        int[] translate = new int[rawCount];
        int components = 0;
        for (int row = 0; row < rows; row++) {
            if (oldAtMinimum[row] >= 0) translate[oldAtMinimum[row]] = components++;
        }
        int[] componentOf = new int[rows];
        int[] memberOffsets = new int[components + 1];
        for (int row = 0; row < rows; row++) {
            int component = translate[rawComponents[row]];
            componentOf[row] = component;
            memberOffsets[component + 1]++;
        }
        for (int component = 0; component < components; component++) {
            memberOffsets[component + 1] += memberOffsets[component];
        }
        int[] members = new int[rows];
        int[] cursor = Arrays.copyOf(memberOffsets, components);
        for (int row = 0; row < rows; row++) members[cursor[componentOf[row]]++] = row;

        int crossCount = 0;
        for (int row = 0; row < rows; row++) {
            for (int edge = 0; edge < source.outgoingEdges(row); edge++) {
                if (admit(source.kind(row, edge), includeDispatchCandidates)
                        && componentOf[row] != componentOf[source.target(row, edge)]) {
                    crossCount = Math.addExact(crossCount, 1);
                }
            }
        }
        long[] crossPairs = new long[crossCount];
        long[] internalSites = new long[components];
        long[] incomingSites = new long[components];
        long[] outgoingSites = new long[components];
        long[] cycles = new long[(int) (((long) components + 63L) >>> 6)];
        for (int component = 0; component < components; component++) {
            if (memberOffsets[component + 1] - memberOffsets[component] > 1) {
                cycles[component >>> 6] |= 1L << (component & 63);
            }
        }
        int at = 0;
        for (int row = 0; row < rows; row++) {
            int parent = componentOf[row];
            for (int edge = 0; edge < source.outgoingEdges(row); edge++) {
                if (!admit(source.kind(row, edge), includeDispatchCandidates)) continue;
                int target = source.target(row, edge);
                int child = componentOf[target];
                int sites = source.callSites(row, edge);
                if (parent == child) {
                    internalSites[parent] = Math.addExact(internalSites[parent], sites);
                    if (row == target) cycles[parent >>> 6] |= 1L << (parent & 63);
                } else {
                    incomingSites[child] = Math.addExact(incomingSites[child], sites);
                    outgoingSites[parent] = Math.addExact(outgoingSites[parent], sites);
                    crossPairs[at++] = ((long) parent << 32) | (child & 0xffffffffL);
                }
            }
        }
        if (at != crossPairs.length) throw new IllegalStateException("graph edge count drift");
        Arrays.sort(crossPairs);
        int[] dagOffsets = new int[components + 1];
        int[] incomingDegree = new int[components];
        int unique = 0;
        long previous = -1;
        for (long pair : crossPairs) {
            if (pair == previous) continue;
            int parent = (int) (pair >>> 32);
            int child = (int) pair;
            dagOffsets[parent + 1] = Math.addExact(dagOffsets[parent + 1], 1);
            incomingDegree[child] = Math.addExact(incomingDegree[child], 1);
            unique++;
            previous = pair;
        }
        for (int c = 0; c < components; c++) {
            dagOffsets[c + 1] = Math.addExact(dagOffsets[c], dagOffsets[c + 1]);
        }
        int[] dagTargets = new int[unique];
        int written = 0;
        previous = -1;
        for (long pair : crossPairs) {
            if (pair != previous) dagTargets[written++] = (int) pair;
            previous = pair;
        }
        if (written != unique) throw new IllegalStateException("component DAG drift");
        int[] topological = new int[components];
        int[] depth = new int[components];
        topologicalOrder(components, dagOffsets, dagTargets,
                incomingDegree, topological, depth);
        return new M3SignalGraphComponents(componentOf, memberOffsets, members,
                dagOffsets, dagTargets, incomingDegree, topological, depth,
                cycles, internalSites, incomingSites, outgoingSites,
                includeDispatchCandidates);
    }

    private static boolean admit(byte kind, boolean includingDispatch) {
        return includingDispatch || kind == M3SignalRelations.DIRECT;
    }

    /** Explicit per-frame index stack; no recursion, no synthetic graph Entry objects. */
    private static int[] finishingOrder(int rows, int[] offsets, int[] targets) {
        int[] finished = new int[rows];
        int[] stack = new int[rows];
        int[] next = new int[rows];
        byte[] visited = new byte[rows];
        int count = 0;
        for (int root = 0; root < rows; root++) {
            if (visited[root] != 0) continue;
            int top = 0;
            visited[root] = 1;
            stack[0] = root;
            next[0] = offsets[root];
            while (top >= 0) {
                int node = stack[top];
                if (next[top] < offsets[node + 1]) {
                    int target = targets[next[top]++];
                    if (visited[target] == 0) {
                        visited[target] = 1;
                        stack[++top] = target;
                        next[top] = offsets[target];
                    }
                } else {
                    finished[count++] = node;
                    top--;
                }
            }
        }
        if (count != rows) throw new IllegalStateException("nonexhaustive source graph scan");
        return finished;
    }

    private static void topologicalOrder(int components, int[] offsets, int[] targets,
            int[] incomingDegree, int[] ordered, int[] depth) {
        int[] indegree = incomingDegree.clone();
        int[] heap = new int[components];
        int heapSize = 0;
        for (int c = 0; c < components; c++) {
            if (indegree[c] == 0) heapSize = pushMin(heap, heapSize, c);
        }
        int out = 0;
        while (heapSize > 0) {
            int current = heap[0];
            heapSize = removeMin(heap, heapSize);
            ordered[out++] = current;
            for (int edge = offsets[current]; edge < offsets[current + 1]; edge++) {
                int child = targets[edge];
                depth[child] = Math.max(depth[child], depth[current] + 1);
                if (--indegree[child] == 0) heapSize = pushMin(heap, heapSize, child);
            }
        }
        if (out != components) throw new IllegalStateException("condensation graph is cyclic");
    }

    private static int pushMin(int[] heap, int size, int value) {
        int current = size++;
        while (current > 0) {
            int parent = (current - 1) >>> 1;
            if (heap[parent] <= value) break;
            heap[current] = heap[parent];
            current = parent;
        }
        heap[current] = value;
        return size;
    }

    private static int removeMin(int[] heap, int size) {
        int remaining = size - 1;
        if (remaining == 0) return 0;
        int value = heap[remaining];
        int current = 0;
        while ((current << 1) + 1 < remaining) {
            int child = (current << 1) + 1;
            if (child + 1 < remaining && heap[child + 1] < heap[child]) child++;
            if (value <= heap[child]) break;
            heap[current] = heap[child];
            current = child;
        }
        heap[current] = value;
        return remaining;
    }

    public int typeCount() { return componentOf.length; }
    public int componentCount() { return memberOffsets.length - 1; }
    public boolean includesDispatchCandidates() { return includingDispatch; }
    public int componentOf(int typeOrdinal) {
        return componentOf[Objects.checkIndex(typeOrdinal, typeCount())];
    }
    public int componentSize(int component) {
        int c = Objects.checkIndex(component, componentCount());
        return memberOffsets[c + 1] - memberOffsets[c];
    }
    public int member(int component, int ordinal) {
        int c = Objects.checkIndex(component, componentCount());
        return members[memberOffsets[c] + Objects.checkIndex(ordinal, componentSize(c))];
    }
    public boolean cyclic(int component) {
        int c = Objects.checkIndex(component, componentCount());
        return (cycles[c >>> 6] & (1L << (c & 63))) != 0;
    }
    public int outgoingComponentCount(int component) {
        int c = Objects.checkIndex(component, componentCount());
        return dagOffsets[c + 1] - dagOffsets[c];
    }
    public int outgoingComponent(int component, int ordinal) {
        int c = Objects.checkIndex(component, componentCount());
        return dagTargets[dagOffsets[c] + Objects.checkIndex(ordinal,
                dagOffsets[c + 1] - dagOffsets[c])];
    }
    public int incomingComponentCount(int component) {
        return incomingDegree[Objects.checkIndex(component, componentCount())];
    }
    public int topologicalComponent(int ordinal) {
        return topological[Objects.checkIndex(ordinal, componentCount())];
    }
    public int dependencyDepth(int component) {
        return depth[Objects.checkIndex(component, componentCount())];
    }
    public long internalCallSites(int component) {
        return internalSites[Objects.checkIndex(component, componentCount())];
    }
    public long incomingCallSites(int component) {
        return incomingSites[Objects.checkIndex(component, componentCount())];
    }
    public long outgoingCallSites(int component) {
        return outgoingSites[Objects.checkIndex(component, componentCount())];
    }

    /**
     * Materialize explicit static feature columns once for a caller-supplied source identity.
     * The 64-bit evidence hint does not authenticate its own producer or certify similarity.
     * The caller must independently bind the full source SHA-256 and source generation.
     */
    public M3SignalColumns structuralColumns(long evidenceHint) {
        int rows = typeCount();
        int[] size = new int[rows];
        int[] cycle = new int[rows];
        int[] rank = new int[rows];
        long[] incoming = new long[rows];
        long[] outgoing = new long[rows];
        long[] internal = new long[rows];
        for (int row = 0; row < rows; row++) {
            int c = componentOf[row];
            size[row] = memberOffsets[c + 1] - memberOffsets[c];
            cycle[row] = cyclic(c) ? 1 : 0;
            rank[row] = depth[c];
            incoming[row] = incomingSites[c];
            outgoing[row] = outgoingSites[c];
            internal[row] = internalSites[c];
        }
        long[] valid = M3SignalColumns.allValid(rows);
        M3SignalColumns.Origin origin = M3SignalColumns.Origin.STATIC;
        return new M3SignalColumns(rows,
                new M3SignalColumns.Column(M3SignalColumns.Kind.INTEGER, origin,
                        evidenceHint ^ 0x101L, false, valid, size, null, null),
                new M3SignalColumns.Column(M3SignalColumns.Kind.INTEGER, origin,
                        evidenceHint ^ 0x102L, false, valid, cycle, null, null),
                new M3SignalColumns.Column(M3SignalColumns.Kind.INTEGER, origin,
                        evidenceHint ^ 0x103L, false, valid, rank, null, null),
                new M3SignalColumns.Column(M3SignalColumns.Kind.LONG, origin,
                        evidenceHint ^ 0x104L, false, valid, null, incoming, null),
                new M3SignalColumns.Column(M3SignalColumns.Kind.LONG, origin,
                        evidenceHint ^ 0x105L, false, valid, null, outgoing, null),
                new M3SignalColumns.Column(M3SignalColumns.Kind.LONG, origin,
                        evidenceHint ^ 0x106L, false, valid, null, internal, null));
    }
}
