// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import java.util.Arrays;
import java.util.Objects;

/** Deterministic primitive sorting algorithms without comparator boxing. */
//FLLDCIM: RELOCATE - donor blob a42bd2e406a1 at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/algorithm-extras-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/algorithm-extras-20261106/relocate.py v1
public final class SortingAlgorithms {
    private SortingAlgorithms() {}

    public static void mergeSort(int[] values) {
        Objects.requireNonNull(values, "values");
        if (values.length < 2) return;
        int[] source = values.clone();
        int[] destination = values;
        for (int width = 1; width < values.length; width = nextWidth(width, values.length)) {
            for (int start = 0; start < values.length; start += width << 1) {
                int middle = Math.min(start + width, values.length);
                int end = Math.min(start + (width << 1), values.length);
                merge(source, destination, start, middle, end);
            }
            int[] swap = source;
            source = destination;
            destination = swap;
        }
        if (source != values) System.arraycopy(source, 0, values, 0, values.length);
    }

    public static void heapSort(int[] values) {
        Objects.requireNonNull(values, "values");
        for (int parent = (values.length >>> 1) - 1; parent >= 0; parent--) {
            siftDown(values, parent, values.length);
        }
        for (int end = values.length - 1; end > 0; end--) {
            swap(values, 0, end);
            siftDown(values, 0, end);
        }
    }

    /**
     * Stable four-pass LSD radix sort for all signed int values.
     * Signed order is achieved by toggling the sign bit before byte extraction.
     */
    public static void radixSortSigned(int[] values) {
        Objects.requireNonNull(values, "values");
        radixSortSigned(values, new int[values.length], new int[RADIX], ProgressMonitor.none());
    }

    /** Counting sort for values in the caller-supplied inclusive range. */
    public static void countingSort(int[] values, int minimum, int maximum) {
        Objects.requireNonNull(values, "values");
        if (minimum > maximum) throw new IllegalArgumentException("minimum > maximum");
        long range = (long) maximum - minimum + 1L;
        if (range > 16_777_216L) {
            throw new IllegalArgumentException("counting range too large: " + range);
        }
        int[] counts = new int[(int) range];
        for (int value : values) {
            if (value < minimum || value > maximum) {
                throw new IllegalArgumentException("value outside declared counting range: " + value);
            }
            counts[value - minimum]++;
        }
        int cursor = 0;
        for (int offset = 0; offset < counts.length; offset++) {
            int value = minimum + offset;
            for (int count = counts[offset]; count > 0; count--) values[cursor++] = value;
        }
    }

    private static void merge(
            int[] source,
            int[] destination,
            int start,
            int middle,
            int end) {
        int left = start;
        int right = middle;
        int cursor = start;
        while (left < middle && right < end) {
            destination[cursor++] = source[left] <= source[right]
                    ? source[left++]
                    : source[right++];
        }
        while (left < middle) destination[cursor++] = source[left++];
        while (right < end) destination[cursor++] = source[right++];
    }

    private static int nextWidth(int width, int length) {
        if (width > length / 2) return length;
        return width << 1;
    }

    private static void siftDown(int[] values, int root, int size) {
        int value = values[root];
        int index = root;
        while (true) {
            int left = index * 2 + 1;
            if (left >= size) break;
            int right = left + 1;
            int child = right < size && values[right] > values[left] ? right : left;
            if (values[child] <= value) break;
            values[index] = values[child];
            index = child;
        }
        values[index] = value;
    }

    private static void swap(int[] values, int left, int right) {
        int value = values[left];
        values[left] = values[right];
        values[right] = value;
    }

    //FLLDCIM: FOLD - member IntRadix.RADIX from donor blob ad3f6f52ef60 (donor blob ad3f6f52ef60 at commit 4a2e6452a7ad (repository, path and digests: m3/algorithms/recipe/radix-lanes-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/radix-lanes-fold/FOLD_MAP.tsv
    public static final int RADIX = 256;

