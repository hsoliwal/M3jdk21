/* Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Immutable, long-indexed string composition over existing immutable text chunks.
 *
 * <p>This is an M3 adaptation of the algorithmic ideas in Swift Collections' experimental
 * B-tree-backed BigString/Rope: bounded fan-out, subtree summaries, coordinate-based range
 * views, and summaries that let a query skip complete subtrees. It does not copy Swift code
 * or add a Swift runtime dependency. The tree is encoded in parallel primitive arrays rather
 * than allocating a node object per branch or per character.</p>
 *
 * <p>Coordinates are UTF-16 code units, matching {@link String} and {@link Pattern}. Each
 * input chunk is an immutable {@link String} or an existing immutable {@link M3StringPiece};
 * a chunk is never flattened merely because it is joined. The total text length is a long,
 * so a rope may exceed {@link Integer#MAX_VALUE} without materializing one giant Java String.
 * Regex execution is delegated to the stock JDK engine for int-sized ranges only.</p>
 *
 * <p>Composition is batched: {@link #concat(M3BigString...)} copies chunk references and
 * summary/index lanes in O(number of chunks), not text bytes. Querying a code unit is
 * O(log_32(chunks)); summary queries skip whole indexed subtrees. No per-row, per-character,
 * or per-tree-node Java object is retained by the index.</p>
 */
public final class M3BigString {
    private static final int FANOUT = 32;
    private static final int FIRST_HIGH = 1;
    private static final int FIRST_LOW = 1 << 1;
    private static final int LAST_HIGH = 1 << 2;
    private static final int LAST_LOW = 1 << 3;
    private static final int FIRST_CR = 1 << 4;
    private static final int FIRST_LF = 1 << 5;
    private static final int LAST_CR = 1 << 6;
    private static final int LAST_LF = 1 << 7;
    private static final int FIRST_FLAGS = FIRST_HIGH | FIRST_LOW | FIRST_CR | FIRST_LF;
    private static final int LAST_FLAGS = LAST_HIGH | LAST_LOW | LAST_CR | LAST_LF;
    private static final M3BigString EMPTY = new M3BigString(Storage.empty(), 0, 0);

    private final Storage storage;
    private final long start;
    private final long end;

    /** An associative summary for UTF-16 ranges. The hash follows {@link String#hashCode()}. */
    public record Summary(
            long utf16Length,
            long codePointCount,
            long lineTerminatorCount,
            int javaStringHash,
            long characterBloom,
            boolean ascii,
            boolean startsWithHighSurrogate,
            boolean startsWithLowSurrogate,
            boolean endsWithHighSurrogate,
            boolean endsWithLowSurrogate,
            boolean startsWithCarriageReturn,
            boolean startsWithLineFeed,
            boolean endsWithCarriageReturn,
            boolean endsWithLineFeed) {
        public Summary {
            if (utf16Length < 0 || codePointCount < 0 || lineTerminatorCount < 0) {
                throw new IllegalArgumentException("negative text summary");
            }
        }
    }

    private M3BigString(Storage storage, long start, long end) {
        this.storage = Objects.requireNonNull(storage, "storage");
        if (start < 0 || end < start || end > storage.utf16Length) {
            throw new IndexOutOfBoundsException("invalid M3BigString range");
        }
        this.start = start;
        this.end = end;
    }

    /** Build from immutable chunks. Empty chunks are ignored and input order is preserved. */
    public static M3BigString of(CharSequence... input) {
        Objects.requireNonNull(input, "input");
        if (input.length == 0) return EMPTY;
        CharSequence[] chunks = new CharSequence[input.length];
        int count = 0;
        for (CharSequence value : input.clone()) {
            Objects.requireNonNull(value, "chunk");
            if (!(value instanceof String) && !(value instanceof M3StringPiece)) {
                throw new IllegalArgumentException("chunks must be immutable String or M3StringPiece values");
            }
            if (value.length() != 0) chunks[count++] = value;
        }
        if (count == 0) return EMPTY;
        chunks = Arrays.copyOf(chunks, count);
        return fromFreshChunks(chunks);
    }

