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
package jdk.m3.collections.sketch;

import java.util.Arrays;
import java.util.function.LongConsumer;

/**
 * Fixed-size uniform reservoir sample over a long stream; RNG state is one long.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/LongReservoirSampler.java}
 * blob {@code da89ec312a6b1fa804a029ed95e13874f7ed44ed} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class LongReservoirSampler {
    private static final long DEFAULT_SEED = 0x2545f4914f6cdd1dL;

    private final long[] reservoir;
    private final long initialSeed;
    private long randomState;
    private long seen;
    private int size;

    public LongReservoirSampler(int capacity) { this(capacity, DEFAULT_SEED); }

    public LongReservoirSampler(int capacity, long seed) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity <= 0");
        reservoir = new long[capacity];
        initialSeed = seed == 0L ? DEFAULT_SEED : seed;
        randomState = initialSeed;
    }

    public int capacity() { return reservoir.length; }
    public int size() { return size; }
    public long seen() { return seen; }
    public long payloadBytes() { return (long) reservoir.length * Long.BYTES; }

    public boolean add(long value) {
        if (seen == Long.MAX_VALUE) throw new IllegalStateException("seen overflow");
        long position = seen++;
        if (size < reservoir.length) {
            reservoir[size++] = value;
            return true;
        }
        long choice = bounded(position + 1L);
        if (choice >= reservoir.length) return false;
        reservoir[(int) choice] = value;
        return true;
    }

    public long get(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException(index);
        return reservoir[index];
    }

    public long[] toArray() { return Arrays.copyOf(reservoir, size); }

    public void forEach(LongConsumer consumer) {
        for (int i = 0; i < size; i++) consumer.accept(reservoir[i]);
    }

    public void clear() {
        Arrays.fill(reservoir, 0, size, 0L);
        size = 0;
        seen = 0L;
        randomState = initialSeed;
    }

    private long bounded(long bound) {
        long mask = bound - 1L;
        long random = nextRandom() >>> 1;
        if ((bound & mask) == 0L) return random & mask;
        long candidate = random % bound;
        while (random + mask - candidate < 0L) {
            random = nextRandom() >>> 1;
            candidate = random % bound;
        }
        return candidate;
    }

    private long nextRandom() {
        long x = randomState;
        x ^= x >>> 12;
        x ^= x << 25;
        x ^= x >>> 27;
        randomState = x;
        return x * 0x2545f4914f6cdd1dL;
    }
}
