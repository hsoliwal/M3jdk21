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
package jdk.internal.m3.collections.mat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.function.LongConsumer;

/**
 * Immutable exact pool of canonical values. Frozen state retains no domain object per value:
 * only one byte arena, offsets, hashes, and a primitive open-addressed index.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatPool.java}
 * blob {@code b833ef35d4709747e72d6c2f1c757c97d2186036} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatPool<T> {
    private final MatCodec<T> codec;
    private final MatLimits limits;
    private final byte[] arena;
    private final int[] offsets;
    private final long[] hashes64;
    private final int[] slots;

    private MatPool(
            MatCodec<T> codec,
            MatLimits limits,
            byte[] arena,
            int[] offsets,
            long[] hashes64) {
        this.codec = Objects.requireNonNull(codec, "codec");
        this.limits = Objects.requireNonNull(limits, "limits");
        this.arena = Objects.requireNonNull(arena, "arena");
        this.offsets = Objects.requireNonNull(offsets, "offsets");
        this.hashes64 = Objects.requireNonNull(hashes64, "hashes64");
        validateFrozen(arena, offsets, hashes64, limits);
        this.slots = buildSlots(hashes64);
    }

    /** Compatibility path: existing callers retain the original effectively-unbounded contract. */
    public static <T> Builder<T> builder(MatCodec<T> codec) {
        return new Builder<>(codec, MatLimits.UNBOUNDED);
    }

    /** Preferred production path with explicit finite admission/retention limits. */
    public static <T> Builder<T> builder(MatCodec<T> codec, MatLimits limits) {
        return new Builder<>(codec, limits);
    }

    public MatCodec<T> codec() { return codec; }
    public MatLimits limits() { return limits; }
    public int size() { return offsets.length - 1; }

    public long payloadBytes() {
        return arena.length + 4L * offsets.length + 8L * hashes64.length + 4L * slots.length;
    }

    public MatRef<T> ref(int id) {
        checkId(id);
        return new MatRef<>(this, id);
    }

    public T value(int id) { return codec.decode(bytes(id)); }

    public byte[] bytes(int id) {
        checkId(id);
        return Arrays.copyOfRange(arena, offsets[id], offsets[id + 1]);
    }

    public int encodedLength(int id) {
        checkId(id);
        return offsets[id + 1] - offsets[id];
    }

    public long contentHash64(int id) {
        checkId(id);
        return hashes64[id];
    }

    /** Expected O(1), with exact canonical-byte confirmation after every hash match. */
    public int idOf(T value) { return idOfBytes(codec.encode(value)); }

    public int idOfBytes(byte[] canonical) {
        Objects.requireNonNull(canonical, "canonical");
        limits.valueBytes(canonical.length);
        long hash = MatHash.fnv64(canonical);
        int mask = slots.length - 1;
        int slot = MatHash.mix(MatHash.fold32(hash)) & mask;
        for (int probe = 0; probe < slots.length; probe++) {
            int stored = slots[slot];
            if (stored == 0) return -1;
            int id = stored - 1;
            if (hashes64[id] == hash && equalBytes(id, canonical)) return id;
            slot = (slot + 1) & mask;
        }
        return -1;
    }

    public boolean contains(T value) { return idOf(value) >= 0; }

    public MatPool<T> extend(Iterable<? extends T> additions) {
        return extend(additions, () -> false, ignored -> { });
    }

    /** Cancellable/observable extension using JDK callbacks; adapters can delegate to Synexia Progress. */
    public MatPool<T> extend(
            Iterable<? extends T> additions, BooleanSupplier canceled, LongConsumer worked) {
        Objects.requireNonNull(additions, "additions");
        Objects.requireNonNull(canceled, "canceled");
        Objects.requireNonNull(worked, "worked");
        ArrayList<byte[]> fresh = new ArrayList<>();
        long inputCount = 0;
        long stagedBytes = 0;
        for (T value : additions) {
            if (canceled.getAsBoolean()) {
                throw new CancellationException("MAT pool extension canceled");
            }
            limits.inputValues(++inputCount);
            byte[] encoded = codec.encode(value);
            limits.valueBytes(encoded.length);
            stagedBytes = Math.addExact(stagedBytes, encoded.length);
            limits.stagingBytes(stagedBytes);
            if (idOfBytes(encoded) < 0) fresh.add(encoded);
            worked.accept(1L);
        }
        fresh.sort(Arrays::compareUnsigned);
        deduplicateSorted(fresh);
        if (fresh.isEmpty()) return this;

        int extraBytes = 0;
        for (byte[] bytes : fresh) extraBytes = Math.addExact(extraBytes, bytes.length);
        int oldCount = size();
        int newCount = Math.addExact(oldCount, fresh.size());
        limits.entries(newCount);
        int newArenaBytes = Math.addExact(arena.length, extraBytes);
        limits.arenaBytes(newArenaBytes);

        byte[] nextArena = Arrays.copyOf(arena, newArenaBytes);
        int[] nextOffsets = Arrays.copyOf(offsets, newCount + 1);
        long[] nextHashes = Arrays.copyOf(hashes64, newCount);
        int cursor = arena.length;
        for (int i = 0; i < fresh.size(); i++) {
            byte[] bytes = fresh.get(i);
            System.arraycopy(bytes, 0, nextArena, cursor, bytes.length);
            cursor += bytes.length;
            int id = oldCount + i;
            nextOffsets[id + 1] = cursor;
            nextHashes[id] = MatHash.fnv64(bytes);
        }
        return new MatPool<>(codec, limits, nextArena, nextOffsets, nextHashes);
    }

    public boolean prefixCompatible(MatPool<T> older) {
        if (older == null || !codec.typeId().equals(older.codec.typeId())
                || older.size() > size()) return false;
        if (!Arrays.equals(older.offsets, 0, older.offsets.length,
                offsets, 0, older.offsets.length)) {
            return false;
        }
        return Arrays.equals(older.arena, 0, older.arena.length,
                arena, 0, older.arena.length);
    }

    byte[] arenaCopy() { return arena.clone(); }
    int[] offsetsCopy() { return offsets.clone(); }
    long[] hashesCopy() { return hashes64.clone(); }
    int[] slotsCopy() { return slots.clone(); }

    static <T> MatPool<T> fromCanonical(
            MatCodec<T> codec,
            MatLimits limits,
            byte[] arena,
            int[] offsets,
            long[] hashes64) {
        return new MatPool<>(
                codec,
                limits,
                arena.clone(),
                offsets.clone(),
                hashes64.clone());
    }

    private boolean equalBytes(int id, byte[] candidate) {
        int start = offsets[id];
        int length = offsets[id + 1] - start;
        if (length != candidate.length) return false;
        return Arrays.equals(arena, start, start + length,
                candidate, 0, candidate.length);
    }

    static int[] buildSlots(long[] hashes) {
        long required = Math.max(2L, hashes.length * 2L);
        if (required > (1L << 30)) throw new IllegalArgumentException("pool too large");
        int capacity = 2;
        while (capacity < required) capacity <<= 1;
        int[] slots = new int[capacity];
        for (int id = 0; id < hashes.length; id++) {
            int slot = MatHash.mix(MatHash.fold32(hashes[id])) & (capacity - 1);
            while (slots[slot] != 0) slot = (slot + 1) & (capacity - 1);
            slots[slot] = id + 1;
        }
        return slots;
    }

    private void checkId(int id) {
        if (id < 0 || id >= size()) throw new IndexOutOfBoundsException("pool id: " + id);
    }

    private static void deduplicateSorted(ArrayList<byte[]> values) {
        int count = 0;
        for (byte[] value : values) {
            if (count == 0 || !Arrays.equals(value, values.get(count - 1))) {
                values.set(count++, value);
            }
        }
        while (values.size() > count) values.remove(values.size() - 1);
    }

    private static void validateFrozen(
            byte[] arena, int[] offsets, long[] hashes64, MatLimits limits) {
        if (offsets.length == 0 || offsets.length - 1 != hashes64.length || offsets[0] != 0) {
            throw new IllegalArgumentException("invalid pool shape");
        }
        limits.entries(hashes64.length);
        limits.arenaBytes(arena.length);
        int previous = 0;
        for (int id = 0; id < hashes64.length; id++) {
            int end = offsets[id + 1];
            if (end < previous || end > arena.length) {
                throw new IllegalArgumentException("invalid pool offsets");
            }
            int length = end - previous;
            limits.valueBytes(length);
            if (MatHash.fnv64(arena, previous, length) != hashes64[id]) {
                throw new IllegalArgumentException("pool hash mismatch");
            }
            previous = end;
        }
        if (previous != arena.length) throw new IllegalArgumentException("unaddressed pool arena tail");
    }

    /** Transient builder. Frozen pool retains only primitive arrays. */
    public static final class Builder<T> {
        private final MatCodec<T> codec;
        private final MatLimits limits;
        private final ArrayList<byte[]> encoded = new ArrayList<>();
        private long inputValues;
        private long stagedBytes;

        private Builder(MatCodec<T> codec, MatLimits limits) {
            this.codec = Objects.requireNonNull(codec, "codec");
            this.limits = Objects.requireNonNull(limits, "limits");
        }

        public Builder<T> add(T value) {
            limits.inputValues(++inputValues);
            byte[] bytes = codec.encode(value);
            limits.valueBytes(bytes.length);
            stagedBytes = Math.addExact(stagedBytes, bytes.length);
            limits.stagingBytes(stagedBytes);
            encoded.add(bytes);
            return this;
        }

        public Builder<T> addAll(Collection<? extends T> values) {
            return addAll(values, () -> false, ignored -> { });
        }

        /** Cancellable/observable bulk admission using only JDK callback types. */
        public Builder<T> addAll(
                Iterable<? extends T> values, BooleanSupplier canceled, LongConsumer worked) {
            Objects.requireNonNull(values, "values");
            Objects.requireNonNull(canceled, "canceled");
            Objects.requireNonNull(worked, "worked");
            for (T value : values) {
                if (canceled.getAsBoolean()) {
                    throw new CancellationException("MAT pool build canceled");
                }
                add(value);
                worked.accept(1L);
            }
            return this;
        }

        public MatPool<T> build() {
            encoded.sort(Arrays::compareUnsigned);
            deduplicateSorted(encoded);
            limits.entries(encoded.size());
            int total = 0;
            for (byte[] bytes : encoded) total = Math.addExact(total, bytes.length);
            limits.arenaBytes(total);
            byte[] arena = new byte[total];
            int[] offsets = new int[encoded.size() + 1];
            long[] hashes = new long[encoded.size()];
            int cursor = 0;
            for (int id = 0; id < encoded.size(); id++) {
                byte[] bytes = encoded.get(id);
                System.arraycopy(bytes, 0, arena, cursor, bytes.length);
                cursor += bytes.length;
                offsets[id + 1] = cursor;
                hashes[id] = MatHash.fnv64(bytes);
            }
            return new MatPool<>(codec, limits, arena, offsets, hashes);
        }
    }
}
