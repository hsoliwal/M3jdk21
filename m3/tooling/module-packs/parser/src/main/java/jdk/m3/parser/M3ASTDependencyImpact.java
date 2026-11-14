// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Objects;

/**
 * Conservative change-impact closure over the ONE original M3ASTIndex.
 *
 * <p>Sources of possible invalidation are a caller-owned occurrence mask,
 * unavailable/unknown source ranges, ancestor geometry and parser-claimed
 * NODE_REF edges. This never proves that a clear row is semantically reusable.
 * It does not parse, materialize nodes, retain foreign symbols, or change M3File.
 */
public final class M3ASTDependencyImpact {
    public static final String SCHEMA = "M3_AST_DEPENDENCY_IMPACT_V1";
    public static final int DIRECT = 1;
    public static final int UNKNOWN_RANGE = 2;
    public static final int ANCESTOR = 4;
    public static final int REFERENCER = 8;
    private M3ASTDependencyImpact() { }

    /**
     * Build the minimal transitive superset of potentially affected occurrence
     * rows. NODE_REF is interpreted conservatively as an owner-to-target
     * dependency: when a target changes, the owner may change. Cyclic and
     * repeated references are handled by one visited mark per occurrence.
     *
     * <p>maxNodes/maxFacts are hard admission budgets, not preallocation hints.
     * A caller combines this with M3FileEdits.Plan.touchedNodes and/or
     * touchedOrigins, never with an inferred source-text correspondence.
     */
    public static Result propagate(M3ASTIndex index, M3ASTFacts facts,
                                   BitSet direct, int maxNodes, int maxFacts) {
        Objects.requireNonNull(index, "original M3ASTIndex");
        Objects.requireNonNull(direct, "original changed-node mask");
        int count = index.nodeCount();
        int factCount = facts == null ? 0 : facts.factCount();
        if (maxNodes < 0 || maxFacts < 0 || count > maxNodes || factCount > maxFacts) {
            throw new IllegalArgumentException("AST impact work budget exceeded");
        }
        if (direct.length() > count) {
            throw new IllegalArgumentException("changed mask includes foreign AST coordinates");
        }
        if (facts != null && facts.index() != index) {
            throw new IllegalArgumentException("typed facts belong to a different AST generation");
        }
        byte[] reasons = new byte[count];
        int[] queue = new int[count];
        int queued = 0;
        for (int row = 0; row < count; row++) {
            int reason = (direct.get(row) ? DIRECT : 0)
                    | (!index.hasExactRange(row) ? UNKNOWN_RANGE : 0);
            if (reason != 0) {
                reasons[row] = (byte) reason;
                queue[queued++] = row;
            }
        }
        // Reverse CSR-like heads and one next pointer per *existing* typed fact.
        // The fact arrays remain owned by M3ASTFacts and are never duplicated.
        int[] heads = new int[count];
        Arrays.fill(heads, -1);
        int[] next = new int[factCount];
        if (facts != null) {
            for (int fact = 0; fact < factCount; fact++) {
                if (facts.kind(fact) == M3ASTFacts.Kind.NODE_REF) {
                    int target = facts.nodeRef(fact);
                    next[fact] = heads[target];
                    heads[target] = fact;
                }
            }
        }
        // Fan-out is parallelizable; the serial queue makes closure reproducible.
        for (int head = 0; head < queued; head++) {
            int node = queue[head];
            int parent = index.parent(node);
            if (parent >= 0) {
                if (reasons[parent] == 0) queue[queued++] = parent;
                reasons[parent] |= (byte) ANCESTOR;
            }
            for (int fact = heads[node]; fact >= 0; fact = next[fact]) {
                int owner = facts.owner(fact);
                if (reasons[owner] == 0) queue[queued++] = owner;
                reasons[owner] |= (byte) REFERENCER;
            }
        }
        return new Result(reasons, queued);
    }

    /**
     * Create direct edit seeds from an original M3ASTSourceOrigins catalogue.
     * An origin's start/end are closed for invalidation (edits at either edge
     * conservatively touch it). The original M3File instance, not URI or text
     * equality, must belong to the catalogue; otherwise refuse lost provenance.
     */
    public static BitSet touchedOrigins(M3ASTSourceOrigins origins, M3File original,
                                        int changedStart, int changedEnd,
                                        int maxOrigins) {
        Objects.requireNonNull(origins, "original source-origin catalogue");
        Objects.requireNonNull(original, "original source generation");
        Objects.checkFromToIndex(changedStart, changedEnd, original.length());
        if (maxOrigins < 0 || origins.originCount() > maxOrigins) {
            throw new IllegalArgumentException("source-origin scan budget exceeded");
        }
        boolean admitted = false;
        for (int row = 0; row < origins.sourceCount(); row++) {
            if (origins.sourceAt(row) == original) {
                admitted = true;
                break;
            }
        }
        if (!admitted) throw new IllegalArgumentException("changed source generation not indexed");
        BitSet changed = new BitSet(origins.index().nodeCount());
        for (int row = 0; row < origins.originCount(); row++) {
            if (origins.source(row) == original
                    && origins.start(row) <= changedEnd
                    && origins.end(row) >= changedStart) {
                changed.set(origins.owner(row));
            }
        }
        return changed;
    }

    /** A detached, immutable row reason image, not a semantic reuse certificate. */
    public static final class Result {
        private final byte[] reasons;
        private final int impacted;
        private Result(byte[] reasons, int impacted) {
            this.reasons = reasons;
            this.impacted = impacted;
        }
        public int nodeCount() { return reasons.length; }
        public int impactedCount() { return impacted; }
        public int unchangedCandidateCount() { return reasons.length - impacted; }
        public boolean impacted(int node) {
            return reasons[Objects.checkIndex(node, reasons.length)] != 0;
        }
        public int reasonBits(int node) {
            return reasons[Objects.checkIndex(node, reasons.length)] & 0xff;
        }
        public BitSet impactedMask() {
            BitSet mask = new BitSet(reasons.length);
            for (int i = 0; i < reasons.length; i++) if (reasons[i] != 0) mask.set(i);
            return mask;
        }
        public int[] impactedRows(int maxResults) {
            if (maxResults < 0 || impacted > maxResults) {
                throw new IllegalArgumentException("AST impact result budget exceeded");
            }
            int[] rows = new int[impacted];
            for (int i = 0, at = 0; i < reasons.length; i++) {
                if (reasons[i] != 0) rows[at++] = i;
            }
            return rows;
        }
    }
}