    /**
     * Join ropes in encounter order, reusing all complete chunks and their summaries.
     * Partial input views create at most two lightweight boundary slices per input rope.
     */
    public static M3BigString concat(M3BigString... input) {
        Objects.requireNonNull(input, "input");
        M3BigString[] values = input.clone();
        long pieceCount = 0;
        for (M3BigString value : values) {
            Objects.requireNonNull(value, "rope");
            pieceCount = Math.addExact(pieceCount, value.storage.overlappingChunkCount(value.start, value.end));
        }
        if (pieceCount == 0) return EMPTY;
        if (pieceCount > Integer.MAX_VALUE - 8L) {
            throw new IllegalArgumentException("chunk directory exceeds Java array limits");
        }
        int count = (int) pieceCount;
        CharSequence[] chunks = new CharSequence[count];
        int[] lengths = new int[count];
        long[] codePoints = new long[count];
        long[] lineTerminators = new long[count];
        int[] hashes = new int[count];
        long[] blooms = new long[count];
        byte[] flags = new byte[count];
        byte[] ascii = new byte[count];
        int next = 0;
        for (M3BigString value : values) {
            Storage source = value.storage;
            int first = source.firstChunk(value.start);
            for (int i = first; i < source.chunks.length && source.chunkStarts[i] < value.end; i++) {
                long chunkStart = source.chunkStarts[i];
                int from = (int) Math.max(0L, value.start - chunkStart);
                int to = (int) Math.min((long) source.chunkLengths[i], value.end - chunkStart);
                if (from >= to) continue;
                if (from == 0 && to == source.chunkLengths[i]) {
                    chunks[next] = source.chunks[i];
                    lengths[next] = source.chunkLengths[i];
                    codePoints[next] = source.chunkCodePoints[i];
                    lineTerminators[next] = source.chunkLineTerminators[i];
                    hashes[next] = source.chunkHashes[i];
                    blooms[next] = source.chunkBlooms[i];
                    flags[next] = source.chunkFlags[i];
                    ascii[next] = source.chunkAscii[i];
                } else {
                    CharSequence slice = new ChunkSlice(source.chunks[i], from, to - from);
                    Summary summary = summarize(slice, 0, slice.length());
                    chunks[next] = slice;
                    lengths[next] = slice.length();
                    codePoints[next] = summary.codePointCount();
                    lineTerminators[next] = summary.lineTerminatorCount();
                    hashes[next] = summary.javaStringHash();
                    blooms[next] = summary.characterBloom();
                    flags[next] = (byte) flags(summary);
                    ascii[next] = (byte) (summary.ascii() ? 1 : 0);
                }
                next++;
            }
        }
        if (next != count) throw new IllegalStateException("rope chunk census changed during concat");
        Storage result = new Storage(chunks, lengths, codePoints,
                lineTerminators, hashes, blooms, flags, ascii);
        return new M3BigString(result, 0, result.utf16Length);
    }

    private static M3BigString fromFreshChunks(CharSequence[] chunks) {
        int n = chunks.length;
        int[] lengths = new int[n];
        long[] codePoints = new long[n];
        long[] lineTerminators = new long[n];
        int[] hashes = new int[n];
        long[] blooms = new long[n];
        byte[] flags = new byte[n];
        byte[] ascii = new byte[n];
        for (int i = 0; i < n; i++) {
            CharSequence chunk = chunks[i];
            lengths[i] = chunk.length();
            Summary summary = summarize(chunk, 0, chunk.length());
            codePoints[i] = summary.codePointCount();
            lineTerminators[i] = summary.lineTerminatorCount();
            hashes[i] = summary.javaStringHash();
            blooms[i] = summary.characterBloom();
            flags[i] = (byte) flags(summary);
            ascii[i] = (byte) (summary.ascii() ? 1 : 0);
        }
        Storage result = new Storage(chunks, lengths, codePoints,
                lineTerminators, hashes, blooms, flags, ascii);
        return new M3BigString(result, 0, result.utf16Length);
    }

