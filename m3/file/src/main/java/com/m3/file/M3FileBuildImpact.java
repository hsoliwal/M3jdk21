// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.Arrays;
import java.util.Objects;

/**
 * M3JDK21-owned projection of file deltas and compiler-supplied dependency facts.
 *
 * <p>M3File owns file identities and raw byte changes. The original javac/M3AST
 * task owns semantic dependency edges and ABI evidence. This pure planner does
 * not parse files, replace javac, retain source payloads or authorize skipping
 * arbitrary Maven/Gradle plugin executions. A consumer must provide an exact,
 * complete input and dependency graph for the build generation before it can
 * interpret unaffected rows as reuse candidates.</p>
 *
 * <p>Each node is an original {@link M3FileDeltaLedger.Snapshot} physical slot;
 * edge arrays contain {@code dependencySlot -> dependentSlot}. The graph is
 * compressed into primitive offset and index columns. No per-edge object or
 * mirrored canonical file payload is retained.</p>
 */
public final class M3FileBuildImpact {
    /** The compiler proved ABI equivalence in the current exact input context. */
    public static final long ABI_PROVEN_SAME = 1L;
    /** The compiler proved an ABI change (including inlined constants). */
    public static final long ABI_CHANGED = 1L << 1;
    /** Effective toolchain, processor, configuration or relevant model changed. */
    public static final long CONTEXT_CHANGED = 1L << 2;
    /** Semantic facts are unresolved; downstream dependents must be invalidated. */
    public static final long UNKNOWN_SEMANTICS = 1L << 3;
    private static final long FACT_BITS = ABI_PROVEN_SAME | ABI_CHANGED
            | CONTEXT_CHANGED | UNKNOWN_SEMANTICS;

    public static final long DIRECT = 1L;
    public static final long DEPENDENT = 1L << 1;
    public static final long CONSERVATIVE = 1L << 2;
    private static final long WORK_BITS = DIRECT | DEPENDENT | CONSERVATIVE;

    private M3FileBuildImpact() {}

    /** Immutable by-value output over the original stable physical row space. */
    public static final class Plan {
        private final long[] reasons;
        private final boolean exactGraph;
        private final boolean globalInvalidation;

        private Plan(long[] reasons, boolean exactGraph, boolean globalInvalidation) {
            this.reasons = reasons;
            this.exactGraph = exactGraph;
            this.globalInvalidation = globalInvalidation;
        }

        public int slots() { return reasons.length; }
        public long reasonAt(int slot) { return reasons[Objects.checkIndex(slot, reasons.length)]; }
        public boolean requiresWorkAt(int slot) { return (reasonAt(slot) & WORK_BITS) != 0; }
        /** Only a candidate: the caller must still respect task side effects. */
        public boolean unaffectedAt(int slot) { return reasonAt(slot) == 0; }
        public boolean exactGraph() { return exactGraph; }
        public boolean globalInvalidation() { return globalInvalidation; }

        /** Physical indexes in stable order; no source or class payload copies. */
        public int[] affectedSlots() {
            int count = 0;
            for (long reason : reasons) if ((reason & WORK_BITS) != 0) count++;
            int[] result = new int[count];
            for (int i = 0, w = 0; i < reasons.length; i++) {
                if ((reasons[i] & WORK_BITS) != 0) result[w++] = i;
            }
            return result;
        }
    }

    /**
     * Compute a conservative fixed point in O(nodes + edges), even with cycles.
     *
     * @param snapshot freshly scanned M3File generation, not a loaded-only base image
     * @param compilerFacts one compiler-authored fact mask per physical file slot
     * @param dependencies original dependency physical slots (edge sources)
     * @param dependents original dependent physical slots (edge destinations)
     * @param completeGraph true only if the caller proved closed semantic input,
     *        constant, generated-source, processor and build context coverage
     * @return immutable affected-slot projection; not permission to skip a Mojo
     */
    public static Plan plan(M3FileDeltaLedger.Snapshot snapshot, long[] compilerFacts,
                            int[] dependencies, int[] dependents, boolean completeGraph) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(compilerFacts, "compilerFacts");
        Objects.requireNonNull(dependencies, "dependencies");
        Objects.requireNonNull(dependents, "dependents");
        int n = snapshot.slots();
        if (compilerFacts.length != n || dependencies.length != dependents.length) {
            throw new IllegalArgumentException("unaligned build fact/edge lanes");
        }
        long[] reasons = new long[n];
        boolean global = !completeGraph;
        for (int i = 0; i < n; i++) {
            long facts = compilerFacts[i];
            if ((facts & ~FACT_BITS) != 0
                    || ((facts & ABI_PROVEN_SAME) != 0
                            && (facts & (ABI_CHANGED | CONTEXT_CHANGED | UNKNOWN_SEMANTICS)) != 0)) {
                throw new IllegalArgumentException("contradictory or unknown ABI facts at " + i);
            }
            if (snapshot.changedAt(i) || (facts & (ABI_CHANGED | CONTEXT_CHANGED | UNKNOWN_SEMANTICS)) != 0) {
                reasons[i] = DIRECT;
            }
            // Until build model inputs have their own verified dependency scope,
            // one changed POM/DSL/resource invalidates the entire declared generation.
            if (snapshot.changedAt(i) && snapshot.kindAt(i) == M3FileDeltaLedger.Kind.BUILD_INPUT) {
                global = true;
            }
        }
        int edgeCount = dependencies.length;
        int[] start = new int[n + 1];
        for (int e = 0; e < edgeCount; e++) {
            int dependency = Objects.checkIndex(dependencies[e], n);
            Objects.checkIndex(dependents[e], n);
            start[dependency + 1] = Math.addExact(start[dependency + 1], 1);
        }
        for (int i = 0; i < n; i++) start[i + 1] = Math.addExact(start[i + 1], start[i]);
        int[] cursor = Arrays.copyOf(start, n);
        int[] reverse = new int[edgeCount];
        for (int e = 0; e < edgeCount; e++) {
            reverse[cursor[dependencies[e]]++] = dependents[e];
        }
        if (global) {
            for (int i = 0; i < n; i++) reasons[i] |= CONSERVATIVE;
            return new Plan(reasons, completeGraph, true);
        }
        int[] queue = new int[n];
        byte[] enqueued = new byte[n];
        int end = 0;
        for (int i = 0; i < n; i++) {
            if (reasons[i] != 0 && (compilerFacts[i] & ABI_PROVEN_SAME) == 0) {
                queue[end++] = i;
                enqueued[i] = 1;
            }
        }
        for (int read = 0; read < end; read++) {
            int source = queue[read];
            for (int e = start[source]; e < start[source + 1]; e++) {
                int dependent = reverse[e];
                reasons[dependent] |= DEPENDENT;
                if (enqueued[dependent] == 0) {
                    enqueued[dependent] = 1;
                    queue[end++] = dependent;
                }
            }
        }
        return new Plan(reasons, true, false);
    }
}
