/* *****************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections.mat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Spliterator;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

/**
 * Flattened roaring-style signed-int set.
 *
 * <p>Signed order is mapped to unsigned chunk order with value ^ Integer.MIN_VALUE.
 * Frozen state contains no object per chunk. Set algebra is performed container-wise:
 * only one 65,536-value chunk is expanded to temporary machine words at a time.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code
 * m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/RoaringIntSet.java}
 * blob {@code 04f03658aaa65d294e37bcc83c4da57c0ac19e59} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source
 * modulo names).
 */
public final class RoaringIntSet {
    public enum ContainerKind { ARRAY, RUNS, BITMAP }

    private static final byte ARRAY = 0;
    private static final byte RUNS = 1;
    private static final byte BITMAP = 2;
    private static final int BITMAP_WORDS = 1024;
    private static final long INT_MAX_EXCLUSIVE = (long) Integer.MAX_VALUE + 1L;

    private final int size;
    private final int[] chunkKeys;
    private final byte[] kinds;
    private final int[] offsets;
    private final int[] dataLengths;
    private final int[] counts;
    private final int[] prefix;
    private final char[] arrayData;
    private final int[] runData;
    private final long[] bitmapData;

    private RoaringIntSet(
            int size,
            int[] chunkKeys,
            byte[] kinds,
            int[] offsets,
            int[] dataLengths,
            int[] counts,
            int[] prefix,
            char[] arrayData,
            int[] runData,
            long[] bitmapData) {
        this.size = size;
        this.chunkKeys = chunkKeys;
        this.kinds = kinds;
        this.offsets = offsets;
        this.dataLengths = dataLengths;
        this.counts = counts;
        this.prefix = prefix;
        this.arrayData = arrayData;
        this.runData = runData;
        this.bitmapData = bitmapData;
    }

    public static RoaringIntSet of(int... source) {
        Objects.requireNonNull(source, "source");
        if (source.length == 0) return flatten(List.of());
        int[] values = source.clone();
        Arrays.sort(values);
        int unique = 0;
        for (int value : values) {
            if (unique == 0 || values[unique - 1] != value) values[unique++] = value;
        }
        return flatten(chunksOfSorted(values, unique));
    }

    //FLLDCIM: LEETCODIFY — pattern OFFSET_ARRAY
    /** One chunk per run of equal high 16 bits in the sorted, unique values[0, unique). */
    private static List<Chunk> chunksOfSorted(int[] values, int unique) {
        ArrayList<Chunk> chunks = new ArrayList<>();
        int position = 0;
        while (position < unique) {
            int key = (values[position] ^ Integer.MIN_VALUE) >>> 16;
            int start = position;
            while (position < unique && ((values[position] ^ Integer.MIN_VALUE) >>> 16) == key)
                    position++;
            chunks.add(chunkFromSorted(key, values, start, position));
        }
        return chunks;
    }

    //FLLDCIM: LEETCODIFY — pattern CONTAINER_RULE
    /**
     * The chunk of the sorted, unique values[start, end) of one key: chunkFromBitmap's container
     * (same sizes, same tie-breaks) and data; the bitmap is built only when it wins (over 4,096
     * values).
     */
    private static Chunk chunkFromSorted(int key, int[] values, int start, int end) {
        int cardinality = end - start;
        int runs = runCount(values, start, end);
        return switch (kindOf(cardinality, runs)) {
            case ARRAY -> new Chunk(key, ARRAY, cardinality, lowsOf(values, start, end),
                    null, null);
            case RUNS -> new Chunk(key, RUNS, cardinality, null,
                    runsOfSorted(values, start, end, runs), null);
            default -> new Chunk(key, BITMAP, cardinality, null, null,
                    bitmapOf(values, start, end));
        };
    }

    //FLLDCIM: LEETCODIFY — pattern MASK
    private static int low(int value) {
        return value & 0xffff; // value ^ Integer.MIN_VALUE flips bit 31 only
    }