    //FLLDCIM: FOLD - member IntRadix.sortUnsigned from donor blob ad3f6f52ef60 (donor blob ad3f6f52ef60 at commit 4a2e6452a7ad (repository, path and digests: m3/algorithms/recipe/radix-lanes-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/radix-lanes-fold/FOLD_MAP.tsv
    public static void radixSortUnsigned(int[] values) {
        Objects.requireNonNull(values, "values");
        radixSort(values, null, new int[values.length], null, new int[RADIX], true,
                ProgressMonitor.none());
    }

    //FLLDCIM: FOLD - member IntRadix.sortSigned from donor blob ad3f6f52ef60 (donor blob ad3f6f52ef60 at commit 4a2e6452a7ad (repository, path and digests: m3/algorithms/recipe/radix-lanes-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/radix-lanes-fold/FOLD_MAP.tsv
    public static void radixSortSigned(int[] values, int[] scratch, int[] counts) {
        radixSortSigned(values, scratch, counts, ProgressMonitor.none());
    }

    //FLLDCIM: FOLD - member IntRadix.sortSigned from donor blob ad3f6f52ef60 (donor blob ad3f6f52ef60 at commit 4a2e6452a7ad (repository, path and digests: m3/algorithms/recipe/radix-lanes-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/radix-lanes-fold/FOLD_MAP.tsv
    public static void radixSortSigned(
            int[] values,
            int[] scratch,
            int[] counts,
            ProgressMonitor monitor) {
        radixSort(values, null, scratch, null, counts, false, monitor);
    }

    //FLLDCIM: FOLD - member IntRadix.sortPairsSigned from donor blob ad3f6f52ef60 (donor blob ad3f6f52ef60 at commit 4a2e6452a7ad (repository, path and digests: m3/algorithms/recipe/radix-lanes-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/radix-lanes-fold/FOLD_MAP.tsv
    /** Stable signed-key / int-payload sort; equal keys retain payload order. */
    public static void radixSortPairsSigned(
            int[] keys,
            int[] payloads,
            int[] keyScratch,
            int[] payloadScratch,
            int[] counts,
            ProgressMonitor monitor) {
        Objects.requireNonNull(payloads, "payloads");
        if (keys.length != payloads.length) {
            throw new IllegalArgumentException("key/payload length mismatch");
        }
        radixSort(keys, payloads, keyScratch, payloadScratch, counts, false, monitor);
    }

    //FLLDCIM: FOLD - member IntRadix.sort from donor blob ad3f6f52ef60 (donor blob ad3f6f52ef60 at commit 4a2e6452a7ad (repository, path and digests: m3/algorithms/recipe/radix-lanes-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/radix-lanes-fold/FOLD_MAP.tsv
    private static void radixSort(
            int[] keys,
            int[] payloads,
            int[] keyScratch,
            int[] payloadScratch,
            int[] counts,
            boolean unsigned,
            ProgressMonitor supplied) {
        Objects.requireNonNull(keys, "keys");
        Objects.requireNonNull(keyScratch, "keyScratch");
        Objects.requireNonNull(counts, "counts");
        ProgressMonitor monitor = AlgorithmSupport.nonNull(supplied);
        int length = keys.length;
        if (keyScratch.length < length) {
            throw new IllegalArgumentException("key scratch too small");
        }
        if (counts.length < RADIX) {
            throw new IllegalArgumentException("counts must contain at least 256 cells");
        }
        if (payloads != null) {
            Objects.requireNonNull(payloadScratch, "payloadScratch");
            if (payloadScratch.length < length) {
                throw new IllegalArgumentException("payload scratch too small");
            }
        }

        AlgorithmSupport.checkCanceled(monitor);
        if (length < 2) return;

        int[] sourceKeys = keys;
        int[] targetKeys = keyScratch;
        int[] sourcePayloads = payloads;
        int[] targetPayloads = payloadScratch;

        for (int pass = 0; pass < Integer.BYTES; pass++) {
            AlgorithmSupport.checkCanceled(monitor);
            Arrays.fill(counts, 0, RADIX, 0);
            int shift = pass << 3;
            for (int index = 0; index < length; index++) {
                radixCheckpoint(monitor, index);
                counts[radixDigit(sourceKeys[index], shift, unsigned)]++;
            }

            int offset = 0;
            for (int bucket = 0; bucket < RADIX; bucket++) {
                int frequency = counts[bucket];
                counts[bucket] = offset;
                offset += frequency;
            }

            for (int index = 0; index < length; index++) {
                radixCheckpoint(monitor, index);
                int bucket = radixDigit(sourceKeys[index], shift, unsigned);
                int write = counts[bucket]++;
                targetKeys[write] = sourceKeys[index];
                if (payloads != null) targetPayloads[write] = sourcePayloads[index];
            }

            int[] keySwap = sourceKeys;
            sourceKeys = targetKeys;
            targetKeys = keySwap;
            if (payloads != null) {
                int[] payloadSwap = sourcePayloads;
                sourcePayloads = targetPayloads;
                targetPayloads = payloadSwap;
            }
        }

        if (sourceKeys != keys) {
            System.arraycopy(sourceKeys, 0, keys, 0, length);
            if (payloads != null) {
                System.arraycopy(sourcePayloads, 0, payloads, 0, length);
            }
        }
        AlgorithmSupport.checkCanceled(monitor);
    }

