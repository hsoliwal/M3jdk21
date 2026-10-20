// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Immutable sorted fact table with one String[] name lane and one flat SHA-256 byte lane.
 *
 * <p>No Map.Entry objects are retained. Lookup is binary search; deterministic ordering makes the
 * table suitable for stable delta generation and later content-addressed serialization.</p>
 */
public final class M3FileFacts {
    private static final M3FileFacts EMPTY = new M3FileFacts(new String[0], new byte[0]);

    private final String[] names;
    private final byte[] digests;

    private M3FileFacts(String[] names, byte[] digests) {
        this.names = names;
        this.digests = digests;
    }

    public static M3FileFacts empty() {
        return EMPTY;
    }

    public static M3FileFacts of(M3FileFact... facts) {
        Objects.requireNonNull(facts, "facts");
        if (facts.length == 0) return EMPTY;

        M3FileFact[] sorted = facts.clone();
        Arrays.sort(sorted);
        String[] names = new String[sorted.length];
        byte[] digests = new byte[Math.multiplyExact(sorted.length, M3FileFact.SHA256_BYTES)];
        for (int index = 0; index < sorted.length; index++) {
            M3FileFact fact = Objects.requireNonNull(sorted[index], "fact");
            if (index > 0 && fact.name().equals(names[index - 1])) {
                if (!fact.sameDigest(sorted[index - 1])) {
                    throw new IllegalArgumentException("conflicting duplicate M3 file fact: " + fact.name());
                }
                throw new IllegalArgumentException("duplicate M3 file fact: " + fact.name());
            }
            names[index] = fact.name();
            fact.copyDigest(digests, index * M3FileFact.SHA256_BYTES);
        }
        return new M3FileFacts(names, digests);
    }

    public static M3FileFacts of(List<M3FileFact> facts) {
        Objects.requireNonNull(facts, "facts");
        return of(facts.toArray(M3FileFact[]::new));
    }

    public int size() {
        return names.length;
    }

    public boolean isEmpty() {
        return names.length == 0;
    }

    public String nameAt(int index) {
        return names[Objects.checkIndex(index, names.length)];
    }

    public byte[] sha256At(int index) {
        int checked = Objects.checkIndex(index, names.length);
        return Arrays.copyOfRange(
                digests,
                checked * M3FileFact.SHA256_BYTES,
                (checked + 1) * M3FileFact.SHA256_BYTES);
    }

    public int indexOf(String name) {
        return Arrays.binarySearch(names, Objects.requireNonNull(name, "name"));
    }

    public boolean contains(String name) {
        return indexOf(name) >= 0;
    }

    public byte[] requireSha256(String name) {
        int index = indexOf(name);
        if (index < 0) throw new IllegalArgumentException("unknown M3 file fact: " + name);
        return sha256At(index);
    }

    boolean sameDigest(int leftIndex, M3FileFacts right, int rightIndex) {
        int left = leftIndex * M3FileFact.SHA256_BYTES;
        int other = rightIndex * M3FileFact.SHA256_BYTES;
        return Arrays.equals(
                digests, left, left + M3FileFact.SHA256_BYTES,
                right.digests, other, other + M3FileFact.SHA256_BYTES);
    }

    public M3FileFactDelta deltaTo(M3FileFacts next) {
        return M3FileFactDelta.between(this, Objects.requireNonNull(next, "next"));
    }
}
