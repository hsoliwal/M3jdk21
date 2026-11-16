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
package jdk.internal.m3.collections.bits;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.BitSet;
import java.util.Objects;
import java.util.stream.Collector;

/**
 * Bloom filter with primitive double-hashing hot paths and no per-probe lambda traversal.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/ds/BloomFilter.java}
 * blob {@code 6836b30dc0bbba69f8bd4959bdd7180db2b655ae} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (algorithms.ds batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 *
 * <p>Folded from the Guava fork ({@code com.google.common.hash.BloomFilter}, hsoliwal/guava {@code c6e50e77a77};
 * FOLD_GAP v1 found these behaviours absent here): {@link #put}, {@link #putAll}, {@link #isCompatible},
 * {@link #copy}, {@link #approximateElementCount}, {@link #toBloomFilter}, {@link #writeTo}, {@link #readFrom},
 * {@link #equals} and {@link #hashCode}, expressed over the lanes of this owner. Funnel-based hashing and
 * Java serialization stay with the provider.
 */
public final class BloomFilter<T> {
    private final BitSet bits;
    private final int numBits;
    private final int numHashFunctions;
    private int count;

    private BloomFilter(int numBits, int numHashFunctions) {
        this.bits = new BitSet(numBits);
        this.numBits = numBits;
        this.numHashFunctions = numHashFunctions;
    }

    public static <T> BloomFilter<T> create(int expectedElements, double falsePositiveRate) {
        int bits = optimalNumBits(expectedElements, falsePositiveRate);
        return new BloomFilter<>(bits, optimalNumHashFunctions(expectedElements, bits));
    }

    public static <T> BloomFilter<T> create(int numBits, int numHashFunctions) {
        return new BloomFilter<>(numBits, numHashFunctions);
    }

    public static <T> BloomFilter<T> create() {
        return new BloomFilter<>(10_000, 7);
    }

    public BloomFilter<T> add(T item) {
        put(item);
        return this;
    }

    /**
     * Adds an item and reports whether any bit changed. {@code true} means the item was definitely absent before
     * this call; {@code false} means every bit of the item was already set, by this item or by others.
     *
     * @param item the item to add; {@code null} hashes as zero
     * @return whether the bits of this filter changed
     */
    public boolean put(T item) {
        long baseHash = mix64(item == null ? 0L : item.hashCode());
        long deltaHash = mix64(baseHash + 0x9e3779b97f4a7c15L);
        boolean changed = false;
        for (int seed = 0; seed < numHashFunctions; seed++) {
            int position = position(baseHash, deltaHash, seed);
            changed |= !bits.get(position);
            bits.set(position);
        }
        count++;
        return changed;
    }

    public boolean mightContain(T item) {
        long baseHash = mix64(item == null ? 0L : item.hashCode());
        long deltaHash = mix64(baseHash + 0x9e3779b97f4a7c15L);
        for (int seed = 0; seed < numHashFunctions; seed++) {
            if (!bits.get(position(baseHash, deltaHash, seed))) return false;
        }
        return true;
    }

    public double estimatedFalsePositiveRate() {
        double bitsSet = bits.cardinality();
        return Math.pow(bitsSet / numBits, numHashFunctions);
    }

    public int count() {
        return count;
    }

    public int bitsSet() {
        return bits.cardinality();
    }

    public int numBits() {
        return numBits;
    }

    public int numHashFunctions() {
        return numHashFunctions;
    }

    public void clear() {
        bits.clear();
        count = 0;
    }

    public BloomFilter<T> merge(BloomFilter<T> other) {
        if (numBits != other.numBits || numHashFunctions != other.numHashFunctions) {
            throw new IllegalArgumentException("Filters must have same configuration");
        }
        BloomFilter<T> merged = new BloomFilter<>(numBits, numHashFunctions);
        merged.bits.or(bits);
        merged.bits.or(other.bits);
        merged.count = count + other.count;
        return merged;
    }

    /**
     * Tells whether {@link #putAll} accepts the given filter: another instance with the same number of bits and
     * of hash functions. A filter is not compatible with itself.
     *
     * @param that the filter to check
     * @return whether the two filters can be combined in place
     * @throws NullPointerException if {@code that} is {@code null}
     */
    public boolean isCompatible(BloomFilter<T> that) {
        Objects.requireNonNull(that);
        return this != that && numBits == that.numBits && numHashFunctions == that.numHashFunctions;
    }

    /**
     * Combines the given filter into this one, in place: afterwards this filter might contain every item that
     * either filter might contain. Unlike {@link #merge}, no filter is allocated and {@code that} is unchanged.
     *
     * @param that a compatible filter
     * @throws IllegalArgumentException if {@link #isCompatible} rejects {@code that}
     * @throws NullPointerException if {@code that} is {@code null}
     */
    public void putAll(BloomFilter<T> that) {
        if (!isCompatible(that)) {
            throw new IllegalArgumentException("Filters must be distinct and have same configuration");
        }
        bits.or(that.bits);
        count += that.count;
    }