    /** Number of UTF-16 code units; unlike CharSequence.length(), this can exceed int range. */
    public long length() { return end - start; }

    public boolean isEmpty() { return start == end; }

    /** UTF-16 code-unit access with a long index. */
    public char charAt(long index) {
        Objects.checkIndex(index, length());
        return storage.charAt(Math.addExact(start, index));
    }

    /** Constant-time coordinate view; no text or index arrays are copied. */
    public M3BigString subSequence(long from, long to) {
        Objects.checkFromToIndex(from, to, length());
        if (from == 0 && to == length()) return this;
        if (from == to) return EMPTY;
        return new M3BigString(storage, start + from, start + to);
    }

    /** Summary of this range. Complete B-tree subtrees contribute summaries without rescanning text. */
    public Summary summary() {
        if (start == end) return emptySummary();
        if (start == 0 && end == storage.utf16Length) return storage.rootSummary();
        SummaryAccumulator result = new SummaryAccumulator();
        storage.appendRangeSummary(storage.root, 0L, start, end, result);
        return result.toSummary();
    }

    /**
     * A conservative character-presence filter for plain literals and regex-required literals.
     * False means at least one code unit is certainly absent; true is only a candidate result.
     * It does not establish that the literal occurs or that a regex matches.
     */
    public boolean mayContainLiteral(CharSequence literal) {
        Objects.requireNonNull(literal, "literal");
        if (literal.length() == 0) return true;
        long bloom = summary().characterBloom();
        for (int i = 0; i < literal.length(); i++) {
            if ((bloom & bloomBit(literal.charAt(i))) == 0L) return false;
        }
        return true;
    }

    /** Exact UTF-16 literal search across chunk and tree boundaries; returns a range-relative index. */
    public long indexOf(CharSequence needle, long fromIndex) {
        Objects.requireNonNull(needle, "needle");
        long size = length();
        long from = Math.max(0L, fromIndex);
        int m = needle.length();
        if (m == 0) return Math.min(from, size);
        if (from > size) return -1L;
        if ((long) m > size - from || !mayContainLiteral(needle)) return -1L;
        int[] failure = new int[m];
        for (int i = 1, j = 0; i < m; i++) {
            while (j > 0 && needle.charAt(i) != needle.charAt(j)) j = failure[j - 1];
            if (needle.charAt(i) == needle.charAt(j)) j++;
            failure[i] = j;
        }
        long absoluteFrom = start + from;
        int first = storage.firstChunk(absoluteFrom);
        int matched = 0;
        for (int chunk = first; chunk < storage.chunks.length; chunk++) {
            long chunkStart = storage.chunkStarts[chunk];
            if (chunkStart >= end) break;
            int localFrom = (int) Math.max(0L, absoluteFrom - chunkStart);
            int localTo = (int) Math.min((long) storage.chunkLengths[chunk], end - chunkStart);
            CharSequence text = storage.chunks[chunk];
            for (int at = localFrom; at < localTo; at++) {
                char c = text.charAt(at);
                while (matched > 0 && needle.charAt(matched) != c) matched = failure[matched - 1];
                if (needle.charAt(matched) == c) matched++;
                if (matched == m) return chunkStart + at - m + 1L - start;
            }
        }
        return -1L;
    }

    /** Stream UTF-16 chunks to an Appendable without building a contiguous String. */
    public void appendTo(Appendable destination) throws IOException {
        Objects.requireNonNull(destination, "destination");
        if (isEmpty()) return;
        int first = storage.firstChunk(start);
        for (int i = first; i < storage.chunks.length; i++) {
            long chunkStart = storage.chunkStarts[i];
            if (chunkStart >= end) break;
            int from = (int) Math.max(0L, start - chunkStart);
            int to = (int) Math.min((long) storage.chunkLengths[i], end - chunkStart);
            if (from < to) destination.append(storage.chunks[i], from, to);
        }
    }

