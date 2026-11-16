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

/*
 * @test
 * @summary The generic Bloom filter owner carries the provider contract folded from the Guava fork: put, putAll,
 *          isCompatible, copy, approximateElementCount, toBloomFilter, writeTo and readFrom, equals and hashCode.
 * @modules jdk.m3.collections/jdk.internal.m3.collections.bits
 * @run main BloomFilterContractTest
 */
import jdk.internal.m3.collections.bits.BloomFilter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;

public class BloomFilterContractTest {
    private static final int BITS = 4096;
    private static final int HASHES = 5;

    public static void main(String[] args) throws Exception {
        putReportsChangedBits();
        addAndPutSetTheSameBits();
        compatibility();
        putAllIsTheInPlaceUnion();
        copyIsIndependent();
        approximateElementCount();
        collector();
        streamRoundTrip();
        rejectedStreams();
        equality();
        System.out.println("PASS BloomFilter folded contract: 10 groups");
    }

    static void putReportsChangedBits() {
        BloomFilter<String> filter = BloomFilter.create(BITS, HASHES);
        require(filter.put("a"), "first put changes bits");
        require(!filter.put("a"), "repeated put changes nothing");
        require(filter.mightContain("a") && filter.count() == 2, "put inserts and counts");
        require(filter.put(null) && !filter.put(null) && filter.mightContain(null), "null hashes as zero");
        Random random = new Random(0x4d33424cL);
        for (int i = 0; i < 2000; i++) {
            String item = Long.toString(random.nextInt(600));
            int before = filter.bitsSet();
            boolean changed = filter.put(item);
            require(changed == (filter.bitsSet() > before), "put result equals bit change for " + item);
            require(filter.mightContain(item), "inserted item cannot be absent");
        }
    }

    static void addAndPutSetTheSameBits() {
        BloomFilter<Integer> added = BloomFilter.create(BITS, HASHES);
        BloomFilter<Integer> put = BloomFilter.create(BITS, HASHES);
        for (int i = 0; i < 300; i++) {
            require(added.add(i) == added, "add stays fluent");
            put.put(i);
        }
        require(added.equals(put) && added.bitsSet() == put.bitsSet() && added.count() == put.count(),
                "add and put are one insertion");
    }

    static void compatibility() {
        BloomFilter<String> filter = BloomFilter.create(BITS, HASHES);
        require(!filter.isCompatible(filter), "a filter is not compatible with itself");
        require(filter.isCompatible(BloomFilter.create(BITS, HASHES)), "same configuration");
        require(!filter.isCompatible(BloomFilter.create(BITS * 2, HASHES)), "other bit count");
        require(!filter.isCompatible(BloomFilter.create(BITS, HASHES + 1)), "other hash count");
        requireThrows(NullPointerException.class, () -> filter.isCompatible(null));
        requireThrows(IllegalArgumentException.class, () -> filter.putAll(filter));
        requireThrows(IllegalArgumentException.class, () -> filter.putAll(BloomFilter.create(BITS, HASHES + 1)));
        requireThrows(NullPointerException.class, () -> filter.putAll(null));
    }

    static void putAllIsTheInPlaceUnion() {
        BloomFilter<Integer> left = filled(0, 100);
        BloomFilter<Integer> right = filled(100, 200);
        BloomFilter<Integer> rightBefore = right.copy();
        BloomFilter<Integer> merged = left.merge(right);
        left.putAll(right);
        require(left.equals(merged) && left.count() == 200, "putAll equals merge, in place");
        require(right.equals(rightBefore) && right.count() == 100, "the argument is unchanged");
        require(IntStream.range(0, 200).allMatch(left::mightContain), "union holds both sides");
    }

    static void copyIsIndependent() {
        BloomFilter<Integer> filter = filled(0, 50);
        BloomFilter<Integer> copy = filter.copy();
        require(copy != filter && copy.equals(filter) && copy.count() == 50, "copy carries bits and count");
        require(copy.numBits() == BITS && copy.numHashFunctions() == HASHES, "copy carries the configuration");
        int before = filter.bitsSet();
        IntStream.range(1000, 1100).forEach(copy::put);
        require(filter.bitsSet() == before && !copy.equals(filter), "writes to the copy stay in the copy");
        filter.clear();
        require(copy.mightContain(1050), "clearing the source keeps the copy");
    }

    static void approximateElementCount() {
        BloomFilter<Integer> filter = BloomFilter.create(1000, 0.01);
        require(filter.approximateElementCount() == 0, "empty filter");
        IntStream.range(0, 1000).forEach(filter::put);
        long estimate = filter.approximateElementCount();
        double fraction = (double) filter.bitsSet() / filter.numBits();
        long formula = Math.round(-Math.log1p(-fraction) * filter.numBits() / filter.numHashFunctions());
        require(estimate == formula, "estimate follows the bit fraction: " + estimate + " vs " + formula);
        require(Math.abs(estimate - 1000) <= 50, "estimate within 5% of 1000 distinct items: " + estimate);
        IntStream.range(0, 1000).forEach(filter::put);
        require(filter.approximateElementCount() == estimate, "repeats do not move the estimate");
        BloomFilter<Integer> saturated = BloomFilter.create(8, 1);
        IntStream.range(0, 4096).forEach(saturated::put);
        require(saturated.bitsSet() == 8 && saturated.approximateElementCount() == Long.MAX_VALUE, "saturated filter");
    }

