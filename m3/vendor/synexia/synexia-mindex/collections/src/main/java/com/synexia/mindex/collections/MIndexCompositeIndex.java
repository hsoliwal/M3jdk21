// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Canonical integer identity for immutable collection lanes.
 *
 * <p>Composite identity is local to this index. Collection occurrence state is
 * retained as primitive metadata plus one packed integer arena. Domain handles
 * are registered once per distinct {@link MIndexSpace}; repeated occurrences
 * never retain Java references.
 *
 * <p>Hashes and signals are candidate locators only. Canonical reuse is always
 * confirmed by exact domain, kind, length and arena content.
 */
public final class MIndexCompositeIndex {
    public static final int KIND_LIST = 1;
    public static final int KIND_SET = 2;
    public static final int KIND_MAP = 3;
    public static final int KIND_TUPLE = 4;
    public static final int KIND_GRAPH_EDGE_SET = 5;
    public static final int KIND_TABLE = 6;
    public static final int KIND_DEQUE = 7;
    public static final int KIND_MULTI_MAP = 8;
    public static final int KIND_PRIORITY_QUEUE = 9;
    public static final int KIND_SHAPE = 10;
    public static final int KIND_BAG = 11;
    public static final int KIND_ORDERED_SET = 12;

    private static final int[] BUILT_IN_KINDS = {
        KIND_LIST,
        KIND_SET,
        KIND_MAP,
        KIND_TUPLE,
        KIND_GRAPH_EDGE_SET,
        KIND_TABLE,
        KIND_DEQUE,
        KIND_MULTI_MAP,
        KIND_PRIORITY_QUEUE,
        KIND_SHAPE,
        KIND_BAG,
        KIND_ORDERED_SET
    };

    static {
        validateBuiltInKinds();
    }

    private final Map<Object, Integer> domainIds = new IdentityHashMap<>();
    private final Map<Object, Object> domainRepresentatives =
            new IdentityHashMap<>();
    private final MIndexCompositeSpace compositeSpace = new MIndexCompositeSpace(this);
    private int nextDomainId;

    private int[] arena = new int[32];
    private int arenaSize;
    private int[] offsets = new int[16];
    private int[] lengths = new int[16];
    private int[] kinds = new int[16];
    private int[] primaryDomains = new int[16];
    private int[] secondaryDomains = new int[16];
    private long[] hashes = new long[16];
    private long[] signals = new long[16];
    private int[] table = new int[32];
    private int size;

    public int intern(int kind, int[] lane) {
        return intern(kind, null, null, lane);
    }

    public int intern(
            int kind,
            Object primaryDomain,
            Object secondaryDomain,
            int[] lane) {
        checkKind(kind);
        int[] values = lane == null ? new int[0] : lane;
        int primary = domainId(primaryDomain);
        int secondary = domainId(secondaryDomain);
        long hash = structuralHash(kind, primary, secondary, values);
        long signal = collectionSignal(kind, primary, secondary, values);
        int existing = findEntry(kind, primary, secondary, values, hash, signal);
        if (existing >= 0) {
            return existing;
        }
        ensureEntryCapacity(size + 1);
        ensureArenaCapacity(arenaSize + values.length);
        if ((size + 1L) * 3L >= table.length * 2L) {
            rehash(table.length << 1);
        }

        int id = size++;
        offsets[id] = arenaSize;
        lengths[id] = values.length;
        kinds[id] = kind;
        primaryDomains[id] = primary;
        secondaryDomains[id] = secondary;
        hashes[id] = hash;
        signals[id] = signal;
        System.arraycopy(values, 0, arena, arenaSize, values.length);
        arenaSize += values.length;

        int slot = vacant(hash, kind, primary, secondary);
        table[slot] = id + 1;
        return id;
    }

    public int findId(
            int kind,
            Object primaryDomain,
            Object secondaryDomain,
            int[] lane) {
        checkKind(kind);
        int primary = findDomainId(primaryDomain);
        int secondary = findDomainId(secondaryDomain);
        if (primary == Integer.MIN_VALUE || secondary == Integer.MIN_VALUE) {
            return -1;
        }
        int[] values = lane == null ? new int[0] : lane;
        long hash = structuralHash(kind, primary, secondary, values);
        long signal = collectionSignal(kind, primary, secondary, values);
        return findEntry(kind, primary, secondary, values, hash, signal);
    }

    public int size() {
        return size;
    }

    public boolean containsId(int id) {
        return id >= 0 && id < size;
    }

    /**
     * Stable MIndex space whose primitive IDs are canonical IDs in this store.
     * This is the recursive collection-as-value boundary.
     */
    public MIndexCompositeSpace space() {
        return compositeSpace;
    }

    public static int[] builtInKinds() {
        return BUILT_IN_KINDS.clone();
    }