    //FLLDCIM: LEETCODIFY — pattern MASK
    private static int value(int high, int low) {
        return (high | low) ^ Integer.MIN_VALUE;
    }

    //FLLDCIM: LEETCODIFY — pattern MASK
    private static long dropLowest(long bits, int k) {
        long rest = bits;
        for (int i = 0; i < k; i++) rest &= rest - 1;
        return rest;
    }

    //FLLDCIM: LEETCODIFY — pattern RUN_WALK
    private static int runCount(int[] values, int start, int end) {
        int runs = 0;
        int previous = -2;
        for (int i = start; i < end; i++) {
            int low = low(values[i]);
            if (low != previous + 1) runs++;
            previous = low;
        }
        return runs;
    }

    //FLLDCIM: LEETCODIFY — pattern ARRAY_COPY
    private static char[] lowsOf(int[] values, int start, int end) {
        char[] data = new char[end - start];
        for (int i = start; i < end; i++) data[i - start] = (char) low(values[i]);
        return data;
    }

    //FLLDCIM: LEETCODIFY — pattern RUN_WALK
    private static int[] runsOfSorted(int[] values, int start, int end, int runs) {
        int[] data = new int[runs * 2];
        int out = -2;
        int previous = -2;
        for (int i = start; i < end; i++) {
            int low = low(values[i]);
            if (low != previous + 1) data[out += 2] = low;
            data[out + 1]++;
            previous = low;
        }
        return data;
    }

