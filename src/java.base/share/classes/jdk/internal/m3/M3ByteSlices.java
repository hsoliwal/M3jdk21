/*
 * Copyright (c) 2026.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 *
 * M3JDK21 internal immutable storage. Independently authored implementation.
 */
package jdk.internal.m3;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

/**
 * Interned immutable byte atoms and zero-copy slices, using only primitive
 * side arrays for descriptor offsets, lengths, generations and state flags.
 *
 * <p>Every unique atom is copied into a private page exactly once; subsequent
 * interning of equal content returns the same descriptor. Slices only add
 * integer metadata and share the original backing page. Pages are retained
 * for the lifetime of the pool; pressure-aware reclamation is future work.
 *
 * <p>References to descriptors use generation-qualified {@code long} handles.
 * Lease counters are explicit M3 ownership metadata, not Java refcounts.
 * No per-atom or per-slice Java entry objects are stored.
 */
public final class M3ByteSlices {
    public static final int LIVE = 1;
    public static final int INTERNED = 2;
    public static final int LEASED = 4;
    public static final int SHARED = 8;
    public static final int RETIRED = 16;

    private static final int PAGE_SIZE = 1 << 16;
    private static final int INITIAL_CAPACITY = 16;
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    private byte[][] pages = new byte[INITIAL_CAPACITY][];
    private int[] pageUsed = new int[INITIAL_CAPACITY];
    private int pageCount;

    private int[] pageIds = new int[INITIAL_CAPACITY];
    private int[] offsets = new int[INITIAL_CAPACITY];
    private int[] lengths = new int[INITIAL_CAPACITY];
    private int[] hashes = new int[INITIAL_CAPACITY];
    private int[] generations = new int[INITIAL_CAPACITY];
    private int[] states = new int[INITIAL_CAPACITY];
    private int[] leases = new int[INITIAL_CAPACITY];
    private int[] freeLinks = new int[INITIAL_CAPACITY];
    private int[] internTable = new int[32];

    private int allocated;
    private int atomCount;
    private int freeHead = -1;

    /**
     * Admits immutable bytes into the pool, or reuses the existing atom.
     * Hash collisions are always resolved by comparing the actual byte data.
     */
    public synchronized long intern(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        if (atomCount >= (internTable.length * 2) / 3) {
            growInternTable();
        }
        final int hash = Arrays.hashCode(bytes);
        int bucket = locateBucket(bytes, hash);
        if (internTable[bucket] != 0) {
            final int existing = internTable[bucket] - 1;
            return pack(existing, generations[existing]);
        }
        final int slot = allocateSlot();
        final int page = findWritablePage(bytes.length);
        final int offset = pageUsed[page];
        System.arraycopy(bytes, 0, pages[page], offset, bytes.length);
        pageUsed[page] += bytes.length;
        pageIds[slot] = page;
        offsets[slot] = offset;
        lengths[slot] = bytes.length;
        hashes[slot] = hash;
        states[slot] = LIVE | INTERNED;
        leases[slot] = 0;
        internTable[bucket] = slot + 1;
        atomCount++;
        return pack(slot, generations[slot]);
    }

    /** Creates a descriptor sharing bytes in the same immutable backing page. */
    public synchronized long slice(long source, int from, int length) {
        final int original = validSlot(source);
        Objects.checkFromIndexSize(from, length, lengths[original]);
        final int slot = allocateSlot();
        pageIds[slot] = pageIds[original];
        offsets[slot] = offsets[original] + from;
        lengths[slot] = length;
        hashes[slot] = 0;
        leases[slot] = 0;
        states[slot] = LIVE;
        return pack(slot, generations[slot]);
    }

    /** Read-only, zero-copy facade over the backing storage. */
    public synchronized ByteBuffer asReadOnlyBuffer(long handle) {
        final int slot = validSlot(handle);
        return ByteBuffer.wrap(pages[pageIds[slot]], offsets[slot], lengths[slot])
                .asReadOnlyBuffer().slice();
    }

    public synchronized byte byteAt(long handle, int index) {
        final int slot = validSlot(handle);
        Objects.checkIndex(index, lengths[slot]);
        return pages[pageIds[slot]][offsets[slot] + index];
    }

    public synchronized byte[] copyBytes(long handle) {
        final int slot = validSlot(handle);
        return Arrays.copyOfRange(pages[pageIds[slot]],
                offsets[slot], offsets[slot] + lengths[slot]);
    }

    public synchronized int length(long handle) {
        return lengths[validSlot(handle)];
    }

    /** Copies only the selected range, without materializing an intermediate slice. */
    synchronized void copyTo(long handle, int from, byte[] destination,
                             int destinationOffset, int length) {
        final int slot = validSlot(handle);
        Objects.checkFromIndexSize(from, length, lengths[slot]);
        Objects.checkFromIndexSize(destinationOffset, length, destination.length);
        System.arraycopy(pages[pageIds[slot]], offsets[slot] + from,
                         destination, destinationOffset, length);
    }

    /** A stable page index; two slices from the same atom have the same page. */
    public synchronized int pageId(long handle) {
        return pageIds[validSlot(handle)];
    }

    /** A byte offset into the backing page. */
    public synchronized int byteOffset(long handle) {
        return offsets[validSlot(handle)];
    }

    public synchronized int stateMask(long handle) {
        return states[validSlot(handle)];
    }

    public synchronized int ownerCount(long handle) {
        return leases[validSlot(handle)];
    }

