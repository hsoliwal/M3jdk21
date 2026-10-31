// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable sparse positional matrix over the 16-byte packed lexeme coordinate space.
 *
 * <p>The logical matrix is {@code [position 0..15][symbol 0..255]}. Physically it is flattened
 * CSR: 4097 cell offsets plus one posting for every materialized lexeme symbol. A separate
 * 17-bucket exact-length CSR lane allows length to participate in candidate selection.</p>
 *
 * <p>The matrix references the owning packed index's immutable high/low key lanes and caller
 * value ids; it never copies text or packed coordinates. Heap and memory-mapped packed indexes
 * implement the same internal primitive key-source contract.</p>
 *
 * <p>Queries choose the rarest constrained lane, enumerate only that posting list, then verify the
 * remaining position constraints with the two masked 64-bit comparisons in
 * {@link MIndexLexemePattern128}. No String or character array is materialized.</p>
 */
public final class MIndexLexemeMatrixIndex {
    public static final int POSITION_COUNT = MIndexLexemeKey128.MAX_SYMBOLS;
    public static final int SYMBOL_VALUES = 1 << Byte.SIZE;
    public static final int POSITION_CELLS = POSITION_COUNT * SYMBOL_VALUES;
    public static final int LENGTH_BUCKETS = POSITION_COUNT + 1;

    private final MIndexPackedLexemeKeySource keys;
    private final int[] positionOffsets;
    private final int[] positionPostings;
    private final int[] lengthOffsets;
    private final int[] lengthPostings;

    private MIndexLexemeMatrixIndex(
            MIndexPackedLexemeKeySource keys,
            int[] positionOffsets,
            int[] positionPostings,
            int[] lengthOffsets,
            int[] lengthPostings) {
        this.keys = Objects.requireNonNull(keys, "keys");
        this.positionOffsets = Objects.requireNonNull(positionOffsets, "positionOffsets");
        this.positionPostings = Objects.requireNonNull(positionPostings, "positionPostings");
        this.lengthOffsets = Objects.requireNonNull(lengthOffsets, "lengthOffsets");
        this.lengthPostings = Objects.requireNonNull(lengthPostings, "lengthPostings");
    }

    public static MIndexLexemeMatrixIndex compile(MIndexPackedLexemeIndex packed) {
        return compileSource(Objects.requireNonNull(packed, "packed"));
    }

    public static MIndexLexemeMatrixIndex compile(MappedMIndexPackedLexemeIndex packed) {
        return compileSource(Objects.requireNonNull(packed, "packed"));
    }

    private static MIndexLexemeMatrixIndex compileSource(MIndexPackedLexemeKeySource keys) {
        int entries = keys.packedEntryCount();
        int[] positionCounts = new int[POSITION_CELLS];
        int[] lengthCounts = new int[LENGTH_BUCKETS];
        int totalPositionPostings = 0;

        for (int entry = 0; entry < entries; entry++) {
            MIndexLexemeKey128 key =
                    new MIndexLexemeKey128(
                            keys.highKeyAtEntry(entry),
                            keys.lowKeyAtEntry(entry));
            int length = key.length();
            lengthCounts[length] = Math.addExact(lengthCounts[length], 1);
            totalPositionPostings = Math.addExact(totalPositionPostings, length);
            for (int position = 0; position < length; position++) {
                int cell = cell(position, key.symbolAt(position));
                positionCounts[cell] = Math.addExact(positionCounts[cell], 1);
            }
        }

        int[] positionOffsets = prefixOffsets(positionCounts);
        int[] positionPostings = new int[totalPositionPostings];
        int[] positionCursors = Arrays.copyOf(positionOffsets, POSITION_CELLS);
        int[] lengthOffsets = prefixOffsets(lengthCounts);
        int[] lengthPostings = new int[entries];
        int[] lengthCursors = Arrays.copyOf(lengthOffsets, LENGTH_BUCKETS);

        for (int entry = 0; entry < entries; entry++) {
            MIndexLexemeKey128 key =
                    new MIndexLexemeKey128(
                            keys.highKeyAtEntry(entry),
                            keys.lowKeyAtEntry(entry));
            int length = key.length();
            lengthPostings[lengthCursors[length]++] = entry;
            for (int position = 0; position < length; position++) {
                int cell = cell(position, key.symbolAt(position));
                positionPostings[positionCursors[cell]++] = entry;
            }
        }

        return new MIndexLexemeMatrixIndex(
                keys, positionOffsets, positionPostings, lengthOffsets, lengthPostings);
    }

    public MIndexLexemeCodec codec() {
        return keys.codec();
    }

    public int size() {
        return keys.packedEntryCount();
    }

    public int postingCount() {
        return positionPostings.length;
    }

    public int populatedPositionCells() {
        int populated = 0;
        for (int cell = 0; cell < POSITION_CELLS; cell++) {
            if (positionOffsets[cell] != positionOffsets[cell + 1]) populated++;
        }
        return populated;
    }

    /**
     * Primitive payload owned by the matrix itself.
     *
     * <p>The packed high/low coordinate lanes and value ids are deliberately excluded because
     * they remain owned by the canonical packed index.</p>
     */
    public long primitivePayloadBytes() {
        return Integer.BYTES
                * (long)
                        (positionOffsets.length
                                + positionPostings.length
                                + lengthOffsets.length
                                + lengthPostings.length);
    }

    /**
     * Upper bound on values that require mask verification for this pattern.
     *
     * <p>This is the size of the rarest available posting lane, or the total entry count when the
     * pattern has no positional/length constraints.</p>
     */
    public int candidateCount(MIndexLexemePattern128 pattern) {
        Seed seed = seed(pattern);
        return seed.endExclusive - seed.startInclusive;
    }

