// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.Arrays;
import java.util.Objects;

/**
 * Export a bounded M3FileEditBatch as exact Tree-sitter-style sequential edit
 * frames. Coordinates follow UTF-8 bytes with zero-based row and BYTE column.
 * 
 * <p>Frames deliberately execute right-to-left so each stage uses the old
 * source's unshifted byte offsets. Byte positions and newline points are
 * measured from the actual pre/post stage images, including CRLF seams, not
 * inferred from a potentially invalid arithmetic approximation.
 *
 * <p>Current implementation materializes intermediate immutable images for
 * proof-quality coordinates; CPU and allocation are O(edits * source size).
 * A strict caller-supplied work budget prevents accidentally using this path
 * as an unbounded huge-file incremental editor. It is not a Tree-sitter parser.
 */
public final class M3FileEditFrames {
    public static final long DEFAULT_WORK_BUDGET = 64L << 20;

    /** Native Tree-sitter TSPoint-compatible UTF-8 byte position. */
    public record Point(int row, int byteColumn) {
        public Point {
            if (row < 0 || byteColumn < 0) throw new IllegalArgumentException("negative point");
        }
    }
    /** TSInputEdit-shaped data, but without a native dependency or JNI ABI. */
    public record Frame(int originalEditIndex, int startByte, int oldEndByte, int newEndByte,
                        Point startPoint, Point oldEndPoint, Point newEndPoint) {
        public Frame {
            if (originalEditIndex < 0 || startByte < 0 || oldEndByte < startByte
                    || newEndByte < startByte)
                throw new IllegalArgumentException("invalid incremental edit geometry");
            Objects.requireNonNull(startPoint, "startPoint");
            Objects.requireNonNull(oldEndPoint, "oldEndPoint");
            Objects.requireNonNull(newEndPoint, "newEndPoint");
        }
    }

    private final String oldSha256, newSha256;
    private final Frame[] reverseFrames;
    private final long materializedWorkBytes;

    private M3FileEditFrames(String oldHash, String newHash, Frame[] frames, long work) {
        oldSha256 = oldHash;
        newSha256 = newHash;
        reverseFrames = frames;
        materializedWorkBytes = work;
    }

    public static M3FileEditFrames plan(M3FileSource original, M3FileEditBatch.Applied applied) {
        return plan(original, applied, DEFAULT_WORK_BUDGET);
    }
    public static M3FileEditFrames plan(M3FileSource original, M3FileEditBatch.Applied applied,
                                        long maxMaterializedByteWork) {
        Objects.requireNonNull(original, "original source");
        Objects.requireNonNull(applied, "applied edits");
        if (maxMaterializedByteWork < 0) throw new IllegalArgumentException("negative work budget");
        M3FileEditBatch.ChangeMap map = applied.map();
        map.requireOld(original);
        map.requireNew(applied.source());
        byte[] finalBytes = applied.source().bytes();
        byte[] working = original.bytes();
        Frame[] frames = new Frame[map.changes()];
        long work = 0;
        for (int i = map.changes() - 1, ordinal = 0; i >= 0; i--, ordinal++) {
            int start = map.oldStart(i), oldEnd = map.oldEnd(i);
            int insertionLength = map.newEnd(i) - map.newStart(i);
            int newEnd = Math.addExact(start, insertionLength);
            int nextLength = Math.addExact(working.length - (oldEnd - start), insertionLength);
            long stepWork = (long) working.length + nextLength;
            if (stepWork > maxMaterializedByteWork - work)
                throw new IllegalArgumentException("incremental source plan exceeds byte-work budget");
            work += stepWork;
            // Fail closed for an invalid UTF-8 file or an edit splitting a code point.
            M3FileSource oldImage = M3FileSource.fromBytes(working);
            oldImage.utf16Length();
            oldImage.utf16AtByte(start);
            oldImage.utf16AtByte(oldEnd);
            Point first = point(oldImage, start);
            Point last = point(oldImage, oldEnd);
            byte[] updated = new byte[nextLength];
            System.arraycopy(working, 0, updated, 0, start);
            System.arraycopy(finalBytes, map.newStart(i), updated, start, insertionLength);
            System.arraycopy(working, oldEnd, updated, newEnd, working.length - oldEnd);
            M3FileSource nextImage = M3FileSource.fromBytes(updated);
            nextImage.utf16Length();
            nextImage.utf16AtByte(newEnd);
            Point nextPoint = point(nextImage, newEnd);
            frames[ordinal] = new Frame(i, start, oldEnd, newEnd, first, last, nextPoint);
            working = updated;
        }
        if (!Arrays.equals(working, finalBytes))
            throw new IllegalStateException("source generations diverged during edit-frame replay");
        return new M3FileEditFrames(original.sha256(), applied.source().sha256(), frames, work);
    }

