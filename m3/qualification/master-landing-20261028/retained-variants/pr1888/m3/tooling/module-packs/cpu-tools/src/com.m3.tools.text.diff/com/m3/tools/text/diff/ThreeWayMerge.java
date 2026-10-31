// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.tools.text.diff;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * A conservative three-way merge over two exact UTF-16 edit scripts sharing one base.
 * Independent source ranges are composed, identical edits are incorporated once,
 * and intersecting or boundary-ambiguous changes are returned as conflicts.
 * This is not a GNU diff3 line-hunk parser or an automatic conflict resolver.
 */
public final class ThreeWayMerge {
    /** Maximum retained conflict witnesses; a larger count is explicitly reported. */
    public static final int MAX_CONFLICT_WITNESSES = 1_024;
    private static final Runnable NOOP = () -> { };

    private ThreeWayMerge() { }

    public enum Status { MERGED, CONFLICT, INCOMPLETE }

    /** Exact left/right edits in the *original base* half-open UTF-16 coordinates. */
    public record Conflict(BoundedDiff.Change left, BoundedDiff.Change right) {
        public Conflict {
            Objects.requireNonNull(left, "left");
            Objects.requireNonNull(right, "right");
        }
    }

    /**
     * Immutable result. A conflict or edit-budget miss never offers a partial
     * merged value. The two patches retain exact source/side SHA-256 identities.
     */
    public static final class Result {
        private final Status status;
        private final String merged;
        private final BoundedDiff.Patch leftPatch;
        private final BoundedDiff.Patch rightPatch;
        private final List<BoundedDiff.Change> mergedChanges;
        private final List<Conflict> conflicts;
        private final int conflictCount;

        private Result(Status status, String merged, BoundedDiff.Patch leftPatch,
                       BoundedDiff.Patch rightPatch, List<BoundedDiff.Change> mergedChanges,
                       List<Conflict> conflicts, int conflictCount) {
            this.status = status;
            this.merged = merged;
            this.leftPatch = leftPatch;
            this.rightPatch = rightPatch;
            this.mergedChanges = List.copyOf(mergedChanges);
            this.conflicts = List.copyOf(conflicts);
            this.conflictCount = conflictCount;
        }

        public Status status() { return status; }
        public boolean complete() { return status != Status.INCOMPLETE; }
        public boolean mergeable() { return status == Status.MERGED; }
        public BoundedDiff.Patch leftPatch() { return leftPatch; }
        public BoundedDiff.Patch rightPatch() { return rightPatch; }
        public List<BoundedDiff.Change> mergedChanges() { return mergedChanges; }
        public List<Conflict> conflicts() { return conflicts; }
        public int conflictCount() { return conflictCount; }
        public boolean conflictsComplete() { return conflictCount == conflicts.size(); }

        /** Returns the entire lossless merged text only when no conflict exists. */
        public String merged() {
            if (!mergeable()) throw new IllegalStateException("three-way merge was not conflict-free");
            return merged;
        }
    }

    public static Result merge(String base, String left, String right, int maxEdits) {
        return merge(base, left, right, maxEdits, NOOP);
    }

    /**
     * Each side's distance is bounded independently. An over-budget side makes
     * the whole merge incomplete, never a guessed or partially applied merge.
     * Cancellation is cooperative and the caller owns its monitor/checkpoint.
     */
    public static Result merge(String base, String left, String right, int maxEdits,
                               Runnable checkpoint) {
        Objects.requireNonNull(base, "base");
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        Objects.requireNonNull(checkpoint, "checkpoint");
        checkpoint.run();
        BoundedDiff.Patch leftPatch = BoundedDiff.diff(base, left, maxEdits, checkpoint);
        BoundedDiff.Patch rightPatch = BoundedDiff.diff(base, right, maxEdits, checkpoint);
        if (!leftPatch.complete() || !rightPatch.complete()) {
            return new Result(Status.INCOMPLETE, null, leftPatch, rightPatch,
                    List.of(), List.of(), 0);
        }
        List<BoundedDiff.Change> leftChanges = leftPatch.changes();
        List<BoundedDiff.Change> rightChanges = rightPatch.changes();
        List<Conflict> conflicts = new ArrayList<>();
        int conflictCount = 0;
        int comparisons = 0;
        for (BoundedDiff.Change a : leftChanges) {
            checkpoint.run();
            for (BoundedDiff.Change b : rightChanges) {
                if ((++comparisons & 2047) == 0) checkpoint.run();
                if (!a.equals(b) && intersectsOrTouchesInsertion(a, b)) {
                    conflictCount++;
                    if (conflicts.size() < MAX_CONFLICT_WITNESSES) conflicts.add(new Conflict(a, b));
                }
            }
        }
        if (conflictCount != 0) {
            checkpoint.run();
            return new Result(Status.CONFLICT, null, leftPatch, rightPatch,
                    List.of(), conflicts, conflictCount);
        }

        List<BoundedDiff.Change> composed = new ArrayList<>(leftChanges.size() + rightChanges.size());
        composed.addAll(leftChanges);
        for (BoundedDiff.Change candidate : rightChanges) {
            checkpoint.run();
            if (!leftChanges.contains(candidate)) composed.add(candidate);
        }
        composed.sort(Comparator.comparingInt(BoundedDiff.Change::start)
                .thenComparingInt(BoundedDiff.Change::end));
        StringBuilder result = new StringBuilder(Math.max(base.length(), Math.max(left.length(), right.length())));
        int sourceOffset = 0;
        for (BoundedDiff.Change change : composed) {
            checkpoint.run();
            if (change.start() < sourceOffset || change.end() > base.length()) {
                throw new IllegalStateException("non-conflicting scripts overlapped");
            }
            result.append(base, sourceOffset, change.start()).append(change.inserted());
            sourceOffset = change.end();
        }
        result.append(base, sourceOffset, base.length());
        checkpoint.run();
        return new Result(Status.MERGED, result.toString(), leftPatch, rightPatch,
                composed, List.of(), 0);
    }

    /**
     * Replacement intervals use strict half-open intersection. Insertions at
     * either boundary of a replacement are conservatively ambiguous because
     * no outside ordering preference is authorized; adjacent replacements are
     * independent, while divergent inserts at the same position conflict.
     */
    private static boolean intersectsOrTouchesInsertion(BoundedDiff.Change a, BoundedDiff.Change b) {
        boolean aInsert = a.start() == a.end();
        boolean bInsert = b.start() == b.end();
        if (aInsert && bInsert) return a.start() == b.start();
        if (aInsert) return b.start() <= a.start() && a.start() <= b.end();
        if (bInsert) return a.start() <= b.start() && b.start() <= a.end();
        return a.start() < b.end() && b.start() < a.end();
    }
}