    //FLLDCIM: FOLD - member IntRadix.digit from donor blob ad3f6f52ef60 (donor blob ad3f6f52ef60 at commit 4a2e6452a7ad (repository, path and digests: m3/algorithms/recipe/radix-lanes-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/radix-lanes-fold/FOLD_MAP.tsv
    private static int radixDigit(int key, int shift, boolean unsigned) {
        int ordered = unsigned ? key : key ^ Integer.MIN_VALUE;
        return (ordered >>> shift) & 0xff;
    }

    //FLLDCIM: FOLD - member IntRadix.checkpoint from donor blob ad3f6f52ef60 (donor blob ad3f6f52ef60 at commit 4a2e6452a7ad (repository, path and digests: m3/algorithms/recipe/radix-lanes-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/radix-lanes-fold/FOLD_MAP.tsv
    private static void radixCheckpoint(ProgressMonitor monitor, int ordinal) {
        if ((ordinal & 1023) == 0) AlgorithmSupport.checkCanceled(monitor);
    }
    //FLLDCIM: FOLD - member LongRadix.sortSigned from donor blob 5734be7223b0 (donor blob 5734be7223b0 at commit 4a2e6452a7ad (repository, path and digests: m3/algorithms/recipe/radix-lanes-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/radix-lanes-fold/FOLD_MAP.tsv
    public static void radixSortSigned(long[] values) {
        Objects.requireNonNull(values, "values");
        radixSortSigned(values, new long[values.length], new int[RADIX]);
    }

    //FLLDCIM: FOLD - member LongRadix.sortUnsigned from donor blob 5734be7223b0 (donor blob 5734be7223b0 at commit 4a2e6452a7ad (repository, path and digests: m3/algorithms/recipe/radix-lanes-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/radix-lanes-fold/FOLD_MAP.tsv
    public static void radixSortUnsigned(long[] values) {
        Objects.requireNonNull(values, "values");
        radixSortUnsigned(values, new long[values.length], new int[RADIX]);
    }

    //FLLDCIM: FOLD - member LongRadix.sortSigned from donor blob 5734be7223b0 (donor blob 5734be7223b0 at commit 4a2e6452a7ad (repository, path and digests: m3/algorithms/recipe/radix-lanes-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/radix-lanes-fold/FOLD_MAP.tsv
    public static void radixSortSigned(long[] values, long[] scratch, int[] counts) {
        radixSortSigned(values, scratch, counts, ProgressMonitor.none());
    }

    //FLLDCIM: FOLD - member LongRadix.sortSigned from donor blob 5734be7223b0 (donor blob 5734be7223b0 at commit 4a2e6452a7ad (repository, path and digests: m3/algorithms/recipe/radix-lanes-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/radix-lanes-fold/FOLD_MAP.tsv
    public static void radixSortSigned(
            long[] values,
            long[] scratch,
            int[] counts,
            ProgressMonitor monitor) {
        radixSort(values, null, scratch, null, counts, false, monitor);
    }

