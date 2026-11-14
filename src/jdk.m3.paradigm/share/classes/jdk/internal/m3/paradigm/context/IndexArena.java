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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.LongConsumer;

/**
 * Structure-of-arrays storage for large populations of contexts.
 *
 * <p>A logical context is addressed by a 64-bit slot+generation handle. Optional
 * flag, strength and value columns are allocated only on first use.
 *
 * <p>Allocation/release are structural operations and must not race row-local
 * writes to the same handle. Callers quiesce a row before release. This avoids
 * retaining one lock/object per context while preserving direct primitive lanes.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextIndexArena.java}
 * blob {@code c5f1790e3e3a59e6bbb1c70ba60c6da22a9c8ece} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class IndexArena<V> {
    /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
    public IndexArena() {
    }

    private static final long[] EMPTY_LONGS = new long[0];
    private static final int[] EMPTY_INTS = new int[0];
    private static final Object[] EMPTY_OBJECTS = new Object[0];
    private static final int MIN_CAPACITY = 8;
    private static final int STRENGTH_SLOTS = 32;
    private static final int STRENGTH_WORDS = STRENGTH_SLOTS / 8;

    private static final long ALLOCATED = 1L;
    private static final int PHASE_SHIFT = 1;
    private static final long PHASE_MASK = 0x3L << PHASE_SHIFT;
    private static final int REVISION_SHIFT = 3;
    private static final long MAX_REVISION = Long.MAX_VALUE >>> REVISION_SHIFT;

    private static final VarHandle LONG_ELEMENT = MethodHandles.arrayElementVarHandle(long[].class);
    private static final VarHandle INT_ELEMENT = MethodHandles.arrayElementVarHandle(int[].class);
    private static final VarHandle OBJECT_ELEMENT = MethodHandles.arrayElementVarHandle(Object[].class);

    private volatile long[] uidHigh = EMPTY_LONGS;
    private volatile long[] uidLow = EMPTY_LONGS;
    private volatile long[] control = EMPTY_LONGS;
    private volatile long[] parent = EMPTY_LONGS;
    private volatile int[] generation = new int[0];

    private volatile long[] flags = EMPTY_LONGS;
    private volatile long[] strengthWords = EMPTY_LONGS;
    private volatile Object[] values = EMPTY_OBJECTS;

    // Lazy structural adjacency plane. Slot links are encoded as slot+1; zero means absent.
    private volatile int[] firstChild = EMPTY_INTS;
    private volatile int[] nextSibling = EMPTY_INTS;
    private volatile int[] previousSibling = EMPTY_INTS;
    private volatile int[] childCounts = EMPTY_INTS;

    private volatile int size;
    private volatile int highWater;
    private int freeHead = -1;

    public synchronized long allocate(long high, long low) {
        int slot;
        if (freeHead >= 0) {
            slot = freeHead;
            freeHead = (int) parent[slot] - 1;
        } else {
            ensureCoreCapacity(Math.addExact(highWater, 1));
            slot = highWater++;
            if (generation[slot] == 0) generation[slot] = 1;
        }
        uidHigh[slot] = high;
        uidLow[slot] = low;
        parent[slot] = 0L;
        if (flags.length != 0) flags[slot] = 0L;
        if (strengthWords.length != 0) {
            int offset = slot * STRENGTH_WORDS;
            Arrays.fill(strengthWords, offset, offset + STRENGTH_WORDS, 0L);
        }
        if (values.length != 0) values[slot] = null;
        if (firstChild.length != 0) {
            INT_ELEMENT.setRelease(firstChild, slot, 0);
            INT_ELEMENT.setRelease(nextSibling, slot, 0);
            INT_ELEMENT.setRelease(previousSibling, slot, 0);
            INT_ELEMENT.setRelease(childCounts, slot, 0);
        }
        LONG_ELEMENT.setRelease(control, slot, ALLOCATED);
        size++;
        return ArenaHandle.of(slot, generation[slot]);
    }

    public synchronized boolean release(long handle) {
        int slot = checkedSlot(handle);
        if (firstChild.length != 0
                && (int) INT_ELEMENT.getAcquire(childCounts, slot) != 0) {
            throw new IllegalStateException("cannot release context with live children");
        }
        detachFromParent(slot);
        LONG_ELEMENT.setRelease(control, slot, 0L);
        uidHigh[slot] = 0L;
        uidLow[slot] = 0L;
        if (flags.length != 0) flags[slot] = 0L;
        if (strengthWords.length != 0) {
            int offset = slot * STRENGTH_WORDS;
            Arrays.fill(strengthWords, offset, offset + STRENGTH_WORDS, 0L);
        }
        if (values.length != 0) values[slot] = null;
        if (firstChild.length != 0) {
            firstChild[slot] = 0;
            nextSibling[slot] = 0;
            previousSibling[slot] = 0;
            childCounts[slot] = 0;
        }
        size--;
        if (generation[slot] == Integer.MAX_VALUE) {
            parent[slot] = 0L;
        } else {
            generation[slot]++;
            parent[slot] = (long) freeHead + 1L;
            freeHead = slot;
        }
        return true;
    }

    public int size() { return size; }
    public int capacity() { return control.length; }
    public int highWaterMark() { return highWater; }

    public boolean alive(long handle) {
        if (handle == ArenaHandle.INVALID) return false;
        int slot;
        int expectedGeneration;
        try {
            slot = ArenaHandle.slot(handle);
            expectedGeneration = ArenaHandle.generation(handle);
        } catch (IllegalArgumentException invalid) {
            return false;
        }
        if (slot >= highWater) return false;
        long published = (long) LONG_ELEMENT.getAcquire(control, slot);
        return (published & ALLOCATED) != 0L
                && generation[slot] == expectedGeneration;
    }

    public long uidHigh(long handle) { return uidHigh[checkedSlot(handle)]; }
    public long uidLow(long handle) { return uidLow[checkedSlot(handle)]; }

    public long parent(long handle) {
        return (long) LONG_ELEMENT.getAcquire(parent, checkedSlot(handle));
    }

    public synchronized void parent(long handle, long parentHandle) {
        int slot = checkedSlot(handle);
        if (parentHandle == handle) throw new IllegalArgumentException("context cannot parent itself");
        if (parentHandle != ArenaHandle.INVALID) {
            checkedSlot(parentHandle);
            long cursor = parentHandle;
            for (int depth = 0; cursor != ArenaHandle.INVALID; depth++) {
                if (cursor == handle) throw new IllegalArgumentException("context parent cycle");
                if (depth >= size) throw new IllegalStateException("context parent graph is cyclic");
                cursor = parent(cursor);
            }
        }
        long before = (long) LONG_ELEMENT.getAcquire(parent, slot);
        if (before == parentHandle) return;
        detachFromParent(slot);
        LONG_ELEMENT.setRelease(parent, slot, parentHandle);
        if (parentHandle != ArenaHandle.INVALID) {
            int owner = checkedSlot(parentHandle);
            ensureChildren();
            int oldFirst = (int) INT_ELEMENT.getAcquire(firstChild, owner);
            INT_ELEMENT.setRelease(firstChild, owner, slot + 1);
            INT_ELEMENT.setRelease(nextSibling, slot, oldFirst);
            INT_ELEMENT.setRelease(previousSibling, slot, 0);
            if (oldFirst != 0) {
                INT_ELEMENT.setRelease(previousSibling, oldFirst - 1, slot + 1);
            }
            int oldCount = (int) INT_ELEMENT.getAcquire(childCounts, owner);
            INT_ELEMENT.setRelease(childCounts, owner, Math.addExact(oldCount, 1));
        }
    }

    public int childCount(long handle) {
        int slot = checkedSlot(handle);
        return childCounts.length == 0 ? 0 : (int) INT_ELEMENT.getAcquire(childCounts, slot);
    }

    public long firstChild(long handle) {
        int slot = checkedSlot(handle);
        if (firstChild.length == 0) return ArenaHandle.INVALID;
        int encoded = (int) INT_ELEMENT.getAcquire(firstChild, slot);
        return encoded == 0 ? ArenaHandle.INVALID : liveHandle(encoded - 1);
    }

    public long nextSibling(long handle) {
        int slot = checkedSlot(handle);
        if (nextSibling.length == 0) return ArenaHandle.INVALID;
        int encoded = (int) INT_ELEMENT.getAcquire(nextSibling, slot);
        return encoded == 0 ? ArenaHandle.INVALID : liveHandle(encoded - 1);
    }

    /** Allocation-free direct-child traversal. Structural mutation must be quiescent during the callback. */
    public void forEachChild(long handle, LongConsumer consumer) {
        int owner = checkedSlot(handle);
        Objects.requireNonNull(consumer, "consumer");
        if (firstChild.length == 0) return;
        int encoded = (int) INT_ELEMENT.getAcquire(firstChild, owner);
        while (encoded != 0) {
            int slot = encoded - 1;
            consumer.accept(liveHandle(slot));
            encoded = (int) INT_ELEMENT.getAcquire(nextSibling, slot);
        }
    }

    /** Direct-child UID lookup without UUID/String materialization or a hash-map entry. */
    public long childByUid(long handle, long high, long low) {
        int owner = checkedSlot(handle);
        if (firstChild.length == 0) return ArenaHandle.INVALID;
        int encoded = (int) INT_ELEMENT.getAcquire(firstChild, owner);
        while (encoded != 0) {
            int slot = encoded - 1;
            long child = liveHandle(slot);
            if (uidHigh[slot] == high && uidLow[slot] == low) return child;
            encoded = (int) INT_ELEMENT.getAcquire(nextSibling, slot);
        }
        return ArenaHandle.INVALID;
    }

    /**
     * Allocation-free preorder traversal using parent/first-child/next-sibling links.
     *
     * <p>No recursion or stack is retained. Structural mutation must be quiescent.
     */
    public void forEachSubtree(long rootHandle, int maximumNodes, LongConsumer consumer) {
        checkedSlot(rootHandle);
        Objects.requireNonNull(consumer, "consumer");
        if (maximumNodes < 1) throw new IllegalArgumentException("maximumNodes must be >= 1");
        long current = rootHandle;
        int visited = 0;
        walk:
        for (;;) {
            if (visited++ >= maximumNodes) {
                throw new IllegalArgumentException("subtree traversal limit exceeded");
            }
            consumer.accept(current);
            long child = firstChild(current);
            if (child != ArenaHandle.INVALID) {
                current = child;
                continue;
            }
            while (current != rootHandle) {
                long sibling = nextSibling(current);
                if (sibling != ArenaHandle.INVALID) {
                    current = sibling;
                    continue walk;
                }
                current = parent(current);
            }
            return;
        }
    }

    public long control(long handle) {
        return (long) LONG_ELEMENT.getAcquire(control, checkedSlot(handle));
    }

    public long revision(long handle) {
        return control(handle) >>> REVISION_SHIFT;
    }

    public CompactContextCell.Phase phase(long handle) {
        return phaseOf(control(handle));
    }

    public boolean tryFreeze(long handle, long expectedControl) {
        int slot = checkedSlot(handle);
        if (phaseOf(expectedControl) != CompactContextCell.Phase.ACTIVE || revisionOf(expectedControl) >= MAX_REVISION) {
            return false;
        }
        long update = withRevisionAndPhase(expectedControl, revisionOf(expectedControl) + 1,
                CompactContextCell.Phase.FROZEN);
        return LONG_ELEMENT.compareAndSet(control, slot, expectedControl, update);
    }

    public boolean tryDispose(long handle, long expectedControl) {
        int slot = checkedSlot(handle);
        if (phaseOf(expectedControl) == CompactContextCell.Phase.DISPOSED
                || revisionOf(expectedControl) >= MAX_REVISION) {
            return false;
        }
        long update = withRevisionAndPhase(expectedControl, revisionOf(expectedControl) + 1,
                CompactContextCell.Phase.DISPOSED);
        boolean changed = LONG_ELEMENT.compareAndSet(control, slot, expectedControl, update);
        if (changed && values.length != 0) OBJECT_ELEMENT.setRelease(values, slot, null);
        return changed;
    }

    public long flags(long handle) {
        int slot = checkedSlot(handle);
        long[] bank = flags;
        return bank.length == 0 ? 0L : (long) LONG_ELEMENT.getAcquire(bank, slot);
    }

    public long enableFlags(long handle, long mask) {
        int slot = checkedSlot(handle);
        long[] bank = ensureFlags();
        return (long) LONG_ELEMENT.getAndBitwiseOr(bank, slot, mask) | mask;
    }

    public long disableFlags(long handle, long mask) {
        int slot = checkedSlot(handle);
        long[] bank = ensureFlags();
        for (;;) {
            long before = (long) LONG_ELEMENT.getAcquire(bank, slot);
            long after = before & ~mask;
            if (LONG_ELEMENT.compareAndSet(bank, slot, before, after)) return after;
        }
    }

    public int strength(long handle, int strengthSlot) {
        int slot = checkedSlot(handle);
        checkStrengthSlot(strengthSlot);
        long[] bank = strengthWords;
        if (bank.length == 0) return 0;
        int at = slot * STRENGTH_WORDS + (strengthSlot >>> 3);
        int shift = (strengthSlot & 7) << 3;
        return (int) (((long) LONG_ELEMENT.getAcquire(bank, at) >>> shift) & 0xffL);
    }

    public void strength(long handle, int strengthSlot, int unsignedValue) {
        int slot = checkedSlot(handle);
        checkStrengthSlot(strengthSlot);
        if ((unsignedValue & ~0xff) != 0) throw new IllegalArgumentException("strength must be in [0,255]");
        long[] bank = ensureStrengths();
        int at = slot * STRENGTH_WORDS + (strengthSlot >>> 3);
        int shift = (strengthSlot & 7) << 3;
        long mask = 0xffL << shift;
        for (;;) {
            long before = (long) LONG_ELEMENT.getAcquire(bank, at);
            long after = (before & ~mask) | ((long) unsignedValue << shift);
            if (before == after || LONG_ELEMENT.compareAndSet(bank, at, before, after)) return;
        }
    }

    @SuppressWarnings("unchecked")
    public V value(long handle) {
        int slot = checkedSlot(handle);
        if (phaseOf((long) LONG_ELEMENT.getAcquire(control, slot)) == CompactContextCell.Phase.DISPOSED) return null;
        Object[] bank = values;
        return bank.length == 0 ? null : (V) OBJECT_ELEMENT.getAcquire(bank, slot);
    }

    public void value(long handle, V value) {
        int slot = checkedSlot(handle);
        OBJECT_ELEMENT.setRelease(ensureValues(), slot, value);
    }

    public void forEachHandle(LongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        int limit = highWater;
        for (int slot = 0; slot < limit; slot++) {
            long state = (long) LONG_ELEMENT.getAcquire(control, slot);
            if ((state & ALLOCATED) != 0L) consumer.accept(ArenaHandle.of(slot, generation[slot]));
        }
    }

    /** Core columns only; optional flags/strength/value sidecars are excluded. */
    public long logicalCoreBytesPerCapacityRow() {
        return Long.BYTES * 4L + Integer.BYTES;
    }

    public boolean flagsMaterialized() { return flags.length != 0; }
    public boolean strengthsMaterialized() { return strengthWords.length != 0; }
    public boolean valuesMaterialized() { return values.length != 0; }
    public boolean childrenMaterialized() { return firstChild.length != 0; }

    /** Optional child-index plane: first/next/previous/count, four primitive ints per capacity row. */
    public long logicalChildrenBytesPerCapacityRow() {
        return Integer.BYTES * 4L;
    }

    private int checkedSlot(long handle) {
        int slot = ArenaHandle.slot(handle);
        if (slot >= highWater) throw new IllegalArgumentException("stale or foreign context arena handle");
        long published = (long) LONG_ELEMENT.getAcquire(control, slot);
        if ((published & ALLOCATED) == 0L
                || generation[slot] != ArenaHandle.generation(handle)) {
            throw new IllegalArgumentException("stale or foreign context arena handle");
        }
        return slot;
    }

    private synchronized long[] ensureFlags() {
        if (flags.length == 0) flags = new long[control.length];
        return flags;
    }

    private synchronized long[] ensureStrengths() {
        if (strengthWords.length == 0) strengthWords = new long[Math.multiplyExact(control.length, STRENGTH_WORDS)];
        return strengthWords;
    }

    private synchronized Object[] ensureValues() {
        if (values.length == 0) values = new Object[control.length];
        return values;
    }

    private synchronized void ensureChildren() {
        if (firstChild.length != 0) return;
        firstChild = new int[control.length];
        nextSibling = new int[control.length];
        previousSibling = new int[control.length];
        childCounts = new int[control.length];
    }

    private void detachFromParent(int slot) {
        long ownerHandle = (long) LONG_ELEMENT.getAcquire(parent, slot);
        if (ownerHandle == ArenaHandle.INVALID || firstChild.length == 0) {
            LONG_ELEMENT.setRelease(parent, slot, ArenaHandle.INVALID);
            return;
        }
        int owner = checkedSlot(ownerHandle);
        int previous = (int) INT_ELEMENT.getAcquire(previousSibling, slot);
        int next = (int) INT_ELEMENT.getAcquire(nextSibling, slot);
        if (previous == 0) {
            INT_ELEMENT.setRelease(firstChild, owner, next);
        } else {
            INT_ELEMENT.setRelease(nextSibling, previous - 1, next);
        }
        if (next != 0) {
            INT_ELEMENT.setRelease(previousSibling, next - 1, previous);
        }
        INT_ELEMENT.setRelease(previousSibling, slot, 0);
        INT_ELEMENT.setRelease(nextSibling, slot, 0);
        int count = (int) INT_ELEMENT.getAcquire(childCounts, owner);
        INT_ELEMENT.setRelease(childCounts, owner, count - 1);
        LONG_ELEMENT.setRelease(parent, slot, ArenaHandle.INVALID);
    }

    private long liveHandle(int slot) {
        if (slot < 0 || slot >= highWater
                || (((long) LONG_ELEMENT.getAcquire(control, slot)) & ALLOCATED) == 0L) {
            throw new IllegalStateException("child index points to inactive row");
        }
        return ArenaHandle.of(slot, generation[slot]);
    }

    private synchronized void ensureCoreCapacity(int required) {
        if (required <= control.length) return;
        int oldCapacity = control.length;
        int capacity = oldCapacity == 0 ? MIN_CAPACITY : oldCapacity;
        while (capacity < required) capacity = Math.multiplyExact(capacity, 2);
        uidHigh = Arrays.copyOf(uidHigh, capacity);
        uidLow = Arrays.copyOf(uidLow, capacity);
        control = Arrays.copyOf(control, capacity);
        parent = Arrays.copyOf(parent, capacity);
        generation = Arrays.copyOf(generation, capacity);
        if (flags.length != 0) flags = Arrays.copyOf(flags, capacity);
        if (strengthWords.length != 0) {
            strengthWords = Arrays.copyOf(strengthWords, Math.multiplyExact(capacity, STRENGTH_WORDS));
        }
        if (values.length != 0) values = Arrays.copyOf(values, capacity);
        if (firstChild.length != 0) {
            firstChild = Arrays.copyOf(firstChild, capacity);
            nextSibling = Arrays.copyOf(nextSibling, capacity);
            previousSibling = Arrays.copyOf(previousSibling, capacity);
            childCounts = Arrays.copyOf(childCounts, capacity);
        }
    }

    private static long revisionOf(long control) {
        return control >>> REVISION_SHIFT;
    }

    private static CompactContextCell.Phase phaseOf(long control) {
        if ((control & ALLOCATED) == 0L) throw new IllegalArgumentException("context row is not allocated");
        return switch ((int) ((control & PHASE_MASK) >>> PHASE_SHIFT)) {
            case 0 -> CompactContextCell.Phase.ACTIVE;
            case 1 -> CompactContextCell.Phase.FROZEN;
            case 2 -> CompactContextCell.Phase.DISPOSED;
            default -> throw new IllegalStateException("invalid context phase bits");
        };
    }

    private static long withRevisionAndPhase(long before, long revision, CompactContextCell.Phase phase) {
        long encodedPhase = switch (phase) {
            case ACTIVE -> 0L;
            case FROZEN -> 1L;
            case DISPOSED -> 2L;
        };
        return ALLOCATED | (encodedPhase << PHASE_SHIFT) | (revision << REVISION_SHIFT);
    }

    private static void checkStrengthSlot(int slot) {
        if (slot < 0 || slot >= STRENGTH_SLOTS) throw new IndexOutOfBoundsException("strength slot: " + slot);
    }
}
