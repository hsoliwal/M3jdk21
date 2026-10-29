// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.tools.cpu;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/** Domain-separated SHA-256 chain for packed event/source bytes and deterministic replay.
 * Not a persisted WAL, cryptographic signature, trusted timestamp, or transactional database.
 */
public final class M3ProvenanceLedger {
    private M3ProvenanceLedger() { }
    private static final byte[] DOMAIN = "M3-PROVENANCE-V1".getBytes(StandardCharsets.US_ASCII);
    private static final int WIDTH = 32;

    public record Chain(byte[] entries, byte[] root) {
        public Chain { entries = entries.clone(); root = root.clone(); }
        @Override public byte[] entries() { return entries.clone(); }
        @Override public byte[] root() { return root.clone(); }
    }

    /** No per-event objects: offsets delimit packed immutable bytes. Hashes are 32-byte lanes. */
    public static Chain chain(byte[] packed, int[] offsets) {
        Objects.requireNonNull(packed); Objects.requireNonNull(offsets);
        if (offsets.length == 0 || offsets[0] != 0 || offsets[offsets.length - 1] != packed.length)
            throw new IllegalArgumentException("event offsets");
        for (int i = 1; i < offsets.length; i++)
            if (offsets[i] < offsets[i - 1] || offsets[i] > packed.length)
                throw new IllegalArgumentException("nonmonotone event offsets");
        int records = offsets.length - 1;
        if (records > 1_000_000) throw new IllegalArgumentException("journal budget");
        byte[] proofs = new byte[Math.multiplyExact(records, WIDTH)];
        byte[] previous = new byte[WIDTH];
        MessageDigest digest = digest();
        for (int i = 0; i < records; i++) {
            digest.reset(); digest.update(DOMAIN); digest.update(previous);
            int size = offsets[i + 1] - offsets[i];
            for (int shift = 24; shift >= 0; shift -= 8) digest.update((byte) (size >>> shift));
            digest.update(packed, offsets[i], size);
            previous = digest.digest();
            System.arraycopy(previous, 0, proofs, i * WIDTH, WIDTH);
        }
        return new Chain(proofs, previous);
    }

    public static boolean verify(byte[] packed, int[] offsets, byte[] expectedRoot) {
        Objects.requireNonNull(expectedRoot);
        if (expectedRoot.length != WIDTH) throw new IllegalArgumentException("SHA-256 root width");
        return MessageDigest.isEqual(chain(packed, offsets).root(), expectedRoot);
    }

    /** Proof that a prefix was unchanged: compare a complete digest for that prefix. */
    public static byte[] prefixRoot(byte[] packed, int[] offsets, int records) {
        Objects.requireNonNull(offsets);
        if (records < 0 || records >= offsets.length) throw new IllegalArgumentException("prefix boundary");
        if (packed == null || offsets[0] != 0 || offsets[records] < 0 || offsets[records] > packed.length)
            throw new IllegalArgumentException("prefix geometry");
        return chain(Arrays.copyOf(packed, offsets[records]),
                Arrays.copyOf(offsets, records + 1)).root();
    }

    private static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
