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
package jdk.internal.m3.collections;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable directed int-node graph in compressed sparse row form.
 * Each adjacency is sorted and deduplicated; all retained data is primitive arrays.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatCsrGraph.java}
 * blob {@code bad047aab7d8adda8034b330fbcd193135bbbb09} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3MatCsrGraph {
    /** Native execution is explicit; JAVA never attempts library loading. */
    public enum Backend { JAVA, NATIVE_REQUIRED }

    private final int nodes;
    private final int[] offsets;
    private final int[] targets;

    private M3MatCsrGraph(int nodes, int[] offsets, int[] targets) {
        this.nodes = nodes;
        this.offsets = offsets;
        this.targets = targets;
    }

    /**
     * Builds sorted/deduplicated CSR once from a packed primitive edge vector.
     * After sorting, one sequential pass fills both row boundaries and targets;
     * no temporary per-node degree/cursor arrays are needed.
     */
    public static M3MatCsrGraph of(int nodes, int[] from, int[] to) {
        if (nodes < 0 || nodes == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("nodes outside CSR offset-array capacity");
        }
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (from.length != to.length) throw new IllegalArgumentException("length mismatch");
        long[] edges = new long[from.length];
        for (int i = 0; i < from.length; i++) {
            if (from[i] < 0 || from[i] >= nodes || to[i] < 0 || to[i] >= nodes) {
                throw new IllegalArgumentException("edge outside universe");
            }
            edges[i] = ((long) from[i] << Integer.SIZE) | (to[i] & 0xffff_ffffL);
        }
        Arrays.sort(edges);
        int unique = 0;
        for (long edge : edges) {
            if (unique == 0 || edges[unique - 1] != edge) edges[unique++] = edge;
        }
        int[] offsets = new int[nodes + 1];
        int[] targets = new int[unique];
        int nextRow = 0;
        for (int i = 0; i < unique; i++) {
            int source = (int) (edges[i] >>> Integer.SIZE);
            while (nextRow <= source) offsets[nextRow++] = i;
            targets[i] = (int) edges[i];
        }
        while (nextRow <= nodes) offsets[nextRow++] = unique;
        return new M3MatCsrGraph(nodes, offsets, targets);
    }

    public int nodes() { return nodes; }
    public int edges() { return targets.length; }

    public int outDegree(int node) {
        Objects.checkIndex(node, nodes);
        return offsets[node + 1] - offsets[node];
    }

    public M3MatIntList successors(int node) {
        Objects.checkIndex(node, nodes);
        return M3MatIntList.trustedView(targets, offsets[node], offsets[node + 1] - offsets[node]);
    }

    public boolean containsEdge(int from, int to) {
        Objects.checkIndex(from, nodes);
        Objects.checkIndex(to, nodes);
        return Arrays.binarySearch(targets, offsets[from], offsets[from + 1], to) >= 0;
    }

    public long primitivePayloadBytes() { return Integer.BYTES * ((long) offsets.length + targets.length); }

    /** Returns a CSR with every edge reversed in O(nodes + edges) time. */
    public M3MatCsrGraph transpose() { return transpose(Backend.JAVA); }

    /**
     * Transposes using a shared primitive count/prefix plan. The explicit native
     * mode validates the complete native row vector against the Java oracle.
     * No per-edge JNI calls, retained native pointers, or extra row objects.
     */
    public M3MatCsrGraph transpose(Backend backend) {
        Objects.requireNonNull(backend, "backend");
        int[] inverseOffsets = new int[nodes + 1];
        for (int target : targets) inverseOffsets[target + 1]++;
        for (int node = 0; node < nodes; node++) {
            inverseOffsets[node + 1] += inverseOffsets[node];
        }
        int[] javaRows = transposedTargetsJava(inverseOffsets);
        if (backend == Backend.JAVA) return new M3MatCsrGraph(nodes, inverseOffsets, javaRows);
        if (!NativeBackend.AVAILABLE) {
            throw new UnsatisfiedLinkError("m3_mat_csr JNI unavailable");
        }
        int[] nativeRows = transposeTargetsNative(offsets, targets, inverseOffsets);
        if (!Arrays.equals(javaRows, nativeRows)) {
            throw new IllegalStateException("MAT_CSR_JNI_TRANSPOSE_PARITY");
        }
        return new M3MatCsrGraph(nodes, inverseOffsets, nativeRows);
    }

    /** True only when the explicitly configured native library was loaded. */
    public static boolean nativeAvailable() { return NativeBackend.AVAILABLE; }

    private int[] transposedTargetsJava(int[] inverseOffsets) {
        int[] cursor = Arrays.copyOf(inverseOffsets, nodes);
        int[] result = new int[targets.length];
        for (int source = 0; source < nodes; source++) {
            for (int edge = offsets[source]; edge < offsets[source + 1]; edge++) {
                int target = targets[edge];
                result[cursor[target]++] = source;
            }
        }
        return result;
    }

    private static final class NativeBackend {
        private static final boolean AVAILABLE = tryLoad();

        private static boolean tryLoad() {
            try {
                String path = System.getProperty("m3.matcsr.native.path", "").strip();
                if (path.isEmpty()) System.loadLibrary("m3_mat_csr");
                else System.load(Path.of(path).toAbsolutePath().normalize().toString());
                return true;
            } catch (UnsatisfiedLinkError | SecurityException | InvalidPathException unavailable) {
                return false;
            }
        }
    }

    private static native int[] transposeTargetsNative(
            int[] offsets, int[] targets, int[] transposedOffsets);
}