    //FLLDCIM: LEETCODIFY — pattern BITMAP_WALK
    private static long[] bitmapOf(int[] values, int start, int end) {
        long[] words = new long[BITMAP_WORDS];
        for (int i = start; i < end; i++) setBit(words, low(values[i]));
        return words;
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int chunks() { return chunkKeys.length; }

    public ContainerKind chunkKind(int chunkIndex) {
        Objects.checkIndex(chunkIndex, chunks());
        return ContainerKind.values()[kinds[chunkIndex]];
    }

    public int chunkKey(int chunkIndex) {
        return chunkKeys[Objects.checkIndex(chunkIndex, chunks())];
    }

    public int chunkCardinality(int chunkIndex) {
        return counts[Objects.checkIndex(chunkIndex, chunks())];
    }

    public long primitivePayloadBytes() {
        return 4L * (chunkKeys.length + offsets.length + dataLengths.length + counts.length +
                prefix.length)
                + kinds.length + 2L * arrayData.length + 4L * runData.length + 8L *
                        bitmapData.length;
    }

    public boolean contains(int value) {
        int normalized = value ^ Integer.MIN_VALUE;
        int key = normalized >>> 16;
        int chunk = Arrays.binarySearch(chunkKeys, key);
        return chunk >= 0 && containsLow(chunk, normalized & 0xffff);
    }

    /** Number of values strictly less than valueExclusive in signed int order. */
    public int rank(int valueExclusive) {
        int normalized = valueExclusive ^ Integer.MIN_VALUE;
        int key = normalized >>> 16;
        int chunk = Arrays.binarySearch(chunkKeys, key);
        if (chunk >= 0) return prefix[chunk] + countLessLow(chunk, normalized & 0xffff);
        int insertion = -chunk - 1;
        return prefix[insertion];
    }

    /** Supports Integer.MAX_VALUE + 1 as the exclusive upper endpoint. */
    public int rankLong(long valueExclusive) {
        if (valueExclusive <= Integer.MIN_VALUE) return 0;
        if (valueExclusive >= INT_MAX_EXCLUSIVE) return size;
        return rank((int) valueExclusive);
    }

    /** Zero-based element ordinal in signed ascending order. */
    public int select(int ordinal) {
        Objects.checkIndex(ordinal, size);
        int chunk = chunkOf(ordinal);
        int within = ordinal - prefix[chunk];
        int normalized = (chunkKeys[chunk] << 16) | selectLow(chunk, within);
        return normalized ^ Integer.MIN_VALUE;
    }

    public OptionalInt next(int fromInclusive) {
        int normalized = fromInclusive ^ Integer.MIN_VALUE;
        int key = normalized >>> 16;
        int chunk = Arrays.binarySearch(chunkKeys, key);
        if (chunk >= 0) {
            int low = nextLow(chunk, normalized & 0xffff);
            if (low >= 0) return OptionalInt.of(((key << 16) | low) ^ Integer.MIN_VALUE);
            chunk++;
        } else {
            chunk = -chunk - 1;
        }
        if (chunk >= chunks()) return OptionalInt.empty();
        int low = selectLow(chunk, 0);
        return OptionalInt.of(((chunkKeys[chunk] << 16) | low) ^ Integer.MIN_VALUE);
    }

    public OptionalInt previous(int fromInclusive) {
        int normalized = fromInclusive ^ Integer.MIN_VALUE;
        int key = normalized >>> 16;
        int chunk = Arrays.binarySearch(chunkKeys, key);
        if (chunk >= 0) {
            int low = previousLow(chunk, normalized & 0xffff);
            if (low >= 0) return OptionalInt.of(((key << 16) | low) ^ Integer.MIN_VALUE);
            chunk--;
        } else {
            chunk = -chunk - 2;
        }
        if (chunk < 0) return OptionalInt.empty();
        int low = selectLow(chunk, counts[chunk] - 1);
        return OptionalInt.of(((chunkKeys[chunk] << 16) | low) ^ Integer.MIN_VALUE);
    }

    public int[] toArray() {
        return decodeOrdinals(0, size);
    }

    /**
     * Lazy, SIZED and splittable as before; a bulk traversal (forEach, toArray, sum, ...) decodes
     * the containers in order instead of selecting every ordinal.
     */
    public IntStream intStream() {
        return StreamSupport.intStream(new OrdinalSpliterator(0, size), false);
    }

    //FLLDCIM: LEETCODIFY — pattern OFFSET_ARRAY
    /** The values of ordinals [from, to), ascending, decoded container by container. */
    private int[] decodeOrdinals(int from, int to) {
        int[] result = new int[to - from];
        int chunk = from == to ? 0 : chunkOf(from);
        int skip = from == to ? 0 : from - prefix[chunk];
        for (int at = 0; at < result.length; chunk++, skip = 0) {
            int take = Math.min(counts[chunk] - skip, result.length - at);
            at = decodeInto(chunk, skip, at + take, result, at);
        }
        return result;
    }

    //FLLDCIM: LEETCODIFY — pattern OFFSET_ARRAY
    private int chunkOf(int ordinal) {
        int low = 0;
        int high = prefix.length;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (prefix[middle] <= ordinal) low = middle + 1;
            else high = middle;
        }
        return low - 1;
    }

    /** Writes a chunk's values after its first {@code skip} into out[at, end); returns end. */
    private int decodeInto(int chunk, int skip, int end, int[] out, int at) {
        int high = chunkKeys[chunk] << 16;
        return switch (kinds[chunk]) {
            case ARRAY -> decodeArray(high, offsets[chunk] + skip, out, at, end);
            case RUNS -> decodeRuns(high, offsets[chunk], skip, out, at, end);
            case BITMAP -> decodeBitmap(high, offsets[chunk], skip, out, at, end);
            default -> throw new IllegalStateException("container kind");
        };
    }

    //FLLDCIM: LEETCODIFY — pattern ARRAY_COPY
    private int decodeArray(int high, int from, int[] out, int at, int end) {
        int position = at;
        for (int i = from; position < end; i++) out[position++] = value(high, arrayData[i]);
        return position;
    }

    //FLLDCIM: LEETCODIFY — pattern RUN_WALK
    private int decodeRuns(int high, int pair, int skip, int[] out, int at, int end) {
        int position = at;
        int left = skip;
        for (int run = pair; position < end; run += 2) {
            int length = runData[run + 1];
            if (left >= length) {
                left -= length;
                continue;
            }
            int stop = runData[run] + Math.min(length, left + end - position);
            for (int low = runData[run] + left; low < stop; low++) {
                out[position++] = value(high, low);
            }
            left = 0;
        }
        return position;
    }

