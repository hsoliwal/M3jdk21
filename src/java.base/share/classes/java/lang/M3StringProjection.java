/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package java.lang;

import java.util.Objects;

/**
 * Immutable logical UTF-16 selection over canonical M3/String storage.
 *
 * <p>A selection never retains Java text payload, mutable ingress masks, a second interner,
 * or per-character wrapper objects. The backing String or M3String owns the characters. The
 * selection owns only exact source-coordinate metadata. Nested selections collapse to the
 * same root rather than building a chain of per-character virtual objects.</p>
 *
 * <p>Not a subtype of java.lang.String: it is a package-private, explicitly opted-in
 * CharSequence execution view. String object identity, constructors and ABI are unchanged.
 * Materialization of a fresh String/char[] is explicit.</p>
 */
final class M3StringProjection implements CharSequence {
    enum Kind { EMPTY, IDENTITY, RANGE, RUNS, BITMAP }

    private static final int WINDOW = 1024;

    /** Only immutable canonical/String roots are admitted. */
    private final CharSequence root;
    /** Immutable physical-position mapping, independently shared by nested subsequences. */
    private final Geometry geometry;
    private final int from;
    private final int length;

    private M3StringProjection(CharSequence root, Geometry geometry, int from, int length) {
        this.root = Objects.requireNonNull(root, "root");
        this.geometry = Objects.requireNonNull(geometry, "geometry");
        Objects.checkFromIndexSize(from, length, geometry.length());
        this.from = from;
        this.length = length;
    }

    static M3StringProjection select(CharSequence input, boolean[] selected) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(selected, "selected");
        if (selected.length != input.length()) {
            throw new IllegalArgumentException("mask length differs from logical UTF-16 length");
        }
        if (!(input instanceof String || input instanceof M3String
                || input instanceof M3StringProjection)) {
            throw new IllegalArgumentException("projection requires immutable String/M3 owner");
        }

        M3StringProjection prior = input instanceof M3StringProjection view ? view : null;
        CharSequence owner = prior != null ? prior.root : input;
        int count = 0;
        int runs = 0;
        int first = -1;
        int last = -2;
        for (int i = 0; i < selected.length; i++) {
            if (!selected[i]) continue;
            int sourceIndex = prior == null ? i : prior.sourceIndexAt(i);
            if (sourceIndex <= last) {
                throw new IllegalStateException("selection must preserve source order");
            }
            if (sourceIndex != last + 1) runs++;
            if (first < 0) first = sourceIndex;
            last = sourceIndex;
            count++;
        }

        if (count == 0) {
            return new M3StringProjection(owner, new Range(Kind.EMPTY, 0, 0), 0, 0);
        }
        if (count == input.length()) {
            if (prior != null) return prior;
            return new M3StringProjection(owner, new Range(Kind.IDENTITY, 0, count), 0, count);
        }
        if (runs == 1) {
            return new M3StringProjection(owner, new Range(Kind.RANGE, first, count), 0, count);
        }