    /** Explicit contiguous-materialization boundary. A long-indexed rope cannot be flattened. */
    public String flatten() {
        long size = length();
        if (size > Integer.MAX_VALUE) {
            throw new IllegalStateException("M3BigString exceeds the contiguous Java String limit");
        }
        StringBuilder result = new StringBuilder((int) size);
        try { appendTo(result); }
        catch (IOException impossible) { throw new AssertionError(impossible); }
        return result.toString();
    }

    /**
     * Give the stock JDK regex engine an immutable, non-flattening CharSequence for int-sized text.
     * Java's Pattern/Matcher remain the semantic authority; ropes above int range are refused.
     */
    public Matcher matcher(Pattern pattern) {
        Objects.requireNonNull(pattern, "pattern");
        return pattern.matcher(asCharSequence());
    }

    /** Return a CharSequence facade only if the range fits the JDK int-indexed contract. */
    public CharSequence asCharSequence() {
        if (length() > Integer.MAX_VALUE) {
            throw new IllegalStateException("JDK CharSequence/regex indices are int-sized");
        }
        return new IntView(this);
    }

    private static Summary emptySummary() {
        return new Summary(0, 0, 0, 0, 0, true,
                false, false, false, false, false, false, false, false);
    }

    private static int flags(Summary s) {
        return (s.startsWithHighSurrogate() ? FIRST_HIGH : 0)
                | (s.startsWithLowSurrogate() ? FIRST_LOW : 0)
                | (s.endsWithHighSurrogate() ? LAST_HIGH : 0)
                | (s.endsWithLowSurrogate() ? LAST_LOW : 0)
                | (s.startsWithCarriageReturn() ? FIRST_CR : 0)
                | (s.startsWithLineFeed() ? FIRST_LF : 0)
                | (s.endsWithCarriageReturn() ? LAST_CR : 0)
                | (s.endsWithLineFeed() ? LAST_LF : 0);
    }

    private static long bloomBit(char value) {
        int mixed = (value * 0x9e3779b9) ^ (value >>> 3) ^ (value << 7);
        return 1L << (mixed & 63);
    }

    private static int pow31(long exponent) {
        int result = 1;
        int base = 31;
        long remaining = exponent;
        while (remaining != 0) {
            if ((remaining & 1L) != 0) result *= base;
            base *= base;
            remaining >>>= 1;
        }
        return result;
    }

    private static Summary summarize(CharSequence text, int from, int to) {
        Objects.checkFromToIndex(from, to, text.length());
        long cps = 0;
        long lines = 0;
        long bloom = 0L;
        int hash = 0;
        boolean asciiOnly = true;
        int flags = 0;
        for (int i = from; i < to; i++) {
            char c = text.charAt(i);
            hash = 31 * hash + c;
            bloom |= bloomBit(c);
            asciiOnly &= c <= 0x7f;
            if (Character.isHighSurrogate(c)) cps++;
            else if (!Character.isLowSurrogate(c)
                    || i == from || !Character.isHighSurrogate(text.charAt(i - 1))) cps++;
            if (c == '\r' || (c == '\n' && (i == from || text.charAt(i - 1) != '\r'))) lines++;
            if (i == from) {
                if (Character.isHighSurrogate(c)) flags |= FIRST_HIGH;
                if (Character.isLowSurrogate(c)) flags |= FIRST_LOW;
                if (c == '\r') flags |= FIRST_CR;
                if (c == '\n') flags |= FIRST_LF;
            }
            if (i == to - 1) {
                if (Character.isHighSurrogate(c)) flags |= LAST_HIGH;
                if (Character.isLowSurrogate(c)) flags |= LAST_LOW;
                if (c == '\r') flags |= LAST_CR;
                if (c == '\n') flags |= LAST_LF;
            }
        }
        return new Summary(to - (long) from, cps, lines, hash, bloom, asciiOnly,
                (flags & FIRST_HIGH) != 0, (flags & FIRST_LOW) != 0,
                (flags & LAST_HIGH) != 0, (flags & LAST_LOW) != 0,
                (flags & FIRST_CR) != 0, (flags & FIRST_LF) != 0,
                (flags & LAST_CR) != 0, (flags & LAST_LF) != 0);
    }

