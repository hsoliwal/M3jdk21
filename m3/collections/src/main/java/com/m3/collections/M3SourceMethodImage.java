/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Compiler-attributed method-declaration facts in primitive columns and known-method CSR.
 *
 * <p>The original source objects, method descriptors and call-edge builders are discarded after
 * parsing. Retained graph storage consists of packed UTF-8 method names, primitive type-owner
 * indices, primitive fact columns, and the existing M3SignalRelations CSR. A DIRECT edge means
 * a statically resolved call expression, not proof that the callee executed. Dynamic, reflective,
 * generated and external targets are never silently treated as graph closure.</p>
 */
public final class M3SourceMethodImage {
    public enum Scalar {
        MODIFIER_FLAGS, PARAMETERS, CALL_SITES, ALLOCATION_SITES, MEMBER_REFERENCES,
        DIRECT_CALLS, DISPATCH_CANDIDATES, UNRESOLVED_CALLS, LOOPS, BRANCHES,
        TRY_BLOCKS, CATCHES, LAMBDAS, INTERNAL_INCOMING_SITES, INTERNAL_OUTGOING_SITES
    }

    private static final int DECLARED_SCALARS = 13;
    private final M3SignalColumns columns;
    private final M3SignalRelations relationships;
    private final int[] ownerTypes;
    private final int[] nameOffsets;
    private final byte[] nameBytes;
    private final int externalCandidateRecords;
    private final long externalCandidateSites;

    private M3SourceMethodImage(M3SignalColumns columns, M3SignalRelations relationships,
            int[] ownerTypes, int[] nameOffsets, byte[] nameBytes,
            int externalCandidateRecords, long externalCandidateSites) {
        this.columns = columns;
        this.relationships = relationships;
        this.ownerTypes = ownerTypes;
        this.nameOffsets = nameOffsets;
        this.nameBytes = nameBytes;
        this.externalCandidateRecords = externalCandidateRecords;
        this.externalCandidateSites = externalCandidateSites;
    }

    /**
     * Internal one-pass receiving of validated /2 rows. Source image SHA-256 is already checked.
     * Method identity is its exact compiler-attributed signature, not an FNV token or substring.
     */
    static M3SourceMethodImage build(List<String> names, List<int[]> scalarRows,
            int[] ownerTypes, int[] callSources, List<String> callees,
            byte[] kinds, int[] sites, long evidenceId) {
        Objects.requireNonNull(names, "names");
        Objects.requireNonNull(scalarRows, "scalarRows");
        Objects.requireNonNull(ownerTypes, "ownerTypes");
        Objects.requireNonNull(callSources, "callSources");
        Objects.requireNonNull(callees, "callees");
        Objects.requireNonNull(kinds, "kinds");
        Objects.requireNonNull(sites, "sites");
        int rows = names.size();
        if (scalarRows.size() != rows || ownerTypes.length != rows
                || callSources.length != callees.size()
                || callSources.length != kinds.length || callSources.length != sites.length) {
            throw new IllegalArgumentException("misaligned method source fact lanes");
        }
        M3SignalColumns.Column[] prepared = new M3SignalColumns.Column[Scalar.values().length];
        long[] validity = M3SignalColumns.allValid(rows);
        for (int d = 0; d < DECLARED_SCALARS; d++) {
            int[] values = new int[rows];
            for (int r = 0; r < rows; r++) {
                int[] scalar = scalarRows.get(r);
                if (scalar.length != DECLARED_SCALARS) {
                    throw new IllegalArgumentException("invalid method scalar shape");
                }
                values[r] = scalar[d];
            }
            prepared[d] = new M3SignalColumns.Column(M3SignalColumns.Kind.INTEGER,
                    M3SignalColumns.Origin.STATIC,
                    evidenceId ^ 0x4d4554484f445332L ^ d, false,
                    validity, values, null, null);
        }
        int[] knownSources = new int[callSources.length];
        int[] knownTargets = new int[callSources.length];
        int[] knownSites = new int[callSources.length];
        byte[] knownKinds = new byte[callSources.length];
        int accepted = 0, unknown = 0;
        long unknownSites = 0L;
        for (int i = 0; i < callSources.length; i++) {
            int destination = Collections.binarySearch(names, callees.get(i));
            if (destination < 0) {
                unknown++;
                unknownSites = Math.addExact(unknownSites, sites[i]);
            } else {
                knownSources[accepted] = callSources[i];
                knownTargets[accepted] = destination;
                knownKinds[accepted] = kinds[i];
                knownSites[accepted++] = sites[i];
            }
        }
        M3SignalRelations graph = M3SignalRelations.of(rows,
                Arrays.copyOf(knownSources, accepted), Arrays.copyOf(knownTargets, accepted),
                Arrays.copyOf(knownKinds, accepted), Arrays.copyOf(knownSites, accepted));
        long[] incoming = new long[rows], outgoing = new long[rows];
        for (int r = 0; r < rows; r++) {
            incoming[r] = graph.incomingSites(r);
            outgoing[r] = graph.outgoingSites(r);
        }
        prepared[DECLARED_SCALARS] = new M3SignalColumns.Column(M3SignalColumns.Kind.LONG,
                M3SignalColumns.Origin.STATIC, evidenceId ^ 0x4d4554484f445332L
                        ^ DECLARED_SCALARS, false, validity, null, incoming, null);
        prepared[DECLARED_SCALARS + 1] = new M3SignalColumns.Column(M3SignalColumns.Kind.LONG,
                M3SignalColumns.Origin.STATIC, evidenceId ^ 0x4d4554484f445332L
                        ^ (DECLARED_SCALARS + 1), false, validity, null, outgoing, null);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int[] offsets = new int[rows + 1];
        for (int r = 0; r < rows; r++) {
            if (r > 0 && names.get(r).compareTo(names.get(r - 1)) <= 0) {
                throw new IllegalArgumentException("unordered method identity");
            }
            offsets[r] = bytes.size();
            bytes.writeBytes(names.get(r).getBytes(StandardCharsets.UTF_8));
        }
        offsets[rows] = bytes.size();
        return new M3SourceMethodImage(new M3SignalColumns(rows, prepared), graph,
                ownerTypes.clone(), offsets, bytes.toByteArray(), unknown, unknownSites);
    }

    public int methodCount() { return ownerTypes.length; }
    public String signature(int methodOrdinal) {
        int r = Objects.checkIndex(methodOrdinal, methodCount());
        return new String(nameBytes, nameOffsets[r], nameOffsets[r + 1] - nameOffsets[r],
                StandardCharsets.UTF_8);
    }
    public int ownerType(int methodOrdinal) {
        return ownerTypes[Objects.checkIndex(methodOrdinal, methodCount())];
    }
    public M3SignalColumns columns() { return columns; }
    public M3SignalRelations relationships() { return relationships; }
    public int externalCandidateRecords() { return externalCandidateRecords; }
    public long externalCandidateSites() { return externalCandidateSites; }
}
