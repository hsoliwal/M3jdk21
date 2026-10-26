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
