// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/** {@link MessageDigest} shadow; the digest is available after {@link #finish()}. */
public final class M3FileDigestShadow implements M3FileShadow {
    private final MessageDigest digest;
    private byte[] result;

    M3FileDigestShadow(String algorithm) {
        try {
            digest = MessageDigest.getInstance(Objects.requireNonNull(algorithm, "algorithm"));
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalArgumentException("digest unavailable: " + algorithm, unavailable);
        }
    }

    @Override
    public void update(byte[] bytes, int offset, int length) {
        if (result != null) throw new IllegalStateException("digest already finished");
        digest.update(bytes, offset, length);
    }

    @Override
    public void finish() {
        if (result == null) result = digest.digest();
    }

    public boolean finished() {
        return result != null;
    }

    public byte[] bytes() {
        return finishedResult().clone();
    }

    /** Big-endian 64-bit word {@code index} of the digest. */
    public long word(int index) {
        byte[] bytes = finishedResult();
        Objects.checkIndex(index, bytes.length / Long.BYTES);
        long word = 0;
        for (int i = 0; i < Long.BYTES; i++) word = (word << 8) | (bytes[index * Long.BYTES + i] & 0xffL);
        return word;
    }

    public String hex() {
        return HexFormat.of().formatHex(finishedResult());
    }

    private byte[] finishedResult() {
        if (result == null) throw new IllegalStateException("main stream has not ended");
        return result;
    }
}
