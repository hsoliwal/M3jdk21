/*******************************************************************************
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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.function.LongConsumer;

/**
 * Heterogeneous immutable value pool. Type ID participates in canonical identity.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatAnyPool.java}
 * blob {@code f122941ae98bea330ea64e02ed44ab6e7536c9e4} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatAnyPool {
    private final byte[] arena;
    private final int[] offsets;
    private final long[] hashes64;
    private final int[] slots;
    private final Map<String, MatCodec<?>> codecs;
    private final MatLimits limits;

    private MatAnyPool(
            byte[] arena,
            int[] offsets,
            long[] hashes64,
            Map<String, MatCodec<?>> codecs,
            MatLimits limits) {
        this.arena = arena;
        this.offsets = offsets;
        this.hashes64 = hashes64;
        this.codecs = Map.copyOf(codecs);
        this.limits = Objects.requireNonNull(limits, "limits");
        limits.entries(hashes64.length);
        limits.arenaBytes(arena.length);
        this.slots = buildSlots(hashes64);
    }

    public static Builder builder() { return new Builder(MatLimits.UNBOUNDED); }
    public static Builder builder(MatLimits limits) { return new Builder(limits); }
    public int size() { return offsets.length - 1; }
    public MatLimits limits() { return limits; }

    public long payloadBytes() {
        return arena.length + 4L * offsets.length + 8L * hashes64.length + 4L * slots.length;
    }

    public MatAnyRef ref(int id) { checkId(id); return new MatAnyRef(this, id); }
    public String typeId(int id) { return source(id).utf8(); }

    public Object value(int id) {
        MatSource source = source(id);
        String typeId = source.utf8();
        MatCodec<?> codec = codecs.get(typeId);
        if (codec == null) throw new IllegalStateException("codec registry missing " + typeId);
        byte[] payload = source.blob();
        source.end();
        return codec.decode(payload);
    }

    public byte[] canonicalBytes(int id) {
        checkId(id);
        return Arrays.copyOfRange(arena, offsets[id], offsets[id + 1]);
    }

    public long contentHash64(int id) {
        checkId(id);
        return hashes64[id];
    }

    public <T> int idOf(MatCodec<T> codec, T value) {
        return idOfEnvelope(envelope(codec, value, limits));
    }

    public <T> boolean contains(MatCodec<T> codec, T value) {
        return idOf(codec, value) >= 0;
    }

    private int idOfEnvelope(byte[] envelope) {
        long hash = MatHash.fnv64(envelope);
        int mask = slots.length - 1;
        int slot = MatHash.mix(MatHash.fold32(hash)) & mask;
        for (int probe = 0; probe < slots.length; probe++) {
            int stored = slots[slot];
            if (stored == 0) return -1;
            int id = stored - 1;
            if (hashes64[id] == hash && equalBytes(id, envelope)) return id;
            slot = (slot + 1) & mask;
        }
        return -1;
    }

    private MatSource source(int id) { return new MatSource(canonicalBytes(id)); }

    private boolean equalBytes(int id, byte[] value) {
        int start = offsets[id];
        int length = offsets[id + 1] - start;
        if (length != value.length) return false;
        for (int i = 0; i < length; i++) {
            if (arena[start + i] != value[i]) return false;
        }
        return true;
    }

    private static <T> byte[] envelope(MatCodec<T> codec, T value, MatLimits limits) {
        Objects.requireNonNull(codec, "codec");
        byte[] type = codec.typeId().getBytes(StandardCharsets.UTF_8);
        if (type.length > 65_535) throw new IllegalArgumentException("type ID too long");
        byte[] payload = codec.encode(value);
        limits.valueBytes(payload.length);
        int capacity = Math.addExact(Math.addExact(type.length, payload.length), 8);
        byte[] result = new MatSink(capacity).utf8(codec.typeId()).blob(payload).toByteArray();
        limits.valueBytes(result.length);
        return result;
    }

    private static int[] buildSlots(long[] hashes) {
        long required = Math.max(2L, 2L * hashes.length);
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

    public static final class Builder {
        private final ArrayList<byte[]> envelopes = new ArrayList<>();
        private final HashMap<String, MatCodec<?>> codecs = new HashMap<>();
        private final MatLimits limits;
        private long inputValues;
        private long stagedBytes;

        private Builder(MatLimits limits) {
            this.limits = Objects.requireNonNull(limits, "limits");
        }

        public <T> Builder add(MatCodec<T> codec, T value) {
            Objects.requireNonNull(codec, "codec");
            limits.inputValues(++inputValues);
            MatCodec<?> existing = codecs.putIfAbsent(codec.typeId(), codec);
            if (existing != null && existing.javaType() != codec.javaType()) {
                throw new IllegalArgumentException("type ID collision: " + codec.typeId());
            }
            byte[] encoded = envelope(codec, value, limits);
            stagedBytes = Math.addExact(stagedBytes, encoded.length);
            limits.stagingBytes(stagedBytes);
            envelopes.add(encoded);
            return this;
        }

        /** Cancellable/observable heterogeneous bulk admission. */
        public <T> Builder addAll(
                MatCodec<T> codec,
                Iterable<? extends T> values,
                BooleanSupplier canceled,
                LongConsumer worked) {
            Objects.requireNonNull(codec, "codec");
            Objects.requireNonNull(values, "values");
            Objects.requireNonNull(canceled, "canceled");
            Objects.requireNonNull(worked, "worked");
            for (T value : values) {
                if (canceled.getAsBoolean()) {
                    throw new CancellationException("MAT any-pool build canceled");
                }
                add(codec, value);
                worked.accept(1L);
            }
            return this;
        }

        public MatAnyPool build() {
            envelopes.sort(Arrays::compareUnsigned);
            int count = 0;
            for (byte[] bytes : envelopes) {
                if (count == 0 || !Arrays.equals(bytes, envelopes.get(count - 1))) {
                    envelopes.set(count++, bytes);
                }
            }
            while (envelopes.size() > count) envelopes.remove(envelopes.size() - 1);
            limits.entries(count);
            int total = 0;
            for (byte[] bytes : envelopes) total = Math.addExact(total, bytes.length);
            limits.arenaBytes(total);
            byte[] arena = new byte[total];
            int[] offsets = new int[count + 1];
            long[] hashes = new long[count];
            int cursor = 0;
            for (int id = 0; id < count; id++) {
                byte[] bytes = envelopes.get(id);
                System.arraycopy(bytes, 0, arena, cursor, bytes.length);
                cursor += bytes.length;
                offsets[id + 1] = cursor;
                hashes[id] = MatHash.fnv64(bytes);
            }
            return new MatAnyPool(arena, offsets, hashes, codecs, limits);
        }
    }
}
