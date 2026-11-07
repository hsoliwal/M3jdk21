// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/**
 * Object-free dense UUID column: exactly two long[] payload columns.
 *
 * <p>Payload is 16 bytes per UUID instead of retaining one UUID/String object per occurrence.
 * A consumer materializes UUID/String only at an explicit boundary.</p>
 */
//FLLDCIM: RELOCATE - donor blob 8eace5c30c50 at commit 3385d79a2968 (repository, path, URL and digests: m3/collections/recipe/immutable-pools-20261106/PROVENANCE.tsv); recipe m3/collections/recipe/immutable-pools-20261106/relocate.py v2
public final class M3Uuid128Vector {
    private final long[] most;
    private final long[] least;

    private M3Uuid128Vector(long[] most, long[] least, boolean trusted) {
        if (most.length != least.length) throw new IllegalArgumentException("UUID column shape mismatch");
        this.most = trusted ? most : most.clone();
        this.least = trusted ? least : least.clone();
    }

    public static M3Uuid128Vector copyOf(long[] most, long[] least) {
        Objects.requireNonNull(most, "most");
        Objects.requireNonNull(least, "least");
        return new M3Uuid128Vector(most, least, false);
    }

    public int size() { return most.length; }
    public long mostSignificantBitsAt(int index) { return most[index]; }
    public long leastSignificantBitsAt(int index) { return least[index]; }

    public M3Uuid128 get(int index) {
        return new M3Uuid128(most[index], least[index]);
    }

    public UUID toUuid(int index) {
        return new UUID(most[index], least[index]);
    }

    public boolean equalsAt(int index, M3Uuid128 value) {
        Objects.requireNonNull(value, "value");
        return most[index] == value.mostSignificantBits()
            && least[index] == value.leastSignificantBits();
    }

    public int indexOf(M3Uuid128 value) {
        Objects.requireNonNull(value, "value");
        long high = value.mostSignificantBits();
        long low = value.leastSignificantBits();
        for (int i = 0; i < most.length; i++) {
            if (most[i] == high && least[i] == low) return i;
        }
        return -1;
    }

    public void writeBigEndian(int index, byte[] target, int offset) {
        get(index).writeBigEndian(target, offset);
    }

    public void writeBigEndian(int index, ByteBuffer target) {
        get(index).writeBigEndian(target);
    }

    public long payloadBytes() {
        return Math.multiplyExact((long) size(), M3Uuid128.BINARY_BYTES);
    }

    public static Builder builder(int initialCapacity) {
        return new Builder(initialCapacity);
    }

    public static final class Builder {
        private long[] most;
        private long[] least;
        private int size;

        public Builder(int initialCapacity) {
            if (initialCapacity < 0) throw new IllegalArgumentException("negative capacity");
            int capacity = Math.max(1, initialCapacity);
            most = new long[capacity];
            least = new long[capacity];
        }

        public Builder add(M3Uuid128 value) {
            Objects.requireNonNull(value, "value");
            return add(value.mostSignificantBits(), value.leastSignificantBits());
        }

        public Builder add(UUID value) {
            Objects.requireNonNull(value, "value");
            return add(value.getMostSignificantBits(), value.getLeastSignificantBits());
        }

        public Builder add(long mostSignificantBits, long leastSignificantBits) {
            ensure(size + 1);
            most[size] = mostSignificantBits;
            least[size] = leastSignificantBits;
            size++;
            return this;
        }

        public M3Uuid128Vector build() {
            return new M3Uuid128Vector(
                Arrays.copyOf(most, size),
                Arrays.copyOf(least, size),
                true);
        }

        private void ensure(int required) {
            if (required <= most.length) return;
            int next = Math.max(required, most.length << 1);
            most = Arrays.copyOf(most, next);
            least = Arrays.copyOf(least, next);
        }
    }
}