    public synchronized void retain(long handle) {
        final int slot = validSlot(handle);
        if (leases[slot] == Integer.MAX_VALUE) {
            throw new IllegalStateException("M3 byte slice lease overflow");
        }
        leases[slot]++;
        updateLeaseBits(slot);
    }

    public synchronized void release(long handle) {
        final int slot = validSlot(handle);
        if (leases[slot] == 0) {
            throw new IllegalStateException("unbalanced M3 byte slice release");
        }
        leases[slot]--;
        updateLeaseBits(slot);
    }

    /**
     * Invalidates a non-interned slice only when it has zero leases.
     * Interned atoms remain canonical for this pool's lifetime.
     */
    public synchronized boolean forgetSlice(long handle) {
        final int slot = validSlot(handle);
        if ((states[slot] & INTERNED) != 0 || leases[slot] != 0) {
            return false;
        }
        if (generations[slot] == Integer.MAX_VALUE) {
            states[slot] = RETIRED;
        } else {
            generations[slot]++;
            states[slot] = 0;
            freeLinks[slot] = freeHead;
            freeHead = slot;
        }
        return true;
    }

    public synchronized int atomCount() {
        return atomCount;
    }

    private void updateLeaseBits(int slot) {
        final int owners = leases[slot];
        final int atomBit = states[slot] & INTERNED;
        states[slot] = LIVE | atomBit | (owners > 0 ? LEASED : 0)
                | (owners > 1 ? SHARED : 0);
    }

    private int locateBucket(byte[] bytes, int hash) {
        final int mask = internTable.length - 1;
        int bucket = mix(hash) & mask;
        while (internTable[bucket] != 0) {
            final int slot = internTable[bucket] - 1;
            if (hashes[slot] == hash && lengths[slot] == bytes.length
                    && equalsStored(slot, bytes)) {
                return bucket;
            }
            bucket = (bucket + 1) & mask;
        }
        return bucket;
    }

    private boolean equalsStored(int slot, byte[] candidate) {
        final byte[] page = pages[pageIds[slot]];
        final int from = offsets[slot];
        for (int i = 0; i < candidate.length; i++) {
            if (page[from + i] != candidate[i]) {
                return false;
            }
        }
        return true;
    }

    private void growInternTable() {
        final int oldCapacity = internTable.length;
        if (oldCapacity >= (1 << 29)) {
            throw new OutOfMemoryError("M3 intern table too large");
        }
        internTable = new int[oldCapacity << 1];
        final int mask = internTable.length - 1;
        for (int slot = 0; slot < allocated; slot++) {
            if ((states[slot] & INTERNED) == 0) {
                continue;
            }
            int bucket = mix(hashes[slot]) & mask;
            while (internTable[bucket] != 0) {
                bucket = (bucket + 1) & mask;
            }
            internTable[bucket] = slot + 1;
        }
    }

    private static int mix(int hash) {
        hash ^= (hash >>> 16);
        hash *= 0x7feb352d;
        hash ^= (hash >>> 15);
        hash *= 0x846ca68b;
        return hash ^ (hash >>> 16);
    }

    private int findWritablePage(int length) {
        if (pageCount != 0) {
            final int current = pageCount - 1;
            if (pages[current].length - pageUsed[current] >= length) {
                return current;
            }
        }
        if (pageCount == pages.length) {
            if (pages.length >= MAX_ARRAY_SIZE) {
                throw new OutOfMemoryError("M3 page table limit");
            }
            final int grown = (int) Math.min(MAX_ARRAY_SIZE,
                    Math.max((long) pages.length + (pages.length >> 1), pageCount + 1));
            pages = Arrays.copyOf(pages, grown);
            pageUsed = Arrays.copyOf(pageUsed, grown);
        }
        final int next = pageCount++;
        pages[next] = new byte[Math.max(PAGE_SIZE, length)];
        pageUsed[next] = 0;
        return next;
    }

    private int allocateSlot() {
        if (freeHead >= 0) {
            final int slot = freeHead;
            freeHead = freeLinks[slot];
            return slot;
        }
        if (allocated == MAX_ARRAY_SIZE) {
            throw new OutOfMemoryError("M3 descriptor index limit");
        }
        ensureDescriptorCapacity(allocated + 1);
        final int slot = allocated++;
        generations[slot] = 1;
        return slot;
    }

    private void ensureDescriptorCapacity(int needed) {
        if (needed <= states.length) {
            return;
        }
        final int grown = (int) Math.min(MAX_ARRAY_SIZE,
                Math.max((long) states.length + (states.length >> 1), needed));
        pageIds = Arrays.copyOf(pageIds, grown);
        offsets = Arrays.copyOf(offsets, grown);
        lengths = Arrays.copyOf(lengths, grown);
        hashes = Arrays.copyOf(hashes, grown);
        generations = Arrays.copyOf(generations, grown);
        states = Arrays.copyOf(states, grown);
        leases = Arrays.copyOf(leases, grown);
        freeLinks = Arrays.copyOf(freeLinks, grown);
    }

    private int validSlot(long handle) {
        final int slot = (int) handle;
        final int generation = (int) (handle >>> 32);
        if (slot < 0 || slot >= allocated || generation == 0
                || (states[slot] & LIVE) == 0 || generations[slot] != generation) {
            throw new IllegalArgumentException("invalid or stale M3 byte handle");
        }
        return slot;
    }

    private static long pack(int slot, int generation) {
        return ((long) generation << 32) | Integer.toUnsignedLong(slot);
    }
}
