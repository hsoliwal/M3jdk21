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
import java.util.stream.IntStream;

/**
 * Flattened roaring-style signed-int set.
 *
 * <p>Signed order is mapped to unsigned chunk order with value ^ Integer.MIN_VALUE.
 * Frozen state contains no object per chunk. Set algebra is performed container-wise:
 * only one 65,536-value chunk is expanded to temporary machine words at a time.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatRoaringIntSet.java}
 * blob {@code 04f03658aaa65d294e37bcc83c4da57c0ac19e59} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatRoaringIntSet {
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

    private MatRoaringIntSet(
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

    public static MatRoaringIntSet of(int... source) {
        Objects.requireNonNull(source, "source");
        if (source.length == 0) return flatten(List.of());
        int[] values = source.clone();
        Arrays.sort(values);
        int unique = 0;
        for (int value : values) {
            if (unique == 0 || values[unique - 1] != value) values[unique++] = value;
        }
        ArrayList<Chunk> chunks = new ArrayList<>();
        int position = 0;
        while (position < unique) {
            int normalized = values[position] ^ Integer.MIN_VALUE;
            int key = normalized >>> 16;
            int start = position;
            while (position < unique && ((values[position] ^ Integer.MIN_VALUE) >>> 16) == key) position++;
            long[] words = new long[BITMAP_WORDS];
            for (int i = start; i < position; i++) {
                int low = (values[i] ^ Integer.MIN_VALUE) & 0xffff;
                words[low >>> 6] |= 1L << (low & 63);
            }
            chunks.add(chunkFromBitmap(key, words));
        }
        return flatten(chunks);
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
        return 4L * (chunkKeys.length + offsets.length + dataLengths.length + counts.length + prefix.length)
                + kinds.length + 2L * arrayData.length + 4L * runData.length + 8L * bitmapData.length;
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
        int low = 0;
        int high = prefix.length;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (prefix[middle] <= ordinal) low = middle + 1;
            else high = middle;
        }
        int chunk = low - 1;
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
        int[] result = new int[size];
        for (int i = 0; i < size; i++) result[i] = select(i);
        return result;
    }

    public IntStream intStream() {
        return IntStream.range(0, size).map(this::select);
    }

    public MatRoaringIntSet range(long fromInclusive, long toExclusive) {
        if (fromInclusive < Integer.MIN_VALUE || toExclusive > INT_MAX_EXCLUSIVE || fromInclusive > toExclusive) {
            throw new IllegalArgumentException("range outside int domain");
        }
        if (fromInclusive == toExclusive || size == 0) return of();
        int expected = Math.max(0, rankLong(toExclusive) - rankLong(fromInclusive));
        int[] result = new int[expected];
        int out = 0;
        int ordinal = rankLong(fromInclusive);
        int end = rankLong(toExclusive);
        while (ordinal < end) result[out++] = select(ordinal++);
        return of(result);
    }

    public MatRoaringIntSet union(MatRoaringIntSet other) { return combine(other, 0); }
    public MatRoaringIntSet intersect(MatRoaringIntSet other) { return combine(other, 1); }
    public MatRoaringIntSet xor(MatRoaringIntSet other) { return combine(other, 2); }
    public MatRoaringIntSet minus(MatRoaringIntSet other) { return combine(other, 3); }

    private MatRoaringIntSet combine(MatRoaringIntSet other, int operation) {
        Objects.requireNonNull(other, "other");
        ArrayList<Chunk> chunks = new ArrayList<>();
        int left = 0;
        int right = 0;
        while (left < this.chunks() || right < other.chunks()) {
            int leftKey = left < this.chunks() ? chunkKeys[left] : Integer.MAX_VALUE;
            int rightKey = right < other.chunks() ? other.chunkKeys[right] : Integer.MAX_VALUE;
            if (left < this.chunks() && right < other.chunks() && leftKey == rightKey) {
                long[] a = containerBitmap(left);
                long[] b = other.containerBitmap(right);
                for (int word = 0; word < BITMAP_WORDS; word++) {
                    a[word] = switch (operation) {
                        case 0 -> a[word] | b[word];
                        case 1 -> a[word] & b[word];
                        case 2 -> a[word] ^ b[word];
                        case 3 -> a[word] & ~b[word];
                        default -> throw new IllegalStateException("operation");
                    };
                }
                Chunk result = chunkFromBitmap(leftKey, a);
                if (result != null) chunks.add(result);
                left++;
                right++;
            } else if (right >= other.chunks() || (left < this.chunks() && leftKey < rightKey)) {
                if (operation == 0 || operation == 2 || operation == 3) {
                    chunks.add(chunkFromBitmap(leftKey, containerBitmap(left)));
                }
                left++;
            } else {
                if (operation == 0 || operation == 2) {
                    chunks.add(chunkFromBitmap(rightKey, other.containerBitmap(right)));
                }
                right++;
            }
        }
        return flatten(chunks);
    }

    private boolean containsLow(int chunk, int low) {
        return switch (kinds[chunk]) {
            case ARRAY -> Arrays.binarySearch(
                    arrayData, offsets[chunk], offsets[chunk] + dataLengths[chunk], (char) low) >= 0;
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
        int left = 0;
        int right = pairs;
        while (left < right) {
            int middle = (left + right) >>> 1;
            int start = runData[base + middle * 2];
            int length = runData[base + middle * 2 + 1];
            if (start + length <= low) left = middle + 1;
            else right = middle;
        }
        if (left == pairs) return -1;
        int start = runData[base + left * 2];
        return low < start ? start : low;
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
        if (remainder != 0) count += Long.bitCount(bitmapData[base + word] & ((1L << remainder) - 1L));
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
            case ARRAY -> {
                int base = offsets[chunk];
                for (int i = 0; i < dataLengths[chunk]; i++) {
                    int low = arrayData[base + i];
                    result[low >>> 6] |= 1L << (low & 63);
                }
            }
            case RUNS -> {
                int base = offsets[chunk];
                int pairs = dataLengths[chunk] / 2;
                for (int pair = 0; pair < pairs; pair++) {
                    int start = runData[base + pair * 2];
                    int end = start + runData[base + pair * 2 + 1];
                    for (int low = start; low < end; low++) result[low >>> 6] |= 1L << (low & 63);
                }
            }
            case BITMAP -> System.arraycopy(bitmapData, offsets[chunk], result, 0, BITMAP_WORDS);
            default -> throw new IllegalStateException("container kind");
        }
        return result;
    }

    private static Chunk chunkFromBitmap(int key, long[] words) {
        int cardinality = 0;
        int runs = 0;
        int previous = -2;
        for (int word = 0; word < BITMAP_WORDS; word++) {
            long value = words[word];
            cardinality += Long.bitCount(value);
            while (value != 0) {
                int low = (word << 6) + Long.numberOfTrailingZeros(value);
                if (low != previous + 1) runs++;
                previous = low;
                value &= value - 1;
            }
        }
        if (cardinality == 0) return null;
        long arrayBytes = 2L * cardinality;
        long runBytes = 8L * runs;
        long bitmapBytes = 8L * BITMAP_WORDS;
        if (arrayBytes <= runBytes && arrayBytes <= bitmapBytes) {
            char[] data = new char[cardinality];
            int out = 0;
            for (int word = 0; word < BITMAP_WORDS; word++) {
                long value = words[word];
                while (value != 0) {
                    data[out++] = (char) ((word << 6) + Long.numberOfTrailingZeros(value));
                    value &= value - 1;
                }
            }
            return new Chunk(key, ARRAY, cardinality, data, null, null);
        }
        if (runBytes <= bitmapBytes) {
            int[] data = new int[runs * 2];
            int out = 0;
            int runStart = -1;
            int last = -2;
            for (int word = 0; word < BITMAP_WORDS; word++) {
                long value = words[word];
                while (value != 0) {
                    int low = (word << 6) + Long.numberOfTrailingZeros(value);
                    if (low != last + 1) {
                        if (runStart >= 0) {
                            data[out++] = runStart;
                            data[out++] = last - runStart + 1;
                        }
                        runStart = low;
                    }
                    last = low;
                    value &= value - 1;
                }
            }
            data[out++] = runStart;
            data[out] = last - runStart + 1;
            return new Chunk(key, RUNS, cardinality, null, data, null);
        }
        return new Chunk(key, BITMAP, cardinality, null, null, words.clone());
    }

    private static MatRoaringIntSet flatten(List<Chunk> chunks) {
        int total = 0;
        int arrayChars = 0;
        int runInts = 0;
        int bitmapWords = 0;
        for (Chunk chunk : chunks) {
            if (chunk == null) continue;
            total = Math.addExact(total, chunk.count);
            if (chunk.kind == ARRAY) arrayChars = Math.addExact(arrayChars, chunk.array.length);
            else if (chunk.kind == RUNS) runInts = Math.addExact(runInts, chunk.runs.length);
            else bitmapWords = Math.addExact(bitmapWords, BITMAP_WORDS);
        }
        int count = chunks.size();
        int[] keys = new int[count];
        byte[] kinds = new byte[count];
        int[] offsets = new int[count];
        int[] lengths = new int[count];
        int[] counts = new int[count];
        int[] prefix = new int[count + 1];
        char[] arrays = new char[arrayChars];
        int[] runs = new int[runInts];
        long[] bitmaps = new long[bitmapWords];
        int arrayOffset = 0;
        int runOffset = 0;
        int bitmapOffset = 0;
        for (int i = 0; i < count; i++) {
            Chunk chunk = chunks.get(i);
            keys[i] = chunk.key;
            kinds[i] = chunk.kind;
            counts[i] = chunk.count;
            prefix[i + 1] = prefix[i] + chunk.count;
            if (chunk.kind == ARRAY) {
                offsets[i] = arrayOffset;
                lengths[i] = chunk.array.length;
                System.arraycopy(chunk.array, 0, arrays, arrayOffset, chunk.array.length);
                arrayOffset += chunk.array.length;
            } else if (chunk.kind == RUNS) {
                offsets[i] = runOffset;
                lengths[i] = chunk.runs.length;
                System.arraycopy(chunk.runs, 0, runs, runOffset, chunk.runs.length);
                runOffset += chunk.runs.length;
            } else {
                offsets[i] = bitmapOffset;
                lengths[i] = BITMAP_WORDS;
                System.arraycopy(chunk.bitmap, 0, bitmaps, bitmapOffset, BITMAP_WORDS);
                bitmapOffset += BITMAP_WORDS;
            }
        }
        return new MatRoaringIntSet(total, keys, kinds, offsets, lengths, counts, prefix, arrays, runs, bitmaps);
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
