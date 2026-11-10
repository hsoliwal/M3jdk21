/* ******************************************************************************
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
package jdk.internal.m3.paradigm.context;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Immutable shared name-to-ordinal schema for context-specific primitive collections.
 *
 * <p>Exact names are stored once in the schema. The general schema retains up to
 * 65,535 slots; one-word hot collections explicitly require at most 64.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextOrdinalSchema.java}
 * blob {@code 8ee0b83d458287c0a10b954c40a504d2118cbb60} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class OrdinalSchema {
    public static final int MAX_SLOTS = 65_535;
    public static final int MAX_HOT_SLOTS = Long.SIZE;
    public static final int MAX_KEYS = MAX_HOT_SLOTS;
    private static final int MIN_TABLE = 8;

    private final String[] names;
    private final int[] lookup;
    private final int mask;

    /** General shared schema; preserved source-compatible owner. */
    public OrdinalSchema(String... names) {
        Objects.requireNonNull(names, "names");
        if (names.length > MAX_SLOTS) {
            throw new IllegalArgumentException("too many schema slots: " + names.length);
        }
        this.names = names.clone();
        int capacity = MIN_TABLE;
        while (capacity < Math.max(MIN_TABLE, names.length << 1)) {
            capacity <<= 1;
        }
        lookup = new int[capacity];
        mask = capacity - 1;
        for (int ordinal = 0; ordinal < this.names.length; ordinal++) {
            String name = requireName(this.names[ordinal]);
            this.names[ordinal] = name;
            int slot = mix(name.hashCode()) & mask;
            while (lookup[slot] != 0) {
                int existing = lookup[slot] - 1;
                if (this.names[existing].equals(name)) {
                    throw new IllegalArgumentException("duplicate schema name: " + name);
                }
                slot = (slot + 1) & mask;
            }
            lookup[slot] = ordinal + 1;
        }
    }

    /** One-word schema factory used by rank/select hot collections. */
    public static OrdinalSchema of(String... names) {
        return hot(names);
    }

    /** One-word schema factory; general schemas remain available through the constructor. */
    public static OrdinalSchema hot(String... names) {
        Objects.requireNonNull(names, "names");
        if (names.length > MAX_HOT_SLOTS) {
            throw new IllegalArgumentException("hot schema supports at most 64 slots");
        }
        return new OrdinalSchema(names);
    }

    public int size() {
        return names.length;
    }

    public boolean isEmpty() {
        return names.length == 0;
    }

    public String name(int ordinal) {
        return names[Objects.checkIndex(ordinal, names.length)];
    }

    public int ordinal(String name) {
        Objects.requireNonNull(name, "name");
        int slot = mix(name.hashCode()) & mask;
        while (true) {
            int stored = lookup[slot];
            if (stored == 0) {
                return -1;
            }
            int ordinal = stored - 1;
            if (names[ordinal].equals(name)) {
                return ordinal;
            }
            slot = (slot + 1) & mask;
        }
    }

    public int requireOrdinal(String name) {
        int ordinal = ordinal(name);
        if (ordinal < 0) {
            throw new IllegalArgumentException("unknown schema name: " + name);
        }
        return ordinal;
    }

    public boolean contains(String name) {
        return name != null && ordinal(name) >= 0;
    }

    /** Existing defensive array API. */
    public String[] names() {
        return names.clone();
    }

    /** Allocation-at-boundary list view; hot lookup does not use it. */
    public List<String> namesView() {
        return List.copyOf(Arrays.asList(names.clone()));
    }

    public long fullMask() {
        requireHot(this);
        return names.length == Long.SIZE ? -1L : (1L << names.length) - 1L;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof OrdinalSchema schema
                && Arrays.equals(names, schema.names);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(names);
    }

    static OrdinalSchema requireHot(OrdinalSchema schema) {
        Objects.requireNonNull(schema, "schema");
        if (schema.size() > MAX_HOT_SLOTS) {
            throw new IllegalArgumentException(
                    "one-word ordinal collection requires <= 64 schema slots");
        }
        return schema;
    }

    static int denseIndex(long present, int ordinal) {
        long bit = 1L << Objects.checkIndex(ordinal, Long.SIZE);
        return (present & bit) == 0L
                ? -1
                : Long.bitCount(present & (bit - 1L));
    }

    static int insertionIndex(long present, int ordinal) {
        long bit = 1L << Objects.checkIndex(ordinal, Long.SIZE);
        return Long.bitCount(present & (bit - 1L));
    }

    private static String requireName(String name) {
        Objects.requireNonNull(name, "name");
        if (name.isEmpty() || !name.equals(name.strip()) || name.length() > 256) {
            throw new IllegalArgumentException("invalid schema name");
        }
        return name;
    }

    static int mix(int value) {
        int mixed = value ^ (value >>> 16);
        mixed *= 0x7feb352d;
        mixed ^= mixed >>> 15;
        mixed *= 0x846ca68b;
        return mixed ^ (mixed >>> 16);
    }
}