    private static final class SummaryAccumulator {
        long units;
        long codePoints;
        long lines;
        int hash;
        long bloom;
        byte ascii = 1;
        byte flags;

        void add(long addUnits, long addCodePoints, long addLines, int addHash,
                long addBloom, byte addFlags, byte addAscii) {
            if (addUnits == 0) return;
            if (units == 0) {
                codePoints = addCodePoints;
                lines = addLines;
                hash = addHash;
                bloom = addBloom;
                flags = addFlags;
                ascii = addAscii;
                units = addUnits;
                return;
            }
            long cp = Math.addExact(codePoints, addCodePoints);
            if ((flags & LAST_HIGH) != 0 && (addFlags & FIRST_LOW) != 0) cp--;
            long lineCount = Math.addExact(lines, addLines);
            if ((flags & LAST_CR) != 0 && (addFlags & FIRST_LF) != 0) lineCount--;
            hash = hash * pow31(addUnits) + addHash;
            bloom |= addBloom;
            ascii &= addAscii;
            flags = (byte) ((flags & FIRST_FLAGS) | (addFlags & LAST_FLAGS));
            units = Math.addExact(units, addUnits);
            codePoints = cp;
            lines = lineCount;
        }

        Summary toSummary() {
            if (units == 0) return emptySummary();
            return new Summary(units, codePoints, lines, hash, bloom, ascii != 0,
                    (flags & FIRST_HIGH) != 0, (flags & FIRST_LOW) != 0,
                    (flags & LAST_HIGH) != 0, (flags & LAST_LOW) != 0,
                    (flags & FIRST_CR) != 0, (flags & FIRST_LF) != 0,
                    (flags & LAST_CR) != 0, (flags & LAST_LF) != 0);
        }
    }

    private static final class Storage {
        final CharSequence[] chunks;
        final int[] chunkLengths;
        final long[] chunkStarts;
        final long[] chunkCodePoints;
        final long[] chunkLineTerminators;
        final int[] chunkHashes;
        final long[] chunkBlooms;
        final byte[] chunkFlags;
        final byte[] chunkAscii;
        final int[] childStarts;
        final byte[] childCounts;
        final byte[] nodeHeights;
        final int[] children;
        final long[] childPrefixEnds;
        final long[] nodeUnits;
        final long[] nodeCodePoints;
        final long[] nodeLineTerminators;
        final int[] nodeHashes;
        final long[] nodeBlooms;
        final byte[] nodeFlags;
        final byte[] nodeAscii;
        final long utf16Length;
        final int root;

        static Storage empty() {
            return new Storage(new CharSequence[0], new int[0], new long[0], new long[0],
                    new int[0], new long[0], new byte[0], new byte[0]);
        }