    static void collector() {
        BloomFilter<Integer> expected = BloomFilter.create(5000, 0.01);
        IntStream.range(0, 5000).forEach(expected::put);
        BloomFilter<Integer> sequential = IntStream.range(0, 5000).boxed().collect(BloomFilter.toBloomFilter(5000, 0.01));
        BloomFilter<Integer> parallel =
                IntStream.range(0, 5000).boxed().parallel().collect(BloomFilter.toBloomFilter(5000, 0.01));
        require(sequential.equals(expected) && sequential.count() == 5000, "sequential collect");
        require(parallel.equals(expected) && parallel.count() == 5000, "parallel collect combines with putAll");
    }

    static void streamRoundTrip() throws IOException {
        BloomFilter<Integer> first = filled(0, 300);
        BloomFilter<Integer> empty = BloomFilter.create(77, 3);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        first.writeTo(bytes);
        empty.writeTo(bytes);
        ByteArrayInputStream in = new ByteArrayInputStream(bytes.toByteArray());
        BloomFilter<Integer> firstRead = BloomFilter.readFrom(in);
        BloomFilter<Integer> emptyRead = BloomFilter.readFrom(in);
        require(in.available() == 0, "two filters, back to back, consume the stream exactly");
        require(firstRead.equals(first) && firstRead.count() == 300 && firstRead.hashCode() == first.hashCode(),
                "round trip keeps bits and count");
        require(IntStream.range(0, 300).allMatch(firstRead::mightContain), "round trip keeps membership");
        require(emptyRead.equals(empty) && emptyRead.numBits() == 77 && emptyRead.numHashFunctions() == 3
                && emptyRead.bitsSet() == 0, "empty filter round trip");
    }

    static void rejectedStreams() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        filled(0, 300).writeTo(bytes);
        byte[] good = bytes.toByteArray();
        requireThrows(EOFException.class, () -> BloomFilter.readFrom(new ByteArrayInputStream(Arrays.copyOf(good, good.length - 1))));
        requireThrows(IOException.class, () -> BloomFilter.readFrom(header(0, 3, 0, 0)));
        requireThrows(IOException.class, () -> BloomFilter.readFrom(header(64, 0, 0, 0)));
        requireThrows(IOException.class, () -> BloomFilter.readFrom(header(64, 3, -1, 0)));
        requireThrows(IOException.class, () -> BloomFilter.readFrom(header(64, 3, 0, 2)));
        requireThrows(IOException.class, () -> BloomFilter.readFrom(header(60, 3, 1, 1, 1L << 61)));
        require(BloomFilter.readFrom(header(60, 3, 1, 1, 1L << 59)).bitsSet() == 1, "highest valid bit is accepted");
    }

    static void equality() {
        BloomFilter<String> once = BloomFilter.create(BITS, HASHES);
        BloomFilter<String> twice = BloomFilter.create(BITS, HASHES);
        once.put("a");
        twice.put("a");
        twice.put("a");
        require(once.equals(twice) && once.hashCode() == twice.hashCode(), "the insertion count is not compared");
        require(once.equals(once) && !once.equals(null) && !once.equals("a"), "identity, null and other types");
        twice.put("b");
        require(!once.equals(twice), "other bits");
        require(!BloomFilter.create(BITS, HASHES).equals(BloomFilter.create(BITS * 2, HASHES)), "other bit count");
        require(!BloomFilter.create(BITS, HASHES).equals(BloomFilter.create(BITS, HASHES + 1)), "other hash count");
    }

    private static BloomFilter<Integer> filled(int from, int to) {
        BloomFilter<Integer> filter = BloomFilter.create(BITS, HASHES);
        IntStream.range(from, to).forEach(filter::put);
        return filter;
    }

    private static ByteArrayInputStream header(int numBits, int numHashFunctions, int count, int wordCount, long... words)
            throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(bytes);
        data.writeInt(numBits);
        data.writeInt(numHashFunctions);
        data.writeInt(count);
        data.writeInt(wordCount);
        for (long word : words) {
            data.writeLong(word);
        }
        return new ByteArrayInputStream(bytes.toByteArray());
    }

    private interface Action {
        void run() throws Exception;
    }

    private static void requireThrows(Class<? extends Exception> expected, Action action) {
        try {
            action.run();
        } catch (Exception e) {
            require(expected.isInstance(e), "expected " + expected.getSimpleName() + " but got " + e);
            return;
        }
        throw new AssertionError("expected " + expected.getSimpleName());
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
