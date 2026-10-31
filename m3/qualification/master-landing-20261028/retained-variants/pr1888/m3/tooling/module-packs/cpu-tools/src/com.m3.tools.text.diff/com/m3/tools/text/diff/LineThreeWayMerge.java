// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.tools.text.diff;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Conservative three-way composition of exact, source-relative {@link LineDiff}
 * scripts. Each complete line includes its original line terminator; no line
 * ending, Unicode unit, or missing final terminator is normalized.
 *
 * <p>Identical edits are retained once, disjoint replacements remain independent,
 * and intersecting edits or an insertion touching a replacement boundary are
 * reported for review. No conflict markers or partial merged text are produced.
 * This is a bounded SDK operation, not a GNU {@code diff3} compatibility layer.
 */
public final class LineThreeWayMerge {
    public static final int MAX_CONFLICT_WITNESSES = ThreeWayMerge.MAX_CONFLICT_WITNESSES;
    private static final Runnable NOOP = () -> { };

    private LineThreeWayMerge() { }

    /** Divergent edits in half-open line and UTF-16 coordinates of the same base. */
    public record Conflict(LineDiff.LineChange left, LineDiff.LineChange right) {
        public Conflict {
            Objects.requireNonNull(left, "left");
            Objects.requireNonNull(right, "right");
        }
    }

    /** Immutable two-script evidence with a result only for an unambiguous merge. */
    public static final class Result {
        private final ThreeWayMerge.Status status;
        private final String merged;
        private final LineDiff.Patch leftPatch;
        private final LineDiff.Patch rightPatch;
        private final List<LineDiff.LineChange> mergedChanges;
        private final List<Conflict> conflicts;
        private final int conflictCount;

        private Result(ThreeWayMerge.Status status, String merged, LineDiff.Patch leftPatch,
                       LineDiff.Patch rightPatch, List<LineDiff.LineChange> mergedChanges,
                       List<Conflict> conflicts, int conflictCount) {
            this.status = status;
            this.merged = merged;
            this.leftPatch = leftPatch;
            this.rightPatch = rightPatch;
            this.mergedChanges = List.copyOf(mergedChanges);
            this.conflicts = List.copyOf(conflicts);
            this.conflictCount = conflictCount;
        }

        public ThreeWayMerge.Status status() { return status; }
        public boolean complete() { return status != ThreeWayMerge.Status.INCOMPLETE; }
        public boolean mergeable() { return status == ThreeWayMerge.Status.MERGED; }
        public LineDiff.Patch leftPatch() { return leftPatch; }
        public LineDiff.Patch rightPatch() { return rightPatch; }
        public List<LineDiff.LineChange> mergedChanges() { return mergedChanges; }
        public List<Conflict> conflicts() { return conflicts; }
        public int conflictCount() { return conflictCount; }
        public boolean conflictsComplete() { return conflictCount == conflicts.size(); }

        /** Refuses conflict and over-budget states rather than manufacturing a result. */
        public String merged() {
            if (!mergeable()) throw new IllegalStateException("line three-way merge is not conflict-free");
            return merged;
        }
    }

    public static Result merge(String base, String left, String right, int maxEdits) {
        return merge(base, left, right, maxEdits, NOOP);
    }

    /**
     * Each side has a separate line-edit budget. The two existing line-indexed
     * diffs independently seal the same base and their respective target text.
     * Cancellation is cooperative and the supplied checkpoint remains borrowed.
     */
    public static Result merge(String base, String left, String right, int maxEdits,
                               Runnable checkpoint) {
        Objects.requireNonNull(base, "base");
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        Objects.requireNonNull(checkpoint, "checkpoint");
        checkpoint.run();
        LineDiff.Patch leftPatch = LineDiff.diff(base, left, maxEdits, checkpoint);
        LineDiff.Patch rightPatch = LineDiff.diff(base, right, maxEdits, checkpoint);
        if (!leftPatch.complete() || !rightPatch.complete()) {
            return new Result(ThreeWayMerge.Status.INCOMPLETE, null, leftPatch, rightPatch,
                    List.of(), List.of(), 0);
        }
        if (leftPatch.sourceLines() != rightPatch.sourceLines()
                || leftPatch.sourceLength() != rightPatch.sourceLength()
                || !leftPatch.sourceSha256().equals(rightPatch.sourceSha256())) {
            throw new IllegalStateException("three-way line source identity drift");
        }

        List<LineDiff.LineChange> leftChanges = leftPatch.changes();
        List<LineDiff.LineChange> rightChanges = rightPatch.changes();
        List<Conflict> conflicts = new ArrayList<>();
        int conflictCount = 0;
        int comparisons = 0;
        for (LineDiff.LineChange a : leftChanges) {
            checkpoint.run();
            for (LineDiff.LineChange b : rightChanges) {
                if ((++comparisons & 2047) == 0) checkpoint.run();
                if (!a.equals(b) && intersectsOrTouchesInsertion(a, b)) {
                    conflictCount++;
                    if (conflicts.size() < MAX_CONFLICT_WITNESSES) conflicts.add(new Conflict(a, b));
                }
            }
        }
        if (conflictCount != 0) {
            checkpoint.run();
            return new Result(ThreeWayMerge.Status.CONFLICT, null, leftPatch, rightPatch,
                    List.of(), conflicts, conflictCount);
        }

        List<LineDiff.LineChange> composed = new ArrayList<>(leftChanges.size() + rightChanges.size());
        composed.addAll(leftChanges);
        for (LineDiff.LineChange candidate : rightChanges) {
            checkpoint.run();
            if (!leftChanges.contains(candidate)) composed.add(candidate);
        }
        composed.sort(Comparator.comparingInt(LineDiff.LineChange::startLine)
                .thenComparingInt(LineDiff.LineChange::endLine));
        StringBuilder output = new StringBuilder(Math.max(base.length(),
                Math.max(left.length(), right.length())));
        int sourceLine = 0;
        int sourceUtf16 = 0;
        for (LineDiff.LineChange change : composed) {
            checkpoint.run();
            if (change.startLine() < sourceLine || change.startUtf16() < sourceUtf16
                    || change.endUtf16() > base.length()) {
                throw new IllegalStateException("non-conflicting line scripts overlapped");
            }
            output.append(base, sourceUtf16, change.startUtf16()).append(change.inserted());
            sourceLine = change.endLine();
            sourceUtf16 = change.endUtf16();
        }
        output.append(base, sourceUtf16, base.length());
        checkpoint.run();
        return new Result(ThreeWayMerge.Status.MERGED, output.toString(), leftPatch, rightPatch,
                composed, List.of(), 0);
    }

    /** Strict intersection of replacements; boundary-touching insertions conflict. */
    private static boolean intersectsOrTouchesInsertion(LineDiff.LineChange a, LineDiff.LineChange b) {
        boolean aInsert = a.startLine() == a.endLine();
        boolean bInsert = b.startLine() == b.endLine();
        if (aInsert && bInsert) return a.startLine() == b.startLine();
        if (aInsert) return b.startLine() <= a.startLine() && a.startLine() <= b.endLine();
        if (bInsert) return a.startLine() <= b.startLine() && b.startLine() <= a.endLine();
        return a.startLine() < b.endLine() && b.startLine() < a.endLine();
    }
}