        Storage(CharSequence[] chunks, int[] lengths, long[] codePoints, long[] lines,
                int[] hashes, long[] blooms, byte[] flags, byte[] ascii) {
            this.chunks = chunks;
            this.chunkLengths = lengths;
            this.chunkCodePoints = codePoints;
            this.chunkLineTerminators = lines;
            this.chunkHashes = hashes;
            this.chunkBlooms = blooms;
            this.chunkFlags = flags;
            this.chunkAscii = ascii;
            int n = chunks.length;
            if (lengths.length != n || codePoints.length != n || lines.length != n
                    || hashes.length != n || blooms.length != n || flags.length != n || ascii.length != n) {
                throw new IllegalArgumentException("unaligned chunk summary arrays");
            }
            this.chunkStarts = new long[n];
            long total = 0;
            for (int i = 0; i < n; i++) {
                if (lengths[i] <= 0) throw new IllegalArgumentException("empty chunk escaped compaction");
                chunkStarts[i] = total;
                total = Math.addExact(total, lengths[i]);
            }
            this.utf16Length = total;
            if (n == 0) {
                childStarts = new int[0]; childCounts = new byte[0]; nodeHeights = new byte[0];
                children = new int[0]; childPrefixEnds = new long[0]; nodeUnits = new long[0];
                nodeCodePoints = new long[0]; nodeLineTerminators = new long[0]; nodeHashes = new int[0];
                nodeBlooms = new long[0]; nodeFlags = new byte[0]; nodeAscii = new byte[0]; root = -1;
                return;
            }
            int[] counts = new int[16];
            int levels = 0;
            int count = ceilDiv(n, FANOUT);
            do { counts[levels++] = count; count = count == 1 ? 1 : ceilDiv(count, FANOUT); }
            while (counts[levels - 1] != 1);
            int nodes = 0;
            long childSlots = n;
            for (int i = 0; i < levels; i++) {
                nodes = Math.addExact(nodes, counts[i]);
                if (i < levels - 1) childSlots = Math.addExact(childSlots, counts[i]);
            }
            if (childSlots > Integer.MAX_VALUE - 8L || nodes > Integer.MAX_VALUE - 8) {
                throw new IllegalArgumentException("B-tree index exceeds Java array limits");
            }
            childStarts = new int[nodes];
            childCounts = new byte[nodes];
            nodeHeights = new byte[nodes];
            children = new int[(int) childSlots];
            childPrefixEnds = new long[(int) childSlots];
            nodeUnits = new long[nodes];
            nodeCodePoints = new long[nodes];
            nodeLineTerminators = new long[nodes];
            nodeHashes = new int[nodes];
            nodeBlooms = new long[nodes];
            nodeFlags = new byte[nodes];
            nodeAscii = new byte[nodes];
            Arrays.fill(nodeAscii, (byte) 1);
            int[] levelStarts = new int[levels];
            int cursorNode = 0;
            for (int i = 0; i < levels; i++) { levelStarts[i] = cursorNode; cursorNode += counts[i]; }
            int childCursor = 0;
            int chunk = 0;
            for (int j = 0; j < counts[0]; j++) {
                int node = levelStarts[0] + j;
                childStarts[node] = childCursor;
                int endChunk = Math.min(n, chunk + FANOUT);
                childCounts[node] = (byte) (endChunk - chunk);
                nodeHeights[node] = 0;
                for (; chunk < endChunk; chunk++) {
                    children[childCursor] = chunk;
                    appendNodeSummary(node, chunkLengths[chunk], chunkCodePoints[chunk],
                            chunkLineTerminators[chunk], chunkHashes[chunk], chunkBlooms[chunk],
                            chunkFlags[chunk], chunkAscii[chunk]);
                    childPrefixEnds[childCursor++] = nodeUnits[node];
                }
            }
            for (int level = 1; level < levels; level++) {
                int previousStart = levelStarts[level - 1];
                int previousCount = counts[level - 1];
                int childNode = previousStart;
                for (int j = 0; j < counts[level]; j++) {
                    int node = levelStarts[level] + j;
                    childStarts[node] = childCursor;
                    int endNode = Math.min(previousStart + previousCount, childNode + FANOUT);
                    childCounts[node] = (byte) (endNode - childNode);
                    nodeHeights[node] = (byte) level;
                    for (; childNode < endNode; childNode++) {
                        children[childCursor] = childNode;
                        appendNodeSummary(node, nodeUnits[childNode], nodeCodePoints[childNode],
                                nodeLineTerminators[childNode], nodeHashes[childNode], nodeBlooms[childNode],
                                nodeFlags[childNode], nodeAscii[childNode]);
                        childPrefixEnds[childCursor++] = nodeUnits[node];
                    }
                }
            }
            if (childCursor != children.length) throw new IllegalStateException("B-tree child census drift");
            this.root = nodes - 1;
        }