    //FLLDCIM: LEETCODIFY — pattern BITMAP_WALK
    private int decodeBitmap(int high, int base, int skip, int[] out, int at, int end) {
        int position = at;
        int left = skip;
        for (int word = 0; position < end; word++) {
            long bits = bitmapData[base + word];
            int count = Long.bitCount(bits);
            if (left >= count) {
                left -= count;
                continue;
            }
            for (bits = dropLowest(bits, left); bits != 0 && position < end; bits &= bits - 1) {
                out[position++] = value(high, (word << 6) + Long.numberOfTrailingZeros(bits));
            }
            left = 0;
        }
        return position;
    }

    //FLLDCIM: LEETCODIFY — pattern SPLITERATOR
    /** Ordinals [next, end): select one, decode blocks in bulk, halve on split. */
    private final class OrdinalSpliterator implements Spliterator.OfInt {
        private static final int BLOCK = 4096;
        private int next;
        private final int end;

        OrdinalSpliterator(int next, int end) {
            this.next = next;
            this.end = end;
        }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            Objects.requireNonNull(action, "action");
            if (next >= end) return false;
            action.accept(select(next++));
            return true;
        }

        @Override
        public void forEachRemaining(IntConsumer action) {
            Objects.requireNonNull(action, "action");
            for (int to; next < end; next = to) {
                to = Math.min(end, next + BLOCK);
                for (int value : decodeOrdinals(next, to)) action.accept(value);
            }
        }

        @Override
        public Spliterator.OfInt trySplit() {
            int middle = (next + end) >>> 1;
            if (middle <= next) return null;
            OrdinalSpliterator first = new OrdinalSpliterator(next, middle);
            next = middle;
            return first;
        }

        @Override
        public long estimateSize() {
            return end - next;
        }