    //FLLDCIM: FOLD - member LongRadix.sortUnsigned from donor blob 5734be7223b0 (donor blob 5734be7223b0 at commit 4a2e6452a7ad (repository, path and digests: m3/algorithms/recipe/radix-lanes-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/radix-lanes-fold/FOLD_MAP.tsv
    public static void radixSortUnsigned(long[] values, long[] scratch, int[] counts) {
        radixSortUnsigned(values, scratch, counts, ProgressMonitor.none());
    }

    //FLLDCIM: FOLD - member LongRadix.sortUnsigned from donor blob 5734be7223b0 (donor blob 5734be7223b0 at commit 4a2e6452a7ad (repository, path and digests: m3/algorithms/recipe/radix-lanes-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/radix-lanes-fold/FOLD_MAP.tsv
    public static void radixSortUnsigned(
            long[] values,
            long[] scratch,
            int[] counts,
            ProgressMonitor monitor) {
        radixSort(values, null, scratch, null, counts, true, monitor);
    }

    //FLLDCIM: FOLD - member LongRadix.sortPairsSigned from donor blob 5734be7223b0 (donor blob 5734be7223b0 at commit 4a2e6452a7ad (repository, path and digests: m3/algorithms/recipe/radix-lanes-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/radix-lanes-fold/FOLD_MAP.tsv
    /**
     * Stable signed-key pair sort. Payload order is preserved for equal keys.
     */
    public static void radixSortPairsSigned(
            long[] keys,
            long[] payloads,
            long[] keyScratch,
            long[] payloadScratch,
            int[] counts) {
        Objects.requireNonNull(payloads, "payloads");
        if (keys.length != payloads.length) {
            throw new IllegalArgumentException("key/payload length mismatch");
        }
        radixSort(
                keys,
                payloads,
                keyScratch,
                payloadScratch,
                counts,
                false,
                ProgressMonitor.none());
    }

    //FLLDCIM: FOLD - member LongRadix.sortPairsSigned from donor blob 5734be7223b0 (donor blob 5734be7223b0 at commit 4a2e6452a7ad (repository, path and digests: m3/algorithms/recipe/radix-lanes-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/radix-lanes-fold/FOLD_MAP.tsv
    /**
     * Stable signed-key / int-payload pair sort.
     *
     * <p>The payload is moved with its key and therefore preserves original payload order for
     * equal keys. The monitor overload is a cancellation boundary only; it does not begin/end a
     * task or emit per-element progress updates.</p>
     */
    public static void radixSortPairsSigned(long[] keys, int[] payloads) {
        Objects.requireNonNull(keys, "keys");
        Objects.requireNonNull(payloads, "payloads");
        radixSortPairsSigned(
                keys,
                payloads,
                new long[keys.length],
                new int[payloads.length],
                new int[RADIX],
                ProgressMonitor.none());
    }

    //FLLDCIM: FOLD - member LongRadix.sortPairsSigned from donor blob 5734be7223b0 (donor blob 5734be7223b0 at commit 4a2e6452a7ad (repository, path and digests: m3/algorithms/recipe/radix-lanes-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/radix-lanes-fold/FOLD_MAP.tsv
    public static void radixSortPairsSigned(
            long[] keys,
            int[] payloads,
            long[] keyScratch,
            int[] payloadScratch,
            int[] counts) {
        radixSortPairsSigned(
                keys,
                payloads,
                keyScratch,
                payloadScratch,
                counts,
                ProgressMonitor.none());
    }

    //FLLDCIM: FOLD - member LongRadix.sortPairsSigned from donor blob 5734be7223b0 (donor blob 5734be7223b0 at commit 4a2e6452a7ad (repository, path and digests: m3/algorithms/recipe/radix-lanes-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/radix-lanes-fold/FOLD_MAP.tsv
    public static void radixSortPairsSigned(
            long[] keys,
            int[] payloads,
            long[] keyScratch,
            int[] payloadScratch,
            int[] counts,
            ProgressMonitor supplied) {
        radixSortPairs(keys, payloads, keyScratch, payloadScratch, counts, false, supplied);
    }

