// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import jdk.internal.m3.collections.M3PoolProgressMonitor;
import java.util.List;
import java.util.Objects;

/**
 * Primitive structure-of-arrays projection table with transposed 64-bit feature planes.
 *
 * <p>The feature plane for bit N is a bitmap of every object whose projected signal contains N.
 * Querying required/forbidden signal bits is therefore AND/AND-NOT over 64 objects per machine
 * word before any exact object operation is attempted.</p>
 */
//FLLDCIM: RELOCATE - donor blob cf5b38a4654a at commit 3385d79a2968 (repository, path, URL and digests: m3/collections/qualification/object-signal-20261107/PROVENANCE.tsv); recipe m3/collections/qualification/object-signal-20261107/relocate.py v1
public final class M3ObjectSignalTable {
    private static final int FEATURE_BITS = Long.SIZE;

    private final int size;
    private final int wordCount;
    private final byte[] kind;
    private final int[] domain;
    private final int[] length;
    private final int[] flags;
    private final long[] exactHash;
    private final long[] rawIdentity;
    private final long[] featurePlanes;

    private M3ObjectSignalTable(
            int size,
            byte[] kind,
            int[] domain,
            int[] length,
            int[] flags,
            long[] exactHash,
            long[] rawIdentity,
            long[] featurePlanes) {
        this.size = size;
        this.wordCount = (size + 63) >>> 6;
        this.kind = kind;
        this.domain = domain;
        this.length = length;
        this.flags = flags;
        this.exactHash = exactHash;
        this.rawIdentity = rawIdentity;
        this.featurePlanes = featurePlanes;
    }

    public static <T> M3ObjectSignalTable build(
            List<? extends T> values,
            M3ObjectProjector<? super T> projector,
            M3PoolProgressMonitor monitor) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(projector, "projector");
        Objects.requireNonNull(monitor, "monitor");

        int n = values.size();
        int words = (n + 63) >>> 6;
        byte[] kinds = new byte[n];
        int[] domains = new int[n];
        int[] lengths = new int[n];
        int[] flags = new int[n];
        long[] hashes = new long[n];
        long[] identities = new long[n];
        long[] planes = new long[Math.multiplyExact(FEATURE_BITS, words)];

        monitor.begin("Project objects to primitive signals", n);
        try {
            for (int index = 0; index < n; index++) {
                monitor.checkCancelled();
                M3ObjectSignal signal = Objects.requireNonNull(
                    projector.project(values.get(index), child(monitor)), "projected signal");
                kinds[index] = (byte) signal.kind().ordinal();
                domains[index] = signal.domain();
                lengths[index] = signal.logicalLength();
                flags[index] = signal.flags();
                hashes[index] = signal.exactHash64();
                identities[index] = signal.rawIdentity();

                long features = signal.featureBits();
                int word = index >>> 6;
                long rowBit = 1L << (index & 63);
                while (features != 0L) {
                    int feature = Long.numberOfTrailingZeros(features);
                    planes[feature * words + word] |= rowBit;
                    features &= features - 1L;
                }
                monitor.worked(1);
            }
        } finally {
            monitor.done();
        }
        return new M3ObjectSignalTable(n, kinds, domains, lengths, flags, hashes, identities, planes);
    }

    public int size() { return size; }

    public M3ObjectSignal signalAt(int index) {
        checkIndex(index);
        return new M3ObjectSignal(
            M3ObjectProjectionKind.values()[kind[index]],
            domain[index],
            length[index],
            exactHash[index],
            featureBitsAt(index),
            rawIdentity[index],
            flags[index]);
    }

    public long featureBitsAt(int index) {
        checkIndex(index);
        long bits = 0L;
        int word = index >>> 6;
        long rowBit = 1L << (index & 63);
        for (int feature = 0; feature < FEATURE_BITS; feature++) {
            if ((featurePlanes[feature * wordCount + word] & rowBit) != 0L) {
                bits |= 1L << feature;
            }
        }
        return bits;
    }

    public M3ObjectSignalFirstPassResult firstPass(M3ObjectSignalQuery query, M3PoolProgressMonitor monitor) {
        return firstPass(query, M3ObjectSignalBitmap.all(size), monitor);
    }

    public M3ObjectSignalFirstPassResult firstPass(
            M3ObjectSignalQuery query,
            M3ObjectSignalBitmap initialCandidates,
            M3PoolProgressMonitor monitor) {
        Objects.requireNonNull(query, "query");
        Objects.requireNonNull(initialCandidates, "initialCandidates");
        Objects.requireNonNull(monitor, "monitor");
        if (initialCandidates.size() != size) throw new IllegalArgumentException("candidate size mismatch");

        long[] selected = initialCandidates.copyWords();
        int initial = initialCandidates.cardinality();

        monitor.begin("Run bitwise object-signal first pass", Math.max(1, wordCount));
        try {
            M3ObjectSignalOps.applyFeatureMasks(
                featurePlanes,
                wordCount,
                query.requiredBits(),
                query.forbiddenBits(),
                selected);
            int afterBits = cardinality(selected);

            for (int wordIndex = 0; wordIndex < selected.length; wordIndex++) {
                monitor.checkCancelled();
                long bits = selected[wordIndex];
                while (bits != 0L) {
                    int bit = Long.numberOfTrailingZeros(bits);
                    int index = (wordIndex << 6) + bit;
                    if (index >= size) break;
                    if (!query.metadataMatches(
                            kind[index],
                            domain[index],
                            length[index],
                            flags[index],
                            exactHash[index],
                            rawIdentity[index])) {
                        selected[wordIndex] &= ~(1L << bit);
                    }
                    bits &= bits - 1L;
                }
                monitor.worked(1);
            }

            int afterMetadata = cardinality(selected);
            return new M3ObjectSignalFirstPassResult(
                new M3ObjectSignalBitmap(selected, size, true),
                initial,
                afterBits,
                afterMetadata);
        } finally {
            monitor.done();
        }
    }

    public long primitivePayloadBytes() {
        return (long) kind.length
            + (long) domain.length * Integer.BYTES
            + (long) length.length * Integer.BYTES
            + (long) flags.length * Integer.BYTES
            + (long) exactHash.length * Long.BYTES
            + (long) rawIdentity.length * Long.BYTES
            + (long) featurePlanes.length * Long.BYTES;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException(index);
    }

    private static int cardinality(long[] words) {
        int total = 0;
        for (long word : words) total = Math.addExact(total, Long.bitCount(word));
        return total;
    }

    private static M3PoolProgressMonitor child(M3PoolProgressMonitor parent) {
        return new M3PoolProgressMonitor() {
            @Override public boolean cancelled() { return parent.cancelled(); }
        };
    }
}
