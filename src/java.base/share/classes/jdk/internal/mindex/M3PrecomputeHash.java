/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 */
package jdk.internal.mindex;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Internal deterministic hashing for M3 precompute images. */
final class M3PrecomputeHash {
    private M3PrecomputeHash() {}

    static String sha256(String domain, String... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] domainBytes = domain.getBytes(StandardCharsets.UTF_8);
            digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(domainBytes.length).array());
            digest.update(domainBytes);
            for (String value : values) {
                byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    static String sha256(String domain, long[] longs, int[] ints) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(domain.getBytes(StandardCharsets.UTF_8));
            ByteBuffer lb = ByteBuffer.allocate(Long.BYTES);
            for (long value : longs) {
                lb.clear();
                lb.putLong(value);
                digest.update(lb.array());
            }
            ByteBuffer ib = ByteBuffer.allocate(Integer.BYTES);
            for (int value : ints) {
                ib.clear();
                ib.putInt(value);
                digest.update(ib.array());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