        long spanLength = (long) last - first + 1L;
        long bitmapWords = (spanLength + 63L) >>> 6;
        long runBytes = 2L * Integer.BYTES * runs;
        long bitmapBytes = Long.BYTES * bitmapWords + Integer.BYTES * (bitmapWords + 1L);
        if (runBytes <= bitmapBytes) {
            int[] starts = new int[runs];
            int[] ends = new int[runs];
            int previous = -2;
            int runIndex = -1;
            int visible = 0;
            for (int i = 0; i < selected.length; i++) {
                if (!selected[i]) continue;
                int absolute = prior == null ? i : prior.sourceIndexAt(i);
                if (absolute != previous + 1) {
                    starts[++runIndex] = absolute;
                }
                ends[runIndex] = ++visible;
                previous = absolute;
            }
            return new M3StringProjection(owner, new Runs(starts, ends, count), 0, count);
        }
        int words = Math.toIntExact(bitmapWords);
        long[] bits = new long[words];
        for (int i = 0; i < selected.length; i++) {
            if (!selected[i]) continue;
            int absolute = prior == null ? i : prior.sourceIndexAt(i);
            int relative = absolute - first;
            bits[relative >>> 6] |= 1L << (relative & 63);
        }
        int[] cumulative = new int[words + 1];
        for (int i = 0; i < words; i++) {
            cumulative[i + 1] = Math.addExact(cumulative[i], Long.bitCount(bits[i]));
        }
        if (cumulative[words] != count) {
            throw new IllegalStateException("selected-bit count mismatch");
        }
        return new M3StringProjection(owner, new Bitmap(first, bits, cumulative), 0, count);
    }

    Kind kind() { return geometry.kind(); }

    /** The index in the original immutable backing for a visible UTF-16 code unit. */
    int sourceIndexAt(int index) {
        return geometry.sourceIndexAt(Math.addExact(from, Objects.checkIndex(index, length)));
    }

    @Override public int length() { return length; }

    @Override public char charAt(int index) { return root.charAt(sourceIndexAt(index)); }

    @Override public M3StringProjection subSequence(int begin, int end) {
        Objects.checkFromToIndex(begin, end, length);
        if (begin == 0 && end == length) return this;
        return new M3StringProjection(root, geometry, from + begin, end - begin);
    }

    /** A selection of a selection is flattened into root coordinates, never nested. */
    M3StringProjection select(boolean[] selected) { return select(this, selected); }

    /** Copy visible runs directly; no lambda, result wrapper, or per-call cursor array. */
    void getChars(int sourceBegin, int sourceEnd, char[] destination, int destinationBegin) {
        Objects.requireNonNull(destination, "destination");
        Objects.checkFromToIndex(sourceBegin, sourceEnd, length);
        Objects.checkFromIndexSize(destinationBegin, sourceEnd - sourceBegin, destination.length);
        geometry.copy(root, from + sourceBegin, from + sourceEnd, destination, destinationBegin);
    }

    private static void copyRun(CharSequence root, int start, int end,
                                char[] destination, int destinationBegin) {
        if (root instanceof String text) {
            text.getChars(start, end, destination, destinationBegin);
        } else if (root instanceof M3String indexed) {
            indexed.getChars(start, end, destination, destinationBegin);
        } else {
            throw new IllegalStateException("noncanonical projection root");
        }
    }

    char[] toCharArray() {
        char[] output = new char[length];
        getChars(0, length, output, 0);
        return output;
    }

    /** Exact String polynomial hash over the logical UTF-16 view. */
    int javaHashCode() {
        if (length == 0) return 0;
        char[] scratch = new char[Math.min(length, WINDOW)];
        int hash = 0;
        for (int at = 0; at < length; ) {
            int amount = Math.min(scratch.length, length - at);
            getChars(at, at + amount, scratch, 0);
            for (int j = 0; j < amount; j++) hash = 31 * hash + scratch[j];
            at += amount;
        }
        return hash;
    }

    int codePointAt(int index) {
        Objects.checkIndex(index, length);
        return Character.codePointAt(this, index);
    }

    int codePointCount(int begin, int end) {
        Objects.checkFromToIndex(begin, end, length);
        return Character.codePointCount(this, begin, end);
    }

    int offsetByCodePoints(int index, int offset) {
        Objects.checkFromIndexSize(index, 0, length);
        return Character.offsetByCodePoints(this, index, offset);
    }

    boolean contentEquals(CharSequence other) {
        Objects.requireNonNull(other, "other");
        if (length != other.length()) return false;
        for (int i = 0; i < length; i++) {
            if (charAt(i) != other.charAt(i)) return false;
        }
        return true;
    }

    /** Lexical UTF-16 comparison without constructing another String or entry object. */
    int compareContent(CharSequence other) {
        Objects.requireNonNull(other, "other");
        int limit = Math.min(length, other.length());
        for (int i = 0; i < limit; i++) {
            int difference = charAt(i) - other.charAt(i);
            if (difference != 0) return difference;
        }
        return Integer.compare(length, other.length());
    }

    boolean startsWith(CharSequence prefix) {
        Objects.requireNonNull(prefix, "prefix");
        if (prefix.length() > length) return false;
        for (int i = 0; i < prefix.length(); i++) {
            if (charAt(i) != prefix.charAt(i)) return false;
        }
        return true;
    }

    boolean endsWith(CharSequence suffix) {
        Objects.requireNonNull(suffix, "suffix");
        if (suffix.length() > length) return false;
        int offset = length - suffix.length();
        for (int i = 0; i < suffix.length(); i++) {
            if (charAt(offset + i) != suffix.charAt(i)) return false;
        }
        return true;
    }

    /** The plan snapshots only prefix states; its needle must itself be immutable. */
    static final class LiteralPlan {
        private final CharSequence needle;
        private final int[] prefix;

        private LiteralPlan(CharSequence needle, int[] prefix) {
            this.needle = needle;
            this.prefix = prefix;
        }

        static LiteralPlan prepare(CharSequence needle) {
            Objects.requireNonNull(needle, "needle");
            if (!(needle instanceof String || needle instanceof M3String
                    || needle instanceof M3StringProjection)) {
                throw new IllegalArgumentException("prepared literal must be immutable");
            }
            int[] prefix = new int[needle.length()];
            for (int i = 1, matched = 0; i < prefix.length; i++) {
                char current = needle.charAt(i);
                while (matched != 0 && current != needle.charAt(matched)) {
                    matched = prefix[matched - 1];
                }
                if (current == needle.charAt(matched)) matched++;
                prefix[i] = matched;
            }
            return new LiteralPlan(needle, prefix);
        }
    }

    /** Streaming KMP: one bounded window, reused prepared prefix states, no flattening. */
    int indexOf(LiteralPlan plan, int fromIndex) {
        Objects.requireNonNull(plan, "plan");
        int fromIndexClamped = Math.clamp(fromIndex, 0, length);
        int patternLength = plan.needle.length();
        if (patternLength == 0) return fromIndexClamped;
        if (patternLength > length - fromIndexClamped) return -1;
        char[] window = new char[Math.min(WINDOW, length - fromIndexClamped)];
        int matched = 0;
        for (int base = fromIndexClamped; base < length;) {
            int amount = Math.min(window.length, length - base);
            getChars(base, base + amount, window, 0);
            for (int offset = 0; offset < amount; offset++) {
                char value = window[offset];
                while (matched != 0 && value != plan.needle.charAt(matched)) {
                    matched = plan.prefix[matched - 1];
                }
                if (value == plan.needle.charAt(matched)) matched++;
                if (matched == patternLength) return base + offset - patternLength + 1;
            }
            base += amount;
        }
        return -1;
    }

    /** Explicit ABI boundary. Native/JDK admission remains owned by String constructors. */
    String materialize() { return new String(toCharArray()); }

    @Override public String toString() { return materialize(); }

    private sealed interface Geometry permits Range, Runs, Bitmap {
        int length();
        Kind kind();
        int sourceIndexAt(int index);
        void copy(CharSequence root, int from, int to, char[] destination, int destinationBegin);
    }

    private record Range(Kind kind, int start, int length) implements Geometry {
        Range {
            if (kind != Kind.EMPTY && kind != Kind.IDENTITY && kind != Kind.RANGE) {
                throw new IllegalArgumentException("invalid range strategy");
            }
            Objects.checkFromIndexSize(0, length, Integer.MAX_VALUE);
        }
        @Override public int sourceIndexAt(int index) {
            return Math.addExact(start, Objects.checkIndex(index, length));
        }
        @Override public void copy(CharSequence root, int from, int to,
                                   char[] destination, int destinationBegin) {
            Objects.checkFromToIndex(from, to, length);
            if (from < to) copyRun(root, start + from, start + to, destination, destinationBegin);
        }
    }

    private static final class Runs implements Geometry {
        private final int[] starts;
        private final int[] ends;
        private final int length;

        Runs(int[] starts, int[] ends, int length) {
            this.starts = starts;
            this.ends = ends;
            this.length = length;
        }
        @Override public Kind kind() { return Kind.RUNS; }
        @Override public int length() { return length; }
        private int runOf(int index) {
            int low = 0, high = ends.length;
            while (low < high) {
                int middle = (low + high) >>> 1;
                if (ends[middle] <= index) low = middle + 1;
                else high = middle;
            }
            return low;
        }
        @Override public int sourceIndexAt(int index) {
            Objects.checkIndex(index, length);
            int run = runOf(index);
            return starts[run] + index - (run == 0 ? 0 : ends[run - 1]);
        }
        @Override public void copy(CharSequence root, int from, int to,
                                   char[] destination, int destinationBegin) {
            Objects.checkFromToIndex(from, to, length);
            if (from == to) return;
            int run = runOf(from);
            int at = from;
            int written = destinationBegin;
            while (at < to) {
                int beginning = run == 0 ? 0 : ends[run - 1];
                int count = Math.min(to, ends[run]) - at;
                int sourceStart = starts[run] + at - beginning;
                copyRun(root, sourceStart, sourceStart + count, destination, written);
                at += count;
                written += count;
                run++;
            }
        }
    }

    private static final class Bitmap implements Geometry {
        private final int base;
        private final long[] bits;
        private final int[] prefix;
        Bitmap(int base, long[] bits, int[] prefix) {
            this.base = base;
            this.bits = bits;
            this.prefix = prefix;
        }
        @Override public Kind kind() { return Kind.BITMAP; }
        @Override public int length() { return prefix[bits.length]; }
        private int wordOf(int index) {
            int low = 0, high = bits.length;
            while (low < high) {
                int middle = (low + high) >>> 1;
                if (prefix[middle + 1] <= index) low = middle + 1;
                else high = middle;
            }
            return low;
        }
        @Override public int sourceIndexAt(int index) {
            Objects.checkIndex(index, length());
            int word = wordOf(index);
            long selected = bits[word];
            int skip = index - prefix[word];
            while (skip-- != 0) selected &= selected - 1L;
            return Math.addExact(base, (word << 6) + Long.numberOfTrailingZeros(selected));
        }
        @Override public void copy(CharSequence root, int from, int to,
                                   char[] destination, int destinationBegin) {
            Objects.checkFromToIndex(from, to, length());
            if (from == to) return;
            int word = wordOf(from);
            long current = bits[word];
            int skip = from - prefix[word];
            while (skip-- != 0) current &= current - 1L;
            int visible = from;
            int runFrom = -1;
            int runEnd = -1;
            int written = destinationBegin;
            while (visible < to) {
                while (current == 0L) current = bits[++word];
                int physical = base + (word << 6) + Long.numberOfTrailingZeros(current);
                current &= current - 1L;
                if (physical == runEnd) {
                    runEnd++;
                } else {
                    if (runFrom != -1) {
                        copyRun(root, runFrom, runEnd, destination, written);
                        written += runEnd - runFrom;
                    }
                    runFrom = physical;
                    runEnd = physical + 1;
                }
                visible++;
            }
            copyRun(root, runFrom, runEnd, destination, written);
        }
    }
}