    /**
     * Returns an independent filter with the same configuration, bits and insertion count.
     *
     * @return the copy
     */
    public BloomFilter<T> copy() {
        BloomFilter<T> copy = new BloomFilter<>(numBits, numHashFunctions);
        copy.bits.or(bits);
        copy.count = count;
        return copy;
    }

    /**
     * Estimates the number of distinct items added, from the fraction of bits set. The estimate is close while
     * the filter is not saturated; a filter with every bit set reports {@code Long.MAX_VALUE}.
     *
     * @return the estimated number of distinct items
     */
    public long approximateElementCount() {
        double fractionOfBitsSet = (double) bits.cardinality() / numBits;
        return Math.round(-Math.log1p(-fractionOfBitsSet) * numBits / numHashFunctions);
    }

    /**
     * Returns a collector that puts the stream items into a new filter made by {@link #create(int, double)}.
     * The collector is unordered and not concurrent.
     *
     * @param <T> the item type
     * @param expectedElements the expected number of items
     * @param falsePositiveRate the wanted false positive rate
     * @return the collector
     */
    public static <T> Collector<T, ?, BloomFilter<T>> toBloomFilter(int expectedElements, double falsePositiveRate) {
        return Collector.of(
                () -> BloomFilter.<T>create(expectedElements, falsePositiveRate),
                BloomFilter::put,
                (left, right) -> {
                    left.putAll(right);
                    return left;
                },
                Collector.Characteristics.UNORDERED);
    }

    /**
     * Writes this filter: the number of bits, of hash functions and of insertions and the number of 64-bit words
     * as {@code int}, then the words of the bit lane. The stream is flushed, not closed.
     *
     * @param out the stream to write to
     * @throws IOException if the stream fails
     */
    public void writeTo(OutputStream out) throws IOException {
        DataOutputStream data = new DataOutputStream(out);
        long[] words = bits.toLongArray();
        data.writeInt(numBits);
        data.writeInt(numHashFunctions);
        data.writeInt(count);
        data.writeInt(words.length);
        for (long word : words) {
            data.writeLong(word);
        }
        data.flush();
    }

    /**
     * Reads a filter written by {@link #writeTo}. The stream is not closed.
     *
     * @param <T> the item type
     * @param in the stream to read from
     * @return the filter
     * @throws IOException if the stream fails or does not hold a filter written by {@code writeTo}
     */
    public static <T> BloomFilter<T> readFrom(InputStream in) throws IOException {
        DataInputStream data = new DataInputStream(in);
        int numBits = data.readInt();
        int numHashFunctions = data.readInt();
        int count = data.readInt();
        int wordCount = data.readInt();
        if (numBits <= 0 || numHashFunctions <= 0 || count < 0 || wordCount < 0 || wordCount > (numBits + 63L) / 64) {
            throw new IOException("Not a BloomFilter stream");
        }
        BloomFilter<T> filter = new BloomFilter<>(numBits, numHashFunctions);
        filter.bits.or(readWords(data, wordCount, numBits));
        filter.count = count;
        return filter;
    }

    private static BitSet readWords(DataInputStream data, int wordCount, int numBits) throws IOException {
        long[] words = new long[wordCount];
        for (int i = 0; i < wordCount; i++) {
            words[i] = data.readLong();
        }
        BitSet bits = BitSet.valueOf(words);
        if (bits.length() > numBits) {
            throw new IOException("Not a BloomFilter stream");
        }
        return bits;
    }

    public static int optimalNumBits(int expectedElements, double falsePositiveRate) {
        return (int) Math.ceil(
                -expectedElements * Math.log(falsePositiveRate) / (Math.log(2) * Math.log(2)));
    }

    public static int optimalNumHashFunctions(int expectedElements, int numBits) {
        return Math.max(1, (int) Math.round((double) numBits / expectedElements * Math.log(2)));
    }

    private int position(long baseHash, long deltaHash, int seed) {
        return Math.floorMod(baseHash + seed * deltaHash, numBits);
    }

    private static long mix64(long value) {
        long mixed = value;
        mixed ^= mixed >>> 33;
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= mixed >>> 33;
        mixed *= 0xc4ceb9fe1a85ec53L;
        mixed ^= mixed >>> 33;
        return mixed & Long.MAX_VALUE;
    }

    /**
     * Compares configuration and bits: two filters are equal when they have the same number of bits and of hash
     * functions and the same bits set, so they answer {@link #mightContain} alike. The insertion count is not
     * compared.
     */
    @Override
    public boolean equals(Object object) {
        return object == this
                || object instanceof BloomFilter<?> that
                        && numBits == that.numBits
                        && numHashFunctions == that.numHashFunctions
                        && bits.equals(that.bits);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numBits, numHashFunctions, bits);
    }

    @Override
    public String toString() {
        return String.format(
                "M3BloomFilter[bits=%d, hashes=%d, count=%d, fpr=%.4f]",
                numBits,
                numHashFunctions,
                count,
                estimatedFalsePositiveRate());
    }
}