    //FLLDCIM: FOLD - member LongRadix.sortPairsUnsigned from donor blob 5734be7223b0 (donor blob 5734be7223b0 at commit 4a2e6452a7ad (repository, path and digests: m3/algorithms/recipe/radix-lanes-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/radix-lanes-fold/FOLD_MAP.tsv
    /** Stable unsigned-key / int-payload pair sort; equal keys retain payload order. */
    public static void radixSortPairsUnsigned(long[] keys, int[] payloads) {
        Objects.requireNonNull(keys, "keys");
        Objects.requireNonNull(payloads, "payloads");
        radixSortPairsUnsigned(
                keys,
                payloads,
                new long[keys.length],
                new int[payloads.length],
                new int[RADIX],
                ProgressMonitor.none());
    }

    //FLLDCIM: FOLD - member LongRadix.sortPairsUnsigned from donor blob 5734be7223b0 (donor blob 5734be7223b0 at commit 4a2e6452a7ad (repository, path and digests: m3/algorithms/recipe/radix-lanes-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/radix-lanes-fold/FOLD_MAP.tsv
    public static void radixSortPairsUnsigned(
            long[] keys,
            int[] payloads,
            long[] keyScratch,
            int[] payloadScratch,
            int[] counts,
            ProgressMonitor supplied) {
        radixSortPairs(keys, payloads, keyScratch, payloadScratch, counts, true, supplied);
    }

    //FLLDCIM: FOLD - member LongRadix.sortPairs from donor blob 5734be7223b0 (donor blob 5734be7223b0 at commit 4a2e6452a7ad (repository, path and digests: m3/algorithms/recipe/radix-lanes-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/radix-lanes-fold/FOLD_MAP.tsv
    private static void radixSortPairs(
            long[] keys,
            int[] payloads,
            long[] keyScratch,
            int[] payloadScratch,
            int[] counts,
            boolean unsigned,
            ProgressMonitor supplied) {
        Objects.requireNonNull(keys, "keys");
        Objects.requireNonNull(payloads, "payloads");
        Objects.requireNonNull(keyScratch, "keyScratch");
        Objects.requireNonNull(payloadScratch, "payloadScratch");
        Objects.requireNonNull(counts, "counts");
        ProgressMonitor monitor = AlgorithmSupport.nonNull(supplied);

        int length = keys.length;
        if (payloads.length != length) {
            throw new IllegalArgumentException("key/payload length mismatch");
        }
        if (keyScratch.length < length) {
            throw new IllegalArgumentException("key scratch too small");
        }
        if (payloadScratch.length < length) {
            throw new IllegalArgumentException("payload scratch too small");
        }
        if (counts.length < RADIX) {
            throw new IllegalArgumentException("counts must contain at least 256 cells");
        }

        AlgorithmSupport.checkCanceled(monitor);
        if (length < 2) return;

        long[] sourceKeys = keys;
        long[] targetKeys = keyScratch;
        int[] sourcePayloads = payloads;
        int[] targetPayloads = payloadScratch;

        for (int pass = 0; pass < Long.BYTES; pass++) {
            AlgorithmSupport.checkCanceled(monitor);
            Arrays.fill(counts, 0, RADIX, 0);
            int shift = pass << 3;
            for (int index = 0; index < length; index++) {
                radixCheckpoint(monitor, index);
                counts[radixDigit(sourceKeys[index], shift, unsigned)]++;
            }

            int offset = 0;
            for (int bucket = 0; bucket < RADIX; bucket++) {
                int frequency = counts[bucket];
                counts[bucket] = offset;
                offset += frequency;
            }

            for (int index = 0; index < length; index++) {
                radixCheckpoint(monitor, index);
                int bucket = radixDigit(sourceKeys[index], shift, unsigned);
                int write = counts[bucket]++;
                targetKeys[write] = sourceKeys[index];
                targetPayloads[write] = sourcePayloads[index];
            }

            long[] keySwap = sourceKeys;
            sourceKeys = targetKeys;
            targetKeys = keySwap;

            int[] payloadSwap = sourcePayloads;
            sourcePayloads = targetPayloads;
            targetPayloads = payloadSwap;
        }

        if (sourceKeys != keys) {
            System.arraycopy(sourceKeys, 0, keys, 0, length);
            System.arraycopy(sourcePayloads, 0, payloads, 0, length);
        }
        AlgorithmSupport.checkCanceled(monitor);
    }