    /**
     * LF-only fast lane for immutable UTF-8 source. Reuses the original source's
     * indexed line starts, and scans each inserted fragment exactly once.
     * Unlike {@link #plan(M3FileSource, M3FileEditBatch.Applied, long)}, no
     * intermediate full-file generations are materialized. All insertion bytes
     * and old source boundaries must be complete UTF-8 scalars. For CR/CRLF
     * inputs use {@code plan(...)}: its seam-aware replay remains authoritative.
     *
     * <p>The work budget counts original/final byte observations and twice the
     * inserted byte count (including the bounded validation copy). This method
     * is an explicit specialized lane, not an implicit fallback or an AST parse.
     */
    public static M3FileEditFrames planLinearLf(M3FileSource original,
            M3FileEditBatch.Applied applied, long maxByteWork) {
        Objects.requireNonNull(original, "original source");
        Objects.requireNonNull(applied, "applied edits");
        if (maxByteWork < 0) throw new IllegalArgumentException("negative work budget");
        M3FileEditBatch.ChangeMap map = applied.map();
        map.requireOld(original);
        map.requireNew(applied.source());
        if (map.changes() == 0) {
            return new M3FileEditFrames(original.sha256(), applied.source().sha256(),
                    new Frame[0], 0);
        }
        long work = (long) original.length() + applied.source().length();
        if (work > maxByteWork)
            throw new IllegalArgumentException("linear edit observation exceeds byte budget");
        byte[] before = original.bytes(), after = applied.source().bytes();
        for (byte b : before) if (b == '\r')
            throw new IllegalArgumentException("CR/CRLF source requires seam-aware planner");
        for (byte b : after) if (b == '\r')
            throw new IllegalArgumentException("CR/CRLF source requires seam-aware planner");
        original.utf16Length();
        applied.source().utf16Length();
        Frame[] frames = new Frame[map.changes()];
        int oldCursor = 0, newCursor = 0;
        for (int i = 0; i < map.changes(); i++) {
            int start = map.oldStart(i), end = map.oldEnd(i);
            int newStart = map.newStart(i), newEnd = map.newEnd(i);
            original.utf16AtByte(start);
            original.utf16AtByte(end);
            int preserved = start - oldCursor;
            if (preserved < 0 || newStart - newCursor != preserved
                    || Arrays.mismatch(before, oldCursor, start, after,
                            newCursor, newStart) >= 0)
                throw new IllegalStateException("unchanged source segment differs");
            int inserted = newEnd - newStart;
            if (inserted > (maxByteWork - work) / 2)
                throw new IllegalArgumentException("linear edit validation exceeds byte budget");
            work += (long) inserted * 2;
            // Validate inserted scalar boundaries independently of its neighbors:
            // every intermediate right-to-left edit image remains well-formed.
            byte[] fragment = Arrays.copyOfRange(after, newStart, newEnd);
            M3FileSource.fromBytes(fragment).utf16Length();
            int firstRow = original.lineAtByte(start);
            int lastRow = original.lineAtByte(end);
            Point firstPoint = new Point(firstRow, original.columnBytes(start));
            Point oldEndPoint = new Point(lastRow, original.columnBytes(end));
            int newRow = firstRow, newColumn = firstPoint.byteColumn();
            for (byte b : fragment) {
                if (b == '\n') { newRow = Math.addExact(newRow, 1); newColumn = 0; }
                else newColumn = Math.addExact(newColumn, 1);
            }
            frames[map.changes() - 1 - i] = new Frame(i, start, end,
                    Math.addExact(start, inserted), firstPoint,
                    oldEndPoint, new Point(newRow, newColumn));
            oldCursor = end;
            newCursor = newEnd;
        }
        if (before.length - oldCursor != after.length - newCursor
                || Arrays.mismatch(before, oldCursor, before.length, after,
                        newCursor, after.length) >= 0)
            throw new IllegalStateException("unchanged source tail differs");
        return new M3FileEditFrames(original.sha256(), applied.source().sha256(), frames, work);
    }

    /** Conservative planner when no LF-only proof is available. */
    public static M3FileEditFrames planLinearLf(M3FileSource original,
            M3FileEditBatch.Applied applied) {
        return planLinearLf(original, applied, DEFAULT_WORK_BUDGET);
    }
    public int size() { return reverseFrames.length; }
    /** In the order a parser must apply edits to its previous tree. */
    public Frame frame(int ordinal) { return reverseFrames[Objects.checkIndex(ordinal, size())]; }
    public String originalSha256() { return oldSha256; }
    public String finalSha256() { return newSha256; }
    public long materializedByteWork() { return materializedWorkBytes; }
    private static Point point(M3FileSource source, int byteOffset) {
        return new Point(source.lineAtByte(byteOffset), source.columnBytes(byteOffset));
    }
}
