// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.Arrays;
import java.util.Objects;

/** One named immutable SHA-256 fact derived from an exact M3File stream generation. */
public final class M3FileFact implements Comparable<M3FileFact> {
    public static final int SHA256_BYTES = 32;

    private final String name;
    private final byte[] digest;

    public M3FileFact(String name, byte[] sha256) {
        String checked = Objects.requireNonNull(name, "name").strip();
        if (checked.isEmpty()
                || checked.length() > 160
                || checked.indexOf('\0') >= 0
                || !checked.matches("[A-Za-z0-9][A-Za-z0-9._:/+-]*")) {
            throw new IllegalArgumentException("invalid M3 file fact name");
        }
        byte[] bytes = Objects.requireNonNull(sha256, "sha256").clone();
        if (bytes.length != SHA256_BYTES) {
            throw new IllegalArgumentException("M3 file fact requires SHA-256");
        }
        this.name = checked;
        this.digest = bytes;
    }

    public String name() {
        return name;
    }

    public byte[] sha256() {
        return digest.clone();
    }

    boolean sameDigest(M3FileFact other) {
        return Arrays.equals(digest, other.digest);
    }

    void copyDigest(byte[] target, int offset) {
        Objects.checkFromIndexSize(offset, SHA256_BYTES, target.length);
        System.arraycopy(digest, 0, target, offset, SHA256_BYTES);
    }

    @Override public int compareTo(M3FileFact other) {
        return name.compareTo(Objects.requireNonNull(other, "other").name);
    }

    @Override public boolean equals(Object other) {
        return other instanceof M3FileFact fact
                && name.equals(fact.name)
                && Arrays.equals(digest, fact.digest);
    }

    @Override public int hashCode() {
        return 31 * name.hashCode() + Arrays.hashCode(digest);
    }

    @Override public String toString() {
        return name;
    }
}