        private void appendNodeSummary(int node, long units, long codePoints, long lines,
                int hash, long bloom, byte flags, byte ascii) {
            long oldUnits = nodeUnits[node];
            if (oldUnits == 0) {
                nodeCodePoints[node] = codePoints;
                nodeLineTerminators[node] = lines;
                nodeHashes[node] = hash;
                nodeBlooms[node] = bloom;
                nodeFlags[node] = flags;
                nodeAscii[node] = ascii;
            } else {
                long cp = Math.addExact(nodeCodePoints[node], codePoints);
                if ((nodeFlags[node] & LAST_HIGH) != 0 && (flags & FIRST_LOW) != 0) cp--;
                long lineCount = Math.addExact(nodeLineTerminators[node], lines);
                if ((nodeFlags[node] & LAST_CR) != 0 && (flags & FIRST_LF) != 0) lineCount--;
                nodeCodePoints[node] = cp;
                nodeLineTerminators[node] = lineCount;
                nodeHashes[node] = nodeHashes[node] * pow31(units) + hash;
                nodeBlooms[node] |= bloom;
                nodeAscii[node] &= ascii;
                nodeFlags[node] = (byte) ((nodeFlags[node] & FIRST_FLAGS) | (flags & LAST_FLAGS));
            }
            nodeUnits[node] = Math.addExact(oldUnits, units);
        }

        long overlappingChunkCount(long from, long to) {
            if (from == to) return 0;
            int first = firstChunk(from);
            int endIndex = first;
            while (endIndex < chunks.length && chunkStarts[endIndex] < to) endIndex++;
            return endIndex - first;
        }

        int firstChunk(long absoluteOffset) {
            int low = 0, high = chunks.length;
            while (low < high) {
                int middle = (low + high) >>> 1;
                if (chunkStarts[middle] + chunkLengths[middle] <= absoluteOffset) low = middle + 1;
                else high = middle;
            }
            return low;
        }

        char charAt(long index) {
            int node = root;
            long offset = index;
            while (nodeHeights[node] != 0) {
                int slot = findChild(node, offset);
                int base = childStarts[node];
                long prior = slot == 0 ? 0 : childPrefixEnds[base + slot - 1];
                offset -= prior;
                node = children[base + slot];
            }
            int slot = findChild(node, offset);
            int base = childStarts[node];
            long prior = slot == 0 ? 0 : childPrefixEnds[base + slot - 1];
            int chunk = children[base + slot];
            return chunks[chunk].charAt(Math.toIntExact(offset - prior));
        }

        private int findChild(int node, long offset) {
            int base = childStarts[node];
            int count = childCounts[node] & 0xff;
            int low = 0, high = count;
            while (low < high) {
                int mid = (low + high) >>> 1;
                if (childPrefixEnds[base + mid] <= offset) low = mid + 1;
                else high = mid;
            }
            if (low == count) throw new IndexOutOfBoundsException("B-tree summary does not cover coordinate");
            return low;
        }

        Summary rootSummary() {
            if (root < 0) return emptySummary();
            return makeSummary(nodeUnits[root], nodeCodePoints[root], nodeLineTerminators[root],
                    nodeHashes[root], nodeBlooms[root], nodeFlags[root], nodeAscii[root]);
        }

