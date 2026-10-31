// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package com.m3.ast;

import java.util.Arrays;
import java.util.Objects;

/**
 * Canonical immutable primitive-lane atom store.
 *
 * <p>The mechanics deliberately mirror M3String: identity is owner-local coordinate state, payload
 * is external/canonical, and composition is retained as primitive child coordinates. There is no
 * retained per-node object graph, Map.Entry, linked node or copied payload array.</p>
 *
 * <p>Payload IDs are stable coordinates supplied by the owning M3 String/lexicon generation.
 * Zero means no payload. Children must already exist, which makes admission naturally bottom-up
 * and unable to create a cyclic atom graph.</p>
 */
public final class M3AtomStore {
    private final byte[] domains;
    private final int[] types;
    private final long[] payloadIds;
    private final long[] metadataBits;
    private final int[] childOffsets;
    private final int[] childCounts;
    private final long[] contentHashes;
    private final long[] structureHashes;
    private final int[] subtreeSizes;
    private final int[] depths;
    private final int[] childArena;

    private M3AtomStore(
            byte[] domains,
            int[] types,
            long[] payloadIds,
            long[] metadataBits,
            int[] childOffsets,
            int[] childCounts,
            long[] contentHashes,
            long[] structureHashes,
            int[] subtreeSizes,
            int[] depths,
            int[] childArena) {
        this.domains = domains;
        this.types = types;
        this.payloadIds = payloadIds;
        this.metadataBits = metadataBits;
        this.childOffsets = childOffsets;
        this.childCounts = childCounts;
        this.contentHashes = contentHashes;
        this.structureHashes = structureHashes;
        this.subtreeSizes = subtreeSizes;
        this.depths = depths;
        this.childArena = childArena;
    }

    public static Builder builder() {
        return new Builder();
    }

    public int atomCount() {
        return types.length;
    }

    public M3AtomDomain domain(int atomId) {
        int row = checked(atomId);
        return M3AtomDomain.values()[Byte.toUnsignedInt(domains[row])];
    }

    public int type(int atomId) {
        return types[checked(atomId)];
    }

    public long payloadId(int atomId) {
        return payloadIds[checked(atomId)];
    }

    public long metadataBits(int atomId) {
        return metadataBits[checked(atomId)];
    }

    public int childCount(int atomId) {
        return childCounts[checked(atomId)];
    }

    public int childAt(int atomId, int ordinal) {
        int row = checked(atomId);
        int count = childCounts[row];
        return childArena[childOffsets[row] + Objects.checkIndex(ordinal, count)];
    }

    public int[] children(int atomId) {
        int row = checked(atomId);
        int start = childOffsets[row];
        return Arrays.copyOfRange(childArena, start, start + childCounts[row]);
    }

    public long contentHash(int atomId) {
        return contentHashes[checked(atomId)];
    }

    /**
     * Payload/metadata-independent structural hash. Equal tree shape + domain/type yields equality.
     */
    public long structureHash(int atomId) {
        return structureHashes[checked(atomId)];
    }

    public int subtreeSize(int atomId) {
        return subtreeSizes[checked(atomId)];
    }

    public int depth(int atomId) {
        return depths[checked(atomId)];
    }

    public long primitiveBytes() {
        return domains.length
                + 4L * (types.length + childOffsets.length + childCounts.length
                        + subtreeSizes.length + depths.length + childArena.length)
                + 8L * (payloadIds.length + metadataBits.length
                        + contentHashes.length + structureHashes.length);
    }

    private int checked(int atomId) {
        return Objects.checkIndex(atomId, types.length);
    }

    /** Mutable admission phase; the frozen image owns only primitive lanes. */
    public static final class Builder {
        private static final double LOAD = 0.65d;
        private byte[] domains = new byte[32];
        private int[] types = new int[32];
        private long[] payloadIds = new long[32];
        private long[] metadataBits = new long[32];
        private int[] childOffsets = new int[32];
        private int[] childCounts = new int[32];
        private long[] contentHashes = new long[32];
        private long[] structureHashes = new long[32];
        private int[] subtreeSizes = new int[32];
        private int[] depths = new int[32];
        private int[] childArena = new int[64];
        private int[] slots = new int[64];
        private int size;
        private int childSize;
        private boolean frozen;

        public int atomCount() {
            return size;
        }

        public int intern(
                M3AtomDomain domain,
                int type,
                long payloadId,
                long metadata,
                int... children) {
            ensureMutable();
            Objects.requireNonNull(domain, "domain");
            Objects.requireNonNull(children, "children");
            if (type < 0) throw new IllegalArgumentException("atom type must be >= 0");
            if (payloadId < 0L) throw new IllegalArgumentException("payload ID must be >= 0");

            int[] childCopy = children.clone();
            for (int child : childCopy) Objects.checkIndex(child, size);

            long structureHash = startHash(domain.ordinal(), type);
            structureHash = mix(structureHash, childCopy.length);
            long contentHash = mix(structureHash, payloadId);
            contentHash = mix(contentHash, metadata);
            contentHash = mix(contentHash, childCopy.length);
            int subtreeSize = 1;
            int depth = 1;
            for (int child : childCopy) {
                structureHash = mix(structureHash, structureHashes[child]);
                contentHash = mix(contentHash, contentHashes[child]);
                subtreeSize = Math.addExact(subtreeSize, subtreeSizes[child]);
                depth = Math.max(depth, Math.addExact(depths[child], 1));
            }

            reserveTable(size + 1);
            int slot = findSlot(domain, type, payloadId, metadata, childCopy, contentHash);
            int encoded = slots[slot];
            if (encoded != 0) return encoded - 1;

            reserveRows(size + 1);
            reserveChildren(Math.addExact(childSize, childCopy.length));

            int row = size++;
            domains[row] = (byte) domain.ordinal();
            types[row] = type;
            payloadIds[row] = payloadId;
            metadataBits[row] = metadata;
            childOffsets[row] = childSize;
            childCounts[row] = childCopy.length;
            contentHashes[row] = contentHash;
            structureHashes[row] = structureHash;
            subtreeSizes[row] = subtreeSize;
            depths[row] = depth;
            System.arraycopy(childCopy, 0, childArena, childSize, childCopy.length);
            childSize += childCopy.length;

            slot = findSlot(domain, type, payloadId, metadata, childCopy, contentHash);
            slots[slot] = row + 1;
            return row;
        }

