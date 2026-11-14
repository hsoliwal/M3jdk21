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
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Fixed-capacity structure-of-arrays arena for very large context/perception graphs.
 *
 * <p>The arena is intentionally index/offset oriented. A {@link Ref} retains only
 * the arena reference and one int index. Strings, UUID objects, parent references,
 * child collections, flags and strength arrays are not retained per Ref.
 *
 * <p>Structural mutation is single-writer. Reads after construction are allocation-free
 * except explicit boundary materializations such as {@link Ref#uid()} and {@link Ref#id()}.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextArena.java}
 * blob {@code 918dd8b4155b72d11976154a1fbcf6c4494c00c7} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class ContextArena {
    public enum Phase { ACTIVE, FROZEN, DISPOSED }

    private static final long PHASE_MASK = 0b11L;
    private static final int REVISION_SHIFT = 2;
    private static final long MAX_REVISION = Long.MAX_VALUE >>> REVISION_SHIFT;

    private final long[] uidHigh;
    private final long[] uidLow;
    private final long[] control;
    private final long[] semanticFlags;
    private final int[] parent;
    private final int[] firstChild;
    private final int[] lastChild;
    private final int[] nextSibling;
    private final Object[] values;
    private final byte[] strengths;
    private final Ref[] refs;
    private final int strengthSlots;
    private final UidIndex uidIndex;
    private int size;
    private int root = -1;

    public ContextArena(int capacity, int strengthSlots) {
        if (capacity < 1) throw new IllegalArgumentException("capacity must be > 0");
        if (strengthSlots < 0 || strengthSlots > 255) throw new IllegalArgumentException("strength slots must be 0..255");
        this.strengthSlots = strengthSlots;
        uidHigh = new long[capacity];
        uidLow = new long[capacity];
        control = new long[capacity];
        semanticFlags = new long[capacity];
        parent = new int[capacity];
        firstChild = new int[capacity];
        lastChild = new int[capacity];
        nextSibling = new int[capacity];
        values = new Object[capacity];
        refs = new Ref[capacity];
        strengths = strengthSlots == 0 ? null : new byte[Math.multiplyExact(capacity, strengthSlots)];
        Arrays.fill(parent, -1);
        Arrays.fill(firstChild, -1);
        Arrays.fill(lastChild, -1);
        Arrays.fill(nextSibling, -1);
        uidIndex = new UidIndex(capacity);
    }

    public int capacity() { return uidHigh.length; }
    public int size() { return size; }
    public int strengthSlots() { return strengthSlots; }
    public int rootIndex() { return root; }
    public Ref root() { if (root < 0) throw new NoSuchElementException("root not allocated"); return refs[root]; }

    public Ref createRoot(long high, long low, Object value) {
        if (root >= 0) throw new IllegalStateException("root already exists");
        Ref created = allocate(-1, high, low, value);
        root = created.index;
        return created;
    }

    public Ref addChild(Ref parentRef, long high, long low, Object value) {
        int parentIndex = requireOwned(parentRef);
        return allocate(parentIndex, high, low, value);
    }

    public Ref ref(int index) {
        Objects.checkIndex(index, size);
        return refs[index];
    }

    public Ref find(long high, long low) {
        int index = uidIndex.get(high, low);
        return index < 0 ? null : refs[index];
    }

    public Ref parent(Ref ref) {
        int index = requireOwned(ref);
        int p = parent[index];
        return p < 0 ? null : refs[p];
    }

    public Ref firstChild(Ref ref) {
        int index = requireOwned(ref);
        int child = firstChild[index];
        return child < 0 ? null : refs[child];
    }

    public Ref nextSibling(Ref ref) {
        int index = requireOwned(ref);
        int sibling = nextSibling[index];
        return sibling < 0 ? null : refs[sibling];
    }

    public void forEachChild(Ref ref, Consumer<Ref> action) {
        Objects.requireNonNull(action, "action");
        int index = requireOwned(ref);
        for (int child = firstChild[index]; child >= 0; child = nextSibling[child]) action.accept(refs[child]);
    }

    public int childCount(Ref ref) {
        int count = 0;
        int index = requireOwned(ref);
        for (int child = firstChild[index]; child >= 0; child = nextSibling[child]) count++;
        return count;
    }

    public int depth(Ref ref) {
        int depth = 0;
        for (int at = requireOwned(ref); parent[at] >= 0; at = parent[at]) depth++;
        return depth;
    }

    /** Returns root-to-node arena indices. Boundary allocation only. */
    public int[] pathIndices(Ref ref) {
        int index = requireOwned(ref);
        int[] result = new int[depth(ref) + 1];
        for (int at = result.length - 1; at >= 0; at--) {
            result[at] = index;
            index = parent[index];
        }
        return result;
    }

    public int primitiveLaneBytesPerNode() {
        return 4 * Long.BYTES + 4 * Integer.BYTES + strengthSlots;
    }

    private Ref allocate(int parentIndex, long high, long low, Object value) {
        if (size == capacity()) throw new IllegalStateException("arena capacity exhausted");
        if (uidIndex.contains(high, low)) throw new IllegalArgumentException("duplicate context UID");
        int index = size++;
        uidHigh[index] = high;
        uidLow[index] = low;
        parent[index] = parentIndex;
        values[index] = value;
        Ref ref = new Ref(this, index);
        refs[index] = ref;
        uidIndex.put(high, low, index);
        if (parentIndex >= 0) {
            int tail = lastChild[parentIndex];
            if (tail < 0) firstChild[parentIndex] = index;
            else nextSibling[tail] = index;
            lastChild[parentIndex] = index;
        }
        return ref;
    }

    private int requireOwned(Ref ref) {
        Objects.requireNonNull(ref, "ref");
        if (ref.arena != this) throw new IllegalArgumentException("context belongs to another arena");
        return ref.index;
    }

    private Phase phase(int index) {
        return switch ((int) (control[index] & PHASE_MASK)) {
            case 0 -> Phase.ACTIVE;
            case 1 -> Phase.FROZEN;
            case 2 -> Phase.DISPOSED;
            default -> throw new IllegalStateException("invalid phase bits");
        };
    }

    private long revision(int index) { return control[index] >>> REVISION_SHIFT; }

    private void requireActive(int index) {
        if (phase(index) != Phase.ACTIVE) throw new IllegalStateException("context is not active");
    }

    private void touch(int index) {
        long revision = revision(index);
        if (revision == MAX_REVISION) throw new ArithmeticException("context revision exhausted");
        control[index] = ((revision + 1) << REVISION_SHIFT) | (control[index] & PHASE_MASK);
    }

    private void phase(int index, Phase phase) {
        long revision = revision(index);
        if (revision == MAX_REVISION) throw new ArithmeticException("context revision exhausted");
        control[index] = ((revision + 1) << REVISION_SHIFT) | switch (phase) {
            case ACTIVE -> 0L;
            case FROZEN -> 1L;
            case DISPOSED -> 2L;
        };
    }

    /** Two-field hot reference: one shared arena plus one integer row index. */
    public static final class Ref {
        private final ContextArena arena;
        private final int index;

        private Ref(ContextArena arena, int index) { this.arena = arena; this.index = index; }

        public int index() { return index; }
        public long uidHigh() { return arena.uidHigh[index]; }
        public long uidLow() { return arena.uidLow[index]; }
        public UUID uid() { return new UUID(uidHigh(), uidLow()); }
        public String id() { return uid().toString(); }
        public Ref parent() { return arena.parent(this); }
        public Ref firstChild() { return arena.firstChild(this); }
        public Ref nextSibling() { return arena.nextSibling(this); }
        public Phase phase() { return arena.phase(index); }
        public long revision() { return arena.revision(index); }
        public long flags() { return arena.semanticFlags[index]; }
        public Object value() { return arena.values[index]; }

        public void value(Object value) {
            arena.requireActive(index);
            if (!Objects.equals(arena.values[index], value)) {
                arena.values[index] = value;
                arena.touch(index);
            }
        }

        public boolean hasFlags(long mask) { return (flags() & mask) == mask; }

        public void enableFlags(long mask) {
            arena.requireActive(index);
            long before = arena.semanticFlags[index];
            long after = before | mask;
            if (before != after) {
                arena.semanticFlags[index] = after;
                arena.touch(index);
            }
        }

        public void disableFlags(long mask) {
            arena.requireActive(index);
            long before = arena.semanticFlags[index];
            long after = before & ~mask;
            if (before != after) {
                arena.semanticFlags[index] = after;
                arena.touch(index);
            }
        }

        public int strength(int slot) {
            if (arena.strengthSlots == 0) return 0;
            Objects.checkIndex(slot, arena.strengthSlots);
            return Byte.toUnsignedInt(arena.strengths[index * arena.strengthSlots + slot]);
        }

        public void strength(int slot, int unsigned) {
            arena.requireActive(index);
            if (arena.strengthSlots == 0) throw new UnsupportedOperationException("arena has no strength lanes");
            Objects.checkIndex(slot, arena.strengthSlots);
            if ((unsigned & ~0xff) != 0) throw new IllegalArgumentException("strength must be in [0,255]");
            int offset = index * arena.strengthSlots + slot;
            if (Byte.toUnsignedInt(arena.strengths[offset]) != unsigned) {
                arena.strengths[offset] = (byte) unsigned;
                arena.touch(index);
            }
        }

        public void freeze() {
            arena.requireActive(index);
            arena.phase(index, Phase.FROZEN);
        }

        public void dispose() {
            if (phase() == Phase.DISPOSED) return;
            arena.values[index] = null;
            if (arena.strengthSlots > 0) {
                Arrays.fill(arena.strengths, index * arena.strengthSlots, (index + 1) * arena.strengthSlots, (byte) 0);
            }
            arena.semanticFlags[index] = 0L;
            arena.phase(index, Phase.DISPOSED);
        }

        @Override public boolean equals(Object other) {
            return this == other || other instanceof Ref ref && arena == ref.arena && index == ref.index;
        }

        @Override public int hashCode() { return 31 * System.identityHashCode(arena) + index; }
        @Override public String toString() { return "ContextArena.Ref[index=" + index + ",uid=" + id() + "]"; }
    }
}
