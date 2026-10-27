// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Source-sealed multi-edit pass over one immutable M3FileSource generation.
 * All edits refer to the SAME original byte image; there is one materialization
 * and one new content fingerprint, instead of repeatedly copying the entire
 * source for each editor/parser change. This class does not own a filesystem,
 * watcher, persistent delta ledger, incremental parser or rewrite authority.
 *
 * <p>Strictly ordered disjoint source ranges; duplicate or ambiguous zero-width
 * insertion coordinates are rejected. UTF-16 edits use the original source's
 * strict UTF-8 coordinate conversion, rejecting a split surrogate/codepoint.
 */
public final class M3FileEditBatch {
    public enum Affinity { LEFT, RIGHT }
    private final M3FileSource base;
    private final List<Change> pending = new ArrayList<>();
    private int lastOldStart = -1, lastOldEnd;
    private boolean finished;
    private record Change(int start, int end, byte[] inserted) { }

    private M3FileEditBatch(M3FileSource base) {
        this.base = Objects.requireNonNull(base, "base");
    }
    public static M3FileEditBatch on(M3FileSource source) { return new M3FileEditBatch(source); }
    public String originalSha256() { return base.sha256(); }
    public int edits() { return pending.size(); }

    public M3FileEditBatch replaceBytes(int from, int to, byte[] inserted) {
        if (finished) throw new IllegalStateException("batch already materialized");
        Objects.checkFromToIndex(from, to, base.length());
        byte[] stable = Objects.requireNonNull(inserted, "inserted").clone();
        if (from < lastOldEnd || from == lastOldStart)
            throw new IllegalArgumentException("overlapping, unordered or ambiguous insertion ranges");
        pending.add(new Change(from, to, stable));
        lastOldStart = from;
        lastOldEnd = to;
        return this;
    }
    /**
     * Java/JDT/editor UTF-16 offsets; refuses malformed replacement strings
     * rather than silently substituting '?' for unpaired surrogates.
     */
    public M3FileEditBatch replaceUtf16(int from, int to, String inserted) {
        Objects.requireNonNull(inserted, "inserted");
        if (!isWellFormed(inserted)) throw new IllegalArgumentException("invalid UTF-16 replacement");
        if (from < 0 || to < from) throw new IndexOutOfBoundsException("inverted UTF-16 edit");
        return replaceBytes(base.byteAtUtf16(from), base.byteAtUtf16(to),
                inserted.getBytes(StandardCharsets.UTF_8));
    }
    public Applied apply() {
        if (finished) throw new IllegalStateException("batch already materialized");
        finished = true;
        if (pending.isEmpty()) return new Applied(base, ChangeMap.empty(base));
        int finalLength = base.length();
        for (Change edit : pending) {
            finalLength = Math.addExact(finalLength - (edit.end - edit.start), edit.inserted.length);
        }
        byte[] oldBytes = base.bytes();
        byte[] newBytes = new byte[finalLength];
        int oldCursor = 0, newCursor = 0, i = 0;
        int[] oldStarts = new int[pending.size()], oldEnds = new int[pending.size()];
        int[] newStarts = new int[pending.size()], newEnds = new int[pending.size()];
        for (Change edit : pending) {
            int untouched = edit.start - oldCursor;
            System.arraycopy(oldBytes, oldCursor, newBytes, newCursor, untouched);
            newCursor += untouched;
            oldStarts[i] = edit.start;
            oldEnds[i] = edit.end;
            newStarts[i] = newCursor;
            System.arraycopy(edit.inserted, 0, newBytes, newCursor, edit.inserted.length);
            newCursor += edit.inserted.length;
            newEnds[i] = newCursor;
            oldCursor = edit.end;
            i++;
        }
        System.arraycopy(oldBytes, oldCursor, newBytes, newCursor, oldBytes.length - oldCursor);
        newCursor += oldBytes.length - oldCursor;
        if (newCursor != finalLength) throw new AssertionError("incorrect source delta arithmetic");
        M3FileSource next = M3FileSource.fromBytes(newBytes);
        return new Applied(next, new ChangeMap(base.sha256(), next.sha256(), base.length(),
                finalLength, oldStarts, oldEnds, newStarts, newEnds));
    }
    public record Applied(M3FileSource source, ChangeMap map) {
        public Applied {
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(map, "map");
            if (!source.sha256().equals(map.newSha256()))
                throw new IllegalArgumentException("source generation and map differ");
        }
    }
    public static final class ChangeMap {
        private final String oldHash, newHash;
        private final int oldLength, newLength;
        private final int[] oldStart, oldEnd, newStart, newEnd;
        private ChangeMap(String oldHash, String newHash, int oldLength, int newLength,
                          int[] oldStart, int[] oldEnd, int[] newStart, int[] newEnd) {
            this.oldHash = oldHash; this.newHash = newHash;
            this.oldLength = oldLength; this.newLength = newLength;
            this.oldStart = oldStart; this.oldEnd = oldEnd;
            this.newStart = newStart; this.newEnd = newEnd;
        }
        private static ChangeMap empty(M3FileSource source) {
            return new ChangeMap(source.sha256(), source.sha256(), source.length(), source.length(),
                    new int[0], new int[0], new int[0], new int[0]);
        }
        public int changes() { return oldStart.length; }
        public String oldSha256() { return oldHash; }
        public String newSha256() { return newHash; }
        public int oldLength() { return oldLength; }
        public int newLength() { return newLength; }
        public int oldStart(int i) { return oldStart[Objects.checkIndex(i, changes())]; }
        public int oldEnd(int i) { return oldEnd[Objects.checkIndex(i, changes())]; }
        public int newStart(int i) { return newStart[Objects.checkIndex(i, changes())]; }
        public int newEnd(int i) { return newEnd[Objects.checkIndex(i, changes())]; }
        public void requireOld(M3FileSource source) {
            if (source.length() != oldLength || !source.sha256().equals(oldHash))
                throw new IllegalStateException("stale source generation for change map");
        }
        public void requireNew(M3FileSource source) {
            if (source.length() != newLength || !source.sha256().equals(newHash))
                throw new IllegalStateException("stale destination generation for change map");
        }
        /** Returns -1 for interior deleted bytes; endpoints honor caller affinity. */
        public int oldToNew(int position, Affinity affinity) {
            Objects.checkIndex(position, oldLength + 1);
            Objects.requireNonNull(affinity, "affinity");
            int correction = 0;
            for (int i = 0; i < changes(); i++) {
                if (position < oldStart[i]) break;
                if (position == oldStart[i]) {
                    return affinity == Affinity.LEFT ? newStart[i] : newEnd[i];
                }
                if (position < oldEnd[i]) return -1;
                correction = newEnd[i] - oldEnd[i];
            }
            return Math.addExact(position, correction);
        }
        /** Returns -1 for the interior of inserted bytes. */
        public int newToOld(int position, Affinity affinity) {
            Objects.checkIndex(position, newLength + 1);
            Objects.requireNonNull(affinity, "affinity");
            int correction = 0;
            for (int i = 0; i < changes(); i++) {
                if (position < newStart[i]) break;
                if (position == newStart[i]) {
                    return affinity == Affinity.LEFT ? oldStart[i] : oldEnd[i];
                }
                if (position < newEnd[i]) return -1;
                correction = oldEnd[i] - newEnd[i];
            }
            return Math.addExact(position, correction);
        }
        /** Conservatively includes insertion exactly on a span boundary. */
        public boolean touchesOldSpan(int from, int to) {
            Objects.checkFromToIndex(from, to, oldLength);
            for (int i = 0; i < changes(); i++) {
                if (oldStart[i] > to) break;
                if (oldStart[i] == oldEnd[i]) {
                    if (from <= oldStart[i] && oldStart[i] <= to) return true;
                } else if (from < oldEnd[i] && oldStart[i] < to) {
                    return true;
                }
            }
            return false;
        }
        public int[] unchangedOldSpan(int from, int to) {
            Objects.checkFromToIndex(from, to, oldLength);
            if (touchesOldSpan(from, to)) return new int[0];
            return new int[] {oldToNew(from, Affinity.RIGHT), oldToNew(to, Affinity.LEFT)};
        }
    }
    private static boolean isWellFormed(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isHighSurrogate(c)) {
                if (++i == value.length() || !Character.isLowSurrogate(value.charAt(i))) return false;
            } else if (Character.isLowSurrogate(c)) return false;
        }
        return true;
    }
}