        @Override
        public int characteristics() {
            return ORDERED | SIZED | SUBSIZED | IMMUTABLE | NONNULL;
        }
    }

    public RoaringIntSet range(long fromInclusive, long toExclusive) {
        if (fromInclusive < Integer.MIN_VALUE || toExclusive > INT_MAX_EXCLUSIVE || fromInclusive >
                toExclusive) {
            throw new IllegalArgumentException("range outside int domain");
        }
        if (fromInclusive == toExclusive || size == 0) return of();
        int from = rankLong(fromInclusive);
        return of(decodeOrdinals(from, Math.max(from, rankLong(toExclusive))));
    }

    public RoaringIntSet union(RoaringIntSet other) { return combine(other, 0); }
    public RoaringIntSet intersect(RoaringIntSet other) { return combine(other, 1); }
    public RoaringIntSet xor(RoaringIntSet other) { return combine(other, 2); }
    public RoaringIntSet minus(RoaringIntSet other) { return combine(other, 3); }

    private RoaringIntSet combine(RoaringIntSet other, int operation) {
        Objects.requireNonNull(other, "other");
        ArrayList<Chunk> chunks = new ArrayList<>();
        long cursors = cursors(0, 0);
        while ((int) (cursors >>> 32) < this.chunks() || (int) cursors < other.chunks()) {
            cursors = combineStep(other, operation, chunks, (int) (cursors >>> 32), (int) cursors);
        }
        return flatten(chunks);
    }

    //FLLDCIM: LEETCODIFY — pattern MASK
    /** The left chunk cursor in the high 32 bits, the right one in the low 32 bits. */
    private static long cursors(int left, int right) {
        return (long) left << 32 | (right & 0xFFFF_FFFFL);
    }

    /** Emits the chunk of the lower key (of both keys when equal); returns the advanced cursors. */
    private long combineStep(RoaringIntSet other, int operation, List<Chunk> chunks,
            int left, int right) {
        int leftKey = left < this.chunks() ? chunkKeys[left] : Integer.MAX_VALUE;
        int rightKey = right < other.chunks() ? other.chunkKeys[right] : Integer.MAX_VALUE;
        if (left < this.chunks() && right < other.chunks() && leftKey == rightKey) {
            addIfAny(chunks, chunkFromBitmap(leftKey,
                    combineWords(containerBitmap(left), other.containerBitmap(right), operation)));
            return cursors(left + 1, right + 1);
        }
        if (right >= other.chunks() || (left < this.chunks() && leftKey < rightKey)) {
            if (operation != 1) chunks.add(chunkFromBitmap(leftKey, containerBitmap(left)));
            return cursors(left + 1, right);
        }
        if (operation == 0 || operation == 2) {
            chunks.add(chunkFromBitmap(rightKey, other.containerBitmap(right)));
        }
        return cursors(left, right + 1);
    }

    /** Adds a combined chunk unless the operation left it empty. */
    private static void addIfAny(List<Chunk> chunks, Chunk chunk) {
        if (chunk != null) chunks.add(chunk);
    }

    //FLLDCIM: LEETCODIFY — pattern BITMAP_WALK
    /** a := a op b word by word (0 union, 1 intersection, 2 symmetric difference, 3 difference). */
    private static long[] combineWords(long[] a, long[] b, int operation) {
        for (int word = 0; word < BITMAP_WORDS; word++) {
            a[word] = switch (operation) {
                case 0 -> a[word] | b[word];
                case 1 -> a[word] & b[word];
                case 2 -> a[word] ^ b[word];
                case 3 -> a[word] & ~b[word];
                default -> throw new IllegalStateException("operation");
            };
        }
        return a;
    }

    private boolean containsLow(int chunk, int low) {
        return switch (kinds[chunk]) {
            case ARRAY -> Arrays.binarySearch(
                    arrayData, offsets[chunk], offsets[chunk] + dataLengths[chunk], (char) low) >=
                            0;
            case RUNS -> containsRun(chunk, low);
            case BITMAP -> (bitmapData[offsets[chunk] + (low >>> 6)] & (1L << (low & 63))) != 0L;
            default -> throw new IllegalStateException("container kind");
        };
    }

    private int countLessLow(int chunk, int low) {
        return switch (kinds[chunk]) {
            case ARRAY -> {
                int index = Arrays.binarySearch(
                        arrayData, offsets[chunk], offsets[chunk] + dataLengths[chunk], (char) low);
                yield index >= 0 ? index - offsets[chunk] : -index - 1 - offsets[chunk];
            }
            case RUNS -> countLessRuns(chunk, low);
            case BITMAP -> countLessBitmap(chunk, low);
            default -> throw new IllegalStateException("container kind");
        };
    }

    private int selectLow(int chunk, int ordinal) {
        Objects.checkIndex(ordinal, counts[chunk]);
        return switch (kinds[chunk]) {
            case ARRAY -> arrayData[offsets[chunk] + ordinal];
            case RUNS -> selectRun(chunk, ordinal);
            case BITMAP -> selectBitmap(chunk, ordinal);
            default -> throw new IllegalStateException("container kind");
        };
    }

    private int nextLow(int chunk, int low) {
        return switch (kinds[chunk]) {
            case ARRAY -> {
                int index = Arrays.binarySearch(
                        arrayData, offsets[chunk], offsets[chunk] + dataLengths[chunk], (char) low);
                if (index < 0) index = -index - 1;
                yield index < offsets[chunk] + dataLengths[chunk] ? arrayData[index] : -1;
            }
            case RUNS -> nextRun(chunk, low);
            case BITMAP -> nextBitmap(chunk, low);
            default -> throw new IllegalStateException("container kind");
        };
    }

    private int previousLow(int chunk, int low) {
        return switch (kinds[chunk]) {
            case ARRAY -> {
                int index = Arrays.binarySearch(
                        arrayData, offsets[chunk], offsets[chunk] + dataLengths[chunk], (char) low);
                if (index < 0) index = -index - 2;
                yield index >= offsets[chunk] ? arrayData[index] : -1;
            }
            case RUNS -> previousRun(chunk, low);
            case BITMAP -> previousBitmap(chunk, low);
            default -> throw new IllegalStateException("container kind");
        };
    }

    private boolean containsRun(int chunk, int low) {
        int base = offsets[chunk];
        int pairs = dataLengths[chunk] / 2;
        int left = 0;
        int right = pairs - 1;
        while (left <= right) {
            int middle = (left + right) >>> 1;
            int start = runData[base + middle * 2];
            int length = runData[base + middle * 2 + 1];
            if (low < start) right = middle - 1;
            else if (low >= start + length) left = middle + 1;
            else return true;
        }
        return false;
    }

    private int countLessRuns(int chunk, int low) {
        int base = offsets[chunk];
        int pairs = dataLengths[chunk] / 2;
        int count = 0;
        for (int pair = 0; pair < pairs; pair++) {
            int start = runData[base + pair * 2];
            int length = runData[base + pair * 2 + 1];
            if (low <= start) break;
            count += Math.min(length, low - start);
            if (low < start + length) break;
        }
        return count;
    }

    private int selectRun(int chunk, int ordinal) {
        int base = offsets[chunk];
        int pairs = dataLengths[chunk] / 2;
        int remaining = ordinal;
        for (int pair = 0; pair < pairs; pair++) {
            int start = runData[base + pair * 2];
            int length = runData[base + pair * 2 + 1];
            if (remaining < length) return start + remaining;
            remaining -= length;
        }
        throw new IllegalStateException("run select invariant");
    }

    private int nextRun(int chunk, int low) {
        // Find the first run whose exclusive end exceeds the query.
        // Runs are sorted, disjoint and confined to the 16-bit low universe.
        int base = offsets[chunk];
        int pairs = dataLengths[chunk] / 2;
        int left = runEndingAfter(base, pairs, low);
        if (left == pairs) return -1;
        int start = runData[base + left * 2];
        return low < start ? start : low;
    }

    //FLLDCIM: LEETCODIFY — pattern RUN_WALK
    /** The first of {@code pairs} runs from {@code base} ending after {@code low}. */
    private int runEndingAfter(int base, int pairs, int low) {
        int left = 0;
        int right = pairs;
        while (left < right) {
            int middle = (left + right) >>> 1;
            int end = runData[base + middle * 2] + runData[base + middle * 2 + 1];
            if (end <= low) left = middle + 1;
            else right = middle;
        }
        return left;
    }

    private int previousRun(int chunk, int low) {
        // Find the last run whose start does not exceed the query.
        int base = offsets[chunk];
        int pairs = dataLengths[chunk] / 2;
        int left = 0;
        int right = pairs;
        while (left < right) {
            int middle = (left + right) >>> 1;
            if (runData[base + middle * 2] <= low) left = middle + 1;
            else right = middle;
        }
        if (left == 0) return -1;
        int index = base + (left - 1) * 2;
        int end = runData[index] + runData[index + 1] - 1;
        return Math.min(low, end);
    }

    private int countLessBitmap(int chunk, int low) {
        int base = offsets[chunk];
        int word = low >>> 6;
        int count = 0;
        for (int i = 0; i < word; i++) count += Long.bitCount(bitmapData[base + i]);
        int remainder = low & 63;
        if (remainder != 0) count += Long.bitCount(bitmapData[base + word] & ((1L << remainder) - 1L
                ));
        return count;
    }

    private int selectBitmap(int chunk, int ordinal) {
        int base = offsets[chunk];
        int remaining = ordinal;
        for (int word = 0; word < BITMAP_WORDS; word++) {
            long value = bitmapData[base + word];
            int count = Long.bitCount(value);
            if (remaining < count) {
                while (remaining-- > 0) value &= value - 1;
                return (word << 6) + Long.numberOfTrailingZeros(value);
            }
            remaining -= count;
        }
        throw new IllegalStateException("bitmap select invariant");
    }

    private int nextBitmap(int chunk, int low) {
        int base = offsets[chunk];
        int word = low >>> 6;
        long value = bitmapData[base + word] & (-1L << (low & 63));
        while (true) {
            if (value != 0) return (word << 6) + Long.numberOfTrailingZeros(value);
            if (++word >= BITMAP_WORDS) return -1;
            value = bitmapData[base + word];
        }
    }

    private int previousBitmap(int chunk, int low) {
        int base = offsets[chunk];
        int word = low >>> 6;
        int bit = low & 63;
        long mask = bit == 63 ? -1L : (1L << (bit + 1)) - 1L;
        long value = bitmapData[base + word] & mask;
        while (true) {
            if (value != 0) return (word << 6) + (63 - Long.numberOfLeadingZeros(value));
            if (--word < 0) return -1;
            value = bitmapData[base + word];
        }
    }

    private long[] containerBitmap(int chunk) {
        long[] result = new long[BITMAP_WORDS];
        switch (kinds[chunk]) {
            case ARRAY -> setArrayBits(result, offsets[chunk], dataLengths[chunk]);
            case RUNS -> setRunBits(result, offsets[chunk], dataLengths[chunk] / 2);
            case BITMAP -> System.arraycopy(bitmapData, offsets[chunk], result, 0, BITMAP_WORDS);
            default -> throw new IllegalStateException("container kind");
        }
        return result;
    }

    //FLLDCIM: LEETCODIFY — pattern BITMAP_WALK
    private static void setBit(long[] words, int low) {
        words[low >>> 6] |= 1L << (low & 63);
    }

    private void setArrayBits(long[] words, int base, int length) {
        for (int i = 0; i < length; i++) setBit(words, arrayData[base + i]);
    }

    private void setRunBits(long[] words, int base, int pairs) {
        for (int pair = 0; pair < pairs; pair++) {
            int start = runData[base + pair * 2];
            int end = start + runData[base + pair * 2 + 1];
            for (int low = start; low < end; low++) setBit(words, low);
        }
    }

    private static Chunk chunkFromBitmap(int key, long[] words) {
        int cardinality = 0;
        for (long word : words) cardinality += Long.bitCount(word);
        if (cardinality == 0) return null;
        int runs = runCountOfWords(words);
        return switch (kindOf(cardinality, runs)) {
            case ARRAY -> new Chunk(key, ARRAY, cardinality, lowsOfWords(words, cardinality),
                    null, null);
            case RUNS -> new Chunk(key, RUNS, cardinality, null, runsOfWords(words, runs), null);
            default -> new Chunk(key, BITMAP, cardinality, null, null, words); // fresh array
        };
    }

    //FLLDCIM: LEETCODIFY — pattern CONTAINER_RULE
    /** The smallest of 2 bytes a value, 8 a run, 8 KiB; ties go to ARRAY, then to RUNS. */
    private static byte kindOf(int cardinality, int runs) {
        long arrayBytes = 2L * cardinality;
        long runBytes = 8L * runs;
        long bitmapBytes = 8L * BITMAP_WORDS;
        if (arrayBytes <= runBytes && arrayBytes <= bitmapBytes) return ARRAY;
        return runBytes <= bitmapBytes ? RUNS : BITMAP;
    }

    //FLLDCIM: LEETCODIFY — pattern BITMAP_WALK
    private static int runCountOfWords(long[] words) {
        int runs = 0;
        int previous = -2;
        for (int word = 0; word < BITMAP_WORDS; word++) {
            for (long bits = words[word]; bits != 0; bits &= bits - 1) {
                int low = (word << 6) + Long.numberOfTrailingZeros(bits);
                if (low != previous + 1) runs++;
                previous = low;
            }
        }
        return runs;
    }

    private static char[] lowsOfWords(long[] words, int cardinality) {
        char[] data = new char[cardinality];
        int out = 0;
        for (int word = 0; word < BITMAP_WORDS; word++) {
            for (long bits = words[word]; bits != 0; bits &= bits - 1) {
                data[out++] = (char) ((word << 6) + Long.numberOfTrailingZeros(bits));
            }
        }
        return data;
    }

    private static int[] runsOfWords(long[] words, int runs) {
        int[] data = new int[runs * 2];
        int out = -2;
        int previous = -2;
        for (int word = 0; word < BITMAP_WORDS; word++) {
            for (long bits = words[word]; bits != 0; bits &= bits - 1) {
                int low = (word << 6) + Long.numberOfTrailingZeros(bits);
                if (low != previous + 1) data[out += 2] = low;
                data[out + 1]++;
                previous = low;
            }
        }
        return data;
    }

    private static RoaringIntSet flatten(List<Chunk> chunks) {
        int[] sizes = laneSizes(chunks);
        FlatLanes lanes = new FlatLanes(chunks.size(), sizes);
        for (int i = 0; i < chunks.size(); i++) lanes.place(i, chunks.get(i));
        return new RoaringIntSet(sizes[0], lanes.keys, lanes.kinds, lanes.offsets, lanes.lengths,
                lanes.counts, lanes.prefix, lanes.arrays, lanes.runs, lanes.bitmaps);
    }

    //FLLDCIM: LEETCODIFY — pattern OFFSET_ARRAY
    /** The value count, then the lengths of the ARRAY, RUNS and BITMAP lanes (index kind + 1). */
    private static int[] laneSizes(List<Chunk> chunks) {
        int[] sizes = new int[4];
        for (Chunk chunk : chunks) {
            if (chunk == null) continue;
            sizes[0] = Math.addExact(sizes[0], chunk.count);
            int length = chunk.kind == ARRAY ? chunk.array.length
                    : chunk.kind == RUNS ? chunk.runs.length : BITMAP_WORDS;
            int lane = chunk.kind == ARRAY || chunk.kind == RUNS ? chunk.kind + 1 : 3;
            sizes[lane] = Math.addExact(sizes[lane], length);
        }
        return sizes;
    }

    /** The flat lanes filled chunk by chunk, with each chunk's offset into its kind's lane. */
    private static final class FlatLanes {
        final int[] keys;
        final byte[] kinds;
        final int[] offsets;
        final int[] lengths;
        final int[] counts;
        final int[] prefix;
        final char[] arrays;
        final int[] runs;
        final long[] bitmaps;
        private int arrayOffset;
        private int runOffset;
        private int bitmapOffset;

        FlatLanes(int count, int[] sizes) {
            keys = new int[count];
            kinds = new byte[count];
            offsets = new int[count];
            lengths = new int[count];
            counts = new int[count];
            prefix = new int[count + 1];
            arrays = new char[sizes[1]];
            runs = new int[sizes[2]];
            bitmaps = new long[sizes[3]];
        }

        void place(int i, Chunk chunk) {
            keys[i] = chunk.key;
            kinds[i] = chunk.kind;
            counts[i] = chunk.count;
            prefix[i + 1] = prefix[i] + chunk.count;
            if (chunk.kind == ARRAY) {
                offsets[i] = arrayOffset;
                arrayOffset = append(chunk.array, arrays, arrayOffset, i, chunk.array.length);
            } else if (chunk.kind == RUNS) {
                offsets[i] = runOffset;
                runOffset = append(chunk.runs, runs, runOffset, i, chunk.runs.length);
            } else {
                offsets[i] = bitmapOffset;
                bitmapOffset = append(chunk.bitmap, bitmaps, bitmapOffset, i, BITMAP_WORDS);
            }
        }

        /** Copies a chunk's data to the lane at {@code offset}; returns the lane's next offset. */
        private int append(Object data, Object lane, int offset, int i, int length) {
            lengths[i] = length;
            System.arraycopy(data, 0, lane, offset, length);
            return offset + length;
        }
    }

    private static final class Chunk {
        final int key;
        final byte kind;
        final int count;
        final char[] array;
        final int[] runs;
        final long[] bitmap;

        Chunk(int key, byte kind, int count, char[] array, int[] runs, long[] bitmap) {
            this.key = key;
            this.kind = kind;
            this.count = count;
            this.array = array;
            this.runs = runs;
            this.bitmap = bitmap;
        }
    }
}
