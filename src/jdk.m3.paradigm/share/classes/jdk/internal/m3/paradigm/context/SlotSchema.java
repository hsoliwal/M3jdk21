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
import java.util.HashSet;
import java.util.Objects;

/**
 * Shared exact-name schema for up to 64 context-local slots.
 *
 * <p>The schema owns names once. Individual contexts retain only ordinal positions,
 * presence words, and primitive/reference lanes. Lookup uses a primitive short[]
 * index and never allocates boxed slot integers.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextSlotSchema.java}
 * blob {@code e1618c8906afa30b493e28b0cd3e2657ab68fd41} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class SlotSchema {
    public static final int MAX_SLOTS = Long.SIZE;
    private static final int MIN_INDEX_CAPACITY = 8;

    private final String[] names;
    private final short[] index;
    private final int indexMask;

    public SlotSchema(String... names) {
        Objects.requireNonNull(names, "names");
        if (names.length > MAX_SLOTS) {
            throw new IllegalArgumentException(
                    "context slot schema supports at most 64 names");
        }
        this.names = names.clone();
        HashSet<String> unique =
                new HashSet<>(Math.max(1, names.length * 2));
        for (int slot = 0; slot < this.names.length; slot++) {
            String name = requireName(this.names[slot]);
            if (!unique.add(name)) {
                throw new IllegalArgumentException(
                        "duplicate context slot name: " + name);
            }
            this.names[slot] = name;
        }
        int capacity = MIN_INDEX_CAPACITY;
        while (capacity < Math.max(1, names.length * 2)) {
            capacity <<= 1;
        }
        this.index = new short[capacity];
        this.indexMask = capacity - 1;
        for (int slot = 0; slot < this.names.length; slot++) {
            int at = mix(this.names[slot].hashCode()) & indexMask;
            while (index[at] != 0) {
                at = (at + 1) & indexMask;
            }
            index[at] = (short) (slot + 1);
        }
    }

    public static SlotSchema of(String... names) {
        return new SlotSchema(names);
    }

    public int size() {
        return names.length;
    }

    public boolean isEmpty() {
        return names.length == 0;
    }

    public String name(int slot) {
        return names[Objects.checkIndex(slot, names.length)];
    }

    /** Cold boundary lookup. Hot callers should retain the returned ordinal. */
    public int slot(String exactName) {
        Objects.requireNonNull(exactName, "exactName");
        if (names.length == 0) {
            return -1;
        }
        int at = mix(exactName.hashCode()) & indexMask;
        while (true) {
            int encoded = Short.toUnsignedInt(index[at]);
            if (encoded == 0) {
                return -1;
            }
            int slot = encoded - 1;
            if (names[slot].equals(exactName)) {
                return slot;
            }
            at = (at + 1) & indexMask;
        }
    }

    public boolean contains(String exactName) {
        return slot(exactName) >= 0;
    }

    public String[] namesCopy() {
        return names.clone();
    }

    public long allSlotsMask() {
        return names.length == Long.SIZE
                ? -1L
                : (1L << names.length) - 1L;
    }

    /** Compatibility alias retained for the first compact-layout pass. */
    public long allowedBits() {
        return allSlotsMask();
    }

    public void requireCompatible(long presentMask) {
        if ((presentMask & ~allSlotsMask()) != 0L) {
            throw new IllegalArgumentException(
                    "slot mask contains names outside schema");
        }
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof SlotSchema schema
                && Arrays.equals(names, schema.names);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(names);
    }

    private static String requireName(String value) {
        String name = Objects.requireNonNull(value, "slot name");
        if (name.isBlank()
                || !name.equals(name.strip())
                || name.length() > 128) {
            throw new IllegalArgumentException(
                    "invalid context slot name");
        }
        return name;
    }

    private static int mix(int value) {
        int mixed = value ^ (value >>> 16);
        mixed *= 0x7feb352d;
        mixed ^= mixed >>> 15;
        mixed *= 0x846ca68b;
        return mixed ^ (mixed >>> 16);
    }
}
