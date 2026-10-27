// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;

/** Immutable M3File generation edits using four primitive lanes; no second text or AST owner. */
public final class M3FileEdits {
    private M3FileEdits() { }

    /** Half-open UTF-16 offsets in the ORIGINAL source, not sequential edits. */
    public record Edit(int startUtf16, int oldEndUtf16, String replacement) {
        public Edit {
            if (startUtf16 < 0 || oldEndUtf16 < startUtf16)
                throw new IllegalArgumentException("invalid edit source range");
            Objects.requireNonNull(replacement, "replacement");
        }
    }

    /** Validate the entire batch and output budget before allocating the new source. */
    public static Plan apply(M3File original, List<Edit> edits, int maxEdits, int maxOutputUnits) {
        Objects.requireNonNull(original, "original");
        Objects.requireNonNull(edits, "edits");
        if (maxEdits < 0 || maxOutputUnits < 0 || edits.size() > maxEdits)
            throw new IllegalArgumentException("source edit budget exceeded");
        List<Edit> snapshot = new ArrayList<>(edits.size());
        for (Edit edit : edits) snapshot.add(Objects.requireNonNull(edit, "edit"));
        int count = snapshot.size();
        int[] oldStarts = new int[count], oldEnds = new int[count];
        int[] newStarts = new int[count], newEnds = new int[count];
        int originalLength = original.length();
        long outputLength = originalLength;
        int lastEnd = 0, lastStart = -1;
        for (int i = 0; i < count; i++) {
            Edit edit = snapshot.get(i);
            int start = edit.startUtf16(), end = edit.oldEndUtf16();
            if (end > originalLength || start < lastEnd || start == lastStart
                    || splitsSurrogate(original, start) || splitsSurrogate(original, end))
                throw new IllegalArgumentException("unsorted, overlapping or split-surrogate edit");
            outputLength += (long) edit.replacement().length() - (end - start);
            if (outputLength > maxOutputUnits || outputLength < 0)
                throw new IllegalArgumentException("source output UTF-16 budget exceeded");
            oldStarts[i] = start;
            oldEnds[i] = end;
            lastEnd = end;
            lastStart = start;
        }
        if (outputLength > maxOutputUnits)
            throw new IllegalArgumentException("source output UTF-16 budget exceeded");
        if (count == 0) return new Plan(original, original, oldStarts, oldEnds, newStarts, newEnds);
        StringBuilder text = new StringBuilder((int) outputLength);
        int cursor = 0;
        for (int i = 0; i < count; i++) {
            Edit edit = snapshot.get(i);
            text.append(original.text(), cursor, oldStarts[i]);
            newStarts[i] = text.length();
            text.append(edit.replacement());
            newEnds[i] = text.length();
            cursor = oldEnds[i];
        }
        text.append(original.text(), cursor, originalLength);
        if (text.length() != outputLength) throw new AssertionError("edit length invariant");
        // A newly generated character source must NEVER inherit original raw-byte SHA.
        return new Plan(original, M3File.of(original.uri(), text.toString()),
                oldStarts, oldEnds, newStarts, newEnds);
    }

    private static boolean splitsSurrogate(M3File source, int offset) {
        return offset > 0 && offset < source.length()
                && Character.isHighSurrogate(source.charAt(offset - 1))
                && Character.isLowSurrogate(source.charAt(offset));
    }

    /** Immutable before/after generation identities with sorted, original-coordinate edits. */
    public static final class Plan {
        private final M3File before, after;
        private final int[] oldStarts, oldEnds, newStarts, newEnds;
        private Plan(M3File before, M3File after, int[] oldStarts, int[] oldEnds,
                     int[] newStarts, int[] newEnds) {
            this.before = before; this.after = after;
            this.oldStarts = oldStarts; this.oldEnds = oldEnds;
            this.newStarts = newStarts; this.newEnds = newEnds;
        }
        public M3File before() { return before; }
        public M3File after() { return after; }
        public int editCount() { return oldStarts.length; }
        public int oldStart(int i) { return oldStarts[Objects.checkIndex(i, editCount())]; }
        public int oldEnd(int i) { return oldEnds[Objects.checkIndex(i, editCount())]; }
        public int newStart(int i) { return newStarts[Objects.checkIndex(i, editCount())]; }
        public int newEnd(int i) { return newEnds[Objects.checkIndex(i, editCount())]; }
        public long retainedPrimitiveBytes() { return (long) editCount() * Integer.BYTES * 4; }

        /** -1 at both touched boundaries; untouched coordinates require no guessed affinity. */
        public int translateUntouched(int oldOffset) {
            if (oldOffset < 0 || oldOffset > before.length())
                throw new IndexOutOfBoundsException("old source offset");
            int candidate = atOrBefore(oldStarts, oldOffset);
            if (candidate < 0) return oldOffset;
            if (oldOffset <= oldEnds[candidate]) return -1;
            return Math.addExact(oldOffset, newEnds[candidate] - oldEnds[candidate]);
        }

        /** Inclusive boundary intersection, O(log editCount), conservatively invalidating edits. */
        public boolean touches(int startUtf16, int endUtf16) {
            Objects.checkFromToIndex(startUtf16, endUtf16, before.length());
            int candidate = atOrBefore(oldStarts, endUtf16);
            return candidate >= 0 && oldEnds[candidate] >= startUtf16;
        }

        /**
         * Conservative AST row mask. UNKNOWN ranges are always touched.
         * A clear bit does NOT certify parsing, compiler or semantic reuse.
         */
        public BitSet touchedNodes(M3ASTIndex oldIndex, int maxNodes) {
            Objects.requireNonNull(oldIndex, "oldIndex");
            if (oldIndex.source() != before)
                throw new IllegalArgumentException("AST does not own original M3File generation");
            if (maxNodes < 0 || oldIndex.nodeCount() > maxNodes)
                throw new IllegalArgumentException("AST delta node budget exceeded");
            BitSet bits = new BitSet(oldIndex.nodeCount());
            for (int row = 0; row < oldIndex.nodeCount(); row++)
                if (!oldIndex.hasExactRange(row) || touches(oldIndex.start(row), oldIndex.end(row)))
                    bits.set(row);
            return bits;
        }

        private static int atOrBefore(int[] values, int offset) {
            int found = Arrays.binarySearch(values, offset);
            return found >= 0 ? found : -found - 2;
        }
        public int[] oldStarts() { return Arrays.copyOf(oldStarts, oldStarts.length); }
        public int[] newStarts() { return Arrays.copyOf(newStarts, newStarts.length); }
    }
}