    public static boolean isBuiltInKind(int kind) {
        for (int candidate : BUILT_IN_KINDS) {
            if (candidate == kind) {
                return true;
            }
        }
        return false;
    }

    public static String kindName(int kind) {
        return switch (kind) {
            case KIND_LIST -> "LIST";
            case KIND_SET -> "SET";
            case KIND_MAP -> "MAP";
            case KIND_TUPLE -> "TUPLE";
            case KIND_GRAPH_EDGE_SET -> "GRAPH_EDGE_SET";
            case KIND_TABLE -> "TABLE";
            case KIND_DEQUE -> "DEQUE";
            case KIND_MULTI_MAP -> "MULTI_MAP";
            case KIND_PRIORITY_QUEUE -> "PRIORITY_QUEUE";
            case KIND_SHAPE -> "SHAPE";
            case KIND_BAG -> "BAG";
            case KIND_ORDERED_SET -> "ORDERED_SET";
            default -> "CUSTOM(" + kind + ')';
        };
    }

    public int kind(int id) {
        requireId(id);
        return kinds[id];
    }

    public int length(int id) {
        requireId(id);
        return lengths[id];
    }

    public int valueAt(int id, int index) {
        requireId(id);
        int length = lengths[id];
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException(index);
        }
        return arena[offsets[id] + index];
    }

    public int[] copyLane(int id) {
        requireId(id);
        int offset = offsets[id];
        return Arrays.copyOfRange(arena, offset, offset + lengths[id]);
    }

    public long structuralHash64(int id) {
        requireId(id);
        return hashes[id];
    }

    public long signal64(int id) {
        requireId(id);
        return signals[id];
    }

    public boolean sameCanonical(int leftId, int rightId) {
        requireId(leftId);
        requireId(rightId);
        return leftId == rightId;
    }

    boolean hasDomains(int id, Object primaryDomain, Object secondaryDomain) {
        requireId(id);
        int primary = findDomainId(primaryDomain);
        int secondary = findDomainId(secondaryDomain);
        return primary != Integer.MIN_VALUE
                && secondary != Integer.MIN_VALUE
                && primaryDomains[id] == primary
                && secondaryDomains[id] == secondary;
    }

    /**
     * Package-private semantic-domain projection for exact composite transfer.
     *
     * <p>One reference is retained per registered domain, never per collection
     * occurrence. The public collection API continues to expose typed spaces
     * through each semantic owner rather than raw domain objects.
     */
    Object primaryDomainObject(int id) {
        requireId(id);
        return domainObject(primaryDomains[id]);
    }

    Object secondaryDomainObject(int id) {
        requireId(id);
        return domainObject(secondaryDomains[id]);
    }

    private int findEntry(
            int kind,
            int primary,
            int secondary,
            int[] values,
            long hash,
            long signal) {
        int mask = table.length - 1;
        int slot = slot(hash, kind, primary, secondary, mask);
        while (table[slot] != 0) {
            int id = table[slot] - 1;
            if (kinds[id] == kind
                    && primaryDomains[id] == primary
                    && secondaryDomains[id] == secondary
                    && hashes[id] == hash
                    && signals[id] == signal
                    && exact(id, values)) {
                return id;
            }
            slot = (slot + 1) & mask;
        }
        return -1;
    }

    private boolean exact(int id, int[] values) {
        if (lengths[id] != values.length) {
            return false;
        }
        int offset = offsets[id];
        for (int index = 0; index < values.length; index++) {
            if (arena[offset + index] != values[index]) {
                return false;
            }
        }
        return true;
    }

    private int vacant(long hash, int kind, int primary, int secondary) {
        int mask = table.length - 1;
        int slot = slot(hash, kind, primary, secondary, mask);
        while (table[slot] != 0) {
            slot = (slot + 1) & mask;
        }
        return slot;
    }

    private int domainId(Object domain) {
        if (domain == null) {
            return -1;
        }
        Object authority = domainAuthority(domain);
        Integer existing = domainIds.get(authority);
        if (existing != null) {
            return existing;
        }
        if (nextDomainId == IdSupport.MAX_ID) {
            throw new IllegalStateException("MIndex composite domain registry exhausted");
        }
        int id = nextDomainId++;
        domainIds.put(authority, id);
        domainRepresentatives.put(authority, domain);
        return id;
    }

    private int findDomainId(Object domain) {
        if (domain == null) {
            return -1;
        }
        Integer id = domainIds.get(domainAuthority(domain));
        return id == null ? Integer.MIN_VALUE : id;
    }

    private static Object domainAuthority(Object domain) {
        if (domain instanceof MIndexSpace<?> space) {
            return java.util.Objects.requireNonNull(
                    space.identityAuthority(),
                    "MIndexSpace.identityAuthority()");
        }
        return domain;
    }

    private Object domainObject(int domainId) {
        if (domainId < 0) {
            return null;
        }
        if (domainId >= nextDomainId) {
            throw new IllegalStateException(
                    "MIndex composite domain ID outside registry: " + domainId);
        }
        for (Map.Entry<Object, Integer> entry
                : domainIds.entrySet()) {
            if (entry.getValue() == domainId) {
                Object representative =
                        domainRepresentatives.get(entry.getKey());
                if (representative == null) {
                    throw new IllegalStateException(
                            "MIndex composite domain representative missing: "
                                    + domainId);
                }
                return representative;
            }
        }
        throw new IllegalStateException(
                "MIndex composite domain registry hole: " + domainId);
    }

    private void ensureEntryCapacity(int needed) {
        if (needed <= offsets.length) {
            return;
        }
        int capacity = IdSupport.grown(offsets.length, needed);
        offsets = Arrays.copyOf(offsets, capacity);
        lengths = Arrays.copyOf(lengths, capacity);
        kinds = Arrays.copyOf(kinds, capacity);
        primaryDomains = Arrays.copyOf(primaryDomains, capacity);
        secondaryDomains = Arrays.copyOf(secondaryDomains, capacity);
        hashes = Arrays.copyOf(hashes, capacity);
        signals = Arrays.copyOf(signals, capacity);
    }

    private void ensureArenaCapacity(int needed) {
        if (needed <= arena.length) {
            return;
        }
        arena = Arrays.copyOf(arena, IdSupport.grown(arena.length, needed));
    }

    private void rehash(int requested) {
        int capacity = 8;
        while (capacity < requested) {
            capacity <<= 1;
        }
        int[] next = new int[capacity];
        int mask = capacity - 1;
        for (int id = 0; id < size; id++) {
            int slot = slot(
                    hashes[id],
                    kinds[id],
                    primaryDomains[id],
                    secondaryDomains[id],
                    mask);
            while (next[slot] != 0) {
                slot = (slot + 1) & mask;
            }
            next[slot] = id + 1;
        }
        table = next;
    }

    private static long structuralHash(
            int kind,
            int primary,
            int secondary,
            int[] values) {
        long hash = IdSupport.sequenceHash(kind, values, values.length);
        hash ^= Long.rotateLeft(Integer.toUnsignedLong(primary + 1), 17);
        hash ^= Long.rotateLeft(Integer.toUnsignedLong(secondary + 1), 41);
        return IdSupport.mix64(hash);
    }

    private static long collectionSignal(
            int kind,
            int primary,
            int secondary,
            int[] values) {
        long xor = 0L;
        long sum = 0L;
        long ordered = 0x9e3779b97f4a7c15L;
        for (int index = 0; index < values.length; index++) {
            long mixed = IdSupport.mix64(
                    Integer.toUnsignedLong(values[index])
                            ^ (0x94d049bb133111ebL * (index + 1L)));
            xor ^= mixed;
            sum += mixed;
            ordered = Long.rotateLeft(ordered ^ mixed, 13);
        }
        long seed = Integer.toUnsignedLong(kind)
                ^ Long.rotateLeft(Integer.toUnsignedLong(primary + 1), 11)
                ^ Long.rotateLeft(Integer.toUnsignedLong(secondary + 1), 37)
                ^ ((long) values.length << 32)
                ^ xor
                ^ Long.rotateLeft(sum, 23)
                ^ ordered;
        return IdSupport.mix64(seed);
    }

    private static int slot(
            long hash,
            int kind,
            int primary,
            int secondary,
            int mask) {
        long mixed = hash
                ^ (Integer.toUnsignedLong(kind) << 32)
                ^ Long.rotateLeft(Integer.toUnsignedLong(primary + 1), 19)
                ^ Long.rotateLeft(Integer.toUnsignedLong(secondary + 1), 43);
        return IdSupport.mix(mixed) & mask;
    }

    private static void checkKind(int kind) {
        if (kind <= 0) {
            throw new IllegalArgumentException("kind must be positive");
        }
    }

    private static void validateBuiltInKinds() {
        for (int left = 0; left < BUILT_IN_KINDS.length; left++) {
            if (BUILT_IN_KINDS[left] <= 0) {
                throw new ExceptionInInitializerError(
                        "MIndex built-in kind must be positive: " + BUILT_IN_KINDS[left]);
            }
            for (int right = left + 1; right < BUILT_IN_KINDS.length; right++) {
                if (BUILT_IN_KINDS[left] == BUILT_IN_KINDS[right]) {
                    throw new ExceptionInInitializerError(
                            "duplicate MIndex built-in collection kind: "
                                    + BUILT_IN_KINDS[left]);
                }
            }
        }
    }

    private void requireId(int id) {
        if (!containsId(id)) {
            throw new IndexOutOfBoundsException("composite id: " + id);
        }
    }
}