        void appendRangeSummary(int node, long nodeBase, long from, long to, SummaryAccumulator out) {
            if (node < 0 || from >= nodeBase + nodeUnits[node] || to <= nodeBase) return;
            long nodeEnd = nodeBase + nodeUnits[node];
            if (from <= nodeBase && nodeEnd <= to) {
                out.add(nodeUnits[node], nodeCodePoints[node], nodeLineTerminators[node],
                        nodeHashes[node], nodeBlooms[node], nodeFlags[node], nodeAscii[node]);
                return;
            }
            int base = childStarts[node];
            int count = childCounts[node] & 0xff;
            long previous = 0;
            if (nodeHeights[node] == 0) {
                for (int j = 0; j < count; j++) {
                    int chunk = children[base + j];
                    long childStart = nodeBase + previous;
                    long childEnd = childStart + chunkLengths[chunk];
                    previous = childPrefixEnds[base + j];
                    long left = Math.max(from, childStart);
                    long right = Math.min(to, childEnd);
                    if (left >= right) continue;
                    if (left == childStart && right == childEnd) {
                        out.add(chunkLengths[chunk], chunkCodePoints[chunk], chunkLineTerminators[chunk],
                                chunkHashes[chunk], chunkBlooms[chunk], chunkFlags[chunk], chunkAscii[chunk]);
                    } else {
                        Summary s = summarize(chunks[chunk], (int) (left - childStart), (int) (right - childStart));
                        out.add(s.utf16Length(), s.codePointCount(), s.lineTerminatorCount(), s.javaStringHash(),
                                s.characterBloom(), (byte) flags(s), (byte) (s.ascii() ? 1 : 0));
                    }
                }
            } else {
                for (int j = 0; j < count; j++) {
                    int child = children[base + j];
                    long childBase = nodeBase + previous;
                    previous = childPrefixEnds[base + j];
                    appendRangeSummary(child, childBase, from, to, out);
                }
            }
        }

        private static Summary makeSummary(long units, long codePoints, long lines,
                int hash, long bloom, byte flags, byte ascii) {
            return new Summary(units, codePoints, lines, hash, bloom, ascii != 0,
                    (flags & FIRST_HIGH) != 0, (flags & FIRST_LOW) != 0,
                    (flags & LAST_HIGH) != 0, (flags & LAST_LOW) != 0,
                    (flags & FIRST_CR) != 0, (flags & FIRST_LF) != 0,
                    (flags & LAST_CR) != 0, (flags & LAST_LF) != 0);
        }

        private static int ceilDiv(int n, int divisor) {
            return (int) (((long) n + divisor - 1L) / divisor);
        }
    }

    private static final class ChunkSlice implements CharSequence {
        private final CharSequence source;
        private final int offset;
        private final int length;
        ChunkSlice(CharSequence source, int offset, int length) {
            Objects.checkFromIndexSize(offset, length, source.length());
            this.source = source instanceof ChunkSlice slice ? slice.source : source;
            this.offset = source instanceof ChunkSlice slice ? slice.offset + offset : offset;
            this.length = length;
        }
        @Override public int length() { return length; }
        @Override public char charAt(int index) { Objects.checkIndex(index, length); return source.charAt(offset + index); }
        @Override public CharSequence subSequence(int start, int end) {
            Objects.checkFromToIndex(start, end, length);
            return new ChunkSlice(source, offset + start, end - start);
        }
        @Override public String toString() {
            StringBuilder out = new StringBuilder(length);
            for (int i = 0; i < length; i++) out.append(charAt(i));
            return out.toString();
        }
    }

    private static final class IntView implements CharSequence {
        private final M3BigString value;
        IntView(M3BigString value) { this.value = value; }
        @Override public int length() { return (int) value.length(); }
        @Override public char charAt(int index) { Objects.checkIndex(index, length()); return value.charAt(index); }
        @Override public CharSequence subSequence(int start, int end) {
            Objects.checkFromToIndex(start, end, length());
            return value.subSequence(start, end).asCharSequence();
        }
        @Override public String toString() { return value.flatten(); }
    }
}
