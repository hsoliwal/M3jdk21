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
package jdk.m3.collections.bits;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntConsumer;

/**
 * Roaring-style compressed unsigned-int set.
 *
 * <p>The 32-bit space is split into 16-bit chunks. Sparse chunks use sorted
 * char[] containers; dense chunks use fixed long[1024] bitmaps. There is one
 * container object per populated chunk, never one object per retained int.
 *
 * <p>Iteration order is unsigned-int order because chunk keys are the unsigned
 * high 16 bits.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitiveRoaringIntSet.java}
 * blob {@code 6b9b17a4e38de9b4566e955d106ca440e9d0bde7} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class RoaringIntSet {
    /** Creates an empty set (explicit form of the default constructor; required by -Xlint:all -Werror). */
    public RoaringIntSet() {
    }

    private static final byte ARRAY = 0;
    private static final byte BITMAP = 1;
    private static final int ARRAY_TO_BITMAP = 4096;
    private static final int BITMAP_TO_ARRAY = 2048;
    private static final int BITMAP_WORDS = 1 << 10;
    /** Length of the char[] a new chunk starts with; merges replay growth from it. */
    private static final int FRESH_ARRAY_LENGTH = 4;
    /** Array intersection switches to binary search when one side is this many times longer. */
    private static final int GALLOP_RATIO = 32;

    private int[] highKeys = new int[4];
    private byte[] types = new byte[4];
    private Object[] containers = new Object[4];
    private int[] cardinalities = new int[4];
    private int containerCount;
    private long cardinality;

    public int containerCount() { return containerCount; }
    public long cardinality() { return cardinality; }
    public boolean isEmpty() { return cardinality == 0; }
    public int containerReferenceSlots() { return containers.length; }

    public long primitivePayloadBytes() {
        long bytes = (long) highKeys.length * Integer.BYTES
                + types.length
                + (long) cardinalities.length * Integer.BYTES;
        for (int i = 0; i < containerCount; i++) {
            bytes += types[i] == ARRAY
                    ? (long) ((char[]) containers[i]).length * Character.BYTES
                    : (long) BITMAP_WORDS * Long.BYTES;
        }
        return bytes;
    }

    public boolean contains(int value) {
        int high = value >>> 16;
        int index = Arrays.binarySearch(highKeys, 0, containerCount, high);
        if (index < 0) return false;
        char low = (char) value;
        if (types[index] == ARRAY) {
            char[] values = (char[]) containers[index];
            return Arrays.binarySearch(values, 0, cardinalities[index], low) >= 0;
        }
        long[] words = (long[]) containers[index];
        int ordinal = low;
        return (words[ordinal >>> 6] & (1L << (ordinal & 63))) != 0L;
    }

    public boolean add(int value) {
        int high = value >>> 16;
        char low = (char) value;
        int index = Arrays.binarySearch(highKeys, 0, containerCount, high);
        if (index < 0) {
            index = -index - 1;
            insertContainer(index, high);
        }
        boolean added = types[index] == ARRAY
                ? addArray(index, low)
                : addBitmap(index, low);
        if (added) cardinality++;
        return added;
    }

    public boolean remove(int value) {
        int high = value >>> 16;
        char low = (char) value;
        int index = Arrays.binarySearch(highKeys, 0, containerCount, high);
        if (index < 0) return false;
        boolean removed = types[index] == ARRAY
                ? removeArray(index, low)
                : removeBitmap(index, low);
        if (!removed) return false;
        cardinality--;
        if (cardinalities[index] == 0) removeContainer(index);
        return true;
    }

    public void clear() {
        Arrays.fill(containers, 0, containerCount, null);
        Arrays.fill(highKeys, 0, containerCount, 0);
        Arrays.fill(types, 0, containerCount, (byte) 0);
        Arrays.fill(cardinalities, 0, containerCount, 0);
        containerCount = 0;
        cardinality = 0L;
    }

    public void forEach(IntConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int container = 0; container < containerCount; container++) {
            int high = highKeys[container] << 16;
            if (types[container] == ARRAY) {
                char[] values = (char[]) containers[container];
                int count = cardinalities[container];
                for (int i = 0; i < count; i++) consumer.accept(high | values[i]);
            } else {
                long[] words = (long[]) containers[container];
                for (int wordIndex = 0; wordIndex < BITMAP_WORDS; wordIndex++) {
                    long word = words[wordIndex];
                    while (word != 0L) {
                        int bit = Long.numberOfTrailingZeros(word);
                        consumer.accept(high | (wordIndex << 6) | bit);
                        word &= word - 1L;
                    }
                }
            }
        }
    }

    //FLLDCIM: LEETCODIFY - chunk-key lane merge (MAX_VALUE ends a lane); lanes as copy() + adds
    public RoaringIntSet union(RoaringIntSet other) {
        Objects.requireNonNull(other, "other");
        RoaringIntSet result = new RoaringIntSet();
        result.ensureContainerCapacity(containerCount); // directory capacity of copy()
        int i = 0;
        int j = 0;
        while (i < containerCount || j < other.containerCount) {
            int left = i < containerCount ? highKeys[i] : Integer.MAX_VALUE;
            int right = j < other.containerCount ? other.highKeys[j] : Integer.MAX_VALUE;
            if (left < right) result.appendCopy(this, i);
            else if (left > right) result.appendFresh(other, j);
            else result.appendUnion(this, i, other, j);
            if (left <= right) i++;
            if (left >= right) j++;
        }
        return result;
    }

    //FLLDCIM: LEETCODIFY - chunk-key lane walk; lanes as a new set fed the common values in order
    public RoaringIntSet intersection(RoaringIntSet other) {
        Objects.requireNonNull(other, "other");
        RoaringIntSet result = new RoaringIntSet();
        int i = 0;
        int j = 0;
        while (i < containerCount && j < other.containerCount) {
            int left = highKeys[i];
            int right = other.highKeys[j];
            if (left == right) result.appendIntersection(this, i, other, j);
            if (left <= right) i++;
            if (left >= right) j++;
        }
        return result;
    }

    public RoaringIntSet copy() {
        RoaringIntSet result = new RoaringIntSet();
        result.ensureContainerCapacity(containerCount);
        result.containerCount = containerCount;
        result.cardinality = cardinality;
        System.arraycopy(highKeys, 0, result.highKeys, 0, containerCount);
        System.arraycopy(types, 0, result.types, 0, containerCount);
        System.arraycopy(cardinalities, 0, result.cardinalities, 0, containerCount);
        for (int i = 0; i < containerCount; i++) {
            result.containers[i] = types[i] == ARRAY
                    ? ((char[]) containers[i]).clone()
                    : ((long[]) containers[i]).clone();
        }
        return result;
    }

    /** Appends a chunk after the last slot (keys arrive ascending); grows as insertContainer. */
    private void append(int high, byte type, Object container, int count) {
        ensureContainerCapacity(containerCount + 1);
        int slot = containerCount++;
        highKeys[slot] = high;
        types[slot] = type;
        containers[slot] = container;
        cardinalities[slot] = count;
        cardinality += count;
    }

    /** Chunk only in {@code source}: kept exactly as copy() keeps it. */
    private void appendCopy(RoaringIntSet source, int index) {
        byte type = source.types[index];
        Object container = type == ARRAY
                ? ((char[]) source.containers[index]).clone()
                : ((long[]) source.containers[index]).clone();
        append(source.highKeys[index], type, container, source.cardinalities[index]);
    }

    /** Chunk only in {@code source}: laid out as a new chunk fed its values one by one. */
    private void appendFresh(RoaringIntSet source, int index) {
        int high = source.highKeys[index];
        int count = source.cardinalities[index];
        if (source.types[index] == BITMAP) {
            long[] words = (long[]) source.containers[index];
            long[] owned = count > ARRAY_TO_BITMAP ? words.clone() : words;
            appendWords(high, owned, count, FRESH_ARRAY_LENGTH);
            return;
        }
        char[] values = new char[grownLength(FRESH_ARRAY_LENGTH, count)];
        System.arraycopy(source.containers[index], 0, values, 0, count);
        append(high, ARRAY, values, count);
    }

    /** Chunk in both sets: copy() of the left chunk, then each right value added in order. */
    private void appendUnion(RoaringIntSet left, int i, RoaringIntSet right, int j) {
        int high = left.highKeys[i];
        Object mine = left.containers[i];
        Object theirs = right.containers[j];
        int theirCount = right.cardinalities[j];
        if (left.types[i] == BITMAP) {
            long[] words = ((long[]) mine).clone();
            orInto(words, theirs, right.types[j] == BITMAP, theirCount);
            append(high, BITMAP, words, bitCount(words));
        } else if (right.types[j] == BITMAP) {
            appendArrayBitmapUnion(high, (char[]) mine, left.cardinalities[i], (long[]) theirs);
        } else {
            appendArrayUnion(high, (char[]) mine, left.cardinalities[i],
                    (char[]) theirs, theirCount);
        }
    }

    /** Left chunk an array, right chunk a bitmap: the length replays from the left array. */
    private void appendArrayBitmapUnion(int high, char[] values, int count, long[] bitmap) {
        long[] words = bitmap.clone();
        setBits(words, values, count);
        appendWords(high, words, bitCount(words), values.length);
    }

    /** Both chunks arrays: counted first, then merged into the replayed length or a bitmap. */
    private void appendArrayUnion(int high, char[] left, int leftCount,
            char[] right, int rightCount) {
        int merged = unionCount(left, leftCount, right, rightCount);
        if (merged > ARRAY_TO_BITMAP) {
            long[] words = new long[BITMAP_WORDS];
            setBits(words, left, leftCount);
            setBits(words, right, rightCount);
            append(high, BITMAP, words, merged);
            return;
        }
        char[] values = new char[grownLength(left.length, merged)];
        mergeArrays(left, leftCount, right, rightCount, values);
        append(high, ARRAY, values, merged);
    }

    /**
     * Private bitmap of {@code count} values for a chunk the per-value path holds as an array of
     * {@code baseLength} first: kept as the bitmap above ARRAY_TO_BITMAP, else only decoded.
     */
    private void appendWords(int high, long[] words, int count, int baseLength) {
        if (count == 0) return;
        if (count > ARRAY_TO_BITMAP) {
            append(high, BITMAP, words, count);
            return;
        }
        char[] values = new char[grownLength(baseLength, count)];
        decode(words, values);
        append(high, ARRAY, values, count);
    }

    /** Chunk in both sets: a new chunk fed the common values; operand order is irrelevant. */
    private void appendIntersection(RoaringIntSet left, int i, RoaringIntSet right, int j) {
        if (left.types[i] == BITMAP && right.types[j] == ARRAY) {
            appendIntersection(right, j, left, i);
            return;
        }
        int high = left.highKeys[i];
        Object theirs = right.containers[j];
        if (left.types[i] == BITMAP) {
            long[] words = andWords((long[]) left.containers[i], (long[]) theirs);
            appendWords(high, words, bitCount(words), FRESH_ARRAY_LENGTH);
        } else {
            appendArrayIntersection(high, (char[]) left.containers[i], left.cardinalities[i],
                    theirs, right.types[j] == BITMAP, right.cardinalities[j]);
        }
    }

    /** Array chunk against an array or bitmap chunk; hits trimmed to the new-chunk length. */
    private void appendArrayIntersection(int high, char[] values, int count,
            Object other, boolean otherBitmap, int otherCount) {
        char[] buffer = new char[grownLength(FRESH_ARRAY_LENGTH, Math.min(count, otherCount))];
        int found = otherBitmap
                ? filter(values, count, (long[]) other, buffer)
                : intersectArrays(values, count, (char[]) other, otherCount, buffer);
        if (found == 0) return;
        int length = grownLength(FRESH_ARRAY_LENGTH, found);
        char[] kept = length == buffer.length ? buffer : Arrays.copyOf(buffer, length);
        append(high, ARRAY, kept, found);
    }

    /** Array values whose bit is set in {@code words}, in array order. */
    private static int filter(char[] values, int count, long[] words, char[] out) {
        int found = 0;
        for (int k = 0; k < count; k++) {
            int value = values[k];
            if ((words[value >>> 6] & (1L << (value & 63))) != 0L) out[found++] = (char) value;
        }
        return found;
    }

    /** Sorted-array intersection: binary search when one side is GALLOP_RATIO times longer. */
    private static int intersectArrays(char[] left, int leftCount, char[] right, int rightCount,
            char[] out) {
        if (leftCount > rightCount) {
            return intersectArrays(right, rightCount, left, leftCount, out);
        }
        return leftCount * GALLOP_RATIO < rightCount
                ? intersectSkewed(left, leftCount, right, rightCount, out)
                : intersectLinear(left, leftCount, right, rightCount, out);
    }

    private static int intersectLinear(char[] left, int leftCount, char[] right, int rightCount,
            char[] out) {
        int i = 0;
        int j = 0;
        int found = 0;
        while (i < leftCount && j < rightCount) {
            char a = left[i];
            char b = right[j];
            if (a == b) out[found++] = a;
            if (a <= b) i++;
            if (b <= a) j++;
        }
        return found;
    }

    private static int intersectSkewed(char[] small, int smallCount, char[] large, int largeCount,
            char[] out) {
        int found = 0;
        int from = 0;
        for (int k = 0; k < smallCount && from < largeCount; k++) {
            int at = Arrays.binarySearch(large, from, largeCount, small[k]);
            if (at >= 0) out[found++] = small[k];
            from = at >= 0 ? at + 1 : -at - 1;
        }
        return found;
    }

    /** Size of the union of two sorted arrays: both counts less the common values. */
    private static int unionCount(char[] left, int leftCount, char[] right, int rightCount) {
        int i = 0;
        int j = 0;
        int common = 0;
        while (i < leftCount && j < rightCount) {
            char a = left[i];
            char b = right[j];
            if (a == b) common++;
            if (a <= b) i++;
            if (b <= a) j++;
        }
        return leftCount + rightCount - common;
    }

    /** Merge of two sorted arrays without duplicates; the one remaining tail is copied whole. */
    private static void mergeArrays(char[] left, int leftCount, char[] right, int rightCount,
            char[] out) {
        int i = 0;
        int j = 0;
        int n = 0;
        while (i < leftCount && j < rightCount) {
            char a = left[i];
            char b = right[j];
            out[n++] = a <= b ? a : b;
            if (a <= b) i++;
            if (b <= a) j++;
        }
        System.arraycopy(left, i, out, n, leftCount - i);
        System.arraycopy(right, j, out, n + leftCount - i, rightCount - j);
    }

    private static void setBits(long[] words, char[] values, int count) {
        for (int k = 0; k < count; k++) {
            int value = values[k];
            words[value >>> 6] |= 1L << (value & 63);
        }
    }

    private static void orInto(long[] words, Object source, boolean bitmap, int count) {
        if (!bitmap) {
            setBits(words, (char[]) source, count);
            return;
        }
        long[] other = (long[]) source;
        for (int w = 0; w < BITMAP_WORDS; w++) words[w] |= other[w];
    }

    private static long[] andWords(long[] left, long[] right) {
        long[] words = new long[BITMAP_WORDS];
        for (int w = 0; w < BITMAP_WORDS; w++) words[w] = left[w] & right[w];
        return words;
    }

    private static int bitCount(long[] words) {
        int count = 0;
        for (int w = 0; w < BITMAP_WORDS; w++) count += Long.bitCount(words[w]);
        return count;
    }

    private static void decode(long[] words, char[] out) {
        int n = 0;
        for (int w = 0; w < BITMAP_WORDS; w++) {
            for (long word = words[w]; word != 0L; word &= word - 1L) {
                out[n++] = (char) ((w << 6) | Long.numberOfTrailingZeros(word));
            }
        }
    }

    /** The char[] length addArray reaches from {@code length} by holding {@code count} values. */
    private static int grownLength(int length, int count) {
        int next = length;
        while (next < count) next = Math.min(ARRAY_TO_BITMAP + 1, next + (next >>> 1) + 1);
        return next;
    }

    private void insertContainer(int index, int high) {
        ensureContainerCapacity(containerCount + 1);
        int moved = containerCount - index;
        if (moved > 0) {
            System.arraycopy(highKeys, index, highKeys, index + 1, moved);
            System.arraycopy(types, index, types, index + 1, moved);
            System.arraycopy(containers, index, containers, index + 1, moved);
            System.arraycopy(cardinalities, index, cardinalities, index + 1, moved);
        }
        highKeys[index] = high;
        types[index] = ARRAY;
        containers[index] = new char[FRESH_ARRAY_LENGTH];
        cardinalities[index] = 0;
        containerCount++;
    }

    private void removeContainer(int index) {
        int moved = containerCount - index - 1;
        if (moved > 0) {
            System.arraycopy(highKeys, index + 1, highKeys, index, moved);
            System.arraycopy(types, index + 1, types, index, moved);
            System.arraycopy(containers, index + 1, containers, index, moved);
            System.arraycopy(cardinalities, index + 1, cardinalities, index, moved);
        }
        int last = --containerCount;
        highKeys[last] = 0;
        types[last] = 0;
        containers[last] = null;
        cardinalities[last] = 0;
    }

    private boolean addArray(int index, char low) {
        char[] values = (char[]) containers[index];
        int count = cardinalities[index];
        int position = Arrays.binarySearch(values, 0, count, low);
        if (position >= 0) return false;
        position = -position - 1;
        if (count == values.length) {
            int next = Math.min(ARRAY_TO_BITMAP + 1, values.length + (values.length >>> 1) + 1);
            values = Arrays.copyOf(values, next);
            containers[index] = values;
        }
        System.arraycopy(values, position, values, position + 1, count - position);
        values[position] = low;
        cardinalities[index] = count + 1;
        if (count + 1 > ARRAY_TO_BITMAP) promoteToBitmap(index);
        return true;
    }

    private boolean removeArray(int index, char low) {
        char[] values = (char[]) containers[index];
        int count = cardinalities[index];
        int position = Arrays.binarySearch(values, 0, count, low);
        if (position < 0) return false;
        int moved = count - position - 1;
        if (moved > 0) System.arraycopy(values, position + 1, values, position, moved);
        values[count - 1] = 0;
        cardinalities[index] = count - 1;
        return true;
    }

    private boolean addBitmap(int index, char low) {
        long[] words = (long[]) containers[index];
        int ordinal = low;
        int word = ordinal >>> 6;
        long mask = 1L << (ordinal & 63);
        if ((words[word] & mask) != 0L) return false;
        words[word] |= mask;
        cardinalities[index]++;
        return true;
    }

    private boolean removeBitmap(int index, char low) {
        long[] words = (long[]) containers[index];
        int ordinal = low;
        int word = ordinal >>> 6;
        long mask = 1L << (ordinal & 63);
        if ((words[word] & mask) == 0L) return false;
        words[word] &= ~mask;
        int count = --cardinalities[index];
        if (count <= BITMAP_TO_ARRAY) demoteToArray(index);
        return true;
    }

    private void promoteToBitmap(int index) {
        char[] values = (char[]) containers[index];
        long[] words = new long[BITMAP_WORDS];
        int count = cardinalities[index];
        for (int i = 0; i < count; i++) {
            int ordinal = values[i];
            words[ordinal >>> 6] |= 1L << (ordinal & 63);
        }
        containers[index] = words;
        types[index] = BITMAP;
    }

    private void demoteToArray(int index) {
        long[] words = (long[]) containers[index];
        int count = cardinalities[index];
        char[] values = new char[Math.max(4, count)];
        int out = 0;
        for (int wordIndex = 0; wordIndex < BITMAP_WORDS; wordIndex++) {
            long word = words[wordIndex];
            while (word != 0L) {
                int bit = Long.numberOfTrailingZeros(word);
                values[out++] = (char) ((wordIndex << 6) | bit);
                word &= word - 1L;
            }
        }
        containers[index] = values;
        types[index] = ARRAY;
    }

    private void ensureContainerCapacity(int required) {
        if (required <= highKeys.length) return;
        int next = highKeys.length + (highKeys.length >>> 1) + 1;
        if (next < required) next = required;
        highKeys = Arrays.copyOf(highKeys, next);
        types = Arrays.copyOf(types, next);
        containers = Arrays.copyOf(containers, next);
        cardinalities = Arrays.copyOf(cardinalities, next);
    }
}