    public int count(MIndexLexemePattern128 pattern) {
        requireCodec(pattern);
        Seed seed = seed(pattern);
        int matches = 0;
        if (seed.allEntries) {
            for (int entry = 0; entry < size(); entry++) {
                if (matches(pattern, entry)) matches++;
            }
            return matches;
        }

        int[] postings = seed.lengthLane ? lengthPostings : positionPostings;
        for (int at = seed.startInclusive; at < seed.endExclusive; at++) {
            if (matches(pattern, postings[at])) matches++;
        }
        return matches;
    }

    /** Return the first caller value id matching the pattern, or -1. */
    public int first(MIndexLexemePattern128 pattern) {
        requireCodec(pattern);
        Seed seed = seed(pattern);
        if (seed.allEntries) {
            for (int entry = 0; entry < size(); entry++) {
                if (matches(pattern, entry)) return keys.valueIdAtEntry(entry);
            }
            return -1;
        }

        int[] postings = seed.lengthLane ? lengthPostings : positionPostings;
        for (int at = seed.startInclusive; at < seed.endExclusive; at++) {
            int entry = postings[at];
            if (matches(pattern, entry)) return keys.valueIdAtEntry(entry);
        }
        return -1;
    }

    /**
     * Write caller value ids matching {@code pattern} into a preallocated target.
     *
     * @return number of ids written
     */
    public int find(
            MIndexLexemePattern128 pattern,
            int[] target,
            int targetOffset,
            int maxResults) {
        requireCodec(pattern);
        Objects.requireNonNull(target, "target");
        if (maxResults < 0) throw new IllegalArgumentException("negative maxResults");
        Objects.checkFromIndexSize(targetOffset, maxResults, target.length);
        if (maxResults == 0) return 0;

        Seed seed = seed(pattern);
        int written = 0;
        if (seed.allEntries) {
            for (int entry = 0; entry < size() && written < maxResults; entry++) {
                if (matches(pattern, entry)) {
                    target[targetOffset + written++] = keys.valueIdAtEntry(entry);
                }
            }
            return written;
        }

        int[] postings = seed.lengthLane ? lengthPostings : positionPostings;
        for (int at = seed.startInclusive;
                at < seed.endExclusive && written < maxResults;
                at++) {
            int entry = postings[at];
            if (matches(pattern, entry)) {
                target[targetOffset + written++] = keys.valueIdAtEntry(entry);
            }
        }
        return written;
    }

    /** Convenience allocating form intended for non-hot control paths. */
    public int[] find(MIndexLexemePattern128 pattern) {
        int matches = count(pattern);
        int[] result = new int[matches];
        int written = find(pattern, result, 0, result.length);
        if (written != result.length) {
            throw new IllegalStateException("lexeme matrix count/find mismatch");
        }
        return result;
    }

    MIndexPackedLexemeKeySource keySource() {
        return keys;
    }

    int[] positionOffsetsCopy() {
        return positionOffsets.clone();
    }

    int[] positionPostingsCopy() {
        return positionPostings.clone();
    }

    int[] lengthOffsetsCopy() {
        return lengthOffsets.clone();
    }

    int[] lengthPostingsCopy() {
        return lengthPostings.clone();
    }

    private boolean matches(MIndexLexemePattern128 pattern, int entry) {
        return pattern.matches(
                keys.highKeyAtEntry(entry),
                keys.lowKeyAtEntry(entry));
    }

    private Seed seed(MIndexLexemePattern128 pattern) {
        requireCodec(pattern);
        int bestStart = 0;
        int bestEnd = size();
        boolean bestLength = false;
        boolean constrained = false;

        if (pattern.hasExactLength()) {
            int length = pattern.exactLength();
            bestStart = lengthOffsets[length];
            bestEnd = lengthOffsets[length + 1];
            bestLength = true;
            constrained = true;
            if (bestStart == bestEnd) return new Seed(false, true, bestStart, bestEnd);
        }

        for (int position = 0; position < POSITION_COUNT; position++) {
            int symbol = pattern.symbolConstraintAt(position);
            if (symbol == 0) continue;
            int cell = cell(position, symbol);
            int start = positionOffsets[cell];
            int end = positionOffsets[cell + 1];
            if (!constrained || end - start < bestEnd - bestStart) {
                bestStart = start;
                bestEnd = end;
                bestLength = false;
                constrained = true;
            }
            if (start == end) return new Seed(false, false, start, end);
        }

        return constrained
                ? new Seed(false, bestLength, bestStart, bestEnd)
                : new Seed(true, false, 0, size());
    }

    private void requireCodec(MIndexLexemePattern128 pattern) {
        Objects.requireNonNull(pattern, "pattern");
        if (pattern.codecFingerprint64() != codec().fingerprint64()) {
            throw new IllegalArgumentException("lexeme pattern codec mismatch");
        }
    }

    static int cell(int position, int symbol) {
        Objects.checkIndex(position, POSITION_COUNT);
        if (symbol < 1 || symbol >= SYMBOL_VALUES) {
            throw new IllegalArgumentException("invalid lexeme symbol " + symbol);
        }
        return (position << Byte.SIZE) | symbol;
    }

    private static int[] prefixOffsets(int[] counts) {
        int[] offsets = new int[counts.length + 1];
        for (int index = 0; index < counts.length; index++) {
            offsets[index + 1] = Math.addExact(offsets[index], counts[index]);
        }
        return offsets;
    }

    private record Seed(
            boolean allEntries,
            boolean lengthLane,
            int startInclusive,
            int endExclusive) {}
}
