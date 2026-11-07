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
 * Versioned primitive sort-signal evidence indexed by stable ID, without an Entry or
 * pairwise comparison object per element. Computed signals survive subsequent reads,
 * and a dependency-revision mismatch triggers recomputation on demand.
 *
 * <p>The caller or M3JDK compiler must supply a correctly incremented revision whenever
 * one of the signal's dependencies changes. This owner cannot detect arbitrary field
 * mutation and makes no claims of autonomous escape/alias analysis or transparent swap.
 * This class is not thread-safe.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/M3SortSignalLane.java}
 * blob {@code 516c2533ca7c3c040ccbb25fa417ac98db391158} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3SortSignalLane {
    /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
    public M3SortSignalLane() {
    }

    @FunctionalInterface
    public interface Computer {
        long compute(int stableId);
    }

    private long[] values = new long[16];
    private long[] revisions = new long[16];
    private byte[] known = new byte[16];
    // Optional typed evidence: allocated only if a producer explicitly declares a kind.
    // Legacy put/resolve paths keep this lane absent and unverified.
    private byte[] evidenceKinds;
    private long computations;
    private long hits;

    public long computationCount() { return computations; }
    public long hitCount() { return hits; }
    public int reservedSlots() { return values.length; }
    public long payloadBytes() {
        return (long) values.length * (Long.BYTES * 2L + Byte.BYTES)
                + (evidenceKinds == null ? 0L : evidenceKinds.length);
    }

    public boolean hasCurrent(int stableId, long dependencyRevision) {
        requireId(stableId);
        return stableId < known.length && known[stableId] != 0
                && revisions[stableId] == dependencyRevision;
    }

    /** Read existing evidence, computing once per (stable ID, dependency revision). */
    public long resolve(int stableId, long dependencyRevision, Computer computer) {
        requireId(stableId);
        Objects.requireNonNull(computer, "computer");
        if (hasCurrent(stableId, dependencyRevision)) {
            hits++;
            return values[stableId];
        }
        long computed = computer.compute(stableId); // may throw without invalidating prior evidence
        put(stableId, dependencyRevision, computed);
        computations++;
        return computed;
    }

    /** Compiler/precompute producer's direct primitive write; no function allocation. */
    public void put(int stableId, long dependencyRevision, long signal) {
        requireId(stableId);
        ensureCapacity(stableId + 1);
        values[stableId] = signal;
        revisions[stableId] = dependencyRevision;
        // Legacy producers retain their existing keys, but cannot inherit prior proof labels.
        if (evidenceKinds != null) evidenceKinds[stableId] = M3SignalEvidenceAdmission.UNKNOWN;
        known[stableId] = 1; // publish presence last
    }

    /**
     * Retain a producer-classified fact in the existing stable-ID lane.
     * A declared kind does not prove domain semantics, comparator monotonicity,
     * metric bounds, or authenticity of the producer. The optional byte lane
     * is allocated only on the first classified write.
     */
    void putEvidence(int stableId, long dependencyRevision, long signal, byte kind) {
        requireId(stableId);
        // Validate before modifying any existing retained value or its validity.
        M3SignalEvidenceAdmission.assess(kind, dependencyRevision, dependencyRevision,
                M3SignalEvidenceAdmission.HINT);
        if (kind == M3SignalEvidenceAdmission.UNKNOWN) {
            put(stableId, dependencyRevision, signal);
            return;
        }
        ensureCapacity(stableId + 1, true);
        if (evidenceKinds == null) evidenceKinds = new byte[values.length];
        values[stableId] = signal;
        revisions[stableId] = dependencyRevision;
        evidenceKinds[stableId] = kind;
        known[stableId] = 1; // publish presence last
    }

    /**
     * Fail-closed evidence admission for the cached revision. This method does
     * not evaluate a signal or grant correctness-sensitive shortcuts. A legacy
     * put/resolve has UNKNOWN evidence even when its raw key is current.
     */
    byte assessEvidence(int stableId, long dependencyRevision, byte intent) {
        requireId(stableId);
        if (stableId >= known.length || known[stableId] == 0) {
            return M3SignalEvidenceAdmission.assess(M3SignalEvidenceAdmission.UNKNOWN,
                    dependencyRevision, dependencyRevision, intent);
        }
        byte kind = evidenceKinds == null ? M3SignalEvidenceAdmission.UNKNOWN
                : evidenceKinds[stableId];
        return M3SignalEvidenceAdmission.assess(kind,
                revisions[stableId], dependencyRevision, intent);
    }


    /**
     * Read admission decisions for a caller-owned window of stable IDs.
     *
     * <p>All source/output geometry, intent, and IDs are checked before any
     * destination write. The revision lane aligns with source ID positions,
     * including repeated IDs. This method never computes signals, allocates
     * cache entries, grows storage, or grants semantic comparison authority.
     * Like this cache, it requires exclusive owner access during the call.
     */
    void assessEvidenceBatch(int[] stableIds, long[] dependencyRevisions,
            int from, int count, byte intent, byte[] destination, int destinationOffset) {
        Objects.requireNonNull(stableIds, "stableIds");
        Objects.requireNonNull(dependencyRevisions, "dependencyRevisions");
        Objects.requireNonNull(destination, "destination");
        if (stableIds.length != dependencyRevisions.length) {
            throw new IllegalArgumentException("ID and revision lanes are not aligned");
        }
        Objects.checkFromIndexSize(from, count, stableIds.length);
        Objects.checkFromIndexSize(destinationOffset, count, destination.length);
        // Validate before writing; UNKNOWN itself grants no optimization authority.
        M3SignalEvidenceAdmission.assess(M3SignalEvidenceAdmission.UNKNOWN,
                0L, 0L, intent);
        int end = from + count;
        for (int position = from; position < end; position++) {
            requireId(stableIds[position]);
        }
        for (int position = from; position < end; position++) {
            int id = stableIds[position];
            byte kind = evidenceKinds == null || id >= evidenceKinds.length
                    ? M3SignalEvidenceAdmission.UNKNOWN : evidenceKinds[id];
            destination[destinationOffset + position - from] =
                    id >= known.length || known[id] == 0
                            ? M3SignalEvidenceAdmission.REFUSE_UNKNOWN
                            : M3SignalEvidenceAdmission.assess(kind,
                                    revisions[id], dependencyRevisions[position], intent);
        }
    }

    /** Obtain retained evidence without executing an expensive fallback. */
    public long requireCurrent(int stableId, long dependencyRevision) {
        if (!hasCurrent(stableId, dependencyRevision)) {
            throw new IllegalStateException("no current M3 sortable signal for ID " + stableId);
        }
        return values[stableId];
    }

    public void invalidate(int stableId) {
        requireId(stableId);
        if (stableId < known.length) known[stableId] = 0;
    }

    /** Clears validity only, retaining primitive backing for amortized reuse. */
    public void clear() {
        Arrays.fill(known, (byte) 0);
    }

    /** Signed-long order key exactly matching Java's Double.compare, including -0 and NaN. */
    public static long orderedDoubleBits(double signal) {
        long bits = Double.doubleToLongBits(signal);
        return bits ^ ((bits >> 63) & Long.MAX_VALUE);
    }

    private void ensureCapacity(int minimum) {
        ensureCapacity(minimum, false);
    }

    private void ensureCapacity(int minimum, boolean classifiedWrite) {
        if (minimum <= values.length) return;
        long preferred = Math.max((long) minimum, values.length + (values.length >>> 1) + 1L);
        if (preferred > Integer.MAX_VALUE - 8L) preferred = minimum;
        int capacity = Math.toIntExact(M3RuntimeHeap.chooseAmortizedCapacity(
                minimum, preferred, Long.BYTES * 2 + Byte.BYTES
                        + (classifiedWrite || evidenceKinds != null ? Byte.BYTES : 0)));
        long[] nextValues = Arrays.copyOf(values, capacity);
        long[] nextRevisions = Arrays.copyOf(revisions, capacity);
        byte[] nextKnown = Arrays.copyOf(known, capacity);
        byte[] nextKinds = evidenceKinds == null ? null
                : Arrays.copyOf(evidenceKinds, capacity);
        values = nextValues;
        revisions = nextRevisions;
        known = nextKnown;
        evidenceKinds = nextKinds;
    }

    private static void requireId(int stableId) {
        if (stableId < 0 || stableId >= Integer.MAX_VALUE - 1) {
            throw new IllegalArgumentException("stableId outside supported range");
        }
    }
}