        public int intern(M3AtomDomain domain, int type, long payloadId, int... children) {
            return intern(domain, type, payloadId, 0L, children);
        }

        public M3AtomStore freeze() {
            ensureMutable();
            frozen = true;
            return new M3AtomStore(
                    Arrays.copyOf(domains, size),
                    Arrays.copyOf(types, size),
                    Arrays.copyOf(payloadIds, size),
                    Arrays.copyOf(metadataBits, size),
                    Arrays.copyOf(childOffsets, size),
                    Arrays.copyOf(childCounts, size),
                    Arrays.copyOf(contentHashes, size),
                    Arrays.copyOf(structureHashes, size),
                    Arrays.copyOf(subtreeSizes, size),
                    Arrays.copyOf(depths, size),
                    Arrays.copyOf(childArena, childSize));
        }

        private int findSlot(
                M3AtomDomain domain,
                int type,
                long payloadId,
                long metadata,
                int[] children,
                long contentHash) {
            int mask = slots.length - 1;
            int slot = mix32(contentHash) & mask;
            while (true) {
                int encoded = slots[slot];
                if (encoded == 0) return slot;
                int row = encoded - 1;
                if (contentHashes[row] == contentHash
                        && Byte.toUnsignedInt(domains[row]) == domain.ordinal()
                        && types[row] == type
                        && payloadIds[row] == payloadId
                        && metadataBits[row] == metadata
                        && childCounts[row] == children.length
                        && childrenEqual(row, children)) {
                    return slot;
                }
                slot = (slot + 1) & mask;
            }
        }

        private boolean childrenEqual(int row, int[] children) {
            int start = childOffsets[row];
            for (int i = 0; i < children.length; i++) {
                if (childArena[start + i] != children[i]) return false;
            }
            return true;
        }

        private void reserveRows(int required) {
            if (required <= types.length) return;
            int next = grown(types.length, required);
            domains = Arrays.copyOf(domains, next);
            types = Arrays.copyOf(types, next);
            payloadIds = Arrays.copyOf(payloadIds, next);
            metadataBits = Arrays.copyOf(metadataBits, next);
            childOffsets = Arrays.copyOf(childOffsets, next);
            childCounts = Arrays.copyOf(childCounts, next);
            contentHashes = Arrays.copyOf(contentHashes, next);
            structureHashes = Arrays.copyOf(structureHashes, next);
            subtreeSizes = Arrays.copyOf(subtreeSizes, next);
            depths = Arrays.copyOf(depths, next);
        }

        private void reserveChildren(int required) {
            if (required <= childArena.length) return;
            childArena = Arrays.copyOf(childArena, grown(childArena.length, required));
        }

        private void reserveTable(int expected) {
            if (expected <= (int) (slots.length * LOAD)) return;
            int capacity = slots.length;
            while (expected > (int) (capacity * LOAD)) {
                if (capacity >= (1 << 30)) throw new IllegalStateException("atom table too large");
                capacity <<= 1;
            }
            int[] replacement = new int[capacity];
            int mask = capacity - 1;
            for (int row = 0; row < size; row++) {
                int slot = mix32(contentHashes[row]) & mask;
                while (replacement[slot] != 0) slot = (slot + 1) & mask;
                replacement[slot] = row + 1;
            }
            slots = replacement;
        }

        private void ensureMutable() {
            if (frozen) throw new IllegalStateException("atom builder already frozen");
        }

        private static int grown(int current, int required) {
            long next = Math.max(required, Math.max(2L, current + (current >>> 1) + 1L));
            if (next > Integer.MAX_VALUE - 8L) throw new IllegalStateException("atom capacity");
            return (int) next;
        }
    }

    static long startHash(int domain, int type) {
        long hash = 0x9e3779b97f4a7c15L;
        hash = mix(hash, domain);
        return mix(hash, type);
    }

    static long mix(long hash, long value) {
        long x = value + 0x9e3779b97f4a7c15L;
        x = (x ^ (x >>> 30)) * 0xbf58476d1ce4e5b9L;
        x = (x ^ (x >>> 27)) * 0x94d049bb133111ebL;
        x ^= x >>> 31;
        return hash ^ (x + 0x9e3779b97f4a7c15L + (hash << 6) + (hash >>> 2));
    }

    private static int mix32(long value) {
        long x = value;
        x ^= x >>> 33;
        x *= 0xff51afd7ed558ccdL;
        x ^= x >>> 33;
        x *= 0xc4ceb9fe1a85ec53L;
        x ^= x >>> 33;
        return (int) (x ^ (x >>> 32));
    }
}