    //FLLDCIM: FOLD - member LongRadix.sort from donor blob 5734be7223b0 (donor blob 5734be7223b0 at commit 4a2e6452a7ad (repository, path and digests: m3/algorithms/recipe/radix-lanes-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/radix-lanes-fold/FOLD_MAP.tsv
    private static void radixSort(
            long[] keys,
            long[] payloads,
            long[] keyScratch,
            long[] payloadScratch,
            int[] counts,
            boolean unsigned,
            ProgressMonitor supplied) {
        Objects.requireNonNull(keys, "keys");
        Objects.requireNonNull(keyScratch, "keyScratch");
        Objects.requireNonNull(counts, "counts");
        ProgressMonitor monitor = AlgorithmSupport.nonNull(supplied);
        int length = keys.length;
        if (keyScratch.length < length) {
            throw new IllegalArgumentException("key scratch too small");
        }
        if (counts.length < RADIX) {
            throw new IllegalArgumentException("counts must contain at least 256 cells");
        }
        if (payloads != null) {
            Objects.requireNonNull(payloadScratch, "payloadScratch");
            if (payloadScratch.length < length) {
                throw new IllegalArgumentException("payload scratch too small");
            }
        }
        AlgorithmSupport.checkCanceled(monitor);
        if (length < 2) {
            return;
        }

        long[] sourceKeys = keys;
        long[] targetKeys = keyScratch;
        long[] sourcePayloads = payloads;
        long[] targetPayloads = payloadScratch;

        for (int pass = 0; pass < Long.BYTES; pass++) {
            AlgorithmSupport.checkCanceled(monitor);
            Arrays.fill(counts, 0, RADIX, 0);
            int shift = pass << 3;
            for (int i = 0; i < length; i++) {
                radixCheckpoint(monitor, i);
                counts[radixDigit(sourceKeys[i], shift, unsigned)]++;
            }
            int offset = 0;
            for (int bucket = 0; bucket < RADIX; bucket++) {
                int frequency = counts[bucket];
                counts[bucket] = offset;
                offset += frequency;
            }
            for (int i = 0; i < length; i++) {
                radixCheckpoint(monitor, i);
                int bucket = radixDigit(sourceKeys[i], shift, unsigned);
                int write = counts[bucket]++;
                targetKeys[write] = sourceKeys[i];
                if (payloads != null) {
                    targetPayloads[write] = sourcePayloads[i];
                }
            }

            long[] keySwap = sourceKeys;
            sourceKeys = targetKeys;
            targetKeys = keySwap;
            if (payloads != null) {
                long[] payloadSwap = sourcePayloads;
                sourcePayloads = targetPayloads;
                targetPayloads = payloadSwap;
            }
        }

        // Eight passes are even, therefore ordinary workspaces finish in the original arrays.
        if (sourceKeys != keys) {
            System.arraycopy(sourceKeys, 0, keys, 0, length);
            if (payloads != null) {
                System.arraycopy(sourcePayloads, 0, payloads, 0, length);
            }
        }
        AlgorithmSupport.checkCanceled(monitor);
    }

    //FLLDCIM: FOLD - member LongRadix.digit from donor blob 5734be7223b0 (donor blob 5734be7223b0 at commit 4a2e6452a7ad (repository, path and digests: m3/algorithms/recipe/radix-lanes-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/radix-lanes-fold/FOLD_MAP.tsv
    private static int radixDigit(long key, int shift, boolean unsigned) {
        long ordered = unsigned ? key : key ^ Long.MIN_VALUE;
        return (int) (ordered >>> shift) & 0xff;
    }
}
